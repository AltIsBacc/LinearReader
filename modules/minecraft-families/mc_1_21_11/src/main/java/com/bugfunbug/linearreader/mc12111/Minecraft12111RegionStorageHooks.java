package com.bugfunbug.linearreader.mc12111;

import com.bugfunbug.linearreader.linear.IdleRecompressor;
import com.bugfunbug.linearreader.linear.MCAConverter;
import com.bugfunbug.linearreader.minecraftapi.RegionStorageHooks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 1.21.11 keeps the R3 region-storage assumptions documented for 1.18-1.21.11.
 */
public final class Minecraft12111RegionStorageHooks implements RegionStorageHooks {

    public static final Minecraft12111RegionStorageHooks INSTANCE =
            new Minecraft12111RegionStorageHooks();

    private Minecraft12111RegionStorageHooks() {}

    @Override
    public void onStorageOpened(Path regionFolder) {
        if (regionFolder == null) return;
        MCAConverter.convertFolder(regionFolder);
        IdleRecompressor.registerFolder(regionFolder);
    }

    @Override
    public Path resolveLinearRegionPath(Path regionFolder, ChunkPos chunkPos) {
        return regionFolder.resolve(
                "r." + chunkPos.getRegionX() + "." + chunkPos.getRegionZ() + ".linear");
    }

    @Override
    public RegionFile openVanillaRegionFile(Path regionFilePath, Path regionFolder, boolean sync) throws IOException {
        return new RegionFile(
                new RegionStorageInfo("linearreader", Level.OVERWORLD, "region"),
                regionFilePath,
                regionFolder,
                sync
        );
    }
}
