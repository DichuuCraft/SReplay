package com.hadroncfy.sreplay.mixin;

import com.hadroncfy.sreplay.recording.Photographer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;

import static com.hadroncfy.sreplay.recording.Photographer.getRealViewDistance;

@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class MixinThreadedAnvilChunkStorage {
    @Shadow private int watchDistance;

    @Shadow
    private static int getChebyshevDistance(ChunkPos pos, ServerPlayerEntity player, boolean useCameraPosition){ return 0; }

    @Redirect(method = "method_18707", at = @At(
        value = "FIELD",
        target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;watchDistance:I"
    ))
    private int getWatchDistance$lambda0$getPlayersWatchingChunk(ThreadedAnvilChunkStorage cela, ChunkPos pos, boolean bl, ServerPlayerEntity player){
        return getRealViewDistance(player, watchDistance);
    }

    @Redirect(method = "method_17219", at = @At(
        value = "FIELD",
        target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;watchDistance:I"
    ))
    private int getWatchDistance$lambda0$setViewDistance(ThreadedAnvilChunkStorage cela, ChunkPos pos, int previousViewDistance, Packet<?>[] packets, ServerPlayerEntity player){
        return getRealViewDistance(player, watchDistance);
    }

    @Redirect(method = "updateCameraPosition", at = @At(
        value = "sreplay_MultipleOrdinalField",
        target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;watchDistance:I",
        args = {
            "ordinals=2, 3, 6, 8..11"
        }
    ))
    private int getPreviousWatchDistance(ThreadedAnvilChunkStorage cela, ServerPlayerEntity player){
        if (player instanceof Photographer){
            return ((Photographer)player).getCurrentWatchDistance();
        }
        return watchDistance;
    }

    @Redirect(method = "updateCameraPosition", at = @At(
        value = "sreplay_MultipleOrdinalField",
        target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;watchDistance:I",
        args = {
            "ordinals=4, 5, 7, 12..15"
        }
    ))
    private int getCurrentWatchDistance(ThreadedAnvilChunkStorage cela, ServerPlayerEntity player){
        if (player instanceof Photographer){
            return ((Photographer)player).getRecordingParam().watchDistance;
        }
        return watchDistance;
    }
}