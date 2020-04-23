package com.hadroncfy.sreplay;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.hadroncfy.sreplay.config.TextRenderer;
import com.hadroncfy.sreplay.mixin.PlayerSpawnS2CPacketAccessor;
import com.mojang.authlib.GameProfile;
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

    private static final Logger LOGGER = LogManager.getLogger();
    private final MinecraftServer server;
    private final GameProfile profile;

    
    private final Set<UUID> uuids = new HashSet<>();
    
    private final File outputPath;
    private final ReplayFile replayFile;
    private final DataOutputStream packetSaveStream;
    private final ReplayMetaData metaData;
    
    private final ExecutorService saveService = Executors.newSingleThreadExecutor();;
    private long startTime;
    private long lastPacket;
    private long timePassedWhilePaused = 0;
    private NetworkState nstate = NetworkState.LOGIN;
    private boolean stopped = false;
    private long bytesRecorded = 0; 
    private boolean paused = false;

    private long sizeLimit;

    private ISizeLimitExceededListener limiter = null;

    public Recorder(GameProfile profile, MinecraftServer server, File outputPath, long sizeLimit) throws IOException {
        this.server = server;
        this.profile = profile;

        this.sizeLimit = sizeLimit;
        this.outputPath = outputPath;

        replayFile = new ZipReplayFile(new ReplayStudio(), outputPath);
        packetSaveStream = new DataOutputStream(replayFile.writePacketData(true));
        metaData = new ReplayMetaData();
    }

    public void setOnSizeLimitExceededListener(ISizeLimitExceededListener l){
        limiter = l;
    }

    public File getOutputPath(){
        return outputPath;
    }

    public void setSizeLimit(long l){
        sizeLimit = l;
    }

    public long getStartTime(){
        return startTime;
    }

    public void start() {
        startTime = System.currentTimeMillis();
        
        metaData.setSingleplayer(false);
        metaData.setServerName(Main.getConfig().serverName);
        metaData.setGenerator("sreplay");
        metaData.setDate(startTime);
        metaData.setMcVersion("1.14.4");
        server.getPlayerManager()
            .broadcastChatMessage(TextRenderer.render(Main.getFormats().startedRecording, profile.getName()), true);

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

    private void savePacket(Packet<?> packet) {
        if (sizeLimit != -1 && bytesRecorded > sizeLimit){
            stop();
            if (limiter != null){
                limiter.onSizeLimitExceeded(bytesRecorded);
            }
            return;
        }
        try {
            byte[] pb = getPacketBytes(packet);
            final long now = System.currentTimeMillis();
            saveService.submit(() -> {
                try {
                    if (paused){
                        timePassedWhilePaused = now - startTime - lastPacket;
                        paused = false;
                    }
                    lastPacket = now - startTime - timePassedWhilePaused;
                    packetSaveStream.writeInt((int) lastPacket);
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

    public void saveRecording() {
        metaData.setDuration((int) lastPacket);
        server.getPlayerManager()
                .broadcastChatMessage(TextRenderer.render(Main.getFormats().savingRecordingFile, profile.getName()), true);
        new Thread(() -> {
            saveService.shutdown();
            try {
                saveService.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                synchronized (replayFile) {
                    String[] players = new String[uuids.size()];
                    uuids.stream().map(uuid -> uuid.toString()).collect(Collectors.toList()).toArray(players);
                    metaData.setPlayers(players);
                    replayFile.writeMetaData(metaData);
                    replayFile.save();
                    replayFile.close();
                    server.getPlayerManager()
                            .broadcastChatMessage(TextRenderer.render(Main.getFormats().savedRecordingFile, profile.getName(), outputPath.getName()), true);
                }
            } catch (IOException e) {
                server.getPlayerManager().broadcastChatMessage(
                        TextRenderer.render(Main.getFormats().failedToSaveRecordingFile, profile.getName(), e.toString()), true);
            }
        }).start(); 
    }

    @Override
    public void onPacket(Packet<?> p) {
        if (!stopped){
            if (p instanceof PlayerSpawnS2CPacket){
                uuids.add(((PlayerSpawnS2CPacketAccessor) p).getUUID());
            }
            if (p instanceof DisconnectS2CPacket){
                return;
            }
            savePacket(p);
        }
    }
}