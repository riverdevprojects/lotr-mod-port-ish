package com.lotrmod.worldgen.structure;

import com.lotrmod.block.ModBlocks;
import com.lotrmod.worldgen.LOTRWorldGen;
import com.lotrmod.worldgen.feature.FangornTree;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * One giant tree of Fangorn, laid out again from its seed for each chunk it
 * reaches into and written there.
 */
public class FangornTreePiece extends StructurePiece {
    private final int originX;
    private final int originY;
    private final int originZ;
    private final boolean beech;
    private final long seed;
    /** The tree, laid out once per piece rather than once per chunk. */
    private FangornTree tree;

    public FangornTreePiece(BlockPos origin, boolean beech, long seed) {
        this(origin, beech, seed, grow(origin, beech, seed));
    }

    private FangornTreePiece(BlockPos origin, boolean beech, long seed, FangornTree tree) {
        super(LOTRWorldGen.FANGORN_TREE_PIECE.get(), 0, tree.bounds());
        this.originX = origin.getX();
        this.originY = origin.getY();
        this.originZ = origin.getZ();
        this.beech = beech;
        this.seed = seed;
        this.tree = tree;
    }

    public FangornTreePiece(CompoundTag tag) {
        super(LOTRWorldGen.FANGORN_TREE_PIECE.get(), tag);
        this.originX = tag.getInt("OX");
        this.originY = tag.getInt("OY");
        this.originZ = tag.getInt("OZ");
        this.beech = tag.getBoolean("Beech");
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("OX", originX);
        tag.putInt("OY", originY);
        tag.putInt("OZ", originZ);
        tag.putBoolean("Beech", beech);
        tag.putLong("Seed", seed);
    }

    private static FangornTree grow(BlockPos origin, boolean beech, long seed) {
        BlockState log = beech ? ModBlocks.BEECH_LOG.get().defaultBlockState() : Blocks.OAK_LOG.defaultBlockState();
        BlockState wood = beech ? ModBlocks.BEECH_WOOD.get().defaultBlockState() : Blocks.OAK_WOOD.defaultBlockState();
        BlockState leaves = beech ? ModBlocks.BEECH_LEAVES.get().defaultBlockState() : Blocks.OAK_LEAVES.defaultBlockState();
        return new FangornTree(origin, RandomSource.create(seed),
                log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y), wood, leaves);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
            RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        if (tree == null) tree = grow(new BlockPos(originX, originY, originZ), beech, seed);
        tree.place(level, chunkBox);
    }
}
