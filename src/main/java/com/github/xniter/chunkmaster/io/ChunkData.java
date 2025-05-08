package com.github.xniter.chunkmaster.io;

import java.nio.ByteBuffer;

/**
 * A placeholder container for data decoded from a region file.
 * Right now it just stores the chunk coords and the raw NBT bytes.
 * Later you’ll replace this with a fully–parsed chunk object.
 */
public class ChunkData {
    private final int chunkX;
    private final int chunkZ;
    private final ByteBuffer rawNbt;

    public ChunkData(int chunkX, int chunkZ, ByteBuffer rawNbt) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.rawNbt = rawNbt;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    /**
     * The raw NBT payload for this chunk (the bytes you got from the .mca file).
     */
    public ByteBuffer getRawNbt() {
        return rawNbt.asReadOnlyBuffer();
    }
}