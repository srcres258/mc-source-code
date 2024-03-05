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
public class BreezeEyesLayer extends RenderLayer<Breeze, BreezeModel<Breeze>> {
    private final ResourceLocation textureLoc;
    private final BreezeModel<Breeze> model;

    public BreezeEyesLayer(RenderLayerParent<Breeze, BreezeModel<Breeze>> pRenderer, EntityModelSet pModels, ResourceLocation pTextureLoc) {
        super(pRenderer);
        this.model = new BreezeModel<>(pModels.bakeLayer(ModelLayers.BREEZE_EYES));
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
        this.model.prepareMobModel(pLivingEntity, pLimbSwing, pLimbSwingAmount, pPartialTick);
        this.getParentModel().copyPropertiesTo(this.model);
        VertexConsumer vertexconsumer = pBuffer.getBuffer(RenderType.breezeEyes(this.textureLoc));
        this.model.setupAnim(pLivingEntity, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);
        this.model.root().render(pPoseStack, vertexconsumer, pPackedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    protected ResourceLocation getTextureLocation(Breeze pEntity) {
        return this.textureLoc;
    }
}
