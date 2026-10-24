package com.lotrmod.worldgen.dale;

import com.lotrmod.LOTRMod;
import com.lotrmod.command.MiddleEarthCommand;
import com.lotrmod.worldgen.MiddleEarthBiomeSource;
import com.lotrmod.worldgen.MiddleEarthClimate;
import com.lotrmod.worldgen.Region;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * {@code /dale transform}: turns Dale from one {@link DaleEra} to the other,
 * in the world as it stands.
 *
 * <p>Chunks generated from then on are simply generated in the new era: the
 * biome source reads {@link DaleEra#live()}. The work is in the chunks of Dale
 * already generated, which have to be generated afresh. That is done in one of
 * two hidden dimensions, {@code lotrmod:dale_flourishing} and
 * {@code lotrmod:dale_desolation}: Middle-earth again, the same generator and
 * the same seed, but with Dale held in one era. A chunk generated there is
 * exactly what the live world would have generated in that era, so each of
 * Dale's chunks is copied across from it: blocks, block entities and biomes.
 * The template dimensions keep what they have generated, so turning Dale back
 * and forth again is quicker.
 *
 * <p>Only blocks that differ are set, through the level, so light, heightmaps
 * and what players see follow as they would for any other block change; no
 * neighbours are updated, so nothing falls or flows that the template didn't.
 * Anything built in Dale is lost to the transformation; entities are left
 * where they are, and a player left inside a block is lifted out on top.
 *
 * <p>The work is spread over server ticks, at most {@link #BUDGET_NANOS} of
 * copying a tick, nearest the player who asked first. The chunks still to do
 * are saved with the era ({@link DaleEraData}), so a transformation cut short
 * by the server stopping carries on when it starts again.
 */
@EventBusSubscriber(modid = LOTRMod.MODID)
public final class DaleTransformer {
    private DaleTransformer() {}

    public static final ResourceKey<Level> FLOURISHING_TEMPLATE = dimension("dale_flourishing");
    public static final ResourceKey<Level> DESOLATION_TEMPLATE = dimension("dale_desolation");

    private static final TicketType<ChunkPos> TICKET =
            TicketType.create("lotrmod_dale_transform", Comparator.comparingLong(ChunkPos::toLong));

    /** Chunks being loaded or generated at once, in each of the two levels. */
    private static final int IN_FLIGHT = 48;
    /** Longest a tick spends copying chunks. */
    private static final long BUDGET_NANOS = 25_000_000L;
    /** How far outside Dale, in blocks, a chunk can still have something of Dale's in it: a tree's crown, a field's edge. */
    private static final int MARGIN = 32;

    /** The transformation under way, or null. Server thread only. */
    private static Job job;

    private static ResourceKey<Level> dimension(String name) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(LOTRMod.MODID, name));
    }

    public static ResourceKey<Level> template(DaleEra era) {
        return era == DaleEra.FLOURISHING ? FLOURISHING_TEMPLATE : DESOLATION_TEMPLATE;
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        job = null;
        DaleEraData data = DaleEraData.get(event.getServer());
        DaleEra.setLive(data.era());
        if (!data.pending().isEmpty()) {
            LOTRMod.LOGGER.info("Dale: carrying on turning {} chunks to {}", data.pending().size(), data.era().getSerializedName());
            job = new Job(event.getServer(), data, null);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        job = null;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (job != null && job.tick()) job = null;
    }

    /** Whether no transformation is under way. */
    public static boolean isIdle() {
        return job == null;
    }

    /** What {@code /dale} reports. */
    public static Component status(MinecraftServer server) {
        DaleEraData data = DaleEraData.get(server);
        String era = data.era() == DaleEra.FLOURISHING
                ? "Dale flourishes, as in the days of King Girion"
                : "Dale lies in the Desolation of Smaug";
        if (job != null) era += String.format(" (%d chunks still turning)", data.pending().size());
        return Component.literal(era);
    }

    /** Starts turning Dale to the other era. Returns false if a transformation is already under way. */
    public static boolean transform(CommandSourceStack source) {
        if (job != null) return false;
        MinecraftServer server = source.getServer();
        ServerLevel level = server.getLevel(MiddleEarthCommand.MIDDLE_EARTH);
        ServerLevel template;
        DaleEraData data = DaleEraData.get(server);
        DaleEra era = data.era().other();
        if (level == null || (template = server.getLevel(template(era))) == null) {
            source.sendFailure(Component.literal("The Middle-earth dimension is not loaded"));
            return true;
        }

        // From here on, new ground is generated in the new era
        data.setEra(era);
        data.pending().clear();
        announce(level, era);

        // The chunks already generated: everything saved, once everything
        // loaded has been saved too
        level.getChunkSource().save(true);
        List<ChunkPos> candidates = candidates(level, source);
        List<CompletableFuture<Optional<CompoundTag>>> reads = new ArrayList<>(candidates.size());
        for (ChunkPos pos : candidates) reads.add(level.getChunkSource().chunkMap.read(pos));
        source.sendSuccess(() -> Component.literal("Looking through " + candidates.size() + " chunks in and around Dale...")
                .withStyle(ChatFormatting.GRAY), true);

        Job starting = new Job(server, data, source);
        job = starting;
        CompletableFuture.allOf(reads.toArray(new CompletableFuture[0])).whenCompleteAsync((ignored, error) -> {
            if (job != starting) return;
            for (int i = 0; i < candidates.size(); i++) {
                ChunkPos pos = candidates.get(i);
                Optional<CompoundTag> tag = reads.get(i).getNow(Optional.empty());
                boolean generated = tag.filter(t -> ChunkStatus.byName(t.getString("Status")).isOrAfter(ChunkStatus.BIOMES)).isPresent();
                if (generated) data.pending().add(pos.toLong());
            }
            data.setDirty();
            starting.begin();
        }, server);
        return true;
    }

    /** Every chunk that may have something of Dale's in it, nearest the source first. */
    private static List<ChunkPos> candidates(ServerLevel level, CommandSourceStack source) {
        List<ChunkPos> out = new ArrayList<>();
        int[] bounds = MiddleEarthClimate.daleBounds();
        if (bounds == null || !(level.getChunkSource().getGenerator().getBiomeSource() instanceof MiddleEarthBiomeSource biomes)) {
            return out;
        }
        int cx0 = (bounds[0] - MARGIN) >> 4, cz0 = (bounds[1] - MARGIN) >> 4;
        int cx1 = (bounds[2] + MARGIN) >> 4, cz1 = (bounds[3] + MARGIN) >> 4;
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                if (touchesDale(biomes, cx, cz)) out.add(new ChunkPos(cx, cz));
            }
        }
        BlockPos from = BlockPos.containing(source.getPosition());
        boolean here = source.getLevel() == level;
        if (here) {
            out.sort(Comparator.comparingDouble(c -> {
                double dx = c.getMiddleBlockX() - from.getX(), dz = c.getMiddleBlockZ() - from.getZ();
                return dx * dx + dz * dz;
            }));
        }
        return out;
    }

    private static boolean touchesDale(MiddleEarthBiomeSource biomes, int cx, int cz) {
        int x0 = (cx << 4) - MARGIN, z0 = (cz << 4) - MARGIN;
        for (int dx = 0; dx <= 16 + 2 * MARGIN; dx += 16) {
            for (int dz = 0; dz <= 16 + 2 * MARGIN; dz += 16) {
                if (biomes.getRegion(x0 + dx, z0 + dz) == Region.DALE) return true;
            }
        }
        return false;
    }

    /** Smaug comes, or Dale is as it was; told to everyone in Middle-earth, and heard by those near Dale. */
    private static void announce(ServerLevel level, DaleEra era) {
        Component message = era == DaleEra.DESOLATION
                ? Component.literal("The dragon Smaug comes down upon Dale in fire. Its bells are silent, its fields are burnt, and the land about the Mountain becomes a desolation.")
                        .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC)
                : Component.literal("The years turn back. Dale stands again under the Mountain, green and merry, and its bells ring as in the days of Girion.")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC);
        int[] bounds = MiddleEarthClimate.daleBounds();
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(message);
            if (bounds != null && player.getX() > bounds[0] - 512 && player.getX() < bounds[2] + 512
                    && player.getZ() > bounds[1] - 512 && player.getZ() < bounds[3] + 512) {
                if (era == DaleEra.DESOLATION) {
                    player.playNotifySound(SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.0F, 0.6F);
                } else {
                    player.playNotifySound(SoundEvents.BELL_BLOCK, SoundSource.AMBIENT, 1.0F, 0.8F);
                }
            }
        }
    }

    /** One transformation: the chunks of {@link DaleEraData#pending()}, worked through over ticks. */
    private static final class Job {
        private final MinecraftServer server;
        private final DaleEraData data;
        private final CommandSourceStack source;
        private final Map<Long, InFlight> inFlight = new LinkedHashMap<>();
        private boolean begun;
        private int total;
        private int done;
        private int nextReport = 10;
        private long started;

        Job(MinecraftServer server, DaleEraData data, CommandSourceStack source) {
            this.server = server;
            this.data = data;
            this.source = source;
            if (source == null) begin();
        }

        void begin() {
            begun = true;
            total = data.pending().size();
            started = System.nanoTime();
            message(Component.literal(total == 0
                    ? "No chunks of Dale had been generated yet; it will be generated as it now is"
                    : "Turning " + total + " chunks of Dale..."));
        }

        /** Does a tick's work; true once there is none left. */
        boolean tick() {
            if (!begun) return false;
            ServerLevel level = server.getLevel(MiddleEarthCommand.MIDDLE_EARTH);
            ServerLevel template = server.getLevel(template(data.era()));
            if (level == null || template == null) return true;

            // Keep the levels loading and generating chunks ahead of the copying
            Iterator<Long> next = data.pending().iterator();
            while (inFlight.size() < IN_FLIGHT && next.hasNext()) {
                long chunk = next.next();
                if (!inFlight.containsKey(chunk)) inFlight.put(chunk, new InFlight(level, template, new ChunkPos(chunk)));
            }

            long deadline = System.nanoTime() + BUDGET_NANOS;
            List<ChunkAccess> rebuilt = new ArrayList<>();
            Iterator<InFlight> it = inFlight.values().iterator();
            while (it.hasNext() && System.nanoTime() < deadline) {
                InFlight chunk = it.next();
                if (!chunk.ready()) continue;
                it.remove();
                LevelChunk target = chunk.target();
                LevelChunk from = chunk.from();
                if (target != null && from != null) {
                    copy(level, from, target);
                    rebuilt.add(target);
                }
                chunk.release();
                data.pending().remove(chunk.pos.toLong());
                done++;
            }
            if (!rebuilt.isEmpty()) {
                level.getChunkSource().chunkMap.resendBiomesForChunks(rebuilt);
                data.setDirty();
            }

            if (total > 0 && done * 100 >= nextReport * total && done < total) {
                int percent = done * 100 / total;
                message(Component.literal("Dale is " + percent + "% turned").withStyle(ChatFormatting.GRAY));
                nextReport = percent / 10 * 10 + 10;
            }
            if (data.pending().isEmpty() && inFlight.isEmpty()) {
                if (total > 0) {
                    long seconds = (System.nanoTime() - started) / 1_000_000_000L;
                    message(Component.literal("Dale is turned: " + done + " chunks in " + seconds + "s"));
                }
                return true;
            }
            return false;
        }

        private void message(Component text) {
            LOTRMod.LOGGER.info("Dale: {}", text.getString());
            if (source != null) source.sendSuccess(() -> text, true);
        }
    }

    /**
     * A chunk on its way: loaded (or generated) in the live level and in the
     * template. A ticket in each brings it to a full chunk in the levels' own
     * time; asking for it outright on the server thread would wait there.
     */
    private static final class InFlight {
        private final ServerLevel level;
        private final ServerLevel template;
        private final ChunkPos pos;

        InFlight(ServerLevel level, ServerLevel template, ChunkPos pos) {
            this.level = level;
            this.template = template;
            this.pos = pos;
            level.getChunkSource().addRegionTicket(TICKET, pos, 0, pos);
            template.getChunkSource().addRegionTicket(TICKET, pos, 0, pos);
        }

        boolean ready() {
            return target() != null && from() != null;
        }

        LevelChunk target() {
            return level.getChunkSource().getChunkNow(pos.x, pos.z);
        }

        LevelChunk from() {
            return template.getChunkSource().getChunkNow(pos.x, pos.z);
        }

        void release() {
            level.getChunkSource().removeRegionTicket(TICKET, pos, 0, pos);
            template.getChunkSource().removeRegionTicket(TICKET, pos, 0, pos);
        }
    }

    /** Makes {@code target} what {@code from} is: its biomes, its blocks and its block entities. */
    private static void copy(ServerLevel level, LevelChunk from, LevelChunk target) {
        RegistryAccess registries = level.registryAccess();
        LevelChunkSection[] sections = target.getSections();
        LevelChunkSection[] sources = from.getSections();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection source = sources[i];
            LevelChunkSection section = sections[i];
            sections[i] = new LevelChunkSection(section.getStates(), copyOf(source.getBiomes()));
            if (source.hasOnlyAir() && section.hasOnlyAir()) continue;

            int y0 = SectionPos.sectionToBlockCoord(target.getSectionYFromSectionIndex(i));
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState want = source.getBlockState(x, y, z);
                        BlockState have = section.getBlockState(x, y, z);
                        if (want == have) continue;
                        pos.set(target.getPos().getBlockX(x), y0 + y, target.getPos().getBlockZ(z));
                        // Gone without spilling what it held
                        if (have.hasBlockEntity()) level.removeBlockEntity(pos);
                        level.setBlock(pos, want, flags);
                    }
                }
            }
        }

        for (BlockPos at : from.getBlockEntitiesPos()) {
            BlockEntity source = from.getBlockEntity(at);
            BlockEntity copy = level.getBlockEntity(at);
            if (source == null || copy == null || copy.getType() != source.getType()) continue;
            copy.loadWithComponents(source.saveWithoutMetadata(registries), registries);
            copy.setChanged();
            BlockState state = level.getBlockState(at);
            level.sendBlockUpdated(at, state, state, Block.UPDATE_CLIENTS);
        }
        target.setUnsaved(true);

        // Nobody left buried in a wall that wasn't there before
        for (ServerPlayer player : level.players()) {
            if (player.chunkPosition().equals(target.getPos()) && !level.noCollision(player)) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, player.getBlockX(), player.getBlockZ());
                player.teleportTo(player.getX(), y, player.getZ());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static PalettedContainerRO<Holder<Biome>> copyOf(PalettedContainerRO<Holder<Biome>> biomes) {
        return biomes instanceof PalettedContainer<?> container ? (PalettedContainer<Holder<Biome>>) container.copy() : biomes;
    }
}
