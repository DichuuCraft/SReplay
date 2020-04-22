package com.hadroncfy.sreplay;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import com.hadroncfy.sreplay.interfaces.IServer;
import com.mojang.authlib.GameProfile;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import net.minecraft.world.dimension.DimensionType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Photographer extends ServerPlayerEntity implements IGamePausedListener {

    private static final Logger LOGGER = LogManager.getLogger();
    private final long sizeLimit;
    private Timer tablistUpdater;
    private HackyClientConnection connection;
    private Recorder recorder;

    // public Photographer(String pName, MinecraftServer server, long sizeLimit) {
    //     this.server = server;
    //     if (((IServer) server).isIntegratedServer()){
    //         ((IServer) server).setOnPauseListener(this);
    //     }
    //     profile = new GameProfile(PlayerEntity.getOfflinePlayerUuid(pName), pName);
    //     this.sizeLimit = sizeLimit;
    // }

    public Photographer(MinecraftServer server, ServerWorld world, GameProfile profile, ServerPlayerInteractionManager im, long sizeLimit){
        super(server, world, profile, im);
        this.sizeLimit = sizeLimit;
        if (((IServer) server).isIntegratedServer()){
            ((IServer) server).setOnPauseListener(this);
        }
    }

    public static Photographer create(String name, MinecraftServer server, DimensionType dim, long sizeLimit){
        GameProfile profile = new GameProfile(PlayerEntity.getOfflinePlayerUuid(name), name);
        ServerWorld world = server.getWorld(dim);
        ServerPlayerInteractionManager im = new ServerPlayerInteractionManager(world);
        return new Photographer(server, world, profile, im, sizeLimit);
    }

    public void connect(File outputPath) throws IOException {
        recorder = new Recorder(getGameProfile(), server, outputPath, sizeLimit);
        connection = new HackyClientConnection(NetworkSide.CLIENTBOUND, recorder);
        
        recorder.start();

        tablistUpdater = new Timer();
        tablistUpdater.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run() {
                server.getPlayerManager().sendToAll(new PlayerListS2CPacket(Action.UPDATE_DISPLAY_NAME, Photographer.this));
            }
        }, 1000, 1000);

        setHealth(20.0F);
        removed = false;
        server.getPlayerManager().onPlayerConnect(connection, this);
        interactionManager.setGameMode(GameMode.SPECTATOR);
        getServerWorld().getChunkManager().updateCameraPosition(this);
    }

    @Override
    public void tick() {
        if (getServer().getTicks() % 10 == 0){
            networkHandler.syncWithPlayerPosition();
            getServerWorld().getChunkManager().updateCameraPosition(this);
        }
        super.tick();
        method_14226();// playerTick
    }

    @Override
    public Text method_14206() {
        long duration = (System.currentTimeMillis() - recorder.getStartTime()) / 1000;
        long sec = duration % 60;
        duration /= 60;
        long min = duration % 60;
        duration /= 60;
        long hour = duration;
        String time;
        if (hour == 0){
            time = String.format("%d:%02d", min, sec);
        }
        else {
            time = String.format("%d:%02d:%02d", hour, min, sec);
        }
        String size = String.format("%.2f", recorder.getRecordedBytes() / 1024F / 1024F) + "MB";
        return new LiteralText(getGameProfile().getName()).setStyle(new Style().setItalic(true).setColor(Formatting.AQUA))
            .append(new LiteralText(" " + time).setStyle(new Style().setItalic(false).setColor(Formatting.GREEN)))
            .append(new LiteralText(" " + size).setStyle(new Style().setItalic(false).setColor(Formatting.GREEN)));
    }

    public void tp(double x, double y, double z) {
        requestTeleport(x, y, z);
    }

    public Recorder getRecorder(){
        return recorder;
    }

    public void kill() {
        recorder.stop();
        tablistUpdater.cancel();
        tablistUpdater = null;

        networkHandler.onDisconnected(new LiteralText("Killed"));

        recorder.saveRecording();
    }

    @Override
    public boolean method_14239() {
        return false;
    }

    @Override
    public void onPause() {
        if (recorder != null){
            recorder.setPaused();
        }        
    }
}