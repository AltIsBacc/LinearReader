package com.bugfunbug.linearreader.mc1202to1214;

import com.bugfunbug.linearreader.linear.IdleRecompressor;
import com.bugfunbug.linearreader.linear.MCAConverter;
import com.bugfunbug.linearreader.minecraftapi.RegionStorageHooks;
import net.minecraft.world.level.ChunkPos;

import java.nio.file.Path;

/**
 * Starting assumption for the 1.20.2-1.21.4 family: the storage-folder
 * lifecycle and `.linear` naming scheme still match the 1.20.1 line.
 *
 * When the first live target for this family is added, this seam is the place
 * where any RegionFileStorage drift should be captured instead of pushed back
 * into shared runtime code.
 */
public final class Minecraft1202To1214RegionStorageHooks implements RegionStorageHooks {

    public static final Minecraft1202To1214RegionStorageHooks INSTANCE =
            new Minecraft1202To1214RegionStorageHooks();

    private Minecraft1202To1214RegionStorageHooks() {}

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
}
