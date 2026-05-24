package com.bugfunbug.linearreader.mc1215to12110;

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
 * The 1.21.5-1.21.10 family stays on the same RegionFileStorage shape as the
 * 1.20.2-1.21.4 line. The real break in this range is the NBT helper surface,
 * not region-folder lifecycle or chunk-to-region path rules.
 */
public final class Minecraft1215To12110RegionStorageHooks implements RegionStorageHooks {

    public static final Minecraft1215To12110RegionStorageHooks INSTANCE =
            new Minecraft1215To12110RegionStorageHooks();

    private Minecraft1215To12110RegionStorageHooks() {}

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
