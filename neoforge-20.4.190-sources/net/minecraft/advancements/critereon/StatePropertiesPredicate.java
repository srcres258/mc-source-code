package net.minecraft.advancements.critereon;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;

public record StatePropertiesPredicate(List<StatePropertiesPredicate.PropertyMatcher> properties) {
    private static final Codec<List<StatePropertiesPredicate.PropertyMatcher>> PROPERTIES_CODEC = Codec.unboundedMap(
            Codec.STRING, StatePropertiesPredicate.ValueMatcher.CODEC
        )
        .xmap(
            p_297916_ -> p_297916_.entrySet()
                    .stream()
                    .map(
                        p_297914_ -> new StatePropertiesPredicate.PropertyMatcher(
                                (String)p_297914_.getKey(), (StatePropertiesPredicate.ValueMatcher)p_297914_.getValue()
                            )
                    )
                    .toList(),
            p_297915_ -> p_297915_.stream()
                    .collect(Collectors.toMap(StatePropertiesPredicate.PropertyMatcher::name, StatePropertiesPredicate.PropertyMatcher::valueMatcher))
        );
    public static final Codec<StatePropertiesPredicate> CODEC = PROPERTIES_CODEC.xmap(StatePropertiesPredicate::new, StatePropertiesPredicate::properties);

    public <S extends StateHolder<?, S>> boolean matches(StateDefinition<?, S> pProperties, S pTargetProperty) {
        for(StatePropertiesPredicate.PropertyMatcher statepropertiespredicate$propertymatcher : this.properties) {
            if (!statepropertiespredicate$propertymatcher.match(pProperties, pTargetProperty)) {
                return false;
            }
        }

        return true;
    }

    public boolean matches(BlockState pState) {
        return this.matches(pState.getBlock().getStateDefinition(), pState);
    }

    public boolean matches(FluidState pState) {
        return this.matches(pState.getType().getStateDefinition(), pState);
    }

    public Optional<String> checkState(StateDefinition<?, ?> pState) {
        for(StatePropertiesPredicate.PropertyMatcher statepropertiespredicate$propertymatcher : this.properties) {
            Optional<String> optional = statepropertiespredicate$propertymatcher.checkState(pState);
            if (optional.isPresent()) {
                return optional;
            }
        }

        return Optional.empty();
    }

    public static class Builder {
        private final ImmutableList.Builder<StatePropertiesPredicate.PropertyMatcher> matchers = ImmutableList.builder();

        private Builder() {
        }

        public static StatePropertiesPredicate.Builder properties() {
            return new StatePropertiesPredicate.Builder();
        }

        public StatePropertiesPredicate.Builder hasProperty(Property<?> pProperty, String pValue) {
            this.matchers.add(new StatePropertiesPredicate.PropertyMatcher(pProperty.getName(), new StatePropertiesPredicate.ExactMatcher(pValue)));
            return this;
        }

        public StatePropertiesPredicate.Builder hasProperty(Property<Integer> pProperty, int pValue) {
            return this.hasProperty(pProperty, Integer.toString(pValue));
        }

        public StatePropertiesPredicate.Builder hasProperty(Property<Boolean> pProperty, boolean pValue) {
            return this.hasProperty(pProperty, Boolean.toString(pValue));
        }

        public <T extends Comparable<T> & StringRepresentable> StatePropertiesPredicate.Builder hasProperty(Property<T> pProperty, T pValue) {
            return this.hasProperty(pProperty, pValue.getSerializedName());
        }

        public Optional<StatePropertiesPredicate> build() {
            return Optional.of(new StatePropertiesPredicate(this.matchers.build()));
        }
    }

    static record ExactMatcher(String value) implements StatePropertiesPredicate.ValueMatcher {
        public static final Codec<StatePropertiesPredicate.ExactMatcher> CODEC = Codec.STRING
            .xmap(StatePropertiesPredicate.ExactMatcher::new, StatePropertiesPredicate.ExactMatcher::value);

        @Override
        public <T extends Comparable<T>> boolean match(StateHolder<?, ?> p_298379_, Property<T> p_299294_) {
            T t = p_298379_.getValue(p_299294_);
            Optional<T> optional = p_299294_.getValue(this.value);
            return optional.isPresent() && t.compareTo(optional.get()) == 0;
        }
    }

    static record PropertyMatcher(String name, StatePropertiesPredicate.ValueMatcher valueMatcher) {
        public <S extends StateHolder<?, S>> boolean match(StateDefinition<?, S> pProperties, S pPropertyToMatch) {
            Property<?> property = pProperties.getProperty(this.name);
            return property != null && this.valueMatcher.match(pPropertyToMatch, property);
        }

        public Optional<String> checkState(StateDefinition<?, ?> pState) {
            Property<?> property = pState.getProperty(this.name);
            return property != null ? Optional.empty() : Optional.of(this.name);
        }
    }

    static record RangedMatcher(Optional<String> minValue, Optional<String> maxValue) implements StatePropertiesPredicate.ValueMatcher {
        public static final Codec<StatePropertiesPredicate.RangedMatcher> CODEC = RecordCodecBuilder.create(
            p_298882_ -> p_298882_.group(
                        ExtraCodecs.strictOptionalField(Codec.STRING, "min").forGetter(StatePropertiesPredicate.RangedMatcher::minValue),
                        ExtraCodecs.strictOptionalField(Codec.STRING, "max").forGetter(StatePropertiesPredicate.RangedMatcher::maxValue)
                    )
                    .apply(p_298882_, StatePropertiesPredicate.RangedMatcher::new)
        );

        @Override
        public <T extends Comparable<T>> boolean match(StateHolder<?, ?> p_298772_, Property<T> p_298371_) {
            T t = p_298772_.getValue(p_298371_);
            if (this.minValue.isPresent()) {
                Optional<T> optional = p_298371_.getValue(this.minValue.get());
                if (optional.isEmpty() || t.compareTo(optional.get()) < 0) {
                    return false;
                }
            }

            if (this.maxValue.isPresent()) {
                Optional<T> optional1 = p_298371_.getValue(this.maxValue.get());
                if (optional1.isEmpty() || t.compareTo(optional1.get()) > 0) {
                    return false;
                }
            }

            return true;
        }
    }

    interface ValueMatcher {
        Codec<StatePropertiesPredicate.ValueMatcher> CODEC = Codec.either(
                StatePropertiesPredicate.ExactMatcher.CODEC, StatePropertiesPredicate.RangedMatcher.CODEC
            )
            .xmap(p_299142_ -> p_299142_.map(p_298410_ -> p_298410_, p_298516_ -> p_298516_), p_299089_ -> {
                if (p_299089_ instanceof StatePropertiesPredicate.ExactMatcher statepropertiespredicate$exactmatcher) {
                    return Either.left(statepropertiespredicate$exactmatcher);
                } else if (p_299089_ instanceof StatePropertiesPredicate.RangedMatcher statepropertiespredicate$rangedmatcher) {
                    return Either.right(statepropertiespredicate$rangedmatcher);
                } else {
                    throw new UnsupportedOperationException();
                }
            });

        <T extends Comparable<T>> boolean match(StateHolder<?, ?> pStateHolder, Property<T> pProperty);
    }
}
