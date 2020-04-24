package com.hadroncfy.sreplay.mixin;

import com.hadroncfy.sreplay.interfaces.IServer;
import com.hadroncfy.sreplay.recording.IGamePausedListener;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.server.dedicated.MinecraftDedicatedServer;

@Mixin(MinecraftDedicatedServer.class)
public class MixinMinecraftDedicatedServer implements IServer {

    @Override
    public void setOnPauseListener(IGamePausedListener l) {
        throw new RuntimeException("Should never reach here");
    }

    @Override
    public boolean isIntegratedServer() {
        return false;
    }

}