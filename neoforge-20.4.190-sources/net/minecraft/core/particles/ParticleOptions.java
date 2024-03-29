package net.minecraft.core.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.network.FriendlyByteBuf;

public interface ParticleOptions {
    ParticleType<?> getType();

    void writeToNetwork(FriendlyByteBuf pBuffer);

    String writeToString();

    @Deprecated
    public interface Deserializer<T extends ParticleOptions> {
        T fromCommand(ParticleType<T> pParticleType, StringReader pReader) throws CommandSyntaxException;

        T fromNetwork(ParticleType<T> pParticleType, FriendlyByteBuf pBuffer);
    }
}
