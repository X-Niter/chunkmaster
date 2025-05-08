package com.github.xniter.chunkmaster.io;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * Loads a region file’s raw bytes for the given chunk coords.
 */
public interface RegionReader {
    CompletableFuture<ByteBuffer> readRegion(int chunkX, int chunkZ);
}
