package com.hadroncfy.sreplay.recording;

import net.minecraft.network.Packet;

public interface IPacketListener {
    void onPacket(Packet<?> p);
}