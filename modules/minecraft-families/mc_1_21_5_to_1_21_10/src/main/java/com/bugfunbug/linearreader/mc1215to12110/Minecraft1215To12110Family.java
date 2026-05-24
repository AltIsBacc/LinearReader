package com.bugfunbug.linearreader.mc1215to12110;

import com.bugfunbug.linearreader.minecraftapi.ChunkNbtAdapter;
import com.bugfunbug.linearreader.minecraftapi.MinecraftFamily;
import com.bugfunbug.linearreader.minecraftapi.RegionStorageHooks;
import com.bugfunbug.linearreader.minecraftapi.WorldPathResolver;

public final class Minecraft1215To12110Family implements MinecraftFamily {

    public static final Minecraft1215To12110Family INSTANCE = new Minecraft1215To12110Family();

    private Minecraft1215To12110Family() {}

    @Override
    public WorldPathResolver worldPathResolver() {
        return Minecraft1215To12110WorldPathResolver.INSTANCE;
    }

    @Override
    public RegionStorageHooks regionStorageHooks() {
        return Minecraft1215To12110RegionStorageHooks.INSTANCE;
    }

    @Override
    public ChunkNbtAdapter chunkNbtAdapter() {
        return Minecraft1215To12110ChunkNbtAdapter.INSTANCE;
    }
}
