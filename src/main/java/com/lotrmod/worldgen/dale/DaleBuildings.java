package com.lotrmod.worldgen.dale;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The original mod's Dale buildings, ported line for line from its
 * {@code LOTRWorldGenDale*} classes, to be built with a {@link DaleBuilder}.
 *
 * <p>Each takes the builder, its own random (so its materials and its rolls
 * come out the same in every chunk it is built in), the spot in front of its
 * door and the ground's height there, and the way it faces. The original's
 * checks for level grass are left out: here a building is only placed where
 * the ground has been made ready for it.
 *
 * <p>Left out, as the mod has none of them: the Men of Dale who lived and kept
 * watch in them, the plates and mugs of food and drink on their tables, the
 * item frames, and the shop sign naming the bakery. Stand-ins: a fletching
 * table for the original's Dale crafting table, campfires for its hearths, a
 * cartography table for its command table, a blast furnace and a grindstone
 * for its alloy forge and unsmeltery, cakes for its pastries and pies,
 * spruce fence gates for its wooden gates, and blue banners for Dale's.
 */
final class DaleBuildings {
    private DaleBuildings() {}

    /** The extent of each building, in its own (i, k), from the spot it is placed at, for knowing which chunks it touches. */
    static final int[] HOUSE = {-8, -1, 3, 10};
    static final int[] SMITHY = {-5, -2, 5, 13};
    static final int[] BAKERY = {-7, -4, 7, 16};
    static final int[] TOWER = {-3, -3, 3, 3};
    static final int[] WATCHTOWER = {-3, -3, 3, 3};
    static final int[] FORTRESS = {-12, -12, 12, 12};

    /** {@code LOTRWorldGenDaleHouse}: a two-storey house of brick and timber, with a porch and a hay loft. */
    static void house(DaleBuilder b, RandomSource random, int x, int y, int z, int rotation) {
        b.origin(x, y, z, rotation, 1);
        b.roll(random);
        for (int i1 = -7; i1 <= 2; i1++) {
            for (int k1 = 0; k1 <= 8; k1++) {
                if (i1 < -2 && k1 > 4) continue;
                for (int j1 = 1; j1 <= 10; j1++) b.air(i1, j1, k1);
                BlockState fill;
                if ((i1 == -2 || i1 == 2) && (k1 == 0 || k1 == 4 || k1 == 8)) {
                    fill = b.brick;
                    for (int j1 = 1; j1 <= 7; j1++) b.set(i1, j1, k1, b.brick);
                } else if ((k1 == 0 || k1 == 4) && i1 >= -6 && i1 <= -4) {
                    fill = b.brick;
                    for (int j1 = 1; j1 <= 4; j1++) b.set(i1, j1, k1, b.brick);
                    for (int j1 = 5; j1 <= 6; j1++) b.set(i1, j1, k1, b.plank);
                    b.set(i1, 7, k1, b.beam(4));
                } else if (i1 == -7 && k1 >= 1 && k1 <= 3) {
                    fill = b.brick;
                    for (int j1 = 1; j1 <= 4; j1++) b.set(i1, j1, k1, b.brick);
                    for (int j1 = 5; j1 <= 6; j1++) b.set(i1, j1, k1, b.plank);
                    b.set(i1, 7, k1, b.beam(8));
                } else if ((k1 == 0 || k1 == 4) && (i1 == -7 || i1 == -3)) {
                    fill = b.log(b.wood, 0);
                    for (int j1 = 1; j1 <= 7; j1++) b.set(i1, j1, k1, b.log(b.wood, 0));
                } else {
                    fill = b.floor;
                }
                b.fillDown(i1, 0, 0, k1, fill);
            }
        }
        for (int[] pos : new int[][] {{-3, -1}, {-7, -1}, {-8, 0}, {-8, 4}, {-7, 5}}) {
            b.fillDown(pos[0], 7, 4, pos[1], b.fence);
        }
        for (int k1 : new int[] {0, 4, 8}) {
            b.set(-1, 3, k1, b.stairs(b.brickStairs, 4));
            b.set(1, 3, k1, b.stairs(b.brickStairs, 5));
        }
        for (int i1 : new int[] {-2, 2}) {
            b.set(i1, 3, 1, b.stairs(b.brickStairs, 7));
            b.set(i1, 3, 3, b.stairs(b.brickStairs, 6));
            b.set(i1, 3, 5, b.stairs(b.brickStairs, 7));
            b.set(i1, 3, 7, b.stairs(b.brickStairs, 6));
        }
        for (int j1 = 1; j1 <= 2; j1++) {
            b.set(-2, j1, 1, b.brickWall);
            b.set(-2, j1, 3, b.brickWall);
        }
        for (int k1 = 1; k1 <= 3; k1++) {
            for (int j1 = 1; j1 <= 3; j1++) b.set(-3, j1, k1, b.brick);
        }
        b.door(-3, 1, 2, b.door, 0);
        b.set(0, 1, 7, Blocks.HAY_BLOCK.defaultBlockState());
        b.set(1, 1, 7, Blocks.HAY_BLOCK.defaultBlockState());
        b.set(1, 2, 7, Blocks.HAY_BLOCK.defaultBlockState());
        b.set(1, 1, 6, Blocks.HAY_BLOCK.defaultBlockState());
        for (int i1 = -2; i1 <= 2; i1++) {
            b.set(i1, 4, -1, b.stairs(b.brickStairs, 6));
            if (Math.floorMod(i1, 2) == 1) b.set(i1, 5, -1, b.brickWall);
        }
        for (int k1 = -1; k1 <= 9; k1++) {
            b.set(3, 4, k1, b.stairs(b.brickStairs, 4));
            if (Math.floorMod(k1, 2) == 1) b.set(3, 5, k1, b.brickWall);
        }
        for (int i1 = 2; i1 >= -2; i1--) {
            b.set(i1, 4, 9, b.stairs(b.brickStairs, 7));
            if (Math.floorMod(i1, 2) == 1) b.set(i1, 5, 9, b.brickWall);
        }
        for (int k1 = 9; k1 >= 5; k1--) {
            b.set(-3, 4, k1, b.stairs(b.brickStairs, 5));
            if (Math.floorMod(k1, 2) == 1) b.set(-3, 5, k1, b.brickWall);
        }
        for (int i1 = -4; i1 >= -6; i1--) b.set(i1, 4, 5, b.stairs(b.brickStairs, 7));
        b.set(-7, 4, 5, b.brick);
        b.set(-8, 4, 5, DaleBuilder.slab(b.brickSlab, 8));
        b.set(-8, 4, 4, b.brick);
        for (int k1 = 3; k1 >= 1; k1--) b.set(-8, 4, k1, b.stairs(b.brickStairs, 5));
        b.set(-8, 4, 0, b.brick);
        b.set(-8, 4, -1, DaleBuilder.slab(b.brickSlab, 8));
        b.set(-7, 4, -1, b.brick);
        for (int i1 = -6; i1 <= -4; i1++) b.set(i1, 4, -1, b.stairs(b.brickStairs, 6));
        b.set(-3, 4, -1, b.brick);
        for (int k1 : new int[] {0, 4, 8}) {
            for (int i1 = -1; i1 <= 1; i1++) {
                b.set(i1, 4, k1, b.brick);
                if (k1 == 0 || k1 == 8) {
                    b.set(i1, 5, k1, b.plank);
                    b.set(i1, 6, k1, b.plank);
                    b.set(i1, 7, k1, b.beam(4));
                }
            }
        }
        for (int i1 : new int[] {-2, 2}) {
            for (int k1 = 1; k1 <= 3; k1++) {
                b.set(i1, 4, k1, b.brick);
                b.set(i1, 5, k1, b.plank);
                b.set(i1, 6, k1, b.plank);
                b.set(i1, 7, k1, b.beam(8));
            }
            for (int k1 = 5; k1 <= 7; k1++) {
                b.set(i1, 4, k1, b.brick);
                b.set(i1, 5, k1, b.plank);
                b.set(i1, 6, k1, b.plank);
                b.set(i1, 7, k1, b.beam(8));
            }
        }
        for (int i1 = -1; i1 <= 1; i1++) {
            for (int k1 = 1; k1 <= 3; k1++) b.set(i1, 4, k1, b.plank);
            for (int k1 = 5; k1 <= 7; k1++) b.set(i1, 4, k1, b.plank);
        }
        b.set(-5, 2, 0, b.fence);
        b.set(0, 6, 0, b.fence);
        b.set(2, 6, 2, b.fence);
        b.set(2, 6, 6, b.fence);
        b.set(0, 6, 8, b.fence);
        b.set(-2, 6, 6, b.fence);
        b.set(-5, 6, 4, b.fence);
        b.set(-7, 6, 2, b.fence);
        b.set(-5, 6, 0, b.fence);
        b.set(-2, 7, -1, b.stairs(b.brickStairs, 6));
        b.set(2, 7, -1, b.stairs(b.brickStairs, 6));
        b.set(3, 7, 0, b.stairs(b.brickStairs, 4));
        b.set(3, 7, 4, b.stairs(b.brickStairs, 4));
        b.set(3, 7, 8, b.stairs(b.brickStairs, 4));
        b.set(2, 7, 9, b.stairs(b.brickStairs, 7));
        b.set(-2, 7, 9, b.stairs(b.brickStairs, 7));
        b.set(-3, 7, 8, b.stairs(b.brickStairs, 5));
        for (int i1 = -8; i1 <= 3; i1++) {
            for (int k1 = -1; k1 <= 9; k1++) {
                if (i1 >= -3 || k1 <= 5) b.set(i1, 8, k1, b.brick);
            }
        }
        b.set(-8, 8, -1, DaleBuilder.slab(b.brickSlab, 8));
        b.set(-8, 8, 5, DaleBuilder.slab(b.brickSlab, 8));
        for (int i1 = -8; i1 <= 3; i1++) {
            b.set(i1, 9, -1, b.stairs(b.roofStairs, 2));
            b.set(i1, 10, 0, b.stairs(b.roofStairs, 2));
            b.set(i1, 11, 1, b.stairs(b.roofStairs, 2));
            b.set(i1, 11, 2, b.roof);
            if (i1 <= -1 || i1 >= 1) b.set(i1, 11, 3, b.stairs(b.roofStairs, 3));
            if (i1 <= -2 || i1 >= 2) b.set(i1, 10, 4, b.stairs(b.roofStairs, 3));
            if (i1 <= -3 || i1 >= 3) b.set(i1, 9, 5, b.stairs(b.roofStairs, 3));
        }
        for (int k1 = 3; k1 <= 9; k1++) {
            if (k1 >= 6) {
                b.set(-3, 9, k1, b.stairs(b.roofStairs, 1));
                b.set(3, 9, k1, b.stairs(b.roofStairs, 0));
            }
            if (k1 >= 5) {
                b.set(-2, 10, k1, b.stairs(b.roofStairs, 1));
                b.set(2, 10, k1, b.stairs(b.roofStairs, 0));
            }
            if (k1 >= 4) {
                b.set(-1, 11, k1, b.stairs(b.roofStairs, 1));
                b.set(1, 11, k1, b.stairs(b.roofStairs, 0));
            }
            b.set(0, 11, k1, b.roof);
        }
        for (int i1 : new int[] {-7, 2}) {
            for (int k1 = 0; k1 <= 4; k1++) b.set(i1, 9, k1, b.brick);
            for (int k1 = 1; k1 <= 3; k1++) b.set(i1, 10, k1, b.brick);
        }
        for (int i1 : new int[] {-8, 3}) {
            b.set(i1, 9, 0, b.stairs(b.roofStairs, 7));
            b.set(i1, 10, 1, b.stairs(b.roofStairs, 7));
            b.set(i1, 10, 3, b.stairs(b.roofStairs, 6));
            b.set(i1, 9, 4, b.stairs(b.roofStairs, 6));
        }
        for (int i1 = -2; i1 <= 2; i1++) b.set(i1, 9, 8, b.brick);
        for (int i1 = -1; i1 <= 1; i1++) b.set(i1, 10, 8, b.brick);
        b.set(-2, 9, 9, b.stairs(b.roofStairs, 4));
        b.set(-1, 10, 9, b.stairs(b.roofStairs, 4));
        b.set(1, 10, 9, b.stairs(b.roofStairs, 5));
        b.set(2, 9, 9, b.stairs(b.roofStairs, 5));
        // The chimney
        b.set(0, 12, 2, b.hearth());
        b.air(0, 13, 2);
        b.set(0, 11, 1, b.brick);
        b.set(0, 12, 1, b.brick);
        b.set(-1, 12, 2, b.brick);
        b.set(1, 12, 2, b.brick);
        b.set(0, 12, 3, b.brick);
        b.set(0, 13, 1, b.brickWall);
        b.set(-1, 13, 2, b.brickWall);
        b.set(1, 13, 2, b.brickWall);
        b.set(0, 13, 3, b.brickWall);
        b.set(0, 14, 1, b.stairs(b.roofStairs, 2));
        b.set(-1, 14, 2, b.stairs(b.roofStairs, 1));
        b.set(1, 14, 2, b.stairs(b.roofStairs, 0));
        b.set(0, 14, 3, b.stairs(b.roofStairs, 3));
        // Smoke needs a way out, so the cap over the hearth is left open
        b.air(0, 14, 2);
        for (int j1 = 1; j1 <= 7; j1++) b.set(-5, j1, 2, b.beam(0));
        b.set(-4, 3, 1, b.torch(1));
        b.set(-6, 7, 2, b.torch(2));
        b.set(-3, 7, 2, b.torch(1));
        // The stair up round the beam in the porch
        b.set(-5, 1, 1, b.stairs(b.brickStairs, 0));
        b.set(-6, 1, 1, b.brick);
        b.set(-6, 1, 2, b.stairs(b.brickStairs, 7));
        b.set(-6, 2, 2, b.stairs(b.brickStairs, 2));
        b.set(-6, 2, 3, b.brick);
        b.set(-5, 2, 3, b.stairs(b.brickStairs, 4));
        b.set(-5, 3, 3, b.stairs(b.brickStairs, 1));
        b.set(-4, 3, 3, b.brick);
        b.set(-4, 3, 2, b.stairs(b.brickStairs, 6));
        b.set(-4, 4, 2, b.stairs(b.brickStairs, 3));
        b.set(-4, 4, 1, b.brick);
        b.set(-5, 4, 1, b.brick);
        b.set(-5, 5, 1, b.fence);
        for (int k1 = 1; k1 <= 3; k1++) {
            b.set(-3, 4, k1, b.beam(8));
            b.set(-2, 5, k1, b.brick);
            b.set(-2, 6, k1, b.brick);
        }
        b.door(-2, 5, 2, b.door, 2);
        b.set(0, 7, 1, b.torch(3));
        b.set(-1, 7, 3, b.torch(2));
        b.set(1, 7, 3, b.torch(1));
        b.set(-1, 7, 5, b.torch(2));
        b.set(0, 5, 1, b.bed(1));
        b.set(1, 5, 1, b.bed(9));
        b.set(1, 5, 2, b.beam(0));
        b.chest(random, 1, 5, 3, 5, DaleBuilder.HOUSE_LOOT);
        b.set(1, 5, 4, b.brick);
        b.set(1, 6, 4, b.brick);
        b.set(1, 7, 4, b.brick);
        b.set(0, 7, 4, b.stairs(b.brickStairs, 5));
        b.set(-1, 7, 4, b.stairs(b.brickStairs, 4));
        b.set(1, 5, 5, Blocks.CRAFTING_TABLE.defaultBlockState());
        b.set(1, 5, 6, b.furnace(Blocks.FURNACE, 5));
        b.set(1, 5, 7, b.brick);
        b.set(0, 5, 7, Blocks.CAULDRON.defaultBlockState());
        b.set(-1, 5, 7, Blocks.FLETCHING_TABLE.defaultBlockState());
        b.set(-1, 7, 7, b.stairs(b.brickStairs, 2));
        b.set(0, 7, 7, b.stairs(b.brickStairs, 2));
        b.set(1, 7, 7, b.brick);
        b.set(1, 7, 6, b.stairs(b.brickStairs, 1));
        b.set(1, 7, 5, b.stairs(b.brickStairs, 1));
        b.set(-3, 10, 2, b.chandelier(0));
        b.set(0, 10, 5, b.chandelier(0));
        if (random.nextInt(3) == 0) {
            int i1 = -6 + random.nextInt(8);
            int k1 = random.nextInt(5);
            int chestDir = new int[] {2, 5, 3, 4}[random.nextInt(4)];
            b.chest(random, i1, 9, k1, chestDir, DaleBuilder.TREASURE_LOOT);
        }
    }

    /** {@code LOTRWorldGenDaleVillageTower}: an open pavilion of brick, its lantern roofed over its hearth, for a market square. */
    static void tower(DaleBuilder b, RandomSource random, int x, int y, int z, int rotation) {
        b.origin(x, y, z, rotation, 3);
        b.roll(random);
        for (int i1 = -3; i1 <= 3; i1++) {
            for (int k1 = -3; k1 <= 3; k1++) {
                int i2 = Math.abs(i1);
                int k2 = Math.abs(k1);
                if (i2 <= 2 && k2 <= 2) {
                    b.fillDown(i1, 0, 0, k1, b.floor);
                    for (int j1 = 1; j1 <= 10; j1++) b.air(i1, j1, k1);
                }
                if (i2 == 2 && k2 == 3 || k2 == 2 && i2 == 3) {
                    b.fillDown(i1, 1, 1, k1, b.brickWall);
                }
                if (i2 <= 2 && k2 <= 2) {
                    if (i2 == 2 && k2 == 2) {
                        for (int j1 = 1; j1 <= 6; j1++) b.set(i1, j1, k1, b.brick);
                    } else if (i2 == 2 || k2 == 2) {
                        for (int j1 = 4; j1 <= 6; j1++) b.set(i1, j1, k1, b.brick);
                    }
                    if (i2 == 2 && k2 == 2) b.set(i1, 7, k1, b.brickWall);
                    if (i2 == 2 && k2 == 1 || k2 == 2 && i2 == 1) b.set(i1, 7, k1, b.brick);
                    if (i2 == 2 && k2 == 0 || k2 == 2 && i2 == 0) {
                        b.set(i1, 7, k1, b.bars);
                        b.set(i1, 8, k1, b.brick);
                        b.set(i1, 9, k1, DaleBuilder.slab(b.brickSlab, 0));
                    }
                    if (i2 <= 1 && k2 <= 1) {
                        if (i2 == 1 && k2 == 1) {
                            b.set(i1, 6, k1, DaleBuilder.slab(b.brickSlab, 8));
                            b.set(i1, 7, k1, b.brickWall);
                            for (int j1 = 8; j1 <= 11; j1++) b.set(i1, j1, k1, b.brick);
                            b.set(i1, 12, k1, b.brickWall);
                        }
                        if (i2 == 1 && k2 == 0 || k2 == 1 && i2 == 0) {
                            b.set(i1, 9, k1, b.brick);
                            b.set(i1, 10, k1, b.brick);
                        }
                        if (i2 == 0 && k2 == 0) {
                            b.set(i1, 9, k1, b.brick);
                            b.set(i1, 10, k1, b.brick);
                            b.set(i1, 11, k1, b.hearth());
                            b.air(i1, 12, k1);
                            b.set(i1, 13, k1, b.roof);
                            b.set(i1, 14, k1, DaleBuilder.slab(b.roofSlab, 0));
                        }
                    }
                }
            }
        }
        for (int i1 : new int[] {-2, 2}) {
            b.set(i1, 3, -1, b.stairs(b.brickStairs, 7));
            b.set(i1, 3, 1, b.stairs(b.brickStairs, 6));
        }
        for (int k1 : new int[] {-2, 2}) {
            b.set(-1, 3, k1, b.stairs(b.brickStairs, 4));
            b.set(1, 3, k1, b.stairs(b.brickStairs, 5));
        }
        for (int i1 = -3; i1 <= 3; i1++) {
            b.set(i1, 5, -3, b.stairs(b.brickStairs, 6));
            b.set(i1, 5, 3, b.stairs(b.brickStairs, 7));
            if (Math.floorMod(i1, 2) == 1) {
                b.set(i1, 6, -3, b.brickWall);
                b.set(i1, 6, 3, b.brickWall);
            }
        }
        for (int k1 = -2; k1 <= 2; k1++) {
            b.set(-3, 5, k1, b.stairs(b.brickStairs, 5));
            b.set(3, 5, k1, b.stairs(b.brickStairs, 4));
            if (Math.floorMod(k1, 2) == 1) {
                b.set(-3, 6, k1, b.brickWall);
                b.set(3, 6, k1, b.brickWall);
            }
        }
        for (int i1 : new int[] {-2, 2}) {
            b.set(i1, 8, -1, b.stairs(b.brickStairs, 2));
            b.set(i1, 8, 1, b.stairs(b.brickStairs, 3));
        }
        for (int k1 : new int[] {-2, 2}) {
            b.set(-1, 8, k1, b.stairs(b.brickStairs, 1));
            b.set(1, 8, k1, b.stairs(b.brickStairs, 0));
        }
        b.set(-1, 8, 0, b.stairs(b.brickStairs, 4));
        b.set(1, 8, 0, b.stairs(b.brickStairs, 5));
        b.set(0, 8, -1, b.stairs(b.brickStairs, 7));
        b.set(0, 8, 1, b.stairs(b.brickStairs, 6));
        for (int i1 = -2; i1 <= 2; i1++) {
            b.set(i1, 10, -2, b.stairs(b.roofStairs, 2));
            b.set(i1, 10, 2, b.stairs(b.roofStairs, 3));
        }
        for (int k1 = -1; k1 <= 1; k1++) {
            b.set(-2, 10, k1, b.stairs(b.roofStairs, 1));
            b.set(2, 10, k1, b.stairs(b.roofStairs, 0));
        }
        b.set(-1, 11, 0, b.stairs(b.brickStairs, 1));
        b.set(1, 11, 0, b.stairs(b.brickStairs, 0));
        b.set(0, 11, -1, b.stairs(b.brickStairs, 2));
        b.set(0, 11, 1, b.stairs(b.brickStairs, 3));
        for (int i1 = -1; i1 <= 1; i1++) {
            b.set(i1, 13, -1, b.stairs(b.roofStairs, 2));
            b.set(i1, 13, 1, b.stairs(b.roofStairs, 3));
        }
        b.set(-1, 13, 0, b.stairs(b.roofStairs, 1));
        b.set(1, 13, 0, b.stairs(b.roofStairs, 0));
        b.set(0, 4, -3, b.wallBanner(2));
        b.set(-3, 4, 0, b.wallBanner(3));
        b.set(0, 4, 3, b.wallBanner(0));
        b.set(3, 4, 0, b.wallBanner(1));
        b.set(-1, 5, 0, b.torch(2));
        b.set(1, 5, 0, b.torch(1));
        b.set(0, 5, -1, b.torch(3));
        b.set(0, 5, 1, b.torch(4));
    }

    /** {@code LOTRWorldGenDaleWatchtower}: a square tower of brick with a guardroom, a loft and a roofed lookout. */
    static void watchtower(DaleBuilder b, RandomSource random, int x, int y, int z, int rotation) {
        b.origin(x, y, z, rotation, 3);
        b.roll(random);
        for (int i1 = -2; i1 <= 2; i1++) {
            for (int k1 = -2; k1 <= 2; k1++) {
                for (int j1 = 9; j1 <= 13; j1++) b.air(i1, j1, k1);
            }
        }
        for (int i1 = -2; i1 <= 2; i1++) {
            for (int k1 = -2; k1 <= 2; k1++) {
                int i2 = Math.abs(i1);
                int k2 = Math.abs(k1);
                b.fillDown(i1, 8, 0, k1, i2 == 2 && k2 == 2 ? b.pillar : b.brick);
                if (i2 == 2 && k2 == 2) {
                    for (int j1 = 9; j1 <= 11; j1++) b.set(i1, j1, k1, b.pillar);
                    b.set(i1, 12, k1, b.roof);
                    b.set(i1, 12, k1 - 1, b.stairs(b.roofStairs, 6));
                    b.set(i1, 12, k1 + 1, b.stairs(b.roofStairs, 7));
                    b.set(i1 - 1, 12, k1, b.stairs(b.roofStairs, 5));
                    b.set(i1 + 1, 12, k1, b.stairs(b.roofStairs, 4));
                }
            }
        }
        for (int i1 = -3; i1 <= 3; i1++) {
            for (int k1 = -3; k1 <= 3; k1++) {
                int i2 = Math.abs(i1);
                int k2 = Math.abs(k1);
                if (i2 == 1 && k2 == 3 || i2 == 3 && k2 == 1) b.fillDown(i1, 3, 0, k1, b.brick);
            }
        }
        b.set(-1, 4, -3, b.stairs(b.brickStairs, 2));
        b.set(1, 4, -3, b.stairs(b.brickStairs, 2));
        b.set(-1, 4, 3, b.stairs(b.brickStairs, 3));
        b.set(1, 4, 3, b.stairs(b.brickStairs, 3));
        b.set(-3, 4, -1, b.stairs(b.brickStairs, 1));
        b.set(-3, 4, 1, b.stairs(b.brickStairs, 1));
        b.set(3, 4, -1, b.stairs(b.brickStairs, 0));
        b.set(3, 4, 1, b.stairs(b.brickStairs, 0));
        for (int i1 = -1; i1 <= 1; i1++) {
            for (int k1 = -1; k1 <= 1; k1++) {
                for (int j1 = 1; j1 <= 3; j1++) b.air(i1, j1, k1);
                for (int j1 = 5; j1 <= 7; j1++) b.air(i1, j1, k1);
                b.set(i1, 4, k1, b.plank);
                b.set(i1, 8, k1, b.plank);
            }
        }
        for (int i1 = -1; i1 <= 1; i1++) {
            b.set(i1, 9, -2, b.fence);
            b.set(i1, 9, 2, b.fence);
        }
        for (int k1 = -1; k1 <= 1; k1++) {
            b.set(-2, 9, k1, b.fence);
            b.set(2, 9, k1, b.fence);
        }
        for (int j1 = 1; j1 <= 7; j1++) b.set(0, j1, 1, b.ladder(2));
        b.set(0, 8, 1, b.trapdoor(b.trapdoor, 9));
        b.door(0, 1, -2, b.door, 1);
        b.set(0, 3, -1, b.torch(3));
        b.set(0, 0, 0, b.pillar);
        b.set(-2, 1, -1, Blocks.CRAFTING_TABLE.defaultBlockState());
        b.air(-2, 2, -1);
        b.set(-2, 1, 1, Blocks.FLETCHING_TABLE.defaultBlockState());
        b.air(-2, 2, 1);
        b.set(2, 1, -1, b.plank);
        b.set(2, 2, -1, b.barrel(5));
        b.chest(random, 2, 1, 1, 5, DaleBuilder.WATCHTOWER_LOOT);
        b.air(2, 2, 1);
        for (int i1 : new int[] {-1, 1}) {
            b.set(i1, 5, 0, b.bed(2));
            b.set(i1, 5, -1, b.bed(10));
        }
        b.set(0, 7, -1, b.torch(3));
        b.set(0, 7, -2, b.wallBanner(2));
        b.set(0, 7, 2, b.wallBanner(0));
        b.set(-2, 7, 0, b.wallBanner(3));
        b.set(2, 7, 0, b.wallBanner(1));
        for (int i1 = -3; i1 <= 3; i1++) {
            for (int k1 = -3; k1 <= 3; k1++) {
                int i2 = Math.abs(i1);
                int k2 = Math.abs(k1);
                if (i2 == 3 || k2 == 3) {
                    b.set(i1, 13, k1, DaleBuilder.slab(b.roofSlab, 0));
                } else if (i2 == 2 || k2 == 2) {
                    b.set(i1, 13, k1, b.roof);
                } else if (i2 == 1 || k2 == 1) {
                    b.set(i1, 14, k1, b.roof);
                    b.set(i1, 15, k1, DaleBuilder.slab(b.roofSlab, 0));
                }
            }
        }
        for (int i1 = -2; i1 <= 2; i1++) {
            b.set(i1, 14, -2, b.stairs(b.roofStairs, 2));
            b.set(i1, 14, 2, b.stairs(b.roofStairs, 3));
        }
        for (int k1 = -2; k1 <= 2; k1++) {
            b.set(-2, 14, k1, b.stairs(b.roofStairs, 1));
            b.set(2, 14, k1, b.stairs(b.roofStairs, 0));
        }
        for (int i1 = -1; i1 <= 1; i1++) {
            b.set(i1, 14, -1, b.stairs(b.roofStairs, 7));
            b.set(i1, 14, 1, b.stairs(b.roofStairs, 6));
        }
        b.set(-1, 14, 0, b.stairs(b.roofStairs, 4));
        b.set(1, 14, 0, b.stairs(b.roofStairs, 5));
        b.set(0, 13, 1, b.torch(4));
    }

    /** {@code LOTRWorldGenDaleSmithy}: a forge hall with a lava hearth, its chimney through the roof, and lodging above. */
    static void smithy(DaleBuilder b, RandomSource random, int x, int y, int z, int rotation) {
        b.origin(x, y, z, rotation, 2);
        b.roll(random);
        for (int i1 = -3; i1 <= 3; i1++) {
            for (int k1 = 0; k1 <= 10; k1++) {
                b.fillDown(i1, 0, 1, k1, b.brick);
                for (int j1 = 1; j1 <= 10; j1++) b.air(i1, j1, k1);
            }
        }
        for (int i1 = -4; i1 <= 4; i1++) {
            for (int k1 = -1; k1 <= 11; k1++) {
                int i2 = Math.abs(i1);
                if (i2 == 4 && Math.floorMod(k1, 4) == 3 || i1 == 0 && k1 == 11) b.fillDown(i1, 3, 1, k1, b.brick);
                if (i2 == 4 && Math.floorMod(k1, 4) == 0) b.set(i1, 3, k1, b.stairs(b.brickStairs, 7));
                if (i2 == 4 && Math.floorMod(k1, 4) == 2) b.set(i1, 3, k1, b.stairs(b.brickStairs, 6));
                if (k1 == 11 && Math.floorMod(i1, 4) == 1) b.set(i1, 3, k1, b.stairs(b.brickStairs, 4));
                if (k1 == 11 && Math.floorMod(i1, 4) == 3) b.set(i1, 3, k1, b.stairs(b.brickStairs, 5));
                if (k1 == -1 && i1 == -3) b.set(i1, 3, k1, b.stairs(b.brickStairs, 4));
                if (k1 == -1 && i1 == 3) b.set(i1, 3, k1, b.stairs(b.brickStairs, 5));
                if (i2 == 1 && k1 == -1) b.fillDown(i1, 3, 1, k1, b.beam(0));
                if (i2 == 0 && k1 == -1) {
                    b.fillDown(i1, 0, 0, k1, b.floor);
                    for (int j1 = 1; j1 <= 3; j1++) b.air(i1, j1, k1);
                }
                if (i2 <= 2 && k1 >= 1 && k1 <= 9) b.set(i1, 0, k1, b.floor);
                if (i2 <= 3 && k1 >= 0 && k1 <= 10) {
                    if (i2 == 3 || k1 == 0 || k1 == 10) {
                        for (int j1 = 1; j1 <= 4; j1++) b.set(i1, j1, k1, b.brick);
                    }
                    b.set(i1, 4, k1, b.brick);
                }
                if (i2 == 4 || k1 == -1 || k1 == 11) {
                    b.set(i1, 4, k1, b.brick);
                    for (int j1 = 5; j1 <= 6; j1++) b.set(i1, j1, k1, b.plank);
                }
            }
        }
        for (int i1 = -5; i1 <= 5; i1++) {
            b.set(i1, 4, -2, b.stairs(b.brickStairs, 6));
            b.set(i1, 4, 12, b.stairs(b.brickStairs, 7));
            if (Math.floorMod(i1, 2) == 1) {
                b.set(i1, 5, -2, b.brickWall);
                b.set(i1, 5, 12, b.brickWall);
            }
        }
        for (int k1 = -1; k1 <= 11; k1++) {
            b.set(-5, 4, k1, b.stairs(b.brickStairs, 5));
            b.set(5, 4, k1, b.stairs(b.brickStairs, 4));
            if (Math.floorMod(k1, 2) == 0) {
                b.set(-5, 5, k1, b.brickWall);
                b.set(5, 5, k1, b.brickWall);
            }
        }
        for (int i1 = -3; i1 <= 3; i1++) {
            b.set(i1, 7, -1, b.beam(4));
            b.set(i1, 7, 11, b.beam(4));
        }
        for (int k1 = 0; k1 <= 10; k1++) {
            b.set(-4, 7, k1, b.beam(8));
            b.set(4, 7, k1, b.beam(8));
        }
        for (int i1 = -4; i1 <= 4; i1++) {
            if (Math.floorMod(i1, 4) != 0) {
                if (Math.floorMod(i1, 4) == 2) {
                    b.set(i1, 6, -1, b.bars);
                    b.set(i1, 6, 11, b.bars);
                }
            } else {
                for (int j1 = 5; j1 <= 7; j1++) {
                    b.set(i1, j1, -1, b.brick);
                    b.set(i1, j1, 11, b.brick);
                }
                b.set(i1, 7, -2, b.stairs(b.brickStairs, 6));
                b.set(i1, 7, 12, b.stairs(b.brickStairs, 7));
            }
        }
        for (int k1 = -1; k1 <= 11; k1++) {
            if (Math.floorMod(k1, 4) != 3) {
                if (Math.floorMod(k1, 4) == 1) {
                    b.set(-4, 6, k1, b.bars);
                    b.set(4, 6, k1, b.bars);
                }
            } else {
                for (int j1 = 5; j1 <= 7; j1++) {
                    b.set(-4, j1, k1, b.brick);
                    b.set(4, j1, k1, b.brick);
                }
                b.set(-5, 7, k1, b.stairs(b.brickStairs, 5));
                b.set(5, 7, k1, b.stairs(b.brickStairs, 4));
            }
        }
        for (int i1 = -5; i1 <= 5; i1++) {
            for (int k1 = -2; k1 <= 12; k1++) {
                if (i1 <= -4 || i1 >= 4 || k1 <= -1 || k1 >= 11) b.set(i1, 8, k1, b.brick);
            }
        }
        for (int i1 = -4; i1 <= 4; i1++) {
            for (int k1 = -1; k1 <= 11; k1++) {
                if (i1 == -4 || i1 == 4 || k1 == -1 || k1 == 11) b.set(i1, 9, k1, b.brick);
            }
        }
        for (int i1 = -3; i1 <= 3; i1++) {
            for (int k1 = 0; k1 <= 10; k1++) {
                if (i1 == -3 || i1 == 3 || k1 == 0 || k1 == 10) b.set(i1, 8, k1, b.plank);
                if (k1 == 3 || k1 == 7) {
                    b.set(i1, 8, k1, b.beam(4));
                } else if (i1 == 0) {
                    b.set(i1, 8, k1, b.beam(8));
                }
                b.set(i1, 10, k1, b.brick);
            }
            b.set(i1, 10, -1, b.plank);
            b.set(i1, 10, 11, b.plank);
        }
        b.set(0, 9, -2, b.stairs(b.brickStairs, 2));
        b.set(0, 10, -2, b.stairs(b.brickStairs, 6));
        b.set(0, 9, 12, b.stairs(b.brickStairs, 3));
        b.set(0, 10, 12, b.stairs(b.brickStairs, 7));
        for (int k1 = -2; k1 <= 12; k1++) {
            b.set(-5, 9, k1, b.stairs(b.roofStairs, 1));
            b.set(-4, 10, k1, b.stairs(b.roofStairs, 1));
            b.set(-3, 11, k1, b.stairs(b.roofStairs, 1));
            for (int i1 = -2; i1 <= 2; i1++) b.set(i1, 11, k1, b.roof);
            b.set(3, 11, k1, b.stairs(b.roofStairs, 0));
            b.set(4, 10, k1, b.stairs(b.roofStairs, 0));
            b.set(5, 9, k1, b.stairs(b.roofStairs, 0));
            if (k1 == -2 || k1 == 12) {
                b.set(-4, 9, k1, b.stairs(b.roofStairs, 4));
                b.set(-3, 10, k1, b.stairs(b.roofStairs, 4));
                b.set(3, 10, k1, b.stairs(b.roofStairs, 5));
                b.set(4, 9, k1, b.stairs(b.roofStairs, 5));
            }
        }
        // The forge's chimney
        for (int i1 = -1; i1 <= 1; i1++) {
            for (int k1 = 7; k1 <= 9; k1++) {
                for (int j1 = 1; j1 <= 12; j1++) {
                    if (i1 == 0 && k1 == 8) {
                        b.air(i1, j1, k1);
                    } else {
                        b.set(i1, j1, k1, b.brick);
                    }
                }
                if (Math.abs(i1) == 1 && (k1 == 7 || k1 == 9)) {
                    b.set(i1, 13, k1, DaleBuilder.slab(b.brickSlab, 0));
                } else if (Math.abs(i1) != 1 && k1 != 7 && k1 != 9) {
                    b.set(i1, 13, k1, b.bars);
                } else {
                    b.set(i1, 13, k1, b.brick);
                }
            }
        }
        b.set(0, 0, 0, b.floor);
        b.door(0, 1, 0, b.door, 3);
        b.set(0, 3, 0, b.stairs(b.brickStairs, 6));
        b.set(0, 3, 1, b.torch(3));
        for (int i1 : new int[] {-2, 2}) {
            b.set(i1, 1, 1, b.pillar);
            b.set(i1, 2, 1, b.pillar);
            b.set(i1, 3, 1, b.brick);
            b.set(i1, 3, 2, b.stairs(b.brickStairs, 7));
            b.set(i1, 3, 3, b.stairs(b.brickStairs, 6));
            b.set(i1, 1, 4, b.pillar);
            b.set(i1, 2, 4, b.pillar);
            b.set(i1, 3, 4, b.pillar);
        }
        b.set(-1, 3, 1, b.stairs(b.brickStairs, 4));
        b.set(1, 3, 1, b.stairs(b.brickStairs, 5));
        b.set(-2, 1, 2, Blocks.GRINDSTONE.defaultBlockState());
        b.set(-2, 1, 3, b.furnace(Blocks.BLAST_FURNACE, 4));
        b.set(-2, 2, 3, b.furnace(Blocks.FURNACE, 4));
        b.set(2, 1, 2, Blocks.CRAFTING_TABLE.defaultBlockState());
        b.set(2, 1, 3, Blocks.FLETCHING_TABLE.defaultBlockState());
        for (int l = 0; l <= 3; l++) {
            int k1 = 6 + l;
            int j1 = 1 + l;
            for (int i1 : new int[] {-2, 2}) {
                b.air(i1, 4, k1);
                b.set(i1, j1, k1, b.stairs(b.brickStairs, 2));
                for (int j2 = j1 - 1; j2 >= 1; j2--) b.set(i1, j2, k1, b.brick);
            }
        }
        b.set(0, 1, 6, Blocks.ANVIL.defaultBlockState().setValue(net.minecraft.world.level.block.AnvilBlock.FACING, b.facing(5).getClockWise()));
        b.set(-1, 1, 7, Blocks.STONE_BRICKS.defaultBlockState());
        b.set(0, 1, 7, b.stairs(Blocks.STONE_BRICK_STAIRS, 2));
        b.set(1, 1, 7, Blocks.STONE_BRICKS.defaultBlockState());
        b.set(-1, 2, 7, Blocks.STONE_BRICK_WALL.defaultBlockState());
        b.air(0, 2, 7);
        b.set(1, 2, 7, Blocks.STONE_BRICK_WALL.defaultBlockState());
        b.set(0, 3, 7, b.stairs(b.brickStairs, 6));
        b.set(-1, 1, 8, Blocks.STONE_BRICKS.defaultBlockState());
        // The forge's fire: lava in the original, a lit campfire here, which
        // smokes up the chimney and sets nothing alight
        b.set(0, 1, 8, b.ruined ? Blocks.AIR.defaultBlockState() : Blocks.CAMPFIRE.defaultBlockState());
        b.set(1, 1, 8, Blocks.STONE_BRICKS.defaultBlockState());
        b.set(-2, 2, 8, Blocks.STONE_BRICKS.defaultBlockState());
        b.air(-1, 2, 8);
        b.air(1, 2, 8);
        b.set(2, 2, 8, Blocks.STONE_BRICKS.defaultBlockState());
        b.set(0, 1, 9, Blocks.STONE_BRICKS.defaultBlockState());
        b.set(-1, 2, 9, Blocks.STONE_BRICKS.defaultBlockState());
        b.set(0, 2, 9, Blocks.STONE_BRICKS.defaultBlockState());
        b.set(1, 2, 9, Blocks.STONE_BRICKS.defaultBlockState());
        b.set(0, 7, 0, b.torch(3));
        b.set(-3, 7, 3, b.torch(2));
        b.set(3, 7, 3, b.torch(1));
        b.set(-3, 7, 7, b.torch(2));
        b.set(3, 7, 7, b.torch(1));
        b.set(0, 7, 10, b.torch(4));
        b.set(0, 5, 6, Blocks.CRAFTING_TABLE.defaultBlockState());
        b.chest(random, -1, 5, 6, 2, DaleBuilder.HOUSE_LOOT);
        b.chest(random, 1, 5, 6, 2, DaleBuilder.HOUSE_LOOT);
        for (int i1 = -2; i1 <= 0; i1++) {
            for (int k1 = 1; k1 <= 3; k1++) {
                if ((i1 == -2 || i1 == 0) && (k1 == 1 || k1 == 3)) {
                    b.set(i1, 5, k1, b.plank);
                } else {
                    b.set(i1, 5, k1, DaleBuilder.slab(b.plankSlab, 8));
                }
                // The plates and mugs laid on the table
                if (random.nextBoolean()) {
                    random.nextInt(4);
                } else {
                    random.nextInt(4);
                }
            }
        }
        for (int i1 : new int[] {2, 3}) {
            b.set(i1, 5, 1, b.bed(2));
            b.set(i1, 5, 0, b.bed(10));
        }
    }

    /** {@code LOTRWorldGenDaleBakery}: a long, low shop with an awning, counters of cakes and a great brick oven. */
    static void bakery(DaleBuilder b, RandomSource random, int x, int y, int z, int rotation) {
        b.origin(x, y, z, rotation, 2);
        b.roll(random);
        for (int i1 = -5; i1 <= 5; i1++) {
            for (int k1 = 0; k1 <= 13; k1++) {
                b.fillDown(i1, 0, 1, k1, b.brick);
                for (int j1 = 1; j1 <= 7; j1++) b.air(i1, j1, k1);
                if (Math.abs(i1) != 5 && k1 != 0 && k1 != 13) {
                    b.set(i1, 0, k1, b.floor);
                } else {
                    for (int j1 = 1; j1 <= 3; j1++) b.set(i1, j1, k1, b.brick);
                }
            }
        }
        for (int i1 = -6; i1 <= 6; i1++) {
            for (int k1 = -1; k1 <= 14; k1++) {
                if (Math.abs(i1) == 6 && (k1 == -1 || k1 == 14) || Math.abs(i1) == 1 && k1 == -1) {
                    b.fillDown(i1, 4, 1, k1, b.beam(0));
                }
            }
        }
        for (int i1 = -1; i1 <= 1; i1++) {
            if (Math.abs(i1) == 1) {
                b.fillDown(i1, 2, 1, -2, b.fence);
            } else {
                b.fillDown(i1, 0, 0, -1, b.floor);
            }
        }
        for (int i1 = -5; i1 <= 5; i1++) {
            b.set(i1, 4, -1, b.brick);
            b.set(i1, 4, 14, b.brick);
        }
        for (int k1 = 0; k1 <= 13; k1++) {
            b.set(-6, 4, k1, b.brick);
            b.set(6, 4, k1, b.brick);
        }
        for (int i1 = -7; i1 <= 7; i1++) {
            b.set(i1, 4, -2, DaleBuilder.slab(b.brickSlab, 8));
            b.set(i1, 4, 15, DaleBuilder.slab(b.brickSlab, 8));
        }
        for (int k1 = -2; k1 <= 15; k1++) {
            b.set(-7, 4, k1, DaleBuilder.slab(b.brickSlab, 8));
            b.set(7, 4, k1, DaleBuilder.slab(b.brickSlab, 8));
        }
        for (int i1 : new int[] {-5, 2}) {
            b.set(i1, 2, -1, b.trapdoor(b.trapdoor, 12));
            b.set(i1, 3, -1, DaleBuilder.slab(b.brickSlab, 8));
            b.set(i1, 4, -2, b.brick);
            b.set(i1 + 1, 2, 0, b.fence);
            b.set(i1 + 1, 3, -1, DaleBuilder.slab(b.plankSlab, 0));
            b.set(i1 + 2, 2, 0, b.fence);
            b.set(i1 + 2, 3, -1, DaleBuilder.slab(b.plankSlab, 0));
            b.set(i1 + 3, 2, -1, b.trapdoor(b.trapdoor, 12));
            b.set(i1 + 3, 3, -1, DaleBuilder.slab(b.brickSlab, 8));
            b.set(i1 + 3, 4, -2, b.brick);
        }
        for (int i1 : new int[] {-5, 2}) {
            b.set(i1, 2, 14, b.trapdoor(b.trapdoor, 13));
            b.set(i1, 3, 14, DaleBuilder.slab(b.brickSlab, 8));
            b.set(i1, 4, 15, b.brick);
            b.set(i1 + 1, 2, 13, b.fence);
            b.set(i1 + 1, 3, 14, DaleBuilder.slab(b.plankSlab, 0));
            b.set(i1 + 2, 2, 13, b.fence);
            b.set(i1 + 2, 3, 14, DaleBuilder.slab(b.plankSlab, 0));
            b.set(i1 + 3, 2, 14, b.trapdoor(b.trapdoor, 13));
            b.set(i1 + 3, 3, 14, DaleBuilder.slab(b.brickSlab, 8));
            b.set(i1 + 3, 4, 15, b.brick);
        }
        for (int k1 = 0; k1 <= 13; k1++) {
            if (k1 == 0 || k1 == 3 || k1 == 6 || k1 == 8 || k1 == 13) {
                b.set(-6, 2, k1, b.trapdoor(b.trapdoor, 15));
                b.set(-6, 3, k1, DaleBuilder.slab(b.brickSlab, 8));
                b.set(-7, 4, k1, b.brick);
                b.set(6, 2, k1, b.trapdoor(b.trapdoor, 14));
                b.set(6, 3, k1, DaleBuilder.slab(b.brickSlab, 8));
                b.set(7, 4, k1, b.brick);
            }
            if (k1 == 1 || k1 == 2 || k1 == 4 || k1 == 5 || k1 >= 9 && k1 <= 12) {
                b.set(-5, 2, k1, b.fence);
                b.set(-6, 3, k1, DaleBuilder.slab(b.plankSlab, 0));
                b.set(5, 2, k1, b.fence);
                b.set(6, 3, k1, DaleBuilder.slab(b.plankSlab, 0));
            }
        }
        // The awning over the door: red and white
        for (int k1 = -2; k1 <= -1; k1++) {
            b.set(-1, 3, k1, Blocks.RED_WOOL.defaultBlockState());
            b.set(0, 3, k1, Blocks.WHITE_WOOL.defaultBlockState());
            b.set(1, 3, k1, Blocks.RED_WOOL.defaultBlockState());
        }
        b.door(0, 1, -1, b.door, 3);
        b.set(0, 0, 0, b.floor);
        b.air(0, 1, 0);
        b.air(0, 2, 0);
        for (int k1 = 0; k1 <= 13; k1++) {
            for (int i1 = -5; i1 <= 5; i1++) {
                int i2 = Math.abs(i1);
                if (i2 < 1 || i2 > 3 || (k1 < 2 || k1 > 6) && (k1 < 8 || k1 > 11)) {
                    b.set(i1, 4, k1, b.plank);
                } else {
                    b.set(i1, 4, k1, DaleBuilder.slab(b.plankSlab, 8));
                }
            }
            for (int i1 = -6; i1 <= 6; i1++) b.set(i1, 5, k1, b.roof);
            for (int i1 = -5; i1 <= 5; i1++) b.set(i1, 6, k1, b.roof);
        }
        for (int k1 : new int[] {-1, 14}) {
            for (int i1 = -6; i1 <= 6; i1++) b.set(i1, 5, k1, b.plank);
            for (int i1 = -5; i1 <= 5; i1++) b.set(i1, 6, k1, b.plank);
        }
        for (int k1 : new int[] {-2, 15}) {
            b.set(-6, 5, k1, b.stairs(b.roofStairs, 4));
            b.set(-5, 6, k1, b.stairs(b.roofStairs, 4));
            b.set(5, 6, k1, b.stairs(b.roofStairs, 5));
            b.set(6, 5, k1, b.stairs(b.roofStairs, 5));
            for (int i1 : new int[] {-3, 0, 3}) {
                for (int j1 = 5; j1 <= 6; j1++) b.set(i1, j1, k1, b.brick);
            }
        }
        for (int k1 = -2; k1 <= 15; k1++) {
            b.set(-7, 5, k1, b.stairs(b.roofStairs, 1));
            b.set(-6, 6, k1, b.stairs(b.roofStairs, 1));
            b.set(-5, 7, k1, b.stairs(b.roofStairs, 1));
            for (int i1 = -4; i1 <= 4; i1++) b.set(i1, 7, k1, b.roof);
            for (int i1 = -2; i1 <= 2; i1++) b.set(i1, 8, k1, DaleBuilder.slab(b.roofSlab, 0));
            b.set(5, 7, k1, b.stairs(b.roofStairs, 0));
            b.set(6, 6, k1, b.stairs(b.roofStairs, 0));
            b.set(7, 5, k1, b.stairs(b.roofStairs, 0));
        }
        // The oven's chimney, of red brick
        BlockState redBrick = Blocks.BRICKS.defaultBlockState();
        for (int j1 = 1; j1 <= 9; j1++) {
            b.set(0, j1, 11, redBrick);
            b.set(-1, j1, 12, redBrick);
            b.air(0, j1, 12);
            b.set(1, j1, 12, redBrick);
            b.set(0, j1, 13, redBrick);
        }
        b.set(0, 1, 12, b.hearth());
        b.set(0, 8, 12, b.hearth());
        b.set(-2, 3, 1, b.torch(3));
        b.set(2, 3, 1, b.torch(3));
        b.set(-4, 3, 3, b.torch(2));
        b.set(4, 3, 3, b.torch(1));
        b.set(-4, 3, 10, b.torch(2));
        b.set(4, 3, 10, b.torch(1));
        b.set(-2, 3, 12, b.torch(4));
        b.set(2, 3, 12, b.torch(4));
        b.set(-2, 4, 4, b.plank);
        b.set(-2, 3, 4, b.chandelier(3));
        b.set(2, 4, 4, b.plank);
        b.set(2, 3, 4, b.chandelier(3));
        for (int i1 = -4; i1 <= 4; i1++) {
            b.set(i1, 1, 7, b.brick);
            b.set(i1, 3, 7, b.fence);
        }
        b.set(-1, 1, 7, b.stairs(b.brickStairs, 4));
        b.set(0, 1, 7, DaleBuilder.slab(b.brickSlab, 8));
        b.set(1, 1, 7, b.stairs(b.brickStairs, 5));
        b.set(3, 1, 7, b.fenceGate(0));
        b.set(-1, 1, 11, b.furnace(Blocks.FURNACE, 2));
        b.set(0, 1, 11, b.stairs(Blocks.BRICK_STAIRS, 2));
        b.set(1, 1, 11, b.furnace(Blocks.FURNACE, 2));
        b.set(-1, 2, 11, b.furnace(Blocks.FURNACE, 2));
        b.set(0, 2, 11, b.bars);
        b.set(1, 2, 11, b.furnace(Blocks.FURNACE, 2));
        b.set(-1, 3, 11, redBrick);
        b.set(1, 3, 11, redBrick);
        for (int i1 = -2; i1 <= 2; i1++) {
            for (int k1 = 10; k1 <= 12; k1++) b.set(i1, 4, k1, redBrick);
        }
        b.air(0, 4, 12);
        b.set(-2, 1, 1, b.plank);
        b.set(-3, 1, 1, DaleBuilder.slab(b.plankSlab, 8));
        b.set(-4, 1, 1, b.plank);
        for (int k1 = 2; k1 <= 5; k1++) b.set(-4, 1, k1, DaleBuilder.slab(b.plankSlab, 8));
        b.set(-4, 1, 6, b.plank);
        b.set(-3, 1, 6, DaleBuilder.slab(b.plankSlab, 8));
        b.set(-2, 1, 6, b.plank);
        for (int k1 = 1; k1 <= 6; k1++) cake(b, random, -4, 2, k1);
        for (int i1 = -3; i1 <= -2; i1++) {
            cake(b, random, i1, 2, 1);
            cake(b, random, i1, 2, 6);
        }
        b.set(2, 1, 3, b.stairs(b.plankStairs, 7));
        b.set(2, 1, 4, b.stairs(b.plankStairs, 6));
        cake(b, random, 2, 2, 3);
        cake(b, random, 2, 2, 4);
        b.set(4, 1, 1, b.plank);
        for (int k1 = 2; k1 <= 5; k1++) b.set(4, 1, k1, DaleBuilder.slab(b.plankSlab, 8));
        b.set(4, 1, 6, b.plank);
        for (int k1 = 1; k1 <= 6; k1++) cake(b, random, 4, 2, k1);
        for (int i1 = -4; i1 <= -3; i1++) {
            b.set(i1, 1, 8, b.plank);
            for (int k1 = 9; k1 <= 11; k1++) b.set(i1, 1, k1, DaleBuilder.slab(b.plankSlab, 8));
            b.set(i1, 1, 12, b.plank);
            for (int k1 = 8; k1 <= 12; k1++) cake(b, random, i1, 2, k1);
        }
        b.set(4, 1, 8, b.plank);
        cake(b, random, 4, 2, 8);
        b.set(4, 1, 9, Blocks.CAULDRON.defaultBlockState());
        b.set(4, 1, 10, DaleBuilder.slab(b.plankSlab, 8));
        b.set(4, 1, 11, Blocks.CRAFTING_TABLE.defaultBlockState());
        b.set(4, 1, 12, b.plank);
        cake(b, random, 4, 2, 12);
        b.set(3, 1, 12, DaleBuilder.slab(b.plankSlab, 8));
        cake(b, random, 3, 2, 12);
        b.set(2, 1, 12, b.plank);
        b.set(2, 2, 12, b.barrel(2));
        // The shop sign, hung out over the street on a post
        b.set(0, 5, -3, DaleBuilder.slab(b.plankSlab, 8));
        b.set(0, 6, -3, DaleBuilder.slab(b.plankSlab, 0));
        b.set(0, 6, -4, DaleBuilder.slab(b.plankSlab, 0));
        b.set(0, 5, -4, b.fence);
        b.set(0, 4, -4, b.plank);
    }

    /** The original's {@code placeRandomCake}: half the time a cake (for its pastries, pies and marzipan too). */
    private static void cake(DaleBuilder b, RandomSource random, int i, int j, int k) {
        if (random.nextBoolean()) {
            if (!random.nextBoolean()) random.nextInt(4);
            b.set(i, j, k, Blocks.CAKE.defaultBlockState());
        }
    }

    /**
     * {@code LOTRWorldGenDaleFortress}: a walled keep about a courtyard, with a
     * gate on three sides, corner towers, a hearth in its middle, barracks,
     * stores and the captain's table.
     */
    static void fortress(DaleBuilder b, RandomSource random, int x, int y, int z, int rotation) {
        b.origin(x, y, z, rotation, 12);
        b.roll(random);
        for (int i1 = -11; i1 <= 11; i1++) {
            for (int k1 = -11; k1 <= 11; k1++) {
                int i2 = Math.abs(i1);
                int k2 = Math.abs(k1);
                b.fillDown(i1, 0, 0, k1, b.floor);
                if (i2 > 9 || k2 > 9) continue;
                for (int j1 = 1; j1 <= 8; j1++) b.air(i1, j1, k1);
                if (i2 <= 8 && k2 <= 8 && (i2 == 8 && k2 >= 4 || k2 == 8 && i2 >= 4)) {
                    for (int j1 = 1; j1 <= 3; j1++) b.set(i1, j1, k1, b.brick);
                }
                if (i2 == 9 && k2 == 2 || k2 == 9 && i2 == 2) {
                    for (int j1 = 1; j1 <= 5; j1++) b.set(i1, j1, k1, b.pillar);
                } else if (i2 == 9 && (k2 == 3 || k2 == 5 || k2 == 9) || k2 == 9 && (i2 == 3 || i2 == 5 || i2 == 9)) {
                    for (int j1 = 1; j1 <= 5; j1++) b.set(i1, j1, k1, b.brick);
                } else if (i2 == 9 || k2 == 9) {
                    for (int j1 = 4; j1 <= 5; j1++) b.set(i1, j1, k1, b.brick);
                }
                if (i2 == 9) {
                    b.set(i1, 3, -8, b.stairs(b.brickStairs, 7));
                    b.set(i1, 3, -6, b.stairs(b.brickStairs, 6));
                    b.set(i1, 3, 6, b.stairs(b.brickStairs, 7));
                    b.set(i1, 3, 8, b.stairs(b.brickStairs, 6));
                }
                if (k2 == 9) {
                    b.set(-8, 3, k1, b.stairs(b.brickStairs, 4));
                    b.set(-6, 3, k1, b.stairs(b.brickStairs, 5));
                    b.set(6, 3, k1, b.stairs(b.brickStairs, 4));
                    b.set(8, 3, k1, b.stairs(b.brickStairs, 5));
                }
                if (i2 == 9 && k2 <= 5 || k2 == 9 && i2 <= 5) b.set(i1, 6, k1, b.brickWall);
                if (i2 == 6 && k2 <= 5 || k2 == 6 && i2 <= 5) {
                    b.set(i1, 6, k1, b.brickWall);
                    if ((i2 != 6 || k2 > 3) && (k2 != 6 || i2 > 3)) {
                        b.set(i1, 5, k1, b.brick);
                    } else {
                        b.set(i1, 5, k1, DaleBuilder.slab(b.brickSlab, 8));
                    }
                    if (i1 == -5) {
                        b.set(i1, 4, k1, b.stairs(b.brickStairs, 4));
                    } else if (i1 == 5) {
                        b.set(i1, 4, k1, b.stairs(b.brickStairs, 5));
                    } else if (k1 == -5) {
                        b.set(i1, 4, k1, b.stairs(b.brickStairs, 7));
                    } else if (k1 == 5) {
                        b.set(i1, 4, k1, b.stairs(b.brickStairs, 6));
                    }
                }
                if (i2 <= 8 && k2 <= 8 && (i2 >= 7 || k2 >= 7)) {
                    if (i2 <= 2 || k2 <= 2) {
                        b.set(i1, 5, k1, b.plank);
                    } else if (i1 == -3) {
                        b.set(i1, 4, k1, b.stairs(b.plankStairs, 4));
                        b.set(i1, 5, k1, b.stairs(b.plankStairs, 1));
                    } else if (i1 == 3) {
                        b.set(i1, 4, k1, b.stairs(b.plankStairs, 5));
                        b.set(i1, 5, k1, b.stairs(b.plankStairs, 0));
                    } else if (k1 == -3) {
                        b.set(i1, 4, k1, b.stairs(b.plankStairs, 7));
                        b.set(i1, 5, k1, b.stairs(b.plankStairs, 2));
                    } else if (k1 == 3) {
                        b.set(i1, 4, k1, b.stairs(b.plankStairs, 6));
                        b.set(i1, 5, k1, b.stairs(b.plankStairs, 3));
                    } else {
                        b.set(i1, 4, k1, b.plank);
                    }
                }
                if (i2 == 6 && k2 == 6) {
                    for (int j1 = 1; j1 <= 5; j1++) b.set(i1, j1, k1, b.brick);
                }
                if ((i2 == 6 || i2 == 9) && (k2 == 6 || k2 == 9)) {
                    for (int j1 = 6; j1 <= 7; j1++) b.set(i1, j1, k1, b.brick);
                }
                if (i2 == 9 && (k2 == 7 || k2 == 8) || k2 == 9 && (i2 == 7 || i2 == 8)) {
                    for (int j1 = 6; j1 <= 7; j1++) b.set(i1, j1, k1, b.bars);
                }
            }
        }
        // The gates: three of them, and a wall where the fourth would be
        BlockState gate = Blocks.SPRUCE_FENCE.defaultBlockState();
        for (int j1 = 1; j1 <= 3; j1++) {
            for (int i1 = -1; i1 <= 1; i1++) {
                b.set(i1, j1, -9, gate);
                b.set(i1, j1, 9, b.brick);
            }
            for (int k1 = -1; k1 <= 1; k1++) {
                b.set(-9, j1, k1, gate);
                b.set(9, j1, k1, gate);
            }
        }
        for (int i1 : new int[] {-9, 6}) {
            for (int k1 : new int[] {-9, 6}) {
                for (int i2 = i1; i2 <= i1 + 3; i2++) {
                    b.set(i2, 8, k1, b.brick);
                    b.set(i2, 8, k1 + 3, b.brick);
                }
                for (int k2 = k1 + 1; k2 <= k1 + 2; k2++) {
                    b.set(i1, 8, k2, b.brick);
                    b.set(i1 + 3, 8, k2, b.brick);
                }
                for (int i2 = i1; i2 <= i1 + 3; i2++) {
                    for (int k2 = k1; k2 <= k1 + 3; k2++) {
                        b.set(i2, 9, k2, b.roof);
                        b.set(i2, 10, k2, DaleBuilder.slab(b.roofSlab, 0));
                    }
                }
                for (int i2 = i1 - 1; i2 <= i1 + 4; i2++) {
                    b.set(i2, 9, k1 - 1, b.stairs(b.roofStairs, 2));
                    b.set(i2, 9, k1 + 4, b.stairs(b.roofStairs, 3));
                }
                for (int k2 = k1; k2 <= k1 + 3; k2++) {
                    b.set(i1 - 1, 9, k2, b.stairs(b.roofStairs, 1));
                    b.set(i1 + 4, 9, k2, b.stairs(b.roofStairs, 0));
                }
                b.set(i1, 8, k1 - 1, b.stairs(b.roofStairs, 6));
                b.set(i1 + 3, 8, k1 - 1, b.stairs(b.roofStairs, 6));
                b.set(i1 + 4, 8, k1, b.stairs(b.roofStairs, 4));
                b.set(i1 + 4, 8, k1 + 3, b.stairs(b.roofStairs, 4));
                b.set(i1 + 3, 8, k1 + 4, b.stairs(b.roofStairs, 7));
                b.set(i1, 8, k1 + 4, b.stairs(b.roofStairs, 7));
                b.set(i1 - 1, 8, k1 + 3, b.stairs(b.roofStairs, 5));
                b.set(i1 - 1, 8, k1, b.stairs(b.roofStairs, 5));
            }
        }
        b.set(-6, 8, -8, b.stairs(b.roofStairs, 7));
        b.set(-6, 8, -7, b.stairs(b.roofStairs, 6));
        b.set(-7, 8, -6, b.stairs(b.roofStairs, 5));
        b.set(-8, 8, -6, b.stairs(b.roofStairs, 4));
        b.set(6, 8, -8, b.stairs(b.roofStairs, 7));
        b.set(6, 8, -7, b.stairs(b.roofStairs, 6));
        b.set(7, 8, -6, b.stairs(b.roofStairs, 4));
        b.set(8, 8, -6, b.stairs(b.roofStairs, 5));
        b.set(-6, 8, 8, b.stairs(b.roofStairs, 6));
        b.set(-6, 8, 7, b.stairs(b.roofStairs, 7));
        b.set(-7, 8, 6, b.stairs(b.roofStairs, 5));
        b.set(-8, 8, 6, b.stairs(b.roofStairs, 4));
        b.set(6, 8, 8, b.stairs(b.roofStairs, 6));
        b.set(6, 8, 7, b.stairs(b.roofStairs, 7));
        b.set(7, 8, 6, b.stairs(b.roofStairs, 4));
        b.set(8, 8, 6, b.stairs(b.roofStairs, 5));
        b.set(-8, 8, -8, b.torch(2));
        b.set(8, 8, -8, b.torch(1));
        b.set(-8, 8, 8, b.torch(2));
        b.set(8, 8, 8, b.torch(1));
        for (int i1 : new int[] {-2, 2}) {
            for (int j1 = 6; j1 <= 8; j1++) b.set(i1, j1, -9, b.pillar);
            b.set(i1, 9, -9, DaleBuilder.slab(b.roofSlab, 0));
            b.set(i1, 5, -10, b.torch(4));
            b.set(i1, 8, -10, b.wallBanner(2));
        }
        for (int j1 = 1; j1 <= 4; j1++) {
            b.set(-7, j1, -7, b.ladder(4));
            b.set(7, j1, -7, b.ladder(5));
        }
        b.set(0, 4, -8, b.torch(3));
        b.set(0, 4, 8, b.torch(4));
        b.set(-8, 4, 0, b.torch(2));
        b.set(8, 4, 0, b.torch(1));
        b.set(-6, 3, -5, b.torch(3));
        b.set(-5, 3, -6, b.torch(2));
        b.set(6, 3, -5, b.torch(3));
        b.set(5, 3, -6, b.torch(1));
        b.set(-6, 3, 5, b.torch(4));
        b.set(-5, 3, 6, b.torch(2));
        b.set(6, 3, 5, b.torch(4));
        b.set(5, 3, 6, b.torch(1));
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        for (int k1 = -8; k1 <= -4; k1++) {
            for (int i1 : new int[] {-3, -2, 2, 3}) b.set(i1, 0, k1, grass);
        }
        for (int k1 = 4; k1 <= 8; k1++) {
            for (int i1 : new int[] {-3, -2, 2, 3}) b.set(i1, 0, k1, grass);
        }
        for (int i1 = -8; i1 <= -4; i1++) {
            for (int k1 : new int[] {-3, -2, 2, 3}) b.set(i1, 0, k1, grass);
        }
        for (int i1 = 4; i1 <= 8; i1++) {
            for (int k1 : new int[] {-3, -2, 2, 3}) b.set(i1, 0, k1, grass);
        }
        // The hearth in the middle of the courtyard
        for (int i1 = -1; i1 <= 1; i1++) {
            for (int k1 = -1; k1 <= 1; k1++) {
                int i2 = Math.abs(i1);
                int k2 = Math.abs(k1);
                b.set(i1, 1, k1, b.brick);
                b.set(i1, 2, k1, b.brick);
                if (i2 == 1 && k2 == 1) {
                    b.set(i1, 3, k1, b.brickWall);
                } else if (i2 == 1 || k2 == 1) {
                    b.set(i1, 3, k1, b.brick);
                    b.set(i1, 4, k1, b.brick);
                    b.set(i1, 5, k1, b.brickWall);
                }
                if (i1 == 0 && k1 == 0) {
                    b.set(i1, 3, k1, b.brick);
                    b.set(i1, 4, k1, b.hearth());
                    b.air(i1, 5, k1);
                }
            }
        }
        b.set(0, 6, 0, b.roof);
        b.set(-1, 6, 0, b.stairs(b.roofStairs, 1));
        b.set(1, 6, 0, b.stairs(b.roofStairs, 0));
        b.set(0, 6, -1, b.stairs(b.roofStairs, 2));
        b.set(0, 6, 1, b.stairs(b.roofStairs, 3));
        for (int k1 = -7; k1 <= -4; k1++) {
            for (int i1 = -7; i1 <= -4; i1++) b.set(i1, 0, k1, Blocks.DIRT_PATH.defaultBlockState());
            for (int i1 = 4; i1 <= 7; i1++) b.set(i1, 0, k1, Blocks.DIRT_PATH.defaultBlockState());
        }
        b.set(-4, 1, -5, b.brickWall);
        b.set(-5, 1, -4, b.brickWall);
        b.set(5, 1, -4, b.brickWall);
        b.set(4, 1, -5, b.brickWall);
        for (int k1 = 4; k1 <= 7; k1++) {
            for (int i1 = -7; i1 <= -4; i1++) b.set(i1, 0, k1, b.plank);
            for (int i1 = 4; i1 <= 7; i1++) b.set(i1, 0, k1, b.plank);
        }
        for (int k1 = 4; k1 <= 6; k1++) {
            for (int i1 = -6; i1 <= -4; i1++) b.set(i1, 4, k1, Blocks.BLUE_WOOL.defaultBlockState());
            for (int i1 = 4; i1 <= 6; i1++) b.set(i1, 4, k1, Blocks.BLUE_WOOL.defaultBlockState());
        }
        for (int j1 = 1; j1 <= 3; j1++) {
            b.set(-4, j1, 4, b.fence);
            b.set(4, j1, 4, b.fence);
        }
        b.set(-4, 0, -4, grass);
        b.set(-3, 0, -3, grass);
        b.set(4, 0, -4, grass);
        b.set(3, 0, -3, grass);
        b.set(-4, 0, 4, grass);
        b.set(-3, 0, 3, grass);
        b.set(4, 0, 4, grass);
        b.set(3, 0, 3, grass);
        for (int j1 = 1; j1 <= 3; j1++) b.set(-7, j1, 7, b.plank);
        b.set(-7, 3, 6, b.stairs(b.plankStairs, 6));
        b.set(-6, 3, 7, b.stairs(b.plankStairs, 4));
        // Bunks, two high in the original; one high here, as vanilla beds don't stack
        b.set(-7, 1, 5, b.bed(0));
        b.set(-7, 1, 6, b.bed(8));
        b.set(-5, 1, 7, b.bed(3));
        b.set(-6, 1, 7, b.bed(11));
        for (int j1 = 1; j1 <= 3; j1++) b.set(7, j1, 7, b.plank);
        b.set(7, 3, 6, b.stairs(b.plankStairs, 6));
        b.set(6, 3, 7, b.stairs(b.plankStairs, 5));
        for (int i1 = 4; i1 <= 6; i1++) {
            b.set(i1, 1, 7, b.plank);
            if (i1 <= 5) b.set(i1, 2, 7, b.barrel(2));
        }
        for (int k1 = 4; k1 <= 6; k1++) {
            b.set(7, 1, k1, b.plank);
            if (k1 <= 5) b.set(7, 2, k1, b.barrel(5));
        }
        for (int i1 = -3; i1 <= 3; i1++) b.set(i1, 0, 8, b.floor);
        b.set(-3, 1, 8, b.plank);
        b.set(-2, 1, 8, b.plank);
        b.chest(random, -3, 2, 8, 2, DaleBuilder.WATCHTOWER_LOOT);
        b.chest(random, -2, 2, 8, 2, DaleBuilder.WATCHTOWER_LOOT);
        b.set(2, 1, 8, Blocks.FLETCHING_TABLE.defaultBlockState());
        b.set(3, 1, 8, Blocks.CRAFTING_TABLE.defaultBlockState());
        b.set(0, 1, 7, Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
        b.set(6, 1, 6, b.furnace(Blocks.FURNACE, 2));
    }
}
