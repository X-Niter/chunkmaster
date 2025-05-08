package com.github.xniter.chunkmaster.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;

/**
 * Catch every ScheduledTick<T> before vanilla queues it,
 * shove it into our ring, cancel vanilla.
 */
@Implements(@Interface(iface = ILevelTicksWorld.class, prefix = "world$"))
@Mixin(LevelTicks.class)
public class LevelTicksMixin {
    private ServerLevel world;

    LevelTicks<?> self = (LevelTicks<?>)(Object)this;

    // mixin implements this
    public void world$setWorld(ServerLevel world) {
        this.world = world;
    }

    // schedule interceptor…
    @Inject(method = "schedule", at = @At("HEAD"), cancellable = true)
    private <T> void intercept(ScheduledTick<T> tick, CallbackInfo ci) {
        if (self == world.getBlockTicks()) {
            LevelTicksWorldInjector.getInstance().scheduleBlock(tick);
        } else {
            LevelTicksWorldInjector.getInstance().scheduleFluid(tick);
        }
        ci.cancel();
    }

    // flush at end of each chunk‑tick…
    @Inject(
            method = "tick(JILjava/util/function/BiConsumer;)V",
            at     = @At("TAIL")
    )
    @SuppressWarnings("unchecked")
    private <T> void afterTick(
            long gameTime,
            int maxAllowedTicks,
            BiConsumer<BlockPos, T> ticker,
            CallbackInfo ci
    ) {
        if (self == world.getBlockTicks()) {
            LevelTicksWorldInjector.getInstance()
                    .processBlockTicks(gameTime, maxAllowedTicks, (BiConsumer<BlockPos, Block>) ticker);
        } else {
            LevelTicksWorldInjector.getInstance()
                    .processFluidTicks(gameTime, maxAllowedTicks, (BiConsumer<BlockPos, Fluid>) ticker);
        }
    }
}