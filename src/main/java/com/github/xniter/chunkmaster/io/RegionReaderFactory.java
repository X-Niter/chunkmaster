package com.github.xniter.chunkmaster.io;

import com.github.xniter.chunkmaster.ChunkMasterMod;
import com.github.xniter.chunkmaster.config.CommonConfig;

/**
 * Chooses between Java- and native-IO readers.
 */
public final class RegionReaderFactory {
    private RegionReaderFactory() {}

    public static RegionReader create() {
        if (CommonConfig.USE_NATIVE_IO.get()) {
            try {
                return new NativeRegionReader();
            } catch (Throwable t) {
                ChunkMasterMod.LOGGER.warn("Native IO unavailable, falling back to async Java IO", t);
            }
        }
        return new AsyncRegionReader();
    }
}