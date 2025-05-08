package com.github.xniter.chunkmaster.io;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronously reads .mca region files using AsynchronousFileChannel.
 */
public class AsyncRegionReader implements RegionReader {
    private static final Logger log = LogManager.getLogger(AsyncRegionReader.class);

    @Override
    public CompletableFuture<ByteBuffer> readRegion(int chunkX, int chunkZ) {
        int regionX = chunkX >> 5, regionZ = chunkZ >> 5;
        Path file = Path.of("world/region", "r." + regionX + "." + regionZ + ".mca");
        CompletableFuture<ByteBuffer> future = new CompletableFuture<>();

        try {
            AsynchronousFileChannel ch = AsynchronousFileChannel.open(
                    file, StandardOpenOption.READ
            );
            long size = ch.size();
            ByteBuffer buf = ByteBuffer.allocate((int) size);
            ch.read(buf, 0, null, new java.nio.channels.CompletionHandler<>() {
                @Override
                public void completed(Integer result, Object attachment) {
                    buf.flip();
                    log.debug("Loaded region {} ({} bytes)", file, size);
                    future.complete(buf);
                }
                @Override
                public void failed(Throwable exc, Object attachment) {
                    log.error("Failed to load region {}", file, exc);
                    future.completeExceptionally(exc);
                }
            });
        } catch (Exception e) {
            log.error("Error opening region {}", file, e);
            future.completeExceptionally(e);
        }

        return future;
    }
}
