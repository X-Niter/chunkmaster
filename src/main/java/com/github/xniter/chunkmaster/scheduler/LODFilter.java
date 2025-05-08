package com.github.xniter.chunkmaster.scheduler;

import com.github.xniter.chunkmaster.api.Tier;
import com.github.xniter.chunkmaster.config.CommonConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;


public class LODFilter {

    public static Tier classify(LevelChunk chunk, ServerLevel world) {
        ChunkPos pos = chunk.getPos();

        // center of chunk in world‐coords:
        double centerX = pos.getMinBlockX() + 8;
        double centerZ = pos.getMinBlockZ() + 8;

        double distSq = world.getChunkSource().chunkMap
                .getPlayers(pos, false)
                .stream()
                .mapToDouble(player -> player.distanceToSqr(
                        centerX, player.getY(), centerZ))
                .min()
                .orElse(Double.MAX_VALUE);

        int hotR  = CommonConfig.HOT_RADIUS.get();
        int warmR = CommonConfig.WARM_RADIUS.get();
        double hotSq  = hotR  * hotR;
        double warmSq = warmR * warmR;

        if (distSq <= hotSq) {
            return new Tier.Hot(pos);
        } else if (distSq <= warmSq) {
            return new Tier.Warm(pos);
        } else {
            return new Tier.Cold(pos);
        }
    }

    /** convenience: full-tick only HOT chunks */
    public static boolean shouldFullTick(LevelChunk chunk, ServerLevel world) {
        return classify(chunk, world) instanceof Tier.Hot;
    }

    /** partial-tick only WARM chunks */
    public static boolean shouldPartialTick(LevelChunk chunk, ServerLevel world) {
        return classify(chunk, world) instanceof Tier.Warm;
    }

    /** skip-tick only COLD chunks */
    public static boolean shouldSkipTick(LevelChunk chunk, ServerLevel world) {
        return classify(chunk, world) instanceof Tier.Cold;
    }

}
