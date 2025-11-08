package com.lanche.simpleprogress;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ProgressJournalScreen extends AbstractContainerScreen<ProgressJournalMenu> {
    private static final int GUI_WIDTH = 500;
    private static final int GUI_HEIGHT = 330;

    private List<ProgressManager.CustomProgress> progressList = new ArrayList<>();
    private ProgressManager.ProgressType selectedType = ProgressManager.ProgressType.KILL;
    private EditBox titleField;
    private EditBox targetField;
    private EditBox countField;
    private int selectedProgressIndex = -1;
    private int scrollOffset = 0;
    private boolean isDraggingScroll = false;
    private static final int SCROLLBAR_WIDTH = 8;
    private int currentTheme = 0;
    private boolean showStatistics = false;
    private boolean confirmClear = false;
    private long clearConfirmTime = 0;
    private String selectedParentId = null;
    private boolean showSubProgress = false;

    public ProgressJournalScreen(@Nonnull ProgressJournalMenu menu, @Nonnull Inventory playerInventory, @Nonnull Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        // 进度类型选择按钮
        int buttonX = leftPos + 25;
        int buttonY = topPos + 45;
        int buttonWidth = 55;
        int buttonHeight = 18;

        // 第一行：击杀、获得、探索
        for (int i = 0; i < 3; i++) {
            ProgressManager.ProgressType type = ProgressManager.ProgressType.values()[i];
            this.addRenderableWidget(Button.builder(
                    Component.literal(type.getDisplayName()),
                    button -> {
                        selectedType = type;
                        updateTargetFieldHint();
                    }
            ).pos(buttonX, buttonY).size(buttonWidth, buttonHeight).build());
            buttonX += buttonWidth + 5;
        }

        // 第二行：建筑、附魔
        buttonX = leftPos + 25;
        buttonY += buttonHeight + 5;
        for (int i = 3; i < ProgressManager.ProgressType.values().length; i++) {
            ProgressManager.ProgressType type = ProgressManager.ProgressType.values()[i];
            this.addRenderableWidget(Button.builder(
                    Component.literal(type.getDisplayName()),
                    button -> {
                        selectedType = type;
                        updateTargetFieldHint();
                    }
            ).pos(buttonX, buttonY).size(buttonWidth, buttonHeight).build());
            buttonX += buttonWidth + 5;
        }

        // 输入框位置
        int inputY = topPos + 106;
        titleField = new EditBox(font, leftPos + 25, inputY, 180, 18, Component.literal("标题"));
        titleField.setMaxLength(32);
        titleField.setValue("新进度");
        this.addRenderableWidget(titleField);

        targetField = new EditBox(font, leftPos + 25, inputY + 33, 180, 18, Component.literal("目标"));
        targetField.setMaxLength(64);
        updateTargetFieldHint();
        this.addRenderableWidget(targetField);

        countField = new EditBox(font, leftPos + 25, inputY + 68, 180, 18, Component.literal("数量"));
        countField.setMaxLength(5);
        countField.setValue("10");
        this.addRenderableWidget(countField);

        // 功能按钮
        int functionButtonY = inputY + 124;
        int functionButtonWidth = 60;
        int functionButtonHeight = 20;

        // 第一行功能按钮
        this.addRenderableWidget(Button.builder(
                Component.literal("添加进度"),
                button -> addProgress()
        ).pos(leftPos + 25, functionButtonY).size(functionButtonWidth, functionButtonHeight).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("删除进度"),
                button -> deleteSelectedProgress()
        ).pos(leftPos + 95, functionButtonY).size(functionButtonWidth, functionButtonHeight).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("添加子进度"),
                button -> addSubProgress()
        ).pos(leftPos + 165, functionButtonY).size(70, functionButtonHeight).build());

        // 第二行功能按钮
        this.addRenderableWidget(Button.builder(
                Component.literal("统计"),
                button -> showStatistics = !showStatistics
        ).pos(leftPos + 25, functionButtonY + 25).size(functionButtonWidth, functionButtonHeight).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("清除进度"),
                button -> showClearConfirmation()
        ).pos(leftPos + 95, functionButtonY + 25).size(functionButtonWidth, functionButtonHeight).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(showSubProgress ? "显示全部" : "显示子进度"),
                button -> toggleSubProgressView()
        ).pos(leftPos + 165, functionButtonY + 25).size(70, functionButtonHeight).build());

        // 第三行功能按钮
        this.addRenderableWidget(Button.builder(
                Component.literal("帮助"),
                button -> showHelpScreen()
        ).pos(leftPos + 25, functionButtonY + 50).size(functionButtonWidth, functionButtonHeight).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("主题"),
                button -> switchTheme()
        ).pos(leftPos + 95, functionButtonY + 50).size(functionButtonWidth, functionButtonHeight).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("关闭"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(null);
                    }
                }
        ).pos(leftPos + 165, functionButtonY + 50).size(70, functionButtonHeight).build());

        // 加载玩家进度
        reloadProgressList();
    }

    private void reloadProgressList() {
        if (minecraft != null && minecraft.player != null) {
            progressList = ProgressManager.getProgresses(minecraft.player);
            ProgressManager.PlayerStats stats = ProgressManager.getPlayerStats(minecraft.player);
            stats.totalProgresses = progressList.size();
        }
    }

    private void updateTargetFieldHint() {
        switch(selectedType) {
            case KILL:
                targetField.setValue("minecraft:zombie");
                break;
            case OBTAIN:
                targetField.setValue("minecraft:diamond");
                break;
            case EXPLORE:
                targetField.setValue("minecraft:overworld");
                break;
            case BUILD:
                targetField.setValue("minecraft:dirt");
                break;
            case ENCHANT:
                targetField.setValue("minecraft:sharpness");
                break;
        }
    }

    private void switchTheme() {
        currentTheme = (currentTheme + 1) % 6;
    }

    private void addProgress() {
        if (minecraft == null || minecraft.player == null) return;

        try {
            String title = titleField.getValue().trim();
            String target = targetField.getValue().trim();

            if (title.isEmpty()) {
                minecraft.player.displayClientMessage(Component.literal("§c请输入进度标题"), false);
                return;
            }

            if (target.isEmpty()) {
                minecraft.player.displayClientMessage(Component.literal("§c请输入目标ID"), false);
                return;
            }

            ProgressManager.CustomProgress progress = new ProgressManager.CustomProgress();
            progress.title = title;
            progress.description = selectedType.getDisplayName() + " " + target;
            progress.type = selectedType;
            progress.target = target;

            if (selectedType == ProgressManager.ProgressType.EXPLORE || selectedType == ProgressManager.ProgressType.ENCHANT) {
                progress.targetCount = 1;
            } else {
                progress.targetCount = Integer.parseInt(countField.getValue());
            }

            progress.current = 0;
            progress.completed = false;

            ProgressManager.addProgress(minecraft.player, progress);
            reloadProgressList();

            titleField.setValue("");
            countField.setValue("10");
            updateTargetFieldHint();

            minecraft.player.displayClientMessage(Component.literal("§a进度添加成功!"), false);

        } catch (NumberFormatException e) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal("§c数量必须是有效的数字"), false);
            }
        }
    }

    private void addSubProgress() {
        if (minecraft == null || minecraft.player == null) return;

        if (selectedProgressIndex == -1) {
            minecraft.player.displayClientMessage(Component.literal("§c请先选择一个父进度"), false);
            return;
        }

        try {
            String title = titleField.getValue().trim();
            String target = targetField.getValue().trim();

            if (title.isEmpty()) {
                minecraft.player.displayClientMessage(Component.literal("§c请输入子进度标题"), false);
                return;
            }

            if (target.isEmpty()) {
                minecraft.player.displayClientMessage(Component.literal("§c请输入目标ID"), false);
                return;
            }

            ProgressManager.CustomProgress parentProgress = progressList.get(selectedProgressIndex);
            ProgressManager.CustomProgress subProgress = new ProgressManager.CustomProgress();
            subProgress.title = title;
            subProgress.description = selectedType.getDisplayName() + " " + target;
            subProgress.type = selectedType;
            subProgress.target = target;

            if (selectedType == ProgressManager.ProgressType.EXPLORE || selectedType == ProgressManager.ProgressType.ENCHANT) {
                subProgress.targetCount = 1;
            } else {
                subProgress.targetCount = Integer.parseInt(countField.getValue());
            }

            subProgress.current = 0;
            subProgress.completed = false;

            ProgressManager.addSubProgress(minecraft.player, parentProgress.id, subProgress);
            ProgressManager.savePlayerProgress(minecraft.player);
            reloadProgressList();

            titleField.setValue("");
            countField.setValue("10");
            updateTargetFieldHint();

            minecraft.player.displayClientMessage(Component.literal("§a子进度添加成功!"), false);

        } catch (NumberFormatException e) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal("§c数量必须是有效的数字"), false);
            }
        }
    }

    private void deleteSelectedProgress() {
        if (minecraft != null && minecraft.player != null && selectedProgressIndex >= 0 && selectedProgressIndex < progressList.size()) {
            ProgressManager.CustomProgress progress = progressList.get(selectedProgressIndex);
            ProgressManager.removeProgress(minecraft.player, progress.id);
            reloadProgressList();
            selectedProgressIndex = -1;
            minecraft.player.displayClientMessage(Component.literal("§c进度已删除"), false);
        } else {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal("§c请先选择一个进度"), false);
            }
        }
    }

    private void toggleSubProgressView() {
        if (showSubProgress) {
            showSubProgress = false;
            selectedParentId = null;
            selectedProgressIndex = -1;
            reloadProgressList();
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal("§a显示全部进度"), false);
            }
        } else {
            if (selectedProgressIndex != -1) {
                ProgressManager.CustomProgress selectedProgress = progressList.get(selectedProgressIndex);

                reloadProgressList();

                boolean hasSubProgresses = false;
                for (ProgressManager.CustomProgress progress : progressList) {
                    if (selectedProgress.id.equals(progress.parentId)) {
                        hasSubProgresses = true;
                        break;
                    }
                }

                if (hasSubProgresses) {
                    showSubProgress = true;
                    selectedParentId = selectedProgress.id;
                    scrollOffset = 0;
                    if (minecraft != null && minecraft.player != null) {
                        minecraft.player.displayClientMessage(Component.literal("§a显示子进度: " + selectedProgress.title), false);
                    }
                } else {
                    if (minecraft != null && minecraft.player != null) {
                        minecraft.player.displayClientMessage(Component.literal("§c所选进度没有子进度，请先添加子进度"), false);
                    }
                }
            } else {
                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.displayClientMessage(Component.literal("§c请先选择一个父进度"), false);
                }
            }
        }

        init();
    }

    private void showClearConfirmation() {
        if (!confirmClear) {
            confirmClear = true;
            clearConfirmTime = System.currentTimeMillis();
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal("§c再次点击清除按钮确认清除所有进度"), false);
            }
        } else {
            confirmClear = false;
            clearAllProgresses();
        }
    }

    private void clearAllProgresses() {
        if (minecraft != null && minecraft.player != null) {
            ProgressManager.clearAllProgresses(minecraft.player);
            reloadProgressList();
            selectedProgressIndex = -1;
            minecraft.player.displayClientMessage(Component.literal("§a已清除所有进度"), false);
        }
    }

    private void showHelpScreen() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new HelpScreen(this));
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 根据主题绘制背景
        int bgColor = getThemeBackgroundColor();
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, bgColor);

        // 绘制标题区域背景
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 25, getThemeHeaderColor());

        // 绘制左侧输入区域背景
        guiGraphics.fill(leftPos + 20, topPos + 25, leftPos + 240, topPos + imageHeight - 20, getThemePanelColor());

        // 绘制右侧进度列表背景
        guiGraphics.fill(leftPos + 255, topPos + 25, leftPos + imageWidth - 20, topPos + imageHeight - 20, getThemePanelColor());

        if (showStatistics) {
            renderStatistics(guiGraphics);
        } else {
            renderProgressList(guiGraphics, mouseX, mouseY);
        }
    }

    private void renderStatistics(GuiGraphics guiGraphics) {
        if (minecraft == null || minecraft.player == null) return;

        try {
            ProgressManager.TreeChartData treeData = ProgressManager.getTreeChartData(minecraft.player);

            // 绘制统计标题
            guiGraphics.drawString(font, "§6进度树状图", leftPos + 260, topPos + 50, 0xFFFFFF, false);

            // 绘制总体统计信息
            ProgressManager.PlayerStats stats = ProgressManager.getPlayerStats(minecraft.player);
            String completionRate = String.format("总完成率: §e%.1f%%", stats.getCompletionRate() * 100);
            guiGraphics.drawString(font, completionRate, leftPos + 260, topPos + 65, 0xCCCCCC, false);
            guiGraphics.drawString(font, "总进度数: §e" + stats.totalProgresses, leftPos + 260, topPos + 77, 0xCCCCCC, false);
            guiGraphics.drawString(font, "已完成: §a" + stats.completedProgresses, leftPos + 260, topPos + 89, 0xCCCCCC, false);
            guiGraphics.drawString(font, "进行中: §6" + (stats.totalProgresses - stats.completedProgresses), leftPos + 260, topPos + 101, 0xCCCCCC, false);

            // 绘制树状图
            int startY = topPos + 120;
            for (ProgressManager.TreeNode node : treeData.nodes) {
                drawTreeNode(guiGraphics, node, leftPos + 260, startY, 0);
                startY += 20;
                if (startY > topPos + imageHeight - 30) break;
            }

            // 绘制清除按钮状态
            if (confirmClear) {
                long timeLeft = 3000 - (System.currentTimeMillis() - clearConfirmTime);
                if (timeLeft <= 0) {
                    confirmClear = false;
                } else {
                    String confirmText = "确认清除 (" + (timeLeft / 1000 + 1) + "s)";
                    guiGraphics.drawString(font, "§c" + confirmText, leftPos + 260, topPos + 290, 0xFFFFFF, false);
                }
            }
        } catch (Exception e) {
            guiGraphics.drawString(font, "§c统计界面加载失败", leftPos + 260, topPos + 50, 0xFFFFFF, false);
            SimpleProgress.LOGGER.error("统计界面渲染错误: {}", e.getMessage());
        }
    }

    private void drawTreeNode(GuiGraphics guiGraphics, ProgressManager.TreeNode node, int x, int y, int depth) {
        int indent = depth * 15;
        int nodeX = x + indent;

        // 绘制节点图标
        String icon = node.progress.completed ? "✓" : "○";
        int iconColor = node.progress.completed ? 0x00FF00 : 0xFFFFFF;
        guiGraphics.drawString(font, icon, nodeX, y, iconColor, false);

        // 绘制进度标题
        String title = node.progress.type.getColorCode() + node.progress.title;
        if (font.width(title) > 150 - indent) {
            title = font.plainSubstrByWidth(title, 150 - indent) + "...";
        }
        guiGraphics.drawString(font, title, nodeX + 10, y, 0xFFFFFF, false);

        // 绘制进度信息
        String info = "§7" + node.progress.current + "/" + node.progress.targetCount;
        guiGraphics.drawString(font, info, nodeX + 160 - indent, y, 0xCCCCCC, false);

        // 绘制子节点
        int childY = y + 15;
        for (ProgressManager.TreeNode child : node.children) {
            drawTreeNode(guiGraphics, child, x, childY, depth + 1);
            childY += 15;
        }
    }

    private void renderProgressList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<ProgressManager.CustomProgress> displayList = getDisplayProgressList();
        int listStartY = topPos + 50;
        int visibleSlots = 11;
        int listContentWidth = 220 - SCROLLBAR_WIDTH;

        for (int i = 0; i < visibleSlots; i++) {
            int listIndex = i + scrollOffset;
            if (listIndex >= displayList.size()) break;

            ProgressManager.CustomProgress progress = displayList.get(listIndex);
            int entryY = listStartY + i * 22;

            // 绘制进度条目背景
            int bgColor = (listIndex == selectedProgressIndex) ? 0x88666666 : 0x88555555;
            guiGraphics.fill(leftPos + 255, entryY, leftPos + 255 + listContentWidth, entryY + 20, bgColor);

            // 绘制进度条
            int progressWidth = (int) (listContentWidth * progress.getProgress());
            int progressColor = progress.completed ? 0x88FFAA00 : getProgressColor(progress.type);
            guiGraphics.fill(leftPos + 255, entryY, leftPos + 255 + progressWidth, entryY + 20, progressColor);

            // 绘制进度文本
            String prefix = progress.parentId != null ? "  ↳ " : "";
            String displayTitle = progress.type.getColorCode() + prefix + progress.title;
            int maxTextWidth = listContentWidth - 50;

            if (font.width(displayTitle) > maxTextWidth) {
                displayTitle = font.plainSubstrByWidth(displayTitle, maxTextWidth) + "...";
            }

            guiGraphics.drawString(font, displayTitle, leftPos + 270, entryY + 6, 0xFFFFFF, false);

            // 如果有子进度，显示子进度图标
            if (progress.hasSubProgresses()) {
                guiGraphics.drawString(font, "📁", leftPos + 255 + listContentWidth - 40, entryY + 6, 0xFFFFFF, false);
            }

            // 绘制进度数字
            String progressText = progress.current + "/" + progress.targetCount;
            int progressTextWidth = font.width(progressText);
            guiGraphics.drawString(font, progressText, leftPos + 255 + listContentWidth - progressTextWidth - 5, entryY + 6,
                    progress.completed ? 0x00FF00 : 0xFFFF00, false);
        }

        // 绘制滚动条
        if (isContentScrollable(displayList)) {
            drawScrollbar(guiGraphics, mouseX, mouseY, displayList.size());
        }
    }

    private List<ProgressManager.CustomProgress> getDisplayProgressList() {
        List<ProgressManager.CustomProgress> result = new ArrayList<>();

        if (showSubProgress) {
            if (selectedParentId != null) {
                for (ProgressManager.CustomProgress progress : progressList) {
                    if (selectedParentId.equals(progress.parentId)) {
                        result.add(progress);
                    }
                }
            } else {
                for (ProgressManager.CustomProgress progress : progressList) {
                    if (progress.parentId != null) {
                        result.add(progress);
                    }
                }
            }
        } else {
            for (ProgressManager.CustomProgress progress : progressList) {
                if (progress.parentId == null) {
                    result.add(progress);
                }
            }
        }

        return result;
    }

    private boolean isContentScrollable(List<ProgressManager.CustomProgress> displayList) {
        return displayList.size() > 11;
    }

    private void drawScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY, int totalEntries) {
        int scrollbarX = leftPos + 255 + (220 - SCROLLBAR_WIDTH);
        int scrollbarY = topPos + 50;
        int scrollbarHeight = 11 * 22;

        // 绘制滚动条背景
        guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight, 0xFF555555);

        // 计算滚动条滑块
        int visibleEntries = 11;
        int scrollbarThumbHeight = Math.max(20, scrollbarHeight * visibleEntries / totalEntries);
        int maxScroll = Math.max(0, (totalEntries - visibleEntries) * 22);
        int scrollProgress = maxScroll > 0 ? (int) ((float) scrollOffset / maxScroll * (scrollbarHeight - scrollbarThumbHeight)) : 0;

        int thumbY = scrollbarY + scrollProgress;

        // 绘制滚动条滑块
        boolean isHovered = isMouseOverScrollbar(mouseX, mouseY);
        int thumbColor = isHovered || isDraggingScroll ? 0xFF8888CC : 0xFF666699;
        guiGraphics.fill(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + scrollbarThumbHeight, thumbColor);
    }

    private boolean isMouseOverScrollbar(int mouseX, int mouseY) {
        int scrollbarX = leftPos + 255 + (220 - SCROLLBAR_WIDTH);
        int scrollbarY = topPos + 50;
        int scrollbarHeight = 11 * 22;

        return mouseX >= scrollbarX &&
                mouseX <= scrollbarX + SCROLLBAR_WIDTH &&
                mouseY >= scrollbarY &&
                mouseY <= scrollbarY + scrollbarHeight;
    }

    private int getProgressColor(ProgressManager.ProgressType type) {
        switch(type) {
            case KILL: return 0x88FF0000;
            case OBTAIN: return 0x8800FF00;
            case EXPLORE: return 0x880000FF;
            case BUILD: return 0x88FFAA00;
            case ENCHANT: return 0x88AA00FF;
            default: return 0x8800FF00;
        }
    }

    private int getThemeBackgroundColor() {
        switch(currentTheme) {
            case 0: return 0xCC000000;
            case 1: return 0xCC111111;
            case 2: return 0xCCAAAAAA;
            case 3: return 0xCC002200;
            case 4: return 0xCC000022;
            case 5: return 0xCC220022;
            default: return 0xCC000000;
        }
    }

    private int getThemeHeaderColor() {
        switch(currentTheme) {
            case 0: return 0xFF333333;
            case 1: return 0xFF222222;
            case 2: return 0xFF666666;
            case 3: return 0xFF006600;
            case 4: return 0xFF000066;
            case 5: return 0xFF660066;
            default: return 0xFF333333;
        }
    }

    private int getThemePanelColor() {
        switch(currentTheme) {
            case 0: return 0x66444444;
            case 1: return 0x66333333;
            case 2: return 0x66888888;
            case 3: return 0x66446644;
            case 4: return 0x66444466;
            case 5: return 0x66664466;
            default: return 0x66444444;
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 绘制标题
        int titleX = (int) ((imageWidth - font.width("进度管理器")) / 2.0f);
        guiGraphics.drawString(font, "进度管理器", titleX, 8, 0xFFFFFF, false);

        // 绘制左侧标签
        guiGraphics.drawString(font, "类型:", 25, 32, 0xCCCCCC, false);
        guiGraphics.drawString(font, "标题:", 25, 93, 0xCCCCCC, false);
        guiGraphics.drawString(font, "目标:", 25, 128, 0xCCCCCC, false);
        guiGraphics.drawString(font, "数量:", 25, 163, 0xCCCCCC, false);

        // 绘制右侧标签
        List<ProgressManager.CustomProgress> displayList = getDisplayProgressList();
        String listTitle = showStatistics ? "进度统计" :
                showSubProgress ? "子进度列表 (" + displayList.size() + ")" :
                        "进度列表 (" + displayList.size() + ")";
        guiGraphics.drawString(font, listTitle, 270, 37, 0xFFFFFF, false);

        // 绘制当前类型显示
        String typeLabel = "当前类型: " + selectedType.getColorCode() + selectedType.getDisplayName();
        guiGraphics.drawString(font, typeLabel, 25, 205, 0xCCCCCC, false);

        // 绘制当前主题显示
        String themeLabel = "当前主题: " + getThemeDisplayName();
        guiGraphics.drawString(font, themeLabel, 25, 220, 0xCCCCCC, false);
    }

    private String getThemeDisplayName() {
        switch(currentTheme) {
            case 0: return "默认";
            case 1: return "暗色";
            case 2: return "亮色";
            case 3: return "绿色";
            case 4: return "蓝色";
            case 5: return "紫色";
            default: return "默认";
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltips(guiGraphics, mouseX, mouseY);

        if (confirmClear && System.currentTimeMillis() - clearConfirmTime > 3000) {
            confirmClear = false;
        }
    }

    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 简化的工具提示
        if (isMouseOver(mouseX, mouseY, leftPos + 25, topPos + 128, 180, 18)) {
            String hint = getTargetFieldHint();
            guiGraphics.renderTooltip(font, Component.literal(hint), mouseX, mouseY);
        }

        // 进度条目工具提示
        if (!showStatistics) {
            List<ProgressManager.CustomProgress> displayList = getDisplayProgressList();
            int listStartY = topPos + 50;
            for (int i = 0; i < 11; i++) {
                int listIndex = i + scrollOffset;
                if (listIndex >= displayList.size()) break;

                ProgressManager.CustomProgress progress = displayList.get(listIndex);
                int entryY = listStartY + i * 22;

                if (isMouseOver(mouseX, mouseY, leftPos + 255, entryY, 220 - SCROLLBAR_WIDTH, 20)) {
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(Component.literal(progress.type.getColorCode() + "【" + progress.type.getDisplayName() + "】" + progress.title));
                    tooltip.add(Component.literal("§7目标: §e" + progress.target));
                    tooltip.add(Component.literal("§7进度: §e" + progress.current + "§7/§e" + progress.targetCount));
                    tooltip.add(Component.literal("§7完成度: §e" + String.format("%.1f", progress.getProgress() * 100) + "%"));
                    if (progress.hasSubProgresses()) {
                        tooltip.add(Component.literal("§7子进度数量: §e" + progress.subProgresses.size()));
                    }
                    if (!progress.completed) {
                        tooltip.add(Component.literal("§7预估剩余时间: §e" + progress.getEstimatedTime()));
                    }
                    guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
                    break;
                }
            }
        }
    }

    private String getTargetFieldHint() {
        switch(selectedType) {
            case KILL: return "§e输入生物ID\n§7例如: minecraft:zombie";
            case OBTAIN: return "§e输入物品ID\n§7例如: minecraft:diamond";
            case EXPLORE: return "§e输入维度ID\n§7例如: minecraft:the_nether";
            case BUILD: return "§e输入方块ID\n§7例如: minecraft:dirt";
            case ENCHANT: return "§e输入附魔ID\n§7例如: minecraft:sharpness";
            default: return "§e输入目标ID";
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOverScrollbar((int)mouseX, (int)mouseY)) {
            isDraggingScroll = true;

            int scrollbarY = topPos + 50;
            int scrollbarHeight = 11 * 22;
            List<ProgressManager.CustomProgress> displayList = getDisplayProgressList();
            int totalEntries = displayList.size();
            int visibleEntries = 11;
            int maxScroll = Math.max(0, (totalEntries - visibleEntries) * 22);

            double scrollPercent = (mouseY - scrollbarY) / (double)scrollbarHeight;
            scrollOffset = (int)(maxScroll * Math.max(0, Math.min(1, scrollPercent)));

            return true;
        }

        List<ProgressManager.CustomProgress> displayList = getDisplayProgressList();
        int listStartY = topPos + 50;
        for (int i = 0; i < 11; i++) {
            int listIndex = i + scrollOffset;
            if (listIndex >= displayList.size()) break;

            ProgressManager.CustomProgress progress = displayList.get(listIndex);
            int entryY = listStartY + i * 22;

            if (isMouseOver(mouseX, mouseY, leftPos + 255, entryY, 220 - SCROLLBAR_WIDTH, 20)) {
                selectedProgressIndex = listIndex;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDraggingScroll) {
            isDraggingScroll = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScroll) {
            int scrollbarY = topPos + 50;
            int scrollbarHeight = 11 * 22;
            List<ProgressManager.CustomProgress> displayList = getDisplayProgressList();
            int totalEntries = displayList.size();
            int visibleEntries = 11;
            int maxScroll = Math.max(0, (totalEntries - visibleEntries) * 22);

            double scrollPercent = (mouseY - scrollbarY) / (double)scrollbarHeight;
            scrollOffset = (int)(maxScroll * Math.max(0, Math.min(1, scrollPercent)));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isMouseOver(mouseX, mouseY, leftPos + 250, topPos + 25, 230, 280)) {
            List<ProgressManager.CustomProgress> displayList = getDisplayProgressList();
            int maxScroll = Math.max(0, displayList.size() - 11);
            if (delta > 0) {
                if (scrollOffset > 0) scrollOffset--;
            } else if (delta < 0) {
                if (scrollOffset < maxScroll) scrollOffset++;
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}