package com.hadroncfy.sreplay.mixin;

import com.hadroncfy.sreplay.interfaces.IServer;
import com.hadroncfy.sreplay.recording.IGamePausedListener;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.integrated.IntegratedServer;

@Mixin(IntegratedServer.class)
public class MixinIntegratedServer implements IServer {
    private IGamePausedListener l;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/DisableableProfiler;pop()V"))
    public void onPause(CallbackInfo ci) {
        if (l != null){
            l.onPause();
        }
    }

    @Override
    public void setOnPauseListener(IGamePausedListener l) {
        this.l = l;
    }

    @Override
    public boolean isIntegratedServer() {
        return true;
    }
}