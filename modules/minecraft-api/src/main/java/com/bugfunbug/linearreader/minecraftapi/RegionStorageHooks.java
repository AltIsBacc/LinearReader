package com.bugfunbug.linearreader.minecraftapi;

import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.ChunkPos;

import java.io.IOException;
import java.nio.file.Path;

/**
 * RegionFileStorage seam for Minecraft-family code.
 *
 * This owns the storage-folder lifecycle and path rules that are likely to
 * drift across Minecraft versions as the porting matrix grows.
 */
public interface RegionStorageHooks {

    void onStorageOpened(Path regionFolder);

    Path resolveLinearRegionPath(Path regionFolder, ChunkPos chunkPos);

    RegionFile openVanillaRegionFile(Path regionFilePath, Path regionFolder, boolean sync) throws IOException;
}
