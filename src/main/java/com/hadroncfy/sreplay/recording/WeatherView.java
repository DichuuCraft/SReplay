package com.hadroncfy.sreplay.recording;

import net.minecraft.world.World;

public interface WeatherView {
    ForcedWeather getWeather();
    World getWorld();
}