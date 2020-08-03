package com.hadroncfy.sreplay.config;

import java.io.File;
import java.net.InetAddress;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.LowercaseEnumTypeAdapterFactory;

public class Config {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .registerTypeHierarchyAdapter(Text.class, new Text.Serializer())
        .registerTypeHierarchyAdapter(Style.class, new Style.Serializer())
        .registerTypeAdapterFactory(new LowercaseEnumTypeAdapterFactory())
        .registerTypeAdapter(Pattern.class, new PatternSerializer())
        .registerTypeAdapter(File.class, new FileSerializer()).create();

    public File savePath = new File("replay_recordings");
    public String serverName = "localhost";
    public InetAddress serverHost = InetAddress.getLoopbackAddress();
    public String serverHostName = "localhost";
    public int serverPort = 12346;
    public long downloadTimeout = 60000;
    public int sizeLimit = -1;
    public int timeLimit = -1;
    public boolean autoReconnect = true;
    public Pattern playerNamePattern = Pattern.compile("^cam_.*$");

    public Formats formats = new Formats();
}