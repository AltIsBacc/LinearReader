package com.bugfunbug.linearreader;

import com.bugfunbug.linearreader.mc1215to12110.Minecraft1215To12110Family;
import com.bugfunbug.linearreader.targets.Fabric1215To12110Target;
import net.fabricmc.api.ModInitializer;

public class LinearReader implements ModInitializer {

    public static void installForTests() {
        LinearRuntime.install(Minecraft1215To12110Family.INSTANCE);
    }

    @Override
    public void onInitialize() {
        Fabric1215To12110Target.INSTANCE.onInitialize();
    }
}
