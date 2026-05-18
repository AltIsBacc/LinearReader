package com.bugfunbug.linearreader.mc1201;

import com.bugfunbug.linearreader.minecraftapi.MinecraftFamily;
import com.bugfunbug.linearreader.minecraftapi.RegionStorageHooks;
import com.bugfunbug.linearreader.minecraftapi.WorldPathResolver;

public final class Minecraft1201Family implements MinecraftFamily {

    public static final Minecraft1201Family INSTANCE = new Minecraft1201Family();

    private Minecraft1201Family() {}

    @Override
    public WorldPathResolver worldPathResolver() {
        return Minecraft1201WorldPathResolver.INSTANCE;
    }

    @Override
    public RegionStorageHooks regionStorageHooks() {
        return Minecraft1201RegionStorageHooks.INSTANCE;
    }
}
