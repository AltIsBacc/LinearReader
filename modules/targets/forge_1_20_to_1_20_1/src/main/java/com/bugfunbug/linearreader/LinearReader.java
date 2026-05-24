package com.bugfunbug.linearreader;

import com.bugfunbug.linearreader.mc1201.Minecraft1201Family;
import com.bugfunbug.linearreader.targets.Forge1201Target;
import net.minecraftforge.fml.common.Mod;

@Mod(LinearRuntime.MOD_ID)
public class LinearReader {

    public static void installForTests() {
        LinearRuntime.install(Minecraft1201Family.INSTANCE);
    }

    public LinearReader() {
        new Forge1201Target();
    }
}
