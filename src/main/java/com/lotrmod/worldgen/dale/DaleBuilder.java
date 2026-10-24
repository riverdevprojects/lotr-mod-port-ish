package com.lotrmod.worldgen.dale;

import com.lotrmod.LOTRMod;
import com.lotrmod.block.ChandelierBlock;
import com.lotrmod.block.DaleBlocks;
import com.lotrmod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Builds Dale's buildings into the world, a chunk at a time, as the town of
 * King Girion's day or as the ruin Smaug left.
 *
 * <h2>As the original built them</h2>
 * The buildings are the original mod's ({@link DaleBuildings}), ported line
 * for line, so this follows its {@code LOTRWorldGenStructureBase2}: {@code i}
 * across, {@code k} from the front back, {@code j} up from the ground block,
 * turned by the rotation (0 to 3, a quarter turn clockwise each), and block
 * facings written as the original's metadata, which reads as world
 * directions for rotation 0, turned with the building. The materials are
 * those its {@code LOTRWorldGenDaleStructure.setupRandomBlocks} rolled,
 * rolled in the same order ({@link #roll}), with vanilla stand-ins for the
 * original's own woods and furniture.
 *
 * <h2>A chunk at a time</h2>
 * Every chunk a building touches replays the whole of it from the same seed
 * and writes only what falls inside the chunk ({@link #box}). Reads outside
 * the chunk see air.
 *
 * <h2>In the Desolation</h2>
 * Unless the building is {@link State#INTACT}, every block passes through
 * {@link #ruin} on its way in. {@link State#BURNT}, where Smaug's fire
 * reached: roofs fall in, timber burns to charred beams or is gone, walls
 * stand to a ragged height, bricks are cracked and mossed over, and the
 * floors are strewn with rubble. {@link State#ABANDONED}, further out, where
 * the people fled and nobody came back: the roof gapes, the timber has rotted
 * away in places, the walls stand higher, and moss has the rest. Each ruin is
 * the same building as before, so what is left stands where it stood.
 */
public final class DaleBuilder {
    /** What has become of a building. */
    public enum State { INTACT, BURNT, ABANDONED }

    final WorldGenLevel level;
    final BoundingBox box;
    final State state;
    /** Whether the building is anything but intact. */
    final boolean ruined;

    private int originX;
    private int originY;
    private int originZ;
    private int rotation;
    private long ruinSeed;

    // The materials, as setupRandomBlocks named them
    BlockState brick;
    Block brickSlab;
    Block brickStairs;
    BlockState brickWall;
    BlockState pillar;
    BlockState floor;
    BlockState plank;
    Block plankSlab;
    Block plankStairs;
    BlockState fence;
    Block fenceGate;
    Block wood;
    Block beam;
    Block door;
    Block trapdoor;
    BlockState roof;
    Block roofSlab;
    Block roofStairs;
    BlockState bars;

    static final ResourceKey<LootTable> HOUSE_LOOT = loot("dale_house");
    static final ResourceKey<LootTable> TREASURE_LOOT = loot("dale_house_treasure");
    static final ResourceKey<LootTable> WATCHTOWER_LOOT = loot("dale_watchtower");
    static final ResourceKey<LootTable> RUINS_LOOT = loot("dale_ruins");
    static final ResourceKey<LootTable> GATE_LOOT = loot("erebor_gate");

    DaleBuilder(WorldGenLevel level, BoundingBox box, State state) {
        this.level = level;
        this.box = box;
        this.state = state;
        this.ruined = state != State.INTACT;
    }

    private static ResourceKey<LootTable> loot(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(LOTRMod.MODID, "chests/" + name));
    }

    /**
     * Sets where the next building stands: its front at (x, z) on ground at
     * {@code groundY}, facing the way {@code rotation} turns it, and its
     * origin {@code shift} blocks back from the front, as the original's
     * {@code setOriginAndRotation}.
     */
    void origin(int x, int groundY, int z, int rotation, int shift) {
        this.rotation = rotation & 3;
        switch (this.rotation) {
            case 0 -> z += shift;
            case 1 -> x -= shift;
            case 2 -> z -= shift;
            default -> x += shift;
        }
        originX = x;
        originY = groundY;
        originZ = z;
        ruinSeed = BlockPos.asLong(x, groundY, z) * 0x9E3779B97F4A7C15L;
    }

    /**
     * The ground a building would cover, placed as {@link #origin} places it,
     * with extents {i0, k0, i1, k1} in its own coordinates: {min x, min z,
     * max x, max z} in the world.
     */
    static int[] footprint(int x, int z, int rotation, int shift, int[] extents) {
        DaleBuilder b = new DaleBuilder(null, null, State.INTACT);
        b.origin(x, 0, z, rotation, shift);
        int x0 = b.worldX(extents[0], extents[1]), z0 = b.worldZ(extents[0], extents[1]);
        int x1 = b.worldX(extents[2], extents[3]), z1 = b.worldZ(extents[2], extents[3]);
        return new int[] {Math.min(x0, x1), Math.min(z0, z1), Math.max(x0, x1), Math.max(z0, z1)};
    }

    /**
     * Places a block by world x and z, and height above the ground, through
     * whatever has become of the building: for building in plain world
     * coordinates, with the origin set at rotation 0.
     */
    void setAt(int x, int j, int z, BlockState state) {
        set(originX - x, j, z - originZ, state);
    }

    BlockState getAt(int x, int j, int z) {
        return get(originX - x, j, z - originZ);
    }

    /** Rolls the materials, as the original's {@code setupRandomBlocks} for its Dale buildings. */
    void roll(RandomSource random) {
        brick = DaleBlocks.DALE_BRICKS.get().defaultBlockState();
        brickSlab = DaleBlocks.DALE_BRICK_SLAB.get();
        brickStairs = DaleBlocks.DALE_BRICK_STAIRS.get();
        brickWall = DaleBlocks.DALE_BRICK_WALL.get().defaultBlockState();
        pillar = DaleBlocks.DALE_PILLAR.get().defaultBlockState();
        floor = Blocks.COBBLESTONE.defaultBlockState();
        switch (random.nextInt(3)) {
            // Spruce, as the original's; pine and fir for its own woods, on
            // the nearest vanilla planks, with the mod's pine and fir logs
            case 0 -> wood(Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_FENCE,
                    Blocks.SPRUCE_FENCE_GATE, Blocks.SPRUCE_LOG, Blocks.SPRUCE_DOOR, Blocks.SPRUCE_TRAPDOOR);
            case 1 -> wood(Blocks.OAK_PLANKS, Blocks.OAK_SLAB, Blocks.OAK_STAIRS, Blocks.OAK_FENCE,
                    Blocks.OAK_FENCE_GATE, ModBlocks.PINE_LOG.get(), Blocks.OAK_DOOR, Blocks.OAK_TRAPDOOR);
            default -> wood(Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_SLAB, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_FENCE,
                    Blocks.DARK_OAK_FENCE_GATE, ModBlocks.FIR_LOG.get(), Blocks.DARK_OAK_DOOR, Blocks.DARK_OAK_TRAPDOOR);
        }
        switch (random.nextInt(4)) {
            case 0 -> roof(DaleBlocks.CLAY_TILE_ORANGE.get(), DaleBlocks.CLAY_TILE_ORANGE_SLAB.get(), DaleBlocks.CLAY_TILE_ORANGE_STAIRS.get());
            case 1 -> roof(DaleBlocks.CLAY_TILE_RED.get(), DaleBlocks.CLAY_TILE_RED_SLAB.get(), DaleBlocks.CLAY_TILE_RED_STAIRS.get());
            case 2 -> roof(DaleBlocks.CLAY_TILE_BROWN.get(), DaleBlocks.CLAY_TILE_BROWN_SLAB.get(), DaleBlocks.CLAY_TILE_BROWN_STAIRS.get());
            default -> roof(DaleBlocks.CLAY_TILE_BLUE.get(), DaleBlocks.CLAY_TILE_BLUE_SLAB.get(), DaleBlocks.CLAY_TILE_BLUE_STAIRS.get());
        }
        // Iron bars, or the original's bronze bars, for which iron stands in too
        random.nextInt(3);
        bars = Blocks.IRON_BARS.defaultBlockState();
        // The plates its tables were laid with, which are left out
        if (random.nextBoolean()) random.nextBoolean();
    }

    private void wood(Block planks, Block slab, Block stairs, Block fence, Block gate, Block log, Block door, Block trapdoor) {
        plank = planks.defaultBlockState();
        plankSlab = slab;
        plankStairs = stairs;
        this.fence = fence.defaultBlockState();
        fenceGate = gate;
        wood = log;
        beam = log;
        this.door = door;
        this.trapdoor = trapdoor;
    }

    private void roof(Block tiles, Block slab, Block stairs) {
        roof = tiles.defaultBlockState();
        roofSlab = slab;
        roofStairs = stairs;
    }

    // ── placement ────────────────────────────────────────────────────────────

    BlockPos pos(int i, int j, int k) {
        return new BlockPos(worldX(i, k), originY + j, worldZ(i, k));
    }

    private int worldX(int i, int k) {
        return switch (rotation) {
            case 0 -> originX - i;
            case 1 -> originX - k;
            case 2 -> originX + i;
            default -> originX + k;
        };
    }

    private int worldZ(int i, int k) {
        return switch (rotation) {
            case 0 -> originZ + k;
            case 1 -> originZ - i;
            case 2 -> originZ - k;
            default -> originZ + i;
        };
    }

    /** Whether this chunk holds the column. */
    boolean columnInBox(int i, int k) {
        return box.isInside(worldX(i, k), box.minY(), worldZ(i, k));
    }

    /** Whether any of the box from (i0, k0) to (i1, k1) falls in this chunk, so a building there is worth replaying. */
    boolean touches(int i0, int k0, int i1, int k1) {
        int x0 = worldX(i0, k0), x1 = worldX(i1, k1), z0 = worldZ(i0, k0), z1 = worldZ(i1, k1);
        return Math.max(x0, x1) >= box.minX() && Math.min(x0, x1) <= box.maxX()
                && Math.max(z0, z1) >= box.minZ() && Math.min(z0, z1) <= box.maxZ();
    }

    void set(int i, int j, int k, BlockState state) {
        BlockPos pos = pos(i, j, k);
        if (!box.isInside(pos)) return;
        if (ruined) state = ruin(state, i, j, k, pos);
        level.setBlock(pos, state, 2);
        // Fences, walls, bars and stairs join their neighbours when the chunk
        // is finished, as in vanilla's structures
        Block block = state.getBlock();
        if (block instanceof CrossCollisionBlock || block instanceof WallBlock || block instanceof StairBlock
                || block instanceof FenceGateBlock) {
            level.getChunk(pos).markPosForPostprocessing(pos);
        }
    }

    void air(int i, int j, int k) {
        set(i, j, k, Blocks.AIR.defaultBlockState());
    }

    BlockState get(int i, int j, int k) {
        BlockPos pos = pos(i, j, k);
        return box.isInside(pos) ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState();
    }

    boolean isOpaque(int i, int j, int k) {
        BlockPos pos = pos(i, j, k);
        return box.isInside(pos) && level.getBlockState(pos).isSolidRender(level, pos);
    }

    /**
     * Fills a column downwards from {@code top}: always down to
     * {@code always}, then on down until it meets solid ground, turning grass
     * smothered beneath to dirt, as the original's foundation loops.
     */
    void fillDown(int i, int top, int always, int k, BlockState state) {
        if (!columnInBox(i, k)) return;
        for (int j = top; originY + j >= level.getMinBuildHeight(); j--) {
            if (j < always && isOpaque(i, j, k)) break;
            set(i, j, k, state);
            grassToDirt(i, j - 1, k);
        }
    }

    void grassToDirt(int i, int j, int k) {
        BlockState state = get(i, j, k);
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM) || state.is(Blocks.PODZOL)) {
            set(i, j, k, Blocks.DIRT.defaultBlockState());
        }
    }

    // ── the original's metadata, turned with the building ───────────────────

    private Direction turn(Direction direction) {
        for (int r = 0; r < rotation; r++) direction = direction.getClockWise();
        return direction;
    }

    /** The original's "direction" values: 0 south, 1 west, 2 north, 3 east. */
    private Direction dir4(int dir) {
        return turn(switch (dir & 3) {
            case 0 -> Direction.SOUTH;
            case 1 -> Direction.WEST;
            case 2 -> Direction.NORTH;
            default -> Direction.EAST;
        });
    }

    /** The original's "facing" values: 2 north, 3 south, 4 west, 5 east. */
    Direction facing(int facing) {
        return turn(switch (facing) {
            case 3 -> Direction.SOUTH;
            case 4 -> Direction.WEST;
            case 5 -> Direction.EAST;
            default -> Direction.NORTH;
        });
    }

    /** Stairs: the low two bits pick the high side (east, west, south, north), 4 hangs them upside down. */
    BlockState stairs(Block block, int meta) {
        Direction facing = turn(switch (meta & 3) {
            case 0 -> Direction.EAST;
            case 1 -> Direction.WEST;
            case 2 -> Direction.SOUTH;
            default -> Direction.NORTH;
        });
        return block.defaultBlockState().setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, (meta & 4) != 0 ? Half.TOP : Half.BOTTOM);
    }

    /** Slabs: 8 sets them in the top of the block. */
    static BlockState slab(Block block, int meta) {
        return block.defaultBlockState().setValue(SlabBlock.TYPE, (meta & 8) != 0 ? SlabType.TOP : SlabType.BOTTOM);
    }

    /** Logs and beams: 4 lays them along i, 8 along k, otherwise upright. */
    BlockState log(Block block, int meta) {
        Direction.Axis axis = (meta & 12) == 4 ? Direction.Axis.X : (meta & 12) == 8 ? Direction.Axis.Z : Direction.Axis.Y;
        if (axis != Direction.Axis.Y && (rotation & 1) == 1) axis = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        return block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    BlockState beam(int meta) {
        return log(beam, meta);
    }

    /** Torches: 5 stands on the floor; 1 to 4 lean east, west, south and north off a wall. */
    BlockState torch(int meta) {
        if (meta == 5) return Blocks.TORCH.defaultBlockState();
        Direction facing = turn(switch (meta) {
            case 1 -> Direction.EAST;
            case 2 -> Direction.WEST;
            case 3 -> Direction.SOUTH;
            default -> Direction.NORTH;
        });
        return Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, facing);
    }

    BlockState ladder(int meta) {
        return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing(meta));
    }

    BlockState furnace(Block block, int meta) {
        return block.defaultBlockState().setValue(FurnaceBlock.FACING, facing(meta));
    }

    BlockState barrel(int meta) {
        return Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, facing(meta));
    }

    /** Trapdoors: the low bits the side they hinge from (north, south, west, east), 4 open, 8 in the top of the block. */
    BlockState trapdoor(Block block, int meta) {
        Direction facing = turn(switch (meta & 3) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.SOUTH;
            case 2 -> Direction.WEST;
            default -> Direction.EAST;
        });
        return block.defaultBlockState().setValue(TrapDoorBlock.FACING, facing)
                .setValue(TrapDoorBlock.OPEN, (meta & 4) != 0)
                .setValue(TrapDoorBlock.HALF, (meta & 8) != 0 ? Half.TOP : Half.BOTTOM);
    }

    /** A door's two halves, from the original's lower-half metadata: 0 facing east, 1 south, 2 west, 3 north. */
    void door(int i, int j, int k, Block block, int meta) {
        Direction facing = turn(switch (meta & 3) {
            case 0 -> Direction.EAST;
            case 1 -> Direction.SOUTH;
            case 2 -> Direction.WEST;
            default -> Direction.NORTH;
        });
        BlockState lower = block.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        set(i, j, k, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(i, j + 1, k, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** Beds: the original's direction, 8 for the head; straw beds are yellow. */
    BlockState bed(int meta) {
        return Blocks.YELLOW_BED.defaultBlockState().setValue(BedBlock.FACING, dir4(meta))
                .setValue(BedBlock.PART, (meta & 8) != 0 ? BedPart.HEAD : BedPart.FOOT);
    }

    BlockState fenceGate(int meta) {
        return fenceGate.defaultBlockState().setValue(FenceGateBlock.FACING, dir4(meta))
                .setValue(FenceGateBlock.OPEN, (meta & 4) != 0);
    }

    /** The banner of Dale, hung on a wall: blue, as its arms and its archers' cloaks. */
    BlockState wallBanner(int dir) {
        // The original's direction is the way the banner faces out from its wall
        return Blocks.BLUE_WALL_BANNER.defaultBlockState().setValue(WallBannerBlock.FACING, dir4(dir));
    }

    /** A hearth with its fire: a campfire, whose smoke rises from the chimney. */
    BlockState hearth() {
        return Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true);
    }

    BlockState chandelier(int meta) {
        return switch (meta) {
            case 1 -> ModBlocks.CHANDELIER_IRON.get().defaultBlockState();
            case 2 -> ModBlocks.CHANDELIER_SILVER.get().defaultBlockState();
            case 3 -> ModBlocks.CHANDELIER_GOLD.get().defaultBlockState();
            default -> ModBlocks.CHANDELIER_BRONZE.get().defaultBlockState();
        };
    }

    void chest(RandomSource random, int i, int j, int k, int meta, ResourceKey<LootTable> loot) {
        long lootSeed = random.nextLong();
        BlockPos pos = pos(i, j, k);
        if (!box.isInside(pos)) return;
        // In the ruins, a chest only now and then survived the fire
        if (state == State.BURNT && Math.floorMod(lootSeed, 3) != 0) {
            air(i, j, k);
            return;
        }
        set(i, j, k, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing(meta)));
        if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity chest) {
            chest.setLootTable(ruined ? RUINS_LOOT : loot, lootSeed);
        }
    }

    // ── the ruin ─────────────────────────────────────────────────────────────

    /** A number from 0 to 1, the same every time for this block of this building. */
    private double roll(BlockPos pos, int salt) {
        long h = (pos.asLong() ^ ruinSeed) * 0xD1B54A32D192ED03L + salt * 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h >>> 11) * 0x1.0p-53;
    }

    /**
     * How high the walls still stand over this column, in blocks above the
     * ground: ragged, two to nine where they burnt, four to eleven where they
     * were only left, clumped two blocks wide so they break away in jagged
     * steps.
     */
    private int standing(int i, int k) {
        BlockPos clump = new BlockPos(Math.floorDiv(worldX(i, k), 2), 0, Math.floorDiv(worldZ(i, k), 2));
        int low = state == State.BURNT ? 2 : 4;
        return low + (int) (roll(clump, 7) * 8.0);
    }

    /** What is left of this block. */
    private BlockState ruin(BlockState state, int i, int j, int k, BlockPos pos) {
        boolean burnt = this.state == State.BURNT;
        BlockState air = Blocks.AIR.defaultBlockState();
        if (state.isAir()) {
            // Rubble on the floors
            if (j == 1 && roll(pos, 1) < (burnt ? 0.14 : 0.06)) return burnt ? rubble(roll(pos, 2)) : overgrown(roll(pos, 2));
            return state;
        }
        if (!state.getFluidState().isEmpty()) return state;
        Block block = state.getBlock();
        double r = roll(pos, 3);
        // Foundations and floors survive
        if (j <= 0) {
            if (state.is(Blocks.COBBLESTONE)) {
                if (burnt) return r < 0.15 ? Blocks.GRAVEL.defaultBlockState() : r < 0.25 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : state;
                return r < 0.35 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : state;
            }
            return state;
        }
        boolean high = j > standing(i, k);

        if (burnt) {
            // Timber burns: beams to charred logs, planks to charred planks, the rest to nothing
            if (state.is(BlockTags.LOGS)) {
                if (high || r < 0.45) return air;
                return DaleBlocks.CHARRED_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
            }
            if (state.is(BlockTags.PLANKS)) return high || r < 0.7 ? air : DaleBlocks.CHARRED_PLANKS.get().defaultBlockState();
            if (isFurnishing(state)) return air;
        } else {
            // Timber rots where the roof let the rain in
            if (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS)) return high && r < 0.6 || r < 0.2 ? air : state;
            if (state.is(BlockTags.WOODEN_DOORS) || state.is(BlockTags.WOOL) || state.is(BlockTags.BANNERS)
                    || state.is(BlockTags.BEDS) || state.is(Blocks.CAKE) || state.is(Blocks.HAY_BLOCK)) {
                return air;
            }
            if (isFurnishing(state)) return high || r < 0.45 ? air : state;
        }
        // Fire, light and glass are gone
        if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH) || state.is(Blocks.CAMPFIRE) || block instanceof ChandelierBlock
                || state.is(Blocks.IRON_BARS) || state.is(BlockTags.FLOWER_POTS) || block instanceof LeavesBlock
                || state.is(BlockTags.FLOWERS) || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.LANTERN)) {
            return air;
        }
        // Roofs have fallen in
        if (isRoof(block)) return burnt ? (r < 0.06 && !high ? state : air) : (r < 0.45 ? state : air);

        // Stone stands, to a ragged height, cracked and grown over
        if (high) return air;
        if (state.is(DaleBlocks.DALE_BRICKS.get())) {
            double cracked = burnt ? 0.35 : 0.15;
            double mossy = burnt ? 0.12 : 0.3;
            return r < cracked ? DaleBlocks.CRACKED_DALE_BRICKS.get().defaultBlockState()
                    : r < cracked + mossy ? DaleBlocks.MOSSY_DALE_BRICKS.get().defaultBlockState() : state;
        }
        if (block instanceof StairBlock || block instanceof SlabBlock || block instanceof WallBlock) {
            return r < 0.25 ? air : state;
        }
        if (state.is(DaleBlocks.DALE_PILLAR.get())) return r < 0.1 ? air : state;
        return state;
    }

    /** Wooden fittings and furnishings: what burns, or rots, first. */
    private static boolean isFurnishing(BlockState state) {
        return state.is(BlockTags.WOODEN_STAIRS) || state.is(BlockTags.WOODEN_SLABS) || state.is(BlockTags.WOODEN_FENCES)
                || state.is(BlockTags.FENCE_GATES) || state.is(BlockTags.WOODEN_DOORS) || state.is(BlockTags.WOODEN_TRAPDOORS)
                || state.is(BlockTags.BEDS) || state.is(BlockTags.WOOL) || state.is(BlockTags.WOOL_CARPETS)
                || state.is(BlockTags.BANNERS) || state.is(Blocks.LADDER) || state.is(Blocks.BARREL)
                || state.is(Blocks.CRAFTING_TABLE) || state.is(Blocks.FLETCHING_TABLE) || state.is(Blocks.CARTOGRAPHY_TABLE)
                || state.is(Blocks.HAY_BLOCK) || state.is(Blocks.CAKE) || state.is(Blocks.BOOKSHELF) || state.is(Blocks.GRINDSTONE);
    }

    /** A block of what lies on the floor of an empty house, long left. */
    static BlockState overgrown(double r) {
        if (r < 0.3) return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        if (r < 0.55) return DaleBlocks.MOSSY_DALE_BRICKS.get().defaultBlockState();
        if (r < 0.75) return Blocks.MOSS_CARPET.defaultBlockState();
        return Blocks.COBBLESTONE.defaultBlockState();
    }

    private boolean isRoof(Block block) {
        return block == DaleBlocks.CLAY_TILE_ORANGE.get() || block == DaleBlocks.CLAY_TILE_RED.get()
                || block == DaleBlocks.CLAY_TILE_BROWN.get() || block == DaleBlocks.CLAY_TILE_BLUE.get()
                || block == roofSlab || block == roofStairs;
    }

    /** A block of what lies on the floor of a burnt house. */
    static BlockState rubble(double r) {
        if (r < 0.25) return Blocks.GRAVEL.defaultBlockState();
        if (r < 0.45) return DaleBlocks.CRACKED_DALE_BRICKS.get().defaultBlockState();
        if (r < 0.6) return DaleBlocks.ASH.get().defaultBlockState();
        if (r < 0.75) return DaleBlocks.CHARRED_PLANKS.get().defaultBlockState();
        if (r < 0.88) return Blocks.COBBLESTONE.defaultBlockState();
        return DaleBlocks.DALE_BRICK_SLAB.get().defaultBlockState();
    }
}
