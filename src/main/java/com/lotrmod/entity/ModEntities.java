package com.lotrmod.entity;

import com.lotrmod.LOTRMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The mod's entities: the hobbits who live in the Shire's hobbit holes. */
public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, LOTRMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<HobbitEntity>> HOBBIT =
            ENTITY_TYPES.register("hobbit", () -> EntityType.Builder.of(HobbitEntity::new, MobCategory.CREATURE)
                    .sized(0.45f, 1.2f)
                    .build("hobbit"));

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(ModEntities::createAttributes);
    }

    private static void createAttributes(EntityAttributeCreationEvent event) {
        event.put(HOBBIT.get(), HobbitEntity.createAttributes().build());
    }
}
