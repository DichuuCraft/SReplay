package com.hadroncfy.sreplay.recording;

import com.hadroncfy.sreplay.config.Config;

public class RecordingParam {
    public long sizeLimit = -1, timeLimit = -1;
    public boolean autoReconnect = true;

    public static RecordingParam createDefaultRecordingParam(Config config){
        RecordingParam p = new RecordingParam();
        p.autoReconnect = config.autoReconnect;
        p.sizeLimit = config.sizeLimit;
        p.timeLimit = config.timeLimit;
        return p;
    }
}