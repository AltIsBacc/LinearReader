package com.bugfunbug.linearreader.minecraftapi;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.nio.file.Path;

/**
 * Small world-path contract that isolates Minecraft version drift from the
 * shared runtime.
 */
public interface WorldPathResolver {

    Path resolveWorldRoot(MinecraftServer server);

    Path resolveRegionFolder(Path worldRoot, ResourceKey<Level> dimension);
}
