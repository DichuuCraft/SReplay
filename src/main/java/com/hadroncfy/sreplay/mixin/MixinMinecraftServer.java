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
    
    @Inject(method = "shutdown", at = @At("INVOKE"))
    public void onShutdown(CallbackInfo ci){
        Main.killAllFakes(getPlayerManager(), false);
    }
}