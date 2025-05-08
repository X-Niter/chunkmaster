package com.github.xniter.chunkmaster;

import com.github.xniter.chunkmaster.core.ChunkTickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.ticks.ScheduledTick;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class TickChunkProxy {

    // ----------------------------------------------------------------
    // Reflection to call the protected BlockBehaviour.tick(...) method
    // ----------------------------------------------------------------
    private static final MethodHandle BLOCK_TICK_HANDLE;
    static {
        try {
            BLOCK_TICK_HANDLE = MethodHandles.lookup()
                    .findVirtual(
                            BlockBehaviour.class,               // superclass where tick is declared
                            "tick",
                            MethodType.methodType(
                                    void.class,
                                    BlockState.class,               // block state
                                    ServerLevel.class,              // world (ServerLevel, not Level)
                                    BlockPos.class,                 // position
                                    RandomSource.class              // random
                            )
                    );
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to bind BlockBehaviour.tick()", e);
        }
    }

    // --------------------------------------------------------
    // Reflection to call the protected Fluid.tick(...) method
    // --------------------------------------------------------
    private static final MethodHandle FLUID_TICK_HANDLE;
    static {
        try {
            FLUID_TICK_HANDLE = MethodHandles.lookup()
                    .findVirtual(
                            Fluid.class,
                            "tick",
                            MethodType.methodType(
                                    void.class,
                                    ServerLevel.class,
                                    BlockPos.class,
                                    BlockState.class,
                                    FluidState.class
                            )
                    );
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to bind Fluid.tick()", e);
        }
    }

    /**
     * Full block tick: run vanilla logic (via reflection), then record
     * any block‑state change into the Disruptor event.
     */
    public static void fullTick(
            ChunkTickEvent<?> event,
            LevelChunk chunk,
            ServerLevel world,
            ScheduledTick<Block> tick
    ) {
        ChunkMasterMod.LOGGER.trace("[CF][proxy] fullTick on {} in chunk {}", tick.pos(), chunk.getPos());
        BlockPos pos = tick.pos();
        BlockState before = chunk.getBlockState(pos);

        // invoke the protected method
        try {
            BLOCK_TICK_HANDLE.invoke(
                    before.getBlock(), // the BlockBehaviour instance
                    before,
                    world,
                    pos,
                    world.random
            );
        } catch (Throwable t) {
            throw new RuntimeException("Error running block tick", t);
        }

        BlockState after = world.getBlockState(pos);
        event.collectedUpdates.add(new ChunkTickEvent.BlockUpdate(pos, after, 3));
    }

    /**
     * Partial block tick: only run every Nth tick (for LOD). Here we
     * simply delegate to fullTick, but you could add sampling logic:
     */
    public static void partialTick(
            ChunkTickEvent<?> event,
            LevelChunk chunk,
            ServerLevel world,
            ScheduledTick<Block> tick
    ) {
        ChunkMasterMod.LOGGER.trace("[CF][proxy] partialTick on {} in chunk {}", tick.pos(), chunk.getPos());
        // TODO: sample down (e.g. 1 in 2 or based on tick.subTickOrder())
        fullTick(event, chunk, world, tick);
    }

    /**
     * Full fluid tick: run vanilla logic (via reflection), then record
     * any fluid‐state change into the Disruptor event.
     */
    public static void fullFluidTick(
            ChunkTickEvent<?> event,
            LevelChunk chunk,
            ServerLevel world,
            ScheduledTick<Fluid> tick
    ) {
        ChunkMasterMod.LOGGER.trace("[CF][proxy] fullFluidTick on {} in chunk {}", tick.pos(), chunk.getPos());
        BlockPos pos      = tick.pos();
        FluidState before = world.getFluidState(pos);

        // invoke the protected Fluid#tick(Level, BlockPos, BlockState, FluidState)
        try {
            // need the current block state at that pos too:
            BlockState blockState = world.getBlockState(pos);
            FLUID_TICK_HANDLE.invoke(
                    before.getType(),
                    world,
                    pos,
                    blockState,
                    before
            );
        } catch (Throwable t) {
            throw new RuntimeException("Error running fluid tick", t);
        }

        // capture the new FluidState after the tick
        FluidState after = world.getFluidState(pos);

        // record it for commit‑phase, include the flags:
        event.collectedUpdates.add(
                new ChunkTickEvent.FluidUpdate(pos, after, /*flags=*/3)
        );
    }

    /**
     * Partial fluid tick: sample down or delegate to fullFluidTick.
     */
    public static void partialFluidTick(
            ChunkTickEvent<?> event,
            LevelChunk chunk,
            ServerLevel world,
            ScheduledTick<Fluid> tick
    ) {
        // TODO: sample down
        fullFluidTick(event, chunk, world, tick);
    }

    /**
     * Skip block tick entirely.
     */
    public static void skipTick(
            ChunkTickEvent<?> event,
            LevelChunk chunk,
            ServerLevel world,
            ScheduledTick<?> tick
    ) {
        // no‐op
    }
}