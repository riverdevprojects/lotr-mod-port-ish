package com.lotrmod.worldgen.dale;

import com.lotrmod.block.DaleBlocks;
import com.lotrmod.worldgen.DaleTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * The Front Gate of Erebor, at the head of the gorge in the Mountain's south
 * face, where the River Running comes out.
 *
 * <p>A carved face of dwarven stone forty blocks wide and thirty-five high,
 * pillared, banded and corniced, with Durin's gold set above a great round
 * arch twenty blocks across. Through the arch the river runs out of a vaulted
 * tunnel, between two stone ledges; the gate's great doors stand open,
 * folded back against its walls; lanterns hang from the vault, braziers burn
 * on plinths before the face, and off the western ledge is the gate-wardens'
 * guardroom. At the tunnel's end the river wells up through a grating.
 *
 * <p>In the Desolation the gate is broken, as Smaug left it: the doors gone
 * to splinters and scattered iron, the top of the arch smashed open, the face
 * cracked and scorched black about the arch, the gold gone, the braziers cold
 * and fallen, rubble heaped in the gorge, and the guardroom a tomb.
 */
final class DaleGate {
    private DaleGate() {}

    private static final int GX = DaleTerrain.GATE_X;
    /** The outer face of the gate, and how far back into the rock the tunnel runs. */
    private static final int FACE = DaleTerrain.GATE_Z - 1;
    private static final int TUNNEL_END = FACE - 28;
    private static final int HALF = 20;
    private static final int TOP = 97;
    /** The arch: half its width, the height its curve springs from, and the ledges' floor. */
    private static final double ARCH = 10.0;
    private static final int SPRING = 72;
    private static final int LEDGE = 65;
    private static final int WATER = 62;
    private static final double CHANNEL = 7.0;

    static void build(WorldGenLevel level, BoundingBox box, DaleEra era) {
        if (box.maxX() < GX - HALF - 8 || box.minX() > GX + HALF + 8 || box.maxZ() < TUNNEL_END - 2 || box.minZ() > FACE + 12) return;
        boolean whole = era == DaleEra.FLOURISHING;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                int dx = x - GX;
                if (Math.abs(dx) > HALF + 8) continue;
                if (z >= TUNNEL_END - 1 && z <= FACE) {
                    for (int y = 54; y <= TOP + 2; y++) {
                        BlockState state = block(dx, y, z, whole);
                        if (state != null) level.setBlock(pos.set(x, y, z), state, 2);
                    }
                    // Above the carved face, the Mountain's own rock, up to the slope behind it
                    if (z >= FACE - 8 && Math.abs(dx) <= HALF + 8) {
                        int rock = DaleTerrain.groundY(x, FACE - 9);
                        for (int y = TOP + 1; y <= rock; y++) {
                            if (level.getBlockState(pos.set(x, y, z)).isAir()) level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
                        }
                    }
                } else if (z > FACE && z <= FACE + 12) {
                    front(level, pos, x, z, dx, whole);
                }
            }
        }
        guardroom(level, box, whole);
    }

    /** How far inside the arch's opening (dx, y) is: negative inside, the distance out to its edge otherwise. */
    private static double arch(int dx, int y) {
        if (y <= SPRING) return Math.abs(dx) - ARCH;
        double up = (y - SPRING) * 1.25;
        return Math.sqrt(dx * dx + up * up) - ARCH;
    }

    /** What the gate, or the tunnel behind it, is at this block, or null to leave it. */
    private static BlockState block(int dx, int y, int z, boolean whole) {
        int adx = Math.abs(dx);
        double a = arch(dx, y);
        double r = DaleTown.hash(dx * 31 + z, y, 41);
        boolean face = z >= FACE - 2;
        boolean tunnel = z < FACE - 2;

        // The river, running out between its ledges
        if (adx <= CHANNEL && y <= WATER && y >= 56) return Blocks.WATER.defaultBlockState();
        if (adx <= CHANNEL && y == 55) return Blocks.GRAVEL.defaultBlockState();
        if (z == TUNNEL_END - 1 && adx <= HALF) return stone(r, whole, false);
        if (z == TUNNEL_END) {
            // The end wall, the river welling up through a grating
            if (adx <= 3 && y >= 57 && y <= 61) return whole ? Blocks.IRON_BARS.defaultBlockState() : r < 0.5 ? Blocks.IRON_BARS.defaultBlockState() : Blocks.WATER.defaultBlockState();
            if (adx <= CHANNEL && y <= WATER) return Blocks.WATER.defaultBlockState();
            if (adx <= ARCH + 1 && y >= 54 && y <= TOP - 10) return stone(r, whole, false);
            return null;
        }

        if (a < 0.0 && y > WATER) {
            // Inside the arch: the ledges either side of the water, open above
            if (adx > CHANNEL && y <= LEDGE) return y == LEDGE ? ledge(dx, z, whole) : stone(r, whole, false);
            if (adx == (int) ARCH && y > LEDGE && y <= LEDGE + 13 && z <= FACE - 3 && z >= FACE - 8) {
                // The great doors, folded back against the walls; gone in the Desolation
                if (!whole) return Blocks.AIR.defaultBlockState();
                return (y - LEDGE) % 4 == 3 ? Blocks.IRON_BARS.defaultBlockState() : Blocks.DARK_OAK_PLANKS.defaultBlockState();
            }
            if (whole && tunnel && dx == 0 && Math.floorMod(z - FACE, 6) == 0 && arch(0, y + 1) >= 0.0) {
                return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
            }
            if (!whole && y == LEDGE + 1 && adx > CHANNEL && r < 0.18) return debris(r);
            return Blocks.AIR.defaultBlockState();
        }
        if (adx <= CHANNEL && y <= WATER) return Blocks.WATER.defaultBlockState();

        if (tunnel) {
            // The vault and walls of the tunnel, dressed stone, and solid above,
            // where the river's cut would otherwise open it to the sky
            if (adx <= HALF && y <= TOP) return a < 1.0 ? voussoir(r, whole) : stone(r, whole, false);
            return null;
        }

        // The carved face
        if (!face || adx > HALF || y > TOP || y < 54) return null;
        // Smashed open over the arch, where the dragon broke in
        if (!whole && y > SPRING + 1 && adx < 8 && a < 4.0 + DaleTown.hash(dx, y, 45) * 3.0 - adx * 0.25) return Blocks.AIR.defaultBlockState();
        if (a < 1.2) return voussoir(r, whole);
        if (y >= TOP - 1) {
            if (z == FACE) return y == TOP ? slab(whole, r) : DaleBlocks.DWARVEN_BRICK_STAIRS.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.NORTH)
                    .setValue(net.minecraft.world.level.block.StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP);
            return stone(r, whole, true);
        }
        if (adx == 12 || adx == 18) {
            return whole || r > 0.15 ? DaleBlocks.DWARVEN_PILLAR.get().defaultBlockState() : Blocks.AIR.defaultBlockState();
        }
        if (y == 83 || y == TOP - 3) return whole || r > 0.3 ? DaleBlocks.CHISELED_DWARVEN_BRICKS.get().defaultBlockState() : DaleBlocks.CRACKED_DWARVEN_BRICKS.get().defaultBlockState();
        // Durin's gold, set in the face above the arch
        if (z == FACE && adx <= 1 && y >= 86 && y <= 90) {
            if (!whole) return DaleBlocks.SCORCHED_STONE.get().defaultBlockState();
            return adx == 0 || y == 88 ? Blocks.GOLD_BLOCK.defaultBlockState() : DaleBlocks.CHISELED_DWARVEN_BRICKS.get().defaultBlockState();
        }
        // Scorched black about the arch, where the dragon's fire beat on it
        if (!whole && a < 7.0 && r < 0.6 - a * 0.07) return DaleBlocks.SCORCHED_STONE.get().defaultBlockState();
        return stone(r, whole, true);
    }

    private static BlockState stone(double r, boolean whole, boolean face) {
        if (!whole && r < 0.4) return DaleBlocks.CRACKED_DWARVEN_BRICKS.get().defaultBlockState();
        if (whole && face && r < 0.05) return DaleBlocks.CHISELED_DWARVEN_BRICKS.get().defaultBlockState();
        return DaleBlocks.DWARVEN_BRICKS.get().defaultBlockState();
    }

    private static BlockState voussoir(double r, boolean whole) {
        if (!whole && r < 0.35) return DaleBlocks.SCORCHED_STONE.get().defaultBlockState();
        return DaleBlocks.CHISELED_DWARVEN_BRICKS.get().defaultBlockState();
    }

    private static BlockState slab(boolean whole, double r) {
        if (!whole && r < 0.3) return Blocks.AIR.defaultBlockState();
        return DaleBlocks.DWARVEN_BRICK_SLAB.get().defaultBlockState();
    }

    private static BlockState ledge(int dx, int z, boolean whole) {
        if (whole) return Math.floorMod(z, 4) == 0 ? DaleBlocks.CHISELED_DWARVEN_BRICKS.get().defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState();
        return DaleTown.hash(dx, z, 42) < 0.4 ? Blocks.GRAVEL.defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState();
    }

    /** What the doors were smashed to, scattered on the ledges. */
    private static BlockState debris(double r) {
        if (r < 0.06) return DaleBlocks.CHARRED_PLANKS.get().defaultBlockState();
        if (r < 0.1) return Blocks.IRON_BARS.defaultBlockState();
        if (r < 0.14) return DaleBlocks.CRACKED_DWARVEN_BRICKS.get().defaultBlockState();
        return Blocks.GRAVEL.defaultBlockState();
    }

    /** In front of the face: the braziers on their plinths, and in the Desolation, rubble heaped on the gorge floor. */
    private static void front(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z, int dx, boolean whole) {
        int adx = Math.abs(dx);
        int ground = DaleTerrain.GORGE_FLOOR - 1;
        if (adx == 15 && z == FACE + 3) {
            int height = whole ? 3 : 1 + (int) (DaleTown.hash(x, z, 43) * 2);
            for (int y = ground + 1; y <= ground + height; y++) level.setBlock(pos.set(x, y, z), DaleBlocks.DWARVEN_BRICKS.get().defaultBlockState(), 2);
            if (whole) {
                level.setBlock(pos.set(x, ground + 4, z), DaleBlocks.CHISELED_DWARVEN_BRICKS.get().defaultBlockState(), 2);
                level.setBlock(pos.set(x, ground + 5, z), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, false), 2);
            }
            return;
        }
        if (whole || adx <= CHANNEL + 1) return;
        // Rubble from the broken arch, thickest before the gate
        double heap = (12.0 - (z - FACE)) / 12.0 * (1.0 - adx / (double) (HALF + 8));
        double r = DaleTown.hash(x, z, 44);
        if (r < heap * 0.7) {
            pos.set(x, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, x, z), z);
            if (pos.getY() > WATER + 1) level.setBlock(pos, debris(r * 0.5 + 0.1 * heap), 2);
        }
    }

    /**
     * The gate-wardens' guardroom, off the western ledge: a chamber cut in the
     * rock with a table, a chest and lanterns. In the Desolation, its door
     * broken in and the bones of its wardens on the floor.
     */
    private static void guardroom(WorldGenLevel level, BoundingBox box, boolean whole) {
        int x0 = GX - 18, x1 = GX - 11, z0 = FACE - 21, z1 = FACE - 13;
        if (box.maxX() < x0 || box.minX() > x1 || box.maxZ() < z0 || box.minZ() > z1) return;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int floor = LEDGE;
        for (int x = Math.max(x0, box.minX()); x <= Math.min(x1, box.maxX()); x++) {
            for (int z = Math.max(z0, box.minZ()); z <= Math.min(z1, box.maxZ()); z++) {
                boolean wall = x == x0 || x == x1 || z == z0 || z == z1;
                for (int y = floor; y <= floor + 6; y++) {
                    BlockState state;
                    if (y == floor || y == floor + 6 || wall) {
                        state = DaleBlocks.DWARVEN_BRICKS.get().defaultBlockState();
                    } else {
                        state = Blocks.AIR.defaultBlockState();
                    }
                    level.setBlock(pos.set(x, y, z), state, 2);
                }
            }
        }
        // The doorway onto the ledge
        int dz = (z0 + z1) / 2;
        for (int y = floor + 1; y <= floor + 3; y++) set(level, box, pos.set(x1, y, dz), Blocks.AIR.defaultBlockState());
        set(level, box, pos.set(x1, floor + 1, dz + 1), Blocks.AIR.defaultBlockState());
        if (whole) {
            set(level, box, pos.set(x0 + 1, floor + 1, z0 + 1), Blocks.SMITHING_TABLE.defaultBlockState());
            set(level, box, pos.set(x0 + 2, floor + 1, z0 + 1), Blocks.ANVIL.defaultBlockState());
            set(level, box, pos.set(x0 + 1, floor + 1, z1 - 1), Blocks.BARREL.defaultBlockState());
            set(level, box, pos.set(x0 + 3, floor + 5, dz), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            set(level, box, pos.set(x1 - 2, floor + 5, dz), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        } else {
            for (int[] bone : new int[][] {{2, 2}, {3, 5}, {5, 3}, {2, 6}}) {
                set(level, box, pos.set(x0 + bone[0], floor + 1, z0 + bone[1]), Blocks.BONE_BLOCK.defaultBlockState());
            }
            set(level, box, pos.set(x0 + 4, floor + 1, z0 + 2), Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, 5));
            set(level, box, pos.set(x0 + 3, floor + 1, z1 - 2), Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, 11));
            set(level, box, pos.set(x0 + 5, floor + 1, z1 - 1), Blocks.COBWEB.defaultBlockState());
            set(level, box, pos.set(x0 + 1, floor + 5, z0 + 1), Blocks.COBWEB.defaultBlockState());
        }
        BlockPos chest = new BlockPos(x0 + 1, floor + 1, dz);
        if (box.isInside(chest)) {
            level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST), 2);
            if (level.getBlockEntity(chest) instanceof RandomizableContainerBlockEntity c) {
                c.setLootTable(whole ? DaleBuilder.GATE_LOOT : DaleBuilder.RUINS_LOOT, chest.asLong() * 31L);
            }
        }
    }

    private static void set(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
        if (box.isInside(pos)) level.setBlock(pos, state, 2);
    }
}
