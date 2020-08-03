package com.hadroncfy.sreplay.recording.param;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.server.command.ServerCommandSource;

public class ParamManager {
    private final Class<?> paramClass;
    private final Object param;

    public ParamManager(Object param){
        this.param = param;
        paramClass = param.getClass();
    }

    LiteralArgumentBuilder<ServerCommandSource> buildCommand(){
        LiteralArgumentBuilder<ServerCommandSource> ret = null;
        
        return ret;
    }
}