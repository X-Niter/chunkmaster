package com.github.xniter.chunkmaster;

import com.github.xniter.chunkmaster.commands.ChunkStatusCommand;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ChunkMasterMod.MOD_ID)
public class ChunkMasterMod {
    public static final String MOD_ID = "chunkmaster";
    private static final Logger LOGGER = LoggerFactory.getLogger("ChunkMaster");
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RESET = "\u001B[0m";

    /**
     * NeoForge will pass you the mod-specific event bus here.
     * Use it to register your lifecycle listeners.
     */
    public ChunkMasterMod(IEventBus modBus) {
        LOGGER.info(ANSI_GREEN + "[✔] ChunkMaster initialized!" + ANSI_RESET);
        modBus.addListener(this::setup);

        // Register global game events
        NeoForge.EVENT_BUS.register(this);
    }

    private void setup(FMLCommonSetupEvent event) {
        LOGGER.info("[SETUP] FMLCommonSetupEvent fired!");
        ChunkMasterScheduler.init();
    }

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        LOGGER.info("[CMD] ChunkStatusCommand wired up and ready.");
        ChunkStatusCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server != null) {
            //LOGGER.debug("[TICK] Server tick fired!");
            ChunkMasterScheduler.tickAll(server);
        }
    }
}