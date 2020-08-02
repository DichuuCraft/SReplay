package com.hadroncfy.sreplay.interfaces;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.chunk.WorldChunk;

public interface IChunkSender {
    void sendChunk(ServerPlayerEntity player, WorldChunk chunk);
}