package com.lanche.simpleprogress;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProgressManager {
    private static final Map<UUID, List<CustomProgress>> playerProgresses = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LogManager.getLogger("SimpleProgress");

    private static final Map<UUID, PlayerStats> playerStats = new ConcurrentHashMap<>();

    public static List<CustomProgress> getProgresses(Player player) {
        UUID playerId = player.getUUID();

        if (!playerProgresses.containsKey(playerId)) {
            loadPlayerProgress(player);
        }

        return playerProgresses.computeIfAbsent(playerId, k -> new ArrayList<>());
    }

    public static PlayerStats getPlayerStats(Player player) {
        return playerStats.computeIfAbsent(player.getUUID(), k -> new PlayerStats());
    }

    public static void addProgress(Player player, CustomProgress progress) {
        List<CustomProgress> progresses = getProgresses(player);
        progresses.add(progress);
        savePlayerProgress(player);
    }

    public static void addSubProgress(Player player, String parentId, CustomProgress subProgress) {
        List<CustomProgress> progresses = getProgresses(player);
        CustomProgress parentProgress = findProgressById(progresses, parentId);

        if (parentProgress != null) {
            if (parentProgress.subProgresses == null) {
                parentProgress.subProgresses = new ArrayList<>();
            }
            subProgress.parentId = parentId;
            parentProgress.subProgresses.add(subProgress);
            savePlayerProgress(player);
        }
    }

    private static CustomProgress findProgressById(List<CustomProgress> progresses, String id) {
        for (CustomProgress progress : progresses) {
            if (progress.id.equals(id)) {
                return progress;
            }
            if (progress.subProgresses != null && !progress.subProgresses.isEmpty()) {
                CustomProgress found = findProgressById(progress.subProgresses, id);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    public static List<CustomProgress> getAllSubProgresses(CustomProgress parent) {
        List<CustomProgress> allSubs = new ArrayList<>();
        if (parent.subProgresses != null) {
            for (CustomProgress sub : parent.subProgresses) {
                allSubs.add(sub);
                allSubs.addAll(getAllSubProgresses(sub));
            }
        }
        return allSubs;
    }

    public static List<CustomProgress> getDirectSubProgresses(List<CustomProgress> allProgresses, String parentId) {
        List<CustomProgress> directSubs = new ArrayList<>();
        for (CustomProgress progress : allProgresses) {
            if (parentId.equals(progress.parentId)) {
                directSubs.add(progress);
            }
        }
        return directSubs;
    }

    public static void removeProgress(Player player, String id) {
        List<CustomProgress> progresses = getProgresses(player);
        boolean removed = removeProgressRecursive(progresses, id);
        if (removed) {
            savePlayerProgress(player);
        }
    }

    private static boolean removeProgressRecursive(List<CustomProgress> progresses, String id) {
        Iterator<CustomProgress> iterator = progresses.iterator();
        while (iterator.hasNext()) {
            CustomProgress progress = iterator.next();
            if (progress.id.equals(id)) {
                iterator.remove();
                return true;
            }
            if (progress.subProgresses != null && !progress.subProgresses.isEmpty()) {
                if (removeProgressRecursive(progress.subProgresses, id)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void updateProgress(Player player, String target, ProgressType type) {
        List<CustomProgress> progresses = getProgresses(player);
        boolean changed = updateProgressRecursive(progresses, target, type, player);

        if (changed) {
            savePlayerProgress(player);
        }
    }

    private static String getCompletionMessage(ProgressType type, String title, int current, int targetCount, String target) {
        switch(type) {
            case KILL:
                return LanguageManager.getTranslation("simpleprogress.message.kill_complete", title, current, targetCount);
            case OBTAIN:
                return LanguageManager.getTranslation("simpleprogress.message.obtain_complete", title, current, targetCount);
            case EXPLORE:
                return LanguageManager.getTranslation("simpleprogress.message.explore_complete", title, getDimensionDisplayName(target));
            case BUILD:
                return LanguageManager.getTranslation("simpleprogress.message.build_complete", title, current, targetCount);
            case ENCHANT:
                return LanguageManager.getTranslation("simpleprogress.message.enchant_complete", title, getEnchantmentDisplayName(target));
            default:
                return LanguageManager.getTranslation("simpleprogress.message.progress_complete", title, current, targetCount);
        }
    }

    private static boolean updateProgressRecursive(List<CustomProgress> progresses, String target, ProgressType type, Player player) {
        boolean changed = false;

        for (CustomProgress progress : progresses) {
            if (progress.type == type && progress.target.equals(target) && !progress.completed) {
                boolean wasNotCompleted = !progress.completed;
                progress.current++;
                if (progress.current >= progress.targetCount) {
                    progress.completed = true;
                    getPlayerStats(player).completedProgresses++;

                    if (wasNotCompleted && player instanceof ServerPlayer) {
                        String completionMessage = getCompletionMessage(progress.type, progress.title, progress.current, progress.targetCount, progress.target);
                        player.displayClientMessage(Component.literal(completionMessage), false);
                        ProgressHUD.showProgressUpdate(completionMessage);
                    }
                } else {
                    if (progress.current % Math.max(1, progress.targetCount / 5) == 0) {
                        float percentage = (float) progress.current / progress.targetCount * 100;
                        String updateMessage = LanguageManager.getTranslation("simpleprogress.message.progress_update",
                                progress.title, progress.current, progress.targetCount, percentage);
                        player.displayClientMessage(Component.literal(updateMessage), false);
                    }
                }
                changed = true;
            }

            if (progress.subProgresses != null && !progress.subProgresses.isEmpty()) {
                if (updateProgressRecursive(progress.subProgresses, target, type, player)) {
                    changed = true;
                }
            }
        }

        return changed;
    }

    public static void updateExploreProgress(Player player, String dimension, int x, int z) {
        List<CustomProgress> progresses = getProgresses(player);
        boolean changed = updateExploreProgressRecursive(progresses, dimension, player);

        if (changed) {
            savePlayerProgress(player);
        }
    }

    private static boolean updateExploreProgressRecursive(List<CustomProgress> progresses, String dimension, Player player) {
        boolean changed = false;

        for (CustomProgress progress : progresses) {
            if (progress.type == ProgressType.EXPLORE && progress.target.equals(dimension) && !progress.completed) {
                progress.current = 1;
                progress.completed = true;
                getPlayerStats(player).completedProgresses++;

                String completionMessage = LanguageManager.getTranslation("simpleprogress.message.explore_complete",
                        progress.title, getDimensionDisplayName(dimension));
                player.displayClientMessage(Component.literal(completionMessage), false);
                ProgressHUD.showProgressUpdate(completionMessage);
                changed = true;
            }

            if (progress.subProgresses != null && !progress.subProgresses.isEmpty()) {
                if (updateExploreProgressRecursive(progress.subProgresses, dimension, player)) {
                    changed = true;
                }
            }
        }

        return changed;
    }

    public static void updateBuildProgress(Player player, String blockId) {
        List<CustomProgress> progresses = getProgresses(player);
        boolean changed = updateBuildProgressRecursive(progresses, blockId, player);

        if (changed) {
            savePlayerProgress(player);
        }
    }

    private static boolean updateBuildProgressRecursive(List<CustomProgress> progresses, String blockId, Player player) {
        boolean changed = false;

        for (CustomProgress progress : progresses) {
            if (progress.type == ProgressType.BUILD && progress.target.equals(blockId) && !progress.completed) {
                progress.current++;
                if (progress.current >= progress.targetCount) {
                    progress.completed = true;
                    getPlayerStats(player).completedProgresses++;

                    String completionMessage = LanguageManager.getTranslation("simpleprogress.message.build_complete",
                            progress.title, progress.current, progress.targetCount);
                    player.displayClientMessage(Component.literal(completionMessage), false);
                    ProgressHUD.showProgressUpdate(completionMessage);
                }
                changed = true;
            }

            if (progress.subProgresses != null && !progress.subProgresses.isEmpty()) {
                if (updateBuildProgressRecursive(progress.subProgresses, blockId, player)) {
                    changed = true;
                }
            }
        }

        return changed;
    }

    public static void updateEnchantProgress(Player player, String enchantmentId) {
        List<CustomProgress> progresses = getProgresses(player);
        boolean changed = updateEnchantProgressRecursive(progresses, enchantmentId, player);

        if (changed) {
            savePlayerProgress(player);
        }
    }

    private static boolean updateEnchantProgressRecursive(List<CustomProgress> progresses, String enchantmentId, Player player) {
        boolean changed = false;

        for (CustomProgress progress : progresses) {
            if (progress.type == ProgressType.ENCHANT && progress.target.equals(enchantmentId) && !progress.completed) {
                progress.current = 1;
                progress.completed = true;
                getPlayerStats(player).completedProgresses++;

                String completionMessage = LanguageManager.getTranslation("simpleprogress.message.enchant_complete",
                        progress.title, getEnchantmentDisplayName(enchantmentId));
                player.displayClientMessage(Component.literal(completionMessage), false);
                ProgressHUD.showProgressUpdate(completionMessage);
                changed = true;
            }

            if (progress.subProgresses != null && !progress.subProgresses.isEmpty()) {
                if (updateEnchantProgressRecursive(progress.subProgresses, enchantmentId, player)) {
                    changed = true;
                }
            }
        }

        return changed;
    }

    public static class TreeChartData {
        public final List<TreeNode> nodes;
        public final int totalProgresses;
        public final int completedProgresses;

        public TreeChartData(List<TreeNode> nodes, int totalProgresses, int completedProgresses) {
            this.nodes = nodes;
            this.totalProgresses = totalProgresses;
            this.completedProgresses = completedProgresses;
        }
    }

    public static class TreeNode {
        public final CustomProgress progress;
        public final List<TreeNode> children;
        public int depth;

        public TreeNode(CustomProgress progress, int depth) {
            this.progress = progress;
            this.children = new ArrayList<>();
            this.depth = depth;
        }
    }

    public static TreeChartData getTreeChartData(Player player) {
        List<CustomProgress> progresses = getProgresses(player);
        List<TreeNode> rootNodes = new ArrayList<>();

        Map<String, TreeNode> nodeMap = new HashMap<>();
        List<TreeNode> allNodes = new ArrayList<>();

        List<CustomProgress> allProgresses = getAllProgressesFlat(progresses);
        for (CustomProgress progress : allProgresses) {
            TreeNode node = new TreeNode(progress, 0);
            nodeMap.put(progress.id, node);
            allNodes.add(node);
        }

        for (TreeNode node : allNodes) {
            if (node.progress.parentId != null && !node.progress.parentId.isEmpty()) {
                TreeNode parent = nodeMap.get(node.progress.parentId);
                if (parent != null) {
                    parent.children.add(node);
                    node.depth = parent.depth + 1;
                } else {
                    rootNodes.add(node);
                }
            } else {
                rootNodes.add(node);
            }
        }

        int total = allProgresses.size();
        int completed = (int) allProgresses.stream().filter(p -> p.completed).count();

        return new TreeChartData(rootNodes, total, completed);
    }

    private static List<CustomProgress> getAllProgressesFlat(List<CustomProgress> progresses) {
        List<CustomProgress> allProgresses = new ArrayList<>();
        for (CustomProgress progress : progresses) {
            allProgresses.add(progress);
            if (progress.subProgresses != null && !progress.subProgresses.isEmpty()) {
                allProgresses.addAll(getAllProgressesFlat(progress.subProgresses));
            }
        }
        return allProgresses;
    }

    public static void clearAllProgresses(Player player) {
        UUID playerId = player.getUUID();
        if (playerProgresses.containsKey(playerId)) {
            playerProgresses.get(playerId).clear();
            savePlayerProgress(player);

            PlayerStats stats = getPlayerStats(player);
            stats.completedProgresses = 0;
            stats.totalProgresses = 0;

            if (player instanceof ServerPlayer) {
                player.displayClientMessage(Component.literal(LanguageManager.getTranslation("simpleprogress.message.all_cleared")), false);
            }
        }
    }

    public static void clearCompletedProgresses(Player player) {
        UUID playerId = player.getUUID();
        if (playerProgresses.containsKey(playerId)) {
            List<CustomProgress> progresses = playerProgresses.get(playerId);
            int removedCount = clearCompletedRecursive(progresses);

            if (removedCount > 0) {
                savePlayerProgress(player);

                PlayerStats stats = getPlayerStats(player);
                stats.completedProgresses = Math.max(0, stats.completedProgresses - removedCount);
                stats.totalProgresses = Math.max(0, stats.totalProgresses - removedCount);

                if (player instanceof ServerPlayer) {
                    player.displayClientMessage(Component.literal(LanguageManager.getTranslation("simpleprogress.message.cleared_completed", removedCount)), false);
                }
            }
        }
    }

    private static int clearCompletedRecursive(List<CustomProgress> progresses) {
        int removedCount = 0;
        Iterator<CustomProgress> iterator = progresses.iterator();
        while (iterator.hasNext()) {
            CustomProgress progress = iterator.next();
            if (progress.completed) {
                iterator.remove();
                removedCount++;
            } else if (progress.subProgresses != null && !progress.subProgresses.isEmpty()) {
                removedCount += clearCompletedRecursive(progress.subProgresses);
            }
        }
        return removedCount;
    }

    private static String getDimensionDisplayName(String dimension) {
        switch(dimension) {
            case "minecraft:overworld":
                return LanguageManager.getCurrentLanguage().equals("zh_cn") ? "主世界" : "Overworld";
            case "minecraft:the_nether":
                return LanguageManager.getCurrentLanguage().equals("zh_cn") ? "下界" : "Nether";
            case "minecraft:the_end":
                return LanguageManager.getCurrentLanguage().equals("zh_cn") ? "末地" : "End";
            default: return dimension;
        }
    }

    private static String getEnchantmentDisplayName(String enchantmentId) {
        if (LanguageManager.getCurrentLanguage().equals("zh_cn")) {
            switch(enchantmentId) {
                case "minecraft:sharpness": return "锋利";
                case "minecraft:protection": return "保护";
                case "minecraft:fire_protection": return "火焰保护";
                case "minecraft:feather_falling": return "摔落保护";
                case "minecraft:blast_protection": return "爆炸保护";
                case "minecraft:projectile_protection": return "弹射物保护";
                case "minecraft:respiration": return "水下呼吸";
                case "minecraft:aqua_affinity": return "水下速掘";
                case "minecraft:thorns": return "荆棘";
                case "minecraft:depth_strider": return "深海探索者";
                case "minecraft:frost_walker": return "冰霜行者";
                case "minecraft:efficiency": return "效率";
                case "minecraft:silk_touch": return "精准采集";
                case "minecraft:unbreaking": return "耐久";
                case "minecraft:fortune": return "时运";
                case "minecraft:power": return "力量";
                case "minecraft:punch": return "冲击";
                case "minecraft:flame": return "火矢";
                case "minecraft:infinity": return "无限";
                case "minecraft:luck_of_the_sea": return "海之眷顾";
                case "minecraft:lure": return "饵钓";
                default: return enchantmentId;
            }
        } else {
            return enchantmentId.replace("minecraft:", "");
        }
    }

    public static void savePlayerProgress(Player player) {
        if (player.level().isClientSide) return;

        try {
            Path progressFile = getProgressFilePath(player);
            List<CustomProgress> progresses = playerProgresses.get(player.getUUID());

            if (progresses == null) return;

            Files.createDirectories(progressFile.getParent());

            CompoundTag rootTag = new CompoundTag();
            ListTag progressList = new ListTag();

            for (CustomProgress progress : progresses) {
                progressList.add(progress.serialize());
            }

            rootTag.put("progresses", progressList);
            rootTag.putLong("lastSaved", System.currentTimeMillis());

            PlayerStats stats = getPlayerStats(player);
            CompoundTag statsTag = new CompoundTag();
            statsTag.putInt("completedProgresses", stats.completedProgresses);
            statsTag.putInt("totalProgresses", stats.totalProgresses);
            rootTag.put("stats", statsTag);

            File file = progressFile.toFile();
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                NbtIo.writeCompressed(rootTag, outputStream);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to save player progress: " + player.getScoreboardName(), e);
        }
    }

    public static void loadPlayerProgress(Player player) {
        if (player.level().isClientSide) return;

        try {
            Path progressFile = getProgressFilePath(player);
            File file = progressFile.toFile();

            if (!file.exists()) {
                playerProgresses.put(player.getUUID(), new ArrayList<>());
                playerStats.put(player.getUUID(), new PlayerStats());
                return;
            }

            CompoundTag rootTag;
            try (FileInputStream inputStream = new FileInputStream(file)) {
                rootTag = NbtIo.readCompressed(inputStream);
            }

            ListTag progressList = rootTag.getList("progresses", 10);
            List<CustomProgress> progresses = new ArrayList<>();

            for (int i = 0; i < progressList.size(); i++) {
                CompoundTag progressTag = progressList.getCompound(i);
                CustomProgress progress = new CustomProgress();
                progress.deserialize(progressTag);
                progresses.add(progress);
            }

            playerProgresses.put(player.getUUID(), progresses);

            if (rootTag.contains("stats")) {
                CompoundTag statsTag = rootTag.getCompound("stats");
                PlayerStats stats = new PlayerStats();
                stats.completedProgresses = statsTag.getInt("completedProgresses");
                stats.totalProgresses = statsTag.getInt("totalProgresses");
                playerStats.put(player.getUUID(), stats);
            } else {
                playerStats.put(player.getUUID(), new PlayerStats());
            }

        } catch (Exception e) {
            LOGGER.error("Failed to load player progress: " + player.getScoreboardName(), e);
            playerProgresses.put(player.getUUID(), new ArrayList<>());
            playerStats.put(player.getUUID(), new PlayerStats());
        }
    }

    private static Path getProgressFilePath(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            Path worldPath = serverPlayer.server.getWorldPath(LevelResource.ROOT);
            return worldPath.resolve("simpleprogress")
                    .resolve(player.getUUID() + ".dat");
        }
        return null;
    }

    public static class PlayerStats {
        public int completedProgresses = 0;
        public int totalProgresses = 0;
        public float getCompletionRate() {
            return totalProgresses > 0 ? (float) completedProgresses / totalProgresses : 0;
        }
    }

    public static class CustomProgress {
        public String id;
        public String parentId;
        public String title;
        public String description;
        public ProgressType type;
        public String target;
        public int current;
        public int targetCount;
        public boolean completed;
        public long createdTime;
        public long estimatedCompletionTime;
        public List<CustomProgress> subProgresses;

        public CustomProgress() {
            this.id = UUID.randomUUID().toString();
            this.createdTime = System.currentTimeMillis();
            this.subProgresses = new ArrayList<>();
        }

        public float getProgress() {
            return targetCount > 0 ? (float) current / targetCount : 0;
        }

        public String getEstimatedTime() {
            if (current == 0 || completed) return LanguageManager.getCurrentLanguage().equals("zh_cn") ? "未知" : "Unknown";
            long elapsed = System.currentTimeMillis() - createdTime;
            long totalEstimated = (long) (elapsed / getProgress());
            long remaining = totalEstimated - elapsed;

            if (remaining < 60000) return LanguageManager.getCurrentLanguage().equals("zh_cn") ? "小于1分钟" : "Less than 1 minute";
            long minutes = remaining / 60000;
            if (minutes < 60) return minutes + (LanguageManager.getCurrentLanguage().equals("zh_cn") ? "分钟" : " minutes");
            long hours = minutes / 60;
            return hours + (LanguageManager.getCurrentLanguage().equals("zh_cn") ? "小时" : " hours");
        }

        public boolean hasSubProgresses() {
            return subProgresses != null && !subProgresses.isEmpty();
        }

        public CompoundTag serialize() {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", id);
            tag.putString("parentId", parentId != null ? parentId : "");
            tag.putString("title", title);
            tag.putString("description", description);
            tag.putString("type", type.name());
            tag.putString("target", target);
            tag.putInt("current", current);
            tag.putInt("targetCount", targetCount);
            tag.putBoolean("completed", completed);
            tag.putLong("createdTime", createdTime);
            tag.putLong("estimatedCompletionTime", estimatedCompletionTime);

            ListTag subProgressList = new ListTag();
            if (subProgresses != null) {
                for (CustomProgress subProgress : subProgresses) {
                    subProgressList.add(subProgress.serialize());
                }
            }
            tag.put("subProgresses", subProgressList);

            return tag;
        }

        public void deserialize(CompoundTag tag) {
            id = tag.getString("id");
            parentId = tag.getString("parentId");
            if (parentId.isEmpty()) parentId = null;
            title = tag.getString("title");
            description = tag.getString("description");
            type = ProgressType.valueOf(tag.getString("type"));
            target = tag.getString("target");
            current = tag.getInt("current");
            targetCount = tag.getInt("targetCount");
            completed = tag.getBoolean("completed");
            createdTime = tag.getLong("createdTime");
            estimatedCompletionTime = tag.getLong("estimatedCompletionTime");

            subProgresses = new ArrayList<>();
            if (tag.contains("subProgresses")) {
                ListTag subProgressList = tag.getList("subProgresses", 10);
                for (int i = 0; i < subProgressList.size(); i++) {
                    CompoundTag subProgressTag = subProgressList.getCompound(i);
                    CustomProgress subProgress = new CustomProgress();
                    subProgress.deserialize(subProgressTag);
                    subProgresses.add(subProgress);
                }
            }
        }
    }

    public enum ProgressType {
        KILL(LanguageManager.getTranslation("simpleprogress.type.kill"), "§c"),
        OBTAIN(LanguageManager.getTranslation("simpleprogress.type.obtain"), "§a"),
        EXPLORE(LanguageManager.getTranslation("simpleprogress.type.explore"), "§9"),
        BUILD(LanguageManager.getTranslation("simpleprogress.type.build"), "§6"),
        ENCHANT(LanguageManager.getTranslation("simpleprogress.type.enchant"), "§5");

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
}