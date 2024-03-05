package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.BreezeModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BreezeWindLayer extends RenderLayer<Breeze, BreezeModel<Breeze>> {
    private static final float TOP_PART_ALPHA = 1.0F;
    private static final float MIDDLE_PART_ALPHA = 1.0F;
    private static final float BOTTOM_PART_ALPHA = 1.0F;
    private final ResourceLocation textureLoc;
    private final BreezeModel<Breeze> model;

    public BreezeWindLayer(RenderLayerParent<Breeze, BreezeModel<Breeze>> pRenderer, EntityModelSet pModels, ResourceLocation pTextureLoc) {
        super(pRenderer);
        this.model = new BreezeModel<>(pModels.bakeLayer(ModelLayers.BREEZE_WIND));
        this.textureLoc = pTextureLoc;
    }

    public void render(
        PoseStack pPoseStack,
        MultiBufferSource pBuffer,
        int pPackedLight,
        Breeze pLivingEntity,
        float pLimbSwing,
        float pLimbSwingAmount,
        float pPartialTick,
        float pAgeInTicks,
        float pNetHeadYaw,
        float pHeadPitch
    ) {
        float f = (float)pLivingEntity.tickCount + pPartialTick;
        this.model.prepareMobModel(pLivingEntity, pLimbSwing, pLimbSwingAmount, pPartialTick);
        this.getParentModel().copyPropertiesTo(this.model);
        VertexConsumer vertexconsumer = pBuffer.getBuffer(RenderType.breezeWind(this.getTextureLocation(pLivingEntity), this.xOffset(f) % 1.0F, 0.0F));
        this.model.setupAnim(pLivingEntity, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);
        this.model.windTop().skipDraw = true;
        this.model.windMiddle().skipDraw = true;
        this.model.windBottom().skipDraw = false;
        this.model.root().render(pPoseStack, vertexconsumer, pPackedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        this.model.windTop().skipDraw = true;
        this.model.windMiddle().skipDraw = false;
        this.model.windBottom().skipDraw = true;
        this.model.root().render(pPoseStack, vertexconsumer, pPackedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        this.model.windTop().skipDraw = false;
        this.model.windMiddle().skipDraw = true;
        this.model.windBottom().skipDraw = true;
        this.model.root().render(pPoseStack, vertexconsumer, pPackedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    private float xOffset(float pTickCount) {
        return pTickCount * 0.02F;
    }

    protected ResourceLocation getTextureLocation(Breeze pEntity) {
        return this.textureLoc;
    }
}
