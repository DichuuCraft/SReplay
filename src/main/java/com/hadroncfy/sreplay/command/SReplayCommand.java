package com.hadroncfy.sreplay.command;

import com.hadroncfy.sreplay.Main;
import com.hadroncfy.sreplay.config.TextRenderer;
import com.hadroncfy.sreplay.recording.Photographer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.Vec3d;

import static net.minecraft.server.command.CommandManager.literal;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.regex.Matcher;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandSource.suggestMatching;
import static com.hadroncfy.sreplay.Util.tryGetArg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SReplayCommand {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ConfirmationManager cm = new ConfirmationManager(20000);
    private static final Random rand = new Random();

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        final LiteralArgumentBuilder<ServerCommandSource> b = literal("sreplay")
            .then(literal("player").then(argument("player", StringArgumentType.word())
                .suggests((src, sb) -> suggestMatching(Main.listFakes(src.getSource().getMinecraftServer()).stream().map(p -> p.getGameProfile().getName()), sb))
                .then(literal("spawn").executes(SReplayCommand::playerSpawn)
                    .then(argument("fileName", StringArgumentType.word()).executes(SReplayCommand::playerSpawn)))
                .then(literal("kill").executes(SReplayCommand::playerKill))
                .then(literal("respawn").executes(SReplayCommand::playerRespawn)
                    .then(argument("fileName", StringArgumentType.word()).executes(SReplayCommand::playerRespawn)))
                .then(literal("tp").executes(SReplayCommand::playerTp))
                .then(literal("getFileName").executes(SReplayCommand::getPlayerFile))
                .then(buildPlayerParameterCommand())))
            .then(literal("list").executes(SReplayCommand::listRecordings))
            .then(literal("delete").then(argument("recording", StringArgumentType.word())
                .suggests((src, sb) -> suggestMatching(Main.listRecordings().stream().map(f -> f.getName()), sb))
                .executes(SReplayCommand::deleteRecording)))
            .then(literal("confirm")
                .then(argument("code", StringArgumentType.word()).executes(SReplayCommand::confirm)))
            .then(literal("cancel").executes(SReplayCommand::cancel))
            .then(literal("reload").executes(SReplayCommand::reload));
        d.register(b);
    }

    private static int getPlayerFile(CommandContext<ServerCommandSource> ctx){
        Photographer p = requirePlayer(ctx);
        if (p != null){
            ctx.getSource().sendFeedback(TextRenderer.render(Main.getFormats().recordingFile, p.getGameProfile().getName(), p.getRecorder().getOutputPath().getName()), false);
            return 1;
        }
        return 0;
    } 

    private static LiteralArgumentBuilder<ServerCommandSource> buildPlayerParameterCommand(){
        return literal("set")
        .then(literal("sizeLimit")
            // XXX: Velocity doesn't seem to recognize LongArgumentType
            // https://github.com/VelocityPowered/Velocity/issues/295
            .then(argument("sizeLimit", IntegerArgumentType.integer(-1))
                .executes(ctx -> {
                    final Photographer p = requirePlayer(ctx);
                    if (p != null){
                        int i = IntegerArgumentType.getInteger(ctx, "sizeLimit");
                        if (i == -1){
                            p.setSizeLimit(-1);
                        }
                        else if (i <= 10){
                            ctx.getSource().sendFeedback(Main.getFormats().sizeLimitTooSmall, true);
                        }
                        else {
                            p.setSizeLimit(((long)i) << 20);
                        }
                        return 1;
                    }
                    return 0;
                })))
        .then(literal("autoRestart")
            .then(argument("autoRestart", BoolArgumentType.bool())
                .executes(ctx -> {
                    final Photographer p = requirePlayer(ctx);
                    if (p != null){
                        p.setAutoReconnect(BoolArgumentType.getBool(ctx, "autoRestart"));
                        return 1;
                    }
                    return 0;
                })));
    }

    private static Photographer requirePlayer(CommandContext<ServerCommandSource> ctx){
        String name = StringArgumentType.getString(ctx, "player");
        Photographer p = Main.getFake(ctx.getSource().getMinecraftServer(), name);
        if (p != null){
            return p;
        }
        else {
            try {
                ctx.getSource().sendFeedback(TextRenderer.render(Main.getConfig().formats.playerNotFound, name), true);
            }
            catch(Exception e){
                e.printStackTrace();
            }
            return null;
        }
    }

    public static int reload(CommandContext<ServerCommandSource> ctx){
        try {
            Main.loadConfig();
            ctx.getSource().sendFeedback(TextRenderer.render(Main.getFormats().reloadedConfig), false);
            return 1;
        } catch (IOException e) {
            e.printStackTrace();
            ctx.getSource().sendFeedback(TextRenderer.render(Main.getFormats().failedToReloadConfig, e.toString()), false);
            return 0;
        }
    }

    public static int confirm(CommandContext<ServerCommandSource> ctx) {
        final String code = StringArgumentType.getString(ctx, "code");
        if (!cm.confirm(ctx.getSource().getName(), code)) {
            ctx.getSource().sendFeedback(Main.getFormats().nothingToConfirm, false);
        }

        return 0;
    }

    public static int cancel(CommandContext<ServerCommandSource> ctx) {
        if (!cm.cancel(ctx.getSource().getName())) {
            ctx.getSource().sendFeedback(Main.getFormats().nothingToCancel, false);
        }
        return 0;
    }

    public static int listRecordings(CommandContext<ServerCommandSource> ctx) {
        final ServerCommandSource src = ctx.getSource();
        src.sendFeedback(Main.getFormats().recordingFileListHead, true);
        Main.listRecordings().forEach(f -> {
            String size = String.format("%.2f", f.length() / 1024F / 1024F);
            src.sendFeedback(TextRenderer.render(Main.getFormats().recordingFileItem, f.getName(), size), false);
        });
        
        return 0;
    }

    public static int deleteRecording(CommandContext<ServerCommandSource> ctx) {
        final ServerCommandSource src = ctx.getSource();
        final File rec = new File(Main.getConfig().savePath, StringArgumentType.getString(ctx, "recording"));
        final MinecraftServer server = src.getMinecraftServer();
        if (rec.exists()) {
            String code = Integer.toString(rand.nextInt(100));
            src.sendFeedback(TextRenderer.render(Main.getFormats().aboutToDeleteRecording, rec.getName()), true);
            src.sendFeedback(TextRenderer.render(Main.getFormats().confirmingHint, code), false);
            cm.submit(src.getName(), code, (match, cancelled) -> {
                if (!cancelled) {
                    if (match) {
                        rec.delete();
                        server.getPlayerManager()
                            .sendToAll(TextRenderer.render(Main.getFormats().deletedRecordingFile, src.getName(), rec.getName()));
                    } else {
                        src.sendFeedback(Main.getFormats().incorrectConfirmationCode, false);
                    }
                } else {
                    src.sendFeedback(Main.getFormats().operationCancelled, false);
                }
            });
        } else {
            src.sendFeedback(TextRenderer.render(Main.getFormats().fileNotFound, rec.getName()), true);
        }
        return 0;
    }

    public static int playerTp(CommandContext<ServerCommandSource> ctx) {
        final Photographer p = requirePlayer(ctx);
        if (p != null){
            Vec3d pos = ctx.getSource().getPosition();
            p.tp(ctx.getSource().getWorld().getDimension().getType(), pos.x, pos.y, pos.z);
            ctx.getSource().getMinecraftServer().getPlayerManager().broadcastChatMessage(TextRenderer.render(Main.getFormats().teleportedBotToYou, p.getGameProfile().getName(), ctx.getSource().getName()), true);
            LOGGER.info("Teleported " + p.getGameProfile().getName() + " to " + ctx.getSource().getName());
            return 1;
        }

        return 0;
    }

    public static int playerSpawn(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        final String pName = StringArgumentType.getString(ctx, "player");
        final ServerCommandSource src = ctx.getSource();
        final MinecraftServer server = src.getMinecraftServer();

        Matcher m = Main.getConfig().playerNamePattern.matcher(pName);
        if (!m.matches()) {
            src.sendFeedback(Main.getFormats().invalidPlayerName, true);
            return 0;
        }
        if (pName.length() > 16) {
            src.sendFeedback(Main.getFormats().playerNameTooLong, true);
            return 0;
        }
        if (server.getPlayerManager().getPlayer(pName) != null) {
            src.sendFeedback(TextRenderer.render(Main.getFormats().playerIsLoggedIn, pName), true);
            return 0;
        }

        File recordingFile = tryGetArg(() -> Main.getRecordingFile(StringArgumentType.getString(ctx, "fileName")),
                Main::getDefaultRecordingFile);
        if (Main.checkForRecordingDupe(server, recordingFile)) {
            src.sendFeedback(TextRenderer.render(Main.getFormats().recordFileExists, recordingFile.getName()), true);
            return 0;
        }

        try {
            Main.createFake(server, pName, src.getWorld().getDimension().getType(), src.getPosition(), recordingFile);
        } catch (IOException e) {
            src.sendFeedback(TextRenderer.render(Main.getFormats().failedToStartRecording, e.toString()), false);
            e.printStackTrace();
        }
        return 1;
    }

    public static int playerKill(CommandContext<ServerCommandSource> ctx) {
        final Photographer p = requirePlayer(ctx);
        if (p != null){
            p.kill();
            return 1;
        }
        return 0;
    }

    public static int playerRespawn(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        final ServerCommandSource src = ctx.getSource();
        final Photographer p = requirePlayer(ctx);
        if (p != null) {
            File recordingFile = tryGetArg(
                () -> Main.getRecordingFile(StringArgumentType.getString(ctx, "fileName")),
                Main::getDefaultRecordingFile
            );
            if (Main.checkForRecordingDupe(ctx.getSource().getMinecraftServer(), recordingFile)){
                src.sendFeedback(TextRenderer.render(Main.getFormats().recordFileExists, recordingFile.getName()), true);
                return 0;
            }
            p.kill(() -> {
                try {
                    p.connect(recordingFile);
                } catch (Exception e) {
                    src.sendFeedback(TextRenderer.render(Main.getFormats().failedToStartRecording, e.toString()), false);
                    e.printStackTrace();
                }
            }, true);
            return 1;
        }
        return 0;
    }
}