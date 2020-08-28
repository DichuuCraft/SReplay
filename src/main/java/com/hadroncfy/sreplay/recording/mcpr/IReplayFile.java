package com.hadroncfy.sreplay.recording.mcpr;

import net.minecraft.network.Packet;

public interface IReplayFile {
    void saveMetaData(IMetaData data);
    void savePacket(long timestamp, Packet<?> packet);
}