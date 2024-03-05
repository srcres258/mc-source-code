package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GustSeedParticle extends NoRenderParticle {
    GustSeedParticle(ClientLevel p_311842_, double p_312103_, double p_312358_, double p_312614_) {
        super(p_311842_, p_312103_, p_312358_, p_312614_, 0.0, 0.0, 0.0);
        this.lifetime = 7;
    }

    @Override
    public void tick() {
        for(int i = 0; i < 3; ++i) {
            double d0 = this.x + (this.random.nextDouble() - this.random.nextDouble()) * 4.0;
            double d1 = this.y + (this.random.nextDouble() - this.random.nextDouble()) * 4.0;
            double d2 = this.z + (this.random.nextDouble() - this.random.nextDouble()) * 4.0;
            this.level.addParticle(ParticleTypes.GUST, d0, d1, d2, (double)((float)this.age / (float)this.lifetime), 0.0, 0.0);
        }

        if (this.age++ == this.lifetime) {
            this.remove();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        public Particle createParticle(
            SimpleParticleType p_312546_,
            ClientLevel p_311824_,
            double p_311962_,
            double p_312036_,
            double p_312293_,
            double p_312087_,
            double p_312437_,
            double p_312656_
        ) {
            return new GustSeedParticle(p_311824_, p_311962_, p_312036_, p_312293_);
        }
    }
}
