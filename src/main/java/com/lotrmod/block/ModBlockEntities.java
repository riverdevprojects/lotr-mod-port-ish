package com.lotrmod.block;

import com.lotrmod.LOTRMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Block entity types for LOTR mod blocks. */
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, LOTRMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HobbitOvenBlockEntity>> HOBBIT_OVEN =
            BLOCK_ENTITIES.register("hobbit_oven", () -> BlockEntityType.Builder.of(
                    HobbitOvenBlockEntity::new, ModBlocks.HOBBIT_OVEN.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
