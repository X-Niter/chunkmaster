package com.github.xniter.chunkmaster;

import com.github.xniter.chunkmaster.mixin.ChunkMapAccessor;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.mojang.text2speech.Narrator.LOGGER;

public class ChunkMasterScheduler {
    private static final ExecutorService CHUNK_EXECUTOR =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    /**
     * Called once at mod‐setup to wire in your hooks (via mixin, events, etc).
     */
    public static void init() {
        // No-op: mixin will redirect tickChunks to scheduleChunkTick
    }

    /**
     * Schedules a single chunk tick off the main thread.
     *
     * @param world          the server level
     * @param chunkPosPacked the chunk position, packed into a long
     */
    public static void scheduleChunkTick(ServerLevel world, long chunkPosPacked) {
        CHUNK_EXECUTOR.submit(() -> {
            ChunkPos pos = new ChunkPos(chunkPosPacked);
            ChunkAccess access = world.getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
            if (!(access instanceof LevelChunk chunk)) return;

            if (!LODFilter.shouldFullTick(chunk, world)) return;

            // Schedule actual tick on the main thread
            world.getServer().execute(() -> {
                // Main-thread-safe tick logic
                TickChunkProxy.tick(chunk, world);
            });
        });
    }

    public static void tickAll(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            ChunkMapAccessor accessor = (ChunkMapAccessor) level.getChunkSource().chunkMap;
            Long2ObjectLinkedOpenHashMap<ChunkHolder> chunkMap = accessor.getVisibleChunkMap();

            chunkMap.values().forEach(holder -> {
                LevelChunk chunk = holder.getTickingChunk();
                if (chunk != null) {
                    //LOGGER.debug("[~] Evaluating chunk {} for ticking...", chunk.getPos());
                    boolean shouldTick = LODFilter.shouldFullTick(chunk, level);
                    if (shouldTick) {
                        //LOGGER.info("✅ Ticking chunk at {} in dimension {}", chunk.getPos(), level.dimension().location());
                        TickChunkProxy.tick(chunk, level);
                    } else {
                        LOGGER.debug("⏩ Skipping chunk at {} in dimension {} (too far from players)", chunk.getPos(), level.dimension().location());
                    }
                }
            });
        }
    }
}
