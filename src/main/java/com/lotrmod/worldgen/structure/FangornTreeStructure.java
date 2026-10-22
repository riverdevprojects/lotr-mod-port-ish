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
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;

/**
 * {@code lotrmod:fangorn_tree} — Fangorn's giant oaks and beeches (see
 * {@link com.lotrmod.worldgen.feature.FangornTree}), grown as a structure so
 * their crowns, which spread far past what a feature may write, come out
 * whole.
 *
 * <p>The original rolled them among Fangorn's other trees, an oak five times
 * in 121 and a beech twice. Here each start grows one, and a second some way
 * off one time in three; the structure set decides how often starts come.
 * Like the original's, a tree needs dry ground under its trunk.
 */
public class FangornTreeStructure extends Structure {
    public static final MapCodec<FangornTreeStructure> CODEC = simpleCodec(FangornTreeStructure::new);

    public FangornTreeStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        WorldgenRandom random = context.random();
        int x = chunkPos.getMinBlockX() + random.nextInt(16);
        int z = chunkPos.getMinBlockZ() + random.nextInt(16);
        BlockPos first = groundAt(context, x, z);
        if (first == null) return Optional.empty();

        BlockPos second = null;
        if (random.nextInt(3) == 0) {
            float angle = random.nextFloat() * 2.0F * (float) Math.PI;
            int distance = 20 + random.nextInt(12);
            second = groundAt(context, x + Math.round((float) Math.cos(angle) * distance),
                    z + Math.round((float) Math.sin(angle) * distance));
        }

        long seed = random.nextLong();
        BlockPos secondTree = second;
        return Optional.of(new GenerationStub(first, builder -> {
            addTree(builder, first, seed);
            if (secondTree != null) addTree(builder, secondTree, seed * 31L + 1L);
        }));
    }

    private static void addTree(StructurePiecesBuilder builder, BlockPos pos, long seed) {
        // An oak five times in seven, a beech otherwise, as the original's weights
        boolean beech = Math.floorMod(seed, 7L) < 2;
        builder.addPiece(new FangornTreePiece(pos, beech, seed));
    }

    /** Where a trunk standing at x, z would start, or null if it would be in water or out of Fangorn. */
    private static BlockPos groundAt(GenerationContext context, int x, int z) {
        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor heightAccessor = context.heightAccessor();
        RandomState randomState = context.randomState();

        int y = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState);
        Holder<Biome> biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y),
                QuartPos.fromBlock(z), randomState.sampler());
        if (!context.validBiome().test(biome)) return null;

        // Rivers are cut below sea level and only filled with water later
        if (y - 1 < generator.getSeaLevel()) return null;
        NoiseColumn column = generator.getBaseColumn(x, z, heightAccessor, randomState);
        if (!column.getBlock(y - 1).getFluidState().isEmpty()) return null;
        return new BlockPos(x, y, z);
    }

    @Override
    public StructureType<?> type() {
        return LOTRWorldGen.FANGORN_TREE.get();
    }
}
