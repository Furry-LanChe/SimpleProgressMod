package com.lanche.simpleprogress;

import net.minecraft.nbt.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ProgressManager {
    private static final Map<UUID, List<CustomProgress>> PLAYER_DATA = new HashMap<>();
    private static final Path SAVE_DIR = Path.of("config/simpleprogress/data");

    public enum ProgressType {
        KILL("Kill", "§c"),
        OBTAIN("Obtain", "§a"),
        BUILD("Build", "§6");

        private final String displayName;
        private final String colorCode;

        ProgressType(String displayName, String colorCode) {
            this.displayName = displayName;
            this.colorCode = colorCode;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColorCode() {
            return colorCode;
        }
    }

    public static class CustomProgress {
        public String id;
        public String title;
        public ProgressType type;
        public String target;
        public int current;
        public int targetCount;
        public boolean completed;
        public long createdTime;

        public CustomProgress() {
            this.id = UUID.randomUUID().toString();
            this.createdTime = System.currentTimeMillis();
        }

        public float getProgress() {
            return targetCount > 0 ? (float) current / targetCount : 0;
        }

        public CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", id);
            tag.putString("title", title);
            tag.putString("type", type.name());
            tag.putString("target", target);
            tag.putInt("current", current);
            tag.putInt("targetCount", targetCount);
            tag.putBoolean("completed", completed);
            tag.putLong("createdTime", createdTime);
            return tag;
        }

        public static CustomProgress fromNbt(CompoundTag tag) {
            CustomProgress progress = new CustomProgress();
            progress.id = tag.getString("id");
            progress.title = tag.getString("title");
            progress.type = ProgressType.valueOf(tag.getString("type"));
            progress.target = tag.getString("target");
            progress.current = tag.getInt("current");
            progress.targetCount = tag.getInt("targetCount");
            progress.completed = tag.getBoolean("completed");
            progress.createdTime = tag.getLong("createdTime");
            return progress;
        }
    }

    public static List<CustomProgress> getPlayerData(Player player) {
        UUID playerId = player.getUUID();
        if (!PLAYER_DATA.containsKey(playerId)) {
            loadPlayerData(player);
        }
        return PLAYER_DATA.getOrDefault(playerId, new ArrayList<>());
    }

    public static void addProgress(Player player, CustomProgress progress) {
        List<CustomProgress> data = getPlayerData(player);
        data.add(progress);
        savePlayerData(player);
    }

    public static void removeProgress(Player player, String progressId) {
        List<CustomProgress> data = getPlayerData(player);
        data.removeIf(p -> p.id.equals(progressId));
        savePlayerData(player);
    }

    public static void clearAllProgresses(Player player) {
        PLAYER_DATA.put(player.getUUID(), new ArrayList<>());
        savePlayerData(player);
    }

    private static void savePlayerData(Player player) {
        try {
            if (!Files.exists(SAVE_DIR)) {
                Files.createDirectories(SAVE_DIR);
            }

            List<CustomProgress> data = getPlayerData(player);
            CompoundTag root = new CompoundTag();
            ListTag progressList = new ListTag();

            for (CustomProgress progress : data) {
                progressList.add(progress.toNbt());
            }

            root.put("progresses", progressList);

            File saveFile = SAVE_DIR.resolve(player.getUUID().toString() + ".dat").toFile();
            try (FileOutputStream stream = new FileOutputStream(saveFile)) {
                NbtIo.writeCompressed(root, stream);
            }
        } catch (Exception e) {
            SimpleProgressMod.LOGGER.error("Failed to save progress data", e);
        }
    }

    private static void loadPlayerData(Player player) {
        try {
            File saveFile = SAVE_DIR.resolve(player.getUUID().toString() + ".dat").toFile();
            if (!saveFile.exists()) {
                PLAYER_DATA.put(player.getUUID(), new ArrayList<>());
                return;
            }

            try (FileInputStream stream = new FileInputStream(saveFile)) {
                CompoundTag root = NbtIo.readCompressed(stream);
                List<CustomProgress> data = new ArrayList<>();

                ListTag progressList = root.getList("progresses", 10);
                for (int i = 0; i < progressList.size(); i++) {
                    data.add(CustomProgress.fromNbt(progressList.getCompound(i)));
                }

                PLAYER_DATA.put(player.getUUID(), data);
            }
        } catch (Exception e) {
            SimpleProgressMod.LOGGER.error("Failed to load progress data", e);
            PLAYER_DATA.put(player.getUUID(), new ArrayList<>());
        }
    }
}