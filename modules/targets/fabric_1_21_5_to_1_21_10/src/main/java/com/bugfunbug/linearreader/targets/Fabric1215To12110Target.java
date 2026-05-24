package com.bugfunbug.linearreader.targets;

import com.bugfunbug.linearreader.fabricfamily.Fabric119To12111Bootstrap;
import com.bugfunbug.linearreader.mc1215to12110.Minecraft1215To12110Family;

public final class Fabric1215To12110Target implements TargetBootstrap {

    public static final Fabric1215To12110Target INSTANCE = new Fabric1215To12110Target();

    private final Fabric119To12111Bootstrap loaderBootstrap =
            new Fabric119To12111Bootstrap(Minecraft1215To12110Family.INSTANCE);

    private Fabric1215To12110Target() {}

    public void onInitialize() {
        loaderBootstrap.onInitialize();
    }
}
