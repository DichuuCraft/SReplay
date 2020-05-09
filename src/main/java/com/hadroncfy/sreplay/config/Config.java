package com.hadroncfy.sreplay.config;

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
        .registerTypeAdapter(Pattern.class, new PatternSerializer()).create();

    public String savePath = "";
    public String serverName = "localhost";
    public long sizeLimit = -1;
    public long timeLimit = -1;
    public boolean autoReconnect = true;
    public Pattern playerNamePattern = Pattern.compile("^cam_.*$");

    public Formats formats = new Formats();
}