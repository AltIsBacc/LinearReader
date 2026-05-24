package com.bugfunbug.linearreader.minecraftapi;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.Set;

/**
 * Small adapter for the chunk-NBT helper surface that drifts at 1.21.5.
 *
 * This keeps chunk-shape rules in shared code while moving version-specific
 * CompoundTag helper differences behind one family-owned seam.
 */
public interface ChunkNbtAdapter {

    CompoundTag unwrapChunkTag(CompoundTag rawTag);

    boolean hasCompound(CompoundTag tag, String key);

    CompoundTag getCompoundOrEmpty(CompoundTag tag, String key);

    boolean hasNumeric(CompoundTag tag, String key);

    long getLongOrDefault(CompoundTag tag, String key, long fallback);

    ListTag getListOrEmpty(CompoundTag tag, String key, int expectedElementType);

    Set<String> keySet(CompoundTag tag);

    boolean hasLongArray(CompoundTag tag, String key);

    boolean hasNonEmptyLongArray(CompoundTag tag, String key);
}
