package com.lotrmod.block;

import net.minecraft.util.ColorRGBA;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.lotrmod.block.ModBlocks.BLOCKS;
import static com.lotrmod.block.ModBlocks.ITEMS;

/**
 * Dale's blocks, in both its eras.
 *
 * <p>For the town of Dale: the original mod's Dale bricks, a warm cream stone,
 * plain, mossy, cracked and carved, with slabs, stairs, walls and its pillar;
 * and its dyed clay roof tiles in the four colours its Dale houses were
 * roofed with, orange, red, brown and blue. For the Front Gate of Erebor: the
 * original's dwarven bricks and pillar. For the orchards: the original's
 * apple, pear and plum trees, whose leaves drop their fruit. And for what
 * Smaug left: the original's charred wood and scorched stone, and ash.
 */
public final class DaleBlocks {
    private DaleBlocks() {}

    private static final List<DeferredItem<? extends Item>> NATURAL = new ArrayList<>();
    private static final List<DeferredItem<? extends Item>> BUILDING = new ArrayList<>();
    private static final List<DeferredItem<? extends Item>> FOOD = new ArrayList<>();

    // ── Dale's stone ────────────────────────────────────────────────────────
    public static final DeferredBlock<Block> DALE_BRICKS = building("dale_bricks",
            () -> new Block(stone(Blocks.STONE_BRICKS, MapColor.SAND)));
    public static final DeferredBlock<Block> MOSSY_DALE_BRICKS = building("mossy_dale_bricks",
            () -> new Block(stone(Blocks.MOSSY_STONE_BRICKS, MapColor.SAND)));
    public static final DeferredBlock<Block> CRACKED_DALE_BRICKS = building("cracked_dale_bricks",
            () -> new Block(stone(Blocks.CRACKED_STONE_BRICKS, MapColor.SAND)));
    public static final DeferredBlock<Block> CHISELED_DALE_BRICKS = building("chiseled_dale_bricks",
            () -> new Block(stone(Blocks.CHISELED_STONE_BRICKS, MapColor.SAND)));
    public static final DeferredBlock<SlabBlock> DALE_BRICK_SLAB = building("dale_brick_slab",
            () -> new SlabBlock(stone(Blocks.STONE_BRICK_SLAB, MapColor.SAND)));
    public static final DeferredBlock<StairBlock> DALE_BRICK_STAIRS = building("dale_brick_stairs",
            () -> new StairBlock(DALE_BRICKS.get().defaultBlockState(), stone(Blocks.STONE_BRICK_STAIRS, MapColor.SAND)));
    public static final DeferredBlock<WallBlock> DALE_BRICK_WALL = building("dale_brick_wall",
            () -> new WallBlock(stone(Blocks.STONE_BRICK_WALL, MapColor.SAND).forceSolidOn()));
    public static final DeferredBlock<RotatedPillarBlock> DALE_PILLAR = building("dale_pillar",
            () -> new RotatedPillarBlock(stone(Blocks.QUARTZ_PILLAR, MapColor.SAND)));

    // ── the Dwarves' stone ──────────────────────────────────────────────────
    public static final DeferredBlock<Block> DWARVEN_BRICKS = building("dwarven_bricks",
            () -> new Block(stone(Blocks.STONE_BRICKS, MapColor.STONE).strength(2.5f, 8.0f)));
    public static final DeferredBlock<Block> CRACKED_DWARVEN_BRICKS = building("cracked_dwarven_bricks",
            () -> new Block(stone(Blocks.CRACKED_STONE_BRICKS, MapColor.STONE).strength(2.5f, 8.0f)));
    public static final DeferredBlock<Block> CHISELED_DWARVEN_BRICKS = building("chiseled_dwarven_bricks",
            () -> new Block(stone(Blocks.CHISELED_STONE_BRICKS, MapColor.STONE).strength(2.5f, 8.0f)));
    public static final DeferredBlock<SlabBlock> DWARVEN_BRICK_SLAB = building("dwarven_brick_slab",
            () -> new SlabBlock(stone(Blocks.STONE_BRICK_SLAB, MapColor.STONE).strength(2.5f, 8.0f)));
    public static final DeferredBlock<StairBlock> DWARVEN_BRICK_STAIRS = building("dwarven_brick_stairs",
            () -> new StairBlock(DWARVEN_BRICKS.get().defaultBlockState(), stone(Blocks.STONE_BRICK_STAIRS, MapColor.STONE).strength(2.5f, 8.0f)));
    public static final DeferredBlock<RotatedPillarBlock> DWARVEN_PILLAR = building("dwarven_pillar",
            () -> new RotatedPillarBlock(stone(Blocks.QUARTZ_PILLAR, MapColor.STONE).strength(2.5f, 8.0f)));

    // ── roof tiles ──────────────────────────────────────────────────────────
    public static final DeferredBlock<Block> CLAY_TILE_ORANGE = building("clay_tile_orange", () -> new Block(tiles(MapColor.COLOR_ORANGE)));
    public static final DeferredBlock<SlabBlock> CLAY_TILE_ORANGE_SLAB = building("clay_tile_orange_slab", () -> new SlabBlock(tiles(MapColor.COLOR_ORANGE)));
    public static final DeferredBlock<StairBlock> CLAY_TILE_ORANGE_STAIRS = building("clay_tile_orange_stairs",
            () -> new StairBlock(CLAY_TILE_ORANGE.get().defaultBlockState(), tiles(MapColor.COLOR_ORANGE)));
    public static final DeferredBlock<Block> CLAY_TILE_RED = building("clay_tile_red", () -> new Block(tiles(MapColor.COLOR_RED)));
    public static final DeferredBlock<SlabBlock> CLAY_TILE_RED_SLAB = building("clay_tile_red_slab", () -> new SlabBlock(tiles(MapColor.COLOR_RED)));
    public static final DeferredBlock<StairBlock> CLAY_TILE_RED_STAIRS = building("clay_tile_red_stairs",
            () -> new StairBlock(CLAY_TILE_RED.get().defaultBlockState(), tiles(MapColor.COLOR_RED)));
    public static final DeferredBlock<Block> CLAY_TILE_BROWN = building("clay_tile_brown", () -> new Block(tiles(MapColor.COLOR_BROWN)));
    public static final DeferredBlock<SlabBlock> CLAY_TILE_BROWN_SLAB = building("clay_tile_brown_slab", () -> new SlabBlock(tiles(MapColor.COLOR_BROWN)));
    public static final DeferredBlock<StairBlock> CLAY_TILE_BROWN_STAIRS = building("clay_tile_brown_stairs",
            () -> new StairBlock(CLAY_TILE_BROWN.get().defaultBlockState(), tiles(MapColor.COLOR_BROWN)));
    public static final DeferredBlock<Block> CLAY_TILE_BLUE = building("clay_tile_blue", () -> new Block(tiles(MapColor.COLOR_BLUE)));
    public static final DeferredBlock<SlabBlock> CLAY_TILE_BLUE_SLAB = building("clay_tile_blue_slab", () -> new SlabBlock(tiles(MapColor.COLOR_BLUE)));
    public static final DeferredBlock<StairBlock> CLAY_TILE_BLUE_STAIRS = building("clay_tile_blue_stairs",
            () -> new StairBlock(CLAY_TILE_BLUE.get().defaultBlockState(), tiles(MapColor.COLOR_BLUE)));

    // ── what the dragon left ────────────────────────────────────────────────
    public static final DeferredBlock<RotatedPillarBlock> CHARRED_LOG = natural("charred_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).mapColor(MapColor.COLOR_BLACK)));
    public static final DeferredBlock<RotatedPillarBlock> CHARRED_WOOD = natural("charred_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD).mapColor(MapColor.COLOR_BLACK)));
    public static final DeferredBlock<Block> CHARRED_PLANKS = building("charred_planks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).mapColor(MapColor.COLOR_BLACK)));
    public static final DeferredBlock<Block> SCORCHED_STONE = natural("scorched_stone",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.COLOR_BLACK)));
    public static final DeferredBlock<ColoredFallingBlock> ASH = natural("ash",
            () -> new ColoredFallingBlock(new ColorRGBA(0x6E6C69),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).mapColor(MapColor.COLOR_GRAY).strength(0.4f)));

    // ── fruit trees ─────────────────────────────────────────────────────────
    public static final DeferredBlock<RotatedPillarBlock> APPLE_LOG = natural("apple_log", DaleBlocks::fruitLog);
    public static final DeferredBlock<LeavesBlock> APPLE_LEAVES = natural("apple_leaves", DaleBlocks::fruitLeaves);
    public static final DeferredBlock<RotatedPillarBlock> PEAR_LOG = natural("pear_log", DaleBlocks::fruitLog);
    public static final DeferredBlock<LeavesBlock> PEAR_LEAVES = natural("pear_leaves", DaleBlocks::fruitLeaves);
    public static final DeferredBlock<RotatedPillarBlock> PLUM_LOG = natural("plum_log", DaleBlocks::fruitLog);
    public static final DeferredBlock<LeavesBlock> PLUM_LEAVES = natural("plum_leaves", DaleBlocks::fruitLeaves);

    public static final DeferredItem<Item> PEAR = food("pear");
    public static final DeferredItem<Item> PLUM = food("plum");

    private static BlockBehaviour.Properties stone(Block like, MapColor color) {
        return BlockBehaviour.Properties.ofFullCopy(like).mapColor(color);
    }

    private static BlockBehaviour.Properties tiles(MapColor color) {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.TERRACOTTA).mapColor(color).strength(1.25f, 7.0f);
    }

    private static RotatedPillarBlock fruitLog() {
        return new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG));
    }

    private static LeavesBlock fruitLeaves() {
        return new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES));
    }

    private static <B extends Block> DeferredBlock<B> building(String name, Supplier<B> block) {
        DeferredBlock<B> registered = BLOCKS.register(name, block);
        BUILDING.add(ITEMS.registerSimpleBlockItem(registered));
        return registered;
    }

    private static <B extends Block> DeferredBlock<B> natural(String name, Supplier<B> block) {
        DeferredBlock<B> registered = BLOCKS.register(name, block);
        NATURAL.add(ITEMS.registerSimpleBlockItem(registered));
        return registered;
    }

    /** A fruit as filling as an apple. */
    private static DeferredItem<Item> food(String name) {
        DeferredItem<Item> item = ITEMS.register(name, () -> new Item(new Item.Properties()
                .food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.3f).build())));
        FOOD.add(item);
        return item;
    }

    /** Loads this class, so its blocks are added to the registers before they are registered. */
    static void init() {}

    static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        List<DeferredItem<? extends Item>> items = null;
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) items = NATURAL;
        else if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) items = BUILDING;
        else if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) items = FOOD;
        if (items != null) items.forEach(event::accept);
    }
}
