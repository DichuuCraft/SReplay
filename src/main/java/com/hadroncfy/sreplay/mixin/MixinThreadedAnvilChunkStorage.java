package com.hadroncfy.sreplay.mixin;

import java.util.function.Predicate;
import java.util.stream.Stream;

import com.hadroncfy.sreplay.recording.Photographer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;

@Mixin(ThreadedAnvilChunkStorage.class)
public class MixinThreadedAnvilChunkStorage {
    private static final Logger LOGGER = LogManager.getLogger();
    @Shadow private int watchDistance;

    @Shadow
    private static int getChebyshevDistance(ChunkPos pos, ServerPlayerEntity player, boolean useCameraPosition){ return 0; }

    private int getRealViewDistance(ServerPlayerEntity player){
        if (player instanceof Photographer){
            return ((Photographer)player).getRecordingParam().getWatchDistance();
        }
        else {
            return watchDistance;
        }
    }

    @Redirect(method = "getPlayersWatchingChunk", at = @At(
        value = "INVOKE",
        target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"
    ))
    private Stream<ServerPlayerEntity> filter(Stream<ServerPlayerEntity> stream, Predicate<? super ServerPlayerEntity> entity, ChunkPos chunkPos, boolean onlyOnWatchDistanceEdge){
        return stream.filter(player -> {
            int d = getRealViewDistance(player);

            int i = getChebyshevDistance(chunkPos, player, true);
            if (i > d) {
                return false;
            } else {
                return !onlyOnWatchDistanceEdge || i == d;
            }
        });
    }
}