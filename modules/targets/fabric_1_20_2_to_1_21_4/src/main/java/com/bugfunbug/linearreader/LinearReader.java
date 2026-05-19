package com.bugfunbug.linearreader;

import com.bugfunbug.linearreader.targets.Fabric1202To1214Target;
import net.fabricmc.api.ModInitializer;

public class LinearReader implements ModInitializer {

    @Override
    public void onInitialize() {
        Fabric1202To1214Target.INSTANCE.onInitialize();
    }
}
