package com.hadroncfy.sreplay.recording;

import java.util.Timer;
import java.util.TimerTask;

import com.hadroncfy.sreplay.SReplayMod;
import com.hadroncfy.sreplay.config.TextRenderer;
import com.hadroncfy.sreplay.recording.mcpr.ProgressBar;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class RecordingSaveProgressBar extends ServerBossBar implements ProgressBar {
    private final MinecraftServer server;
    public RecordingSaveProgressBar(MinecraftServer server, String recordingName) {
        super(TextRenderer.render(SReplayMod.getFormats().saveRecordingProgressBarTitle, recordingName), 
            BossBar.Color.GREEN, 
            BossBar.Style.PROGRESS
        );
        setPercent(0);
        for (ServerPlayerEntity player: server.getPlayerManager().getPlayerList()){
            addPlayer(player);
        }
        this.server = server;
    }

    @Override
    public void onStart() {
        setPercent(0);
    }

    @Override
    public void onProgress(float percentage) {
        setPercent(percentage);
    }

    @Override
    public void onDone() {
        new Timer().schedule(new TimerTask(){
            @Override
            public void run() {
                clearPlayers();
            }
        }, 2000);
    }
}