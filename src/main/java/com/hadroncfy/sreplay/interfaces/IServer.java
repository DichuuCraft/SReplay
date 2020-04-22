package com.hadroncfy.sreplay.interfaces;

import com.hadroncfy.sreplay.IGamePausedListener;

public interface IServer {
    void setOnPauseListener(IGamePausedListener l);
    boolean isIntegratedServer();
}