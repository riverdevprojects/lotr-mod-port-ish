package com.lotrmod.worldgen.structure;

import com.lotrmod.LOTRMod;
import com.lotrmod.block.HobbitGateBlock;
import com.lotrmod.block.ModBlocks;
import com.lotrmod.entity.HobbitEntity;
import com.lotrmod.entity.HobbitNames;
import com.lotrmod.entity.ModEntities;
import com.lotrmod.worldgen.LOTRWorldGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.HashSet;
import java.util.Set;

/**
 * The building of a hobbit hole: a line-for-line port of the original mod's
 * {@code LOTRWorldGenHobbitHole.generateWithSetRotation}, with the materials
 * its {@code LOTRWorldGenHobbitStructure.setupRandomBlocks} rolled.
 *
 * <h2>Layout</h2>
 * A grassy mound 16 blocks in radius and 7 high, a brick chimney with a flower
 * pot on top, and a fenced front garden with a path, flowers and sometimes a
 * hedge and a notice on the fence. The round two-by-two front door is one of
 * four colours of hobbit gate, set in a brick front wall under a porch of
 * green clay tiles between two oak beams, with the family's name on a sign
 * beside it. Inside, a long hall runs back to a round sitting room with a
 * window at its back; off the hall are a study (bookshelves, a writing desk,
 * a chest), a bedroom (two beds, a clock in a frame), and through the sitting
 * room a dining room, a kitchen with two hobbit ovens, a cauldron and a
 * hobbit table, and a larder with four chests of food. One hole in five is
 * wealthy, with fruit-wood walls, silver or gold chandeliers, and half the
 * time a chest of treasure under the sitting-room floor.
 *
 * <h2>Coordinates</h2>
 * Kept exactly as the original's: {@code i} across the hole, {@code k} from
 * the garden gate ({@code -17}) back into the hill, {@code j} up from the
 * ground the gate stands on, all relative to the mound's centre and turned by
 * the hole's rotation through {@link #worldX}/{@link #worldZ}. Block facings
 * are written as the original's metadata, which reads as world directions for
 * rotation 0, and turned clockwise once per rotation step, as the original's
 * {@code rotateMeta} did.
 *
 * <h2>A chunk at a time</h2>
 * The original placed a hole in one go; here each chunk the hole touches
 * builds its own share. Every chunk replays the whole build from the same
 * seed, so the random rolls come out the same, and only writes the blocks
 * that fall inside it, much as the original did when given a bounding box.
 * Reads outside the chunk see air, as the original's did outside its box.
 *
 * <h2>Not ported</h2>
 * The plates and mugs of food on the dining table and larder shelf, the wall
 * banner in the study and the hobbits' wedding rings are left out. The
 * original's own woods, pies, keg and flowers have vanilla stand-ins; see
 * {@link Palette}.
 */
public class HobbitHolePiece extends StructurePiece {
    /** How far the mound's centre lies behind the garden gate. */
    static final int SHIFT = 17;

    private static final int RADIUS = HobbitHoleStructure.RADIUS;
    private static final int HEIGHT = 7;
    private static final int EXTRA_RADIUS = 2;

    static final ResourceKey<LootTable> STUDY_LOOT = chestLoot("hobbit_hole_study");
    static final ResourceKey<LootTable> LARDER_LOOT = chestLoot("hobbit_hole_larder");
    static final ResourceKey<LootTable> TREASURE_LOOT = chestLoot("hobbit_hole_treasure");

    private static final ResourceKey<ConfiguredFeature<?, ?>> SHIRE_TREES = ResourceKey.create(
            Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(LOTRMod.MODID, "trees_shire"));

    private final int originX;
    private final int originY;
    private final int originZ;
    private final int rotation;
    private final long seed;

    public HobbitHolePiece(BlockPos centre, int rotation, long seed) {
        super(LOTRWorldGen.HOBBIT_HOLE_PIECE.get(), 0, boundingBox(centre, rotation));
        this.originX = centre.getX();
        this.originY = centre.getY();
        this.originZ = centre.getZ();
        this.rotation = rotation;
        this.seed = seed;
    }

    public HobbitHolePiece(CompoundTag tag) {
        super(LOTRWorldGen.HOBBIT_HOLE_PIECE.get(), tag);
        this.originX = tag.getInt("OX");
        this.originY = tag.getInt("OY");
        this.originZ = tag.getInt("OZ");
        this.rotation = tag.getInt("Rot");
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("OX", originX);
        tag.putInt("OY", originY);
        tag.putInt("OZ", originZ);
        tag.putInt("Rot", rotation);
        tag.putLong("Seed", seed);
    }

    /** The mound's centre for a hole whose garden gate is at the given spot. */
    static BlockPos centre(int gateX, int y, int gateZ, int rotation) {
        return switch (rotation) {
            case 0 -> new BlockPos(gateX, y, gateZ + SHIFT);
            case 1 -> new BlockPos(gateX - SHIFT, y, gateZ);
            case 2 -> new BlockPos(gateX, y, gateZ - SHIFT);
            default -> new BlockPos(gateX + SHIFT, y, gateZ);
        };
    }

    private static BoundingBox boundingBox(BlockPos centre, int rotation) {
        int x0 = worldX(centre.getX(), rotation, -RADIUS, -SHIFT);
        int z0 = worldZ(centre.getZ(), rotation, -RADIUS, -SHIFT);
        int x1 = worldX(centre.getX(), rotation, RADIUS, RADIUS);
        int z1 = worldZ(centre.getZ(), rotation, RADIUS, RADIUS);
        return new BoundingBox(Math.min(x0, x1), centre.getY() - 8, Math.min(z0, z1),
                Math.max(x0, x1), centre.getY() + 32, Math.max(z0, z1));
    }

    private static int worldX(int originX, int rotation, int i, int k) {
        return switch (rotation) {
            case 0 -> originX - i;
            case 1 -> originX - k;
            case 2 -> originX + i;
            default -> originX + k;
        };
    }

    private static int worldZ(int originZ, int rotation, int i, int k) {
        return switch (rotation) {
            case 0 -> originZ + k;
            case 1 -> originZ - i;
            case 2 -> originZ - k;
            default -> originZ + i;
        };
    }

    private static ResourceKey<LootTable> chestLoot(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(LOTRMod.MODID, "chests/" + name));
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
            RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        new Builder(level, generator, chunkBox).build(RandomSource.create(seed));
    }

    /** One chunk's run through the build. */
    private final class Builder {
        private final WorldGenLevel level;
        private final ChunkGenerator generator;
        private final BoundingBox box;
        private Palette palette;

        Builder(WorldGenLevel level, ChunkGenerator generator, BoundingBox box) {
            this.level = level;
            this.generator = generator;
            this.box = box;
        }

        void build(RandomSource random) {
            Palette p = palette = new Palette(random);

            // The mound, and the ground beneath it filled in down to solid
            for (int i1 = -RADIUS; i1 <= RADIUS; i1++) {
                for (int k1 = -RADIUS; k1 <= RADIUS; k1++) {
                    if (!columnInBox(i1, k1)) continue;
                    for (int j1 = HEIGHT; j1 >= 0; j1--) {
                        int j2 = j1 + (RADIUS - HEIGHT);
                        if (i1 * i1 + j2 * j2 + k1 * k1 < (RADIUS + EXTRA_RADIUS) * (RADIUS + EXTRA_RADIUS)) {
                            boolean grass = !isOpaque(i1, j1 + 1, k1);
                            set(i1, j1, k1, grass ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState());
                            setGrassToDirt(i1, j1 - 1, k1);
                        }
                    }
                }
            }
            for (int i1 = -RADIUS; i1 <= RADIUS; i1++) {
                for (int k1 = -RADIUS; k1 <= RADIUS; k1++) {
                    if (i1 * i1 + k1 * k1 < RADIUS * RADIUS && columnInBox(i1, k1)) {
                        for (int j1 = -1; !isOpaque(i1, j1, k1) && y(j1) >= level.getMinBuildHeight(); j1--) {
                            boolean grass = !isOpaque(i1, j1 + 1, k1);
                            set(i1, j1, k1, grass ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState());
                            setGrassToDirt(i1, j1 - 1, k1);
                        }
                    }
                }
            }

            // Chimney
            setGrassToDirt(0, 7, 0);
            set(0, 8, 0, Blocks.BRICKS.defaultBlockState());
            set(0, 9, 0, Blocks.BRICKS.defaultBlockState());
            set(0, 10, 0, Blocks.FLOWER_POT.defaultBlockState());

            // Front garden, cut into the mound and levelled
            for (int k1 = -16; k1 <= -13; k1++) {
                for (int j1 = 1; j1 <= 4; j1++) {
                    for (int i1 = -3; i1 <= 3; i1++) {
                        setAir(i1, j1, k1);
                    }
                }
            }
            for (int j1 = 1; j1 <= 3; j1++) {
                for (int i1 = -2; i1 <= 2; i1++) {
                    setAir(i1, j1, -12);
                }
            }
            for (int k1 = -17; k1 <= -13; k1++) {
                for (int i1 = -5; i1 <= 5; i1++) {
                    if (!columnInBox(i1, k1)) continue;
                    for (int j1 = 0; j1 == 0 || !isOpaque(i1, j1, k1) && y(j1) >= level.getMinBuildHeight(); j1--) {
                        set(i1, j1, k1, j1 == 0 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState());
                    }
                    for (int j1 = 1; j1 <= 3; j1++) {
                        setAir(i1, j1, k1);
                    }
                }
            }
            for (int k1 = -16; k1 <= -13; k1++) {
                set(4, 1, k1, p.outFence);
                set(-4, 1, k1, p.outFence);
                set(0, 0, k1, p.path);
            }
            for (int i1 = -1; i1 <= 1; i1++) {
                set(i1, 0, -12, p.path);
                set(i1, 0, -11, p.path);
            }
            for (int i1 = -3; i1 <= 3; i1++) {
                set(i1, 1, -16, p.outFence);
            }
            set(0, 1, -16, Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, dir4(0)));
            if (random.nextInt(5) == 0) {
                String[] signLines = HobbitNames.gateSign(random);
                int[] signPos = {-3, -2, -1, 1, 2, 3};
                int i1 = signPos[random.nextInt(signPos.length)];
                int signMeta = Mth.nextInt(random, 6, 10) & 15;
                placeSign(i1, 2, -16, Blocks.OAK_SIGN.defaultBlockState()
                        .setValue(StandingSignBlock.ROTATION, (signMeta + rotation * 4) & 15), signLines);
            }
            for (int k1 = -15; k1 <= -13; k1++) {
                for (int i1 : new int[]{-1, 1}) {
                    plantFlower(random, i1, 1, k1);
                }
            }
            if (random.nextInt(3) == 0) {
                for (int k1 = -14; k1 <= -13; k1++) {
                    for (int i1 : new int[]{-2, 2}) {
                        set(i1, 1, k1, p.hedge);
                    }
                }
            }

            // Front wall and door
            for (int i1 = -2; i1 <= 2; i1++) {
                for (int j1 = 1; j1 <= 3; j1++) {
                    set(i1, j1, -10, Blocks.BRICKS.defaultBlockState());
                }
            }
            boolean gateFlip = random.nextBoolean();
            if (gateFlip) {
                placeDoor(0, 1);
                set(-2, 1, -11, p.plank2);
                set(-2, 2, -11, p.plank2);
                set(-2, 3, -11, p.plank2);
                set(-2, 1, -12, stairs(p.plank2Stairs, 2));
                set(-2, 3, -12, stairs(p.plank2Stairs, 6));
                set(-1, 3, -12, stairs(p.plank2Stairs, 4));
                set(-1, 1, -11, stairs(p.plank2Stairs, 0));
                set(-1, 2, -11, stairs(p.plank2Stairs, 4));
                set(-1, 3, -11, p.plank2);
                set(0, 3, -11, stairs(p.plank2Stairs, 4));
                set(0, 3, -12, slab(p.plank2Slab, true));
                set(1, 3, -11, stairs(p.plank2Stairs, 5));
                set(1, 3, -12, slab(p.plank2Slab, true));
                set(2, 1, -11, stairs(p.plank2Stairs, 1));
                set(2, 2, -11, stairs(p.plank2Stairs, 5));
                set(2, 3, -11, p.plank2);
                set(2, 1, -12, stairs(p.plank2Stairs, 2));
                set(2, 3, -12, stairs(p.plank2Stairs, 6));
                placeSign(-2, 2, -12, wallSign(2), new String[]{"", p.names.surname(), p.names.home(), ""});
            } else {
                placeDoor(-1, 0);
                set(2, 1, -11, p.plank2);
                set(2, 2, -11, p.plank2);
                set(2, 3, -11, p.plank2);
                set(2, 1, -12, stairs(p.plank2Stairs, 2));
                set(2, 3, -12, stairs(p.plank2Stairs, 6));
                set(1, 3, -12, stairs(p.plank2Stairs, 5));
                set(1, 1, -11, stairs(p.plank2Stairs, 1));
                set(1, 2, -11, stairs(p.plank2Stairs, 5));
                set(1, 3, -11, p.plank2);
                set(0, 3, -11, stairs(p.plank2Stairs, 5));
                set(0, 3, -12, slab(p.plank2Slab, true));
                set(-1, 3, -11, stairs(p.plank2Stairs, 4));
                set(-1, 3, -12, slab(p.plank2Slab, true));
                set(-2, 1, -11, stairs(p.plank2Stairs, 0));
                set(-2, 2, -11, stairs(p.plank2Stairs, 4));
                set(-2, 3, -11, p.plank2);
                set(-2, 1, -12, stairs(p.plank2Stairs, 2));
                set(-2, 3, -12, stairs(p.plank2Stairs, 6));
                placeSign(2, 2, -12, wallSign(2), new String[]{"", p.names.surname(), p.names.home(), ""});
            }

            // Porch: oak beams and a roof of green clay tiles
            for (int j1 = 1; j1 <= 3; j1++) {
                set(-3, j1, -12, ModBlocks.WOOD_BEAM_OAK.get().defaultBlockState());
                set(3, j1, -12, ModBlocks.WOOD_BEAM_OAK.get().defaultBlockState());
            }
            BlockState tileSlab = ModBlocks.CLAY_TILE_GREEN_SLAB.get().defaultBlockState();
            for (int i1 = -3; i1 <= 3; i1++) {
                if (Math.abs(i1) <= 1) {
                    set(i1, 4, -13, tileSlab);
                } else {
                    set(i1, 3, -13, tileSlab.setValue(SlabBlock.TYPE, SlabType.TOP));
                }
            }
            set(-4, 3, -13, tileSlab);
            set(4, 3, -13, tileSlab);

            // The hall
            for (int k1 = -9; k1 <= 1; k1++) {
                for (int i1 = -2; i1 <= 2; i1++) {
                    for (int j1 = 1; j1 <= 3; j1++) {
                        setAir(i1, j1, k1);
                    }
                }
                set(1, 0, k1, p.floor);
                set(0, 0, k1, p.plank);
                set(-1, 0, k1, p.floor);
                set(2, 1, k1, stairs(p.plank2Stairs, 1));
                set(-2, 1, k1, stairs(p.plank2Stairs, 0));
                for (int j1 = 1; j1 <= 3; j1++) {
                    set(3, j1, k1, p.plank2);
                    set(-3, j1, k1, p.plank2);
                }
                set(2, 3, k1, p.plank2);
                set(-2, 3, k1, p.plank2);
                set(1, 3, k1, slab(p.plank2Slab, true));
                set(-1, 3, k1, slab(p.plank2Slab, true));
                set(0, 4, k1, p.plank2);
            }
            for (int k1 : new int[]{-8, -4, 0}) {
                set(0, 3, k1, p.chandelier);
            }

            // Sitting room
            for (int j1 = 1; j1 <= 3; j1++) {
                for (int i1 = -2; i1 <= 2; i1++) {
                    set(i1, j1, 2, p.plank2);
                }
            }
            set(0, 0, 2, p.plank);
            setAir(0, 1, 2);
            setAir(0, 2, 2);
            set(-1, 1, 2, stairs(p.plank2Stairs, 0));
            set(1, 1, 2, stairs(p.plank2Stairs, 1));
            set(-1, 2, 2, stairs(p.plank2Stairs, 4));
            set(1, 2, 2, stairs(p.plank2Stairs, 5));
            for (int k1 = 3; k1 <= 9; k1++) {
                for (int i1 = -3; i1 <= 3; i1++) {
                    for (int j1 = 1; j1 <= 3; j1++) {
                        setAir(i1, j1, k1);
                    }
                    set(i1, 4, k1, p.plank2);
                    set(i1, 0, k1, p.plank);
                }
            }
            set(0, 3, 6, p.chandelier);
            for (int k1 = 5; k1 <= 7; k1++) {
                for (int i1 = -1; i1 <= 1; i1++) {
                    set(i1, 1, k1, p.carpet);
                }
            }
            if (p.isWealthy && random.nextBoolean()) {
                placeChest(random, 0, 0, 6, facing(2), TREASURE_LOOT);
            }
            for (int i1 = -3; i1 <= 3; i1++) {
                for (int j1 = 1; j1 <= 3; j1++) {
                    set(i1, j1, 10, p.plank2);
                }
            }
            for (int i1 = -1; i1 <= 1; i1++) {
                set(i1, 2, 10, Blocks.GLASS_PANE.defaultBlockState());
                set(i1, 3, 10, Blocks.GLASS_PANE.defaultBlockState());
                for (int k1 = 11; k1 <= 14; k1++) {
                    set(i1, 1, k1, Blocks.GRASS_BLOCK.defaultBlockState());
                    for (int j1 = 2; j1 <= 3; j1++) {
                        setAir(i1, j1, k1);
                    }
                }
                set(i1, 4, 10, p.plank2);
            }
            for (int j1 = 1; j1 <= 3; j1++) {
                set(-3, j1, 3, p.plank2);
                set(3, j1, 3, p.plank2);
            }
            for (int i1 = -2; i1 <= 2; i1++) {
                set(i1, 3, 3, slab(p.plank2Slab, true));
            }
            for (int k1 = 4; k1 <= 9; k1++) {
                set(-3, 3, k1, slab(p.plank2Slab, true));
                set(3, 3, k1, slab(p.plank2Slab, true));
            }
            for (int i1 = -3; i1 <= 3; i1++) {
                set(i1, 1, 9, stairs(Blocks.OAK_STAIRS, 2));
            }
            set(-3, 1, 8, stairs(Blocks.OAK_STAIRS, 0));
            set(3, 1, 8, stairs(Blocks.OAK_STAIRS, 1));
            for (int k1 = 4; k1 <= 9; k1++) {
                for (int j1 = 1; j1 <= 3; j1++) {
                    set(-4, j1, k1, p.plank2);
                    set(4, j1, k1, p.plank2);
                }
            }
            setAir(-4, 1, 6);
            setAir(-4, 2, 6);
            set(-4, 1, 5, stairs(p.plank2Stairs, 3));
            set(-4, 1, 7, stairs(p.plank2Stairs, 2));
            set(-4, 2, 5, stairs(p.plank2Stairs, 7));
            set(-4, 2, 7, stairs(p.plank2Stairs, 6));
            setAir(4, 1, 6);
            setAir(4, 2, 6);
            set(4, 1, 5, stairs(p.plank2Stairs, 3));
            set(4, 1, 7, stairs(p.plank2Stairs, 2));
            set(4, 2, 5, stairs(p.plank2Stairs, 7));
            set(4, 2, 7, stairs(p.plank2Stairs, 6));
            set(-3, 2, 4, torch(3));
            set(3, 2, 4, torch(3));
            set(-3, 2, 9, torch(2));
            set(3, 2, 9, torch(1));

            // Study
            setAir(2, 1, -6);
            set(2, 0, -6, p.floor);
            set(3, 0, -6, p.floor);
            setAir(3, 1, -6);
            setAir(3, 2, -6);
            set(3, 1, -7, stairs(p.plank2Stairs, 3));
            set(3, 1, -5, stairs(p.plank2Stairs, 2));
            set(3, 2, -7, stairs(p.plank2Stairs, 7));
            set(3, 2, -5, stairs(p.plank2Stairs, 6));
            for (int k1 = -8; k1 <= -3; k1++) {
                for (int i1 = 4; i1 <= 8; i1++) {
                    if (i1 != 8 || k1 != -8) {
                        for (int j1 = 1; j1 <= 3; j1++) {
                            setAir(i1, j1, k1);
                        }
                        set(i1, 0, k1, p.floor);
                        if (i1 < 7 || k1 > -7) {
                            set(i1, 4, k1, p.plank2);
                        }
                    }
                }
            }
            for (int i1 = 4; i1 <= 7; i1++) {
                for (int j1 = 1; j1 <= 3; j1++) {
                    set(i1, j1, -2, p.plank2);
                    set(i1, j1, -8, p.plank2);
                }
                set(i1, 3, -7, slab(p.plank2Slab, true));
                set(i1, 3, -3, slab(p.plank2Slab, true));
            }
            for (int k1 = -7; k1 <= -3; k1++) {
                for (int j1 = 1; j1 <= 3; j1++) {
                    set(8, j1, k1, p.plank2);
                }
            }
            for (int j1 = 1; j1 <= 2; j1++) {
                for (int i1 = 5; i1 <= 6; i1++) {
                    setAir(i1, j1, -8);
                    set(i1, j1, -9, Blocks.BOOKSHELF.defaultBlockState());
                }
                for (int k1 = -6; k1 <= -4; k1++) {
                    setAir(8, j1, k1);
                    set(9, j1, k1, Blocks.BOOKSHELF.defaultBlockState());
                }
            }
            set(6, 3, -5, p.chandelier);
            set(5, 1, -5, stairs(Blocks.OAK_STAIRS, 3));
            set(5, 1, -3, slab(Blocks.OAK_SLAB, true));
            placeChest(random, 7, 1, -3, facing(2), STUDY_LOOT);
            // The original hung a hobbit banner here half the time
            random.nextBoolean();

            // Bedroom
            setAir(-2, 1, -6);
            set(-2, 0, -6, p.floor);
            set(-3, 0, -6, p.floor);
            setAir(-3, 1, -6);
            setAir(-3, 2, -6);
            set(-3, 1, -7, stairs(p.plank2Stairs, 3));
            set(-3, 1, -5, stairs(p.plank2Stairs, 2));
            set(-3, 2, -7, stairs(p.plank2Stairs, 7));
            set(-3, 2, -5, stairs(p.plank2Stairs, 6));
            for (int k1 = -7; k1 <= -4; k1++) {
                for (int i1 = -4; i1 >= -7; i1--) {
                    set(i1, 0, k1, p.floor);
                    for (int j1 = 1; j1 <= 3; j1++) {
                        setAir(i1, j1, k1);
                    }
                    set(i1, 4, k1, p.plank2);
                }
            }
            for (int i1 = -4; i1 >= -7; i1--) {
                for (int j1 = 1; j1 <= 3; j1++) {
                    set(i1, j1, -8, p.plank2);
                    set(i1, j1, -3, p.plank2);
                }
                set(i1, 3, -7, slab(p.plank2Slab, true));
                set(i1, 3, -4, slab(p.plank2Slab, true));
            }
            for (int k1 = -7; k1 <= -3; k1++) {
                for (int j1 = 1; j1 <= 3; j1++) {
                    set(-8, j1, k1, p.plank2);
                }
            }
            for (int k1 = -7; k1 <= -6; k1++) {
                for (int i1 = -5; i1 >= -6; i1--) {
                    set(i1, 1, k1, p.carpet);
                }
            }
            for (int i1 = -5; i1 >= -6; i1--) {
                set(i1, 0, -8, p.floor);
                set(i1, 1, -8, slab(Blocks.OAK_SLAB, true));
                set(i1, 2, -8, Blocks.BOOKSHELF.defaultBlockState());
                set(i1, 1, -9, p.plank2);
            }
            set(-4, 1, -4, Blocks.OAK_PLANKS.defaultBlockState());
            set(-7, 1, -4, Blocks.OAK_PLANKS.defaultBlockState());
            set(-4, 2, -4, torch(5));
            set(-7, 2, -4, torch(5));
            set(-5, 1, -5, bed(0, false));
            set(-5, 1, -4, bed(0, true));
            set(-6, 1, -5, bed(0, false));
            set(-6, 1, -4, bed(0, true));
            spawnItemFrame(-8, 2, -6, dir4(1), new ItemStack(Items.CLOCK));

            // Dining room
            set(4, 0, 6, p.plank);
            for (int i1 = 5; i1 <= 6; i1++) {
                set(i1, 0, 7, p.floor);
                for (int j1 = 1; j1 <= 3; j1++) {
                    setAir(i1, j1, 7);
                }
                set(i1, 4, 7, p.plank2);
            }
            for (int i1 = 5; i1 <= 7; i1++) {
                set(i1, 0, 6, p.floor);
                for (int j1 = 1; j1 <= 3; j1++) {
                    setAir(i1, j1, 6);
                }
                set(i1, 4, 6, p.plank2);
            }
            for (int i1 = 5; i1 <= 8; i1++) {
                set(i1, 0, 5, p.floor);
                for (int j1 = 1; j1 <= 3; j1++) {
                    setAir(i1, j1, 5);
                }
                set(i1, 4, 5, p.plank2);
            }
            for (int j1 = 1; j1 <= 3; j1++) {
                set(7, j1, 7, p.plank2);
                set(8, j1, 6, p.plank2);
                set(9, j1, 5, p.plank2);
            }
            set(7, 2, 6, torch(4));
            set(8, 2, 5, torch(1));
            for (int k1 = 4; k1 >= -1; k1--) {
                for (int i1 = 4; i1 <= 9; i1++) {
                    set(i1, 0, k1, p.floor);
                    for (int j1 = 1; j1 <= 3; j1++) {
                        setAir(i1, j1, k1);
                    }
                    set(i1, 4, k1, p.plank2);
                }
                for (int j1 = 1; j1 <= 3; j1++) {
                    set(3, j1, k1, p.plank2);
                    set(10, j1, k1, p.plank2);
                }
                set(4, 3, k1, slab(p.plank2Slab, true));
                set(9, 3, k1, slab(p.plank2Slab, true));
            }
            for (int k1 = 2; k1 >= 0; k1--) {
                set(4, 1, k1, stairs(Blocks.OAK_STAIRS, 0));
                set(9, 1, k1, stairs(Blocks.OAK_STAIRS, 1));
            }
            set(4, 1, -1, stairs(Blocks.OAK_STAIRS, 3));
            set(4, 1, 3, stairs(Blocks.OAK_STAIRS, 2));
            set(9, 1, -1, stairs(Blocks.OAK_STAIRS, 3));
            set(9, 1, 3, stairs(Blocks.OAK_STAIRS, 2));
            set(6, 3, 1, p.chandelier);
            set(7, 3, 1, p.chandelier);
            set(6, 1, 2, Blocks.SPRUCE_PLANKS.defaultBlockState());
            set(7, 1, 2, Blocks.SPRUCE_PLANKS.defaultBlockState());
            set(6, 1, 1, slab(Blocks.SPRUCE_SLAB, true));
            set(7, 1, 1, slab(Blocks.SPRUCE_SLAB, true));
            set(6, 1, 0, Blocks.SPRUCE_PLANKS.defaultBlockState());
            set(7, 1, 0, Blocks.SPRUCE_PLANKS.defaultBlockState());
            // The original laid the table with plates of food and mugs of drink here
            set(5, 3, 7, slab(p.plank2Slab, true));
            set(6, 3, 7, slab(p.plank2Slab, true));
            set(7, 3, 6, slab(p.plank2Slab, true));
            set(8, 3, 5, slab(p.plank2Slab, true));
            for (int j1 = 1; j1 <= 3; j1++) {
                for (int i1 = 5; i1 <= 6; i1++) {
                    set(i1, j1, 8, p.plank2);
                }
                for (int i1 = 8; i1 <= 9; i1++) {
                    set(i1, j1, -2, p.plank2);
                }
            }

            // Kitchen
            set(-4, 0, 6, p.plank);
            BlockState smoothStone = Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE);
            for (int k1 = 7; k1 >= 3; k1--) {
                for (int i1 = -5; i1 >= -7; i1--) {
                    set(i1, 0, k1, smoothStone);
                    for (int j1 = 1; j1 <= 3; j1++) {
                        setAir(i1, j1, k1);
                    }
                    set(i1, 4, k1, p.plank2);
                }
            }
            for (int k1 = 6; k1 >= 3; k1--) {
                for (int i1 = -5; i1 >= -6; i1--) {
                    set(i1, 0, k1, p.floor);
                }
            }
            set(-5, 1, 8, Blocks.CRAFTING_TABLE.defaultBlockState());
            set(-6, 1, 8, Blocks.CRAFTING_TABLE.defaultBlockState());
            set(-7, 1, 8, ModBlocks.HOBBIT_TABLE.get().defaultBlockState());
            for (int i1 = -7; i1 <= -5; i1++) {
                setAir(i1, 2, 8);
                set(i1, 2, 9, p.plank2);
                set(i1, 3, 8, smoothStone);
            }
            set(-8, 1, 8, p.plank2);
            set(-8, 2, 8, p.plank2);
            for (int k1 = 6; k1 <= 7; k1++) {
                for (int j1 = 1; j1 <= 3; j1++) {
                    set(-8, j1, k1, p.plank2);
                }
            }
            for (int k1 = 3; k1 <= 5; k1++) {
                set(-8, 0, k1, smoothStone);
                setAir(-8, 2, k1);
                set(-9, 2, k1, p.plank2);
                set(-8, 3, k1, smoothStone);
            }
            BlockState oven = ModBlocks.HOBBIT_OVEN.get().defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, facing(4));
            set(-8, 1, 4, p.plank2);
            set(-8, 1, 5, oven);
            set(-8, 1, 3, oven);
            set(-8, 1, 4, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
            set(-6, 3, 5, p.chandelier);
            for (int i1 = -4; i1 >= -9; i1--) {
                for (int j1 = 1; j1 <= 3; j1++) {
                    set(i1, j1, 2, p.plank2);
                }
            }
            set(-6, 0, 2, p.plank);
            setAir(-6, 1, 2);
            setAir(-6, 2, 2);
            set(-7, 1, 2, stairs(p.plank2Stairs, 0));
            set(-5, 1, 2, stairs(p.plank2Stairs, 1));
            set(-7, 2, 2, stairs(p.plank2Stairs, 4));
            set(-5, 2, 2, stairs(p.plank2Stairs, 5));

            // Larder
            for (int k1 = -2; k1 <= 1; k1++) {
                for (int i1 = -9; i1 <= -4; i1++) {
                    set(i1, 0, k1, p.plank);
                    for (int j1 = 1; j1 <= 3; j1++) {
                        setAir(i1, j1, k1);
                    }
                    set(i1, 4, k1, smoothStone);
                }
                for (int j1 = 1; j1 <= 3; j1++) {
                    set(-10, j1, k1, p.plank2);
                }
            }
            for (int i1 = -9; i1 <= -4; i1++) {
                set(i1, 1, -2, slab(Blocks.OAK_SLAB, true));
                set(i1, 3, -2, slab(Blocks.OAK_SLAB, false));
            }
            for (int k1 = -2; k1 <= 1; k1++) {
                set(-4, 1, k1, slab(Blocks.OAK_SLAB, true));
                set(-4, 3, k1, slab(Blocks.OAK_SLAB, false));
                set(-9, 1, k1, slab(Blocks.OAK_SLAB, true));
                set(-9, 3, k1, slab(Blocks.OAK_SLAB, false));
            }
            set(-8, 1, 1, slab(Blocks.OAK_SLAB, true));
            set(-8, 3, 1, slab(Blocks.OAK_SLAB, false));
            set(-6, 3, 1, torch(4));
            for (int k1 = -2; k1 <= 1; k1++) {
                if (random.nextInt(3) != 0) {
                    // The original chose between cake, apple crumble, cherry
                    // pie, berry pie and marzipan; only the cake exists here
                    random.nextInt(5);
                    set(-4, 2, k1, Blocks.CAKE.defaultBlockState());
                }
            }
            // The original put plates of food on the shelf here, and a barrel of drink beside them
            set(-5, 2, -2, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, facing(3)));
            for (int j1 = 1; j1 <= 3; j1++) {
                set(-9, j1, -3, p.plank2);
                set(-4, j1, 3, p.plank2);
            }
            placeChest(random, -8, 2, -2, facing(3), LARDER_LOOT);
            placeChest(random, -9, 2, -1, facing(4), LARDER_LOOT);
            placeChest(random, -9, 2, 0, facing(4), LARDER_LOOT);
            placeChest(random, -8, 2, 1, facing(2), LARDER_LOOT);
            pairChests(-9, 2, -1, -9, 2, 0, facing(4));
            if (gateFlip) {
                set(-1, 2, -9, Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, dir4(0)));
            } else {
                set(1, 2, -9, Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, dir4(0)));
            }

            // Grass and flowers over the mound, and sometimes a tree
            int grassRadius = RADIUS - 3;
            int grass = Mth.nextInt(random, 80, 120);
            for (int l = 0; l < grass; l++) {
                int i1 = Mth.nextInt(random, -grassRadius, grassRadius);
                int k1 = Mth.nextInt(random, -grassRadius, grassRadius);
                int j1 = getTopBlock(i1, k1);
                plantTallGrass(i1, j1, k1);
            }
            int flowers = Mth.nextInt(random, 8, 16);
            for (int l = 0; l < flowers; l++) {
                int i1 = Mth.nextInt(random, -grassRadius, grassRadius);
                int k1 = Mth.nextInt(random, -grassRadius, grassRadius);
                int j1 = getTopBlock(i1, k1);
                plantFlower(random, i1, j1, k1);
            }
            if (random.nextInt(4) == 0) {
                int i1 = Mth.nextInt(random, -grassRadius, grassRadius);
                int k1 = Mth.nextInt(random, -grassRadius, grassRadius);
                int j1 = getTopBlock(i1, k1);
                growTree(RandomSource.create(random.nextLong()), i1, j1, k1);
            }

            spawnHobbitCouple(0, 1, 0, Mth.floor(RADIUS * 1.5));
        }

        // ── The front door ──────────────────────────────────────────────────

        /** The two-by-two hobbit gate between columns {@code i0} and {@code i1}, with the floor beneath. */
        private void placeDoor(int i0, int i1) {
            Direction facing = facing(2);
            Set<BlockPos> door = new HashSet<>();
            for (int i = i0; i <= i1; i++) {
                for (int j = 1; j <= 2; j++) {
                    door.add(pos(i, j, -10));
                }
            }
            for (int i = i0; i <= i1; i++) {
                set(i, 0, -10, palette.floor);
                for (int j = 1; j <= 2; j++) {
                    setAir(i, j, -11);
                    BlockState gate = palette.gate.defaultBlockState().setValue(HobbitGateBlock.FACING, facing);
                    set(i, j, -10, HobbitGateBlock.withConnections(pos(i, j, -10), gate, door::contains));
                }
            }
        }

        // ── Placement, as the original's StructureBase2 ─────────────────────

        private BlockPos pos(int i, int j, int k) {
            return new BlockPos(worldX(originX, rotation, i, k), y(j), worldZ(originZ, rotation, i, k));
        }

        private int y(int j) {
            return originY + j;
        }

        /** Whether this chunk holds the column; skipping the others changes nothing. */
        private boolean columnInBox(int i, int k) {
            return box.isInside(worldX(originX, rotation, i, k), box.minY(), worldZ(originZ, rotation, i, k));
        }

        private void set(int i, int j, int k, BlockState state) {
            BlockPos pos = pos(i, j, k);
            if (!box.isInside(pos)) return;
            level.setBlock(pos, state, 2);
            // Fences, walls, panes and stairs join up with their neighbours
            // when the chunk is finished, as in vanilla's structures
            Block block = state.getBlock();
            if (block instanceof FenceBlock || block instanceof WallBlock || block instanceof IronBarsBlock
                    || block instanceof StairBlock || block instanceof FenceGateBlock) {
                level.getChunk(pos).markPosForPostprocessing(pos);
            }
        }

        private void setAir(int i, int j, int k) {
            set(i, j, k, Blocks.AIR.defaultBlockState());
        }

        private BlockState getBlock(int i, int j, int k) {
            BlockPos pos = pos(i, j, k);
            return box.isInside(pos) ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState();
        }

        private boolean isOpaque(int i, int j, int k) {
            BlockPos pos = pos(i, j, k);
            return box.isInside(pos) && level.getBlockState(pos).isSolidRender(level, pos);
        }

        /** The height above the hole's floor of the first open block, or 0 outside this chunk. */
        private int getTopBlock(int i, int k) {
            if (!columnInBox(i, k)) return 0;
            BlockPos pos = pos(i, 0, k);
            return level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) - originY;
        }

        /** Turns grass smothered by the block now on top of it to dirt. */
        private void setGrassToDirt(int i, int j, int k) {
            BlockState state = getBlock(i, j, k);
            if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM) || state.is(Blocks.PODZOL)) {
                set(i, j, k, Blocks.DIRT.defaultBlockState());
            }
        }

        private void plantFlower(RandomSource random, int i, int j, int k) {
            plant(i, j, k, Palette.FLOWERS[random.nextInt(Palette.FLOWERS.length)].defaultBlockState());
        }

        private void plantTallGrass(int i, int j, int k) {
            plant(i, j, k, Blocks.SHORT_GRASS.defaultBlockState());
        }

        /** Unlike the original, only plants on soil, where the plant can stay. */
        private void plant(int i, int j, int k, BlockState plant) {
            if (getBlock(i, j, k).isAir() && getBlock(i, j - 1, k).is(BlockTags.DIRT)) {
                set(i, j, k, plant);
            }
        }

        private void placeChest(RandomSource random, int i, int j, int k, Direction facing, ResourceKey<LootTable> loot) {
            long lootSeed = random.nextLong();
            BlockPos pos = pos(i, j, k);
            if (!box.isInside(pos)) return;
            set(i, j, k, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
            if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity chest) {
                chest.setLootTable(loot, lootSeed);
            }
        }

        /** Two chests side by side make a double chest, as they did in the original. */
        private void pairChests(int i0, int j0, int k0, int i1, int j1, int k1, Direction facing) {
            BlockPos a = pos(i0, j0, k0);
            BlockPos b = pos(i1, j1, k1);
            ChestType typeA;
            if (b.equals(a.relative(facing.getClockWise()))) {
                typeA = ChestType.LEFT;
            } else if (b.equals(a.relative(facing.getCounterClockWise()))) {
                typeA = ChestType.RIGHT;
            } else {
                return;
            }
            setChestType(a, typeA);
            setChestType(b, typeA.getOpposite());
        }

        private void setChestType(BlockPos pos, ChestType type) {
            if (!box.isInside(pos)) return;
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.CHEST)) {
                level.setBlock(pos, state.setValue(ChestBlock.TYPE, type), 2);
            }
        }

        private void placeSign(int i, int j, int k, BlockState sign, String[] lines) {
            BlockPos pos = pos(i, j, k);
            if (!box.isInside(pos)) return;
            set(i, j, k, sign);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof SignBlockEntity)) return;
            SignText text = new SignText();
            for (int line = 0; line < lines.length; line++) {
                text = text.setMessage(line, Component.literal(lines[line]));
            }
            // The sign has no level yet, so its setters can't be used; load
            // the text the way structure templates load block entities
            CompoundTag tag = new CompoundTag();
            Tag encoded = SignText.DIRECT_CODEC.encodeStart(
                    level.registryAccess().createSerializationContext(NbtOps.INSTANCE), text).getOrThrow();
            tag.put("front_text", encoded);
            blockEntity.loadWithComponents(tag, level.registryAccess());
        }

        private void spawnItemFrame(int i, int j, int k, Direction direction, ItemStack item) {
            BlockPos framePos = pos(i, j, k).relative(direction);
            if (!box.isInside(framePos)) return;
            ItemFrame frame = new ItemFrame(level.getLevel(), framePos, direction);
            frame.setItem(item, false);
            level.addFreshEntity(frame);
        }

        private void growTree(RandomSource random, int i, int j, int k) {
            BlockPos pos = pos(i, j, k);
            if (!box.isInside(pos)) return;
            level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolder(SHIRE_TREES)
                    .ifPresent(tree -> tree.value().place(level, generator, random, pos));
        }

        private void spawnHobbitCouple(int i, int j, int k, int homeRange) {
            BlockPos pos = pos(i, j, k);
            if (!box.isInside(pos)) return;
            HobbitNames.Household names = palette.names;
            HobbitEntity husband = spawnHobbit(pos, names.husband(), false, homeRange);
            HobbitEntity wife = spawnHobbit(pos, names.wife(), true, homeRange);
            if (husband == null || wife == null) return;
            level.addFreshEntityWithPassengers(husband);
            level.addFreshEntityWithPassengers(wife);
        }

        private HobbitEntity spawnHobbit(BlockPos pos, String name, boolean female, int homeRange) {
            HobbitEntity hobbit = ModEntities.HOBBIT.get().create(level.getLevel());
            if (hobbit == null) return null;
            hobbit.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
            hobbit.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
            hobbit.setFemale(female);
            hobbit.setCustomName(Component.literal(name));
            hobbit.setPersistenceRequired();
            hobbit.restrictTo(pos, homeRange);
            return hobbit;
        }

        // ── The original's metadata, turned for the rotation ────────────────

        /** A direction given for rotation 0, turned to this hole's rotation. */
        private Direction turn(Direction direction) {
            for (int r = 0; r < rotation; r++) {
                direction = direction.getClockWise();
            }
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
        private Direction facing(int facing) {
            return turn(switch (facing) {
                case 3 -> Direction.SOUTH;
                case 4 -> Direction.WEST;
                case 5 -> Direction.EAST;
                default -> Direction.NORTH;
            });
        }

        /** Stairs: the low two bits pick the high side (east, west, south, north), bit 4 hangs them upside down. */
        private BlockState stairs(Block block, int meta) {
            Direction facing = turn(switch (meta & 3) {
                case 0 -> Direction.EAST;
                case 1 -> Direction.WEST;
                case 2 -> Direction.SOUTH;
                default -> Direction.NORTH;
            });
            return block.defaultBlockState()
                    .setValue(StairBlock.FACING, facing)
                    .setValue(StairBlock.HALF, (meta & 4) != 0 ? Half.TOP : Half.BOTTOM);
        }

        /** Torches: 5 stands on the floor; 1 to 4 lean east, west, south and north off a wall. */
        private BlockState torch(int meta) {
            if (meta == 5) return Blocks.TORCH.defaultBlockState();
            Direction facing = turn(switch (meta) {
                case 1 -> Direction.EAST;
                case 2 -> Direction.WEST;
                case 3 -> Direction.SOUTH;
                default -> Direction.NORTH;
            });
            return Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, facing);
        }

        private BlockState wallSign(int facing) {
            return Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing(facing));
        }

        private BlockState bed(int dir, boolean head) {
            return Blocks.RED_BED.defaultBlockState()
                    .setValue(BedBlock.FACING, dir4(dir))
                    .setValue(BedBlock.PART, head ? BedPart.HEAD : BedPart.FOOT);
        }

        private static BlockState slab(Block block, boolean top) {
            return block.defaultBlockState().setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
        }
    }

    /**
     * The materials a hole is built from, rolled as the original's
     * {@code setupRandomBlocks} rolled them, in the same order, including the
     * rolls for materials only its other hobbit buildings use.
     *
     * <p>Where the original used one of its own woods, the vanilla planks
     * closest in colour stand in: oak for shire pine and willow, acacia for
     * chestnut and pear, birch for beech and apple, mangrove for cherry. Its
     * flowers come from vanilla's plains flowers.
     */
    static final class Palette {
        static final Block[] FLOWERS = {
                Blocks.DANDELION, Blocks.POPPY, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER,
                Blocks.RED_TULIP, Blocks.ORANGE_TULIP, Blocks.WHITE_TULIP, Blocks.PINK_TULIP};

        final boolean isWealthy;
        /** Floors: shire pine, oak, birch, chestnut or beech planks. */
        final BlockState plank;
        /** Walls and ceilings: apple, pear or cherry if wealthy, else oak, spruce or willow. */
        final BlockState plank2;
        final Block plank2Slab;
        final Block plank2Stairs;
        final BlockState floor;
        final BlockState carpet;
        final BlockState chandelier;
        final BlockState outFence;
        final BlockState path;
        final BlockState hedge;
        final HobbitGateBlock gate;
        final HobbitNames.Household names;

        Palette(RandomSource random) {
            isWealthy = random.nextInt(5) == 0;
            plank = (switch (random.nextInt(5)) {
                case 0, 1 -> Blocks.OAK_PLANKS;
                case 2, 4 -> Blocks.BIRCH_PLANKS;
                default -> Blocks.ACACIA_PLANKS;
            }).defaultBlockState();
            // Door, for the original's other hobbit buildings
            random.nextBoolean();
            Block[] wood2 = isWealthy
                    ? switch (random.nextInt(3)) {
                        case 0 -> new Block[]{Blocks.BIRCH_PLANKS, Blocks.BIRCH_SLAB, Blocks.BIRCH_STAIRS};
                        case 1 -> new Block[]{Blocks.ACACIA_PLANKS, Blocks.ACACIA_SLAB, Blocks.ACACIA_STAIRS};
                        default -> new Block[]{Blocks.MANGROVE_PLANKS, Blocks.MANGROVE_SLAB, Blocks.MANGROVE_STAIRS};
                    }
                    : switch (random.nextInt(3)) {
                        case 1 -> new Block[]{Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_STAIRS};
                        default -> new Block[]{Blocks.OAK_PLANKS, Blocks.OAK_SLAB, Blocks.OAK_STAIRS};
                    };
            plank2 = wood2[0].defaultBlockState();
            plank2Slab = wood2[1];
            plank2Stairs = wood2[2];
            floor = (switch (random.nextInt(3)) {
                case 0 -> Blocks.BRICKS;
                case 1 -> Blocks.COBBLESTONE;
                default -> Blocks.STONE_BRICKS;
            }).defaultBlockState();
            // Walls of the original's hobbit houses: daub or planks
            random.nextBoolean();
            carpet = (switch (random.nextInt(5)) {
                case 0 -> Blocks.BLACK_CARPET;
                case 1 -> Blocks.RED_CARPET;
                case 2 -> Blocks.GREEN_CARPET;
                case 3 -> Blocks.BROWN_CARPET;
                default -> Blocks.GRAY_CARPET;
            }).defaultBlockState();
            boolean secondChandelier = random.nextInt(2) == 1;
            chandelier = (isWealthy
                    ? (secondChandelier ? ModBlocks.CHANDELIER_GOLD : ModBlocks.CHANDELIER_SILVER)
                    : (secondChandelier ? ModBlocks.CHANDELIER_IRON : ModBlocks.CHANDELIER_BRONZE))
                    .get().defaultBlockState();
            // Window bars, for the original's other hobbit buildings
            random.nextBoolean();
            outFence = (random.nextInt(3) == 0 ? Blocks.OAK_FENCE : Blocks.COBBLESTONE_WALL).defaultBlockState();
            path = (random.nextInt(3) == 0 ? Blocks.DIRT_PATH : Blocks.GRAVEL).defaultBlockState();
            hedge = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
            // Roof tiles, for the original's other hobbit buildings
            random.nextBoolean();
            gate = (switch (random.nextInt(4)) {
                case 0 -> ModBlocks.GATE_HOBBIT_GREEN;
                case 1 -> ModBlocks.GATE_HOBBIT_BLUE;
                case 2 -> ModBlocks.GATE_HOBBIT_RED;
                default -> ModBlocks.GATE_HOBBIT_YELLOW;
            }).get();
            // Plates: wooden or ceramic
            random.nextBoolean();
            names = HobbitNames.household(random);
        }
    }
}
