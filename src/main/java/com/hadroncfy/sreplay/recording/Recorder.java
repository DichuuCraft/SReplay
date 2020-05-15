package com.hadroncfy.sreplay.recording;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.hadroncfy.sreplay.SReplayMod;
import com.hadroncfy.sreplay.config.TextRenderer;
import com.hadroncfy.sreplay.mixin.PlayerSpawnS2CPacketAccessor;
import com.mojang.authlib.GameProfile;
import com.replaymod.replaystudio.data.Marker;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import com.replaymod.replaystudio.replay.ZipReplayFile;
import com.replaymod.replaystudio.studio.ReplayStudio;

import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.login.LoginSuccessS2CPacket;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerSpawnS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.PacketByteBuf;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class Recorder implements IPacketListener {
    private static final String RECORDING_SUBDIR = "raw";
    private static final String MARKER_PAUSE = "PAUSE";
    private static final Logger LOGGER = LogManager.getLogger();
    private final MinecraftServer server;
    private final GameProfile profile;
    
    private final Set<UUID> uuids = new HashSet<>();
    
    private final File outputPath;
    private final ReplayFile replayFile;
    private final DataOutputStream packetSaveStream;
    private final ReplayMetaData metaData;
    private final RecordingParam param;
    
    private final ExecutorService saveService = Executors.newSingleThreadExecutor();
    private long startTime;
    private int startTick;
    private long lastPacket;
    private long timeShift = 0;
    private NetworkState nstate = NetworkState.LOGIN;
    private boolean stopped = false;
    private boolean isSaving = false;
    private long bytesRecorded = 0; 
    private boolean paused = false;
    private boolean gPaused = false;
    private boolean followTick = false;
    private final List<Marker> markers = new ArrayList<>();

    private ISizeLimitExceededListener limiter = null;

    public Recorder(GameProfile profile, MinecraftServer server, File outputPath, RecordingParam param) throws IOException {
        this.server = server;
        this.profile = profile;

        this.param = param;
        this.outputPath = outputPath;

        replayFile = new ZipReplayFile(new ReplayStudio(), outputPath);
        packetSaveStream = new DataOutputStream(replayFile.writePacketData(true));
        metaData = new ReplayMetaData();
    }

    public void setOnSizeLimitExceededListener(ISizeLimitExceededListener l){
        limiter = l;
    }

    public long getStartTime(){
        return startTime;
    }

    public boolean isPaused(){
        return paused;
    }

    public void start() {
        startTime = System.currentTimeMillis();
        startTick = server.getTicks();
        
        metaData.setSingleplayer(false);
        metaData.setServerName(SReplayMod.getConfig().serverName);
        metaData.setGenerator("sreplay");
        metaData.setDate(startTime);
        metaData.setMcVersion("1.14.4");
        server.getPlayerManager()
            .broadcastChatMessage(TextRenderer.render(SReplayMod.getFormats().startedRecording, profile.getName()), true);

        // Must contain this packet, otherwise ReplayMod would complain
        savePacket(new LoginSuccessS2CPacket(profile));
        nstate = NetworkState.PLAY;
    }

    private byte[] getPacketBytes(Packet<?> packet) throws Exception {
        int id = nstate.getPacketId(NetworkSide.CLIENTBOUND, packet);
        ByteBuf bbuf = Unpooled.buffer();
        PacketByteBuf packetBuf = new PacketByteBuf(bbuf);
        packetBuf.writeVarInt(id);
        packet.write(packetBuf);

        bbuf.readerIndex(0);
        byte[] ret = new byte[bbuf.readableBytes()];
        bbuf.readBytes(ret);
        bbuf.release();
        return ret;
    }

    public void setPaused(){
        paused = true;
    }

    // Should only increase
    public long getRecordedTime(){
        final long base;
        if (followTick){
            base = (server.getTicks() - startTick) * 50;
        }
        else {
            base = System.currentTimeMillis() - startTime;
        }
        return base - timeShift;
    }

    private synchronized long getCurrentTimeAndUpdate(){
        long now = getRecordedTime();
        if (paused){
            if (!gPaused){
                paused = false;
            }
            timeShift += now - lastPacket;
            return lastPacket;
        }
        return lastPacket = now;
    }

    public void pauseRecording(){
        gPaused = true;
        paused = true;
        addMarker(MARKER_PAUSE);
    }

    public boolean isRecordingPaused(){
        return gPaused;
    }

    public boolean isSoftPaused(){
        return !gPaused && paused;
    }

    public void resumeRecording(){
        gPaused = false;
    }

    public synchronized void setFollowTick(boolean f){
        if (followTick != f){
            final long t1 = getRecordedTime();
            followTick = f;
            final long t2 = getRecordedTime();
            timeShift = t2 - t1;
        }
        else {
            followTick = f;
        }
    }

    private void savePacket(Packet<?> packet) {
        if (param.sizeLimit != -1 && bytesRecorded > param.sizeLimit || param.timeLimit != -1 && getRecordedTime() > param.timeLimit){
            stop();
            if (limiter != null){
                limiter.onSizeLimitExceeded(bytesRecorded);
            }
            return;
        }
        try {
            byte[] pb = getPacketBytes(packet);
            saveService.submit(() -> {
                try {
                    // XXX: should be put outside?
                    final long timeStamp = getCurrentTimeAndUpdate();
                    packetSaveStream.writeInt((int) timeStamp);
                    packetSaveStream.writeInt(pb.length);
                    packetSaveStream.write(pb);
                    bytesRecorded += pb.length + 8;
                } catch (IOException e) {
                    LOGGER.error("Error saving packet: " + e);
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            LOGGER.error("Error saving packet: " + e);
            e.printStackTrace();
        }
    }

    public void stop(){
        stopped = true;
    }

    public long getRecordedBytes(){
        return bytesRecorded;
    }

    public void addMarker(String name){
        Marker m = new Marker();
        m.setName(name);
        m.setTime((int) getRecordedTime());
        markers.add(m);

        saveMarkers();
    }

    public List<Marker> getMarkers(){
        return markers;
    }

    public void removeMarker(int i){
        markers.remove(i);
        saveMarkers();
    }

    private void saveMetadata(){
        saveService.submit(() -> {
            String[] players = new String[uuids.size()];
            uuids.stream().map(uuid -> uuid.toString()).collect(Collectors.toList()).toArray(players);
            metaData.setPlayers(players);
            try {
                replayFile.writeMetaData(metaData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void saveMarkers(){
        if (markers.size() > 0){
            saveService.submit(() -> {
                Set<Marker> m = new HashSet<>();
                m.addAll(markers);
                try {
                    replayFile.writeMarkers(m);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public CompletableFuture<Void> saveRecording(File dest) {
        if (!isSaving){
            isSaving = true;
            metaData.setDuration((int) lastPacket);
            server.getPlayerManager()
                    .broadcastChatMessage(TextRenderer.render(SReplayMod.getFormats().savingRecordingFile, profile.getName()), true);
            return CompletableFuture.runAsync(() -> {
                saveMetadata();
                saveMarkers();
                saveService.shutdown();
                try {
                    saveService.awaitTermination(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    synchronized (replayFile) {
                        replayFile.saveTo(dest);
                        replayFile.close();
                    }
                }
                catch(IOException e){
                    e.printStackTrace();
                    throw new CompletionException(e);
                }
            });
        }
        else {
            LOGGER.warn("saveRecording() called twice");
            return CompletableFuture.supplyAsync(() -> {
                throw new IllegalStateException("saveRecording() called twice");
            });
        }
    }

    @Override
    public void onPacket(Packet<?> p) {
        if (!stopped){
            if (p instanceof PlayerSpawnS2CPacket){
                uuids.add(((PlayerSpawnS2CPacketAccessor) p).getUUID());
                saveMetadata();
            }
            if (p instanceof DisconnectS2CPacket){
                return;
            }
            savePacket(p);
        }
    }
}