package com.lotrmod.worldgen.dale;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Which era Dale is in, saved with the world (in the overworld's
 * {@code data/lotrmod_dale.dat}), and the chunks a {@code /dale transform}
 * still has to rebuild, so one cut short by the server stopping carries on
 * when it starts again.
 */
public final class DaleEraData extends SavedData {
    private static final String NAME = "lotrmod_dale";

    private DaleEra era = DaleEra.FLOURISHING;
    /** Chunks of the live world still to be rebuilt for {@link #era}, as {@code ChunkPos.toLong}, in order. */
    private final Set<Long> pending = new LinkedHashSet<>();

    public static DaleEraData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DaleEraData::new, DaleEraData::load), NAME);
    }

    private static DaleEraData load(CompoundTag tag, HolderLookup.Provider registries) {
        DaleEraData data = new DaleEraData();
        data.era = DaleEra.CODEC.byName(tag.getString("Era"), DaleEra.FLOURISHING);
        for (long chunk : tag.getLongArray("Pending")) data.pending.add(chunk);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("Era", era.getSerializedName());
        tag.put("Pending", new LongArrayTag(pending.stream().mapToLong(Long::longValue).toArray()));
        return tag;
    }

    public DaleEra era() {
        return era;
    }

    void setEra(DaleEra era) {
        this.era = era;
        DaleEra.setLive(era);
        setDirty();
    }

    /** Chunks still to be rebuilt; call {@link #setDirty()} after changing it. */
    Set<Long> pending() {
        return pending;
    }
}
