package com.bugfunbug.linearreader.targets;

import com.bugfunbug.linearreader.forgefamily.Forge119To1215Bootstrap;
import com.bugfunbug.linearreader.mc1201.Minecraft1201Family;

public final class Forge1201Target implements TargetBootstrap {

    public Forge1201Target() {
        new Forge119To1215Bootstrap(Minecraft1201Family.INSTANCE);
    }
}
