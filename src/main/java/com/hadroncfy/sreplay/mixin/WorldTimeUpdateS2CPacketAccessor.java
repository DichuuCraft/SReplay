package com.hadroncfy.sreplay.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

@Mixin(WorldTimeUpdateS2CPacket.class)
public interface WorldTimeUpdateS2CPacketAccessor {
    @Accessor("time")
    long getTime();
}