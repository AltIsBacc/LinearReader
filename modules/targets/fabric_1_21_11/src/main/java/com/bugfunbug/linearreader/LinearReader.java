package com.bugfunbug.linearreader;

import com.bugfunbug.linearreader.mc12111.Minecraft12111Family;
import com.bugfunbug.linearreader.targets.Fabric12111Target;
import net.fabricmc.api.ModInitializer;

public class LinearReader implements ModInitializer {

    public static void installForTests() {
        LinearRuntime.install(Minecraft12111Family.INSTANCE);
    }

    @Override
    public void onInitialize() {
        Fabric12111Target.INSTANCE.onInitialize();
    }
}
