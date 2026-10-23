package com.lotrmod.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The logs and leaves of one tree, gathered while a feature lays it out and
 * then written to the world together.
 *
 * <p>The original mod's tree generators set leaves with their decay flag on and
 * left the game to work out whether a log was near. Here leaves carry a
 * {@link LeavesBlock#DISTANCE} that nothing recomputes during worldgen, so a
 * leaf placed at its default distance of 7 would decay on its first random
 * tick. {@link #place} works the distance out from the logs, as vanilla's
 * {@code TreeFeature} does.
 */
final class TreeBlocks {
    private final Map<BlockPos, BlockState> logs = new LinkedHashMap<>();
    private final Set<BlockPos> leaves = new LinkedHashSet<>();
    /** Leaves that aren't the tree's usual kind, by where they are. */
    private final Map<BlockPos, BlockState> leafStates = new HashMap<>();

    void log(BlockPos pos, BlockState state) {
        BlockPos at = pos.immutable();
        logs.put(at, state);
        leaves.remove(at);
        leafStates.remove(at);
    }

    void leaf(BlockPos pos) {
        BlockPos at = pos.immutable();
        if (!logs.containsKey(at)) leaves.add(at);
    }

    /** A leaf of another kind than the one {@link #place} is given. */
    void leaf(BlockPos pos, BlockState state) {
        BlockPos at = pos.immutable();
        if (logs.containsKey(at)) return;
        leaves.add(at);
        leafStates.put(at, state);
    }

    boolean hasLeaf(BlockPos pos) {
        return leaves.contains(pos);
    }

    void place(WorldGenLevel level, BlockState leafState) {
        place(level, leafState, false);
    }

    /** As {@link #place(WorldGenLevel, BlockState)}, leaving out any leaf too far from the tree's wood to last if {@code dropLoose}. */
    void place(WorldGenLevel level, BlockState leafState, boolean dropLoose) {
        logs.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE));
        leafDistances().forEach((pos, d) -> {
            if (dropLoose && d >= LeavesBlock.DECAY_DISTANCE) return;
            level.setBlock(pos, leafStates.getOrDefault(pos, leafState).setValue(LeavesBlock.DISTANCE, d),
                    Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE);
        });
    }

    /**
     * The part of the tree inside {@code box}, for a tree laid out without
     * looking at the world: a log goes only where there is air, leaves or a
     * plant, and a leaf only into air or leaves, as the original's generators
     * checked block by block.
     */
    void place(WorldGenLevel level, BlockState leafState, BoundingBox box) {
        logs.forEach((pos, state) -> {
            if (box.isInside(pos) && roomForLog(level.getBlockState(pos))) {
                level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            }
        });
        leafDistances().forEach((pos, d) -> {
            if (box.isInside(pos) && roomForLeaf(level.getBlockState(pos))) {
                level.setBlock(pos, leafStates.getOrDefault(pos, leafState).setValue(LeavesBlock.DISTANCE, d),
                        Block.UPDATE_CLIENTS);
            }
        });
    }

    static boolean roomForLog(BlockState state) {
        return roomForLeaf(state) || state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    static boolean roomForLeaf(BlockState state) {
        return state.isAir() || state.is(BlockTags.LEAVES);
    }

    /** Each leaf's distance: steps through leaves to the nearest log, as LeavesBlock counts it. */
    private Map<BlockPos, Integer> leafDistances() {
        Map<BlockPos, Integer> distance = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(logs.keySet());
        logs.keySet().forEach(pos -> distance.put(pos, 0));
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            int next = distance.get(pos) + 1;
            if (next >= LeavesBlock.DECAY_DISTANCE) continue;
            for (Direction dir : Direction.values()) {
                BlockPos side = pos.relative(dir);
                if (leaves.contains(side) && !distance.containsKey(side)) {
                    distance.put(side, next);
                    queue.add(side);
                }
            }
        }

        Map<BlockPos, Integer> result = new LinkedHashMap<>();
        for (BlockPos pos : leaves) {
            result.put(pos, distance.getOrDefault(pos, LeavesBlock.DECAY_DISTANCE));
        }
        return result;
    }

    /** The smallest box holding every log and leaf. */
    BoundingBox bounds() {
        return BoundingBox.encapsulatingPositions(
                Stream.concat(logs.keySet().stream(), leaves.stream())::iterator).orElseThrow();
    }
}
