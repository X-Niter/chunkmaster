package com.github.xniter.chunkmaster.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.ticks.ScheduledTick;

import java.util.ArrayList;
import java.util.List;

/**
 * Holder for a chunk‑tick or a commit‑phase update.
 */
public class ChunkTickEvent<T> {
    // regular fields
    public ServerLevel      world;
    public ChunkPos         chunkPos;
    public ScheduledTick<T> tick;

    // commit-phase fields
    private boolean commitPhase;
    /** All the world‑mutation calls we recorded for commit‑phase */
    public final List<Update> collectedUpdates = new ArrayList<>();

    /** Reset to a normal scheduled-tick event */
    public void reset() {
        world            = null;
        chunkPos         = null;
        tick             = null;
        commitPhase      = false;
        collectedUpdates.clear();
    }

    /** Prepare this slot to be a “commit” event instead */
    public void resetToCommitPhase() {
        reset();
        commitPhase = true;
    }

    public boolean isCommitPhase() {
        return commitPhase;
    }

    /** Marker interface for anything you can apply back to the world. */
    public interface Update { void apply(ServerLevel world); }

    public static record BlockUpdate(BlockPos pos, BlockState state, int flags) implements Update {
        @Override public void apply(ServerLevel w) { w.setBlock(pos, state, flags); }
    }

    public static record FluidUpdate(BlockPos pos, FluidState fluid, int flags) implements Update {
        @Override public void apply(ServerLevel w) {
            w.setBlock(pos, fluid.createLegacyBlock(), flags);
        }
    }
}