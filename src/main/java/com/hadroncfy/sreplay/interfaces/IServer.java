package com.hadroncfy.sreplay.interfaces;

import com.hadroncfy.sreplay.recording.IGamePausedListener;

public interface IServer {
    void addOnPauseListener(IGamePausedListener l);
    boolean isIntegratedServer();
}