package com.lotrmod.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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

    void log(BlockPos pos, BlockState state) {
        BlockPos at = pos.immutable();
        logs.put(at, state);
        leaves.remove(at);
    }

    void leaf(BlockPos pos) {
        BlockPos at = pos.immutable();
        if (!logs.containsKey(at)) leaves.add(at);
    }

    boolean hasLeaf(BlockPos pos) {
        return leaves.contains(pos);
    }

    void place(WorldGenLevel level, BlockState leafState) {
        logs.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE));

        // Distance is steps through leaves to the nearest log, as LeavesBlock counts it
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

        for (BlockPos pos : leaves) {
            int d = distance.getOrDefault(pos, LeavesBlock.DECAY_DISTANCE);
            level.setBlock(pos, leafState.setValue(LeavesBlock.DISTANCE, d), Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE);
        }
    }
}
