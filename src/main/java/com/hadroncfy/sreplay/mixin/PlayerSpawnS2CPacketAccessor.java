package com.hadroncfy.sreplay.mixin;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.packet.s2c.play.PlayerSpawnS2CPacket;

@Mixin(PlayerSpawnS2CPacket.class)
public interface PlayerSpawnS2CPacketAccessor {
    @Accessor("uuid")
    UUID getUUID();
}