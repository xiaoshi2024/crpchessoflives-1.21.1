package com.xiaoshi2022.crpchessoflives;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue SHELL_FORGE_ENERGY_CAPACITY = BUILDER
            .comment("Maximum energy storage capacity of the Shell Forge")
            .defineInRange("shellForgeEnergyCapacity", 1_000_000, 1, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue SHELL_FORGE_ENERGY_USAGE_CREATION = BUILDER
            .comment("Energy consumed by the Shell Forge to create a single Shell")
            .defineInRange("shellForgeEnergyUsageCreation", 1_000_000, 1, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue SHELL_FORGE_ENERGY_USAGE_MAINTENANCE = BUILDER
            .comment("Energy consumed by the Shell Forge per tick to maintain an alive Shell")
            .defineInRange("shellForgeEnergyUsageMaintenance", 1_000, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue SHELL_FORGE_CREATION_COOLDOWN = BUILDER
            .comment("Time required to progress by 1% during Shell creation (in ticks)")
            .defineInRange("shellForgeCreationCooldown", 20, 1, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue SHELL_FORGE_DECAY_COOLDOWN = BUILDER
            .comment("Time required to progress by 1% during Shell decay (in ticks)")
            .defineInRange("shellForgeDecayCooldown", 10, 1, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue SHELL_FORGE_DNA_AMOUNT = BUILDER
            .comment("DNA amount required to create a Shell")
            .defineInRange("shellForgeDnaAmount", 64, 1, 64);

    public static final ModConfigSpec.IntValue CENTRIFUGE_ENERGY_CAPACITY = BUILDER
            .comment("Maximum energy storage capacity of the Centrifuge")
            .defineInRange("centrifugeEnergyCapacity", 100_000, 1, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue CENTRIFUGE_ENERGY_USAGE = BUILDER
            .comment("Energy consumed by the Centrifuge per tick")
            .defineInRange("centrifugeEnergyUsage", 3_000, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue TRANSFER_INTO_SHELL_AFTER_DEATH = BUILDER
            .comment("If enabled, the player will respawn in the nearest shell after death")
            .define("transferIntoShellAfterDeath", true);

    // Redstone configuration group
    public static final ModConfigSpec.BooleanValue REDSTONE_POWER_ENABLED;
    public static final ModConfigSpec.IntValue REDSTONE_ENERGY_PER_LEVEL_PER_TICK;
    public static final ModConfigSpec.BooleanValue REDSTONE_SIGNAL_REQUIRED;
    public static final ModConfigSpec.IntValue REDSTONE_MIN_ACTIVATION_LEVEL;

    static {
        BUILDER.push("redstoneActivation");

        REDSTONE_POWER_ENABLED = BUILDER
                .comment("If enabled, Shell Forge and Centrifuge can be powered directly by Redstone signals")
                .comment("Black Fireline: Redstone current activates the culture chamber")
                .define("redstonePowerEnabled", true);

        REDSTONE_ENERGY_PER_LEVEL_PER_TICK = BUILDER
                .comment("FE (Forge Energy) generated per Redstone signal level per tick")
                .comment("Signal strength 0-15. Example: level 15 * 2000 FE = 30000 FE/t")
                .defineInRange("redstoneEnergyPerLevelPerTick", 2000, 1, Integer.MAX_VALUE);

        REDSTONE_SIGNAL_REQUIRED = BUILDER
                .comment("If true, a Redstone signal is REQUIRED to start/continue any operation")
                .comment("Black Fireline failsafe: no activation signal = hard shutdown")
                .define("redstoneSignalRequired", true);

        REDSTONE_MIN_ACTIVATION_LEVEL = BUILDER
                .comment("Minimum Redstone signal level (1-15) to allow operation (when signal is required)")
                .defineInRange("redstoneMinActivationLevel", 1, 1, 15);

        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}