package com.hadroncfy.sreplay.recording.mcpr;

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.minecraft.network.Packet;

public interface IReplayFile {
    void saveMetaData(Metadata data) throws IOException;
    void saveMarkers(List<Marker> markers) throws IOException;
    void savePacket(long timestamp, Packet<?> packet, boolean isLoginPhase) throws Exception;
    long getRecordedBytes();
    void closeAndSave(File file) throws IOException;
}