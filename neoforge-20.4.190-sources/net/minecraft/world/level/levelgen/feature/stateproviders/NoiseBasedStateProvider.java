package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.datafixers.Products.P3;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public abstract class NoiseBasedStateProvider extends BlockStateProvider {
    protected final long seed;
    protected final NormalNoise.NoiseParameters parameters;
    protected final float scale;
    protected final NormalNoise noise;

    protected static <P extends NoiseBasedStateProvider> P3<Mu<P>, Long, NormalNoise.NoiseParameters, Float> noiseCodec(Instance<P> pInstance) {
        return pInstance.group(
            Codec.LONG.fieldOf("seed").forGetter(p_191435_ -> p_191435_.seed),
            NormalNoise.NoiseParameters.DIRECT_CODEC.fieldOf("noise").forGetter(p_191433_ -> p_191433_.parameters),
            ExtraCodecs.POSITIVE_FLOAT.fieldOf("scale").forGetter(p_191428_ -> p_191428_.scale)
        );
    }

    protected NoiseBasedStateProvider(long pSeed, NormalNoise.NoiseParameters pParameters, float pScale) {
        this.seed = pSeed;
        this.parameters = pParameters;
        this.scale = pScale;
        this.noise = NormalNoise.create(new WorldgenRandom(new LegacyRandomSource(pSeed)), pParameters);
    }

    protected double getNoiseValue(BlockPos pPos, double pDelta) {
        return this.noise.getValue((double)pPos.getX() * pDelta, (double)pPos.getY() * pDelta, (double)pPos.getZ() * pDelta);
    }
}
