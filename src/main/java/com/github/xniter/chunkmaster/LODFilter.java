package com.github.xniter.chunkmaster;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;


public class LODFilter {

    // Define LOD radii
    private static final int HOT_RADIUS = 2;
    private static final int WARM_RADIUS = 5;

    public static boolean shouldFullTick(LevelChunk chunk, ServerLevel level) {
        ChunkTier tier = getChunkTier(chunk, level);
        return tier == ChunkTier.HOT;
    }

    public static boolean shouldPartialTick(LevelChunk chunk, ServerLevel level) {
        ChunkTier tier = getChunkTier(chunk, level);
        return tier == ChunkTier.WARM;
    }

    public static boolean shouldSkipTick(LevelChunk chunk, ServerLevel level) {
        ChunkTier tier = getChunkTier(chunk, level);
        return tier == ChunkTier.COLD;
    }

    public enum ChunkTier {
        HOT, WARM, COLD
    }

    public static ChunkTier getChunkTier(LevelChunk chunk, ServerLevel level) {
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;

        double minDist = Double.MAX_VALUE;

        for (ServerPlayer player : level.players()) {
            Vec3 pos = player.position();
            int playerChunkX = (int) pos.x >> 4;
            int playerChunkZ = (int) pos.z >> 4;

            double dx = chunkX - playerChunkX;
            double dz = chunkZ - playerChunkZ;
            double distSq = dx * dx + dz * dz;

            if (distSq < minDist) {
                minDist = distSq;
            }
        }

        if (minDist <= HOT_RADIUS * HOT_RADIUS) return ChunkTier.HOT;
        if (minDist <= WARM_RADIUS * WARM_RADIUS) return ChunkTier.WARM;
        return ChunkTier.COLD;
    }
}
