package com.hadroncfy.sreplay.mixin;

import java.io.File;
import java.net.Proxy;

import com.hadroncfy.sreplay.recording.Photographer;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.DataFixer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.UserCache;

@Mixin(IntegratedServer.class)
public abstract class MixinIntegratedServer extends MinecraftServer {

    public MixinIntegratedServer(File gameDir, Proxy proxy, DataFixer dataFixer, CommandManager commandManager,
            YggdrasilAuthenticationService authService, MinecraftSessionService sessionService,
            GameProfileRepository gameProfileRepository, UserCache userCache,
            WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory, String levelName) {
        super(gameDir, proxy, dataFixer, commandManager, authService, sessionService, gameProfileRepository, userCache,
                worldGenerationProgressListenerFactory, levelName);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/DisableableProfiler;pop()V"))
    public void onPause(CallbackInfo ci) {
        Photographer.listFakes(this).forEach(Photographer::onSoftPause);
    }

    @Inject(method = "stop", at = @At("HEAD"))
    public void onStop(boolean b, CallbackInfo ci){
        Photographer.killAllFakes(this, false);
    }
}