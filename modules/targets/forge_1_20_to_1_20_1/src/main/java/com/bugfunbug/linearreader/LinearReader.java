package com.bugfunbug.linearreader;

import com.bugfunbug.linearreader.targets.Forge1201Target;
import net.minecraftforge.fml.common.Mod;

@Mod(LinearRuntime.MOD_ID)
public class LinearReader {

    public LinearReader() {
        new Forge1201Target();
    }
}
