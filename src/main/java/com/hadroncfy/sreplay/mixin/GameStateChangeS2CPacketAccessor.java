package com.hadroncfy.sreplay.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;

@Mixin(GameStateChangeS2CPacket.class)
public interface GameStateChangeS2CPacketAccessor {
    @Accessor("reason")
    int getReason();
}