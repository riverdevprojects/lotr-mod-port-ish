package com.lotrmod.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * {@code lotrmod:fallen_log} — a port of the original mod's {@code LOTRWorldGenLogs}:
 * one time in five a log of two to seven blocks lying east–west, one time in
 * five one lying north–south, and otherwise a stump, a single upright log.
 * Each block needs ground a sapling could grow on, and room.
 *
 * <pre>{"type": "lotrmod:fallen_log", "config": {"log": {...}}}</pre>
 */
public class FallenLogFeature extends Feature<FallenLogFeature.Config> {

    public record Config(BlockState log) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockState.CODEC.fieldOf("log").forGetter(Config::log)
        ).apply(instance, Config::new));
    }

    public FallenLogFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockState log = context.config().log();
        BlockPos origin = context.origin();
        if (!suitable(level, origin)) return false;

        int logType = random.nextInt(5);
        if (logType <= 1) {
            Direction direction = logType == 0 ? Direction.EAST : Direction.SOUTH;
            BlockState lying = log.setValue(RotatedPillarBlock.AXIS, direction.getAxis());
            int length = 2 + random.nextInt(6);
            BlockPos.MutableBlockPos pos = origin.mutable();
            for (int l = 0; l < length && suitable(level, pos); l++) {
                level.setBlock(pos, lying, Block.UPDATE_CLIENTS);
                pos.move(direction);
            }
        } else {
            level.setBlock(origin, log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y), Block.UPDATE_CLIENTS);
        }
        return true;
    }

    private static boolean suitable(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return level.getBlockState(pos.below()).is(BlockTags.DIRT)
                && state.canBeReplaced() && state.getFluidState().isEmpty();
    }
}
