package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;

/**
 * Context for validating loot tables. Loot tables are validated recursively by checking that all functions, conditions, etc. (implementing {@link LootContextUser}) are valid according to their LootTable's {@link LootContextParamSet}.
 */
public class ValidationContext {
    private final ProblemReporter reporter;
    private final LootContextParamSet params;
    private final LootDataResolver resolver;
    private final Set<LootDataId<?>> visitedElements;

    public ValidationContext(ProblemReporter pReporter, LootContextParamSet pParams, LootDataResolver pResolver) {
        this(pReporter, pParams, pResolver, Set.of());
    }

    private ValidationContext(ProblemReporter pReporter, LootContextParamSet pParams, LootDataResolver pResolver, Set<LootDataId<?>> pVisitedElements) {
        this.reporter = pReporter;
        this.params = pParams;
        this.resolver = pResolver;
        this.visitedElements = pVisitedElements;
    }

    /**
     * Create a new ValidationContext with {@code childName} being added to the context.
     */
    public ValidationContext forChild(String pChildName) {
        return new ValidationContext(this.reporter.forChild(pChildName), this.params, this.resolver, this.visitedElements);
    }

    public ValidationContext enterElement(String pChildName, LootDataId<?> pElement) {
        ImmutableSet<LootDataId<?>> immutableset = ImmutableSet.<LootDataId<?>>builder().addAll(this.visitedElements).add(pElement).build();
        return new ValidationContext(this.reporter.forChild(pChildName), this.params, this.resolver, immutableset);
    }

    public boolean hasVisitedElement(LootDataId<?> pElement) {
        return this.visitedElements.contains(pElement);
    }

    /**
     * Report a problem to this ValidationContext.
     */
    public void reportProblem(String pProblem) {
        this.reporter.report(pProblem);
    }

    /**
     * Validate the given LootContextUser.
     */
    public void validateUser(LootContextUser pLootContextUser) {
        this.params.validateUser(this, pLootContextUser);
    }

    public LootDataResolver resolver() {
        return this.resolver;
    }

    /**
     * Create a new ValidationContext with the given LootContextParamSet.
     */
    public ValidationContext setParams(LootContextParamSet pParams) {
        return new ValidationContext(this.reporter, pParams, this.resolver, this.visitedElements);
    }
}
