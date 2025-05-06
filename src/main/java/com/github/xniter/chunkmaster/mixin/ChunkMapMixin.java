package com.github.xniter.chunkmaster.mixin;

import com.github.xniter.chunkmaster.ChunkMasterScheduler;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {

    @Shadow @Final ServerLevel level;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onConstructed(CallbackInfo ci) {
        System.out.println("✅ Mixin applied to ChunkMap!");
    }

    /**
     * Intercept the main chunk‐ticking loop (the BooleanSupplier form),
     * cancel all of vanilla’s work, and hand it off to our async scheduler.
     */
    @Inject(
            method = "tick(Ljava/util/function/BooleanSupplier;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onTickChunks(BooleanSupplier hasMoreTime, CallbackInfo ci) {
        // cancel the rest of ChunkMap.tick(hasMoreTime)
        ci.cancel();

        // now fetch all of the loaded ChunkHolder instances and schedule them:
        for (ChunkHolder holder : ((ChunkMapAccessor)(Object)this).getVisibleChunkMap().values()) {
            long pos = holder.getPos().toLong();
            ChunkMasterScheduler.scheduleChunkTick(this.level, pos);
        }
    }
}