package com.hadroncfy.sreplay.recording;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.hadroncfy.sreplay.SReplayMod;
import com.hadroncfy.sreplay.command.SReplayCommand;
import com.hadroncfy.sreplay.config.TextRenderer;
import com.hadroncfy.sreplay.mixin.GameStateChangeS2CPacketAccessor;
import com.hadroncfy.sreplay.mixin.PlayerSpawnS2CPacketAccessor;
import com.hadroncfy.sreplay.mixin.WorldTimeUpdateS2CPacketAccessor;
import com.hadroncfy.sreplay.recording.mcpr.IReplayFile;
import com.hadroncfy.sreplay.recording.mcpr.Marker;
import com.hadroncfy.sreplay.recording.mcpr.Metadata;
import com.hadroncfy.sreplay.recording.mcpr.ProgressBar;
import com.hadroncfy.sreplay.recording.mcpr.SReplayFile;
import com.mojang.authlib.GameProfile;

import net.minecraft.SharedConstants;
import net.minecraft.entity.EntityType;
import net.minecraft.network.MessageType;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.login.LoginSuccessS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerSpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.server.MinecraftServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Recorder implements IPacketListener {
    private static final String MARKER_PAUSE = "PAUSE";
    private static final Logger LOGGER = LogManager.getLogger();
    private final MinecraftServer server;
    private final GameProfile profile;
    private final WeatherView wv;
    
    private final IReplayFile replayFile;
    private final Metadata metaData;
    private final RecordingOption param;
    
    private final ExecutorService saveService = Executors.newSingleThreadExecutor();
    private long startTime;
    private int startTick;
    private long lastPacket;
    private long timeShift = 0;
    private NetworkState nstate = NetworkState.LOGIN;
    private boolean stopped = false;
    private boolean isSaving = false;
    private boolean hasSaved0 = false;

    private boolean paused = false;
    private boolean resumeOnNextPacket = true;
    private boolean followTick = false;
    private final List<Marker> markers = new ArrayList<>();

    private ISizeLimitExceededListener limiter = null;

    public Recorder(GameProfile profile, MinecraftServer server, WeatherView wv, File outputPath, RecordingOption param) throws IOException {
        this.server = server;
        this.profile = profile;
        this.wv = wv;

        this.param = param;

        replayFile = new SReplayFile(outputPath);
        metaData = new Metadata();
    }

    public void setOnSizeLimitExceededListener(ISizeLimitExceededListener l){
        limiter = l;
    }

    public long getStartTime(){
        return startTime;
    }

    public void start() {
        startTime = System.currentTimeMillis();
        startTick = server.getTicks();
        
        metaData.singleplayer = false;
        metaData.serverName = SReplayMod.getConfig().serverName;
        metaData.generator = "sreplay";
        metaData.date = startTime;
        metaData.mcversion = SharedConstants.getGameVersion().getName();
        server.getPlayerManager()
            .broadcastChatMessage(TextRenderer.render(SReplayMod.getFormats().startedRecording, profile.getName()), MessageType.CHAT, new UUID(0, 0));

        // Must contain this packet, otherwise ReplayMod would complain
        savePacket(new LoginSuccessS2CPacket(profile));
        nstate = NetworkState.PLAY;
    }

    public void setSoftPaused(){
        paused = true;
    }

    // Should only increase
    public long getRecordedTime(){
        final long base;
        if (followTick){
            base = (server.getTicks() - startTick) * 50L;
        }
        else {
            base = System.currentTimeMillis() - startTime;
        }
        return base - timeShift;
    }

    private synchronized long getCurrentTimeAndUpdate(){
        long now = getRecordedTime();
        if (paused){
            if (resumeOnNextPacket){
                paused = false;
            }
            timeShift += now - lastPacket;
            return lastPacket;
        }
        return lastPacket = now;
    }

    public void pauseRecording(){
        resumeOnNextPacket = false;
        paused = true;
        if (param.pauseMarkers){
            addMarker(MARKER_PAUSE);
        }
    }

    public boolean isRecordingPaused(){
        return !resumeOnNextPacket;
    }

    public boolean isSoftPaused(){
        return resumeOnNextPacket && paused;
    }

    public void resumeRecording(){
        resumeOnNextPacket = true;
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
        long bytesRecorded = replayFile.getRecordedBytes();
        if (param.sizeLimit != -1 && bytesRecorded > ((long)param.sizeLimit) << 20 || param.timeLimit != -1 && getRecordedTime() > (long)param.timeLimit * 1000){
            stop();
            if (limiter != null){
                limiter.onSizeLimitExceeded(bytesRecorded);
            }
            return;
        }
        try {
            final long timestamp = getCurrentTimeAndUpdate();
            final boolean login = nstate == NetworkState.LOGIN;
            saveService.submit(() -> {
                try {
                    replayFile.savePacket(timestamp, packet, login);
                } catch (Exception e) {
                    LOGGER.error("Error saving packet", e);
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            LOGGER.error("Error saving packet", e);
            e.printStackTrace();
        }
    }

    public void stop(){
        stopped = true;
    }

    public boolean isStopped(){
        return stopped;
    }

    public long getRecordedBytes(){
        return replayFile.getRecordedBytes();
    }

    public void addMarker(String name){
        Marker m = new Marker(name, (int) getRecordedTime());
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
            try {
                replayFile.saveMetaData(metaData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void saveMarkers(){
        if (!markers.isEmpty()){
            saveService.submit(() -> {
                try {
                    replayFile.saveMarkers(markers);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public boolean hasSaved(){
        return hasSaved0;
    }

    public CompletableFuture<Void> saveRecording(File dest, ProgressBar bar) {
        hasSaved0 = true;
        if (!isSaving){
            isSaving = true;
            metaData.duration = (int) lastPacket;
            server.getPlayerManager().broadcastChatMessage(TextRenderer.render(SReplayMod.getFormats().savingRecordingFile, profile.getName()), MessageType.CHAT, new UUID(0, 0));
            return CompletableFuture.runAsync(() -> {
                saveMetadata();
                saveMarkers();
                saveService.shutdown();
                boolean interrupted = false;
                try {
                    saveService.awaitTermination(10, TimeUnit.SECONDS);
                }
                catch (InterruptedException e) {
                    interrupted = true;
                }
                try {
                    replayFile.closeAndSave(dest, bar);
                }
                catch(IOException e){
                    e.printStackTrace();
                    throw new CompletionException(e);
                }
                finally {
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, r -> {
                final Thread t = new Thread(r, "Recording file save thread");
                t.start();
            });
        }
        else {
            LOGGER.warn("saveRecording() called twice");
            return CompletableFuture.supplyAsync(() -> {
                throw new IllegalStateException("saveRecording() called twice");
            });
        }
    }

    private void setWeather(ForcedWeather weather){
        switch(weather){
            case RAIN:
                savePacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_STARTED, 0));
                savePacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, 0));
                break;
            case CLEAR:
                savePacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_STOPPED, 0));
                savePacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, 0));
                break;
            case THUNDER:
                savePacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_STARTED, 0));
                savePacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, 1));
                break;
            default:
                break;
        }
    }

    public void syncParam(){
        switch(param.forcedWeather){
            case RAIN:
            case CLEAR:
            case THUNDER:
                setWeather(param.forcedWeather);
                break;
            case NONE:
                setWeather(wv.getWeather());
                break;
        }
    }

    @Override
    public void onPacket(Packet<?> p) {
        if (!stopped){
            if (p instanceof PlayerSpawnS2CPacket){
                metaData.players.add(((PlayerSpawnS2CPacketAccessor) p).getUUID());
                saveMetadata();
            }
            if (p instanceof DisconnectS2CPacket){
                return;
            }
            if (param.dayTime != -1 && p instanceof WorldTimeUpdateS2CPacket){
                final WorldTimeUpdateS2CPacketAccessor p2 = (WorldTimeUpdateS2CPacketAccessor)p;
                p = new WorldTimeUpdateS2CPacket(p2.getTime(), param.dayTime, false);
            }
            if (param.forcedWeather != ForcedWeather.NONE && p instanceof GameStateChangeS2CPacket){
                GameStateChangeS2CPacket.Reason r = ((GameStateChangeS2CPacketAccessor)p).getReason();
                if (
                    r == GameStateChangeS2CPacket.RAIN_STARTED
                    || r == GameStateChangeS2CPacket.RAIN_STOPPED
                    || r == GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED
                    || r == GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED
                ) return;
            }
            if (param.ignoreChat && p instanceof GameMessageS2CPacket){
                return;
            }
            savePacket(p);
        }
    }
}