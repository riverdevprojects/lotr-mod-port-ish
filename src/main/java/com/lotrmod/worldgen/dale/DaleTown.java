package com.lotrmod.worldgen.dale;

import com.lotrmod.block.DaleBlocks;
import com.lotrmod.worldgen.DaleTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/**
 * The town of Dale, in the loop of the River Running under the Mountain.
 *
 * <p>It is walled round, the wall three blocks thick and eight high, with a
 * tower every few dozen blocks and a gate to the north, towards the Mountain
 * and the Front Gate, to the south, over the river and down to the Long Lake,
 * and to the west, out to Ravenhill. Inside, broad streets run in from the
 * gates to a market square, ringed by stalls about the original's market
 * pavilion, with the great bell-tower of Dale beside it and King Girion's hall
 * (the original's Dale fortress) at the head of the north street. Lanes run
 * parallel to the great streets, and a street runs round inside the wall,
 * and the houses, smithies and bakeries of the original's Dale fill the
 * blocks between them, close packed, two rows back to back, each facing its
 * street; here and there a garden is left. Lamps light the streets.
 *
 * <p>In the Desolation it is the same town, burnt ({@link DaleBuilder.State#BURNT}):
 * the wall broken, the houses roofless shells, the streets choked with
 * rubble and ash, and the bells of Dale lying where they fell.
 *
 * <p>The layout is fixed, the same in every world: there was only ever the
 * one Dale. {@link #build} builds what of it falls in one chunk.
 */
final class DaleTown {
    private DaleTown() {}

    /** The town's ground, the top block of its levelled rise. */
    static final int GROUND = DaleTerrain.TOWN_Y - 1;

    static final double SQUARE = 24.0;
    /** The street running round just inside the wall. */
    static final double RING = 82.0;
    /** The lanes: parallel to the great streets, this far out from them either way. */
    static final int[] LANES = {32, 62};
    static final double WALL_IN = 89.0;
    static final double WALL_OUT = 92.5;
    static final int WALL_HEIGHT = 8;
    /** Half the width of the great streets, and of the lanes parallel to them. */
    static final double STREET = 3.0;
    static final double LANE = 2.0;
    /** The gates: north, south and west, as compass angles (0 east, 90 south). */
    static final double[] GATES = {270.0, 90.0, 180.0};
    /** Half the width of a gateway. */
    static final double GATEWAY = 2.5;

    enum Kind { HOUSE, SMITHY, BAKERY, PAVILION, TOWER, HALL, BELFRY }

    record Building(Kind kind, int x, int z, int rotation, long seed, int[] footprint) {}

    static final List<Building> BUILDINGS = layout();

    /** Everything from the town centre out to where the roads leave the gates. */
    static final double REACH = WALL_OUT + 12.0;

    // ── the plan ─────────────────────────────────────────────────────────────

    private static List<Building> layout() {
        RandomSource random = RandomSource.create(2770L);
        List<Building> out = new ArrayList<>();
        int cx = DaleTerrain.TOWN_X, cz = DaleTerrain.TOWN_Z;

        // The market pavilion in the middle of the square, and the bell-tower beside it
        add(out, Kind.PAVILION, cx, cz - 3, 0, random, DaleBuildings.TOWER, 3);
        out.add(new Building(Kind.BELFRY, cx + 14, cz + 14, 0, random.nextLong(), new int[] {cx + 11, cz + 11, cx + 17, cz + 17}));
        // King Girion's hall, its gate facing south onto the square
        add(out, Kind.HALL, cx, cz - 28, 2, random, DaleBuildings.FORTRESS, 12);

        // Towers round the wall, and either side of each gate
        List<Double> towers = new ArrayList<>();
        for (double gate : GATES) {
            towers.add(gate - 11.0);
            towers.add(gate + 11.0);
        }
        for (double a : new double[] {0.0, 45.0, 135.0, 225.0, 315.0, 30.0, 330.0}) towers.add(a);
        for (double a : towers) {
            double r = (WALL_IN + WALL_OUT) / 2.0;
            int x = cx + (int) Math.round(Math.cos(Math.toRadians(a)) * r);
            int z = cz + (int) Math.round(Math.sin(Math.toRadians(a)) * r);
            // The tower's door faces into the town
            int rotation = facing(a + 180.0);
            int[] spot = front(x, z, rotation, 3);
            add(out, Kind.TOWER, spot[0], spot[1], rotation, random, DaleBuildings.WATCHTOWER, 3);
        }

        // The houses, smithies and bakeries, packed along every street
        pack(out, random);
        return out;
    }

    /**
     * Packs the town with buildings: every column beside a street could be
     * a front door, and each is tried in turn, from the middle of the town
     * out, with the building turned to face the street, and kept if it fits.
     * So the blocks between the streets fill up with two rows of buildings
     * back to back, an alley between their gables. Smithies and bakeries
     * stand nearest the square, houses everywhere.
     */
    private static void pack(List<Building> out, RandomSource random) {
        int cx = DaleTerrain.TOWN_X, cz = DaleTerrain.TOWN_Z;
        int reach = (int) WALL_IN;
        for (int ring = 0; ring <= reach; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    int x = cx + dx, z = cz + dz;
                    if (street(x, z) != Street.NONE || radius(x, z) > WALL_IN - 3.0) continue;
                    for (int rotation = 0; rotation < 4; rotation++) {
                        int[] ahead = ahead(x, z, rotation, 1);
                        if (street(ahead[0], ahead[1]) == Street.NONE) continue;
                        Kind kind = Kind.HOUSE;
                        if (ring < 45) {
                            int roll = random.nextInt(6);
                            kind = roll == 0 ? Kind.SMITHY : roll == 1 ? Kind.BAKERY : Kind.HOUSE;
                        }
                        int[] footprint = DaleBuilder.footprint(x, z, rotation, shift(kind), extents(kind));
                        if (!fits(out, footprint) && kind != Kind.HOUSE) {
                            kind = Kind.HOUSE;
                            footprint = DaleBuilder.footprint(x, z, rotation, shift(kind), extents(kind));
                        }
                        if (fits(out, footprint)) {
                            out.add(new Building(kind, x, z, rotation, random.nextLong(), footprint));
                            break;
                        }
                    }
                }
            }
        }
    }

    /** The column {@code n} blocks out of the front door of a building at (x, z) turned this way. */
    private static int[] ahead(int x, int z, int rotation, int n) {
        return switch (rotation & 3) {
            case 0 -> new int[] {x, z - n};
            case 1 -> new int[] {x + n, z};
            case 2 -> new int[] {x, z + n};
            default -> new int[] {x - n, z};
        };
    }

    private static void add(List<Building> out, Kind kind, int x, int z, int rotation, RandomSource random, int[] extents, int shift) {
        out.add(new Building(kind, x, z, rotation, random.nextLong(), DaleBuilder.footprint(x, z, rotation, shift, extents)));
    }

    private static int[] extents(Kind kind) {
        return switch (kind) {
            case SMITHY -> DaleBuildings.SMITHY;
            case BAKERY -> DaleBuildings.BAKERY;
            default -> DaleBuildings.HOUSE;
        };
    }

    private static int shift(Kind kind) {
        return kind == Kind.HOUSE ? 1 : 2;
    }

    /** The spot in front of a building's door whose origin, {@code shift} back, is at (x, z). */
    private static int[] front(int x, int z, int rotation, int shift) {
        return switch (rotation & 3) {
            case 0 -> new int[] {x, z - shift};
            case 1 -> new int[] {x + shift, z};
            case 2 -> new int[] {x, z + shift};
            default -> new int[] {x - shift, z};
        };
    }

    /** The rotation whose building faces (out of its front door) towards the compass angle. */
    private static int facing(double angle) {
        int quadrant = Math.floorMod((int) Math.round(angle / 90.0), 4);
        // Rotation 0 faces north (270), 1 east (0), 2 south (90), 3 west (180)
        return switch (quadrant) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 3;
            default -> 0;
        };
    }

    /** Whether a footprint is clear of the streets and the wall, and a block clear of every building so far. */
    private static boolean fits(List<Building> out, int[] f) {
        for (int x = f[0]; x <= f[2]; x++) {
            for (int z = f[1]; z <= f[3]; z++) {
                if (street(x, z) != Street.NONE) return false;
                if (radius(x, z) > WALL_IN - 3.0) return false;
            }
        }
        for (Building b : out) {
            int[] g = b.footprint();
            if (f[0] - 1 <= g[2] && f[2] + 1 >= g[0] && f[1] - 1 <= g[3] && f[3] + 1 >= g[1]) return false;
        }
        return true;
    }

    // ── where things are ────────────────────────────────────────────────────

    enum Street { NONE, SQUARE, GREAT, LANE, RING }

    static double radius(double x, double z) {
        return DaleTerrain.townDistance(x, z);
    }

    /** What sort of street, if any, this column of the town is. */
    static Street street(int x, int z) {
        double dx = x - DaleTerrain.TOWN_X, dz = z - DaleTerrain.TOWN_Z;
        double r = Math.sqrt(dx * dx + dz * dz);
        if (r < SQUARE) return Street.SQUARE;
        if (r >= WALL_IN) return Street.NONE;
        // The great streets, crossing at the square, out to the gates
        if (Math.abs(dx) <= STREET || Math.abs(dz) <= STREET) return Street.GREAT;
        if (r >= RING) return Street.RING;
        // The lanes, parallel to them
        for (int lane : LANES) {
            if (Math.abs(Math.abs(dx) - lane) <= LANE || Math.abs(Math.abs(dz) - lane) <= LANE) return Street.LANE;
        }
        return Street.NONE;
    }

    /** Whether a column is in the wall, and not a gateway through it. */
    static boolean wall(int x, int z) {
        double r = radius(x, z);
        if (r < WALL_IN || r > WALL_OUT) return false;
        return !gateway(x, z);
    }

    static boolean gateway(int x, int z) {
        double dx = x - DaleTerrain.TOWN_X, dz = z - DaleTerrain.TOWN_Z;
        for (double gate : GATES) {
            double ux = Math.cos(Math.toRadians(gate)), uz = Math.sin(Math.toRadians(gate));
            double along = dx * ux + dz * uz;
            double across = Math.abs(-dx * uz + dz * ux);
            if (along > 0 && across <= GATEWAY) return true;
        }
        return false;
    }

    // ── building it ──────────────────────────────────────────────────────────

    /** Builds what of the town falls in {@code box}, as it is in {@code state}. */
    static void build(WorldGenLevel level, BoundingBox box, DaleBuilder.State state) {
        int cx = DaleTerrain.TOWN_X, cz = DaleTerrain.TOWN_Z;
        if (box.maxX() < cx - REACH || box.minX() > cx + REACH || box.maxZ() < cz - REACH || box.minZ() > cz + REACH) return;

        DaleBuilder ground = new DaleBuilder(level, box, state);
        ground.origin(cx, GROUND, cz, 0, 0);
        ground.roll(RandomSource.create(1L));
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                groundAndWall(ground, x, z, state);
            }
        }

        for (Building b : BUILDINGS) {
            int[] f = b.footprint();
            if (f[2] + 1 < box.minX() || f[0] - 1 > box.maxX() || f[3] + 1 < box.minZ() || f[1] - 1 > box.maxZ()) continue;
            DaleBuilder builder = new DaleBuilder(level, box, state);
            RandomSource random = RandomSource.create(b.seed());
            switch (b.kind()) {
                case HOUSE -> DaleBuildings.house(builder, random, b.x(), GROUND, b.z(), b.rotation());
                case SMITHY -> DaleBuildings.smithy(builder, random, b.x(), GROUND, b.z(), b.rotation());
                case BAKERY -> DaleBuildings.bakery(builder, random, b.x(), GROUND, b.z(), b.rotation());
                case PAVILION -> DaleBuildings.tower(builder, random, b.x(), GROUND, b.z(), b.rotation());
                case TOWER -> DaleBuildings.watchtower(builder, random, b.x(), GROUND, b.z(), b.rotation());
                case HALL -> DaleBuildings.fortress(builder, random, b.x(), GROUND, b.z(), b.rotation());
                case BELFRY -> belfry(builder, random, b.x(), b.z(), state);
            }
        }

        DaleBuilder stalls = new DaleBuilder(level, box, state);
        stalls.origin(cx, GROUND, cz, 0, 0);
        stalls.roll(RandomSource.create(2L));
        stalls(stalls, state);
    }

    /** One column of the town's ground: street, wall, or garden. */
    private static void groundAndWall(DaleBuilder b, int x, int z, DaleBuilder.State state) {
        double r = radius(x, z);
        if (r > REACH) return;
        boolean ruined = state != DaleBuilder.State.INTACT;
        double noise = hash(x, z, 11);

        if (wall(x, z)) {
            BlockState brick = DaleBlocks.DALE_BRICKS.get().defaultBlockState();
            // Down to solid ground, and up to the wall-walk
            for (int j = 0; GROUND + j > b.level.getMinBuildHeight(); j--) {
                if (j < 0 && b.getAt(x, j, z).blocksMotion()) break;
                b.setAt(x, j, z, brick);
            }
            for (int j = 1; j < WALL_HEIGHT; j++) b.setAt(x, j, z, brick);
            // The wall-walk, paved, and battlements along its outer edge
            b.setAt(x, WALL_HEIGHT, z, DaleBlocks.CHISELED_DALE_BRICKS.get().defaultBlockState());
            if (r > WALL_OUT - 1.0 && Math.floorMod((int) Math.round(Math.atan2(z - DaleTerrain.TOWN_Z, x - DaleTerrain.TOWN_X) * WALL_OUT / 2.0), 2) == 0) {
                b.setAt(x, WALL_HEIGHT + 1, z, brick);
                b.setAt(x, WALL_HEIGHT + 2, z, DaleBlocks.DALE_BRICK_SLAB.get().defaultBlockState());
            }
            return;
        }
        if (r >= WALL_IN && r <= WALL_OUT + 0.5) {
            // A gateway: an arch over the road
            for (int j = 1; j <= 5; j++) b.setAt(x, j, z, Blocks.AIR.defaultBlockState());
            for (int j = 6; j <= WALL_HEIGHT; j++) b.setAt(x, j, z, DaleBlocks.DALE_BRICKS.get().defaultBlockState());
            b.setAt(x, 0, z, paving(noise, ruined, true));
            return;
        }
        if (r > WALL_OUT) return;

        // Close any cave mouth that opened in the town's rise
        if (b.getAt(x, 0, z).isAir()) {
            b.setAt(x, 0, z, Blocks.GRASS_BLOCK.defaultBlockState());
            for (int j = -1; j > -24 && b.getAt(x, j, z).isAir(); j--) b.setAt(x, j, z, Blocks.DIRT.defaultBlockState());
        }
        Street street = street(x, z);
        if (street == Street.NONE) return;
        b.setAt(x, 0, z, street == Street.SQUARE ? squarePaving(x, z, noise, ruined) : paving(noise, ruined, street == Street.GREAT));
        // Fill under the paving, in case a cave opened beneath it
        for (int j = -1; j > -4; j--) {
            if (b.getAt(x, j, z).isAir()) b.setAt(x, j, z, Blocks.COBBLESTONE.defaultBlockState());
        }
        for (int j = 1; j <= 3; j++) {
            BlockState above = b.getAt(x, j, z);
            if (!above.isAir() && !above.getFluidState().isEmpty()) break;
            if (!above.isAir()) b.setAt(x, j, z, Blocks.AIR.defaultBlockState());
        }
        if (ruined && hash(x, z, 12) < 0.06) b.setAt(x, 1, z, DaleBuilder.rubble(hash(x, z, 13)));

        // Lamps along the great streets and the rings: a stone post with a lantern on it
        if ((street == Street.GREAT || street == Street.RING) && lampSpot(x, z)) {
            b.setAt(x, 1, z, DaleBlocks.DALE_BRICK_WALL.get().defaultBlockState());
            b.setAt(x, 2, z, DaleBlocks.DALE_BRICK_WALL.get().defaultBlockState());
            b.setAt(x, 3, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
        }
    }

    /** Where a lamp-post stands: along the edges of a street, every dozen blocks or so. */
    private static boolean lampSpot(int x, int z) {
        double dx = x - DaleTerrain.TOWN_X, dz = z - DaleTerrain.TOWN_Z;
        double r = Math.sqrt(dx * dx + dz * dz);
        if (r < SQUARE + 2.0) return false;
        // On the great streets, at their edge
        if (Math.abs(Math.abs(dx) - STREET) < 0.5 && Math.floorMod(z, 12) == 0 && Math.abs(dz) > SQUARE) return true;
        if (Math.abs(Math.abs(dz) - STREET) < 0.5 && Math.floorMod(x, 12) == 0 && Math.abs(dx) > SQUARE) return true;
        // Along the street inside the wall, at its inner edge
        if (Math.abs(r - RING - 0.5) < 0.5 && Math.floorMod((int) Math.round(Math.atan2(dz, dx) * RING), 14) == 0) return true;
        return false;
    }

    /** Cobbles, with setts of stone brick and andesite; in the ruin, cracked and choked with gravel, ash and weeds. */
    private static BlockState paving(double noise, boolean ruined, boolean great) {
        if (ruined) {
            if (noise < 0.22) return Blocks.GRAVEL.defaultBlockState();
            if (noise < 0.34) return Blocks.COARSE_DIRT.defaultBlockState();
            if (noise < 0.42) return DaleBlocks.ASH.get().defaultBlockState();
            if (noise < 0.6) return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            return Blocks.COBBLESTONE.defaultBlockState();
        }
        if (great) {
            if (noise < 0.45) return Blocks.STONE_BRICKS.defaultBlockState();
            if (noise < 0.75) return Blocks.COBBLESTONE.defaultBlockState();
            if (noise < 0.9) return Blocks.ANDESITE.defaultBlockState();
            return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        }
        if (noise < 0.6) return Blocks.COBBLESTONE.defaultBlockState();
        if (noise < 0.85) return Blocks.GRAVEL.defaultBlockState();
        return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
    }

    /** The market square: rings of Dale brick and polished andesite. */
    private static BlockState squarePaving(int x, int z, double noise, boolean ruined) {
        int ring = (int) radius(x, z) / 3;
        BlockState state = ring % 2 == 0 ? DaleBlocks.DALE_BRICKS.get().defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState();
        if (ruined) {
            if (noise < 0.3) return Blocks.GRAVEL.defaultBlockState();
            if (noise < 0.42) return DaleBlocks.ASH.get().defaultBlockState();
            if (noise < 0.6 && ring % 2 == 0) return DaleBlocks.CRACKED_DALE_BRICKS.get().defaultBlockState();
        }
        return state;
    }

    // ── the bell-tower ───────────────────────────────────────────────────────

    /**
     * The bell-tower of Dale, its bells ringing over the town: seven blocks
     * square and thirty high, open arches round its belfry, four bells under
     * a steep roof of tiles. In the Desolation it stands broken, and its
     * bells lie fallen at its foot.
     */
    private static void belfry(DaleBuilder b, RandomSource random, int x, int z, DaleBuilder.State state) {
        b.origin(x, GROUND, z, 0, 0);
        b.roll(random);
        BlockState brick = b.brick;
        int top = 26;
        for (int i = -3; i <= 3; i++) {
            for (int k = -3; k <= 3; k++) {
                int ai = Math.abs(i), ak = Math.abs(k);
                b.fillDown(i, 0, 0, k, b.floor);
                boolean edge = ai == 3 || ak == 3;
                boolean corner = ai == 3 && ak == 3;
                for (int j = 1; j <= top; j++) {
                    if (!edge) {
                        b.air(i, j, k);
                        continue;
                    }
                    // The belfry: open arches on every side, between the corner piers
                    if (j >= 18 && j <= 22 && !corner && ai <= 1 | ak <= 1) {
                        b.air(i, j, k);
                        continue;
                    }
                    b.set(i, j, k, corner ? b.pillar : brick);
                }
                // Floors: the ground, a loft halfway, and the bell-chamber
                if (!edge) {
                    b.set(i, 9, k, b.plank);
                    b.set(i, 17, k, b.plank);
                }
                // A string course at the loft, and the cornice under the roof
                if (edge && !corner) {
                    b.set(i, 9, k, DaleBlocks.CHISELED_DALE_BRICKS.get().defaultBlockState());
                    b.set(i, top, k, DaleBlocks.CHISELED_DALE_BRICKS.get().defaultBlockState());
                }
            }
        }
        // The door, windows and a ladder up through the floors
        b.door(0, 1, -3, b.door, 3);
        b.set(0, 3, -3, b.stairs(b.brickStairs, 6));
        for (int j = 1; j <= 17; j++) b.set(0, j, 2, b.ladder(2));
        b.air(0, 9, 2);
        b.set(0, 9, 2, b.ladder(2));
        b.air(0, 17, 2);
        b.set(0, 17, 2, b.ladder(2));
        for (int j : new int[] {5, 6, 13, 14}) {
            b.set(-3, j, 0, b.bars);
            b.set(3, j, 0, b.bars);
            b.set(0, j, 3, b.bars);
        }
        b.set(0, 4, -2, b.torch(3));
        b.set(0, 12, -2, b.torch(3));
        // The bells, hung from a frame of beams
        for (int i = -2; i <= 2; i++) b.set(i, 23, 0, b.beam(4));
        for (int k = -2; k <= 2; k++) {
            if (k != 0) b.set(0, 23, k, b.beam(8));
        }
        BlockState bell = Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING);
        int[][] bells = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] at : bells) {
            Direction facing = at[0] != 0 ? Direction.EAST : Direction.NORTH;
            b.set(at[0], 22, at[1], bell.setValue(BellBlock.FACING, facing));
        }
        // The roof: a steep pyramid of tiles
        for (int layer = 0; layer <= 4; layer++) {
            int j = top + 1 + layer;
            int w = 4 - layer;
            for (int i = -w; i <= w; i++) {
                for (int k = -w; k <= w; k++) {
                    if (Math.abs(i) == w || Math.abs(k) == w) {
                        int meta;
                        if (k == -w) meta = 2;
                        else if (k == w) meta = 3;
                        else if (i == -w) meta = 1;
                        else meta = 0;
                        b.set(i, j, k, b.stairs(b.roofStairs, meta));
                    } else {
                        b.set(i, j, k, b.roof);
                    }
                }
            }
        }
        b.set(0, top + 6, 0, b.roof);
        b.set(0, top + 7, 0, DaleBlocks.DALE_BRICK_WALL.get().defaultBlockState());

        if (state == DaleBuilder.State.BURNT) {
            // The bells of Dale, fallen with the belfry, lying in the rubble at its foot
            BlockState fallen = Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, BellAttachType.FLOOR);
            b.set(5, 1, 1, fallen.setValue(BellBlock.FACING, Direction.EAST));
            b.set(-1, 1, 5, fallen.setValue(BellBlock.FACING, Direction.NORTH));
            b.set(2, 1, 1, fallen.setValue(BellBlock.FACING, Direction.WEST));
            for (int[] r : new int[][] {{4, 0}, {5, 2}, {4, 3}, {0, 5}, {-2, 4}, {1, 4}, {6, 1}, {3, 5}}) {
                b.set(r[0], 1, r[1], DaleBuilder.rubble(hash(r[0], r[1], 5)));
            }
        }
    }

    // ── the market ───────────────────────────────────────────────────────────

    /**
     * Stalls round the market square, awnings of coloured cloth over counters
     * of goods: barrels and baskets, pots and lanterns, the toys of Dale that
     * were the wonder of the North. In the Desolation, nothing.
     */
    private static void stalls(DaleBuilder b, DaleBuilder.State state) {
        if (state != DaleBuilder.State.INTACT) return;
        Block[] awnings = {Blocks.RED_WOOL, Blocks.YELLOW_WOOL, Blocks.BLUE_WOOL, Blocks.WHITE_WOOL, Blocks.GREEN_WOOL,
                Blocks.ORANGE_WOOL, Blocks.LIGHT_BLUE_WOOL, Blocks.MAGENTA_WOOL};
        BlockState[] goods = {Blocks.BARREL.defaultBlockState(), Blocks.DECORATED_POT.defaultBlockState(),
                Blocks.HAY_BLOCK.defaultBlockState(), Blocks.COMPOSTER.defaultBlockState(),
                Blocks.CHISELED_BOOKSHELF.defaultBlockState(), Blocks.PUMPKIN.defaultBlockState(),
                Blocks.MELON.defaultBlockState(), Blocks.LANTERN.defaultBlockState()};
        int n = 10;
        for (int s = 0; s < n; s++) {
            double a = Math.toRadians(s * 360.0 / n + 18.0);
            int sx = DaleTerrain.TOWN_X + (int) Math.round(Math.cos(a) * 15.0);
            int sz = DaleTerrain.TOWN_Z + (int) Math.round(Math.sin(a) * 15.0);
            // Keep the great streets clear
            if (Math.abs(sx - DaleTerrain.TOWN_X) <= STREET + 2 || Math.abs(sz - DaleTerrain.TOWN_Z) <= STREET + 2) continue;
            BlockState awning = awnings[s % awnings.length].defaultBlockState();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int x = sx + dx, z = sz + dz;
                    boolean corner = dx != 0 && dz != 0;
                    if (corner) {
                        b.setAt(x, 1, z, Blocks.SPRUCE_FENCE.defaultBlockState());
                        b.setAt(x, 2, z, Blocks.SPRUCE_FENCE.defaultBlockState());
                    }
                    b.setAt(x, 3, z, awning);
                }
            }
            b.setAt(sx, 1, sz, goods[s % goods.length]);
            b.setAt(sx, 2, sz, goods[(s + 3) % goods.length].is(Blocks.LANTERN)
                    ? Blocks.LANTERN.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
    }

    /** A number from 0 to 1 for a column, the same every time. */
    static double hash(int x, int z, int salt) {
        long h = BlockPos.asLong(x, salt, z) * 0x9E3779B97F4A7C15L;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (h >>> 11) * 0x1.0p-53;
    }

    static double clamp01(double v) {
        return Mth.clamp(v, 0.0, 1.0);
    }
}
