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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * {@code lotrmod:old_pine} — an ancient pine of Arnor, drawn for this mod
 * after the old Scots pines of the north rather than the spires of Mirkwood:
 * a crooked trunk, bare for most of its height but for the stubs of branches
 * long dead, under a flat, ragged crown of needles held out on a few long
 * limbs. Now and then the top has died back to a bare snag above the crown.
 *
 * <p>The thick ones stand on a trunk two blocks across for the lower half of
 * their height, narrowing to one above, with roots sprawling out over the
 * ground at their foot. The wood and needles are the original mod's pine.
 *
 * <pre>{"type": "lotrmod:old_pine", "config": {"min_height": 16, "max_height": 24, "thick": false}}</pre>
 */
public class OldPineFeature extends Feature<OldPineFeature.Config> {

    public record Config(int minHeight, int maxHeight, boolean thick) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(6, 40).fieldOf("min_height").forGetter(Config::minHeight),
                Codec.intRange(6, 40).fieldOf("max_height").forGetter(Config::maxHeight),
                Codec.BOOL.optionalFieldOf("thick", false).forGetter(Config::thick)
        ).apply(instance, Config::new));
    }

    public OldPineFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        Config config = context.config();
        BlockPos origin = context.origin();
        int i = origin.getX();
        int j = origin.getY();
        int k = origin.getZ();
        int height = Mth.randomBetweenInclusive(random, config.minHeight(), Math.max(config.minHeight(), config.maxHeight()));
        int width = config.thick() ? 2 : 1;

        if (j < level.getMinBuildHeight() + 2 || j + height + 6 >= level.getMaxBuildHeight()) return false;

        // Soil under every column of the trunk; a thick trunk may reach a block
        // down to it where the ground falls away under one corner
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int a = 0; a < width; a++) {
            for (int b = 0; b < width; b++) {
                BlockState below = level.getBlockState(pos.set(i + a, j - 1, k + b));
                if (below.is(BlockTags.DIRT)) continue;
                if (config.thick() && TreeFeature.validTreePos(level, pos)
                        && level.getBlockState(pos.move(Direction.DOWN)).is(BlockTags.DIRT)) continue;
                return false;
            }
        }
        for (int y = 0; y < height; y++) {
            for (int a = 0; a < width; a++) {
                for (int b = 0; b < width; b++) {
                    if (!TreeFeature.validTreePos(level, pos.set(i + a, j + y, k + b))) return false;
                }
            }
        }

        TreeBlocks tree = new TreeBlocks();
        BlockState log = ModBlocks.PINE_LOG.get().defaultBlockState();
        for (int a = 0; a < width; a++) {
            for (int b = 0; b < width; b++) {
                pos.set(i + a, j - 1, k + b);
                if (level.getBlockState(pos).is(Blocks.GRASS_BLOCK)) {
                    level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
                } else if (!level.getBlockState(pos).is(BlockTags.DIRT)) {
                    tree.log(pos, log);
                }
            }
        }

        // The trunk, two across up to thickTop if thick, jogging a block aside
        // once or twice on the way up. trunkX and trunkZ hold where it is at
        // each height, for the limbs to grow from.
        int thickTop = config.thick() ? (int) (height * (0.4F + random.nextFloat() * 0.2F)) : 0;
        int crownBase = Math.max(thickTop + 1, (int) (height * (0.55F + random.nextFloat() * 0.15F)));
        int[] trunkX = new int[height];
        int[] trunkZ = new int[height];
        Direction lean = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int kinkLow = Math.max(thickTop + 1, height / 3);
        int kinkA = kinkLow + random.nextInt(Math.max(1, height - 3 - kinkLow));
        int kinkB = random.nextBoolean() ? kinkLow + random.nextInt(Math.max(1, height - 3 - kinkLow)) : -1;
        int cx = 0;
        int cz = 0;
        for (int y = 0; y < height; y++) {
            if (y == kinkA || y == kinkB) {
                // The log at the old spot and the new one share a height, so the trunk stays whole
                tree.log(pos.set(i + cx, j + y, k + cz), log);
                Direction jog = random.nextInt(3) == 0 ? Direction.Plane.HORIZONTAL.getRandomDirection(random) : lean;
                cx += jog.getStepX();
                cz += jog.getStepZ();
            }
            trunkX[y] = cx;
            trunkZ[y] = cz;
            int w = y < thickTop ? 2 : 1;
            for (int a = 0; a < w; a++) {
                for (int b = 0; b < w; b++) {
                    pos.set(i + cx + a, j + y, k + cz + b);
                    if (y < thickTop || TreeFeature.validTreePos(level, pos)) tree.log(pos, log);
                }
            }
        }

        if (config.thick()) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (random.nextBoolean()) root(level, tree, random, i, j, k, dir, log);
            }
        }

        // Stubs of dead branches on the bare trunk
        for (int y = 2; y < crownBase; y++) {
            if (random.nextInt(config.thick() ? 3 : 4) != 0) continue;
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            int w = y < thickTop ? 2 : 1;
            BlockPos start = edge(random, i + trunkX[y], j + y, k + trunkZ[y], w, dir);
            int length = config.thick() && random.nextBoolean() ? 2 : 1;
            BlockState stub = log.setValue(RotatedPillarBlock.AXIS, dir.getAxis());
            for (int l = 0; l < length; l++) {
                BlockPos at = start.relative(dir, l);
                if (!TreeFeature.validTreePos(level, at)) break;
                tree.log(at, stub);
            }
        }

        // The crown: a few long limbs reaching out and up from the top of the
        // trunk, each holding out a cushion of needles
        int limbs = 3 + random.nextInt(3) + (config.thick() ? 1 : 0);
        double angle0 = random.nextDouble() * Math.PI * 2.0;
        int crownSpan = Math.max(1, height - 2 - crownBase);
        for (int l = 0; l < limbs; l++) {
            double angle = angle0 + l * Math.PI * 2.0 / limbs + (random.nextDouble() - 0.5) * 0.9;
            int y = Mth.clamp(crownBase + (int) (crownSpan * (l + random.nextDouble()) / limbs), crownBase, height - 2);
            int length = 2 + random.nextInt(3) + (config.thick() ? 1 : 0);
            if (y >= height - 3) length = Math.max(2, length - 1);
            BlockPos end = limb(level, tree, i + trunkX[y], j + y, k + trunkZ[y], angle, length, log);
            int radius = Math.min(3, 2 + (random.nextInt(3) == 0 ? 1 : 0) + (config.thick() ? 1 : 0));
            pad(level, tree, random, end, radius);
        }

        // Tufts of needles close about the trunk among the limbs
        for (int y = crownBase; y < height - 1; y++) {
            if (random.nextInt(3) == 0) tuft(level, tree, i + trunkX[y], j + y, k + trunkZ[y]);
        }

        int topX = i + trunkX[height - 1];
        int topZ = k + trunkZ[height - 1];
        if (random.nextInt(7) == 0) {
            // Died back at the top: a bare snag standing above a thinner crown
            int snag = 2 + random.nextInt(3);
            for (int y = height; y < height + snag; y++) {
                if (!TreeFeature.validTreePos(level, pos.set(topX, j + y, topZ))) break;
                tree.log(pos, log);
            }
            tuft(level, tree, topX, j + height - 2, topZ);
        } else {
            pad(level, tree, random, new BlockPos(topX, j + height - 1, topZ), 2);
        }

        tree.place(level, ModBlocks.PINE_LEAVES.get().defaultBlockState());
        return true;
    }

    /**
     * Where a branch leaving the trunk on its {@code dir} side starts: the
     * block just outside it, anywhere along that face of a trunk {@code width}
     * across.
     */
    private static BlockPos edge(RandomSource random, int x, int y, int z, int width, Direction dir) {
        int along = width > 1 ? random.nextInt(width) : 0;
        int ex = dir.getStepX() > 0 ? x + width : dir.getStepX() < 0 ? x - 1 : x + along;
        int ez = dir.getStepZ() > 0 ? z + width : dir.getStepZ() < 0 ? z - 1 : z + along;
        return new BlockPos(ex, y, ez);
    }

    /**
     * A limb from the trunk at {@code (x, y, z)}, {@code length} blocks out
     * along {@code angle} and rising a block for every two out, laid a block
     * at a time so each log touches the last face to face. Returns its end.
     */
    private static BlockPos limb(WorldGenLevel level, TreeBlocks tree, int x, int y, int z, double angle, int length,
                                 BlockState log) {
        double dx = Math.cos(angle);
        double dz = Math.sin(angle);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        for (int step = 1; step <= length; step++) {
            int tx = x + (int) Math.round(dx * step);
            int tz = z + (int) Math.round(dz * step);
            int ty = y + step / 2;
            while (pos.getX() != tx) {
                pos.move(tx > pos.getX() ? Direction.EAST : Direction.WEST);
                if (TreeFeature.validTreePos(level, pos)) tree.log(pos, log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
            }
            while (pos.getZ() != tz) {
                pos.move(tz > pos.getZ() ? Direction.SOUTH : Direction.NORTH);
                if (TreeFeature.validTreePos(level, pos)) tree.log(pos, log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
            }
            while (pos.getY() != ty) {
                pos.move(Direction.UP);
                if (TreeFeature.validTreePos(level, pos)) tree.log(pos, log);
            }
        }
        return pos.immutable();
    }

    /**
     * A flat, ragged cushion of needles about a limb's end: widest level with
     * it, a narrower layer above, and a sparse fringe hanging below, its edges
     * left gappy.
     */
    private static void pad(WorldGenLevel level, TreeBlocks tree, RandomSource random, BlockPos centre, int radius) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dy = -1; dy <= 2; dy++) {
            double r = switch (dy) {
                case -1 -> radius - 1.0;
                case 0 -> radius + 0.5;
                case 1 -> radius - 0.5;
                default -> radius - 1.5;
            };
            if (r < 0.5) continue;
            int reach = Mth.ceil(r);
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d > r) continue;
                    if (d > r - 1.0 && random.nextInt(3) == 0) continue;
                    if (dy == -1 && random.nextBoolean()) continue;
                    pos.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    if (TreeFeature.validTreePos(level, pos)) tree.leaf(pos);
                }
            }
        }
    }

    /** A diamond of needles one block out from the trunk. */
    private static void tuft(WorldGenLevel level, TreeBlocks tree, int x, int y, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (TreeFeature.validTreePos(level, pos.set(x, y, z).move(dir))) tree.leaf(pos);
        }
        if (TreeFeature.validTreePos(level, pos.set(x, y + 1, z))) tree.leaf(pos);
    }

    /**
     * A root sprawling one or two blocks out over the ground from a thick
     * trunk's {@code dir} side, following the ground down where it falls away.
     */
    private static void root(WorldGenLevel level, TreeBlocks tree, RandomSource random, int i, int j, int k,
                             Direction dir, BlockState log) {
        BlockPos at = edge(random, i, j, k, 2, dir);
        BlockState root = log.setValue(RotatedPillarBlock.AXIS, dir.getAxis());
        int length = 1 + random.nextInt(2);
        for (int l = 0; l < length; l++) {
            if (TreeFeature.validTreePos(level, at.below()) && level.getBlockState(at.below(2)).is(BlockTags.DIRT)) {
                at = at.below();
            }
            if (!TreeFeature.validTreePos(level, at) || !level.getBlockState(at.below()).isSolid()) return;
            tree.log(at, root);
            at = at.relative(dir);
        }
    }
}
