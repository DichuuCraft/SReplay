package com.hadroncfy.sreplay;

import net.fabricmc.api.ModInitializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.hadroncfy.sreplay.config.Config;
import com.hadroncfy.sreplay.config.Formats;
import com.hadroncfy.sreplay.recording.Photographer;
import com.hadroncfy.sreplay.server.Server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SReplayMod implements ModInitializer {

    private static final Logger LOGGER = LogManager.getLogger();
    private static Config config;
    private static final Server downloadServer = new Server();

    public static Photographer getFake(MinecraftServer server, String name) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(name);
        if (player instanceof Photographer) {
            return (Photographer) player;
        }
        return null;
    }

    public static Server getServer() {
        return downloadServer;
    }

    public static List<File> listRecordings() {
        List<File> files = Arrays.asList(config.savePath.listFiles(f -> !f.isDirectory()));
        Collections.sort(files);
        return files;
    }

    public static void loadConfig() throws IOException {
        File dir = new File("config");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, "sreplay.json");
        if (file.exists()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                config = Config.GSON.fromJson(reader, Config.class);
            }
        } else {
            config = new Config();
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(Config.GSON.toJson(config));
        }
    }

    public static Config getConfig() {
        return config;
    }

    public static Formats getFormats() {
        return config.formats;
    }

    @Override
    public void onInitialize() {

        try {
            SReplayMod.loadConfig();
            Lang.load("zh_cn");
            LOGGER.info("SReplay: Initialzed");
        } catch (Exception e) {
            LOGGER.error("Exception initializing mod: {}", e);
            e.printStackTrace();
        }

        if (config == null) {
            config = new Config();
        }

        if (!config.savePath.exists()) {
            config.savePath.mkdirs();
        }
    }
    
}