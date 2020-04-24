package com.hadroncfy.sreplay.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.server.PlayerManager;
import net.minecraft.world.PlayerSaveHandler;

@Mixin(PlayerManager.class)
public interface PlayerManagerAccessor {
    @Accessor("saveHandler")
    PlayerSaveHandler getSaveHandler();
}