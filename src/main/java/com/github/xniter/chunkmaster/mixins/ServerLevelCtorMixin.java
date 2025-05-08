package com.github.xniter.chunkmaster.mixins;

import com.github.xniter.chunkmaster.ChunkMasterMod;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * After ServerLevel::<init> runs, give its two LevelTicks instances a pointer back to 'this'.
 */
@Mixin(ServerLevel.class)
public class ServerLevelCtorMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onCtor(
            MinecraftServer server,
            Executor dispatcher,
            LevelStorageSource.LevelStorageAccess levelStorageAccess,
            ServerLevelData serverLevelData,
            ResourceKey dimension,
            LevelStem levelStem,
            ChunkProgressListener progressListener,
            boolean isDebug,
            long biomeZoomSeed,
            List customSpawners,
            boolean tickTime,
            RandomSequences randomSequences,
            CallbackInfo ci
    ) {
        ChunkMasterMod.LOGGER.debug("Binding injector for world {}", this);
        LevelTicksWorldInjector.getInstance().bind((ServerLevel)(Object)this);
    }
}
