package net.minecraft.commands;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlagSet;

public interface CommandBuildContext {
    <T> HolderLookup<T> holderLookup(ResourceKey<? extends Registry<T>> pRegistryResourceKey);

    static CommandBuildContext simple(final HolderLookup.Provider pProvider, final FeatureFlagSet pEnabledFeatures) {
        return new CommandBuildContext() {
            @Override
            public <T> HolderLookup<T> holderLookup(ResourceKey<? extends Registry<T>> p_255791_) {
                return pProvider.<T>lookupOrThrow(p_255791_).filterFeatures(pEnabledFeatures);
            }
        };
    }

    static CommandBuildContext.Configurable configurable(final RegistryAccess pRegistryAccess, final FeatureFlagSet pEnabledFeatures) {
        return new CommandBuildContext.Configurable() {
            CommandBuildContext.MissingTagAccessPolicy missingTagAccessPolicy = CommandBuildContext.MissingTagAccessPolicy.FAIL;

            @Override
            public void missingTagAccessPolicy(CommandBuildContext.MissingTagAccessPolicy p_256626_) {
                this.missingTagAccessPolicy = p_256626_;
            }

            @Override
            public <T> HolderLookup<T> holderLookup(ResourceKey<? extends Registry<T>> p_256616_) {
                Registry<T> registry = pRegistryAccess.registryOrThrow(p_256616_);
                final HolderLookup.RegistryLookup<T> registrylookup = registry.asLookup();
                final HolderLookup.RegistryLookup<T> registrylookup1 = registry.asTagAddingLookup();
                HolderLookup.RegistryLookup<T> registrylookup2 = new HolderLookup.RegistryLookup.Delegate<T>() {
                    @Override
                    protected HolderLookup.RegistryLookup<T> parent() {
                        return switch(missingTagAccessPolicy) {
                            case FAIL -> registrylookup;
                            case CREATE_NEW -> registrylookup1;
                        };
                    }
                };
                return registrylookup2.filterFeatures(pEnabledFeatures);
            }
        };
    }

    public interface Configurable extends CommandBuildContext {
        void missingTagAccessPolicy(CommandBuildContext.MissingTagAccessPolicy pMissingTagAccessPolicy);
    }

    public static enum MissingTagAccessPolicy {
        CREATE_NEW,
        FAIL;
    }
}
