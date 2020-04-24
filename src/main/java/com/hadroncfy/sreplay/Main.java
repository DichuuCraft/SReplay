package com.hadroncfy.sreplay;

import net.fabricmc.api.ModInitializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import com.google.gson.JsonParseException;
import com.hadroncfy.sreplay.config.Config;
import com.hadroncfy.sreplay.config.Formats;
import com.hadroncfy.sreplay.recording.Photographer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main implements ModInitializer {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");

    private static final Logger LOGGER = LogManager.getLogger();
    private static Config config;
    
    public static Photographer createFake(MinecraftServer server, String name, DimensionType dim, Vec3d pos, File recordingFile)
            throws IOException {
        Photographer p = Photographer.create(name, server, dim, pos, config.sizeLimit, config.autoReconnect);
        p.connect(recordingFile);
        // p.tp(dim, pos.x, pos.y, pos.z);
        return p;
    }

    public static void killAllFakes(MinecraftServer server, boolean async) {
        listFakes(server).forEach(p -> p.kill(null, async));
    }

    public static void killAllFakes(PlayerManager pm, boolean async) {
        LOGGER.info("Killing all fakes");
        listFakes(pm).forEach(p -> LOGGER.info("Fake: " + p.getGameProfile().getName()));
        listFakes(pm).forEach(p -> p.kill(null, async));
    }

    public static Collection<Photographer> listFakes(MinecraftServer server){
        return listFakes(server.getPlayerManager());
    }

    public static Collection<Photographer> listFakes(PlayerManager pm){
        Collection<Photographer> ret = new ArrayList<>();
        for (ServerPlayerEntity player: pm.getPlayerList()){
            if (player instanceof Photographer){
                ret.add((Photographer) player);
            }
        }
        return ret;
    }

    public static boolean checkForRecordingDupe(MinecraftServer server, File f){
        if (f.exists()){
            return true;
        }
        for (Photographer p: listFakes(server)){
            if (f.equals(p.getRecorder().getOutputPath())) {
                return true;
            }
        }
        return false;
    }

    public static Photographer getFake(MinecraftServer server, String name){
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(name);
        if (player != null && player instanceof Photographer){
            return (Photographer) player;
        }
        return null;
    }

    public static List<File> listRecordings() {
        return Arrays.asList(new File(config.savePath).listFiles(f -> !f.isDirectory()));
    }

    public static void loadConfig() throws IOException {
        File file = new File("config", "sreplay.json");
        if (file.exists()){
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)){
                config = Config.GSON.fromJson(reader, Config.class);
            }
        }
        else {
            config = new Config();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file))){
                writer.write(Config.GSON.toJson(config));
            }
        }
    }

    public static Config getConfig(){
        return config;
    }

    public static Formats getFormats(){
        return config.formats;
    }

    public static File getDefaultRecordingFile(){
        return new File(config.savePath, sdf.format(Calendar.getInstance().getTime()) + ".mcpr");
    }
    public static File getRecordingFile(String name){
        if (!name.endsWith(".mcpr")){
            name += ".mcpr";
        }
        return new File(config.savePath, name);
    }

    @Override
    public void onInitialize() {
        try {
            Main.loadConfig();
            LOGGER.info("SReplay: Loaded config");
        }
        catch(IOException | JsonParseException e) {
            LOGGER.error("Failed to load config: " + e);
            e.printStackTrace();
            config = new Config();
        }

        File output = new File(config.savePath);
        if (!output.exists()){
            output.mkdirs();
        }
    }
    
}