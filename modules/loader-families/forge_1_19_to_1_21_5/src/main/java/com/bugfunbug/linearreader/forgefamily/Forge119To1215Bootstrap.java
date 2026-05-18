package com.bugfunbug.linearreader.forgefamily;

import com.bugfunbug.linearreader.LinearRuntime;
import com.bugfunbug.linearreader.command.LinearCommand;
import com.bugfunbug.linearreader.config.ForgeLinearConfig;
import com.bugfunbug.linearreader.loaderfamilies.LoaderBootstrap;
import com.bugfunbug.linearreader.minecraftapi.MinecraftFamily;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class Forge119To1215Bootstrap implements LoaderBootstrap {

    private final MinecraftFamily minecraftFamily;
    private final LinearRuntime runtime;

    public Forge119To1215Bootstrap(MinecraftFamily minecraftFamily) {
        this.minecraftFamily = minecraftFamily;
        this.runtime = installRuntime();

        ModLoadingContext.get().registerConfig(
                ModConfig.Type.COMMON, ForgeLinearConfig.SPEC, "linearreader-server.toml");
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onConfigLoad);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onConfigReload);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(LinearCommand::register);
    }

    private LinearRuntime installRuntime() {
        return LoaderBootstrap.super.installRuntime(minecraftFamily);
    }

    private void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ForgeLinearConfig.SPEC) {
            ForgeLinearConfig.pushToLinearConfig();
        }
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ForgeLinearConfig.SPEC) {
            ForgeLinearConfig.pushToLinearConfig();
            LinearRuntime.LOGGER.info("[LinearReader] Config reloaded.");
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        runtime.onServerStarting(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        runtime.onServerStopping();
    }

    @SubscribeEvent
    public void onLevelSave(LevelEvent.Save event) {
        runtime.onLevelSave();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            runtime.onServerTick();
        }
    }
}
