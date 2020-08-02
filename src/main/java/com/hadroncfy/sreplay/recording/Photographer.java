package com.hadroncfy.sreplay.recording;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.hadroncfy.sreplay.SReplayMod;
import com.hadroncfy.sreplay.config.TextRenderer;
import com.hadroncfy.sreplay.interfaces.IChunkSender;
import com.hadroncfy.sreplay.mixin.PlayerManagerAccessor;
import com.mojang.authlib.GameProfile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.s2c.play.ChunkLoadDistanceS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Lazy;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.hadroncfy.sreplay.config.TextRenderer.render;

public class Photographer extends ServerPlayerEntity implements ISizeLimitExceededListener {
    public static final String MCPR = ".mcpr";
    private static final String RAW_SUBDIR = "raw";
    private static final GameMode MODE = GameMode.SPECTATOR;
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Lazy<Class<?>> carpetPlayerClass = new Lazy<>(() -> {
        try {
            return Class.forName("carpet.patches.EntityPlayerMPFake");
        } catch (ClassNotFoundException e) {
            return null;
        }
    });
    private final RecordingParam rparam;
    private int reconnectCount = 0;
    private long lastTablistUpdateTime;
    private HackyClientConnection connection;
    private Recorder recorder;
    private final File outputDir;
    private final List<Entity> trackedPlayers = new ArrayList<>();

    private String recordingFileName, saveFileName;

    public Photographer(MinecraftServer server, ServerWorld world, GameProfile profile, ServerPlayerInteractionManager im, File outputDir, RecordingParam param){
        super(server, world, profile, im);
        rparam = param;
        this.outputDir = outputDir;
    }

    public Photographer(MinecraftServer server, ServerWorld world, GameProfile profile, ServerPlayerInteractionManager im, File outputDir){
        this(server, world, profile, im, outputDir, new RecordingParam());
    } 

    public static Photographer create(String name, MinecraftServer server, DimensionType dim, Vec3d pos, File outputDir, RecordingParam param){
        GameProfile profile = new GameProfile(PlayerEntity.getOfflinePlayerUuid(name), name);
        ServerWorld world = server.getWorld(dim);
        ServerPlayerInteractionManager im = new ServerPlayerInteractionManager(world);
        Photographer ret = new Photographer(server, world, profile, im, outputDir, param);
        ret.updatePosition(pos.x, pos.y, pos.z);
        ((PlayerManagerAccessor)server.getPlayerManager()).getSaveHandler().savePlayerData(ret);
        return ret;
    }

    public static Photographer create(String name, MinecraftServer server, DimensionType dim, Vec3d pos, File outputDir){
        return create(name, server, dim, pos, outputDir, RecordingParam.createDefaultRecordingParam(SReplayMod.getConfig(), server.getPlayerManager().getViewDistance()));
    }

    private boolean checkForRecordingFileDupe(String name){
        for (Photographer p: listFakes(getServer())){
            if (p.saveFileName.equals(name) && p.outputDir.equals(outputDir)){
                return true;
            }
        }
        return false;
    }

    private String genRecordingFileName(String name){
        if (!checkForRecordingFileDupe(name)){
            return name;
        }
        else {
            int i = 0;
            while (checkForRecordingFileDupe(name + "_" + i++));
            return name + "_" + i;
        }
    }

    public String getSaveName(){
        return reconnectCount == 0 ? saveFileName : saveFileName + "_" + reconnectCount;
    }
    
    public void setSaveName(String name){
        reconnectCount = 0;
        saveFileName = name;
    }

    public void setWatchDistance(int distance){
        rparam.setWatchDistance(distance);
        recorder.onPacket(new ChunkLoadDistanceS2CPacket(distance));
        int cx = MathHelper.floor(x) >> 4, cz = MathHelper.floor(z) >> 4;
        ServerChunkManager chunkManager = (ServerChunkManager) world.getChunkManager();
        for (int i = -distance; i <= distance; i++){
            for (int j = -distance; j <= distance; j++){
                WorldChunk chunk = chunkManager.method_21730(cx + i, cz + j);
                if (chunk != null){
                    ((IChunkSender)chunkManager.threadedAnvilChunkStorage).sendChunk(this, chunk);
                }
            }
        }
    }

    private void connect() throws IOException{
        recordingFileName = genRecordingFileName(getSaveName());
        
        final File raw = new File(outputDir, RAW_SUBDIR);
        if (!raw.exists()){
            raw.mkdirs();
        }
        recorder = new Recorder(getGameProfile(), server, new File(raw, recordingFileName), rparam);
        connection = new HackyClientConnection(NetworkSide.CLIENTBOUND, recorder);
        
        recorder.setOnSizeLimitExceededListener(this);
        recorder.start();
        lastTablistUpdateTime = System.currentTimeMillis();

        setHealth(20.0F);
        removed = false;
        trackedPlayers.clear();
        server.getPlayerManager().onPlayerConnect(connection, this);
        if (rparam.getWatchDistance() != server.getPlayerManager().getViewDistance()){
            recorder.onPacket(new ChunkLoadDistanceS2CPacket(rparam.getWatchDistance()));
        }
        interactionManager.setGameMode(MODE);
        getServerWorld().getChunkManager().updateCameraPosition(this);
    }

    public void connect(String saveName) throws IOException {
        reconnectCount = 0;
        saveFileName = saveName;
        connect();
    }

    @Override
    public void tick() {
        if (getServer().getTicks() % 10 == 0){
            networkHandler.syncWithPlayerPosition();
            getServerWorld().getChunkManager().updateCameraPosition(this);
        }
        super.tick();
        method_14226();// playerTick
        
        final long now = System.currentTimeMillis();
        if (now - lastTablistUpdateTime >= 1000){
            lastTablistUpdateTime = now;
            if (!recorder.isSoftPaused()){
                server.getPlayerManager().sendToAll(new PlayerListS2CPacket(Action.UPDATE_DISPLAY_NAME, Photographer.this));
            }
        }
    }

    private boolean isRealPlayer(Entity entity){
        if (entity instanceof ServerPlayerEntity){
            Class<?> c = carpetPlayerClass.get();
            return !((entity instanceof Photographer) || (c != null && c.isInstance(entity)));
        }
        return false;
    }

    @Override
    public void onStartedTracking(Entity entity) {
        super.onStartedTracking(entity);
        if (isRealPlayer(entity)){
            trackedPlayers.add(entity);
            updatePause();
        }
    }

    @Override
    public void onStoppedTracking(Entity entity) {
        super.onStoppedTracking(entity);
        if (isRealPlayer(entity)){
            trackedPlayers.remove(entity);
            updatePause();
        }
    }

    public void setAutoPause(boolean b){
        rparam.autoPause = b;
        updatePause();
    }

    private void updatePause(){
        if (recorder != null && rparam.autoPause){
            final String name = getGameProfile().getName();
            if (trackedPlayers.size() == 0 && !recorder.isRecordingPaused()){
                recorder.pauseRecording();
                server.getPlayerManager().broadcastChatMessage(render(SReplayMod.getFormats().autoPaused, name), true);
            }
            if (trackedPlayers.size() > 0 && recorder.isRecordingPaused()){
                recorder.resumeRecording();
                server.getPlayerManager().broadcastChatMessage(render(SReplayMod.getFormats().autoResumed, name), true);
            }
        }
    }

    private static String timeToString(long ms){
        final long sec = ms % 60;
        ms /= 60;
        final long min = ms % 60;
        ms /= 60;
        final long hour = ms;
        if (hour == 0){
            return String.format("%d:%02d", min, sec);
        }
        else {
            return String.format("%d:%02d:%02d", hour, min, sec);
        }
    }

    @Override
    public Text method_14206() {
        if (recorder == null){
            return null;
        }
        long duration = recorder.getRecordedTime() / 1000;

        String time = timeToString(duration);
        if (rparam.timeLimit != -1){
            time += "/" + timeToString(rparam.timeLimit / 1000);
        }

        String size = String.format("%.2f", recorder.getRecordedBytes() / 1024F / 1024F) + "M";
        if (rparam.sizeLimit != -1){
            size += "/" + String.format("%.2f", rparam.sizeLimit / 1024F / 1024F) + "M";
        }
        Text ret = new LiteralText(getGameProfile().getName()).setStyle(new Style().setItalic(true).setColor(Formatting.AQUA));
        if (rparam.getWatchDistance() != server.getPlayerManager().getViewDistance()){
            ret.append(new LiteralText(" (" + rparam.getWatchDistance() + ")").setStyle(new Style().setColor(Formatting.GRAY)));
        }
        if (rparam.autoReconnect){
            ret.append(new LiteralText(" [R]").setStyle(new Style().setItalic(false).setColor(Formatting.DARK_PURPLE)));
        }
        if (rparam.autoPause){
            ret.append(new LiteralText("[P]").setStyle(new Style().setItalic(false).setColor(Formatting.GOLD)));
        }
        ret.append(new LiteralText(" " + time).setStyle(new Style().setItalic(false).setColor(Formatting.GREEN)))
            .append(new LiteralText(" " + size).setStyle(new Style().setItalic(false).setColor(Formatting.GREEN)));
        return ret;
    }

    public void tp(DimensionType dim, double x, double y, double z) {
        if (dimension != dim){
            ServerWorld oldMonde = server.getWorld(dimension), nouveau = server.getWorld(dim);
            oldMonde.removePlayer(this);
            removed = false;
            setWorld(nouveau);
            server.getPlayerManager().sendWorldInfo(this, nouveau);
            interactionManager.setWorld(nouveau);
            networkHandler.sendPacket(new PlayerRespawnS2CPacket(dim, nouveau.getGeneratorType(), MODE));
            nouveau.method_18211(this);
        }
        requestTeleport(x, y, z);
    }

    public Recorder getRecorder(){
        return recorder;
    }

    public void kill(){
        kill(null, true);
    }

    public void kill(Runnable r, boolean async) {
        if (recorder != null){
            final File saveFile = new File(outputDir, getSaveName() + MCPR);
            recorder.stop();
            CompletableFuture<Void> f = recorder.saveRecording(saveFile)
            .thenRun(() -> {
                server.getPlayerManager().broadcastChatMessage(
                    TextRenderer.render(SReplayMod.getFormats().savedRecordingFile, getGameProfile().getName(), saveFile.getName()), false);
            })
            .exceptionally(exception -> {
                exception.printStackTrace();
                server.getPlayerManager().broadcastChatMessage(
                    TextRenderer.render(SReplayMod.getFormats().failedToSaveRecordingFile, getGameProfile().getName(), exception.toString()), false);
                return null;
            });
            recorder = null;
            if (!async){
                f.join();
            }
        }
        final Runnable task =  () -> {
            if (networkHandler != null){
                networkHandler.onDisconnected(new LiteralText("Killed"));
            }
            if (r != null){
                // Whatever went wrong, save the recording first
                try {
                    r.run();
                }
                catch(Throwable e){
                    LOGGER.error("error killing photographer", e);
                    e.printStackTrace();
                }
            }
        };
        if (async){
            server.send(new ServerTask(server.getTicks(), task));
        }
        else {
            task.run();
        }
    }

    @Override
    public boolean method_14239() {
        return false;
    }

    public void onSoftPause() {
        if (recorder != null){
            recorder.setSoftPaused();
        }        
    }

    public void reconnect(){
        kill(() -> {
            reconnectCount++;
            try {
                connect();
            } catch (IOException e) {
                server.getPlayerManager().broadcastChatMessage(TextRenderer.render(SReplayMod.getFormats().failedToStartRecording, getGameProfile().getName()), true);
                e.printStackTrace();
            }
        }, true);
    }

    @Override
    public void onSizeLimitExceeded(long size) {
        if (rparam.autoReconnect){
            reconnect();
        }
        else {
            kill();
        }
    }

    public RecordingParam getRecordingParam(){
        return rparam;
    }

    public static void killAllFakes(MinecraftServer server, boolean async) {
        // LOGGER.info("Killing all fakes");
        listFakes(server).forEach(p -> LOGGER.info("Fake: " + p.getGameProfile().getName()));
        listFakes(server).forEach(p -> p.kill(null, async));
    }

    public static Collection<Photographer> listFakes(MinecraftServer server){
        Collection<Photographer> ret = new ArrayList<>();
        for (ServerPlayerEntity player: server.getPlayerManager().getPlayerList()){
            if (player instanceof Photographer){
                ret.add((Photographer) player);
            }
        }
        return ret;
    }

    public static Photographer getFake(MinecraftServer server, String name){
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(name);
        if (player instanceof Photographer){
            return (Photographer) player;
        }
        return null;
    }

    public static boolean checkForSaveFileDupe(MinecraftServer server, File outputDir, String name){
        if (new File(outputDir, name + MCPR).exists()){
            return true;
        }
        for (Photographer p: listFakes(server)){
            if (p.outputDir.equals(outputDir) && p.getSaveName().equals(name)){
                return true;
            }
        }
        return false;
    }

    public static int getRealViewDistance(ServerPlayerEntity player, int watchDistance){
        if (player instanceof Photographer){
            return ((Photographer)player).getRecordingParam().getWatchDistance();
        }
        else {
            return watchDistance;
        }
    }
}