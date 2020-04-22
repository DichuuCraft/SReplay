package com.hadroncfy.sreplay.command;

import com.hadroncfy.sreplay.Main;
import com.hadroncfy.sreplay.Photographer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import static net.minecraft.server.command.CommandManager.literal;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandSource.suggestMatching;
import static com.hadroncfy.sreplay.Util.tryGetArg;
import static com.hadroncfy.sreplay.Util.makeBroadcastMsg;

public class SReplayCommand {
    private static final ConfirmationManager cm = new ConfirmationManager(20000);
    private static Random rand = new Random();

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        LiteralArgumentBuilder<ServerCommandSource> b = literal("sreplay")
                .then(literal("player").then(argument("player", StringArgumentType.word())
                        .suggests((src, sb) -> suggestMatching(Main.listFakes(), sb))
                        .then(literal("spawn").executes(SReplayCommand::playerSpawn).then(
                                argument("fileName", StringArgumentType.word()).executes(SReplayCommand::playerSpawn)))
                        .then(literal("kill").executes(SReplayCommand::playerKill))
                        .then(literal("respawn").executes(SReplayCommand::playerRespawn)
                                .then(argument("fileName", StringArgumentType.word()).executes(SReplayCommand::playerRespawn)))
                        .then(literal("tp").executes(SReplayCommand::playerTp))))
                .then(literal(
                        "list").executes(SReplayCommand::listRecordings))
                .then(literal("delete").then(argument("recording", StringArgumentType.word())
                        .suggests(
                                (src, sb) -> suggestMatching(Main.listRecordings().stream().map(f -> f.getName()), sb))
                        .executes(SReplayCommand::deleteRecording)))
                .then(literal("confirm")
                        .then(argument("code", StringArgumentType.word()).executes(SReplayCommand::confirm)))
                .then(literal("cancel").executes(SReplayCommand::cancel))
                .then(literal("reload").executes(SReplayCommand::reload));
        d.register(b);
    }

    public static int reload(CommandContext<ServerCommandSource> ctx){
        try {
            Main.loadConfig();
            ctx.getSource().sendFeedback(new LiteralText("Reloaded config"), false);
            return 1;
        } catch (IOException e) {
            e.printStackTrace();
            ctx.getSource().sendFeedback(new LiteralText("Failed to reload config: " + e), false);
            return 0;
        }
    }

    public static int confirm(CommandContext<ServerCommandSource> ctx) {
        String code = StringArgumentType.getString(ctx, "code");
        if (!cm.confirm(ctx.getSource().getName(), code)) {
            ctx.getSource().sendFeedback(new LiteralText("Nothing to confirm"), false);
        }

        return 0;
    }

    public static int cancel(CommandContext<ServerCommandSource> ctx) {
        if (!cm.cancel(ctx.getSource().getName())) {
            ctx.getSource().sendFeedback(new LiteralText("Nothing to cancel"), false);
        }
        return 0;
    }

    public static int listRecordings(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        src.sendFeedback(new LiteralText("Recordings: "), true);
        Main.listRecordings().forEach(f -> {
            String size = String.format("%.2fMB", f.length() / 1024F / 1024F);
            src.sendFeedback(new LiteralText("-    " + f.getName() + "(" + size + ")")
                    .setStyle(new Style().setColor(Formatting.DARK_GREEN).setItalic(true)), false);
        });
        return 0;
    }

    public static int deleteRecording(CommandContext<ServerCommandSource> ctx) {
        final ServerCommandSource src = ctx.getSource();
        final File rec = new File(Main.getConfig().savePath, StringArgumentType.getString(ctx, "recording"));
        final MinecraftServer server = src.getMinecraftServer();
        if (rec.exists()) {
            String code = Integer.toString(rand.nextInt(100));
            src.sendFeedback(new LiteralText("Use /sreplay confirm " + code + " to confirm this operation"), false);
            cm.submit(src.getName(), code, (match, cancelled) -> {
                if (!cancelled) {
                    if (match) {
                        rec.delete();
                        server.getPlayerManager()
                                .sendToAll(makeBroadcastMsg(src.getName(), "Deleted recording file " + rec.getName()));
                    } else {
                        src.sendFeedback(new LiteralText("Incorrect code"), false);
                    }
                } else {
                    src.sendFeedback(new LiteralText("Operation cancelled"), false);
                }
            });
        } else {
            src.sendFeedback(new LiteralText("File " + rec.getName() + " not found"), true);
        }
        return 0;
    }

    public static int playerTp(CommandContext<ServerCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "player");
        Photographer p = Main.getFake(name);
        if (p != null){
            Vec3d pos = ctx.getSource().getPosition();
            p.tp(pos.x, pos.y, pos.z);
            ctx.getSource().sendFeedback(new LiteralText("Player " + name + " was teleported to you"), true);
        }
        else {
            ctx.getSource().sendFeedback(new LiteralText("Player not found"), true);
            return 0;
        }

        return 1;
    }

    public static int playerSpawn(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String pName = StringArgumentType.getString(ctx, "player");
        ServerCommandSource src = ctx.getSource();
        MinecraftServer server = src.getMinecraftServer();
        if (!pName.startsWith("cam_")) {
            src.sendFeedback(new LiteralText("Invalid player name: must begin with cam_"), true);
            return 0;
        }
        if (pName.length() > 16) {
            src.sendFeedback(new LiteralText("Invalid player name: too long"), true);
            return 0;
        }
        if (server.getPlayerManager().getPlayer(pName) != null) {
            src.sendFeedback(new LiteralText("Player " + pName + " is already logged in"), true);
            return 0;
        }

        File recordingFile = tryGetArg(() -> Main.getRecordingFile(StringArgumentType.getString(ctx, "fileName")),
                Main::getDefaultRecordingFile);
        if (recordingFile.exists()) {
            src.sendFeedback(new LiteralText("Recording file " + recordingFile.getName() + " already exists"), true);
            return 0;
        }

        try {
            Main.createFake(server, pName, src.getWorld().getDimension().getType(), src.getPosition(), recordingFile);
        } catch (IOException e) {
            src.sendFeedback(new LiteralText("Failed to start recording: " + e), false);
            e.printStackTrace();
        }
        return 1;
    }

    public static int playerKill(CommandContext<ServerCommandSource> ctx) {
        Photographer p = Main.getFake(StringArgumentType.getString(ctx, "player"));
        ServerCommandSource src = ctx.getSource();
        if (p != null){
            Main.killFake(p);
        }
        else {
            src.sendFeedback(new LiteralText("Player not found"), true);
        }
        return 0;
    }

    public static int playerRespawn(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String pName = StringArgumentType.getString(ctx, "player");
        ServerCommandSource src = ctx.getSource();
        MinecraftServer server = src.getMinecraftServer();
        Photographer p = Main.getFake(pName);
        if (p == null){
            src.sendFeedback(new LiteralText("Player " + pName + " not found"), true);
        }
        else {
            File recordingFile = tryGetArg(
                () -> Main.getRecordingFile(StringArgumentType.getString(ctx, "fileName")),
                Main::getDefaultRecordingFile
            );
            if (recordingFile.exists()){
                src.sendFeedback(new LiteralText("Recording file " + recordingFile.getName() + " already exists"), true);
                return 0;
            }
            try {
                p.kill();
                p.connect(recordingFile);
            } catch (Exception e) {
                src.sendFeedback(new LiteralText("Failed to start recording: " + e), false);
                e.printStackTrace();
            }
        }
        return 0;
    }
}