package com.lanche.simpleprogress;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class LanguageSettingsScreen extends Screen {
    private final Screen parent;
    private int scrollOffset = 0;
    private String selectedLanguage = LanguageManager.getCurrentLanguage();
    private static final int VISIBLE_LANGUAGES = 10;

    public LanguageSettingsScreen(Screen parent) {
        super(Component.translatable("simpleprogress.gui.language_settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        String[] languages = LanguageManager.getSupportedLanguages();
        int startY = 50;
        int buttonHeight = 20;

        for (int i = 0; i < Math.min(VISIBLE_LANGUAGES, languages.length - scrollOffset); i++) {
            final String langCode = languages[i + scrollOffset];
            String displayName = LanguageManager.getLanguageDisplayName(langCode);

            this.addRenderableWidget(Button.builder(
                    Component.literal(displayName + (langCode.equals(selectedLanguage) ? " ✓" : "")),
                    button -> {
                        selectedLanguage = langCode;
                        LanguageManager.loadLanguage(langCode);
                        this.clearWidgets();
                        this.init();
                    }
            ).pos(this.width / 2 - 150, startY + i * (buttonHeight + 5)).size(300, buttonHeight).build());
        }

        this.addRenderableWidget(Button.builder(
                Component.translatable("simpleprogress.gui.back"),
                button -> this.minecraft.setScreen(this.parent)
        ).pos(this.width / 2 - 100, this.height - 40).size(200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        String currentLangText = LanguageManager.getTranslation("simpleprogress.gui.current_language") + ": " +
                LanguageManager.getLanguageDisplayName(selectedLanguage);
        guiGraphics.drawCenteredString(this.font, currentLangText, this.width / 2, 35, 0xCCCCCC);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}