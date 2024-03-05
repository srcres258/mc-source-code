package net.minecraft.client.particle;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SpriteSet {
    TextureAtlasSprite get(int pAge, int pLifetime);

    TextureAtlasSprite get(RandomSource pRandom);
}
