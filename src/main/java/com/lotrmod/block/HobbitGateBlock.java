package com.lotrmod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A port of the original mod's {@code LOTRBlockGate}: a thin panel that joins
 * up with the gates beside, above and below it into one picture (a hobbit
 * hole's round front door is four of them), and opens and shuts as a whole
 * run when one is clicked or powered.
 *
 * <p>The picture is layered the way the original's {@code LOTRConnectedTextures}
 * layered it: the base tile, then an edge tile along each side with no
 * joining gate, a corner tile where two such edges meet, and an inner-corner
 * tile where the gates either side join but the one on the diagonal doesn't.
 * An open gate leaves out the base tile, so only its frame shows. The eight
 * joining flags are the block state, read by a multipart blockstate; left and
 * right are as seen by someone looking at the gate's front, its
 * {@link #FACING} side, and the back face shows the same picture mirrored.
 *
 * <p>Which gates join follows the original too: the same gate, in the same
 * plane, open or shut alike. Open gates join any open gate, and an open gate
 * with no gate below it counts its bottom as joined, so its frame stops at the
 * ground.
 */
public class HobbitGateBlock extends Block {

    public static final MapCodec<HobbitGateBlock> CODEC = simpleCodec(HobbitGateBlock::new);

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    public static final BooleanProperty UP_LEFT = BooleanProperty.create("up_left");
    public static final BooleanProperty UP_RIGHT = BooleanProperty.create("up_right");
    public static final BooleanProperty DOWN_LEFT = BooleanProperty.create("down_left");
    public static final BooleanProperty DOWN_RIGHT = BooleanProperty.create("down_right");

    /** How far the original let a click spread through a run of gates. */
    private static final int MAX_GATE_RANGE = 16;

    public HobbitGateBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH).setValue(OPEN, false).setValue(POWERED, false)
                .setValue(UP, false).setValue(DOWN, false).setValue(LEFT, false).setValue(RIGHT, false)
                .setValue(UP_LEFT, false).setValue(UP_RIGHT, false)
                .setValue(DOWN_LEFT, false).setValue(DOWN_RIGHT, false));
    }

    @Override
    protected MapCodec<HobbitGateBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, POWERED, UP, DOWN, LEFT, RIGHT, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        boolean powered = ctx.getLevel().hasNeighborSignal(pos);
        BlockState state = defaultBlockState().setValue(FACING, ctx.getHorizontalDirection())
                .setValue(OPEN, powered).setValue(POWERED, powered);
        return withConnections(ctx.getLevel(), pos, state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                      LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return withConnections(level, pos, state);
    }

    // Shape updates only reach the six direct neighbours, never the diagonal
    // gates whose inner corners depend on this one, so repaint those by hand
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!oldState.is(state.getBlock())) {
            refreshNeighbors(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            refreshNeighbors(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private static void refreshNeighbors(Level level, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos neighborPos = pos.offset(dx, dy, dz);
                    BlockState neighborState = level.getBlockState(neighborPos);
                    if (neighborState.getBlock() instanceof HobbitGateBlock) {
                        BlockState updated = withConnections(level, neighborPos, neighborState);
                        if (updated != neighborState) {
                            level.setBlock(neighborPos, updated, 3);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), 2);
            if (powered != state.getValue(OPEN)) {
                toggleConnectedRun(level, pos, state, powered);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            toggleConnectedRun(level, pos, state, !state.getValue(OPEN));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void toggleConnectedRun(Level level, BlockPos pos, BlockState state, boolean open) {
        List<BlockPos> run = getConnectedGates(level, pos, state);
        for (BlockPos p : run) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof HobbitGateBlock) {
                level.setBlock(p, s.setValue(OPEN, open), 2);
            }
        }
        // Repaint once the whole run has turned, so every gate sees its
        // neighbours' new state
        for (BlockPos p : run) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof HobbitGateBlock) {
                level.setBlock(p, withConnections(level, p, s), 3);
            }
        }
        level.playSound(null, pos, open ? SoundEvents.FENCE_GATE_OPEN : SoundEvents.FENCE_GATE_CLOSE,
                SoundSource.BLOCKS, 1.0f, 0.8f + level.random.nextFloat() * 0.4f);
    }

    // ── Joining up ───────────────────────────────────────────────────────────

    /** The gate's state with its joining flags worked out from the gates around it. */
    public static BlockState withConnections(BlockGetter level, BlockPos pos, BlockState state) {
        return withConnections(pos, state, other -> joins(level, pos, state, other));
    }

    /**
     * The gate's state with its joining flags taken from {@code joins}, which
     * says whether the gate would join one at a given position. A structure
     * built a chunk at a time uses this to work its gates out from its own
     * layout, since the next chunk may not be built yet.
     */
    public static BlockState withConnections(BlockPos pos, BlockState state, Predicate<BlockPos> joins) {
        Direction left = state.getValue(FACING).getClockWise();
        Direction right = left.getOpposite();
        return state
                .setValue(UP, joins.test(pos.above()))
                .setValue(DOWN, joins.test(pos.below()))
                .setValue(LEFT, joins.test(pos.relative(left)))
                .setValue(RIGHT, joins.test(pos.relative(right)))
                .setValue(UP_LEFT, joins.test(pos.above().relative(left)))
                .setValue(UP_RIGHT, joins.test(pos.above().relative(right)))
                .setValue(DOWN_LEFT, joins.test(pos.below().relative(left)))
                .setValue(DOWN_RIGHT, joins.test(pos.below().relative(right)));
    }

    /** {@code LOTRBlockGate.areBlocksConnected}, for a position in the gate's own plane. */
    private static boolean joins(BlockGetter level, BlockPos pos, BlockState state, BlockPos otherPos) {
        BlockState other = level.getBlockState(otherPos);
        boolean open = state.getValue(OPEN);
        boolean otherIsGate = other.getBlock() instanceof HobbitGateBlock;
        if (open && otherPos.equals(pos.below()) && !otherIsGate) return true;
        return sameRun(state, other);
    }

    /** Whether two gates belong to one run: they join up and open and shut together. */
    private static boolean sameRun(BlockState state, BlockState other) {
        if (!(other.getBlock() instanceof HobbitGateBlock)) return false;
        boolean open = state.getValue(OPEN);
        if (!open && other.getBlock() != state.getBlock()) return false;
        return other.getValue(FACING).getAxis() == state.getValue(FACING).getAxis()
                && other.getValue(OPEN) == open;
    }

    /** The run of gates a click on the one at {@code origin} opens or shuts. */
    private static List<BlockPos> getConnectedGates(Level level, BlockPos origin, BlockState state) {
        Direction side = state.getValue(FACING).getClockWise();
        Set<BlockPos> all = new HashSet<>();
        Deque<BlockPos> frontier = new ArrayDeque<>();
        all.add(origin);
        frontier.add(origin);
        for (int depth = 0; depth < MAX_GATE_RANGE && !frontier.isEmpty(); depth++) {
            Deque<BlockPos> next = new ArrayDeque<>();
            for (BlockPos p : frontier) {
                for (Direction d : new Direction[]{Direction.UP, Direction.DOWN, side, side.getOpposite()}) {
                    BlockPos np = p.relative(d);
                    if (all.contains(np)) continue;
                    if (sameRun(state, level.getBlockState(np))) {
                        all.add(np);
                        next.add(np);
                    }
                }
            }
            frontier = next;
        }
        return new ArrayList<>(all);
    }

    // ── Shape: a thin panel, which can be walked through when open ─────────

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        boolean alongX = state.getValue(FACING).getAxis() == Direction.Axis.X;
        return alongX ? Shapes.box(0.375, 0.0, 0.0, 0.625, 1.0, 1.0) : Shapes.box(0.0, 0.0, 0.375, 1.0, 1.0, 0.625);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(OPEN) ? Shapes.empty() : getShape(state, level, pos, ctx);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return state.getValue(OPEN);
    }
}
