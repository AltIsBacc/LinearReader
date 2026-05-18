package com.bugfunbug.linearreader;

import com.bugfunbug.linearreader.targets.Fabric1201Target;
import net.fabricmc.api.ModInitializer;

public class LinearReader implements ModInitializer {

    @Override
    public void onInitialize() {
        Fabric1201Target.INSTANCE.onInitialize();
    }
}
