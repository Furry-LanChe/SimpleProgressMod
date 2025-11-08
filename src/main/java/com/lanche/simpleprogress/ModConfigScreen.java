package com.lanche.simpleprogress;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModConfigScreen extends Screen {
    private final Screen parent;

    public ModConfigScreen(Screen parent) {
        super(Component.literal("Simple Progress 设置"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // 添加打开进度管理器的按钮
        this.addRenderableWidget(Button.builder(
                Component.literal("打开进度管理器"),
                button -> {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.setScreen(new ProgressJournalScreen(
                                new ProgressJournalMenu(0, this.minecraft.player.getInventory()),
                                this.minecraft.player.getInventory(),
                                Component.literal("进度管理器")
                        ));
                    }
                }
        ).pos(this.width / 2 - 100, this.height / 4 + 48).size(200, 20).build());

        // 帮助按钮
        this.addRenderableWidget(Button.builder(
                Component.literal("使用帮助"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new HelpScreen(this));
                    }
                }
        ).pos(this.width / 2 - 100, this.height / 4 + 72).size(200, 20).build());

        // 返回按钮
        this.addRenderableWidget(Button.builder(
                Component.literal("返回"),
                button -> this.minecraft.setScreen(this.parent)
        ).pos(this.width / 2 - 100, this.height / 4 + 120).size(200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, "Simple Progress Mod", this.width / 2, 40, 0xCCCCCC);
        guiGraphics.drawCenteredString(this.font, "版本 1.0.2", this.width / 2, 55, 0xCCCCCC);
        guiGraphics.drawCenteredString(this.font, "新增建筑和附魔进度类型", this.width / 2, 70, 0xCCCCCC);
        guiGraphics.drawCenteredString(this.font, "支持子进度和树状图统计", this.width / 2, 85, 0xCCCCCC);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}