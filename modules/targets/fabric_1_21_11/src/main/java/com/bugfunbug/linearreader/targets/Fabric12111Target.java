package com.bugfunbug.linearreader.targets;

import com.bugfunbug.linearreader.fabricfamily.Fabric119To12111Bootstrap;
import com.bugfunbug.linearreader.mc12111.Minecraft12111Family;

public final class Fabric12111Target implements TargetBootstrap {

    public static final Fabric12111Target INSTANCE = new Fabric12111Target();

    private final Fabric119To12111Bootstrap loaderBootstrap =
            new Fabric119To12111Bootstrap(Minecraft12111Family.INSTANCE);

    private Fabric12111Target() {}

    public void onInitialize() {
        loaderBootstrap.onInitialize();
    }
}
