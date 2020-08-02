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

import com.hadroncfy.sreplay.recording.Photographer;

@Mixin(targets = { "net.minecraft.server.world.ThreadedAnvilChunkStorage$EntityTracker" })
public class MixinEntityTracker {
    @Shadow @Final
    private Entity entity;

    @Redirect(method = "updateCameraPosition(Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At(
        value = "INVOKE",
        // XXX: AAAAAAhhhhhh!!! Why cannot I redirect ThreadedAnvilChunkStorage.watchDistance????!!!!!
        target = "Ljava/lang/Math;min(II)I"
    ))
    private int getViewDistance(int a, int b, ServerPlayerEntity player){
        if (entity instanceof Photographer){
            return Math.min(a, (((Photographer)entity).getRecordingParam().getWatchDistance() - 1) * 16);
        }
        else {
            return Math.min(a, b);
        }
    }
}