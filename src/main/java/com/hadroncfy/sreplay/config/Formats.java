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
    private static Text green(String s){
        return new LiteralText(s).setStyle(new Style().setColor(Formatting.GREEN));
    }
    private static Text white(String s){
        return new LiteralText(s).setStyle(new Style().setColor(Formatting.WHITE));
    }
    private static Text yellow(String s){
        return new LiteralText(s).setStyle(new Style().setColor(Formatting.YELLOW));
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
    deletedRecordingFile = new LiteralText("[SReplay] $1: 已删除录像文件$2"),
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
    recordingFileItem = new LiteralText("- $1($2M) ").setStyle(new Style().setColor(Formatting.GREEN))
        .append(new LiteralText("[下载]").setStyle(new Style().setColor(Formatting.BLUE).setClickEvent(
            new ClickEvent(Action.RUN_COMMAND, "/sreplay get $1")
        ).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new LiteralText("点击以生成下载链接").setStyle(new Style().setItalic(true).setColor(Formatting.GRAY))
        ))))
        .append(new LiteralText("[删除]").setStyle(new Style().setColor(Formatting.RED).setClickEvent(
            new ClickEvent(Action.RUN_COMMAND, "/sreplay delete $1")
        ).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new LiteralText("点击以删除").setStyle(new Style().setItalic(true).setColor(Formatting.GRAY))
        )))),
    savingRecordingFile = new LiteralText("[SReplay] 正在保存")
        .append(new LiteralText("$1").setStyle(new Style().setColor(Formatting.GREEN)))
        .append(new LiteralText("的录像文件")),
    savedRecordingFile = new LiteralText("[SReplay] 已保存")
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
    recordingPaused = new LiteralText("[SReplay] $1: ")
        .append(new LiteralText("$2").setStyle(new Style().setColor(Formatting.GOLD)))
        .append(new LiteralText("已暂停录制")),
    recordingResumed = new LiteralText("[SReplay] $1: ")
        .append(new LiteralText("$2").setStyle(new Style().setColor(Formatting.GOLD)))
        .append(new LiteralText("已继续开始录制")),
    markerAdded = new LiteralText("[SReplay] $1: 已在")
        .append(new LiteralText("$2").setStyle(new Style().setColor(Formatting.GOLD)))
        .append(new LiteralText("添加标记"))
        .append(new LiteralText("$3").setStyle(new Style().setItalic(true).setColor(Formatting.GREEN))),
    markerRemoved = new LiteralText("[SReplay] $1: 已在")
        .append(new LiteralText("$2").setStyle(new Style().setColor(Formatting.GOLD)))
        .append(new LiteralText("删除标记"))
        .append(new LiteralText("$3").setStyle(new Style().setItalic(true).setColor(Formatting.GREEN))),
    invalidMarkerId = red("[SReplay] 无效的标记序号"),
    markerListTitle = new LiteralText("[SReplay] ")
        .append(new LiteralText("$1").setStyle(new Style().setColor(Formatting.GOLD)))
        .append("的所有标记："),
    markerListItem = new LiteralText("- [$2] $3")
        .append(new LiteralText("[删除]").setStyle(new Style().setColor(Formatting.GREEN)
            .setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/sreplay player $1 marker remove $2"))
        )),
    renamedFile = new LiteralText("[SReplay] $1: 已将")
        .append(new LiteralText("$2").setStyle(new Style().setColor(Formatting.GREEN)))
        .append(new LiteralText("的文件名设置为"))
        .append(new LiteralText("$3").setStyle(new Style().setColor(Formatting.GREEN))),
    serverStarted = new LiteralText("[SReplay] 下载服务器已启动"),
    serverStartFailed = red("[SReplay] 下载服务器启动失败：$1"),
    serverStopped = new LiteralText("[SReplay] 下载服务器已停止"),
    serverStopFailed = new LiteralText("[SReplay] 下载服务器停止失败：$1"),
    downloadUrl = new LiteralText("[SReplay] 下载链接：")
        .append(new LiteralText("$1").setStyle(new Style().setUnderline(true)
            .setClickEvent(new ClickEvent(Action.OPEN_URL, "$1"))
            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new LiteralText("点击以下载").setStyle(new Style().setColor(Formatting.GRAY).setItalic(true))
            )))),
    autoPaused = new LiteralText("[SReplay] $1: 附近无玩家，暂停录制"),
    autoResumed = new LiteralText("[SReplay] $1: 附近有玩家，继续录制"),
    setParam = new LiteralText("[SReplay] $1: 将")
        .append(new LiteralText("$2").setStyle(new Style().setColor(Formatting.GOLD)))
        .append(new LiteralText("的"))
        .append(new LiteralText("$3").setStyle(new Style().setColor(Formatting.GREEN)))
        .append(new LiteralText("值设置为"))
        .append(new LiteralText("$4").setStyle(new Style().setColor(Formatting.GREEN))),
    getParam = new LiteralText("[SReplay]").append(
        new LiteralText("$1").setStyle(new Style().setColor(Formatting.GREEN))
    ).append(new LiteralText("的")).append(
        new LiteralText("$2").setStyle(new Style().setColor(Formatting.GREEN))
    ).append(new LiteralText("值为")).append(
        new LiteralText("$3").setStyle(new Style().setColor(Formatting.GREEN))
    ),
    positiveParam = red("[SReplay] 此参数须为正整数"),
    nonNegativeOrMinusOne = red("[SReplay] 此参数须为-1或非负数"),
    invalidEnum = red("[SReplay] 无效的值"),
    paginationFooter = new LiteralText("第").append(
        green("($1/$2)")
    ).append(white("页")),
    invalidPageNum = red("[SReplay] 无效的页码"),
    noSuchParam = red("[SReplay] 无此参数"),
    paramHelp = yellow("$1: ").append(green("$2")),
    botLocation = white("[SReplay] ")
        .append(green("$1"))
        .append(white("位于"))
        .append(white("[x: $2, y: $3, z: $4, dim: $5]"));

    public Text[] help = new Text[]{
        new LiteralText("====== SReplay 用法 ======").setStyle(new Style().setColor(Formatting.YELLOW)),
        green("/sreplay player <玩家名> ..."),
        green("- spawn ").append(white("召唤一个录像假人并开始录制")),
        green("- kill ").append(white("踢掉指定的录像假人并保存录像文件")),
        green("- respawn ").append(white("先踢掉假人，保存文件，并在原地开始新一轮的录制")),
        green("- name [文件名]").append(white("获取或设置录像文件名")),
        green("- tp").append(white("将一个录像假人传送到你的位置")),
        green("- pause").append(white("暂停录制")),
        green("- resume").append(white("继续录制")),
        green("- locate").append(white("显示假人的位置")),
        green("- marker list [页码]").append(white("列出所有已添加的标记")),
        green("- marker add [标记名]").append(white("在当前位置添加一个标记")),
        green("- marker remove [标记序号]").append(white("删除一个标记")),
        green("- set <参数名> [参数值]").append(white("设置或获取相应的参数，查看详情请使用")).append(green("/sreplay help set <参数名>")),
        white(""),
        green("/sreplay list [页码]").append(white("列出所有已录制的文件")),
        green("/sreplay delete <文件名>").append(white("删除给定的录像文件。需要用")).append(green("/sreplay confirm <确认码>")).append(white("来确认")),
        green("/sreplay reload").append(white("重新加载配置文件")),
        green("/sreplay server <start|stop>").append(white("启动/停止用于下载录像文件的http服务器")),
        green("/sreplay get <文件名>").append(white("下载指定的录像文件。该命令会返回一个用于下载文件的临时链接。"))
    };
}