package com.hadroncfy.sreplay.command;

import com.hadroncfy.sreplay.SReplayMod;
import com.hadroncfy.sreplay.config.TextRenderer;
import com.hadroncfy.sreplay.recording.Photographer;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public abstract class RecordParameterExecutor<T> implements Command<ServerCommandSource> {
    private final Class<T> c;
    private final ArgumentType<T> type;
    private final String argName;

    protected abstract int run(CommandContext<ServerCommandSource> context, Photographer p, T val);

    public RecordParameterExecutor(String name, ArgumentType<T> type, Class<T> clazz){
        argName = name;
        this.type = type;
        c = clazz;
    }

    @Override
    public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final Photographer p = SReplayCommand.requirePlayer(context);
        final T val = context.getArgument(argName, c);
        final ServerCommandSource src = context.getSource();
        if (p != null && run(context, p, val) == 0){
            src.getMinecraftServer().getPlayerManager()
                .broadcastChatMessage(TextRenderer.render(SReplayMod.getFormats().setParam, 
                src.getName(), 
                p.getGameProfile().getName(),
                argName,
                val.toString()
            ), true);
        }
        return 0;
    }

    public ArgumentBuilder<ServerCommandSource, ?> build(){
        return literal(argName)
            .then(argument(argName, type).executes(this));
    }
}