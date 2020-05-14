package com.hadroncfy.sreplay.command;

import com.hadroncfy.sreplay.SReplayMod;
import com.hadroncfy.sreplay.config.TextRenderer;
import com.hadroncfy.sreplay.recording.Photographer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.Vec3d;

import static net.minecraft.server.command.CommandManager.literal;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandSource.suggestMatching;
import static com.hadroncfy.sreplay.recording.Photographer.MCPR;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelFuture;

public class SReplayCommand {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");

    private final ConfirmationManager cm = new ConfirmationManager(20000);
    private final Random rand = new Random();

    public SReplayCommand(){
    }

    public void register(CommandDispatcher<ServerCommandSource> d) {
        final LiteralArgumentBuilder<ServerCommandSource> b = literal("sreplay")
            .then(literal("player").then(argument("player", StringArgumentType.word())
                .suggests((src, sb) -> suggestMatching(Photographer.listFakes(src.getSource().getMinecraftServer()).stream().map(p -> p.getGameProfile().getName()), sb))
                .then(literal("spawn").executes(this::playerSpawn))
                .then(literal("kill").executes(this::playerKill))
                .then(literal("respawn").executes(this::playerRespawn))
                .then(literal("tp").executes(this::playerTp))
                .then(literal("name").executes(this::getName)
                    .then(argument("fileName", StringArgumentType.word()).executes(this::setName)))
                .then(buildPlayerParameterCommand())
                .then(literal("pause").executes(this::pause))
                .then(literal("resume").executes(this::resume))
                .then(literal("marker").then(argument("marker", StringArgumentType.word()).executes(this::marker)))))
            .then(literal("list").executes(this::listRecordings))
            .then(literal("delete").then(argument("recording", StringArgumentType.word())
                .suggests(this::suggestRecordingFile)
                .executes(this::deleteRecording)))
            .then(literal("confirm")
                .then(argument("code", StringArgumentType.word()).executes(this::confirm)))
            .then(literal("cancel").executes(this::cancel))
            .then(literal("reload").executes(this::reload))
            .then(literal("server")
                .then(literal("start").executes(this::startServer))
                .then(literal("stop").executes(this::stopServer)))
            .then(literal("get")
                .then(argument("fileName", StringArgumentType.word())
                .suggests(this::suggestRecordingFile)
                .executes(this::getFile)));
        d.register(b);
    }

    private CompletableFuture<Suggestions> suggestRecordingFile(CommandContext<ServerCommandSource> src, SuggestionsBuilder sb){
        return suggestMatching(SReplayMod.listRecordings().stream().map(f -> f.getName()), sb);
    }

    private int getFile(CommandContext<ServerCommandSource> ctx){
        final String fName = StringArgumentType.getString(ctx, "fileName");
        final File f = new File(SReplayMod.getConfig().savePath, fName);
        if (f.exists()){
            final String path = SReplayMod.getServer().addFile(f, 30000);
            String url = "http://" + SReplayMod.getConfig().serverHostName;
            final int port = SReplayMod.getConfig().serverPort;
            if (port != 80){
                url += ":" + port;
            }
            url += path;
            ctx.getSource().sendFeedback(TextRenderer.render(SReplayMod.getFormats().downloadUrl, url), false);
        }
        else {
            ctx.getSource().sendError(TextRenderer.render(SReplayMod.getFormats().fileNotFound, fName));
            return 1;
        }
        return 0;
    }

    private int startServer(CommandContext<ServerCommandSource> ctx){
        final ServerCommandSource src = ctx.getSource();
        final MinecraftServer server = src.getMinecraftServer();
        final ChannelFuture ch = SReplayMod.getServer().bind(SReplayMod.getConfig().serverHost, SReplayMod.getConfig().serverPort);
        ch.addListener(future -> {
            if (future.isSuccess()){
                server.getPlayerManager().broadcastChatMessage(SReplayMod.getFormats().serverStarted, true);
            }
            else {
                src.sendError(TextRenderer.render(SReplayMod.getFormats().serverStartFailed, future.cause().getMessage()));
            }
        });
        return 0;
    }

    private int stopServer(CommandContext<ServerCommandSource> ctx){
        final ServerCommandSource src = ctx.getSource();
        final MinecraftServer server = src.getMinecraftServer();
        final ChannelFuture ch = SReplayMod.getServer().stop();
        ch.addListener(future -> {
            if (future.isSuccess()){
                server.getPlayerManager().broadcastChatMessage(SReplayMod.getFormats().serverStopped, true);
            }
            else {
                src.sendError(TextRenderer.render(SReplayMod.getFormats().serverStopFailed, future.cause().getMessage()));
            }
        });
        return 0;
    }

    private int getName(CommandContext<ServerCommandSource> ctx){
        Photographer p = requirePlayer(ctx);
        if (p != null){
            ctx.getSource().sendFeedback(TextRenderer.render(SReplayMod.getFormats().recordingFile, p.getGameProfile().getName(), p.getSaveName()), false);
            return 1;
        }
        return 0;
    }

    private int setName(CommandContext<ServerCommandSource> ctx){
        Photographer p = requirePlayer(ctx);
        if (p != null){
            String name = StringArgumentType.getString(ctx, "fileName");
            if (name.endsWith(MCPR)){
                name = name.substring(0, name.length() - MCPR.length());
            }
            if (Photographer.checkForSaveFileDupe(ctx.getSource().getMinecraftServer(), SReplayMod.getConfig().savePath, name)){
                ctx.getSource().sendError(TextRenderer.render(SReplayMod.getFormats().recordFileExists, name));
                return 0;
            }
            p.setSaveName(name);
            return 1;
        }
        return 0;
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildPlayerParameterCommand(){
        return literal("set")
        .then(literal("sizeLimit")
            .then(argument("sizeLimit", IntegerArgumentType.integer(-1))
                .executes(ctx -> {
                    final Photographer p = requirePlayer(ctx);
                    if (p != null){
                        int i = IntegerArgumentType.getInteger(ctx, "sizeLimit");
                        if (i == -1){
                            p.getRecordingParam().sizeLimit = -1;
                        }
                        else if (i < 10){
                            ctx.getSource().sendFeedback(SReplayMod.getFormats().sizeLimitTooSmall, true);
                        }
                        else {
                            p.getRecordingParam().sizeLimit = ((long)i) << 20;
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
                        p.getRecordingParam().autoReconnect = BoolArgumentType.getBool(ctx, "autoRestart");
                        return 1;
                    }
                    return 0;
                })))
        .then(literal("timeLimit")
            .then(argument("timeLimit", IntegerArgumentType.integer(-1))
                .executes(ctx -> {
                    final Photographer p = requirePlayer(ctx);
                    if (p != null){
                        int i = IntegerArgumentType.getInteger(ctx, "timeLimit");
                        if (i == -1){
                            p.getRecordingParam().timeLimit = -1;
                        }
                        else if (i < 10){
                            ctx.getSource().sendFeedback(SReplayMod.getFormats().timeLimitTooSmall, true);
                        }
                        else {
                            p.getRecordingParam().timeLimit = i * 1000;
                        }
                    }
                    return 0;
                })));
    }

    private static Photographer requirePlayer(CommandContext<ServerCommandSource> ctx){
        String name = StringArgumentType.getString(ctx, "player");
        Photographer p = SReplayMod.getFake(ctx.getSource().getMinecraftServer(), name);
        if (p != null){
            return p;
        }
        else {
            try {
                ctx.getSource().sendFeedback(TextRenderer.render(SReplayMod.getConfig().formats.playerNotFound, name), true);
            }
            catch(Exception e){
                e.printStackTrace();
            }
            return null;
        }
    }

    public int marker(CommandContext<ServerCommandSource> ctx){
        Photographer p = requirePlayer(ctx);
        if (p != null){
            String name = StringArgumentType.getString(ctx, "marker");
            p.getRecorder().addMarker(name);
            ctx.getSource().getMinecraftServer().getPlayerManager().broadcastChatMessage(TextRenderer.render(SReplayMod.getFormats().markerAdded, p.getGameProfile().getName(), name), false);
            return 1;
        }
        else {
            return 0;
        }
    }

    public int pause(CommandContext<ServerCommandSource> ctx){
        Photographer p = requirePlayer(ctx);
        if (p != null){
            p.getRecorder().pauseRecording();
            ctx.getSource().getMinecraftServer().getPlayerManager().broadcastChatMessage(TextRenderer.render(SReplayMod.getFormats().recordingPaused, p.getGameProfile().getName()), false);
            return 1;
        }
        else {
            return 0;
        }
    }

    public int resume(CommandContext<ServerCommandSource> ctx){
        Photographer p = requirePlayer(ctx);
        if (p != null){
            p.getRecorder().resumeRecording();
            ctx.getSource().getMinecraftServer().getPlayerManager().broadcastChatMessage(TextRenderer.render(SReplayMod.getFormats().recordingResumed, p.getGameProfile().getName()), false);
            return 1;
        }
        else {
            return 0;
        }
    }

    public int reload(CommandContext<ServerCommandSource> ctx){
        try {
            SReplayMod.loadConfig();
            ctx.getSource().sendFeedback(TextRenderer.render(SReplayMod.getFormats().reloadedConfig), false);
            return 1;
        } catch (IOException e) {
            e.printStackTrace();
            ctx.getSource().sendFeedback(TextRenderer.render(SReplayMod.getFormats().failedToReloadConfig, e.toString()), false);
            return 0;
        }
    }

    public int confirm(CommandContext<ServerCommandSource> ctx) {
        final String code = StringArgumentType.getString(ctx, "code");
        if (!cm.confirm(ctx.getSource().getName(), code)) {
            ctx.getSource().sendFeedback(SReplayMod.getFormats().nothingToConfirm, false);
        }

        return 0;
    }

    public int cancel(CommandContext<ServerCommandSource> ctx) {
        if (!cm.cancel(ctx.getSource().getName())) {
            ctx.getSource().sendFeedback(SReplayMod.getFormats().nothingToCancel, false);
        }
        return 0;
    }

    public int listRecordings(CommandContext<ServerCommandSource> ctx) {
        final ServerCommandSource src = ctx.getSource();
        src.sendFeedback(SReplayMod.getFormats().recordingFileListHead, true);
        SReplayMod.listRecordings().forEach(f -> {
            String size = String.format("%.2f", f.length() / 1024F / 1024F);
            src.sendFeedback(TextRenderer.render(SReplayMod.getFormats().recordingFileItem, f.getName(), size), false);
        });
        
        return 0;
    }

    public int deleteRecording(CommandContext<ServerCommandSource> ctx) {
        final ServerCommandSource src = ctx.getSource();
        final File rec = new File(SReplayMod.getConfig().savePath, StringArgumentType.getString(ctx, "recording"));
        final MinecraftServer server = src.getMinecraftServer();
        if (rec.exists()) {
            String code = Integer.toString(rand.nextInt(100));
            src.sendFeedback(TextRenderer.render(SReplayMod.getFormats().aboutToDeleteRecording, rec.getName()), true);
            src.sendFeedback(TextRenderer.render(SReplayMod.getFormats().confirmingHint, code), false);
            cm.submit(src.getName(), code, (match, cancelled) -> {
                if (!cancelled) {
                    if (match) {
                        rec.delete();
                        server.getPlayerManager()
                            .sendToAll(TextRenderer.render(SReplayMod.getFormats().deletedRecordingFile, src.getName(), rec.getName()));
                    } else {
                        src.sendFeedback(SReplayMod.getFormats().incorrectConfirmationCode, false);
                    }
                } else {
                    src.sendFeedback(SReplayMod.getFormats().operationCancelled, false);
                }
            });
        } else {
            src.sendFeedback(TextRenderer.render(SReplayMod.getFormats().fileNotFound, rec.getName()), true);
        }
        return 0;
    }

    public int playerTp(CommandContext<ServerCommandSource> ctx) {
        final Photographer p = requirePlayer(ctx);
        if (p != null){
            Vec3d pos = ctx.getSource().getPosition();
            p.tp(ctx.getSource().getWorld().getDimension().getType(), pos.x, pos.y, pos.z);
            ctx.getSource().getMinecraftServer().getPlayerManager().broadcastChatMessage(TextRenderer.render(SReplayMod.getFormats().teleportedBotToYou, p.getGameProfile().getName(), ctx.getSource().getName()), true);
            LOGGER.info("Teleported " + p.getGameProfile().getName() + " to " + ctx.getSource().getName());
            return 1;
        }

        return 0;
    }

    public int playerSpawn(CommandContext<ServerCommandSource> ctx) {
        final String pName = StringArgumentType.getString(ctx, "player");
        final ServerCommandSource src = ctx.getSource();
        final MinecraftServer server = src.getMinecraftServer();

        Matcher m = SReplayMod.getConfig().playerNamePattern.matcher(pName);
        if (!m.matches()) {
            src.sendFeedback(SReplayMod.getFormats().invalidPlayerName, true);
            return 0;
        }
        if (pName.length() > 16) {
            src.sendFeedback(SReplayMod.getFormats().playerNameTooLong, true);
            return 0;
        }
        if (server.getPlayerManager().getPlayer(pName) != null) {
            src.sendFeedback(TextRenderer.render(SReplayMod.getFormats().playerIsLoggedIn, pName), true);
            return 0;
        }

        String saveName = sdf.format(new Date());
        int i = 0;
        // Although this never happen in normal situations, we'll take care of it anyway
        if (Photographer.checkForSaveFileDupe(server, SReplayMod.getConfig().savePath, saveName)){
            while (Photographer.checkForSaveFileDupe(server, SReplayMod.getConfig().savePath, saveName + "_" + i++));
            saveName = saveName + "_" + i++;
        }

        try {
            Photographer photographer = Photographer.create(pName, server, src.getWorld().getDimension().getType(), src.getPosition(), SReplayMod.getConfig().savePath);
            photographer.connect(saveName);
        } catch (IOException e) {
            src.sendFeedback(TextRenderer.render(SReplayMod.getFormats().failedToStartRecording, e.toString()), false);
            e.printStackTrace();
        }
        return 1;
    }

    public int playerKill(CommandContext<ServerCommandSource> ctx) {
        final Photographer p = requirePlayer(ctx);
        if (p != null){
            p.kill();
            return 1;
        }
        return 0;
    }

    public int playerRespawn(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        final ServerCommandSource src = ctx.getSource();
        final Photographer p = requirePlayer(ctx);
        if (p != null) {
            p.reconnect();
            return 1;
        }
        return 0;
    }
}