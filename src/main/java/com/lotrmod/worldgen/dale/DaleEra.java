package com.lotrmod.worldgen.dale;

import net.minecraft.util.StringRepresentable;

/**
 * The two ages of Dale: as it was under King Girion, green and rich, before
 * Smaug came down on it in Third Age 2770, and the Desolation of the Dragon
 * he left behind.
 *
 * <p>The world is in one of them at a time, {@link #live()}, saved with the
 * world ({@link DaleEraData}) and switched with {@code /dale transform}
 * ({@link DaleTransformer}). Dale's ground is the same in both: what changes is
 * which biomes its landscapes are ({@link DaleTerrain#biomeZone}), so what grows
 * on it, what it is finished with and what stands on it.
 */
public enum DaleEra implements StringRepresentable {
    FLOURISHING("flourishing"),
    DESOLATION("desolation");

    public static final StringRepresentable.EnumCodec<DaleEra> CODEC = StringRepresentable.fromEnum(DaleEra::values);

    /** The era the live world is in; read by chunk generation on any thread. */
    private static volatile DaleEra live = FLOURISHING;

    private final String name;

    DaleEra(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public DaleEra other() {
        return this == FLOURISHING ? DESOLATION : FLOURISHING;
    }

    public static DaleEra live() {
        return live;
    }

    static void setLive(DaleEra era) {
        live = era;
    }
}
