package com.hadroncfy.sreplay.recording;

import com.hadroncfy.sreplay.config.Config;

public class RecordingParam {
    public long sizeLimit = -1, timeLimit = -1;
    public boolean autoReconnect = true, autoPause = false;
    private int watchDistance;

    public static RecordingParam createDefaultRecordingParam(Config config, int watchDistance){
        RecordingParam p = new RecordingParam();
        p.autoReconnect = config.autoReconnect;
        p.sizeLimit = config.sizeLimit;
        p.timeLimit = config.timeLimit;
        p.watchDistance = watchDistance;
        return p;
    }

    public int getWatchDistance(){
        return watchDistance;
    }

    void setWatchDistance(int distance){
        watchDistance = distance;
    }
}