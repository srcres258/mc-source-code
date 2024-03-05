package net.minecraft.client.gui.narration;

import net.minecraft.client.gui.components.TabOrderedElement;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * An interface for GUI elements that can provide narration information.
 */
@OnlyIn(Dist.CLIENT)
public interface NarratableEntry extends TabOrderedElement, NarrationSupplier {
    /**
     * {@return the narration priority}
     */
    NarratableEntry.NarrationPriority narrationPriority();

    /**
     * {@return {@code true} if the element is active, {@code false} otherwise}
     */
    default boolean isActive() {
        return true;
    }

    /**
     * The narration priority levels.
     */
    @OnlyIn(Dist.CLIENT)
    public static enum NarrationPriority {
        /**
         * No narration priority.
         */
        NONE,
        /**
         * Narration priority when the element is being hovered.
         */
        HOVERED,
        /**
         * Narration priority when the element is focused.
         */
        FOCUSED;

        /**
         * Checks if the narration priority is terminal, indicating that no further narration will occur after this.
         * <p>
         * @return {@code true} if the narration priority is terminal, {@code false} otherwise.
         */
        public boolean isTerminal() {
            return this == FOCUSED;
        }
    }
}
