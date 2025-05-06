package com.github.xniter.chunkmaster.config;


import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class CommonConfig {
    // these will be assigned once in the static block
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<Integer> HOT_RADIUS;
    public static final ModConfigSpec.ConfigValue<Integer> WARM_RADIUS;

    // instance fields for temporary holding
    private final ModConfigSpec.ConfigValue<Integer> hotRadius;
    private final ModConfigSpec.ConfigValue<Integer> warmRadius;

    static {
        // configure() calls our private constructor, returns (instance, spec)
        Pair<CommonConfig, ModConfigSpec> specPair =
                new ModConfigSpec.Builder().configure(CommonConfig::new);

        SPEC = specPair.getRight();
        CommonConfig instance = specPair.getLeft();

        // now pull our instance fields into the static finals
        HOT_RADIUS = instance.hotRadius;
        WARM_RADIUS = instance.warmRadius;
    }

    private CommonConfig(ModConfigSpec.Builder builder) {
        builder.comment("ChunkMaster general settings")
                .push("general");

        // assign instance fields
        this.hotRadius = builder
                .comment("Radius in chunks for full (HOT) ticking")
                .translation("chunkmaster.config.hotRadius")
                .define("hotRadius", 2);

        this.warmRadius = builder
                .comment("Radius in chunks for partial (WARM) ticking")
                .translation("chunkmaster.config.warmRadius")
                .define("warmRadius", 5);

        builder.pop();
    }
}
