package com.lotrmod.worldgen.feature;

import com.lotrmod.block.ModBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@code lotrmod:mallorn} — Lothlórien's golden tree, made for this mod.
 *
 * <p>A square trunk of pale bark {@code trunk_size} blocks across rises bare
 * for the lower part of its height, leaning up to {@code lean} blocks to one
 * side as it goes. The broadest trunks swell at the foot, and buttress roots
 * of wood spread out from them into the ground. From {@code bough_start} of
 * the way up, boughs of wood spiral out round the trunk, climbing as they go,
 * shorter towards the top, two blocks thick on the broadest trunks, each
 * ending in a round, clumpy crown of leaves; another crowns the trunk. Spokes
 * of wood run out through every crown so none of its leaves is too far from
 * wood to last. The leaves are mostly {@code mallorn_leaves}, with the
 * brighter {@code golden_mallorn_leaves} over the tops where the sun catches
 * them, or whatever {@code leaves} and {@code sunlit_leaves} say. Strips of
 * {@code mallorn_ivy} climb a share {@code ivy} of the trunk's sides.
 *
 * <pre>{"type": "lotrmod:mallorn", "config": {"min_height": 32, "max_height": 48,
 *   "trunk_size": 4, "bough_length": 7, "bough_start": 0.42, "crown_radius": 4,
 *   "lean": 0, "ivy": 0.35}}</pre>
 *
 * <p>Everything stays within {@code trunk_size + bough_length + crown_radius + lean}
 * blocks of the trunk, which must be no more than 16 for a feature to write it.
 */
public class MallornFeature extends Feature<MallornFeature.Config> {

    public record Config(int minHeight, int maxHeight, int trunkSize, int boughLength, float boughStart, int crownRadius,
            int lean, float ivy, Optional<BlockStateProvider> leaves, Optional<BlockStateProvider> sunlitLeaves)
            implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(1, 64).fieldOf("min_height").forGetter(Config::minHeight),
                Codec.intRange(1, 64).fieldOf("max_height").forGetter(Config::maxHeight),
                Codec.intRange(1, 4).fieldOf("trunk_size").forGetter(Config::trunkSize),
                Codec.intRange(0, 10).fieldOf("bough_length").forGetter(Config::boughLength),
                Codec.floatRange(0.0f, 1.0f).fieldOf("bough_start").forGetter(Config::boughStart),
                Codec.intRange(1, 5).fieldOf("crown_radius").forGetter(Config::crownRadius),
                Codec.intRange(0, 3).optionalFieldOf("lean", 0).forGetter(Config::lean),
                Codec.floatRange(0.0f, 1.0f).optionalFieldOf("ivy", 0.0f).forGetter(Config::ivy),
                BlockStateProvider.CODEC.optionalFieldOf("leaves").forGetter(Config::leaves),
                BlockStateProvider.CODEC.optionalFieldOf("sunlit_leaves").forGetter(Config::sunlitLeaves)
        ).apply(instance, Config::new));
    }

    /** The golden angle, so boughs spiral round the trunk without lining up. */
    private static final float GOLDEN_ANGLE = (float) (Math.PI * (3.0 - Math.sqrt(5.0)));

    /** What the tree is built from, resolved once a tree. */
    private record Parts(BlockState log, BlockState wood, BlockStateProvider leaves, BlockStateProvider sunlit) {}

    /** A block of ivy: where it hangs, and on which side of it the trunk is. */
    private record Ivy(BlockPos pos, Direction trunk) {}

    public MallornFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        Config config = context.config();
        BlockPos origin = context.origin();
        int size = config.trunkSize();
        int height = Mth.nextInt(random, config.minHeight(), Math.max(config.minHeight(), config.maxHeight()));
        // The trunk is centred on the origin, as near as a square of this size can be
        int x0 = origin.getX() - (size - 1) / 2;
        int z0 = origin.getZ() - (size - 1) / 2;
        int y0 = origin.getY();

        if (y0 < level.getMinBuildHeight() + 1 || y0 + height + config.crownRadius() + 4 >= level.getMaxBuildHeight()) {
            return false;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < size; dx++) {
            for (int dz = 0; dz < size; dz++) {
                if (!level.getBlockState(pos.set(x0 + dx, y0 - 1, z0 + dz)).is(BlockTags.DIRT)) return false;
                for (int y = y0; y < y0 + Math.min(height, 8); y++) {
                    if (!TreeFeature.validTreePos(level, pos.set(x0 + dx, y, z0 + dz))) return false;
                }
            }
        }

        Parts parts = new Parts(ModBlocks.MALLORN_LOG.get().defaultBlockState(), ModBlocks.MALLORN_WOOD.get().defaultBlockState(),
                config.leaves().orElseGet(() -> BlockStateProvider.simple(ModBlocks.MALLORN_LEAVES.get())),
                config.sunlitLeaves().orElseGet(() -> BlockStateProvider.simple(ModBlocks.GOLDEN_MALLORN_LEAVES.get())));
        TreeBlocks tree = new TreeBlocks();

        // How far the trunk has leant at each height: none at the foot, the full lean at the top
        Direction leanTo = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int lean = config.lean() > 0 ? random.nextInt(config.lean() + 1) : 0;
        int[] shift = new int[height];
        for (int i = 0; i < height; i++) {
            shift[i] = (int) Math.round(lean * Math.pow((double) i / height, 1.6));
        }

        // The trunk, and for the broadest a swelling at the foot
        for (int i = 0; i < height; i++) {
            int tx = x0 + leanTo.getStepX() * shift[i];
            int tz = z0 + leanTo.getStepZ() * shift[i];
            for (int dx = 0; dx < size; dx++) {
                for (int dz = 0; dz < size; dz++) {
                    tree.log(pos.set(tx + dx, y0 + i, tz + dz), parts.log());
                    // A step in the lean is bridged, so the trunk stays whole
                    if (i > 0 && shift[i] != shift[i - 1]) tree.log(pos.move(Direction.DOWN), parts.log());
                }
            }
        }
        if (size >= 2) {
            int swell = size >= 3 ? height / 3 : 3 + random.nextInt(3);
            for (int y = y0; y < y0 + swell; y++) {
                for (int i = 0; i < size; i++) {
                    tree.log(pos.set(x0 - 1, y, z0 + i), parts.wood());
                    tree.log(pos.set(x0 + size, y, z0 + i), parts.wood());
                    tree.log(pos.set(x0 + i, y, z0 - 1), parts.wood());
                    tree.log(pos.set(x0 + i, y, z0 + size), parts.wood());
                }
            }
            roots(level, random, tree, parts.wood(), x0, y0, z0, size);
        }
        for (int dx = 0; dx < size; dx++) {
            for (int dz = 0; dz < size; dz++) {
                pos.set(x0 + dx, y0 - 1, z0 + dz);
                if (!level.getBlockState(pos).is(Blocks.DIRT)) level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        int top = y0 + height - 1;
        // Boughs spiral up the top of the trunk, about one every two to four blocks
        int start = y0 + Math.max(2, Math.round(height * config.boughStart()));
        if (config.boughLength() > 0) {
            float angle = random.nextFloat() * Mth.TWO_PI;
            int y = start;
            while (y < top - 1) {
                double cx = x0 + (size - 1) / 2.0 + leanTo.getStepX() * shift[y - y0];
                double cz = z0 + (size - 1) / 2.0 + leanTo.getStepZ() * shift[y - y0];
                double up = (double) (y - start) / Math.max(1, top - start);
                // Longest low down, shortening towards the top, so the crown is a broad dome
                double reach = config.boughLength() * (1.0 - 0.5 * up) * (0.75 + random.nextFloat() * 0.35);
                int crown = Math.max(2, config.crownRadius() - (up > 0.7 ? 1 : 0) - random.nextInt(2));
                boolean thick = size >= 3 && up < 0.6;
                bough(level, random, tree, parts, cx, y, cz, angle, size / 2.0 + reach, crown, thick);
                // A broad trunk carries two boughs at a time, on opposite sides
                if (size >= 2 && random.nextInt(3) > 0) {
                    bough(level, random, tree, parts, cx, y, cz, angle + Mth.PI + (random.nextFloat() - 0.5f),
                            size / 2.0 + reach * 0.85, crown, thick);
                }
                angle += GOLDEN_ANGLE;
                y += 2 + random.nextInt(size >= 3 ? 3 : 2);
            }
        }
        double topX = x0 + (size - 1) / 2.0 + leanTo.getStepX() * shift[height - 1];
        double topZ = z0 + (size - 1) / 2.0 + leanTo.getStepZ() * shift[height - 1];
        crown(level, random, tree, parts, topX, top, topZ, config.crownRadius() + (size >= 2 ? 1 : 0));

        // A leaf the spokes couldn't reach, where something stood in their way, would only decay
        tree.place(level, ModBlocks.MALLORN_LEAVES.get().defaultBlockState(), true);

        if (config.ivy() > 0.0f) {
            BlockState vine = ModBlocks.MALLORN_IVY.get().defaultBlockState();
            for (Ivy leaf : climbIvy(random, config.ivy(), x0, y0, z0, size, height, leanTo, shift)) {
                // Where the trunk swells at its foot, the ivy climbs the swelling
                BlockPos at = level.getBlockState(leaf.pos()).is(BlockTags.LOGS) ? leaf.pos().relative(leaf.trunk().getOpposite()) : leaf.pos();
                if (level.isEmptyBlock(at) && level.getBlockState(at.relative(leaf.trunk())).is(BlockTags.LOGS)) {
                    level.setBlock(at, vine.setValue(VineBlock.getPropertyForFace(leaf.trunk()), true), Block.UPDATE_CLIENTS);
                }
            }
        }
        return true;
    }

    /** A bough of wood running out and up from the trunk, a crown at its end and a smaller one on the way. */
    private static void bough(WorldGenLevel level, RandomSource random, TreeBlocks tree, Parts parts,
            double cx, int y, double cz, float angle, double length, int crown, boolean thick) {
        double dx = Mth.cos(angle);
        double dz = Mth.sin(angle);
        double climb = 0.4 + random.nextFloat() * 0.4;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        // Where the bough got to: its last block of wood
        double x = cx;
        double z = cz;
        int end = y;
        int steps = Mth.ceil(length);
        for (int l = 0; l <= steps; l++) {
            double bx = cx + dx * l;
            double bz = cz + dz * l;
            int by = Mth.floor(y + climb * l);
            pos.set(Mth.floor(bx + 0.5), by, Mth.floor(bz + 0.5));
            if (!canGrowInto(level, pos)) break;
            tree.log(pos, parts.wood());
            // The broadest trees' boughs are two blocks thick for most of their length
            if (thick && l < steps * 2 / 3) {
                pos.move(Direction.DOWN);
                if (canGrowInto(level, pos)) tree.log(pos, parts.wood());
            }
            x = bx;
            z = bz;
            end = by;
            if (l == steps / 2 && steps >= 5) {
                crown(level, random, tree, parts, bx, by, bz, Math.max(2, crown - 2));
            }
        }
        crown(level, random, tree, parts, x, end, z, crown);
    }

    /**
     * A round, clumpy crown of leaves on the wood at (x, y, z), a little
     * flattened, widest a block above it and ragged at the rim. The upper
     * layers are mostly sunlit leaves. Spokes of wood run out through it,
     * straight and diagonally, and a stub up its middle, so every leaf is
     * within a few blocks of wood.
     */
    private static void crown(WorldGenLevel level, RandomSource random, TreeBlocks tree, Parts parts,
            double x, int y, double z, int radius) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int ix = Mth.floor(x + 0.5);
        int iz = Mth.floor(z + 0.5);
        y++;
        for (int dy = 0; dy <= 1; dy++) {
            pos.set(ix, y + dy, iz);
            if (canGrowInto(level, pos)) tree.log(pos, parts.wood());
        }
        int diagonal = (int) Math.round((radius - 1) * 0.7);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            Direction next = side.getClockWise();
            for (int k = 1; k < radius; k++) {
                pos.set(ix + side.getStepX() * k, y, iz + side.getStepZ() * k);
                if (!canGrowInto(level, pos)) break;
                tree.log(pos, parts.wood());
            }
            for (int k = 1; k <= diagonal; k++) {
                pos.set(ix + (side.getStepX() + next.getStepX()) * k, y, iz + (side.getStepZ() + next.getStepZ()) * k);
                if (!canGrowInto(level, pos)) break;
                tree.log(pos, parts.wood());
            }
        }

        double high = radius * 0.75;
        for (int dy = -1; dy <= Mth.ceil(high); dy++) {
            // An ellipsoid centred a block above the spokes, three quarters as tall as it is wide
            double v = (dy - 1) / (dy < 1 ? 1.6 : high);
            if (Math.abs(v) >= 1.0) continue;
            double r = (radius + 0.5) * Math.sqrt(1.0 - v * v);
            int ir = Mth.ceil(r);
            for (int ox = -ir; ox <= ir; ox++) {
                for (int oz = -ir; oz <= ir; oz++) {
                    double d = Math.sqrt(ox * ox + oz * oz);
                    if (d > r) continue;
                    // Clumpy at the rim
                    if (d > r - 1.2 && random.nextInt(3) == 0) continue;
                    pos.set(ix + ox, y + dy, iz + oz);
                    if (!TreeFeature.validTreePos(level, pos)) continue;
                    boolean sunlit = dy >= 2 ? random.nextInt(5) > 0 : dy == 1 ? random.nextInt(3) == 0 : random.nextInt(8) == 0;
                    tree.leaf(pos, (sunlit ? parts.sunlit() : parts.leaves()).getState(random, pos));
                }
            }
        }
    }

    /** Strips of ivy up the trunk's sides, from near the foot to a good way up, ragged along their length. */
    private static List<Ivy> climbIvy(RandomSource random, float share, int x0, int y0, int z0, int size, int height,
            Direction leanTo, int[] shift) {
        List<Ivy> ivy = new ArrayList<>();
        for (Direction side : Direction.Plane.HORIZONTAL) {
            for (int along = 0; along < size; along++) {
                if (random.nextFloat() >= share) continue;
                int from = random.nextInt(Math.max(1, height / 4));
                int to = Math.min(height - 1, from + height / 4 + random.nextInt(Math.max(1, height * 2 / 3)));
                for (int i = from; i <= to; i++) {
                    if (random.nextInt(6) == 0) continue;
                    int tx = x0 + leanTo.getStepX() * shift[i];
                    int tz = z0 + leanTo.getStepZ() * shift[i];
                    // The block just outside this side of the trunk, at this place along it
                    int ax = side.getStepX() > 0 ? tx + size : side.getStepX() < 0 ? tx - 1 : tx + along;
                    int az = side.getStepZ() > 0 ? tz + size : side.getStepZ() < 0 ? tz - 1 : tz + along;
                    ivy.add(new Ivy(new BlockPos(ax, y0 + i, az), side.getOpposite()));
                }
            }
        }
        return ivy;
    }

    /** Buttress roots: ridges of wood running out from the trunk's foot, sloping down into the ground. */
    private static void roots(WorldGenLevel level, RandomSource random, TreeBlocks tree, BlockState wood,
            int x0, int y0, int z0, int size) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int roots = 4 + random.nextInt(2 + size * 2);
        float angle = random.nextFloat() * Mth.TWO_PI;
        double cx = x0 + (size - 1) / 2.0;
        double cz = z0 + (size - 1) / 2.0;
        for (int r = 0; r < roots; r++) {
            angle += Mth.TWO_PI / roots + (random.nextFloat() - 0.5f) * 0.5f;
            double dx = Mth.cos(angle);
            double dz = Mth.sin(angle);
            int length = size + 1 + random.nextInt(size + 2);
            int rise = size + random.nextInt(size + 1);
            for (int l = 0; l < length; l++) {
                double d = size / 2.0 + l;
                int x = Mth.floor(cx + dx * d + 0.5);
                int z = Mth.floor(cz + dz * d + 0.5);
                // Highest by the trunk, sinking to the ground at its end
                int above = Math.round(rise * (1.0f - (float) l / length));
                for (int y = y0 + above; y >= y0 - 3; y--) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (y < y0 && !canRootInto(state)) break;
                    if (y >= y0 && !TreeFeature.validTreePos(level, pos)) continue;
                    tree.log(pos, wood);
                }
            }
        }
    }

    /** A bough grows on through air, leaves, plants and other trees' wood. */
    private static boolean canGrowInto(WorldGenLevel level, BlockPos pos) {
        return TreeFeature.validTreePos(level, pos) || level.getBlockState(pos).is(BlockTags.LOGS);
    }

    /** Below the surface, a root runs on down through air, plants and water-free gaps until it meets the ground. */
    private static boolean canRootInto(BlockState state) {
        return state.canBeReplaced() && state.getFluidState().isEmpty();
    }
}
