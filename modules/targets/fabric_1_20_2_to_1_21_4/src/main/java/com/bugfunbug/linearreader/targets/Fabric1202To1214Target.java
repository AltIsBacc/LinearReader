package com.bugfunbug.linearreader.targets;

import com.bugfunbug.linearreader.fabricfamily.Fabric119To12111Bootstrap;
import com.bugfunbug.linearreader.mc1202to1214.Minecraft1202To1214Family;

public final class Fabric1202To1214Target implements TargetBootstrap {

    public static final Fabric1202To1214Target INSTANCE = new Fabric1202To1214Target();

    private final Fabric119To12111Bootstrap loaderBootstrap =
            new Fabric119To12111Bootstrap(Minecraft1202To1214Family.INSTANCE);

    private Fabric1202To1214Target() {}

    public void onInitialize() {
        loaderBootstrap.onInitialize();
    }
}
