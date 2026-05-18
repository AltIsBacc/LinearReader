package com.bugfunbug.linearreader.minecraftapi;

import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MinecraftFamilyContractTest {

    private static final List<String> FAMILY_CLASS_NAMES = List.of(
            "com.bugfunbug.linearreader.mc1201.Minecraft1201Family",
            "com.bugfunbug.linearreader.mc1202to1214.Minecraft1202To1214Family"
    );

    @Test
    void availableFamiliesExposeBothContracts() {
        for (MinecraftFamily family : loadAvailableFamilies()) {
            assertNotNull(family.worldPathResolver());
            assertNotNull(family.regionStorageHooks());
        }
    }

    @Test
    void availableFamiliesResolveSameLinearRegionPathShape() {
        List<MinecraftFamily> families = loadAvailableFamilies();
        assertFalse(families.isEmpty(), "Expected at least one Minecraft family on the test classpath");

        Path regionFolder = Path.of("/tmp/world/region");
        ChunkPos chunkPos = new ChunkPos(65, -34);
        Path expected = regionFolder.resolve("r.2.-2.linear");

        for (MinecraftFamily family : families) {
            assertEquals(expected, family.regionStorageHooks().resolveLinearRegionPath(regionFolder, chunkPos));
        }
    }

    private static List<MinecraftFamily> loadAvailableFamilies() {
        return FAMILY_CLASS_NAMES.stream()
                .map(MinecraftFamilyContractTest::tryLoadFamily)
                .filter(Objects::nonNull)
                .toList();
    }

    private static MinecraftFamily tryLoadFamily(String className) {
        try {
            Class<?> familyClass = Class.forName(className);
            return (MinecraftFamily) familyClass.getField("INSTANCE").get(null);
        } catch (ClassNotFoundException ignored) {
            return null;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to load Minecraft family " + className, exception);
        }
    }
}
