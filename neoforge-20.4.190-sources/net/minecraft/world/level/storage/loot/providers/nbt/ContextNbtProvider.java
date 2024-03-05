package net.minecraft.world.level.storage.loot.providers.nbt;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * A NbtProvider that provides either the {@linkplain LootContextParams#BLOCK_ENTITY block entity}'s NBT data or an entity's NBT data based on an {@link LootContext.EntityTarget}.
 */
public class ContextNbtProvider implements NbtProvider {
    private static final String BLOCK_ENTITY_ID = "block_entity";
    private static final ContextNbtProvider.Getter BLOCK_ENTITY_PROVIDER = new ContextNbtProvider.Getter() {
        @Override
        public Tag get(LootContext p_165582_) {
            BlockEntity blockentity = p_165582_.getParamOrNull(LootContextParams.BLOCK_ENTITY);
            return blockentity != null ? blockentity.saveWithFullMetadata() : null;
        }

        @Override
        public String getId() {
            return "block_entity";
        }

        @Override
        public Set<LootContextParam<?>> getReferencedContextParams() {
            return ImmutableSet.of(LootContextParams.BLOCK_ENTITY);
        }
    };
    public static final ContextNbtProvider BLOCK_ENTITY = new ContextNbtProvider(BLOCK_ENTITY_PROVIDER);
    private static final Codec<ContextNbtProvider.Getter> GETTER_CODEC = Codec.STRING.xmap(p_298998_ -> {
        if (p_298998_.equals("block_entity")) {
            return BLOCK_ENTITY_PROVIDER;
        } else {
            LootContext.EntityTarget lootcontext$entitytarget = LootContext.EntityTarget.getByName(p_298998_);
            return forEntity(lootcontext$entitytarget);
        }
    }, ContextNbtProvider.Getter::getId);
    public static final Codec<ContextNbtProvider> CODEC = RecordCodecBuilder.create(
        p_298866_ -> p_298866_.group(GETTER_CODEC.fieldOf("target").forGetter(p_298514_ -> p_298514_.getter)).apply(p_298866_, ContextNbtProvider::new)
    );
    public static final Codec<ContextNbtProvider> INLINE_CODEC = GETTER_CODEC.xmap(ContextNbtProvider::new, p_298731_ -> p_298731_.getter);
    private final ContextNbtProvider.Getter getter;

    private static ContextNbtProvider.Getter forEntity(final LootContext.EntityTarget pEntityTarget) {
        return new ContextNbtProvider.Getter() {
            @Nullable
            @Override
            public Tag get(LootContext p_165589_) {
                Entity entity = p_165589_.getParamOrNull(pEntityTarget.getParam());
                return entity != null ? NbtPredicate.getEntityTagToCompare(entity) : null;
            }

            @Override
            public String getId() {
                return pEntityTarget.getName();
            }

            @Override
            public Set<LootContextParam<?>> getReferencedContextParams() {
                return ImmutableSet.of(pEntityTarget.getParam());
            }
        };
    }

    private ContextNbtProvider(ContextNbtProvider.Getter p_165568_) {
        this.getter = p_165568_;
    }

    @Override
    public LootNbtProviderType getType() {
        return NbtProviders.CONTEXT;
    }

    @Nullable
    @Override
    public Tag get(LootContext pLootContext) {
        return this.getter.get(pLootContext);
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return this.getter.getReferencedContextParams();
    }

    public static NbtProvider forContextEntity(LootContext.EntityTarget pEntityTarget) {
        return new ContextNbtProvider(forEntity(pEntityTarget));
    }

    interface Getter {
        @Nullable
        Tag get(LootContext pLootContext);

        String getId();

        Set<LootContextParam<?>> getReferencedContextParams();
    }
}
