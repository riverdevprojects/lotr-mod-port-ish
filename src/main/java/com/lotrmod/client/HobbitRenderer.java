package com.lotrmod.client;

import com.lotrmod.LOTRMod;
import com.lotrmod.entity.HobbitEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders hobbits with a real short-and-stout {@link HobbitModel} — a dedicated mesh built from
 * {@code LOTRModelHobbit}'s actual box/pivot numbers (see its javadoc), not the raw vanilla player
 * mesh — and the actual ported 1.7.10 skin pool ({@code textures/entity/hobbit/male_0..12.png} /
 * {@code female_0..12.png}), matching the original's per-NPC persistent random gender+variant
 * selection ({@link HobbitEntity#isFemale()} / {@link HobbitEntity#getSkinVariant()}).
 */
public class HobbitRenderer<T extends HobbitEntity> extends MobRenderer<T, HobbitModel<T>> {

    public HobbitRenderer(EntityRendererProvider.Context context) {
        super(context, new HobbitModel<>(context.bakeLayer(HobbitModel.LAYER_LOCATION)), 0.3f);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        String gender = entity.isFemale() ? "female" : "male";
        int variant = entity.getSkinVariant();
        return ResourceLocation.fromNamespaceAndPath(LOTRMod.MODID,
                "textures/entity/hobbit/" + gender + "_" + variant + ".png");
    }

    @Override
    protected void scale(T entity, com.mojang.blaze3d.vertex.PoseStack poseStack, float partialTickTime) {
        // Player-sized mesh scaled down to roughly match the 0.45x1.2 hobbit hitbox (ModEntities).
        poseStack.scale(0.65f, 0.65f, 0.65f);
    }
}
