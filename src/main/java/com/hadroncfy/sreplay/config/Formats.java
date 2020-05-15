package com.hadroncfy.sreplay.config;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.Action;
import net.minecraft.util.Formatting;

public class Formats {
    private static Text red(String s){
        return new LiteralText(s).setStyle(new Style().setColor(Formatting.RED));
    }
    public Text playerNotFound = red("[SReplay] 玩家$1未找到（或不是录像机器人）"), 
    recordFileExists = red("[SReplay] 录像文件$1已存在"),
    reloadedConfig = new LiteralText("[SReplay] 已加载配置"),
    failedToReloadConfig = red("[SReplay] 加载配置失败：$1"),
    nothingToConfirm = red("[SReplay] 无待确认的操作"),
    nothingToCancel = red("[SReplay] 无待取消的操作"),
    confirmingHint = new LiteralText("[SReplay] 使用")
        .append(new LiteralText("/sreplayer confirm $1").setStyle(new Style().setColor(Formatting.GOLD)))
        .append(new LiteralText("以确认此次操作")),
    deletedRecordingFile = new LiteralText("$1已删除录像文件$2"),
    operationCancelled = new LiteralText("[SReplay] 已取消操作"),
    incorrectConfirmationCode = red("[SReplay] 确认码不匹配"),
    fileNotFound = red("[SReplay] 文件$1不存在"),
    teleportedBotToYou = new LiteralText("[SReplay] 已将")
        .append(new LiteralText("$1").setStyle(new Style().setColor(Formatting.DARK_GRAY)))
        .append(new LiteralText("传送至$2")),
    invalidPlayerName = red("[SReplay] 非法玩家名"),
    playerNameTooLong = red("[SReplay] 玩家名长度不能超过16（否则会chunk ban！）"),
    playerIsLoggedIn = red("[SReplay] 玩家$1已登录"),
    failedToStartRecording = red("[SReplay] 录制失败：$1"),
    recordingFileListHead = new LiteralText("[SReplay] 录制文件列表："),
    recordingFileItem = new LiteralText("- $1($2M)").setStyle(new Style().setColor(Formatting.GREEN)),
    savingRecordingFile = new LiteralText("正在保存")
        .append(new LiteralText("$1").setStyle(new Style().setColor(Formatting.GREEN)))
        .append(new LiteralText("的录像文件")),
    savedRecordingFile = new LiteralText("已保存")
        .append(new LiteralText("$1").setStyle(new Style().setColor(Formatting.GREEN)))
        .append(new LiteralText("的录像文件"))
        .append(new LiteralText("$2").setStyle(new Style().setColor(Formatting.GREEN))),
    failedToSaveRecordingFile = red("[SReplay] 保存$1的录像文件失败：$2"),
    startedRecording = new LiteralText("[SReplay] $1已开始录制"),
    aboutToDeleteRecording = new LiteralText("[SReplay] 将要删除录像文件$1"),
    recordingFile = new LiteralText("[SReplay] $1正在录制")
        .append(new LiteralText("$2").setStyle(new Style().setColor(Formatting.GOLD))),
    sizeLimitTooSmall = red("[SReplay] 大小限制不能小于10M"),
    timeLimitTooSmall = red("[SReplay] 时间限制不能小于10s"),
    recordingPaused = new LiteralText("[SReplay] ")
        .append(new LiteralText("$1").setStyle(new Style().setColor(Formatting.GOLD)))
        .append(new LiteralText("已暂停录制")),
    recordingResumed = new LiteralText("[SReplay] ")
        .append(new LiteralText("$1").setStyle(new Style().setColor(Formatting.GOLD)))
        .append(new LiteralText("已继续开始录制")),
    markerAdded = new LiteralText("[SReplay] 已在")
        .append(new LiteralText("$1").setStyle(new Style().setColor(Formatting.GOLD)))
        .append(new LiteralText("添加标记"))
        .append(new LiteralText("$2").setStyle(new Style().setItalic(true).setColor(Formatting.GREEN))),
    markerRemoved = new LiteralText("[SReplay] 已在")
        .append(new LiteralText("$1").setStyle(new Style().setColor(Formatting.GOLD)))
        .append(new LiteralText("删除标记"))
        .append(new LiteralText("$2").setStyle(new Style().setItalic(true).setColor(Formatting.GREEN))),
    invalidMarkerId = red("[SReplay] 无效的标记序号"),
    markerListTitle = new LiteralText("[SReplay] ")
        .append(new LiteralText("$1").setStyle(new Style().setColor(Formatting.GOLD)))
        .append("的所有标记："),
    markerListItem = new LiteralText("- [$2] $3")
        .append(new LiteralText("[删除]").setStyle(new Style().setColor(Formatting.GREEN)
            .setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/sreplay player $1 marker remove $2"))
        )),
    renamedFile = new LiteralText("[SReplay] 已将")
        .append(new LiteralText("$1").setStyle(new Style().setColor(Formatting.GREEN)))
        .append(new LiteralText("的文件名设置为"))
        .append(new LiteralText("$2").setStyle(new Style().setColor(Formatting.GREEN))),
    serverStarted = new LiteralText("[SReplay] 下载服务器已启动"),
    serverStartFailed = red("[SReplay] 下载服务器启动失败：$1"),
    serverStopped = new LiteralText("[SReplay] 下载服务器已停止"),
    serverStopFailed = new LiteralText("[SReplay] 下载服务器停止失败：$1"),
    downloadUrl = new LiteralText("[SReplay] 下载链接：")
        .append(new LiteralText("$1").setStyle(new Style().setBold(true).setUnderline(true)
            .setClickEvent(new ClickEvent(Action.OPEN_URL, "$1"))
            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new LiteralText("点击以下载").setStyle(new Style().setColor(Formatting.GRAY).setItalic(true))
            ))));
}