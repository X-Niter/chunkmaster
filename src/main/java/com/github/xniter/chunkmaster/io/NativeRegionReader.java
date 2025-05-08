package com.github.xniter.chunkmaster.io;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * Native‐backed region reader (via JNI).
 * For now it’s a stub that always fails—you’ll fill in the JNI bridge later.
 */
public class NativeRegionReader implements RegionReader {
    static {
        // Adjust this to match your actual native library name
        System.loadLibrary("chunkmaster_native_io");
    }

    @Override
    public CompletableFuture<ByteBuffer> readRegion(int chunkX, int chunkZ) {
        CompletableFuture<ByteBuffer> future = new CompletableFuture<>();
        future.completeExceptionally(
                new UnsupportedOperationException("NativeRegionReader not yet implemented")
        );
        return future;
    }
}