package net.minecraft.client.gui.screens.controls;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.ArrayUtils;

@OnlyIn(Dist.CLIENT)
public class KeyBindsList extends ContainerObjectSelectionList<KeyBindsList.Entry> {
    final KeyBindsScreen keyBindsScreen;
    int maxNameWidth;

    public KeyBindsList(KeyBindsScreen pKeyBindsScreen, Minecraft pMinecraft) {
        super(pMinecraft, pKeyBindsScreen.width + 45, pKeyBindsScreen.height - 52, 20, 20);
        this.keyBindsScreen = pKeyBindsScreen;
        KeyMapping[] akeymapping = ArrayUtils.clone((KeyMapping[])pMinecraft.options.keyMappings);
        Arrays.sort((Object[])akeymapping);
        String s = null;

        for(KeyMapping keymapping : akeymapping) {
            String s1 = keymapping.getCategory();
            if (!s1.equals(s)) {
                s = s1;
                this.addEntry(new KeyBindsList.CategoryEntry(Component.translatable(s1)));
            }

            Component component = Component.translatable(keymapping.getName());
            int i = pMinecraft.font.width(component);
            if (i > this.maxNameWidth) {
                this.maxNameWidth = i;
            }

            this.addEntry(new KeyBindsList.KeyEntry(keymapping, component));
        }
    }

    public void resetMappingAndUpdateButtons() {
        KeyMapping.resetMapping();
        this.refreshEntries();
    }

    public void refreshEntries() {
        this.children().forEach(KeyBindsList.Entry::refreshEntry);
    }

    @Override
    protected int getScrollbarPosition() {
        return super.getScrollbarPosition() + 15;
    }

    @Override
    public int getRowWidth() {
        return super.getRowWidth() + 32;
    }

    @OnlyIn(Dist.CLIENT)
    public class CategoryEntry extends KeyBindsList.Entry {
        final Component name;
        private final int width;

        public CategoryEntry(Component pName) {
            this.name = pName;
            this.width = KeyBindsList.this.minecraft.font.width(this.name);
        }

        @Override
        public void render(
            GuiGraphics pGuiGraphics,
            int pIndex,
            int pTop,
            int pLeft,
            int pWidth,
            int pHeight,
            int pMouseX,
            int pMouseY,
            boolean pHovering,
            float pPartialTick
        ) {
            pGuiGraphics.drawString(
                KeyBindsList.this.minecraft.font,
                this.name,
                KeyBindsList.this.minecraft.screen.width / 2 - this.width / 2,
                pTop + pHeight - 9 - 1,
                16777215,
                false
            );
        }

        /**
         * Retrieves the next focus path based on the given focus navigation event.
         * <p>
         * @return the next focus path as a ComponentPath, or {@code null} if there is no next focus path.
         *
         * @param pEvent the focus navigation event.
         */
        @Nullable
        @Override
        public ComponentPath nextFocusPath(FocusNavigationEvent pEvent) {
            return null;
        }

        /**
         * {@return a List containing all GUI element children of this GUI element}
         */
        @Override
        public List<? extends GuiEventListener> children() {
            return Collections.emptyList();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return ImmutableList.of(new NarratableEntry() {
                @Override
                public NarratableEntry.NarrationPriority narrationPriority() {
                    return NarratableEntry.NarrationPriority.HOVERED;
                }

                @Override
                public void updateNarration(NarrationElementOutput p_193906_) {
                    p_193906_.add(NarratedElementType.TITLE, CategoryEntry.this.name);
                }
            });
        }

        @Override
        protected void refreshEntry() {
        }
    }

    @OnlyIn(Dist.CLIENT)
    public abstract static class Entry extends ContainerObjectSelectionList.Entry<KeyBindsList.Entry> {
        abstract void refreshEntry();
    }

    @OnlyIn(Dist.CLIENT)
    public class KeyEntry extends KeyBindsList.Entry {
        /**
         * The keybinding specified for this KeyEntry
         */
        private final KeyMapping key;
        /**
         * The localized key description for this KeyEntry
         */
        private final Component name;
        private final Button changeButton;
        private final Button resetButton;
        private boolean hasCollision = false;

        KeyEntry(KeyMapping pKey, Component pName) {
            this.key = pKey;
            this.name = pName;
            this.changeButton = Button.builder(pName, p_269618_ -> {
                    KeyBindsList.this.keyBindsScreen.selectedKey = pKey;
                    KeyBindsList.this.resetMappingAndUpdateButtons();
                })
                .bounds(0, 0, 75, 20)
                .createNarration(
                    p_253311_ -> pKey.isUnbound()
                            ? Component.translatable("narrator.controls.unbound", pName)
                            : Component.translatable("narrator.controls.bound", pName, p_253311_.get())
                )
                .build();
            this.resetButton = Button.builder(Component.translatable("controls.reset"), p_269616_ -> {
                this.key.setToDefault();
                KeyBindsList.this.minecraft.options.setKey(pKey, pKey.getDefaultKey());
                KeyBindsList.this.resetMappingAndUpdateButtons();
            }).bounds(0, 0, 50, 20).createNarration(p_253313_ -> Component.translatable("narrator.controls.reset", pName)).build();
            this.refreshEntry();
        }

        @Override
        public void render(
            GuiGraphics pGuiGraphics,
            int pIndex,
            int pTop,
            int pLeft,
            int pWidth,
            int pHeight,
            int pMouseX,
            int pMouseY,
            boolean pHovering,
            float pPartialTick
        ) {
            int textMaxWidth = pGuiGraphics.guiWidth() / 2;
            int k = pLeft + 90 - Math.min(KeyBindsList.this.maxNameWidth, textMaxWidth - 40);
            net.minecraft.client.gui.components.AbstractWidget.renderScrollingString(pGuiGraphics, KeyBindsList.this.minecraft.font, this.name, k, k, pTop + pHeight / 2 - 9 / 2, textMaxWidth, pTop + pHeight / 2 - 9 / 2 + 16, 16777215); // Neo: Makes descriptions start scrolling if too long
            this.resetButton.setX(pLeft + 190);
            this.resetButton.setY(pTop);
            this.resetButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            this.changeButton.setX(pLeft + 105);
            this.changeButton.setY(pTop);
            if (this.hasCollision) {
                int i = 3;
                int j = this.changeButton.getX() - 6;
                pGuiGraphics.fill(j, pTop + 2, j + 3, pTop + pHeight + 2, ChatFormatting.RED.getColor() | 0xFF000000);
            }

            this.changeButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        }

        /**
         * {@return a List containing all GUI element children of this GUI element}
         */
        @Override
        public List<? extends GuiEventListener> children() {
            return ImmutableList.of(this.changeButton, this.resetButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return ImmutableList.of(this.changeButton, this.resetButton);
        }

        @Override
        protected void refreshEntry() {
            this.changeButton.setMessage(this.key.getTranslatedKeyMessage());
            this.resetButton.active = !this.key.isDefault();
            this.hasCollision = false;
            MutableComponent mutablecomponent = Component.empty();
            if (!this.key.isUnbound()) {
                for(KeyMapping keymapping : KeyBindsList.this.minecraft.options.keyMappings) {
                    if ((keymapping != this.key && this.key.same(keymapping)) || keymapping.hasKeyModifierConflict(this.key)) { // Neo: gracefully handle conflicts like SHIFT vs SHIFT+G
                        if (this.hasCollision) {
                            mutablecomponent.append(", ");
                        }

                        this.hasCollision = true;
                        mutablecomponent.append(Component.translatable(keymapping.getName()));
                    }
                }
            }

            if (this.hasCollision) {
                this.changeButton
                    .setMessage(
                        Component.literal("[ ")
                            .append(this.changeButton.getMessage().copy().withStyle(ChatFormatting.WHITE))
                            .append(" ]")
                            .withStyle(ChatFormatting.RED)
                    );
                this.changeButton.setTooltip(Tooltip.create(Component.translatable("controls.keybinds.duplicateKeybinds", mutablecomponent)));
            } else {
                this.changeButton.setTooltip(null);
            }

            if (KeyBindsList.this.keyBindsScreen.selectedKey == this.key) {
                this.changeButton
                    .setMessage(
                        Component.literal("> ")
                            .append(this.changeButton.getMessage().copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
                            .append(" <")
                            .withStyle(ChatFormatting.YELLOW)
                    );
            }
        }
    }
}
