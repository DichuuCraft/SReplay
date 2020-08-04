package com.hadroncfy.sreplay.recording;

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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void notifyListener(Future future, GenericFutureListener l) {
        try {
            l.operationComplete(future);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void send(Packet<?> packet, GenericFutureListener<? extends Future<? super Void>> callback) {
        p.onPacket(packet);
        if (callback != null) {
            try {
                notifyListener(new SimpleCompletedFuture(), callback);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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