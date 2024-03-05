package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class GustDustParticle extends TextureSheetParticle {
    private final Vector3f fromColor = new Vector3f(0.5F, 0.5F, 0.5F);
    private final Vector3f toColor = new Vector3f(1.0F, 1.0F, 1.0F);

    GustDustParticle(ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
        super(pLevel, pX, pY, pZ);
        this.hasPhysics = false;
        this.xd = pXSpeed + (double)Mth.randomBetween(this.random, -0.4F, 0.4F);
        this.zd = pZSpeed + (double)Mth.randomBetween(this.random, -0.4F, 0.4F);
        double d0 = Math.random() * 2.0;
        double d1 = Math.sqrt(this.xd * this.xd + this.yd * this.yd + this.zd * this.zd);
        this.xd = this.xd / d1 * d0 * 0.4F;
        this.zd = this.zd / d1 * d0 * 0.4F;
        this.quadSize *= 2.5F;
        this.xd *= 0.08F;
        this.zd *= 0.08F;
        this.lifetime = 18 + this.random.nextInt(4);
    }

    @Override
    public void render(VertexConsumer pBuffer, Camera pRenderInfo, float pPartialTicks) {
        this.lerpColors(pPartialTicks);
        super.render(pBuffer, pRenderInfo, pPartialTicks);
    }

    private void lerpColors(float pPartialTick) {
        float f = ((float)this.age + pPartialTick) / (float)(this.lifetime + 1);
        Vector3f vector3f = new Vector3f(this.fromColor).lerp(this.toColor, f);
        this.rCol = vector3f.x();
        this.gCol = vector3f.y();
        this.bCol = vector3f.z();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.xo = this.x;
            this.zo = this.z;
            this.move(this.xd, 0.0, this.zd);
            this.xd *= 0.99;
            this.zd *= 0.99;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class GustDustParticleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public GustDustParticleProvider(SpriteSet pSprite) {
            this.sprite = pSprite;
        }

        public Particle createParticle(
            SimpleParticleType pType,
            ClientLevel pLevel,
            double pX,
            double pY,
            double pZ,
            double pXSpeed,
            double pYSpeed,
            double pZSpeed
        ) {
            GustDustParticle gustdustparticle = new GustDustParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
            gustdustparticle.pickSprite(this.sprite);
            return gustdustparticle;
        }
    }
}
