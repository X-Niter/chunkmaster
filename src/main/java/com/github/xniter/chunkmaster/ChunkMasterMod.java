package com.github.xniter.chunkmaster;

import com.github.xniter.chunkmaster.commands.ChunkStatusCommand;
import com.github.xniter.chunkmaster.config.CommonConfig;
import com.github.xniter.chunkmaster.core.ChunkEngine;
import com.github.xniter.chunkmaster.io.RegionDecoder;
import com.github.xniter.chunkmaster.io.RegionReader;
import com.github.xniter.chunkmaster.io.RegionReaderFactory;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

@Mod(ChunkMasterMod.MOD_ID)
public class ChunkMasterMod {
    public static final String MOD_ID = "chunkmaster";
    public static final Logger LOGGER = LogManager.getLogger("ChunkMaster");


    public ChunkMasterMod(ModContainer container) {
        LOGGER.info("[✔] ChunkMaster initialized!");

        // register your config spec exactly as NeoForge expects
        container.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);

        // null‑safe grab of the mod‑event‑bus so IntelliJ stops whining
        IEventBus modBus = Objects.requireNonNull(
                container.getEventBus(),
                "Mod event bus must not be null"
        );
        modBus.addListener(this::setup);
        modBus.addListener(this::onConfigLoad);
        modBus.addListener(this::onConfigReload);

        // global bus for other events
        NeoForge.EVENT_BUS.register(this);
    }

    private void setup(FMLCommonSetupEvent event) {
        LOGGER.info("[SETUP] FMLCommonSetupEvent fired!");
        ChunkEngine.startMetricsReporter();
    }

    public void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == CommonConfig.SPEC) {
            LOGGER.info("Loaded ChunkMaster config: hot={}, warm={}",
                    CommonConfig.HOT_RADIUS.get(), CommonConfig.WARM_RADIUS.get());
        }
    }

    public void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == CommonConfig.SPEC) {
            LOGGER.info("Reloaded ChunkMaster config: hot={}, warm={}",
                    CommonConfig.HOT_RADIUS.get(), CommonConfig.WARM_RADIUS.get());
        }
    }

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        LOGGER.info("[CMD] ChunkStatusCommand wired up and ready.");
        ChunkStatusCommand.register(event.getDispatcher());
    }


    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        RegionReader reader = RegionReaderFactory.create();
        int spawnX = level.getSharedSpawnPos().getX() >> 4;
        int spawnZ = level.getSharedSpawnPos().getZ() >> 4;

        reader.readRegion(spawnX, spawnZ)
                .thenCompose(RegionDecoder::decode)
                .thenAccept(chunkData -> {
                    // For now just log success; later insert into 'level'
                    ChunkMasterMod.LOGGER.info("Loaded ChunkData at {}:{}",
                            chunkData.getChunkX(), chunkData.getChunkZ());
                })
                .exceptionally(t -> {
                    ChunkMasterMod.LOGGER.error("Failed to load region", t);
                    return null;
                });
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {

        // stop the Disruptor
        ChunkEngine.shutdown();
    }

    @SubscribeEvent
    public void onServerPostTick(ServerTickEvent.Post evt) {
        ChunkMasterMod.LOGGER.trace("[CF] onServerPostTick");
        ChunkEngine.tickBuckets(evt.getServer());
    }

    @SubscribeEvent
    private void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        ServerPlayer serverPlayer = (ServerPlayer) player;
        ServerLevel level = serverPlayer.serverLevel();

        // This already gives you the chunk the player is standing in:
        ChunkPos chunkPos = serverPlayer.chunkPosition();

        int hotRadius = CommonConfig.HOT_RADIUS.get();
        int warmRadius = CommonConfig.WARM_RADIUS.get();

        ServerChunkCache cache = (ServerChunkCache) level.getChunkSource();
        // both the “pos” and the “id” here are the same ChunkPos instance:
        cache.addRegionTicket(TicketType.PLAYER, chunkPos, hotRadius, chunkPos);
        cache.addRegionTicket(TicketType.PLAYER, chunkPos, warmRadius, chunkPos);
    }
}