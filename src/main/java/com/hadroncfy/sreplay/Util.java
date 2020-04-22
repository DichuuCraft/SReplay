package com.hadroncfy.sreplay;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class Util {
    // Stolen from fabric-carpet
    @FunctionalInterface
    public interface SupplierWithCommandSyntaxException<T> {
        T get() throws CommandSyntaxException;
    }
    public static <T> T tryGetArg(SupplierWithCommandSyntaxException<T> a, SupplierWithCommandSyntaxException<T> b) throws CommandSyntaxException {
        try {
            return a.get();
        }
        catch (IllegalArgumentException e) {
            return b.get();
        }
    }

    public static Text makeBroadcastMsg(String player, String msg){
        return new LiteralText("[" + player + ": " + msg + "]").setStyle(new Style().setItalic(true).setColor(Formatting.DARK_GRAY));
    }
}