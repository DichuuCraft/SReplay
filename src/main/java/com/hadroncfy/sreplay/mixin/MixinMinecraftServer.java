package com.hadroncfy.sreplay.mixin;

import com.hadroncfy.sreplay.Main;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer {

    @Shadow
    public abstract PlayerManager getPlayerManager();

    @Shadow
    public abstract boolean isDedicated();
    
    @Inject(method = "shutdown", at = @At("HEAD"))
    public void onShutdown(CallbackInfo ci){
        if (isDedicated()){
            Main.killAllFakes(getPlayerManager(), false);
        }
    }
}