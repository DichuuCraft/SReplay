package com.hadroncfy.sreplay.recording.mcpr;

public interface ProgressBar {
    void onStart();
    void onProgress(float percentage);
    void onDone();
}