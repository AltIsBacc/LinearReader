package com.bugfunbug.linearreader.loaderfamilies;

import com.bugfunbug.linearreader.LinearRuntime;
import com.bugfunbug.linearreader.minecraftapi.MinecraftFamily;

/**
 * Minimal contract for a loader-family bootstrap.
 *
 * Targets should choose one Minecraft family and hand it to a reusable loader
 * family bootstrap.
 */
public interface LoaderBootstrap {

    default LinearRuntime installRuntime(MinecraftFamily minecraftFamily) {
        return LinearRuntime.install(minecraftFamily);
    }
}
