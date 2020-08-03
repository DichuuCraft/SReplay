package com.hadroncfy.sreplay.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;

import static com.hadroncfy.sreplay.recording.Photographer.getRealViewDistance;

@Mixin(targets = { "net.minecraft.server.world.ThreadedAnvilChunkStorage$EntityTracker" })
public class MixinEntityTracker {
    @Shadow @Final
    private Entity entity;

    @Redirect(method = "updateCameraPosition(Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;method_18725(Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;)I"
    ))
    private int getViewDistance(ThreadedAnvilChunkStorage cela, ServerPlayerEntity player){
        return getRealViewDistance(player, ((ThreadedAnvilChunkStorageAccessor)cela).getWatchDistance());
    }
}