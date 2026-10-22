package com.lotrmod.client;

import com.lotrmod.LOTRMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Port of {@code LOTRModelHobbit} (1.7.10, extends {@code LOTRModelBiped} / vanilla
 * {@code ModelBiped}). Every number below (pivots, box sizes, UV offsets, foot rotation) is
 * transcribed directly from the decompiled constructor, not invented:
 *
 * <pre>
 * this.bipedChest = new ModelRenderer(this, 24, 0);
 * this.bipedChest.addBox(-3, 2, -4, 6, 3, 2, f);
 * this.bipedChest.setRotationPoint(0, 0, 0);
 * this.field_78115_e.addChild(this.bipedChest);           // chest bump, child of body
 *
 * this.field_78114_d = new ModelRenderer(this, 0, 32);     // reuses the "hat" slot
 * this.field_78114_d.addBox(-4, -8, -4, 8, 12, 8, 0.5f + f);
 * this.field_78114_d.setRotationPoint(0, 2, 0);
 *
 * ModelRenderer rightFoot = new ModelRenderer(this, 40, 32);
 * rightFoot.addBox(-2, 10, -5, 4, 2, 3, f);
 * rightFoot.rotateAngleY = toRadians(10);                  // child of right leg
 * // ...mirrored, rotateAngleY = toRadians(-10), for the left foot
 *
 * // then, uniformly, at the end of the constructor:
 * field_78116_c.rotationPointY += 4.0f;   // head
 * field_78114_d.rotationPointY += 4.0f;   // hat/hood
 * field_78115_e.rotationPointY += 4.8f;   // body
 * field_78112_f.rotationPointY += 4.8f;   // right arm
 * field_78113_g.rotationPointY += 4.8f;   // left arm
 * field_78123_h.rotationPointY += 4.8f;   // right leg
 * field_78124_i.rotationPointY += 4.8f;   // left leg
 * </pre>
 *
 * <p>That last block is the part an earlier pass missed: it reused
 * {@link net.minecraft.client.model.geom.ModelLayers#PLAYER}'s raw (un-shifted) vanilla pivots
 * and applied the 10/12 vertical squash on top, unmodified. Because the squash is applied as a
 * {@link PoseStack#scale} wrapped around each part's own render call — same as the original's
 * {@code GL11.glScalef} wrapped around each {@code ModelRenderer.render} call — the part's pivot
 * translation happens *inside* the scaled matrix and gets scaled too. The original compensates by
 * pre-shifting every pivot down by 4-4.8px so the post-scale position lands in the right place;
 * skipping that shift leaves legs/arms/body sitting ~4px too high, i.e. sunk into the torso — the
 * "missing legs" bug. This class bakes the same compensating shift directly into each part's
 * {@link PartPose}, plus the chest bump, oversized head/hood, and toe cubes the original also has.
 */
public class HobbitModel<T extends LivingEntity> extends HumanoidModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(LOTRMod.MODID, "hobbit"), "main");

    private static final float LIMB_Y_SCALE = 0.8333333f;

    public HobbitModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(PartNames.HEAD,
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f),
                PartPose.offset(0.0f, 4.0f, 0.0f));

        // Reuses the vanilla "hat" slot for an oversized, rounder head/hood shape, per the original.
        root.addOrReplaceChild(PartNames.HAT,
                CubeListBuilder.create().texOffs(0, 32)
                        .addBox(-4.0f, -8.0f, -4.0f, 8.0f, 12.0f, 8.0f, CubeDeformation.NONE.extend(0.5f)),
                PartPose.offset(0.0f, 6.0f, 0.0f));

        PartDefinition body = root.addOrReplaceChild(PartNames.BODY,
                CubeListBuilder.create().texOffs(16, 16)
                        .addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f),
                PartPose.offset(0.0f, 4.8f, 0.0f));
        body.addOrReplaceChild("chest",
                CubeListBuilder.create().texOffs(24, 0)
                        .addBox(-3.0f, 2.0f, -4.0f, 6.0f, 3.0f, 2.0f),
                PartPose.ZERO);

        root.addOrReplaceChild(PartNames.RIGHT_ARM,
                CubeListBuilder.create().texOffs(40, 16)
                        .addBox(-3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f),
                PartPose.offset(-5.0f, 6.8f, 0.0f));
        root.addOrReplaceChild(PartNames.LEFT_ARM,
                CubeListBuilder.create().texOffs(40, 16).mirror()
                        .addBox(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f),
                PartPose.offset(5.0f, 6.8f, 0.0f));

        PartDefinition rightLeg = root.addOrReplaceChild(PartNames.RIGHT_LEG,
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f),
                PartPose.offset(-1.9f, 16.8f, 0.0f));
        rightLeg.addOrReplaceChild("foot",
                CubeListBuilder.create().texOffs(40, 32)
                        .addBox(-2.0f, 10.0f, -5.0f, 4.0f, 2.0f, 3.0f),
                PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, (float) Math.toRadians(10.0), 0.0f));

        PartDefinition leftLeg = root.addOrReplaceChild(PartNames.LEFT_LEG,
                CubeListBuilder.create().texOffs(0, 16).mirror()
                        .addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f),
                PartPose.offset(1.9f, 16.8f, 0.0f));
        leftLeg.addOrReplaceChild("foot",
                CubeListBuilder.create().texOffs(40, 32).mirror()
                        .addBox(-2.0f, 10.0f, -5.0f, 4.0f, 2.0f, 3.0f),
                PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, (float) Math.toRadians(-10.0), 0.0f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        head.render(poseStack, buffer, packedLight, packedOverlay, color);
        hat.render(poseStack, buffer, packedLight, packedOverlay, color);
        renderScaledPart(body, poseStack, buffer, packedLight, packedOverlay, color);
        renderScaledPart(rightArm, poseStack, buffer, packedLight, packedOverlay, color);
        renderScaledPart(leftArm, poseStack, buffer, packedLight, packedOverlay, color);
        renderScaledPart(rightLeg, poseStack, buffer, packedLight, packedOverlay, color);
        renderScaledPart(leftLeg, poseStack, buffer, packedLight, packedOverlay, color);
    }

    private static void renderScaledPart(ModelPart part, PoseStack poseStack, VertexConsumer buffer,
                                          int packedLight, int packedOverlay, int color) {
        poseStack.pushPose();
        poseStack.scale(1.0f, LIMB_Y_SCALE, 1.0f);
        part.render(poseStack, buffer, packedLight, packedOverlay, color);
        poseStack.popPose();
    }
}
