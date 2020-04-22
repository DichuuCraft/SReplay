package com.hadroncfy.sreplay;

import net.fabricmc.api.ModInitializer;
import net.minecraft.server.MinecraftServer;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main implements ModInitializer {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, Photographer> fakes = new HashMap<>();
    private static Config config;

    public static Photographer getFake(String name) {
        return fakes.get(name);
    }

    public static Photographer createFake(MinecraftServer server, String name, DimensionType dim, Vec3d pos, File recordingFile)
            throws IOException {
        Photographer p = Photographer.create(name, server, dim, config.sizeLimit);
        p.connect(recordingFile);
        p.tp(pos.x, pos.y, pos.z);
        fakes.put(name, p);
        return p;
    }

    public static void killFake(Photographer p) {
        fakes.remove(p.getGameProfile().getName());
        p.kill();
    }

    public static void killAllFakes() {
        fakes.forEach((k, v) -> v.kill());
        fakes.clear();
    }

    public static Collection<String> listFakes() {
        Collection<String> ret = new ArrayList<>();
        fakes.forEach((k, v) -> ret.add(k));
        return ret;
    }

    public static List<File> listRecordings() {
        return Arrays.asList(new File(config.savePath).listFiles(f -> !f.isDirectory()));
    }

    public static void loadConfig() throws IOException {
        Gson gson = new Gson();
        File file = new File("config", "sreplay.json");
        if (file.exists()){
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)){
                config = gson.fromJson(reader, Config.class);
            }
        }
        else {
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file))){
                writer.write(gson.toJson(config));
            }
        }
    }

    public static Config getConfig(){
        return config;
    }

    public static boolean recordingExists(String name){
        return new File(config.savePath, name).exists();
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
            LOGGER.info("Loaded config");
        }
        catch(IOException e) {
            LOGGER.error("Failed to load config: " + e);
        }

        File output = new File(config.savePath);
        if (!output.exists()){
            output.mkdirs();
        }
    }
    
}