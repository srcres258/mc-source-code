package net.minecraft.client.renderer.block.model;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BakedQuad {
    /**
     * Joined 4 vertex records, each stores packed data according to the VertexFormat of the quad. Vanilla minecraft uses DefaultVertexFormats.BLOCK, Forge uses (usually) ITEM, use BakedQuad.getFormat() to get the correct format.
     */
    protected final int[] vertices;
    protected final int tintIndex;
    protected final Direction direction;
    protected final TextureAtlasSprite sprite;
    private final boolean shade;
    private final boolean hasAmbientOcclusion;

    public BakedQuad(int[] pVertices, int pTintIndex, Direction pDirection, TextureAtlasSprite pSprite, boolean pShade) {
        this(pVertices, pTintIndex, pDirection, pSprite, pShade, true);
    }

    public BakedQuad(int[] p_111298_, int p_111299_, Direction p_111300_, TextureAtlasSprite p_111301_, boolean p_111302_, boolean hasAmbientOcclusion) {
        this.vertices = p_111298_;
        this.tintIndex = p_111299_;
        this.direction = p_111300_;
        this.sprite = p_111301_;
        this.shade = p_111302_;
        this.hasAmbientOcclusion = hasAmbientOcclusion;
    }

    public TextureAtlasSprite getSprite() {
        return this.sprite;
    }

    public int[] getVertices() {
        return this.vertices;
    }

    public boolean isTinted() {
        return this.tintIndex != -1;
    }

    public int getTintIndex() {
        return this.tintIndex;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public boolean isShade() {
        return this.shade;
    }

    public boolean hasAmbientOcclusion() {
        return this.hasAmbientOcclusion;
    }
}
