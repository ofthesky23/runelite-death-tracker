package com.deathtracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("deathtracker")
public interface DeathTrackerConfig extends Config
{
    @ConfigItem(keyName = "screenshot", name = "Capture death screenshot", description = "Save a game view screenshot beside the death journal")
    default boolean screenshot()
    {
        return true;
    }
}
