package com.lanche.simpleprogress;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HelpScreen extends Screen {
    private final Screen parent;
    private int scrollOffset = 0;
    private boolean isDraggingScroll = false;
    private final List<FormattedCharSequence> helpLines = new ArrayList<>();
    private static final int SCROLLBAR_WIDTH = 8;

    private static final List<String> HELP_CONTENT_ZH = Arrays.asList(
            "§6=== Simple Progress 使用指南 v1.0.4 ===",
            "",
            "§a【新增功能】",
            "§e-§7 新增§6建筑§7和§5附魔§7进度类型",
            "§e-§7 支持子进度系统，创建层次化进度",
            "§e-§7 改进的树状图统计界面",
            "§e-§7 修复HUD显示，移动到右上角",
            "§e-§7 新增多种界面主题",
            "§e-§7 增大GUI尺寸，改善用户体验",
            "§e-§7 完整中英双语支持",
            "",
            "§a【创建进度】",
            "§e1.§7 选择进度类型: §c击杀§7 / §a获得§7 / §9探索§7 / §6建筑§7 / §5附魔§7",
            "§e2.§7 输入进度标题 (例如: '僵尸猎人')",
            "§e3.§7 输入目标ID (参考下方对照表)",
            "§e4.§7 输入目标数量",
            "§e5.§7 点击'添加进度'按钮",
            "",
            "§a【子进度系统】",
            "§e-§7 选择一个进度后点击'添加子进度'",
            "§e-§7 子进度会显示在父进度下方",
            "§e-§7 点击'显示子进度'查看特定进度的子进度",
            "§e-§7 树状图统计会显示完整的进度层次",
            "",
            "§a【管理进度】",
            "§e-§7 在右侧列表§e点击§7选择进度",
            "§e-§7 点击'删除进度'移除选中进度",
            "§e-§7 使用鼠标滚轮或拖动滚动条浏览列表",
            "§e-§7 点击'显示子进度'切换视图",
            "",
            "§a【进度追踪】",
            "§e-§7 击杀生物自动更新§c击杀§7进度",
            "§e-§7 获得物品自动更新§a获得§7进度",
            "§e-§7 探索维度自动更新§9探索§7进度",
            "§e-§7 放置方块自动更新§6建筑§7进度",
            "§e-§7 附魔物品自动更新§5附魔§7进度",
            "§e-§7 绿色进度条表示进行中，橙色表示已完成",
            "§e-§7 进度完成时会在聊天栏和HUD显示通知",
            "",
            "§a【统计功能】",
            "§e-§7 点击'统计'按钮查看进度树状图",
            "§e-§7 树状图显示进度层次结构和完成状态",
            "§e-§7 点击'清除进度'按钮删除所有进度",
            "§e-§7 清除操作需要二次确认，防止误操作",
            "",
            "§a【主题系统】",
            "§e-§7 点击'主题'按钮切换界面主题",
            "§e-§7 支持6种主题: 默认/暗色/亮色/绿色/蓝色/紫色",
            "§e-§7 主题设置会保存到本地",
            "",
            "§a【语言设置】",
            "§e-§7 点击语言按钮切换中英文界面",
            "§e-§7 支持简体中文和英文",
            "§e-§7 语言设置会自动保存",
            "",
            "§a【HUD显示】",
            "§e-§7 活跃进度显示在屏幕右上角",
            "§e-§7 显示进度条、标题和完成百分比",
            "§e-§7 最多同时显示3个活跃进度",
            "§e-§7 进度完成时显示临时通知",
            "",
            "§a【数据保存】",
            "§e-§7 进度数据会自动保存到世界文件夹",
            "§e-§7 游戏重启后进度不会丢失",
            "§e-§7 每个玩家有独立的进度数据",
            "§e-§7 支持子进度的完整序列化",
            "",
            "§c【重要提示】",
            "§7- 必须正确填写目标ID，否则进度无法更新",
            "§7- 目标ID格式: §eminecraft:实体名§7 或 §eminecraft:物品名§7 等",
            "§7- 如果进度没有更新，请检查ID是否正确",
            "§7- 建筑进度追踪方块放置，附魔进度追踪附魔获得",
            "",
            "§6【如何获取ID】",
            "§e-§7 按F3+H打开高级提示框",
            "§e-§7 将鼠标指向物品或实体查看ID",
            "§e-§7 或参考下方完整对照表",
            "",
            "§6【常见生物 - 击杀进度】",
            "§e僵尸§7: minecraft:zombie",
            "§e骷髅§7: minecraft:skeleton",
            "§e苦力怕§7: minecraft:creeper",
            "§e蜘蛛§7: minecraft:spider",
            "§e末影人§7: minecraft:enderman",
            "§e女巫§7: minecraft:witch",
            "§e史莱姆§7: minecraft:slime",
            "§e烈焰人§7: minecraft:blaze",
            "§e恶魂§7: minecraft:ghast",
            "§e凋灵骷髅§7: minecraft:wither_skeleton",
            "§e末影龙§7: minecraft:ender_dragon",
            "§e凋灵§7: minecraft:wither",
            "",
            "§6【常见物品 - 获得进度】",
            "§e钻石§7: minecraft:diamond",
            "§e铁锭§7: minecraft:iron_ingot",
            "§e金锭§7: minecraft:gold_ingot",
            "§e绿宝石§7: minecraft:emerald",
            "§e下界合金锭§7: minecraft:netherite_ingot",
            "§e煤炭§7: minecraft:coal",
            "§e红石§7: minecraft:redstone",
            "§e青金石§7: minecraft:lapis_lazuli",
            "§e下界之星§7: minecraft:nether_star",
            "",
            "§6【探索维度】",
            "§e主世界§7: minecraft:overworld",
            "§e下界§7: minecraft:the_nether",
            "§e末地§7: minecraft:the_end",
            "",
            "§6【常见方块 - 建筑进度】",
            "§e泥土§7: minecraft:dirt",
            "§e石头§7: minecraft:stone",
            "§e圆石§7: minecraft:cobblestone",
            "§e橡木木板§7: minecraft:oak_planks",
            "§e玻璃§7: minecraft:glass",
            "§e羊毛§7: minecraft:white_wool",
            "§e砖块§7: minecraft:bricks",
            "§e铁块§7: minecraft:iron_block",
            "§e金块§7: minecraft:gold_block",
            "§e钻石块§7: minecraft:diamond_block",
            "§e黑曜石§7: minecraft:obsidian",
            "",
            "§6【常见附魔 - 附魔进度】",
            "§e锋利§7: minecraft:sharpness",
            "§e保护§7: minecraft:protection",
            "§e火焰保护§7: minecraft:fire_protection",
            "§e摔落保护§7: minecraft:feather_falling",
            "§e爆炸保护§7: minecraft:blast_protection",
            "§e弹射物保护§7: minecraft:projectile_protection",
            "§e水下呼吸§7: minecraft:respiration",
            "§e水下速掘§7: minecraft:aqua_affinity",
            "§e荆棘§7: minecraft:thorns",
            "§e深海探索者§7: minecraft:depth_strider",
            "§e冰霜行者§7: minecraft:frost_walker",
            "§e效率§7: minecraft:efficiency",
            "§e精准采集§7: minecraft:silk_touch",
            "§e耐久§7: minecraft:unbreaking",
            "§e时运§7: minecraft:fortune",
            "§e力量§7: minecraft:power",
            "§e冲击§7: minecraft:punch",
            "§e火矢§7: minecraft:flame",
            "§e无限§7: minecraft:infinity",
            "§e海之眷顾§7: minecraft:luck_of_the_sea",
            "§e饵钓§7: minecraft:lure",
            "",
            "§a【进度类型总结】",
            "§c击杀§7: 击杀特定生物",
            "§a获得§7: 获得特定物品",
            "§9探索§7: 到达特定维度",
            "§6建筑§7: 放置特定方块",
            "§5附魔§7: 获得特定附魔",
            "",
            "§c【注意事项】",
            "§7- 进度数据会自动保存，无需担心丢失",
            "§7- 确保目标ID填写正确，否则进度不会更新",
            "§7- 如果进度没有更新，请检查ID是否拼写正确",
            "§7- 版本: §e1.0.4",
            "§7- 开发者: §e澜澈LanChe",
            "§7- 主页: §9lanche.vvvv.host",
            "§7- 兼容性: §aMinecraft 1.20.1 + Forge 47.4.0"
    );

    private static final List<String> HELP_CONTENT_EN = Arrays.asList(
            "§6=== Simple Progress User Guide v1.0.4 ===",
            "",
            "§a【New Features】",
            "§e-§7 Added §6Build§7 and §5Enchant§7 progress types",
            "§e-§7 Support sub-progress system for hierarchical progress",
            "§e-§7 Improved tree chart statistics interface",
            "§e-§7 Fixed HUD display, moved to top-right corner",
            "§e-§7 Added multiple interface themes",
            "§e-§7 Increased GUI size for better user experience",
            "§e-§7 Full Chinese-English bilingual support",
            "",
            "§a【Creating Progress】",
            "§e1.§7 Select progress type: §cKill§7 / §aObtain§7 / §9Explore§7 / §6Build§7 / §5Enchant§7",
            "§e2.§7 Enter progress title (e.g., 'Zombie Hunter')",
            "§e3.§7 Enter target ID (refer to table below)",
            "§e4.§7 Enter target count",
            "§e5.§7 Click 'Add Progress' button",
            "",
            "§a【Sub-Progress System】",
            "§e-§7 Select a progress and click 'Add Sub-Progress'",
            "§e-§7 Sub-progresses appear below parent progress",
            "§e-§7 Click 'Show Sub-Progress' to view specific progress sub-progresses",
            "§e-§7 Tree chart shows complete progress hierarchy",
            "",
            "§a【Managing Progress】",
            "§e-§7 §eClick§7 on progress in right list to select",
            "§e-§7 Click 'Delete Progress' to remove selected progress",
            "§e-§7 Use mouse wheel or drag scrollbar to browse list",
            "§e-§7 Click 'Show Sub-Progress' to toggle view",
            "",
            "§a【Progress Tracking】",
            "§e-§7 Killing entities auto-updates §cKill§7 progress",
            "§e-§7 Obtaining items auto-updates §aObtain§7 progress",
            "§e-§7 Exploring dimensions auto-updates §9Explore§7 progress",
            "§e-§7 Placing blocks auto-updates §6Build§7 progress",
            "§e-§7 Enchanting items auto-updates §5Enchant§7 progress",
            "§e-§7 Green progress bar indicates in progress, orange indicates completed",
            "§e-§7 Progress completion shows notifications in chat and HUD",
            "",
            "§a【Statistics Function】",
            "§e-§7 Click 'Statistics' button to view progress tree chart",
            "§e-§7 Tree chart shows progress hierarchy and completion status",
            "§e-§7 Click 'Clear All' button to delete all progress",
            "§e-§7 Clear operation requires confirmation to prevent accidents",
            "",
            "§a【Theme System】",
            "§e-§7 Click 'Theme' button to switch interface themes",
            "§e-§7 Supports 6 themes: Default/Dark/Light/Green/Blue/Purple",
            "§e-§7 Theme settings are saved locally",
            "",
            "§a【Language Settings】",
            "§e-§7 Click language button to switch between Chinese and English",
            "§e-§7 Supports Simplified Chinese and English",
            "§e-§7 Language settings are automatically saved",
            "",
            "§a【HUD Display】",
            "§e-§7 Active progress displayed in top-right corner",
            "§e-§7 Shows progress bar, title and completion percentage",
            "§e-§7 Maximum 3 active progresses displayed simultaneously",
            "§e-§7 Temporary notification shown when progress completes",
            "",
            "§a【Data Saving】",
            "§e-§7 Progress data automatically saved to world folder",
            "§e-§7 Progress not lost after game restart",
            "§e-§7 Each player has independent progress data",
            "§e-§7 Supports complete serialization of sub-progresses",
            "",
            "§c【Important Notes】",
            "§7- Must enter correct target ID, otherwise progress won't update",
            "§7- Target ID format: §eminecraft:entity_name§7 or §eminecraft:item_name§7 etc.",
            "§7- If progress not updating, check if ID is correct",
            "§7- Build progress tracks block placement, Enchant progress tracks enchantment acquisition",
            "",
            "§6【How to Get IDs】",
            "§e-§7 Press F3+H to open advanced tooltips",
            "§e-§7 Hover over items or entities to view ID",
            "§e-§7 Or refer to complete reference table below",
            "",
            "§6【Common Entities - Kill Progress】",
            "§eZombie§7: minecraft:zombie",
            "§eSkeleton§7: minecraft:skeleton",
            "§eCreeper§7: minecraft:creeper",
            "§eSpider§7: minecraft:spider",
            "§eEnderman§7: minecraft:enderman",
            "§eWitch§7: minecraft:witch",
            "§eSlime§7: minecraft:slime",
            "§eBlaze§7: minecraft:blaze",
            "§eGhast§7: minecraft:ghast",
            "§eWither Skeleton§7: minecraft:wither_skeleton",
            "§eEnder Dragon§7: minecraft:ender_dragon",
            "§eWither§7: minecraft:wither",
            "",
            "§6【Common Items - Obtain Progress】",
            "§eDiamond§7: minecraft:diamond",
            "§eIron Ingot§7: minecraft:iron_ingot",
            "§eGold Ingot§7: minecraft:gold_ingot",
            "§eEmerald§7: minecraft:emerald",
            "§eNetherite Ingot§7: minecraft:netherite_ingot",
            "§eCoal§7: minecraft:coal",
            "§eRedstone§7: minecraft:redstone",
            "§eLapis Lazuli§7: minecraft:lapis_lazuli",
            "§eNether Star§7: minecraft:nether_star",
            "",
            "§6【Explore Dimensions】",
            "§eOverworld§7: minecraft:overworld",
            "§eNether§7: minecraft:the_nether",
            "§eEnd§7: minecraft:the_end",
            "",
            "§6【Common Blocks - Build Progress】",
            "§eDirt§7: minecraft:dirt",
            "§eStone§7: minecraft:stone",
            "§eCobblestone§7: minecraft:cobblestone",
            "§eOak Planks§7: minecraft:oak_planks",
            "§eGlass§7: minecraft:glass",
            "§eWhite Wool§7: minecraft:white_wool",
            "§eBricks§7: minecraft:bricks",
            "§eIron Block§7: minecraft:iron_block",
            "§eGold Block§7: minecraft:gold_block",
            "§eDiamond Block§7: minecraft:diamond_block",
            "§eObsidian§7: minecraft:obsidian",
            "",
            "§6【Common Enchantments - Enchant Progress】",
            "§eSharpness§7: minecraft:sharpness",
            "§eProtection§7: minecraft:protection",
            "§eFire Protection§7: minecraft:fire_protection",
            "§eFeather Falling§7: minecraft:feather_falling",
            "§eBlast Protection§7: minecraft:blast_protection",
            "§eProjectile Protection§7: minecraft:projectile_protection",
            "§eRespiration§7: minecraft:respiration",
            "§eAqua Affinity§7: minecraft:aqua_affinity",
            "§eThorns§7: minecraft:thorns",
            "§eDepth Strider§7: minecraft:depth_strider",
            "§eFrost Walker§7: minecraft:frost_walker",
            "§eEfficiency§7: minecraft:efficiency",
            "§eSilk Touch§7: minecraft:silk_touch",
            "§eUnbreaking§7: minecraft:unbreaking",
            "§eFortune§7: minecraft:fortune",
            "§ePower§7: minecraft:power",
            "§ePunch§7: minecraft:punch",
            "§eFlame§7: minecraft:flame",
            "§eInfinity§7: minecraft:infinity",
            "§eLuck of the Sea§7: minecraft:luck_of_the_sea",
            "§eLure§7: minecraft:lure",
            "",
            "§a【Progress Type Summary】",
            "§cKill§7: Kill specific entities",
            "§aObtain§7: Obtain specific items",
            "§9Explore§7: Reach specific dimensions",
            "§6Build§7: Place specific blocks",
            "§5Enchant§7: Obtain specific enchantments",
            "",
            "§c【Notes】",
            "§7- Progress data automatically saved, no need to worry about loss",
            "§7- Ensure target ID is entered correctly, otherwise progress won't update",
            "§7- If progress not updating, check if ID spelling is correct",
            "§7- Version: §e1.0.4",
            "§7- Developer: §e澜澈LanChe",
            "§7- Homepage: §9lanche.vvvv.host",
            "§7- Compatibility: §aMinecraft 1.20.1 + Forge 47.4.0"
    );

    public HelpScreen(Screen parent) {
        super(Component.translatable("simpleprogress.gui.help"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        preprocessHelpText();

        this.addRenderableWidget(Button.builder(
                Component.translatable("simpleprogress.gui.back_to_manager"),
                button -> this.minecraft.setScreen(parent)
        ).pos(this.width / 2 - 100, this.height - 40).size(200, 20).build());
    }

    private void preprocessHelpText() {
        helpLines.clear();
        int contentWidth = this.width - 120 - SCROLLBAR_WIDTH;

        List<String> helpContent = LanguageManager.getCurrentLanguage().equals("zh_cn") ?
                HELP_CONTENT_ZH : HELP_CONTENT_EN;

        for (String line : helpContent) {
            if (line.isEmpty()) {
                helpLines.add(FormattedCharSequence.EMPTY);
            } else {
                List<FormattedCharSequence> wrapped = this.font.split(Component.literal(line), contentWidth);
                helpLines.addAll(wrapped);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        int contentX = 40;
        int contentY = 20;
        int contentWidth = this.width - 80 - SCROLLBAR_WIDTH;
        int contentHeight = this.height - 70;

        guiGraphics.fill(contentX, contentY, contentX + contentWidth + SCROLLBAR_WIDTH, contentY + contentHeight, 0xDD000000);

        guiGraphics.fill(contentX, contentY, contentX + contentWidth + SCROLLBAR_WIDTH, contentY + 25, 0xFF333366);

        String title = LanguageManager.getCurrentLanguage().equals("zh_cn") ?
                "§6Simple Progress 完整使用指南 v1.0.4" : "§6Simple Progress Complete User Guide v1.0.4";
        guiGraphics.drawCenteredString(font, title, this.width / 2, contentY + 8, 0xFFFFFF);

        setScissor(contentX, contentY + 25, contentWidth, contentHeight - 25);

        int lineHeight = 14;
        int startY = contentY + 35 - scrollOffset;

        for (int i = 0; i < helpLines.size(); i++) {
            FormattedCharSequence line = helpLines.get(i);
            int yPos = startY + i * lineHeight;

            if (yPos >= contentY + 25 && yPos <= contentY + contentHeight - lineHeight) {
                guiGraphics.drawString(font, line, contentX + 15, yPos, 0xFFFFFF, false);
            }
        }

        RenderSystem.disableScissor();

        drawScrollbar(guiGraphics, contentX + contentWidth, contentY + 25, contentHeight - 25, mouseX, mouseY);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void setScissor(int x, int y, int width, int height) {
        double guiScale = minecraft.getWindow().getGuiScale();
        int scissorX = (int) (x * guiScale);
        int scissorY = (int) (minecraft.getWindow().getHeight() - (y + height) * guiScale);
        int scissorWidth = (int) (width * guiScale);
        int scissorHeight = (int) (height * guiScale);

        RenderSystem.enableScissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }

    private void drawScrollbar(GuiGraphics guiGraphics, int x, int y, int height, int mouseX, int mouseY) {
        int scrollbarX = x + 2;

        guiGraphics.fill(scrollbarX, y, scrollbarX + SCROLLBAR_WIDTH, y + height, 0xFF555555);

        if (isContentScrollable()) {
            int totalLines = helpLines.size();
            int visibleLines = height / 14;
            int scrollbarHeight = Math.max(30, height * visibleLines / totalLines);
            int maxScroll = Math.max(0, (totalLines - visibleLines) * 14);
            int scrollProgress = maxScroll > 0 ? (int) ((float) scrollOffset / maxScroll * (height - scrollbarHeight)) : 0;

            int scrollbarY = y + scrollProgress;

            boolean isHovered = isMouseOverScrollbar(mouseX, mouseY);
            int scrollbarColor = isHovered || isDraggingScroll ? 0xFF8888CC : 0xFF666699;
            guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight, scrollbarColor);

            guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + SCROLLBAR_WIDTH, scrollbarY + 1, 0xFFAAAAAA);
            guiGraphics.fill(scrollbarX, scrollbarY + scrollbarHeight - 1, scrollbarX + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight, 0xFFAAAAAA);
        }
    }

    private boolean isContentScrollable() {
        int contentHeight = this.height - 95;
        int totalHeight = helpLines.size() * 14;
        return totalHeight > contentHeight;
    }

    private boolean isMouseOverScrollbar(int mouseX, int mouseY) {
        int contentX = 40;
        int contentWidth = this.width - 80 - SCROLLBAR_WIDTH;
        int scrollbarX = contentX + contentWidth + 2;
        int contentY = 20;
        int contentHeight = this.height - 70;

        return mouseX >= scrollbarX &&
                mouseX <= scrollbarX + SCROLLBAR_WIDTH &&
                mouseY >= contentY + 25 &&
                mouseY <= contentY + contentHeight;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int contentX = 40;
        int contentY = 20;
        int contentWidth = this.width - 80 - SCROLLBAR_WIDTH;
        int contentHeight = this.height - 70;

        if (mouseX >= contentX && mouseX <= contentX + contentWidth &&
                mouseY >= contentY + 25 && mouseY <= contentY + contentHeight) {

            int lineHeight = 14;
            int totalLines = helpLines.size();
            int visibleLines = (contentHeight - 25) / lineHeight;
            int maxScroll = Math.max(0, (totalLines - visibleLines) * lineHeight);

            if (delta > 0) {
                scrollOffset = Math.max(0, scrollOffset - (int)(lineHeight * 2));
            } else if (delta < 0) {
                scrollOffset = Math.min(maxScroll, scrollOffset + (int)(lineHeight * 2));
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOverScrollbar((int)mouseX, (int)mouseY)) {
            isDraggingScroll = true;

            int contentY = 20;
            int contentHeight = this.height - 70;
            int totalLines = helpLines.size();
            int visibleLines = (contentHeight - 25) / 14;
            int maxScroll = Math.max(0, (totalLines - visibleLines) * 14);

            double scrollPercent = (mouseY - contentY - 25) / (double)(contentHeight - 25);
            scrollOffset = (int)(maxScroll * Math.max(0, Math.min(1, scrollPercent)));

            return true;
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
            int contentY = 20;
            int contentHeight = this.height - 70;
            int totalLines = helpLines.size();
            int visibleLines = (contentHeight - 25) / 14;
            int maxScroll = Math.max(0, (totalLines - visibleLines) * 14);

            double scrollPercent = (mouseY - contentY - 25) / (double)(contentHeight - 25);
            scrollOffset = (int)(maxScroll * Math.max(0, Math.min(1, scrollPercent)));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}