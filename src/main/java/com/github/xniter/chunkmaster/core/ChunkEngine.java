package com.github.xniter.chunkmaster.core;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.github.xniter.chunkmaster.ChunkMasterMod;
import com.github.xniter.chunkmaster.TickChunkProxy;
import com.github.xniter.chunkmaster.api.Tier;
import com.github.xniter.chunkmaster.scheduler.LODFilter;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.ScheduledTick;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * High‑performance, Disruptor‑based chunk scheduler.
 */
public class ChunkEngine {
    // composite key for per‑dim, per‑chunk bucketing
    private record BucketKey(ResourceKey<Level> dim, ChunkPos pos) {}

    // a small virtual‑thread pool for your parallel tick execution
    private static final ExecutorService FIBER_POOL =
            Executors.newVirtualThreadPerTaskExecutor();

    // must be a power of two
    private static final int RING_SIZE = 1 << 12;

    // optional Dropwizard metrics
    private static final MetricRegistry METRICS = new MetricRegistry();

    // our in‑memory holding area: (dim, chunkPos) → pending tick‑events
    private static final ConcurrentMap<BucketKey,List<ChunkTickEvent<?>>> BUCKETS =
            new ConcurrentHashMap<>();

    // the disruptor itself + its ring buffer
    private static final Disruptor<ChunkTickEvent<?>> DISRUPTOR;
    public  static final RingBuffer<ChunkTickEvent<?>> RING;

    static {
        EventFactory<ChunkTickEvent<?>> factory = ChunkTickEvent::new;
        ThreadFactory tf = Thread.ofVirtual().name("chunk-handler-",0).factory();

        DISRUPTOR = new Disruptor<>(
                factory,
                RING_SIZE,
                tf,
                ProducerType.MULTI,
                new BusySpinWaitStrategy()
        );
        RING = DISRUPTOR.getRingBuffer();

        // This just holds ticks until their time
        EventHandler<ChunkTickEvent<?>> bucketHandler = (evt, seq, end) -> {
            long now    = evt.world.getGameTime();
            long target = evt.tick.triggerTick();
            if (now < target) {
                RING.publish(seq);
            } else {
                BucketKey key = new BucketKey(evt.world.dimension(), evt.chunkPos);
                BUCKETS
                        .computeIfAbsent(key, k -> new CopyOnWriteArrayList<>())
                        .add(evt);
            }
        };

        DISRUPTOR.handleEventsWith(bucketHandler);
        DISRUPTOR.start();
    }

    /**
     * Producer: wrap a vanilla ScheduledTick into our ring pipeline.
     */
    public static <T> void publishTick(
            ServerLevel world,
            ChunkPos chunkPos,
            ScheduledTick<T> tick
    ) {
        long seq = RING.next();
        try {
            @SuppressWarnings("unchecked")
            ChunkTickEvent<T> evt = (ChunkTickEvent<T>) RING.get(seq);
            evt.reset();
            evt.world    = world;
            evt.chunkPos = chunkPos;
            evt.tick     = tick;
        } finally {
            RING.publish(seq);
        }
    }

    /**
     * Drain and dispatch every bucket once per server tick.
     * Hook this via a Forge @SubscribeEvent (see below).
     */
    public static void tickBuckets(MinecraftServer server) {
        ChunkMasterMod.LOGGER.trace("[CF] tickBuckets → {} keys", BUCKETS.size());
        for (var entry : List.copyOf(BUCKETS.entrySet())) {
            var key  = entry.getKey();
            var list = BUCKETS.remove(key);
            ChunkMasterMod.LOGGER.trace("[CF] draining {} ticks for {}",
                    list == null ? 0 : list.size(), key);
            if (list == null || list.isEmpty()) continue;

            ServerLevel world = server.getLevel(key.dim());
            if (world == null) continue;

            var access = world.getChunk(key.pos.x, key.pos.z, ChunkStatus.FULL, false);
            if (!(access instanceof LevelChunk chunk)) continue;

            Tier tier = LODFilter.classify(chunk, world);

            ChunkMasterMod.LOGGER.trace("[CF] submitting {} ticks to fibers", list.size());
            FIBER_POOL.submit(() -> {
                var updates = new ArrayList<ChunkTickEvent.Update>();
                for (var evt : list) {
                    var raw = evt.tick;
                    if (raw.type() instanceof Block) {
                        @SuppressWarnings("unchecked")
                        var bt = (ScheduledTick<Block>) raw;
                        TickChunkProxy.fullTick(evt, chunk, world, bt);
                    } else if (raw.type() instanceof Fluid) {
                        @SuppressWarnings("unchecked")
                        var ft = (ScheduledTick<Fluid>) raw;
                        TickChunkProxy.fullFluidTick(evt, chunk, world, ft);
                    }
                    updates.addAll(evt.collectedUpdates);
                }
                server.execute(() -> {
                    ChunkMasterMod.LOGGER.trace("[CF][apply] {} updates", updates.size());
                    updates.forEach(u -> u.apply(world));
                });
            });
        }
    }

    /** Optional: start your console‑reporter on MODINIT. */
    public static void startMetricsReporter() {
        ConsoleReporter.forRegistry(METRICS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build()
                .start(30, TimeUnit.SECONDS);
    }

    /** Call on MOD SHUTDOWN. */
    public static void shutdown() {
        DISRUPTOR.halt();
        FIBER_POOL.shutdownNow();
    }
}