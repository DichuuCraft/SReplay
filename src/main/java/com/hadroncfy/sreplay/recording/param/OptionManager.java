package com.hadroncfy.sreplay.recording.param;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hadroncfy.sreplay.Lang;
import com.hadroncfy.sreplay.command.SReplayCommand;
import com.hadroncfy.sreplay.recording.Photographer;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandSource.suggestMatching;

import static com.hadroncfy.sreplay.SReplayMod.getFormats;

import static com.hadroncfy.sreplay.config.TextRenderer.render;

public class OptionManager {
    private final Class<?> paramClass;
    private final Map<String, OptionEntry<?>> params = new HashMap<>();

    public OptionManager(Class<?> paramClass) {
        this.paramClass = paramClass;
        for (Field f : paramClass.getDeclaredFields()) {
            OptionEntry<?> entry = new OptionEntry<>(f);
            params.put(entry.name, entry);
        }
    }

    public LiteralArgumentBuilder<ServerCommandSource> buildCommand() {
        LiteralArgumentBuilder<ServerCommandSource> ret = literal("set");
        for (OptionEntry<?> entry : params.values()) {
            String name = entry.name;
            Executor cmd = new Executor(entry);
            ret.then(literal(name).executes(cmd).then(getArgument(entry).executes(cmd)));
        }
        return ret;
    }

    public LiteralArgumentBuilder<ServerCommandSource> buildHelpCommand(){
        LiteralArgumentBuilder<ServerCommandSource> ret = literal("set")
            .then(argument("param", StringArgumentType.word())
                .suggests((src, sb) -> suggestMatching(params.keySet(), sb))
                .executes(ctx -> {
                    ServerCommandSource src = ctx.getSource();
                    String name = StringArgumentType.getString(ctx, "param");
                    OptionEntry<?> entry = params.get(name);
                    if (entry != null){
                        src.sendFeedback(render(getFormats().paramHelp, 
                            name, 
                            Lang.getString(entry.desc)
                        ), false);
                    }
                    else {
                        src.sendError(getFormats().noSuchParam);
                    }
                    return 0;
                }));

        return ret;
    }

    @SuppressWarnings({"rawtypes"})
    private static RequiredArgumentBuilder<ServerCommandSource, ?> getArgument(OptionEntry<?> entry){
        Class<?> type = entry.type;
        String name = entry.name;
        if (type.equals(boolean.class)) {
            return argument(name, BoolArgumentType.bool());
        } else if (type.equals(int.class)) {
            return argument(name, IntegerArgumentType.integer());
        } else if (type.isEnum()){
            List<String> names = new ArrayList<>();
            for (Object k: type.getEnumConstants()){
                names.add(((Enum)k).name().toLowerCase());
            }
            return argument(name, StringArgumentType.word()).suggests((c, b) -> suggestMatching(names, b));
        }
        return argument(name, StringArgumentType.word());
    }

    private class Executor implements Command<ServerCommandSource> {
        private final OptionEntry<?> entry;

        Executor(OptionEntry<?> entry) {
            this.entry = entry;
        }

        @Override
        public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
            ServerCommandSource src = context.getSource();
            MinecraftServer server = src.getMinecraftServer();
            String pname = StringArgumentType.getString(context, "player");
            Photographer player = Photographer.getFake(server, pname);
            if (player != null) {
                try {
                    try {
                        if(entry.set(context, player.getRecordingParam())) {
                            player.syncParams();
                            server.getPlayerManager().broadcastChatMessage(render(getFormats().setParam,
                                src.getName(),
                                player.getGameProfile().getName(),
                                entry.name,
                                entry.field.get(player.getRecordingParam()).toString()
                            ), MessageType.GAME_INFO, SReplayCommand.getSenderUUID(context));
                        }
                    }
                    catch(InvalidEnumException e){
                        src.sendError(getFormats().invalidEnum);
                    }
                    catch(IllegalArgumentException e){
                        src.sendFeedback(render(getFormats().getParam,
                            player.getGameProfile().getName(),
                            entry.name,
                            entry.field.get(player.getRecordingParam()).toString()
                        ), false);
                    }
                } catch(IllegalAccessException e){
                    e.printStackTrace();
                }
            }
            else {
                src.sendError(getFormats().playerNotFound);
            }
            return 0;
        }
    }
}