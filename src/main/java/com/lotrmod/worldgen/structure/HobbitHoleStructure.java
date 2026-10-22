package com.lotrmod.worldgen.structure;

import com.lotrmod.worldgen.LOTRWorldGen;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * {@code lotrmod:hobbit_hole} — the original mod's {@code LOTRWorldGenHobbitHole},
 * a smial dug into a grassy mound with a fenced front garden. Which biomes it
 * may stand in is the structure JSON's business: only the heart of the Shire,
 * {@code lotrmod:the_shire}, as in the original, where only the Shire biome
 * itself (not its woodlands, moors or marshes) built hobbit holes.
 *
 * <p>Placement follows the original. A spot is picked in the chunk, and the
 * hole faces one of four ways at random; the spot is its garden gate, and the
 * mound is centred {@value HobbitHolePiece#SHIFT} blocks behind it. The
 * ground within {@value #RADIUS} blocks of the centre must be dry land and
 * vary in height by no more than {@value #MAX_HEIGHT_RANGE} blocks, measured
 * from the gate.
 */
public class HobbitHoleStructure extends Structure {
    public static final MapCodec<HobbitHoleStructure> CODEC = simpleCodec(HobbitHoleStructure::new);

    static final int RADIUS = 16;
    private static final int MAX_HEIGHT_RANGE = 8;
    /** The original tested every column; every fourth is plenty to find a slope or water. */
    private static final int SAMPLE_STEP = 4;

    public HobbitHoleStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        WorldgenRandom random = context.random();
        int gateX = chunkPos.getMinBlockX() + random.nextInt(16);
        int gateZ = chunkPos.getMinBlockZ() + random.nextInt(16);
        int rotation = random.nextInt(4);
        long seed = random.nextLong();

        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor heightAccessor = context.heightAccessor();
        RandomState randomState = context.randomState();

        // The ground the gate stands on is the floor of the whole hole
        int y = generator.getBaseHeight(gateX, gateZ, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState) - 1;
        BlockPos centre = HobbitHolePiece.centre(gateX, y, gateZ, rotation);

        // Check the biome before the ground, which is far dearer to test
        Holder<Biome> biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(centre.getX()),
                QuartPos.fromBlock(centre.getY()), QuartPos.fromBlock(centre.getZ()), randomState.sampler());
        if (!context.validBiome().test(biome)) return Optional.empty();
        if (!groundFits(generator, heightAccessor, randomState, centre)) return Optional.empty();

        return Optional.of(new GenerationStub(centre,
                builder -> builder.addPiece(new HobbitHolePiece(centre, rotation, seed))));
    }

    private static boolean groundFits(ChunkGenerator generator, LevelHeightAccessor heightAccessor,
            RandomState randomState, BlockPos centre) {
        // Rivers and marsh pools are cut below sea level and only filled with
        // water later, so ground that low is under water by the time the hole
        // is built
        int lowestDry = generator.getSeaLevel() - 1;
        int minHeight = 0;
        int maxHeight = 0;
        for (int i = -RADIUS; i <= RADIUS; i += SAMPLE_STEP) {
            for (int k = -RADIUS; k <= RADIUS; k += SAMPLE_STEP) {
                if (i * i + k * k > RADIUS * RADIUS) continue;
                int x = centre.getX() + i;
                int z = centre.getZ() + k;
                NoiseColumn column = generator.getBaseColumn(x, z, heightAccessor, randomState);
                int top = topBlock(column, heightAccessor);
                if (top == Integer.MIN_VALUE || top < lowestDry) return false;
                int height = top - centre.getY();
                minHeight = Math.min(minHeight, height);
                maxHeight = Math.max(maxHeight, height);
            }
        }
        return maxHeight - minHeight <= MAX_HEIGHT_RANGE;
    }

    /** The y of the column's top block, or {@link Integer#MIN_VALUE} if it is water. */
    private static int topBlock(NoiseColumn column, LevelHeightAccessor heightAccessor) {
        for (int y = heightAccessor.getMaxBuildHeight() - 1; y >= heightAccessor.getMinBuildHeight(); y--) {
            BlockState state = column.getBlock(y);
            if (state.isAir()) continue;
            return state.getFluidState().isEmpty() ? y : Integer.MIN_VALUE;
        }
        return Integer.MIN_VALUE;
    }

    @Override
    public StructureType<?> type() {
        return LOTRWorldGen.HOBBIT_HOLE.get();
    }
}
