package net.minecraft.client.gui.screens;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LanguageSelectScreen extends OptionsSubScreen {
    private static final Component WARNING_LABEL = Component.translatable("options.languageAccuracyWarning").withStyle(ChatFormatting.GRAY);
    /**
     * The List GuiSlot object reference.
     */
    private LanguageSelectScreen.LanguageSelectionList packSelectionList;
    /**
     * Reference to the LanguageManager object.
     */
    final LanguageManager languageManager;

    public LanguageSelectScreen(Screen pLastScreen, Options pOptions, LanguageManager pLanguageManager) {
        super(pLastScreen, pOptions, Component.translatable("options.language.title"));
        this.languageManager = pLanguageManager;
    }

    @Override
    protected void init() {
        this.packSelectionList = this.addRenderableWidget(new LanguageSelectScreen.LanguageSelectionList(this.minecraft));
        this.addRenderableWidget(this.options.forceUnicodeFont().createButton(this.options, this.width / 2 - 155, this.height - 38, 150));
        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE, p_288243_ -> this.onDone()).bounds(this.width / 2 - 155 + 160, this.height - 38, 150, 20).build()
        );
    }

    void onDone() {
        LanguageSelectScreen.LanguageSelectionList.Entry languageselectscreen$languageselectionlist$entry = this.packSelectionList.getSelected();
        if (languageselectscreen$languageselectionlist$entry != null
            && !languageselectscreen$languageselectionlist$entry.code.equals(this.languageManager.getSelected())) {
            this.languageManager.setSelected(languageselectscreen$languageselectionlist$entry.code);
            this.options.languageCode = languageselectscreen$languageselectionlist$entry.code;
            this.minecraft.reloadResourcePacks();
            this.options.save();
        }

        this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (CommonInputs.selected(pKeyCode)) {
            LanguageSelectScreen.LanguageSelectionList.Entry languageselectscreen$languageselectionlist$entry = this.packSelectionList.getSelected();
            if (languageselectscreen$languageselectionlist$entry != null) {
                languageselectscreen$languageselectionlist$entry.select();
                this.onDone();
                return true;
            }
        }

        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    /**
     * Renders the graphical user interface (GUI) element.
     *
     * @param pGuiGraphics the GuiGraphics object used for rendering.
     * @param pMouseX      the x-coordinate of the mouse cursor.
     * @param pMouseY      the y-coordinate of the mouse cursor.
     * @param pPartialTick the partial tick time.
     */
    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 16777215);
        pGuiGraphics.drawCenteredString(this.font, WARNING_LABEL, this.width / 2, this.height - 56, -8355712);
    }

    @Override
    public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderDirtBackground(pGuiGraphics);
    }

    @OnlyIn(Dist.CLIENT)
    class LanguageSelectionList extends ObjectSelectionList<LanguageSelectScreen.LanguageSelectionList.Entry> {
        public LanguageSelectionList(Minecraft pMinecraft) {
            super(pMinecraft, LanguageSelectScreen.this.width, LanguageSelectScreen.this.height - 93, 32, 18);
            String s = LanguageSelectScreen.this.languageManager.getSelected();
            LanguageSelectScreen.this.languageManager
                .getLanguages()
                .forEach(
                    (p_265492_, p_265377_) -> {
                        LanguageSelectScreen.LanguageSelectionList.Entry languageselectscreen$languageselectionlist$entry = new LanguageSelectScreen.LanguageSelectionList.Entry(
                            p_265492_, p_265377_
                        );
                        this.addEntry(languageselectscreen$languageselectionlist$entry);
                        if (s.equals(p_265492_)) {
                            this.setSelected(languageselectscreen$languageselectionlist$entry);
                        }
                    }
                );
            if (this.getSelected() != null) {
                this.centerScrollOn(this.getSelected());
            }
        }

        @Override
        protected int getScrollbarPosition() {
            return super.getScrollbarPosition() + 20;
        }

        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 50;
        }

        @OnlyIn(Dist.CLIENT)
        public class Entry extends ObjectSelectionList.Entry<LanguageSelectScreen.LanguageSelectionList.Entry> {
            final String code;
            private final Component language;
            private long lastClickTime;

            public Entry(String pCode, LanguageInfo pLanguage) {
                this.code = pCode;
                this.language = pLanguage.toComponent();
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
                pGuiGraphics.drawCenteredString(LanguageSelectScreen.this.font, this.language, LanguageSelectionList.this.width / 2, pTop + 1, 16777215);
            }

            /**
             * Called when a mouse button is clicked within the GUI element.
             * <p>
             * @return {@code true} if the event is consumed, {@code false} otherwise.
             *
             * @param pMouseX the X coordinate of the mouse.
             * @param pMouseY the Y coordinate of the mouse.
             * @param pButton the button that was clicked.
             */
            @Override
            public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
                this.select();
                if (Util.getMillis() - this.lastClickTime < 250L) {
                    LanguageSelectScreen.this.onDone();
                }

                this.lastClickTime = Util.getMillis();
                return true;
            }

            void select() {
                LanguageSelectionList.this.setSelected(this);
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", this.language);
            }
        }
    }
}
