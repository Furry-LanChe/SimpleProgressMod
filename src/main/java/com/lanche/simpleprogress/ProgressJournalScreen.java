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
    private static final int GUI_HEIGHT = 360;

    private List<ProgressManager.CustomProgress> allProgresses = new ArrayList<>();
    private List<ProgressManager.CustomProgress> displayProgresses = new ArrayList<>();
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

        int inputY = topPos + 106;
        titleField = new EditBox(font, leftPos + 25, inputY, 180, 18,
                Component.literal(LanguageManager.getTranslation("simpleprogress.ui.title")));
        titleField.setMaxLength(32);
        titleField.setValue("");
        this.addRenderableWidget(titleField);

        targetField = new EditBox(font, leftPos + 25, inputY + 33, 180, 18,
                Component.literal(LanguageManager.getTranslation("simpleprogress.ui.target")));
        targetField.setMaxLength(64);
        updateTargetFieldHint();
        this.addRenderableWidget(targetField);

        countField = new EditBox(font, leftPos + 25, inputY + 68, 180, 18,
                Component.literal(LanguageManager.getTranslation("simpleprogress.ui.count")));
        countField.setMaxLength(5);
        countField.setValue("10");
        this.addRenderableWidget(countField);

        int functionButtonY = inputY + 124;
        int functionButtonWidth = 60;
        int functionButtonHeight = 20;

        this.addRenderableWidget(Button.builder(
                Component.literal(LanguageManager.getTranslation("simpleprogress.ui.add_progress")),
                button -> addProgress()
        ).pos(leftPos + 25, functionButtonY).size(functionButtonWidth, functionButtonHeight).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(LanguageManager.getTranslation("simpleprogress.ui.delete_progress")),
                button -> deleteSelectedProgress()
        ).pos(leftPos + 95, functionButtonY).size(functionButtonWidth, functionButtonHeight).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(LanguageManager.getTranslation("simpleprogress.ui.add_subprogress")),
                button -> addSubProgress()
        ).pos(leftPos + 165, functionButtonY).size(70, functionButtonHeight).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(LanguageManager.getTranslation("simpleprogress.ui.statistics")),
                button -> showStatistics = !showStatistics
        ).pos(leftPos + 25, functionButtonY + 25).size(functionButtonWidth, functionButtonHeight).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(LanguageManager.getTranslation("simpleprogress.ui.clear_all")),
                button -> showClearConfirmation()
        ).pos(leftPos + 95, functionButtonY + 25).size(functionButtonWidth, functionButtonHeight).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(showSubProgress ?
                        LanguageManager.getTranslation("simpleprogress.ui.show_all") :
                        LanguageManager.getTranslation("simpleprogress.ui.show_subprogress")),
                button -> toggleSubProgressView()
        ).pos(leftPos + 165, functionButtonY + 25).size(70, functionButtonHeight).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(LanguageManager.getTranslation("simpleprogress.gui.help")),
                button -> showHelpScreen()
        ).pos(leftPos + 25, functionButtonY + 50).size(functionButtonWidth, functionButtonHeight).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(LanguageManager.getTranslation("simpleprogress.ui.theme")),
                button -> switchTheme()
        ).pos(leftPos + 95, functionButtonY + 50).size(functionButtonWidth, functionButtonHeight).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(LanguageManager.getTranslation("simpleprogress.gui.close")),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(null);
                    }
                }
        ).pos(leftPos + 165, functionButtonY + 50).size(70, functionButtonHeight).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("🌐 " + LanguageManager.getLanguageDisplayName(LanguageManager.getCurrentLanguage())),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new LanguageSettingsScreen(this));
                    }
                }
        ).pos(leftPos + 25, functionButtonY + 75).size(210, functionButtonHeight).build());

        reloadProgressList();
    }

    private void reloadProgressList() {
        if (minecraft != null && minecraft.player != null) {
            allProgresses = ProgressManager.getProgresses(minecraft.player);
            updateDisplayProgresses();
            ProgressManager.PlayerStats stats = ProgressManager.getPlayerStats(minecraft.player);
            stats.totalProgresses = getAllProgressCount(allProgresses);
        }
    }

    private int getAllProgressCount(List<ProgressManager.CustomProgress> progresses) {
        int count = progresses.size();
        for (ProgressManager.CustomProgress progress : progresses) {
            if (progress.subProgresses != null && !progress.subProgresses.isEmpty()) {
                count += getAllProgressCount(progress.subProgresses);
            }
        }
        return count;
    }

    private void updateDisplayProgresses() {
        displayProgresses.clear();

        if (showSubProgress && selectedParentId != null) {
            ProgressManager.CustomProgress parentProgress = findProgressById(allProgresses, selectedParentId);
            if (parentProgress != null && parentProgress.subProgresses != null) {
                displayProgresses.addAll(parentProgress.subProgresses);
            }
        } else {
            for (ProgressManager.CustomProgress progress : allProgresses) {
                if (progress.parentId == null) {
                    displayProgresses.add(progress);
                }
            }
        }
    }

    private ProgressManager.CustomProgress findProgressById(List<ProgressManager.CustomProgress> progresses, String id) {
        for (ProgressManager.CustomProgress progress : progresses) {
            if (progress.id.equals(id)) {
                return progress;
            }
            if (progress.subProgresses != null && !progress.subProgresses.isEmpty()) {
                ProgressManager.CustomProgress found = findProgressById(progress.subProgresses, id);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
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
                minecraft.player.displayClientMessage(Component.literal(LanguageManager.getTranslation("simpleprogress.message.enter_title")), false);
                return;
            }

            if (target.isEmpty()) {
                minecraft.player.displayClientMessage(Component.literal(LanguageManager.getTranslation("simpleprogress.message.enter_target")), false);
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

            minecraft.player.displayClientMessage(Component.literal(LanguageManager.getTranslation("simpleprogress.message.progress_added")), false);

        } catch (NumberFormatException e) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal(LanguageManager.getTranslation("simpleprogress.message.invalid_number")), false);
            }
        }
    }

    private void addSubProgress() {
        if (minecraft == null || minecraft.player == null) return;

        if (selectedProgressIndex == -1) {
            minecraft.player.displayClientMessage(Component.literal(LanguageManager.getTranslation("simpleprogress.message.select_parent")), false);
            return;
        }

        try {
            String title = titleField.getValue().trim();
            String target = targetField.getValue().trim();

            if (title.isEmpty()) {
                minecraft.player.displayClientMessage(Component.literal(LanguageManager.getTranslation("simpleprogress.message.enter_title")), false);
                return;
            }

            if (target.isEmpty()) {
                minecraft.player.displayClientMessage(Component.literal(LanguageManager.getTranslation("simpleprogress.message.enter_target")), false);
                return;
            }

            ProgressManager.CustomProgress parentProgress = getSelectedProgress();
            if (parentProgress == null) {
                minecraft.player.displayClientMessage(Component.literal(LanguageManager.getTranslation("simpleprogress.message.invalid_parent")), false);
                return;
            }

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
            reloadProgressList();

            titleField.setValue("");
            countField.setValue("10");
            updateTargetFieldHint();

            minecraft.player.displayClientMessage(Component.literal(LanguageManager.getTranslation("simpleprogress.message.subprogress_added")), false);

        } catch (NumberFormatException e) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal(LanguageManager.getTranslation("simpleprogress.message.invalid_number")), false);
            }
        }
    }

    private ProgressManager.CustomProgress getSelectedProgress() {
        if (selectedProgressIndex >= 0 && selectedProgressIndex < displayProgresses.size()) {
            return displayProgresses.get(selectedProgressIndex);
        }
        return null;
    }

    private void deleteSelectedProgress() {
        if (minecraft != null && minecraft.player != null) {
            ProgressManager.CustomProgress selectedProgress = getSelectedProgress();
            if (selectedProgress != null) {
                ProgressManager.removeProgress(minecraft.player, selectedProgress.id);
                reloadProgressList();
                selectedProgressIndex = -1;
                minecraft.player.displayClientMessage(Component.literal(LanguageManager.getTranslation("simpleprogress.message.progress_deleted")), false);
            } else {
                minecraft.player.displayClientMessage(Component.literal(LanguageManager.getTranslation("simpleprogress.message.select_progress")), false);
            }
        }
    }

    private void toggleSubProgressView() {
        if (showSubProgress) {
            showSubProgress = false;
            selectedParentId = null;
            selectedProgressIndex = -1;
            updateDisplayProgresses();
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal(LanguageManager.getTranslation("simpleprogress.message.show_all")), false);
            }
        } else {
            ProgressManager.CustomProgress selectedProgress = getSelectedProgress();
            if (selectedProgress != null) {
                boolean hasSubProgresses = selectedProgress.hasSubProgresses();

                if (hasSubProgresses) {
                    showSubProgress = true;
                    selectedParentId = selectedProgress.id;
                    updateDisplayProgresses();
                    scrollOffset = 0;
                    if (minecraft != null && minecraft.player != null) {
                        minecraft.player.displayClientMessage(Component.literal(LanguageManager.getTranslation("simpleprogress.message.show_subprogress", selectedProgress.title)), false);
                    }
                } else {
                    if (minecraft != null && minecraft.player != null) {
                        minecraft.player.displayClientMessage(Component.literal(LanguageManager.getTranslation("simpleprogress.message.no_subprogress")), false);
                    }
                }
            } else {
                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.displayClientMessage(Component.literal(LanguageManager.getTranslation("simpleprogress.message.select_parent")), false);
                }
            }
        }
    }

    private void showClearConfirmation() {
        if (!confirmClear) {
            confirmClear = true;
            clearConfirmTime = System.currentTimeMillis();
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal(LanguageManager.getTranslation("simpleprogress.message.confirm_clear")), false);
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
            minecraft.player.displayClientMessage(Component.literal(LanguageManager.getTranslation("simpleprogress.message.all_cleared")), false);
        }
    }

    private void showHelpScreen() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new HelpScreen(this));
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int bgColor = getThemeBackgroundColor();
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, bgColor);

        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 25, getThemeHeaderColor());

        guiGraphics.fill(leftPos + 20, topPos + 25, leftPos + 240, topPos + imageHeight - 20, getThemePanelColor());

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

            guiGraphics.drawString(font, LanguageManager.getTranslation("simpleprogress.stats.tree_chart"), leftPos + 260, topPos + 50, 0xFFFFFF, false);

            ProgressManager.PlayerStats stats = ProgressManager.getPlayerStats(minecraft.player);
            String completionRate = LanguageManager.getTranslation("simpleprogress.stats.completion_rate", stats.getCompletionRate() * 100);
            guiGraphics.drawString(font, completionRate, leftPos + 260, topPos + 65, 0xCCCCCC, false);
            guiGraphics.drawString(font, LanguageManager.getTranslation("simpleprogress.stats.total_progress", stats.totalProgresses), leftPos + 260, topPos + 77, 0xCCCCCC, false);
            guiGraphics.drawString(font, LanguageManager.getTranslation("simpleprogress.stats.completed", stats.completedProgresses), leftPos + 260, topPos + 89, 0xCCCCCC, false);
            guiGraphics.drawString(font, LanguageManager.getTranslation("simpleprogress.stats.in_progress", (stats.totalProgresses - stats.completedProgresses)), leftPos + 260, topPos + 101, 0xCCCCCC, false);

            int startY = topPos + 120;
            for (ProgressManager.TreeNode node : treeData.nodes) {
                drawTreeNode(guiGraphics, node, leftPos + 260, startY, 0);
                startY += 20;
                if (startY > topPos + imageHeight - 50) break;
            }

            if (confirmClear) {
                long timeLeft = 3000 - (System.currentTimeMillis() - clearConfirmTime);
                if (timeLeft <= 0) {
                    confirmClear = false;
                } else {
                    String confirmText = LanguageManager.getTranslation("simpleprogress.ui.confirm_clear", (timeLeft / 1000 + 1));
                    guiGraphics.drawString(font, "§c" + confirmText, leftPos + 260, topPos + imageHeight - 60, 0xFFFFFF, false);
                }
            }
        } catch (Exception e) {
            guiGraphics.drawString(font, LanguageManager.getTranslation("simpleprogress.message.stats_error"), leftPos + 260, topPos + 50, 0xFFFFFF, false);
            SimpleProgress.LOGGER.error("统计界面渲染错误: {}", e.getMessage());
        }
    }

    private void drawTreeNode(GuiGraphics guiGraphics, ProgressManager.TreeNode node, int x, int y, int depth) {
        int indent = depth * 15;
        int nodeX = x + indent;

        String icon = node.progress.completed ? "✓" : "○";
        int iconColor = node.progress.completed ? 0x00FF00 : 0xFFFFFF;
        guiGraphics.drawString(font, icon, nodeX, y, iconColor, false);

        String title = node.progress.type.getColorCode() + node.progress.title;
        if (font.width(title) > 150 - indent) {
            title = font.plainSubstrByWidth(title, 150 - indent) + "...";
        }
        guiGraphics.drawString(font, title, nodeX + 10, y, 0xFFFFFF, false);

        String info = "§7" + node.progress.current + "/" + node.progress.targetCount;
        guiGraphics.drawString(font, info, nodeX + 160 - indent, y, 0xCCCCCC, false);

        int childY = y + 15;
        for (ProgressManager.TreeNode child : node.children) {
            drawTreeNode(guiGraphics, child, x, childY, depth + 1);
            childY += 15;
        }
    }

    private void renderProgressList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int listStartY = topPos + 50;
        int visibleSlots = 14;
        int listContentWidth = 220 - SCROLLBAR_WIDTH;

        for (int i = 0; i < visibleSlots; i++) {
            int listIndex = i + scrollOffset;
            if (listIndex >= displayProgresses.size()) break;

            ProgressManager.CustomProgress progress = displayProgresses.get(listIndex);
            int entryY = listStartY + i * 22;

            int bgColor = (listIndex == selectedProgressIndex) ? 0x88666666 : 0x88555555;
            guiGraphics.fill(leftPos + 255, entryY, leftPos + 255 + listContentWidth, entryY + 20, bgColor);

            int progressWidth = (int) (listContentWidth * progress.getProgress());
            int progressColor = progress.completed ? 0x88FFAA00 : getProgressColor(progress.type);
            guiGraphics.fill(leftPos + 255, entryY, leftPos + 255 + progressWidth, entryY + 20, progressColor);

            String prefix = progress.parentId != null ? "  ↳ " : "";
            String displayTitle = progress.type.getColorCode() + prefix + progress.title;
            int maxTextWidth = listContentWidth - 50;

            if (font.width(displayTitle) > maxTextWidth) {
                displayTitle = font.plainSubstrByWidth(displayTitle, maxTextWidth) + "...";
            }

            guiGraphics.drawString(font, displayTitle, leftPos + 270, entryY + 6, 0xFFFFFF, false);

            if (progress.hasSubProgresses()) {
                guiGraphics.drawString(font, "📁", leftPos + 255 + listContentWidth - 40, entryY + 6, 0xFFFFFF, false);
            }

            String progressText = progress.current + "/" + progress.targetCount;
            int progressTextWidth = font.width(progressText);
            guiGraphics.drawString(font, progressText, leftPos + 255 + listContentWidth - progressTextWidth - 5, entryY + 6,
                    progress.completed ? 0x00FF00 : 0xFFFF00, false);
        }

        if (isContentScrollable()) {
            drawScrollbar(guiGraphics, mouseX, mouseY);
        }
    }

    private boolean isContentScrollable() {
        return displayProgresses.size() > 14;
    }

    private void drawScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int scrollbarX = leftPos + 255 + (220 - SCROLLBAR_WIDTH);
        int scrollbarY = topPos + 50;
        int scrollbarHeight = 14 * 22;

        guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight, 0xFF555555);

        if (isContentScrollable()) {
            int visibleEntries = 14;
            int scrollbarThumbHeight = Math.max(20, scrollbarHeight * visibleEntries / displayProgresses.size());
            int maxScroll = Math.max(0, (displayProgresses.size() - visibleEntries) * 22);
            int scrollProgress = maxScroll > 0 ? (int) ((float) scrollOffset / maxScroll * (scrollbarHeight - scrollbarThumbHeight)) : 0;

            int thumbY = scrollbarY + scrollProgress;

            boolean isHovered = isMouseOverScrollbar(mouseX, mouseY);
            int thumbColor = isHovered || isDraggingScroll ? 0xFF8888CC : 0xFF666699;
            guiGraphics.fill(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + scrollbarThumbHeight, thumbColor);
        }
    }

    private boolean isMouseOverScrollbar(int mouseX, int mouseY) {
        int scrollbarX = leftPos + 255 + (220 - SCROLLBAR_WIDTH);
        int scrollbarY = topPos + 50;
        int scrollbarHeight = 14 * 22;

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
        String titleText = LanguageManager.getTranslation("simpleprogress.gui.progress_manager");
        int titleX = (int) ((imageWidth - font.width(titleText)) / 2.0f);
        guiGraphics.drawString(font, titleText, titleX, 8, 0xFFFFFF, false);

        guiGraphics.drawString(font, LanguageManager.getTranslation("simpleprogress.ui.type") + ":", 25, 32, 0xCCCCCC, false);
        guiGraphics.drawString(font, LanguageManager.getTranslation("simpleprogress.ui.title") + ":", 25, 93, 0xCCCCCC, false);
        guiGraphics.drawString(font, LanguageManager.getTranslation("simpleprogress.ui.target") + ":", 25, 128, 0xCCCCCC, false);
        guiGraphics.drawString(font, LanguageManager.getTranslation("simpleprogress.ui.count") + ":", 25, 163, 0xCCCCCC, false);

        String listTitle = showStatistics ? LanguageManager.getTranslation("simpleprogress.ui.statistics") :
                showSubProgress ? LanguageManager.getTranslation("simpleprogress.ui.subprogress_list", displayProgresses.size()) :
                        LanguageManager.getTranslation("simpleprogress.ui.progress_list", displayProgresses.size());
        guiGraphics.drawString(font, listTitle, 270, 37, 0xFFFFFF, false);

        String typeLabel = LanguageManager.getTranslation("simpleprogress.ui.current_type") + ": " + selectedType.getColorCode() + selectedType.getDisplayName();
        guiGraphics.drawString(font, typeLabel, 25, 205, 0xCCCCCC, false);

        String themeLabel = LanguageManager.getTranslation("simpleprogress.ui.current_theme") + ": " + getThemeDisplayName();
        guiGraphics.drawString(font, themeLabel, 25, 220, 0xCCCCCC, false);
    }

    private String getThemeDisplayName() {
        switch(currentTheme) {
            case 0: return LanguageManager.getTranslation("simpleprogress.theme.default");
            case 1: return LanguageManager.getTranslation("simpleprogress.theme.dark");
            case 2: return LanguageManager.getTranslation("simpleprogress.theme.light");
            case 3: return LanguageManager.getTranslation("simpleprogress.theme.green");
            case 4: return LanguageManager.getTranslation("simpleprogress.theme.blue");
            case 5: return LanguageManager.getTranslation("simpleprogress.theme.purple");
            default: return LanguageManager.getTranslation("simpleprogress.theme.default");
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
        if (isMouseOver(mouseX, mouseY, leftPos + 25, topPos + 128, 180, 18)) {
            String hint = getTargetFieldHint();
            guiGraphics.renderTooltip(font, Component.literal(hint), mouseX, mouseY);
        }

        if (!showStatistics) {
            int listStartY = topPos + 50;
            for (int i = 0; i < 14; i++) {
                int listIndex = i + scrollOffset;
                if (listIndex >= displayProgresses.size()) break;

                ProgressManager.CustomProgress progress = displayProgresses.get(listIndex);
                int entryY = listStartY + i * 22;

                if (isMouseOver(mouseX, mouseY, leftPos + 255, entryY, 220 - SCROLLBAR_WIDTH, 20)) {
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(Component.literal(progress.type.getColorCode() + "【" + progress.type.getDisplayName() + "】" + progress.title));
                    tooltip.add(Component.literal("§7" + LanguageManager.getTranslation("simpleprogress.ui.target") + ": §e" + progress.target));
                    tooltip.add(Component.literal("§7" + LanguageManager.getTranslation("simpleprogress.ui.progress") + ": §e" + progress.current + "§7/§e" + progress.targetCount));
                    tooltip.add(Component.literal("§7" + LanguageManager.getTranslation("simpleprogress.ui.completion") + ": §e" + String.format("%.1f", progress.getProgress() * 100) + "%"));
                    if (progress.hasSubProgresses()) {
                        tooltip.add(Component.literal("§7" + LanguageManager.getTranslation("simpleprogress.ui.subprogress_count") + ": §e" + progress.subProgresses.size()));
                    }
                    if (!progress.completed) {
                        tooltip.add(Component.literal("§7" + LanguageManager.getTranslation("simpleprogress.ui.estimated_time") + ": §e" + progress.getEstimatedTime()));
                    }
                    guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
                    break;
                }
            }
        }
    }

    private String getTargetFieldHint() {
        switch(selectedType) {
            case KILL: return LanguageManager.getTranslation("simpleprogress.hint.kill");
            case OBTAIN: return LanguageManager.getTranslation("simpleprogress.hint.obtain");
            case EXPLORE: return LanguageManager.getTranslation("simpleprogress.hint.explore");
            case BUILD: return LanguageManager.getTranslation("simpleprogress.hint.build");
            case ENCHANT: return LanguageManager.getTranslation("simpleprogress.hint.enchant");
            default: return LanguageManager.getTranslation("simpleprogress.hint.general");
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOverScrollbar((int)mouseX, (int)mouseY)) {
            isDraggingScroll = true;

            int scrollbarY = topPos + 50;
            int scrollbarHeight = 14 * 22;
            int maxScroll = Math.max(0, (displayProgresses.size() - 14) * 22);

            double scrollPercent = (mouseY - scrollbarY) / (double)scrollbarHeight;
            scrollOffset = (int)(maxScroll * Math.max(0, Math.min(1, scrollPercent)));

            return true;
        }

        int listStartY = topPos + 50;
        for (int i = 0; i < 14; i++) {
            int listIndex = i + scrollOffset;
            if (listIndex >= displayProgresses.size()) break;

            ProgressManager.CustomProgress progress = displayProgresses.get(listIndex);
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
            int scrollbarHeight = 14 * 22;
            int maxScroll = Math.max(0, (displayProgresses.size() - 14) * 22);

            double scrollPercent = (mouseY - scrollbarY) / (double)scrollbarHeight;
            scrollOffset = (int)(maxScroll * Math.max(0, Math.min(1, scrollPercent)));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isMouseOver(mouseX, mouseY, leftPos + 250, topPos + 25, 230, 280)) {
            int maxScroll = Math.max(0, displayProgresses.size() - 14);
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