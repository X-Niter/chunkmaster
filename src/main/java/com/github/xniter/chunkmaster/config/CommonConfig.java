package com.github.xniter.chunkmaster.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class CommonConfig {
    // assigned in the static block
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<Integer> HOT_RADIUS;
    public static final ModConfigSpec.ConfigValue<Integer> WARM_RADIUS;
    public static final ModConfigSpec.ConfigValue<Boolean> USE_NATIVE_IO;

    // temporary holders during spec construction
    private final ModConfigSpec.ConfigValue<Integer> hotRadius;
    private final ModConfigSpec.ConfigValue<Integer> warmRadius;
    private final ModConfigSpec.ConfigValue<Boolean> useNativeIo;

    static {
        // builder calls our private constructor and returns (instance, spec)
        Pair<CommonConfig, ModConfigSpec> pair =
                new ModConfigSpec.Builder().configure(CommonConfig::new);

        SPEC = pair.getRight();
        CommonConfig instance = pair.getLeft();

        // now pull the instance fields into the static finals
        HOT_RADIUS     = instance.hotRadius;
        WARM_RADIUS    = instance.warmRadius;
        USE_NATIVE_IO  = instance.useNativeIo;
    }

    private CommonConfig(ModConfigSpec.Builder builder) {
        builder.comment("ChunkMaster general settings")
                .push("general");

        this.hotRadius = builder
                .comment("Radius in chunks for full (HOT) ticking")
                .translation("chunkmaster.config.hotRadius")
                .define("hotRadius", 2);

        this.warmRadius = builder
                .comment("Radius in chunks for partial (WARM) ticking")
                .translation("chunkmaster.config.warmRadius")
                .define("warmRadius", 5);

        this.useNativeIo = builder
                .comment("Use native (JNI + io_uring) region reader when available")
                .translation("chunkmaster.config.useNativeIO")
                .define("useNativeIO", false);

        builder.pop();
    }
}
