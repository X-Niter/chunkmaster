package com.github.xniter.chunkmaster.mixins;

import com.github.xniter.chunkmaster.ChunkMasterMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.ScheduledTick;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;

public final class LevelTicksWorldInjector {
    // our one and only instance
    private static final LevelTicksWorldInjector INSTANCE = new LevelTicksWorldInjector();
    public static LevelTicksWorldInjector getInstance() { return INSTANCE; }
    private LevelTicksWorldInjector() { }

    private final Queue<ScheduledTick<Block>> blockQueue = new ConcurrentLinkedQueue<>();
    private final Queue<ScheduledTick<Fluid>> fluidQueue = new ConcurrentLinkedQueue<>();

    /** Called from ServerLevel constructor mixin */
    public void bind(ServerLevel lvl) {
        // attach the world to both block‑ and fluid‑tick queues
        ((ILevelTicksWorld) lvl.getBlockTicks()).setWorld(lvl);
        ((ILevelTicksWorld) lvl.getFluidTicks()).setWorld(lvl);
    }

    @SuppressWarnings("unchecked")
    public <T> void scheduleBlock(ScheduledTick<T> tick) {
        ChunkMasterMod.LOGGER.trace("Queued BLOCK tick for {} at {} (trigger {})",
                tick.type(), tick.pos(), tick.triggerTick());
        blockQueue.add((ScheduledTick<Block>) tick);
    }

    @SuppressWarnings("unchecked")
    public <T> void scheduleFluid(ScheduledTick<T> tick) {
        ChunkMasterMod.LOGGER.trace("Queued FLUID tick for {} at {} (trigger {})",
                tick.type(), tick.pos(), tick.triggerTick());
        fluidQueue.add((ScheduledTick<Fluid>) tick);
    }

    /**
     * Called from your tick‑end mixin.
     * Replays every held ScheduledTick<T> back into vanilla’s BiConsumer<BlockPos,T>.
     */
    public void processBlockTicks(
            long gameTime,
            int maxAllowedTicks,
            BiConsumer<BlockPos, Block> ticker
    ) {
        int ran = 0;
        Iterator<ScheduledTick<Block>> it = blockQueue.iterator();
        while (it.hasNext() && ran < maxAllowedTicks) {
            ScheduledTick<Block> t = it.next();
            if (t.triggerTick() <= gameTime) {
                ticker.accept(t.pos(), t.type());
                it.remove();
                ran++;
            }
            ChunkMasterMod.LOGGER.trace("Running BLOCK tick for {} at {} ({} of {})",
                    t.type(), t.pos(), ran+1, maxAllowedTicks);
        }
        ChunkMasterMod.LOGGER.debug("✅ Drained {} block‑ticks ({} left)", ran, blockQueue.size());
    }

    public void processFluidTicks(
            long gameTime,
            int maxAllowedTicks,
            BiConsumer<BlockPos, Fluid> ticker
    ) {
        int ran = 0;
        Iterator<ScheduledTick<Fluid>> it = fluidQueue.iterator();
        while (it.hasNext() && ran < maxAllowedTicks) {
            ScheduledTick<Fluid> t = it.next();
            if (t.triggerTick() <= gameTime) {
                ticker.accept(t.pos(), t.type());
                it.remove();
                ran++;
            }
            ChunkMasterMod.LOGGER.trace("Running FLUID tick for {} at {} ({} of {})",
                    t.type(), t.pos(), ran+1, maxAllowedTicks);
        }
        ChunkMasterMod.LOGGER.debug("✅ Drained {} fluid‑ticks ({} left)", ran, fluidQueue.size());
    }
}
