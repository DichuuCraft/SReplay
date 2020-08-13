package com.hadroncfy.sreplay.command;

import com.google.gson.JsonParseException;
import com.hadroncfy.sreplay.SReplayMod;
import com.hadroncfy.sreplay.recording.Photographer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import static net.minecraft.server.command.CommandManager.literal;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandSource.suggestMatching;
import static com.hadroncfy.sreplay.recording.Photographer.MCPR;
import static com.hadroncfy.sreplay.config.TextRenderer.render;
import static com.hadroncfy.sreplay.SReplayMod.getConfig;
import static com.hadroncfy.sreplay.SReplayMod.getFormats;
import static com.hadroncfy.sreplay.Util.tryGetArg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelFuture;

public class SReplayCommand {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");

    private static final ConfirmationManager cm = new ConfirmationManager(20000, 999);

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        final LiteralArgumentBuilder<ServerCommandSource> b = literal("sreplay")
            .then(literal("player").then(argument("player", StringArgumentType.word())
                .suggests((src, sb) -> suggestMatching(Photographer.listFakes(src.getSource().getMinecraftServer()).stream().map(p -> p.getGameProfile().getName()), sb))
                .then(literal("spawn").executes(SReplayCommand::playerSpawn))
                .then(literal("kill").executes(SReplayCommand::playerKill))
                .then(literal("respawn").executes(SReplayCommand::playerRespawn))
                .then(literal("tp").executes(SReplayCommand::playerTp))
                .then(literal("name").executes(SReplayCommand::getName)
                    .then(argument("fileName", StringArgumentType.greedyString()).executes(SReplayCommand::setName)))
                .then(Photographer.PARAM_MANAGER.buildCommand())
                .then(literal("pause").executes(SReplayCommand::pause))
                .then(literal("resume").executes(SReplayCommand::resume))
                .then(literal("locate").executes(SReplayCommand::locate))
                .then(literal("marker")
                    .then(literal("list").executes(SReplayCommand::getMarkers)
                        .then(argument("page", IntegerArgumentType.integer(1)).executes(SReplayCommand::getMarkers))
                    .then(literal("add").then(argument("marker", StringArgumentType.greedyString()).executes(SReplayCommand::marker)))
                    .then(literal("remove").then(argument("markerId", IntegerArgumentType.integer(1)).executes(SReplayCommand::removeMarker)))))))
            .then(literal("list").executes(SReplayCommand::listRecordings)
                .then(argument("page", IntegerArgumentType.integer(1)).executes(SReplayCommand::listRecordings)))
            .then(literal("delete").then(argument("recording", StringArgumentType.greedyString())
                .suggests(SReplayCommand::suggestRecordingFile)
                .executes(SReplayCommand::deleteRecording)))
            .then(literal("confirm")
                .then(argument("code", IntegerArgumentType.integer(0)).executes(SReplayCommand::confirm)))
            .then(literal("cancel").executes(SReplayCommand::cancel))
            .then(literal("reload").executes(SReplayCommand::reload))
            .then(literal("server")
                .then(literal("start").executes(SReplayCommand::startServer))
                .then(literal("stop").executes(SReplayCommand::stopServer)))
            .then(literal("get")
                .then(argument("fileName", StringArgumentType.greedyString())
                .suggests(SReplayCommand::suggestRecordingFile)
                .executes(SReplayCommand::getFile)))
            .then(literal("help").executes(SReplayCommand::help)
                .then(Photographer.PARAM_MANAGER.buildHelpCommand()));
        d.register(b);
    }

    private static CompletableFuture<Suggestions> suggestRecordingFile(CommandContext<ServerCommandSource> src, SuggestionsBuilder sb){
        return suggestMatching(SReplayMod.listRecordings().stream().map(f -> f.getName()), sb);
    }

    private static int help(CommandContext<ServerCommandSource> ctx){
        for (Text t: SReplayMod.getFormats().help){
            ctx.getSource().sendFeedback(t, false);
        }
        return 0;
    }

    private static int locate(CommandContext<ServerCommandSource> ctx){
        Photographer p = requirePlayer(ctx);
        if (p != null){
            ctx.getSource().sendFeedback(render(getFormats().botLocation,
                p.getGameProfile().getName(),
                String.format("%.0f", p.x),
                String.format("%.0f", p.y),
                String.format("%.0f", p.z),
                p.dimension.getRawId()
            ), false);
        }
        return 0;
    }

    private static <T> void paginate(CommandContext<ServerCommandSource> ctx, List<T> p, BiConsumer<Integer, T> consumer){
        ServerCommandSource src = ctx.getSource();
        int page = tryGetArg(() -> IntegerArgumentType.getInteger(ctx, "page"), () -> 1) - 1;
        int s = getConfig().itemsPerPage;
        int start = page * s, end = start + s;
        if (start >= p.size() || start < 0 || end < 0){
            ctx.getSource().sendError(getFormats().invalidPageNum);
            return;
        }
        if (end >= p.size()){
            end = p.size() - 1;
        }
        int i = start;
        for (T v: p.subList(start, end)){
            consumer.accept(i++, v);
        }

        src.sendFeedback(render(getFormats().paginationFooter, page + 1, (int)Math.ceil(p.size() / (float)s)), false);
    }

    private static int getMarkers(CommandContext<ServerCommandSource> ctx){
        final Photographer p = requirePlayer(ctx);
        if (p != null){
            final String name = p.getGameProfile().getName();
            final ServerCommandSource src = ctx.getSource();

            src.sendFeedback(render(SReplayMod.getFormats().markerListTitle, name), false);
            paginate(ctx, p.getRecorder().getMarkers(), (i, marker) -> {
                src.sendFeedback(render(SReplayMod.getFormats().markerListItem, name, Integer.toString(i), marker.getName()), false);
            });
        }
        return 0;
    }

    private static int removeMarker(CommandContext<ServerCommandSource> ctx){
        final Photographer p = requirePlayer(ctx);
        if (p != null){
            final String name = p.getGameProfile().getName();
            final ServerCommandSource src = ctx.getSource();
            final int id = IntegerArgumentType.getInteger(ctx, "markerId") - 1;
            if (id < 0 || id >= p.getRecorder().getMarkers().size()){
                src.sendError(SReplayMod.getFormats().invalidMarkerId);
                return 1;
            }
            p.getRecorder().removeMarker(id);
            src.getMinecraftServer().getPlayerManager().broadcastChatMessage(render(SReplayMod.getFormats().markerRemoved, ctx.getSource().getName(), name, Integer.toString(id + 1)), false);
        }
        return 0;
    }

    private static int getFile(CommandContext<ServerCommandSource> ctx){
        final String fName = StringArgumentType.getString(ctx, "fileName");
        final File f = new File(SReplayMod.getConfig().savePath, fName);
        if (f.exists()){
            final String path = SReplayMod.getServer().addFile(f, SReplayMod.getConfig().downloadTimeout);
            String url = "http://" + SReplayMod.getConfig().serverHostName;
            final int port = SReplayMod.getConfig().serverPort;
            if (port != 80){
                url += ":" + port;
            }
            url += path;
            ctx.getSource().sendFeedback(render(SReplayMod.getFormats().downloadUrl, url), false);
        }
        else {
            ctx.getSource().sendError(render(SReplayMod.getFormats().fileNotFound, fName));
            return 1;
        }
        return 0;
    }

    private static int startServer(CommandContext<ServerCommandSource> ctx){
        final ServerCommandSource src = ctx.getSource();
        final MinecraftServer server = src.getMinecraftServer();
        try {
            final ChannelFuture ch = SReplayMod.getServer().bind(SReplayMod.getConfig().serverListenAddress, SReplayMod.getConfig().serverPort);
            ch.addListener(future -> {
                if (future.isSuccess()){
                    server.getPlayerManager().broadcastChatMessage(SReplayMod.getFormats().serverStarted, true);
                }
                else {
                    src.sendError(render(SReplayMod.getFormats().serverStartFailed, future.cause().getMessage()));
                }
            });
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    private static int stopServer(CommandContext<ServerCommandSource> ctx){
        final ServerCommandSource src = ctx.getSource();
        final MinecraftServer server = src.getMinecraftServer();
        final ChannelFuture ch = SReplayMod.getServer().stop();
        ch.addListener(future -> {
            if (future.isSuccess()){
                server.getPlayerManager().broadcastChatMessage(SReplayMod.getFormats().serverStopped, true);
            }
            else {
                src.sendError(render(SReplayMod.getFormats().serverStopFailed, future.cause().getMessage()));
            }
        });
        return 0;
    }

    private static int getName(CommandContext<ServerCommandSource> ctx){
        Photographer p = requirePlayer(ctx);
        if (p != null){
            ctx.getSource().sendFeedback(render(SReplayMod.getFormats().recordingFile, p.getGameProfile().getName(), p.getSaveName()), false);
            return 1;
        }
        return 0;
    }

    private static int setName(CommandContext<ServerCommandSource> ctx){
        Photographer p = requirePlayer(ctx);
        if (p != null){
            String name = StringArgumentType.getString(ctx, "fileName");
            if (name.endsWith(MCPR)){
                name = name.substring(0, name.length() - MCPR.length());
            }
            if (Photographer.checkForSaveFileDupe(ctx.getSource().getMinecraftServer(), SReplayMod.getConfig().savePath, name)){
                ctx.getSource().sendError(render(SReplayMod.getFormats().recordFileExists, name));
                return 0;
            }
            p.setSaveName(name);
            ctx.getSource().sendFeedback(render(SReplayMod.getFormats().renamedFile, ctx.getSource().getName(), p.getGameProfile().getName(), name), true);
            return 1;
        }
        return 0;
    }

    static Photographer requirePlayer(CommandContext<ServerCommandSource> ctx){
        String name = StringArgumentType.getString(ctx, "player");
        Photographer p = SReplayMod.getFake(ctx.getSource().getMinecraftServer(), name);
        if (p != null){
            return p;
        }
        else {
            try {
                ctx.getSource().sendFeedback(render(SReplayMod.getConfig().formats.playerNotFound, name), true);
            }
            catch(Exception e){
                e.printStackTrace();
            }
            return null;
        }
    }

    public static int marker(CommandContext<ServerCommandSource> ctx){
        Photographer p = requirePlayer(ctx);
        if (p != null){
            String name = StringArgumentType.getString(ctx, "marker");
            p.getRecorder().addMarker(name);
            ctx.getSource().getMinecraftServer().getPlayerManager().broadcastChatMessage(render(SReplayMod.getFormats().markerAdded, ctx.getSource().getName(), p.getGameProfile().getName(), name), false);
            return 1;
        }
        else {
            return 0;
        }
    }

    public static int pause(CommandContext<ServerCommandSource> ctx){
        Photographer p = requirePlayer(ctx);
        if (p != null){
            p.getRecorder().pauseRecording();
            ctx.getSource().getMinecraftServer().getPlayerManager().broadcastChatMessage(render(SReplayMod.getFormats().recordingPaused, ctx.getSource().getName(), p.getGameProfile().getName()), false);
            return 1;
        }
        else {
            return 0;
        }
    }

    public static int resume(CommandContext<ServerCommandSource> ctx){
        Photographer p = requirePlayer(ctx);
        if (p != null){
            p.getRecorder().resumeRecording();
            ctx.getSource().getMinecraftServer().getPlayerManager().broadcastChatMessage(render(SReplayMod.getFormats().recordingResumed, ctx.getSource().getName(), p.getGameProfile().getName()), false);
            return 1;
        }
        else {
            return 0;
        }
    }

    public static int reload(CommandContext<ServerCommandSource> ctx){
        try {
            SReplayMod.loadConfig();
            ctx.getSource().sendFeedback(render(SReplayMod.getFormats().reloadedConfig), false);
            return 1;
        } catch (IOException | JsonParseException e) {
            e.printStackTrace();
            ctx.getSource().sendFeedback(render(SReplayMod.getFormats().failedToReloadConfig, e.toString()), false);
            return 0;
        }
    }

    public static int confirm(CommandContext<ServerCommandSource> ctx) {
        final int code = IntegerArgumentType.getInteger(ctx, "code");
        if (!cm.confirm(ctx.getSource().getName(), code)) {
            ctx.getSource().sendFeedback(SReplayMod.getFormats().nothingToConfirm, false);
        }

        return 0;
    }

    public static int cancel(CommandContext<ServerCommandSource> ctx) {
        if (!cm.cancel(ctx.getSource().getName())) {
            ctx.getSource().sendFeedback(SReplayMod.getFormats().nothingToCancel, false);
        }
        return 0;
    }

    public static int listRecordings(CommandContext<ServerCommandSource> ctx) {
        final ServerCommandSource src = ctx.getSource();
        src.sendFeedback(SReplayMod.getFormats().recordingFileListHead, false);

        paginate(ctx, SReplayMod.listRecordings(), (i, f) -> {
            String size = String.format("%.2f", f.length() / 1024F / 1024F);
            src.sendFeedback(render(SReplayMod.getFormats().recordingFileItem, f.getName(), size), false);
        });
        
        return 0;
    }

    public static int deleteRecording(CommandContext<ServerCommandSource> ctx) {
        final ServerCommandSource src = ctx.getSource();
        final File rec = new File(SReplayMod.getConfig().savePath, StringArgumentType.getString(ctx, "recording"));
        final MinecraftServer server = src.getMinecraftServer();
        if (rec.exists()) {
            src.sendFeedback(render(SReplayMod.getFormats().aboutToDeleteRecording, rec.getName()), true);
            cm.submit(src.getName(), src, () -> {
                rec.delete();
                server.getPlayerManager()
                    .sendToAll(render(SReplayMod.getFormats().deletedRecordingFile, src.getName(), rec.getName()));
            });
        } else {
            src.sendFeedback(render(SReplayMod.getFormats().fileNotFound, rec.getName()), true);
        }
        return 0;
    }

    public static int playerTp(CommandContext<ServerCommandSource> ctx) {
        final Photographer p = requirePlayer(ctx);
        if (p != null){
            Vec3d pos = ctx.getSource().getPosition();
            p.tp(ctx.getSource().getWorld().getDimension().getType(), pos.x, pos.y, pos.z);
            ctx.getSource().getMinecraftServer().getPlayerManager().broadcastChatMessage(render(SReplayMod.getFormats().teleportedBotToYou, p.getGameProfile().getName(), ctx.getSource().getName()), true);
            LOGGER.info("Teleported " + p.getGameProfile().getName() + " to " + ctx.getSource().getName());
            return 1;
        }

        return 0;
    }

    public static int playerSpawn(CommandContext<ServerCommandSource> ctx) {
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
            src.sendFeedback(render(SReplayMod.getFormats().playerIsLoggedIn, pName), true);
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
            src.sendFeedback(render(SReplayMod.getFormats().failedToStartRecording, e.toString()), false);
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
        final Photographer p = requirePlayer(ctx);
        if (p != null) {
            p.reconnect();
            return 1;
        }
        return 0;
    }
}