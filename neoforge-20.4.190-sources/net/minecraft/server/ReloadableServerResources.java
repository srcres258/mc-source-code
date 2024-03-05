package net.minecraft.server;

import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagManager;
import net.minecraft.util.Unit;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootDataManager;
import org.slf4j.Logger;

public class ReloadableServerResources {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final CompletableFuture<Unit> DATA_RELOAD_INITIAL_TASK = CompletableFuture.completedFuture(Unit.INSTANCE);
    private final CommandBuildContext.Configurable commandBuildContext;
    private final Commands commands;
    private final RecipeManager recipes = new RecipeManager();
    private final TagManager tagManager;
    private final LootDataManager lootData = new LootDataManager();
    private final ServerAdvancementManager advancements = new ServerAdvancementManager(this.lootData);
    private final ServerFunctionLibrary functionLibrary;

    public ReloadableServerResources(RegistryAccess.Frozen pRegistryAccess, FeatureFlagSet pEnabledFeatures, Commands.CommandSelection pCommandSelection, int pFunctionCompilationLevel) {
        this.tagManager = new TagManager(pRegistryAccess);
        this.commandBuildContext = CommandBuildContext.configurable(pRegistryAccess, pEnabledFeatures);
        this.commands = new Commands(pCommandSelection, this.commandBuildContext);
        this.commandBuildContext.missingTagAccessPolicy(CommandBuildContext.MissingTagAccessPolicy.CREATE_NEW);
        this.functionLibrary = new ServerFunctionLibrary(pFunctionCompilationLevel, this.commands.getDispatcher());
        // Neo: Create context object and inject it into listeners that need it
        this.registryAccess = pRegistryAccess;
        this.context = new net.neoforged.neoforge.common.conditions.ConditionContext(this.tagManager);
    }

    public ServerFunctionLibrary getFunctionLibrary() {
        return this.functionLibrary;
    }

    public LootDataManager getLootData() {
        return this.lootData;
    }

    public RecipeManager getRecipeManager() {
        return this.recipes;
    }

    public Commands getCommands() {
        return this.commands;
    }

    public ServerAdvancementManager getAdvancements() {
        return this.advancements;
    }

    public List<PreparableReloadListener> listeners() {
        return List.of(this.tagManager, this.lootData, this.recipes, this.functionLibrary, this.advancements);
    }

    public static CompletableFuture<ReloadableServerResources> loadResources(
        ResourceManager pResourceManager,
        RegistryAccess.Frozen pRegistryAccess,
        FeatureFlagSet pEnabledFeatures,
        Commands.CommandSelection pCommandSelection,
        int pFunctionCompilationLevel,
        Executor pBackgroundExecutor,
        Executor pGameExecutor
    ) {
        ReloadableServerResources reloadableserverresources = new ReloadableServerResources(pRegistryAccess, pEnabledFeatures, pCommandSelection, pFunctionCompilationLevel);
        List<PreparableReloadListener> listeners = new java.util.ArrayList<>(reloadableserverresources.listeners());
        listeners.addAll(net.neoforged.neoforge.event.EventHooks.onResourceReload(reloadableserverresources, pRegistryAccess));
        listeners.forEach(rl -> {
            if (rl instanceof net.neoforged.neoforge.resource.ContextAwareReloadListener srl) srl.injectContext(reloadableserverresources.context, reloadableserverresources.registryAccess);
        });
        return SimpleReloadInstance.create(
                pResourceManager, listeners, pBackgroundExecutor, pGameExecutor, DATA_RELOAD_INITIAL_TASK, LOGGER.isDebugEnabled()
            )
            .done()
            .whenComplete(
                (p_255534_, p_255535_) -> reloadableserverresources.commandBuildContext.missingTagAccessPolicy(CommandBuildContext.MissingTagAccessPolicy.FAIL)
            )
            .thenApply(p_214306_ -> reloadableserverresources);
    }

    public void updateRegistryTags(RegistryAccess pRegistryAccess) {
        this.tagManager.getResult().forEach(p_214315_ -> updateRegistryTags(pRegistryAccess, p_214315_));
        Blocks.rebuildCache();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.TagsUpdatedEvent(pRegistryAccess, false, false));
    }

    private static <T> void updateRegistryTags(RegistryAccess pRegistryAccess, TagManager.LoadResult<T> pLoadResult) {
        ResourceKey<? extends Registry<T>> resourcekey = pLoadResult.key();
        Map<TagKey<T>, List<Holder<T>>> map = pLoadResult.tags()
            .entrySet()
            .stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    p_214303_ -> TagKey.create(resourcekey, (ResourceLocation)p_214303_.getKey()), p_214312_ -> List.copyOf((Collection)p_214312_.getValue())
                )
            );
        pRegistryAccess.registryOrThrow(resourcekey).bindTags(map);
    }

    private final net.neoforged.neoforge.common.conditions.ICondition.IContext context;
    private final net.minecraft.core.RegistryAccess registryAccess;

    /**
     * Exposes the current condition context for usage in other reload listeners.<br>
     * This is not useful outside the reloading stage.
     * @return The condition context for the currently active reload.
     */
    public net.neoforged.neoforge.common.conditions.ICondition.IContext getConditionContext() {
        return this.context;
    }
    /**
     * {@return the registry access for the currently active reload}
     */
    public net.minecraft.core.RegistryAccess getRegistryAccess() {
        return this.registryAccess;
    }
}
