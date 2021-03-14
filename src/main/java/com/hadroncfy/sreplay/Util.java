package com.hadroncfy.sreplay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hadroncfy.sreplay.recording.Photographer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class Util {
    // Stolen from fabric-carpet
    @FunctionalInterface
    public interface SupplierWithCommandSyntaxException<T> {
        T get() throws IllegalArgumentException;
    }

    @FunctionalInterface
    public interface Replacer<T> {
        T get(T a);
    }

    public static <T> T tryGetArg(SupplierWithCommandSyntaxException<T> a, SupplierWithCommandSyntaxException<T> b) {
        try {
            return a.get();
        }
        catch (IllegalArgumentException e) {
            return b.get();
        }
    }

    public static Text makeBroadcastMsg(String player, String msg){
        return new LiteralText("[" + player + ": " + msg + "]").setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.DARK_GRAY));
    }

    public static Collection<Photographer> getFakes(MinecraftServer server){
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
        if (player != null && player instanceof Photographer){
            return (Photographer) player;
        }
        return null;
    }

    public static String replaceAll(Pattern pattern, String s, Replacer<String> func){
        StringBuilder sb = new StringBuilder();
        int lastIndex = 0;
        Matcher m = pattern.matcher(s);
        while (m.find()){
            if (lastIndex != m.start()){
                sb.append(s.substring(lastIndex, m.start()));
            }
            String name = m.group();
            lastIndex = m.start() + name.length();
            sb.append(func.get(name));
        }
        if (lastIndex < s.length()){
            sb.append(s.substring(lastIndex));
        }
        return sb.toString();
    }
}