package com.lanche.simpleprogress;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModConfigScreen extends Screen {
    private final Screen parent;

    public ModConfigScreen(Screen parent) {
        super(Component.literal(LanguageManager.getTranslation("simpleprogress.gui.settings")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(
                Component.literal(LanguageManager.getTranslation("simpleprogress.gui.open_progress_manager")),
                button -> {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.setScreen(new ProgressJournalScreen(
                                new ProgressJournalMenu(0, this.minecraft.player.getInventory()),
                                this.minecraft.player.getInventory(),
                                Component.literal(LanguageManager.getTranslation("simpleprogress.gui.progress_manager"))
                        ));
                    }
                }
        ).pos(this.width / 2 - 100, this.height / 4 + 48).size(200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(LanguageManager.getTranslation("simpleprogress.gui.language_settings")),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new LanguageSettingsScreen(this));
                    }
                }
        ).pos(this.width / 2 - 100, this.height / 4 + 72).size(200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(LanguageManager.getTranslation("simpleprogress.gui.help")),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new HelpScreen(this));
                    }
                }
        ).pos(this.width / 2 - 100, this.height / 4 + 96).size(200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(LanguageManager.getTranslation("simpleprogress.gui.back")),
                button -> this.minecraft.setScreen(this.parent)
        ).pos(this.width / 2 - 100, this.height / 4 + 120).size(200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, "Simple Progress Mod", this.width / 2, 40, 0xCCCCCC);
        guiGraphics.drawCenteredString(this.font, LanguageManager.getTranslation("simpleprogress.version", "1.0.4"), this.width / 2, 55, 0xCCCCCC);
        guiGraphics.drawCenteredString(this.font, LanguageManager.getTranslation("simpleprogress.feature.build_enchant"), this.width / 2, 70, 0xCCCCCC);
        guiGraphics.drawCenteredString(this.font, LanguageManager.getTranslation("simpleprogress.feature.subprogress"), this.width / 2, 85, 0xCCCCCC);
        guiGraphics.drawCenteredString(this.font, LanguageManager.getTranslation("simpleprogress.feature.multilanguage"), this.width / 2, 100, 0xCCCCCC);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}