package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class EntityVariantPredicate<V> {
    private final Function<Entity, Optional<V>> getter;
    private final EntitySubPredicate.Type type;

    public static <V> EntityVariantPredicate<V> create(Registry<V> pRegistry, Function<Entity, Optional<V>> pGetter) {
        return new EntityVariantPredicate<>(pRegistry.byNameCodec(), pGetter);
    }

    public static <V> EntityVariantPredicate<V> create(Codec<V> pVariantCodec, Function<Entity, Optional<V>> pGetter) {
        return new EntityVariantPredicate<>(pVariantCodec, pGetter);
    }

    private EntityVariantPredicate(Codec<V> pVariantCodec, Function<Entity, Optional<V>> pGetter) {
        this.getter = pGetter;
        MapCodec<EntityVariantPredicate.SubPredicate<V>> mapcodec = RecordCodecBuilder.mapCodec(
            p_297900_ -> p_297900_.group(pVariantCodec.fieldOf("variant").forGetter(EntityVariantPredicate.SubPredicate::variant))
                    .apply(p_297900_, this::createPredicate)
        );
        this.type = new EntitySubPredicate.Type(mapcodec);
    }

    public EntitySubPredicate.Type type() {
        return this.type;
    }

    public EntityVariantPredicate.SubPredicate<V> createPredicate(V p_219097_) {
        return new EntityVariantPredicate.SubPredicate<>(this.type, this.getter, p_219097_);
    }

    public static record SubPredicate<V>(EntitySubPredicate.Type type, Function<Entity, Optional<V>> getter, V variant) implements EntitySubPredicate {
        @Override
        public boolean matches(Entity p_299274_, ServerLevel p_298330_, @Nullable Vec3 p_298762_) {
            return this.getter.apply(p_299274_).filter(p_299059_ -> p_299059_.equals(this.variant)).isPresent();
        }
    }
}
