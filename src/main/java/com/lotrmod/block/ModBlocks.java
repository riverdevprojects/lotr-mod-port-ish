package com.lotrmod.block;

import com.lotrmod.LOTRMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
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

    public static final DeferredItem<BlockItem> ASPEN_LOG_ITEM = ITEMS.registerSimpleBlockItem(ASPEN_LOG);
    public static final DeferredItem<BlockItem> ASPEN_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(ASPEN_LEAVES);
    public static final DeferredItem<BlockItem> SHIRE_PINE_LOG_ITEM = ITEMS.registerSimpleBlockItem(SHIRE_PINE_LOG);
    public static final DeferredItem<BlockItem> SHIRE_PINE_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(SHIRE_PINE_LEAVES);

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
        }
    }
}
