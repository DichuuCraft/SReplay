package com.hadroncfy.sreplay.recording;

import java.util.List;

import com.hadroncfy.sreplay.SReplayMod;
import com.hadroncfy.sreplay.config.TextRenderer;
import com.hadroncfy.sreplay.recording.mcpr.ProgressBar;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.network.ServerPlayerEntity;

public class RecordingSaveProgressBar extends ServerBossBar implements ProgressBar {
    private int lastPercentage = 0;

    public RecordingSaveProgressBar(List<ServerPlayerEntity> players, String recordingName) {
        super(TextRenderer.render(SReplayMod.getFormats().saveRecordingProgressBarTitle, recordingName), 
            BossBar.Color.GREEN, 
            BossBar.Style.PROGRESS
        );
        for (ServerPlayerEntity player: players){
            addPlayer(player);
        }
    }

    @Override
    public void onStart() {
        lastPercentage = 0;
        setPercent(0);
    }

    @Override
    public void onProgress(float percentage) {
        setPercent(percentage);
    }

    @Override
    public void onDone() {
        clearPlayers();
    }
    
}