package com.hadroncfy.sreplay.mixin;

import com.hadroncfy.sreplay.Main;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.MinecraftServer;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer {
    @Inject(method = "shutdown", at = @At("INVOKE"))
    public void onShutdown(CallbackInfo ci){
        Main.killAllFakes();
    }
}