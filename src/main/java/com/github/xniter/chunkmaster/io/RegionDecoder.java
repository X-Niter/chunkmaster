package com.github.xniter.chunkmaster.io;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * Turns raw region bytes into a ChunkData.
 * TODO: swap this stub for real NBT parsing later.
 */
public class RegionDecoder {
    public static CompletableFuture<ChunkData> decode(ByteBuffer buffer) {
        // For now use dummy coords (0,0), we’ll improve this soon
        ChunkData data = new ChunkData(0, 0, buffer);
        return CompletableFuture.completedFuture(data);
    }
}