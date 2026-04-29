package com.bugfunbug.linearreader.mixin;

import com.bugfunbug.linearreader.LinearRuntime;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Inject(method = "saveAllChunks(ZZZ)Z", at = @At("HEAD"))
    private void queueLinearDirtyRegionsBeforeSave(boolean suppressLog, boolean flush, boolean force,
                                                   CallbackInfoReturnable<Boolean> cir) {
        LinearRuntime.queueDirtyRegionsForSave();
    }
}
