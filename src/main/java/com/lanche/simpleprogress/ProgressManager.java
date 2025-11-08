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

    // 进度统计
    private static final Map<UUID, PlayerStats> playerStats = new ConcurrentHashMap<>();

    public static List<CustomProgress> getProgresses(Player player) {
        UUID playerId = player.getUUID();

        // 如果内存中没有数据，尝试从文件加载
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
        for (CustomProgress progress : progresses) {
            if (progress.id.equals(parentId)) {
                if (progress.subProgresses == null) {
                    progress.subProgresses = new ArrayList<>();
                }
                subProgress.parentId = parentId;
                progress.subProgresses.add(subProgress);
                savePlayerProgress(player);
                break;
            }
        }
    }

    public static void removeProgress(Player player, String id) {
        List<CustomProgress> progresses = getProgresses(player);
        boolean removed = progresses.removeIf(p -> p.id.equals(id));
        if (removed) {
            savePlayerProgress(player);
        }
    }

    public static void updateProgress(Player player, String target, ProgressType type) {
        List<CustomProgress> progresses = getProgresses(player);
        boolean changed = false;

        for (CustomProgress progress : progresses) {
            if (progress.type == type && progress.target.equals(target) && !progress.completed) {
                boolean wasNotCompleted = !progress.completed;
                progress.current++;
                if (progress.current >= progress.targetCount) {
                    progress.completed = true;
                    // 更新统计
                    getPlayerStats(player).completedProgresses++;

                    // 进度完成时在聊天栏显示消息
                    if (wasNotCompleted && player instanceof ServerPlayer) {
                        String completionMessage = String.format("§6【进度完成】§a %s §7- §e%d/%d §7(100%%)",
                                progress.title, progress.current, progress.targetCount);
                        player.displayClientMessage(Component.literal(completionMessage), false);

                        // 显示HUD通知
                        ProgressHUD.showProgressUpdate(completionMessage);
                    }
                } else {
                    // 进度更新时显示当前进度（可选，避免刷屏）
                    if (progress.current % Math.max(1, progress.targetCount / 5) == 0) { // 每完成20%显示一次
                        float percentage = (float) progress.current / progress.targetCount * 100;
                        String updateMessage = String.format("§7【进度更新】§a %s §7- §e%d/%d §7(%.1f%%)",
                                progress.title, progress.current, progress.targetCount, percentage);
                        player.displayClientMessage(Component.literal(updateMessage), false);
                    }
                }
                changed = true;
            }
        }

        if (changed) {
            savePlayerProgress(player);
        }
    }

    // 新方法：更新探索进度
    public static void updateExploreProgress(Player player, String dimension, int x, int z) {
        List<CustomProgress> progresses = getProgresses(player);
        boolean changed = false;

        for (CustomProgress progress : progresses) {
            if (progress.type == ProgressType.EXPLORE && progress.target.equals(dimension) && !progress.completed) {
                // 对于探索进度，我们只需要到达一次
                progress.current = 1;
                progress.completed = true;

                // 更新统计
                getPlayerStats(player).completedProgresses++;

                String completionMessage = String.format("§6【探索完成】§a %s §7- 到达 §e%s",
                        progress.title, getDimensionDisplayName(dimension));
                player.displayClientMessage(Component.literal(completionMessage), false);
                ProgressHUD.showProgressUpdate(completionMessage);
                changed = true;
            }
        }

        if (changed) {
            savePlayerProgress(player);
        }
    }

    // 新方法：更新建筑进度
    public static void updateBuildProgress(Player player, String blockId) {
        List<CustomProgress> progresses = getProgresses(player);
        boolean changed = false;

        for (CustomProgress progress : progresses) {
            if (progress.type == ProgressType.BUILD && progress.target.equals(blockId) && !progress.completed) {
                progress.current++;
                if (progress.current >= progress.targetCount) {
                    progress.completed = true;
                    getPlayerStats(player).completedProgresses++;

                    String completionMessage = String.format("§6【建筑完成】§a %s §7- §e%d/%d §7(100%%)",
                            progress.title, progress.current, progress.targetCount);
                    player.displayClientMessage(Component.literal(completionMessage), false);
                    ProgressHUD.showProgressUpdate(completionMessage);
                }
                changed = true;
            }
        }

        if (changed) {
            savePlayerProgress(player);
        }
    }

    // 新方法：更新附魔进度
    public static void updateEnchantProgress(Player player, String enchantmentId) {
        List<CustomProgress> progresses = getProgresses(player);
        boolean changed = false;

        for (CustomProgress progress : progresses) {
            if (progress.type == ProgressType.ENCHANT && progress.target.equals(enchantmentId) && !progress.completed) {
                // 对于附魔进度，我们只需要获得一次
                progress.current = 1;
                progress.completed = true;

                // 更新统计
                getPlayerStats(player).completedProgresses++;

                String completionMessage = String.format("§6【附魔完成】§a %s §7- 获得 §e%s",
                        progress.title, getEnchantmentDisplayName(enchantmentId));
                player.displayClientMessage(Component.literal(completionMessage), false);
                ProgressHUD.showProgressUpdate(completionMessage);
                changed = true;
            }
        }

        if (changed) {
            savePlayerProgress(player);
        }
    }

    // 树状图数据结构
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
        public int depth; // 移除 final 修饰符

        public TreeNode(CustomProgress progress, int depth) {
            this.progress = progress;
            this.children = new ArrayList<>();
            this.depth = depth; // 通过构造函数设置
        }
    }

    // 获取树状图数据的方法
    public static TreeChartData getTreeChartData(Player player) {
        List<CustomProgress> progresses = getProgresses(player);
        List<TreeNode> rootNodes = new ArrayList<>();

        // 构建树状结构
        Map<String, TreeNode> nodeMap = new HashMap<>();
        List<TreeNode> allNodes = new ArrayList<>();

        // 第一遍：创建所有节点
        for (CustomProgress progress : progresses) {
            TreeNode node = new TreeNode(progress, 0); // 初始深度为0
            nodeMap.put(progress.id, node);
            allNodes.add(node);
        }

        // 第二遍：建立父子关系
        for (TreeNode node : allNodes) {
            if (node.progress.parentId != null && !node.progress.parentId.isEmpty()) {
                TreeNode parent = nodeMap.get(node.progress.parentId);
                if (parent != null) {
                    parent.children.add(node);
                    node.depth = parent.depth + 1; // 现在可以赋值，因为移除了final
                } else {
                    rootNodes.add(node);
                }
            } else {
                rootNodes.add(node);
            }
        }

        // 计算统计数据
        int total = progresses.size();
        int completed = (int) progresses.stream().filter(p -> p.completed).count();

        return new TreeChartData(rootNodes, total, completed);
    }

    // 清除所有进度的方法
    public static void clearAllProgresses(Player player) {
        UUID playerId = player.getUUID();
        if (playerProgresses.containsKey(playerId)) {
            playerProgresses.get(playerId).clear();
            savePlayerProgress(player);

            // 重置统计
            PlayerStats stats = getPlayerStats(player);
            stats.completedProgresses = 0;
            stats.totalProgresses = 0;
        }
    }

    // 清除已完成进度的方法
    public static void clearCompletedProgresses(Player player) {
        UUID playerId = player.getUUID();
        if (playerProgresses.containsKey(playerId)) {
            List<CustomProgress> progresses = playerProgresses.get(playerId);
            int removedCount = 0;

            // 使用迭代器安全删除
            Iterator<CustomProgress> iterator = progresses.iterator();
            while (iterator.hasNext()) {
                CustomProgress progress = iterator.next();
                if (progress.completed) {
                    iterator.remove();
                    removedCount++;
                }
            }

            if (removedCount > 0) {
                savePlayerProgress(player);

                // 更新统计
                PlayerStats stats = getPlayerStats(player);
                stats.completedProgresses = Math.max(0, stats.completedProgresses - removedCount);
                stats.totalProgresses = Math.max(0, stats.totalProgresses - removedCount);

                if (player instanceof ServerPlayer) {
                    player.displayClientMessage(Component.literal("§a已清除 " + removedCount + " 个已完成进度"), false);
                }
            }
        }
    }

    private static String getDimensionDisplayName(String dimension) {
        switch(dimension) {
            case "minecraft:overworld": return "主世界";
            case "minecraft:the_nether": return "下界";
            case "minecraft:the_end": return "末地";
            default: return dimension;
        }
    }

    private static String getEnchantmentDisplayName(String enchantmentId) {
        // 这里可以添加更多的附魔显示名称映射
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
    }

    // 保存玩家进度到文件
    public static void savePlayerProgress(Player player) {
        // 修复：使用 level() 方法而不是直接访问 level 字段
        if (player.level().isClientSide) return; // 只在服务端保存

        try {
            Path progressFile = getProgressFilePath(player);
            List<CustomProgress> progresses = playerProgresses.get(player.getUUID());

            if (progresses == null) return;

            // 创建父目录
            Files.createDirectories(progressFile.getParent());

            // 转换为NBT格式保存
            CompoundTag rootTag = new CompoundTag();
            ListTag progressList = new ListTag();

            for (CustomProgress progress : progresses) {
                progressList.add(progress.serialize());
            }

            rootTag.put("progresses", progressList);
            rootTag.putLong("lastSaved", System.currentTimeMillis());

            // 保存统计
            PlayerStats stats = getPlayerStats(player);
            CompoundTag statsTag = new CompoundTag();
            statsTag.putInt("completedProgresses", stats.completedProgresses);
            statsTag.putInt("totalProgresses", stats.totalProgresses);
            rootTag.put("stats", statsTag);

            // 写入文件 - 使用 File 而不是 Path
            File file = progressFile.toFile();
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                NbtIo.writeCompressed(rootTag, outputStream);
            }

        } catch (Exception e) {
            LOGGER.error("保存玩家进度失败: " + player.getScoreboardName(), e);
        }
    }

    // 从文件加载玩家进度
    public static void loadPlayerProgress(Player player) {
        // 修复：使用 level() 方法而不是直接访问 level 字段
        if (player.level().isClientSide) return; // 只在服务端加载

        try {
            Path progressFile = getProgressFilePath(player);
            File file = progressFile.toFile();

            if (!file.exists()) {
                playerProgresses.put(player.getUUID(), new ArrayList<>());
                playerStats.put(player.getUUID(), new PlayerStats());
                return;
            }

            // 读取文件
            CompoundTag rootTag;
            try (FileInputStream inputStream = new FileInputStream(file)) {
                rootTag = NbtIo.readCompressed(inputStream);
            }

            ListTag progressList = rootTag.getList("progresses", 10); // 10 表示 CompoundTag
            List<CustomProgress> progresses = new ArrayList<>();

            for (int i = 0; i < progressList.size(); i++) {
                CompoundTag progressTag = progressList.getCompound(i);
                CustomProgress progress = new CustomProgress();
                progress.deserialize(progressTag);
                progresses.add(progress);
            }

            playerProgresses.put(player.getUUID(), progresses);

            // 加载统计
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
            LOGGER.error("加载玩家进度失败: " + player.getScoreboardName(), e);
            playerProgresses.put(player.getUUID(), new ArrayList<>());
            playerStats.put(player.getUUID(), new PlayerStats());
        }
    }

    // 获取进度文件路径
    private static Path getProgressFilePath(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            // 保存在世界目录下的 simpleprogress 文件夹中
            Path worldPath = serverPlayer.server.getWorldPath(LevelResource.ROOT);
            return worldPath.resolve("simpleprogress")
                    .resolve(player.getUUID() + ".dat");
        }
        return null;
    }

    // 玩家登出时保存数据
    public static void onPlayerLogout(Player player) {
        savePlayerProgress(player);
    }

    // 玩家登录时加载数据
    public static void onPlayerLogin(Player player) {
        loadPlayerProgress(player);
    }

    // 玩家统计类
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
            if (current == 0 || completed) return "未知";
            long elapsed = System.currentTimeMillis() - createdTime;
            long totalEstimated = (long) (elapsed / getProgress());
            long remaining = totalEstimated - elapsed;

            if (remaining < 60000) return "小于1分钟";
            long minutes = remaining / 60000;
            if (minutes < 60) return minutes + "分钟";
            long hours = minutes / 60;
            return hours + "小时";
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

            // 序列化子进度
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

            // 反序列化子进度
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
        KILL("击杀", "§c"),
        OBTAIN("获得", "§a"),
        EXPLORE("探索", "§9"),
        BUILD("建筑", "§6"),
        ENCHANT("附魔", "§5");

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