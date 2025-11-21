package com.lanche.simpleprogress;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = SimpleProgress.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ProgressHUD {

    private static long lastProgressUpdate = 0;
    private static String currentProgressMessage = "";

    @SubscribeEvent
    public static void onRenderGameOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay().id().getPath().equals("hotbar")) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            GuiGraphics guiGraphics = event.getGuiGraphics();
            int width = event.getWindow().getGuiScaledWidth();
            int height = event.getWindow().getGuiScaledHeight();

            if (System.currentTimeMillis() - lastProgressUpdate < 5000 && !currentProgressMessage.isEmpty()) {
                int yPos = 20;
                int stringWidth = mc.font.width(currentProgressMessage);
                int xPos = width - stringWidth - 10;

                guiGraphics.fill(xPos - 5, yPos - 2, xPos + stringWidth + 5, yPos + 10, 0x80000000);
                guiGraphics.drawString(mc.font, currentProgressMessage, xPos, yPos, 0xFFFFFF);
            }

            renderActiveProgressHUD(guiGraphics, width, height, mc);
        }
    }

    private static void renderActiveProgressHUD(GuiGraphics guiGraphics, int width, int height, Minecraft mc) {
        List<ProgressManager.CustomProgress> progresses = ProgressManager.getProgresses(mc.player);
        if (progresses == null || progresses.isEmpty()) return;

        List<ProgressManager.CustomProgress> activeProgresses = progresses.stream()
                .filter(p -> !p.completed && p.getProgress() > 0)
                .sorted((a, b) -> Float.compare(b.getProgress(), a.getProgress()))
                .limit(3)
                .collect(Collectors.toList());

        if (activeProgresses.isEmpty()) return;

        int startY = 40;
        int barWidth = 150;
        int xPos = width - barWidth - 10;

        for (int i = 0; i < activeProgresses.size(); i++) {
            ProgressManager.CustomProgress progress = activeProgresses.get(i);
            renderProgressBar(guiGraphics, xPos, startY + i * 25, barWidth, progress, mc);
        }
    }

    private static void renderProgressBar(GuiGraphics guiGraphics, int x, int y, int barWidth, ProgressManager.CustomProgress progress, Minecraft mc) {
        int barHeight = 12;

        guiGraphics.fill(x, y, x + barWidth, y + barHeight, 0x80000000);

        int progressWidth = (int) (barWidth * progress.getProgress());
        int color = getProgressColor(progress.type);
        guiGraphics.fill(x, y, x + progressWidth, y + barHeight, color);

        guiGraphics.fill(x, y, x + barWidth, y + 1, 0xFFAAAAAA);
        guiGraphics.fill(x, y + barHeight - 1, x + barWidth, y + barHeight, 0xFFAAAAAA);
        guiGraphics.fill(x, y, x + 1, y + barHeight, 0xFFAAAAAA);
        guiGraphics.fill(x + barWidth - 1, y, x + barWidth, y + barHeight, 0xFFAAAAAA);

        String text = progress.title + " " + progress.current + "/" + progress.targetCount;
        int textWidth = mc.font.width(text);

        if (textWidth > barWidth - 10) {
            text = mc.font.plainSubstrByWidth(text, barWidth - 15) + "...";
            textWidth = mc.font.width(text);
        }

        int textX = x + (barWidth - textWidth) / 2;
        guiGraphics.drawString(mc.font, text, textX, y - 10, 0xFFFFFF);

        String percent = String.format("%.1f%%", progress.getProgress() * 100);
        int percentWidth = mc.font.width(percent);
        int percentX = x + (barWidth - percentWidth) / 2;
        guiGraphics.drawString(mc.font, percent, percentX, y + 2, 0xFFFFFF);
    }

    public static void showProgressUpdate(String message) {
        currentProgressMessage = message;
        lastProgressUpdate = System.currentTimeMillis();
    }

    private static int getProgressColor(ProgressManager.ProgressType type) {
        switch(type) {
            case KILL: return 0xAAFF0000;
            case OBTAIN: return 0xAA00FF00;
            case EXPLORE: return 0xAA0000FF;
            case BUILD: return 0xAAFFAA00;
            case ENCHANT: return 0xAAAA00FF;
            default: return 0xAA00FF00;
        }
    }
}