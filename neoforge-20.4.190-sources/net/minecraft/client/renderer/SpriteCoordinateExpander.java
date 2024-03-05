package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpriteCoordinateExpander implements VertexConsumer {
    private final VertexConsumer delegate;
    private final TextureAtlasSprite sprite;

    public SpriteCoordinateExpander(VertexConsumer pDelegate, TextureAtlasSprite pSprite) {
        this.delegate = pDelegate;
        this.sprite = pSprite;
    }

    @Override
    public VertexConsumer vertex(double pX, double pY, double pZ) {
        this.delegate.vertex(pX, pY, pZ);
        return this; //Forge: Fix MC-263524 not working with chained methods
    }

    @Override
    public VertexConsumer color(int pRed, int pGreen, int pBlue, int pAlpha) {
        this.delegate.color(pRed, pGreen, pBlue, pAlpha);
        return this; //Forge: Fix MC-263524 not working with chained methods
    }

    @Override
    public VertexConsumer uv(float pU, float pV) {
        this.delegate.uv(this.sprite.getU(pU), this.sprite.getV(pV));
        return this; //Forge: Fix MC-263524 not working with chained methods
    }

    @Override
    public VertexConsumer overlayCoords(int pU, int pV) {
        this.delegate.overlayCoords(pU, pV);
        return this; //Forge: Fix MC-263524 not working with chained methods
    }

    @Override
    public VertexConsumer uv2(int pU, int pV) {
        this.delegate.uv2(pU, pV);
        return this; //Forge: Fix MC-263524 not working with chained methods
    }

    @Override
    public VertexConsumer normal(float pX, float pY, float pZ) {
        this.delegate.normal(pX, pY, pZ);
        return this; //Forge: Fix MC-263524 not working with chained methods
    }

    @Override
    public void endVertex() {
        this.delegate.endVertex();
    }

    @Override
    public void defaultColor(int pDefaultR, int pDefaultG, int pDefaultB, int pDefaultA) {
        this.delegate.defaultColor(pDefaultR, pDefaultG, pDefaultB, pDefaultA);
    }

    @Override
    public void unsetDefaultColor() {
        this.delegate.unsetDefaultColor();
    }

    @Override
    public void vertex(
        float pX,
        float pY,
        float pZ,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha,
        float pTexU,
        float pTexV,
        int pOverlayUV,
        int pLightmapUV,
        float pNormalX,
        float pNormalY,
        float pNormalZ
    ) {
        this.delegate
            .vertex(
                pX,
                pY,
                pZ,
                pRed,
                pGreen,
                pBlue,
                pAlpha,
                this.sprite.getU(pTexU),
                this.sprite.getV(pTexV),
                pOverlayUV,
                pLightmapUV,
                pNormalX,
                pNormalY,
                pNormalZ
            );
    }
}
