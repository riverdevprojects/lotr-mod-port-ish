package com.lotrmod.block;

import com.lotrmod.LOTRMod;
import com.lotrmod.entity.ModEntities;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The mod's blocks and their items. */
public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(LOTRMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LOTRMod.MODID);

    public static final DeferredBlock<CattailBlock> CATTAIL = BLOCKS.register("cattail",
            () -> new CattailBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)));
    public static final DeferredItem<DoubleHighBlockItem> CATTAIL_ITEM = ITEMS.register("cattail",
            () -> new DoubleHighBlockItem(CATTAIL.get(), new Item.Properties()));

    // The Shire's trees, from the original mod: aspen, pale-barked with
    // yellow-green leaves, and the shire pine. Their leaves are coloured in the
    // texture rather than tinted by the biome.
    public static final DeferredBlock<RotatedPillarBlock> ASPEN_LOG = BLOCKS.register("aspen_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_LOG)));
    public static final DeferredBlock<LeavesBlock> ASPEN_LEAVES = BLOCKS.register("aspen_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_LEAVES)));
    public static final DeferredBlock<RotatedPillarBlock> SHIRE_PINE_LOG = BLOCKS.register("shire_pine_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LOG)));
    public static final DeferredBlock<LeavesBlock> SHIRE_PINE_LEAVES = BLOCKS.register("shire_pine_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LEAVES)));

    // Eriador's trees, from the original mod: beech, grey-barked with golden
    // leaves, and chestnut, dark-barked and deep green. Untinted, like aspen.
    public static final DeferredBlock<RotatedPillarBlock> BEECH_LOG = BLOCKS.register("beech_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));
    public static final DeferredBlock<LeavesBlock> BEECH_LEAVES = BLOCKS.register("beech_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)));
    public static final DeferredBlock<RotatedPillarBlock> CHESTNUT_LOG = BLOCKS.register("chestnut_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LOG)));
    public static final DeferredBlock<LeavesBlock> CHESTNUT_LEAVES = BLOCKS.register("chestnut_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LEAVES)));

    // Lindon's red poplar, from vanilla's dappled forest (Java 26.3), which
    // 1.21.1 doesn't have. Untinted too.
    public static final DeferredBlock<RotatedPillarBlock> POPLAR_LOG = BLOCKS.register("poplar_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_LOG)));
    public static final DeferredBlock<LeavesBlock> RED_POPLAR_LEAVES = BLOCKS.register("red_poplar_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_LEAVES)));

    // Fangorn's, from the original mod: beech wood, bark on every side, for the
    // boughs of the giant beeches; the rotten logs lying on the forest floor;
    // the Ents' earthen jars; and the six colours of Fangorn plant.
    public static final DeferredBlock<RotatedPillarBlock> BEECH_WOOD = BLOCKS.register("beech_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD)));
    public static final DeferredBlock<RotatedPillarBlock> ROTTEN_LOG = BLOCKS.register("rotten_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LOG).strength(1.0f)));
    public static final DeferredBlock<EntJarBlock> ENT_JAR = BLOCKS.register("ent_jar",
            () -> new EntJarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN)
                    .strength(1.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<FlowerBlock> FANGORN_PLANT_GREEN = registerFangornPlant("fangorn_plant_green");
    public static final DeferredBlock<FlowerBlock> FANGORN_PLANT_BROWN = registerFangornPlant("fangorn_plant_brown");
    public static final DeferredBlock<FlowerBlock> FANGORN_PLANT_GOLD = registerFangornPlant("fangorn_plant_gold");
    public static final DeferredBlock<FlowerBlock> FANGORN_PLANT_YELLOW = registerFangornPlant("fangorn_plant_yellow");
    public static final DeferredBlock<FlowerBlock> FANGORN_PLANT_RED = registerFangornPlant("fangorn_plant_red");
    public static final DeferredBlock<FlowerBlock> FANGORN_PLANT_SILVER = registerFangornPlant("fangorn_plant_silver");

    // Mirkwood's, from the original mod: the mirk oak, near-black bark under
    // leaves of a deep, dull green, with wood (bark on every side) for its
    // boughs and roots; the dark vines that hang from its crown; and the tall
    // pine that grows among the oaks. Untinted, like aspen.
    public static final DeferredBlock<RotatedPillarBlock> MIRK_OAK_LOG = BLOCKS.register("mirk_oak_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LOG)));
    public static final DeferredBlock<RotatedPillarBlock> MIRK_OAK_WOOD = BLOCKS.register("mirk_oak_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_WOOD)));
    public static final DeferredBlock<LeavesBlock> MIRK_OAK_LEAVES = BLOCKS.register("mirk_oak_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LEAVES)));
    public static final DeferredBlock<VineBlock> MIRK_VINES = BLOCKS.register("mirk_vines",
            () -> new VineBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.VINE)));
    public static final DeferredBlock<RotatedPillarBlock> PINE_LOG = BLOCKS.register("pine_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LOG)));
    public static final DeferredBlock<LeavesBlock> PINE_LEAVES = BLOCKS.register("pine_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LEAVES)));

    // Rohan's grasses, sun-dried to straw: a short tuft, a two-tall grass gone
    // to seed, and wild oats. Coloured in the texture, not by the biome, so
    // they stay gold among the greener grass of the swales.
    public static final DeferredBlock<DryGrassBlock> DRY_GRASS = BLOCKS.register("dry_grass",
            () -> new DryGrassBlock(dryGrassProperties()));
    public static final DeferredBlock<DoublePlantBlock> TALL_DRY_GRASS = BLOCKS.register("tall_dry_grass",
            () -> new DoublePlantBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.TALL_GRASS)
                    .mapColor(MapColor.TERRACOTTA_YELLOW)));
    public static final DeferredBlock<DryGrassBlock> WILD_OATS = BLOCKS.register("wild_oats",
            () -> new DryGrassBlock(dryGrassProperties()));

    // The original's short grass, tinted by the biome like vanilla's: the
    // Dead Lands' tufts of withered grass
    public static final DeferredBlock<TallGrassBlock> SHORT_GRASS = BLOCKS.register("short_grass",
            () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS)));

    public static final DeferredItem<BlockItem> ASPEN_LOG_ITEM = ITEMS.registerSimpleBlockItem(ASPEN_LOG);
    public static final DeferredItem<BlockItem> ASPEN_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(ASPEN_LEAVES);
    public static final DeferredItem<BlockItem> SHIRE_PINE_LOG_ITEM = ITEMS.registerSimpleBlockItem(SHIRE_PINE_LOG);
    public static final DeferredItem<BlockItem> SHIRE_PINE_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(SHIRE_PINE_LEAVES);
    public static final DeferredItem<BlockItem> BEECH_LOG_ITEM = ITEMS.registerSimpleBlockItem(BEECH_LOG);
    public static final DeferredItem<BlockItem> BEECH_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(BEECH_LEAVES);
    public static final DeferredItem<BlockItem> CHESTNUT_LOG_ITEM = ITEMS.registerSimpleBlockItem(CHESTNUT_LOG);
    public static final DeferredItem<BlockItem> CHESTNUT_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(CHESTNUT_LEAVES);
    public static final DeferredItem<BlockItem> POPLAR_LOG_ITEM = ITEMS.registerSimpleBlockItem(POPLAR_LOG);
    public static final DeferredItem<BlockItem> RED_POPLAR_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(RED_POPLAR_LEAVES);
    public static final DeferredItem<BlockItem> BEECH_WOOD_ITEM = ITEMS.registerSimpleBlockItem(BEECH_WOOD);
    public static final DeferredItem<BlockItem> ROTTEN_LOG_ITEM = ITEMS.registerSimpleBlockItem(ROTTEN_LOG);
    public static final DeferredItem<BlockItem> ENT_JAR_ITEM = ITEMS.registerSimpleBlockItem(ENT_JAR);
    public static final DeferredItem<BlockItem> FANGORN_PLANT_GREEN_ITEM = ITEMS.registerSimpleBlockItem(FANGORN_PLANT_GREEN);
    public static final DeferredItem<BlockItem> FANGORN_PLANT_BROWN_ITEM = ITEMS.registerSimpleBlockItem(FANGORN_PLANT_BROWN);
    public static final DeferredItem<BlockItem> FANGORN_PLANT_GOLD_ITEM = ITEMS.registerSimpleBlockItem(FANGORN_PLANT_GOLD);
    public static final DeferredItem<BlockItem> FANGORN_PLANT_YELLOW_ITEM = ITEMS.registerSimpleBlockItem(FANGORN_PLANT_YELLOW);
    public static final DeferredItem<BlockItem> FANGORN_PLANT_RED_ITEM = ITEMS.registerSimpleBlockItem(FANGORN_PLANT_RED);
    public static final DeferredItem<BlockItem> FANGORN_PLANT_SILVER_ITEM = ITEMS.registerSimpleBlockItem(FANGORN_PLANT_SILVER);
    public static final DeferredItem<BlockItem> MIRK_OAK_LOG_ITEM = ITEMS.registerSimpleBlockItem(MIRK_OAK_LOG);
    public static final DeferredItem<BlockItem> MIRK_OAK_WOOD_ITEM = ITEMS.registerSimpleBlockItem(MIRK_OAK_WOOD);
    public static final DeferredItem<BlockItem> MIRK_OAK_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(MIRK_OAK_LEAVES);
    public static final DeferredItem<BlockItem> MIRK_VINES_ITEM = ITEMS.registerSimpleBlockItem(MIRK_VINES);
    public static final DeferredItem<BlockItem> PINE_LOG_ITEM = ITEMS.registerSimpleBlockItem(PINE_LOG);
    public static final DeferredItem<BlockItem> PINE_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(PINE_LEAVES);

    // What the hobbit holes are built from that vanilla has no match for: the
    // round front door (one of four colours of the original's hobbit gate), the
    // oak beams framing the porch, the green clay tiles on the porch roof, the
    // chandeliers, and the oven and crafting table in the kitchen.
    public static final DeferredBlock<HobbitGateBlock> GATE_HOBBIT_GREEN = registerGate("gate_hobbit_green", MapColor.COLOR_GREEN);
    public static final DeferredBlock<HobbitGateBlock> GATE_HOBBIT_BLUE = registerGate("gate_hobbit_blue", MapColor.COLOR_BLUE);
    public static final DeferredBlock<HobbitGateBlock> GATE_HOBBIT_RED = registerGate("gate_hobbit_red", MapColor.COLOR_RED);
    public static final DeferredBlock<HobbitGateBlock> GATE_HOBBIT_YELLOW = registerGate("gate_hobbit_yellow", MapColor.COLOR_YELLOW);

    public static final DeferredBlock<RotatedPillarBlock> WOOD_BEAM_OAK = BLOCKS.register("wood_beam_oak",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));

    public static final DeferredBlock<SlabBlock> CLAY_TILE_GREEN_SLAB = BLOCKS.register("clay_tile_green_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(1.25f, 7.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<ChandelierBlock> CHANDELIER_BRONZE = registerChandelier("chandelier_bronze");
    public static final DeferredBlock<ChandelierBlock> CHANDELIER_IRON = registerChandelier("chandelier_iron");
    public static final DeferredBlock<ChandelierBlock> CHANDELIER_SILVER = registerChandelier("chandelier_silver");
    public static final DeferredBlock<ChandelierBlock> CHANDELIER_GOLD = registerChandelier("chandelier_gold");

    public static final DeferredBlock<HobbitOvenBlock> HOBBIT_OVEN = BLOCKS.register("hobbit_oven",
            () -> new HobbitOvenBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.5f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(AbstractFurnaceBlock.LIT) ? 13 : 0)));
    public static final DeferredBlock<HobbitTableBlock> HOBBIT_TABLE = BLOCKS.register("hobbit_table",
            () -> new HobbitTableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE)));

    public static final DeferredItem<BlockItem> DRY_GRASS_ITEM = ITEMS.registerSimpleBlockItem(DRY_GRASS);
    public static final DeferredItem<DoubleHighBlockItem> TALL_DRY_GRASS_ITEM = ITEMS.register("tall_dry_grass",
            () -> new DoubleHighBlockItem(TALL_DRY_GRASS.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WILD_OATS_ITEM = ITEMS.registerSimpleBlockItem(WILD_OATS);
    public static final DeferredItem<BlockItem> SHORT_GRASS_ITEM = ITEMS.registerSimpleBlockItem(SHORT_GRASS);

    public static final DeferredItem<BlockItem> GATE_HOBBIT_GREEN_ITEM = ITEMS.registerSimpleBlockItem(GATE_HOBBIT_GREEN);
    public static final DeferredItem<BlockItem> GATE_HOBBIT_BLUE_ITEM = ITEMS.registerSimpleBlockItem(GATE_HOBBIT_BLUE);
    public static final DeferredItem<BlockItem> GATE_HOBBIT_RED_ITEM = ITEMS.registerSimpleBlockItem(GATE_HOBBIT_RED);
    public static final DeferredItem<BlockItem> GATE_HOBBIT_YELLOW_ITEM = ITEMS.registerSimpleBlockItem(GATE_HOBBIT_YELLOW);
    public static final DeferredItem<BlockItem> WOOD_BEAM_OAK_ITEM = ITEMS.registerSimpleBlockItem(WOOD_BEAM_OAK);
    public static final DeferredItem<BlockItem> CLAY_TILE_GREEN_SLAB_ITEM = ITEMS.registerSimpleBlockItem(CLAY_TILE_GREEN_SLAB);
    public static final DeferredItem<BlockItem> CHANDELIER_BRONZE_ITEM = ITEMS.registerSimpleBlockItem(CHANDELIER_BRONZE);
    public static final DeferredItem<BlockItem> CHANDELIER_IRON_ITEM = ITEMS.registerSimpleBlockItem(CHANDELIER_IRON);
    public static final DeferredItem<BlockItem> CHANDELIER_SILVER_ITEM = ITEMS.registerSimpleBlockItem(CHANDELIER_SILVER);
    public static final DeferredItem<BlockItem> CHANDELIER_GOLD_ITEM = ITEMS.registerSimpleBlockItem(CHANDELIER_GOLD);
    public static final DeferredItem<BlockItem> HOBBIT_OVEN_ITEM = ITEMS.registerSimpleBlockItem(HOBBIT_OVEN);
    public static final DeferredItem<BlockItem> HOBBIT_TABLE_ITEM = ITEMS.registerSimpleBlockItem(HOBBIT_TABLE);

    public static final DeferredItem<DeferredSpawnEggItem> HOBBIT_SPAWN_EGG = ITEMS.register("hobbit_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.HOBBIT, 0xA87C4F, 0x5E8C31, new Item.Properties()));

    private static DeferredBlock<HobbitGateBlock> registerGate(String name, MapColor color) {
        return BLOCKS.register(name, () -> new HobbitGateBlock(BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(4.0f, 5.0f)
                .sound(SoundType.WOOD)
                .ignitedByLava()
                .noOcclusion()));
    }

    private static BlockBehaviour.Properties dryGrassProperties() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS).mapColor(MapColor.TERRACOTTA_YELLOW);
    }

    private static DeferredBlock<FlowerBlock> registerFangornPlant(String name) {
        return BLOCKS.register(name, () -> new FlowerBlock(MobEffects.REGENERATION, 8.0f,
                BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION)));
    }

    private static DeferredBlock<ChandelierBlock> registerChandelier(String name) {
        return BLOCKS.register(name, () -> new ChandelierBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .noCollission()
                .instabreak()
                .explosionResistance(2.0f)
                .sound(SoundType.METAL)
                .lightLevel(state -> 14)
                .noOcclusion()
                .pushReaction(PushReaction.DESTROY)));
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(ModBlocks::addToCreativeTabs);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(CATTAIL_ITEM);
            event.accept(ASPEN_LOG_ITEM);
            event.accept(ASPEN_LEAVES_ITEM);
            event.accept(SHIRE_PINE_LOG_ITEM);
            event.accept(SHIRE_PINE_LEAVES_ITEM);
            event.accept(BEECH_LOG_ITEM);
            event.accept(BEECH_LEAVES_ITEM);
            event.accept(CHESTNUT_LOG_ITEM);
            event.accept(CHESTNUT_LEAVES_ITEM);
            event.accept(POPLAR_LOG_ITEM);
            event.accept(RED_POPLAR_LEAVES_ITEM);
            event.accept(BEECH_WOOD_ITEM);
            event.accept(ROTTEN_LOG_ITEM);
            event.accept(FANGORN_PLANT_GREEN_ITEM);
            event.accept(FANGORN_PLANT_BROWN_ITEM);
            event.accept(FANGORN_PLANT_GOLD_ITEM);
            event.accept(FANGORN_PLANT_YELLOW_ITEM);
            event.accept(FANGORN_PLANT_RED_ITEM);
            event.accept(FANGORN_PLANT_SILVER_ITEM);
            event.accept(MIRK_OAK_LOG_ITEM);
            event.accept(MIRK_OAK_WOOD_ITEM);
            event.accept(MIRK_OAK_LEAVES_ITEM);
            event.accept(MIRK_VINES_ITEM);
            event.accept(PINE_LOG_ITEM);
            event.accept(PINE_LEAVES_ITEM);
            event.accept(DRY_GRASS_ITEM);
            event.accept(TALL_DRY_GRASS_ITEM);
            event.accept(WILD_OATS_ITEM);
            event.accept(SHORT_GRASS_ITEM);
        } else if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(WOOD_BEAM_OAK_ITEM);
            event.accept(CLAY_TILE_GREEN_SLAB_ITEM);
            event.accept(GATE_HOBBIT_GREEN_ITEM);
            event.accept(GATE_HOBBIT_BLUE_ITEM);
            event.accept(GATE_HOBBIT_RED_ITEM);
            event.accept(GATE_HOBBIT_YELLOW_ITEM);
        } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(CHANDELIER_BRONZE_ITEM);
            event.accept(CHANDELIER_IRON_ITEM);
            event.accept(CHANDELIER_SILVER_ITEM);
            event.accept(CHANDELIER_GOLD_ITEM);
            event.accept(HOBBIT_OVEN_ITEM);
            event.accept(HOBBIT_TABLE_ITEM);
            event.accept(ENT_JAR_ITEM);
        } else if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(HOBBIT_SPAWN_EGG);
        }
    }
}
