package com.hadroncfy.sreplay;

import net.minecraft.network.Packet;

public interface IPacketListener {
    void onPacket(Packet<?> p);
}