package com.github.xniter.chunkmaster.api;

import net.minecraft.world.level.ChunkPos;

public sealed interface Tier permits Tier.Hot, Tier.Warm, Tier.Cold { ;

    ChunkPos pos();

    record Hot(ChunkPos pos) implements Tier {}
    record Warm(ChunkPos pos) implements Tier {}
    record Cold(ChunkPos pos) implements Tier {}
}
