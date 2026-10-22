package com.lotrmod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * {@code lotrmod:dead_tree} — a port of the original mod's {@code LOTRWorldGenDeadTrees}:
 * a bare trunk three to six tall, with four leafless branches of wood (bark on
 * every side) climbing out from its top, one to each side.
 *
 * <pre>{"type": "lotrmod:dead_tree", "config": {"log": {...}, "wood": {...}}}</pre>
 */
public class DeadTreeFeature extends Feature<DeadTreeFeature.Config> {

    public record Config(BlockState log, BlockState wood) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockState.CODEC.fieldOf("log").forGetter(Config::log),
                BlockState.CODEC.fieldOf("wood").forGetter(Config::wood)
        ).apply(instance, Config::new));
    }

    public DeadTreeFeature(Codec<Config> codec) {
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

        // Ground a sapling could grow on, or bare stone, sand or gravel
        BlockState below = level.getBlockState(origin.below());
        if (!below.is(BlockTags.DIRT) && !below.is(Blocks.STONE) && !below.is(BlockTags.SAND) && !below.is(Blocks.GRAVEL)) {
            return false;
        }
        if (below.is(Blocks.GRASS_BLOCK)) {
            level.setBlock(origin.below(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
        }

        int height = 3 + random.nextInt(4);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int j1 = j; j1 < j + height; j1++) {
            place(level, pos.set(i, j1, k), config.log());
        }
        for (int branch = 0; branch < 4; branch++) {
            int branchLength = 3 + random.nextInt(5);
            int branchHorizontalPos = 0;
            int branchVerticalPos = j + height - 1 - random.nextInt(2);
            for (int l = 0; l < branchLength; l++) {
                if (random.nextInt(4) == 0) branchHorizontalPos++;
                if (random.nextInt(3) != 0) branchVerticalPos++;
                switch (branch) {
                    case 0 -> pos.set(i - branchHorizontalPos, branchVerticalPos, k);
                    case 1 -> pos.set(i, branchVerticalPos, k + branchHorizontalPos);
                    case 2 -> pos.set(i + branchHorizontalPos, branchVerticalPos, k);
                    default -> pos.set(i, branchVerticalPos, k - branchHorizontalPos);
                }
                place(level, pos, config.wood());
            }
        }
        return true;
    }

    private static void place(WorldGenLevel level, BlockPos pos, BlockState state) {
        BlockState there = level.getBlockState(pos);
        if (there.canBeReplaced() || there.is(BlockTags.LOGS) || there.is(BlockTags.LEAVES)) {
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
    }
}
