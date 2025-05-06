package com.github.xniter.chunkmaster.commands;

import com.github.xniter.chunkmaster.LODFilter;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

public class ChunkStatusCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("chunkstatus")
                .requires(source -> source.hasPermission(2)) // Require OP level 2+
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    ServerPlayer player = source.getPlayerOrException();
                    ServerLevel level = player.serverLevel();
                    BlockPos pos = player.blockPosition();
                    ChunkPos chunkPos = new ChunkPos(pos);
                    LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);

                    var tier = LODFilter.getChunkTier(chunk, level);
                    boolean shouldTick = LODFilter.shouldFullTick(chunk, level);

                    source.sendSuccess(() ->
                            net.minecraft.network.chat.Component.literal(
                                    String.format("Chunk %s in %s is %s (Ticking: %s)",
                                            chunk.getPos(),
                                            level.dimension().location(),
                                            tier,
                                            shouldTick
                                    )
                            ), false
                    );
                    return 1;
                }));
    }
}
