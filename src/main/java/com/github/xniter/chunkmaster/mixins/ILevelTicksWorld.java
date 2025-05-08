package com.github.xniter.chunkmaster.mixins;

import net.minecraft.server.level.ServerLevel;

/**
 * A tiny interface that gets mixed into LevelTicks so we can stash
 * the ServerLevel reference on each tick queue.
 */
public interface ILevelTicksWorld {
    /** called by our ServerLevel mixin to attach its world */
    void setWorld(ServerLevel world);
}
