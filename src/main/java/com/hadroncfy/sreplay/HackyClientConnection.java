package com.hadroncfy.sreplay;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.Packet;

public class HackyClientConnection extends ClientConnection {
    private IPacketListener p;

    public HackyClientConnection(NetworkSide networkSide, IPacketListener p) {
        super(networkSide);
        this.p = p;
    }

    @Override
    public void send(Packet<?> packet, GenericFutureListener<? extends Future<? super Void>> callback) {
        p.onPacket(packet);
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isLocal() {
        return true;
    }
}