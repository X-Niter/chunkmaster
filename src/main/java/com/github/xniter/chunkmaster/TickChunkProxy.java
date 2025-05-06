package com.github.xniter.chunkmaster;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;

import java.util.ArrayList;
import java.util.List;

import static com.mojang.text2speech.Narrator.LOGGER;

public class TickChunkProxy {

    public static void tick(LevelChunk chunk, ServerLevel level) {
        LODFilter.ChunkTier tier = LODFilter.getChunkTier(chunk, level);

        switch (tier) {
            case HOT -> fullTick(chunk, level);
            case WARM -> partialTick(chunk, level);
            case COLD -> skipTick(chunk, level);
        }
    }

    private static void fullTick(LevelChunk chunk, ServerLevel level) {
        tickScheduledBlockTicks(chunk, level);
        tickScheduledFluidTicks(chunk, level);
    }

    private static void tickScheduledBlockTicks(LevelChunk chunk, ServerLevel level) {
        LevelChunkTicks<Block> blockTicks = (LevelChunkTicks<Block>) chunk.getBlockTicks();

        List<ScheduledTick<Block>> scheduledTicks = new ArrayList<>();
        blockTicks.getAll().forEach(scheduledTicks::add);

        for (ScheduledTick<Block> scheduled : scheduledTicks) {
            BlockPos pos = scheduled.pos();
            BlockState state = level.getBlockState(pos);

            try {
                state.tick(level, pos, level.random);
            } catch (Exception e) {
                LOGGER.error("Error ticking block {} at {}: {}", state.getBlock(), pos, e.toString());
            }
        }
    }


    private static void tickScheduledFluidTicks(LevelChunk chunk, ServerLevel level) {
        LevelChunkTicks<Fluid> fluidTicks = (LevelChunkTicks<Fluid>) chunk.getFluidTicks();

        // Snapshot to avoid concurrent modification
        List<ScheduledTick<Fluid>> scheduledTicks = new ArrayList<>();
        fluidTicks.getAll().forEach(scheduledTicks::add);

        for (ScheduledTick<Fluid> scheduled : scheduledTicks) {
            BlockPos pos = scheduled.pos();
            FluidState state = level.getFluidState(pos);

            try {
                state.tick(level, pos, level.getBlockState(pos));
            } catch (Exception e) {
                LOGGER.error("Error ticking fluid {} at {}: {}", state.getType(), pos, e.toString());
            }
        }
    }


    private static void partialTick(LevelChunk chunk, ServerLevel level) {
        // Simulate lighter behavior: lighting updates, fluid ticks, etc.
        // For now we just log it. You can later plug in actual lightweight logic here.
        LOGGER.debug("⚙️ Partial tick for chunk at {} in {}", chunk.getPos(), level.dimension().location());
    }

    private static void skipTick(LevelChunk chunk, ServerLevel level) {
        // Do nothing, just log
        LOGGER.trace("🧊 Skipping cold chunk at {} in {}", chunk.getPos(), level.dimension().location());
    }
}