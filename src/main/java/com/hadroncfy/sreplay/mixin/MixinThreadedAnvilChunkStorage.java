package com.hadroncfy.sreplay.mixin;

import java.util.function.Predicate;
import java.util.stream.Stream;

import com.hadroncfy.sreplay.interfaces.IChunkSender;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import static com.hadroncfy.sreplay.recording.Photographer.getRealViewDistance;

@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class MixinThreadedAnvilChunkStorage implements IChunkSender {
    private static final Logger LOGGER = LogManager.getLogger();
    @Shadow private int watchDistance;

    @Shadow
    private static int getChebyshevDistance(ChunkPos pos, ServerPlayerEntity player, boolean useCameraPosition){ return 0; }

    @Shadow
    abstract void sendChunkDataPackets(ServerPlayerEntity player, Packet<?>[] packets, WorldChunk chunk);

    @Override
    public void sendChunk(ServerPlayerEntity player, WorldChunk chunk) {
        Packet<?>[] packets = new Packet[2];
        sendChunkDataPackets(player, packets, chunk);
    }

    @Redirect(method = "getPlayersWatchingChunk", at = @At(
        value = "INVOKE",
        target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"
    ))
    private Stream<ServerPlayerEntity> filterPlayers(Stream<ServerPlayerEntity> stream, Predicate<? super ServerPlayerEntity> entity, ChunkPos chunkPos, boolean onlyOnWatchDistanceEdge){
        return stream.filter(player -> {
            int d = getRealViewDistance(player, watchDistance);
            int i = getChebyshevDistance(chunkPos, player, true);
            if (i > d) {
                return false;
            } else {
                return !onlyOnWatchDistanceEdge || i == d;
            }
        });
    }
}