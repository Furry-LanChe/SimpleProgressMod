package com.lanche.simpleprogress;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProgressCommand {

    private static final Map<UUID, String> playerLanguages = new HashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // 主命令 /progress
        dispatcher.register(Commands.literal("progress")
                .executes(context -> {
                    sendHelpMessage(context);
                    return 1;
                })
                .then(Commands.literal("help")
                        .executes(context -> {
                            sendHelpMessage(context);
                            return 1;
                        })
                )
                .then(Commands.literal("add")
                        .requires(source -> source.isPlayer())
                        .then(Commands.argument("title", StringArgumentType.greedyString())
                                .executes(context -> addProgress(
                                        context,
                                        StringArgumentType.getString(context, "title"),
                                        "minecraft:zombie",
                                        10,
                                        ProgressManager.ProgressType.KILL
                                ))
                                .then(Commands.argument("target", StringArgumentType.string())
                                        .executes(context -> addProgress(
                                                context,
                                                StringArgumentType.getString(context, "title"),
                                                StringArgumentType.getString(context, "target"),
                                                10,
                                                ProgressManager.ProgressType.KILL
                                        ))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                .executes(context -> addProgress(
                                                        context,
                                                        StringArgumentType.getString(context, "title"),
                                                        StringArgumentType.getString(context, "target"),
                                                        IntegerArgumentType.getInteger(context, "count"),
                                                        ProgressManager.ProgressType.KILL
                                                ))
                                                .then(Commands.literal("kill")
                                                        .executes(context -> addProgress(
                                                                context,
                                                                StringArgumentType.getString(context, "title"),
                                                                StringArgumentType.getString(context, "target"),
                                                                IntegerArgumentType.getInteger(context, "count"),
                                                                ProgressManager.ProgressType.KILL
                                                        ))
                                                )
                                                .then(Commands.literal("obtain")
                                                        .executes(context -> addProgress(
                                                                context,
                                                                StringArgumentType.getString(context, "title"),
                                                                StringArgumentType.getString(context, "target"),
                                                                IntegerArgumentType.getInteger(context, "count"),
                                                                ProgressManager.ProgressType.OBTAIN
                                                        ))
                                                )
                                                .then(Commands.literal("build")
                                                        .executes(context -> addProgress(
                                                                context,
                                                                StringArgumentType.getString(context, "title"),
                                                                StringArgumentType.getString(context, "target"),
                                                                IntegerArgumentType.getInteger(context, "count"),
                                                                ProgressManager.ProgressType.BUILD
                                                        ))
                                                )
                                        )
                                )
                        )
                )
                .then(Commands.literal("list")
                        .requires(source -> source.isPlayer())
                        .executes(context -> listAllProgresses(context))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(context -> listProgressesByPage(
                                        context,
                                        IntegerArgumentType.getInteger(context, "page")
                                ))
                        )
                )
                .then(Commands.literal("view")
                        .requires(source -> source.isPlayer())
                        .then(Commands.argument("id", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player != null) {
                                        List<ProgressManager.CustomProgress> progresses = ProgressManager.getPlayerData(player);
                                        for (ProgressManager.CustomProgress progress : progresses) {
                                            builder.suggest(progress.id);
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> viewProgress(
                                        context,
                                        StringArgumentType.getString(context, "id")
                                ))
                        )
                )
                .then(Commands.literal("update")
                        .requires(source -> source.isPlayer())
                        .then(Commands.argument("id", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player != null) {
                                        List<ProgressManager.CustomProgress> progresses = ProgressManager.getPlayerData(player);
                                        for (ProgressManager.CustomProgress progress : progresses) {
                                            builder.suggest(progress.id);
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("current", IntegerArgumentType.integer(0))
                                        .executes(context -> updateProgress(
                                                context,
                                                StringArgumentType.getString(context, "id"),
                                                IntegerArgumentType.getInteger(context, "current")
                                        ))
                                )
                        )
                )
                .then(Commands.literal("delete")
                        .requires(source -> source.isPlayer())
                        .then(Commands.argument("id", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player != null) {
                                        List<ProgressManager.CustomProgress> progresses = ProgressManager.getPlayerData(player);
                                        for (ProgressManager.CustomProgress progress : progresses) {
                                            builder.suggest(progress.id);
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> deleteProgress(
                                        context,
                                        StringArgumentType.getString(context, "id")
                                ))
                        )
                )
                .then(Commands.literal("clear")
                        .requires(source -> source.isPlayer())
                        .executes(context -> clearProgresses(context))
                        .then(Commands.literal("confirm")
                                .executes(context -> confirmClearProgresses(context))
                        )
                )
                .then(Commands.literal("stats")
                        .requires(source -> source.isPlayer())
                        .executes(context -> showStats(context))
                )
                .then(Commands.literal("lang")
                        .requires(source -> source.isPlayer())
                        .executes(context -> showCurrentLanguage(context))
                        .then(Commands.literal("en_us")
                                .executes(context -> setLanguage(context, "en_us"))
                        )
                        .then(Commands.literal("zh_cn")
                                .executes(context -> setLanguage(context, "zh_cn"))
                        )
                        .then(Commands.literal("reset")
                                .executes(context -> resetLanguage(context))
                        )
                )
        );

        // 快捷命令 /prog
        dispatcher.register(Commands.literal("prog")
                .executes(context -> {
                    sendHelpMessage(context);
                    return 1;
                })
                .then(Commands.literal("list")
                        .requires(source -> source.isPlayer())
                        .executes(context -> listAllProgresses(context))
                )
        );
    }

    private static int addProgress(CommandContext<CommandSourceStack> context,
                                   String title, String target, int count,
                                   ProgressManager.ProgressType type) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player != null) {
            try {
                ProgressManager.CustomProgress progress = new ProgressManager.CustomProgress();
                progress.title = title;
                progress.type = type;
                progress.target = target;
                progress.targetCount = count;
                progress.current = 0;
                progress.completed = false;

                ProgressManager.addProgress(player, progress);

                String typeDisplayName = getTypeDisplayName(type, player.getUUID());
                String message = getPlayerLanguage(player.getUUID()).equals("zh_cn") ?
                        "§a✓ §7进度已添加: §f" + title + "\n§7ID: §e" + progress.id.substring(0, 8) + "..." +
                                "\n§7类型: " + type.getColorCode() + typeDisplayName + "\n§7目标: §a" + target + " §7x§e" + count +
                                "\n§7进度: §e0§7/§a" + count + "\n§7使用 §e/progress view " + progress.id + " §7查看详情" :
                        "§a✓ §7Progress added: §f" + title + "\n§7ID: §e" + progress.id.substring(0, 8) + "..." +
                                "\n§7Type: " + type.getColorCode() + typeDisplayName + "\n§7Target: §a" + target + " §7x§e" + count +
                                "\n§7Progress: §e0§7/§a" + count + "\n§7Use §e/progress view " + progress.id + " §7to view details";

                source.sendSuccess(() -> Component.literal(message), false);
                return 1;
            } catch (Exception e) {
                String errorMsg = getPlayerLanguage(source.getPlayer().getUUID()).equals("zh_cn") ?
                        "§c✗ §7添加进度失败: " + e.getMessage() :
                        "§c✗ §7Failed to add progress: " + e.getMessage();
                source.sendFailure(Component.literal(errorMsg));
                return 0;
            }
        }

        source.sendFailure(Component.literal("§c只有玩家可以使用此命令"));
        return 0;
    }

    private static int listAllProgresses(CommandContext<CommandSourceStack> context) {
        return listProgressesByPage(context, 1);
    }

    private static int listProgressesByPage(CommandContext<CommandSourceStack> context, int page) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player != null) {
            List<ProgressManager.CustomProgress> progresses = ProgressManager.getPlayerData(player);

            String lang = getPlayerLanguage(player.getUUID());
            boolean isChinese = lang.equals("zh_cn");

            if (progresses.isEmpty()) {
                String message = isChinese ?
                        "§a[SimpleProgress] §7你还没有任何进度记录\n§7使用 §e/progress add <标题> §7添加进度" :
                        "§a[SimpleProgress] §7You don't have any progress records\n§7Use §e/progress add <title> §7to add progress";
                source.sendSuccess(() -> Component.literal(message), false);
                return 1;
            }

            int pageSize = 8;
            int totalPages = (progresses.size() + pageSize - 1) / pageSize;
            page = Math.min(Math.max(1, page), totalPages);

            int startIndex = (page - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, progresses.size());

            // 顶部信息
            String header = isChinese ?
                    "§6=== 进度列表 (§e" + progresses.size() + "§6) 第§e" + page + "§6/§a" + totalPages + "§6页 ===" :
                    "§6=== Progress List (§e" + progresses.size() + "§6) Page §e" + page + "§6/§a" + totalPages + "§6 ===";

            String info = isChinese ?
                    "§7使用 §e/progress view <ID> §7查看详细信息" :
                    "§7Use §e/progress view <ID> §7to view details";

            source.sendSuccess(() -> Component.literal(header), false);
            source.sendSuccess(() -> Component.literal(info), false);

            // 列表项
            for (int i = startIndex; i < endIndex; i++) {
                var progress = progresses.get(i);
                String statusIcon = progress.completed ? "§a✓" : "§e⏳";
                String progressBar = createProgressBar(progress.current, progress.targetCount, 20);
                String percentage = String.format("%.1f%%", progress.getProgress() * 100);
                String typeDisplayName = getTypeDisplayName(progress.type, player.getUUID());

                MutableComponent message = Component.literal(statusIcon + " §7" + (i + 1) + ". §f" + progress.title)
                        .append(Component.literal(" §7[" + progress.type.getColorCode() + typeDisplayName + "§7]"))
                        .append(Component.literal("\n   §7进度: " + progressBar + " §e" + percentage))
                        .append(Component.literal("\n   §7ID: §e" + progress.id.substring(0, 8) + "..."))
                        .append(Component.literal("\n   §7目标: §a" + progress.target + " §7x§e" + progress.targetCount))
                        .append(Component.literal("\n   §7完成: §e" + progress.current + "§7/§a" + progress.targetCount))
                        .append(Component.literal("\n   §7操作: ")
                                .append(Component.literal("§a[查看]")
                                        .withStyle(Style.EMPTY.withClickEvent(
                                                new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/progress view " + progress.id)
                                        ).withHoverEvent(
                                                new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                        Component.literal(isChinese ? "点击查看详情" : "Click to view details"))
                                        )))
                                .append(Component.literal(" §c[删除]")
                                        .withStyle(Style.EMPTY.withClickEvent(
                                                new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/progress delete " + progress.id)
                                        ).withHoverEvent(
                                                new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                        Component.literal(isChinese ? "点击删除进度" : "Click to delete progress"))
                                        )))
                        );

                source.sendSuccess(() -> message, false);
            }

            // 分页导航
            if (totalPages > 1) {
                MutableComponent navigation = Component.literal(isChinese ? "§7页面: " : "§7Page: ");
                if (page > 1) {
                    String prevText = isChinese ? "§e[上一页]" : "§e[Previous]";
                    int finalPage = page;
                    navigation.append(Component.literal(prevText)
                            .withStyle(Style.EMPTY.withClickEvent(
                                    new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/progress list " + (finalPage - 1))
                            ).withHoverEvent(
                                    new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                            Component.literal(isChinese ? "点击查看上一页" : "Click to view previous page"))
                            )));
                }

                for (int i = 1; i <= totalPages; i++) {
                    if (i == page) {
                        navigation.append(Component.literal(" §a[" + i + "] "));
                    } else {
                        int finalI = i;
                        navigation.append(Component.literal(" §7[" + i + "]")
                                .withStyle(Style.EMPTY.withClickEvent(
                                        new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/progress list " + finalI)
                                ).withHoverEvent(
                                        new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                Component.literal(isChinese ? "点击查看第" + finalI + "页" : "Click to view page " + finalI))
                                )));
                    }
                }

                if (page < totalPages) {
                    String nextText = isChinese ? "§e[下一页]" : "§e[Next]";
                    int finalPage1 = page;
                    navigation.append(Component.literal(nextText)
                            .withStyle(Style.EMPTY.withClickEvent(
                                    new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/progress list " + (finalPage1 + 1))
                            ).withHoverEvent(
                                    new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                            Component.literal(isChinese ? "点击查看下一页" : "Click to view next page"))
                            )));
                }

                source.sendSuccess(() -> navigation, false);
            }

            return 1;
        }

        source.sendFailure(Component.literal("§c只有玩家可以使用此命令"));
        return 0;
    }

    private static int viewProgress(CommandContext<CommandSourceStack> context, String progressId) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player != null) {
            List<ProgressManager.CustomProgress> progresses = ProgressManager.getPlayerData(player);
            ProgressManager.CustomProgress progress = progresses.stream().filter(p -> p.id.equals(progressId)).findFirst().orElse(null);

            if (progress == null) {
                String lang = getPlayerLanguage(player.getUUID());
                String errorMsg = lang.equals("zh_cn") ?
                        "§c✗ §7未找到ID为 §e" + progressId + " §7的进度" :
                        "§c✗ §7Progress not found with ID: §e" + progressId;
                source.sendFailure(Component.literal(errorMsg));
                return 0;
            }

            String lang = getPlayerLanguage(player.getUUID());
            boolean isChinese = lang.equals("zh_cn");

            String status = progress.completed ?
                    (isChinese ? "§a已完成" : "§aCompleted") :
                    (isChinese ? "§e进行中" : "§eIn Progress");
            String progressBar = createProgressBar(progress.current, progress.targetCount, 30);
            String percentage = String.format("%.1f%%", progress.getProgress() * 100);
            long createdTime = progress.createdTime;
            String timeAgo = formatTimeAgo(createdTime, isChinese);
            String typeDisplayName = getTypeDisplayName(progress.type, player.getUUID());

            String header = isChinese ? "§6=== 进度详情 ===" : "§6=== Progress Details ===";
            source.sendSuccess(() -> Component.literal(header), false);

            MutableComponent details = Component.literal("§f" + progress.title)
                    .append(Component.literal("\n§7" + (isChinese ? "状态: " : "Status: ") + status))
                    .append(Component.literal("\n§7" + (isChinese ? "类型: " : "Type: ") + progress.type.getColorCode() + typeDisplayName))
                    .append(Component.literal("\n§7" + (isChinese ? "目标: " : "Target: ") + "§a" + progress.target + " §7x§e" + progress.targetCount))
                    .append(Component.literal("\n§7" + (isChinese ? "进度: " : "Progress: ") + progressBar))
                    .append(Component.literal("\n§7" + (isChinese ? "完成度: " : "Completion: ") + "§e" + percentage + " §7(§e" + progress.current + "§7/§a" + progress.targetCount + "§7)"))
                    .append(Component.literal("\n§7ID: §e" + progress.id))
                    .append(Component.literal("\n§7" + (isChinese ? "创建时间: " : "Created: ") + "§7" + timeAgo));

            source.sendSuccess(() -> details, false);

            // 操作按钮
            MutableComponent actions = Component.literal("§7" + (isChinese ? "操作: " : "Actions: "));

            if (!progress.completed) {
                actions.append(Component.literal("§a[+1] ")
                        .withStyle(Style.EMPTY.withClickEvent(
                                new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                        "/progress update " + progress.id + " " + (progress.current + 1))
                        ).withHoverEvent(
                                new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.literal(isChinese ? "点击增加进度" : "Click to increase progress"))
                        )));

                actions.append(Component.literal("§6[+5] ")
                        .withStyle(Style.EMPTY.withClickEvent(
                                new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                        "/progress update " + progress.id + " " + (progress.current + 5))
                        ).withHoverEvent(
                                new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.literal(isChinese ? "点击增加5点进度" : "Click to add 5 progress"))
                        )));

                actions.append(Component.literal("§c[" + (isChinese ? "删除" : "Delete") + "] ")
                        .withStyle(Style.EMPTY.withClickEvent(
                                new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/progress delete " + progress.id)
                        ).withHoverEvent(
                                new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.literal(isChinese ? "点击删除此进度" : "Click to delete this progress"))
                        )));
            } else {
                actions.append(Component.literal("§a[" + (isChinese ? "已完成" : "Completed") + "] "));
                actions.append(Component.literal("§c[" + (isChinese ? "删除" : "Delete") + "] ")
                        .withStyle(Style.EMPTY.withClickEvent(
                                new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/progress delete " + progress.id)
                        ).withHoverEvent(
                                new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.literal(isChinese ? "点击删除此进度" : "Click to delete this progress"))
                        )));
            }

            source.sendSuccess(() -> actions, false);

            return 1;
        }

        source.sendFailure(Component.literal("§c只有玩家可以使用此命令"));
        return 0;
    }

    private static int updateProgress(CommandContext<CommandSourceStack> context, String progressId, int current) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player != null) {
            List<ProgressManager.CustomProgress> progresses = ProgressManager.getPlayerData(player);
            ProgressManager.CustomProgress progress = null;

            for (var p : progresses) {
                if (p.id.equals(progressId)) {
                    progress = p;
                    break;
                }
            }

            if (progress == null) {
                String lang = getPlayerLanguage(player.getUUID());
                String errorMsg = lang.equals("zh_cn") ?
                        "§c✗ §7未找到ID为 §e" + progressId + " §7的进度" :
                        "§c✗ §7Progress not found with ID: §e" + progressId;
                source.sendFailure(Component.literal(errorMsg));
                return 0;
            }

            int oldCurrent = progress.current;
            progress.current = Math.min(Math.max(0, current), progress.targetCount);
            progress.completed = progress.current >= progress.targetCount;

            // 保存更新
            ProgressManager.removeProgress(player, progressId);
            ProgressManager.addProgress(player, progress);

            String lang = getPlayerLanguage(player.getUUID());
            boolean isChinese = lang.equals("zh_cn");

            String status = progress.completed ?
                    (isChinese ? "§a已完成！" : "§aCompleted!") :
                    (isChinese ? "§e更新成功" : "§eUpdated successfully");
            String progressBar = createProgressBar(progress.current, progress.targetCount, 20);
            String percentage = String.format("%.1f%%", progress.getProgress() * 100);
            String typeDisplayName = getTypeDisplayName(progress.type, player.getUUID());

            MutableComponent message = Component.literal("§a✓ " + (isChinese ? "§7进度已更新: " : "§7Progress updated: ") + "§f" + progress.title)
                    .append(Component.literal("\n§7" + (isChinese ? "类型: " : "Type: ") + progress.type.getColorCode() + typeDisplayName))
                    .append(Component.literal("\n§7" + (isChinese ? "进度: " : "Progress: ") + progressBar + " §e" + percentage))
                    .append(Component.literal("\n§7" + (isChinese ? "完成: " : "Completed: ") + "§e" + progress.current + "§7/§a" + progress.targetCount))
                    .append(Component.literal("\n§7" + (isChinese ? "变化: " : "Change: ") + "§e" + oldCurrent + " §7→ §a" + progress.current))
                    .append(Component.literal("\n§7" + (isChinese ? "状态: " : "Status: ") + status));

            source.sendSuccess(() -> message, false);

            if (progress.completed) {
                String congrats = isChinese ?
                        "§a🎉 恭喜！你完成了进度: §f" + progress.title :
                        "§a🎉 Congratulations! You completed progress: §f" + progress.title;
                source.sendSuccess(() -> Component.literal(congrats), false);
            }

            return 1;
        }

        source.sendFailure(Component.literal("§c只有玩家可以使用此命令"));
        return 0;
    }

    private static int deleteProgress(CommandContext<CommandSourceStack> context, String progressId) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player != null) {
            List<ProgressManager.CustomProgress> progresses = ProgressManager.getPlayerData(player);
            ProgressManager.CustomProgress progress = null;

            for (var p : progresses) {
                if (p.id.equals(progressId)) {
                    progress = p;
                    break;
                }
            }

            if (progress == null) {
                String lang = getPlayerLanguage(player.getUUID());
                String errorMsg = lang.equals("zh_cn") ?
                        "§c✗ §7未找到ID为 §e" + progressId + " §7的进度" :
                        "§c✗ §7Progress not found with ID: §e" + progressId;
                source.sendFailure(Component.literal(errorMsg));
                return 0;
            }

            ProgressManager.removeProgress(player, progressId);

            String lang = getPlayerLanguage(player.getUUID());
            String message = lang.equals("zh_cn") ?
                    "§a✓ §7已删除进度: §f" + progress.title :
                    "§a✓ §7Deleted progress: §f" + progress.title;

            source.sendSuccess(() -> Component.literal(message), false);
            return 1;
        }

        source.sendFailure(Component.literal("§c只有玩家可以使用此命令"));
        return 0;
    }

    private static int clearProgresses(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player != null) {
            var progresses = ProgressManager.getPlayerData(player);

            String lang = getPlayerLanguage(player.getUUID());
            boolean isChinese = lang.equals("zh_cn");

            if (progresses.isEmpty()) {
                String message = isChinese ?
                        "§a[SimpleProgress] §7你没有任何进度记录可清除" :
                        "§a[SimpleProgress] §7You don't have any progress records to clear";
                source.sendSuccess(() -> Component.literal(message), false);
                return 1;
            }

            source.sendSuccess(() -> Component.literal("§c⚠ " + (isChinese ? "§7警告：此操作将清除所有进度数据！" : "§7Warning: This will clear all progress data!")), false);
            source.sendSuccess(() -> Component.literal("§7" + (isChinese ? "当前有 §e" : "You have §e") + progresses.size() + (isChinese ? " §7个进度记录" : " §7progress records")), false);
            source.sendSuccess(() -> Component.literal("§7" + (isChinese ? "使用 §e/progress clear confirm §7来确认清除" : "Use §e/progress clear confirm §7to confirm")), false);
            return 1;
        }

        source.sendFailure(Component.literal("§c只有玩家可以使用此命令"));
        return 0;
    }

    private static int confirmClearProgresses(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player != null) {
            var progresses = ProgressManager.getPlayerData(player);
            int count = progresses.size();

            ProgressManager.clearAllProgresses(player);

            String lang = getPlayerLanguage(player.getUUID());
            String message = lang.equals("zh_cn") ?
                    "§a✓ §7已清除 §e" + count + " §7个进度记录" :
                    "§a✓ §7Cleared §e" + count + " §7progress records";

            source.sendSuccess(() -> Component.literal(message), false);
            return 1;
        }

        source.sendFailure(Component.literal("§c只有玩家可以使用此命令"));
        return 0;
    }

    private static int showStats(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player != null) {
            var progresses = ProgressManager.getPlayerData(player);

            int total = progresses.size();
            int completed = 0;
            int killCount = 0, obtainCount = 0, buildCount = 0;
            int totalProgress = 0, totalTarget = 0;

            for (var progress : progresses) {
                if (progress.completed) completed++;

                switch (progress.type) {
                    case KILL: killCount++; break;
                    case OBTAIN: obtainCount++; break;
                    case BUILD: buildCount++; break;
                }

                totalProgress += progress.current;
                totalTarget += progress.targetCount;
            }

            float completionRate = total > 0 ? (float) completed / total * 100 : 0;
            float overallProgress = totalTarget > 0 ? (float) totalProgress / totalTarget * 100 : 0;

            String lang = getPlayerLanguage(player.getUUID());
            boolean isChinese = lang.equals("zh_cn");

            String header = isChinese ? "§6=== 进度统计 ===" : "§6=== Progress Statistics ===";
            source.sendSuccess(() -> Component.literal(header), false);

            // 创建final变量供lambda使用
            final int finalTotal = total;
            final int finalCompleted = completed;
            final float finalCompletionRate = completionRate;
            final int finalTotalProgress = totalProgress;
            final int finalTotalTarget = totalTarget;
            final float finalOverallProgress = overallProgress;
            final int finalKillCount = killCount;
            final int finalObtainCount = obtainCount;
            final int finalBuildCount = buildCount;
            final boolean finalIsChinese = isChinese;

            source.sendSuccess(() -> Component.literal("§7" + (finalIsChinese ? "总进度数: " : "Total Progresses: ") + "§e" + finalTotal), false);
            source.sendSuccess(() -> Component.literal("§7" + (finalIsChinese ? "已完成: " : "Completed: ") + "§a" + finalCompleted +
                    " §7(§e" + String.format("%.1f", finalCompletionRate) + "%§7)"), false);
            source.sendSuccess(() -> Component.literal("§7" + (finalIsChinese ? "总进度: " : "Total Progress: ") + "§e" + finalTotalProgress + "§7/§a" + finalTotalTarget +
                    " §7(§e" + String.format("%.1f", finalOverallProgress) + "%§7)"), false);
            source.sendSuccess(() -> Component.literal("§7" + (finalIsChinese ? "类型分布:" : "Type Distribution:")), false);
            source.sendSuccess(() -> Component.literal("  §c" + (finalIsChinese ? "击杀: " : "Kill: ") + "§7" + finalKillCount), false);
            source.sendSuccess(() -> Component.literal("  §a" + (finalIsChinese ? "获得: " : "Obtain: ") + "§7" + finalObtainCount), false);
            source.sendSuccess(() -> Component.literal("  §6" + (finalIsChinese ? "建筑: " : "Build: ") + "§7" + finalBuildCount), false);

            // 进度排行榜
            if (total > 0) {
                source.sendSuccess(() -> Component.literal("§7" + (finalIsChinese ? "进度排名:" : "Top Progresses:")), false);

                // 找到进度最接近完成的3个
                progresses.sort((a, b) -> {
                    float aRatio = a.getProgress();
                    float bRatio = b.getProgress();
                    return Float.compare(bRatio, aRatio); // 降序排列
                });

                int showCount = Math.min(3, progresses.size());
                for (int i = 0; i < showCount; i++) {
                    var progress = progresses.get(i);
                    String ranking;
                    if (i == 0) ranking = "🥇";
                    else if (i == 1) ranking = "🥈";
                    else ranking = "🥉";

                    final var finalProgress = progress;
                    source.sendSuccess(() -> Component.literal("  " + ranking + " §f" + finalProgress.title +
                            " §7(§e" + String.format("%.1f", finalProgress.getProgress() * 100) + "%§7)"), false);
                }
            }

            return 1;
        }

        source.sendFailure(Component.literal("§c只有玩家可以使用此命令"));
        return 0;
    }

    // 语言相关命令
    private static int showCurrentLanguage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player != null) {
            String lang = getPlayerLanguage(player.getUUID());
            String currentLangName = lang.equals("zh_cn") ? "简体中文" : "English (US)";
            String message = "§a[SimpleProgress] §7当前语言: §e" + currentLangName + " §7(" + lang + ")";
            source.sendSuccess(() -> Component.literal(message), false);
            source.sendSuccess(() -> Component.literal("§7使用 §e/progress lang en_us §7切换为英文"), false);
            source.sendSuccess(() -> Component.literal("§7使用 §e/progress lang zh_cn §7切换为中文"), false);
            source.sendSuccess(() -> Component.literal("§7使用 §e/progress lang reset §7重置为系统默认"), false);
            return 1;
        }

        source.sendFailure(Component.literal("§c只有玩家可以使用此命令"));
        return 0;
    }

    private static int setLanguage(CommandContext<CommandSourceStack> context, String language) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player != null) {
            playerLanguages.put(player.getUUID(), language);

            String langName = language.equals("zh_cn") ? "简体中文" : "English (US)";
            String message = language.equals("zh_cn") ?
                    "§a✓ §7语言已设置为 §e简体中文 §7(zh_cn)" :
                    "§a✓ §7Language set to §eEnglish (US) §7(en_us)";

            source.sendSuccess(() -> Component.literal(message), false);
            source.sendSuccess(() -> Component.literal("§7" + (language.equals("zh_cn") ?
                    "现在所有进度命令将显示中文界面" :
                    "All progress commands will now display in English")), false);

            return 1;
        }

        source.sendFailure(Component.literal("§c只有玩家可以使用此命令"));
        return 0;
    }

    private static int resetLanguage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player != null) {
            playerLanguages.remove(player.getUUID());

            // 获取系统默认语言
            String systemLang = LanguageManager.getCurrentLanguage();
            String langName = systemLang.equals("zh_cn") ? "简体中文" : "English (US)";

            String message = systemLang.equals("zh_cn") ?
                    "§a✓ §7语言已重置为系统默认 §e简体中文" :
                    "§a✓ §7Language reset to system default §eEnglish (US)";

            source.sendSuccess(() -> Component.literal(message), false);
            return 1;
        }

        source.sendFailure(Component.literal("§c只有玩家可以使用此命令"));
        return 0;
    }

    // 辅助方法
    private static String createProgressBar(int current, int target, int length) {
        float percentage = target > 0 ? (float) current / target : 0;
        int filled = (int) (percentage * length);
        int empty = length - filled;

        StringBuilder bar = new StringBuilder("§a");
        for (int i = 0; i < filled; i++) {
            bar.append("█");
        }

        bar.append("§7");
        for (int i = 0; i < empty; i++) {
            bar.append("░");
        }

        return bar.toString();
    }

    private static String formatTimeAgo(long timestamp, boolean isChinese) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60000) { // 小于1分钟
            long seconds = diff / 1000;
            return seconds + (isChinese ? "秒前" : " seconds ago");
        } else if (diff < 3600000) { // 小于1小时
            long minutes = diff / 60000;
            return minutes + (isChinese ? "分钟前" : " minutes ago");
        } else if (diff < 86400000) { // 小于1天
            long hours = diff / 3600000;
            return hours + (isChinese ? "小时前" : " hours ago");
        } else {
            long days = diff / 86400000;
            return days + (isChinese ? "天前" : " days ago");
        }
    }

    private static String getTypeDisplayName(ProgressManager.ProgressType type, UUID playerId) {
        String lang = getPlayerLanguage(playerId);
        return LanguageManager.getTranslation("progress.type." + type.name().toLowerCase(), lang);
    }

    private static String getPlayerLanguage(UUID playerId) {
        // 如果玩家设置了语言偏好，使用该偏好
        if (playerLanguages.containsKey(playerId)) {
            return playerLanguages.get(playerId);
        }
        // 否则使用系统默认语言
        return LanguageManager.getCurrentLanguage();
    }

    private static void sendHelpMessage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        // 获取玩家语言偏好
        String lang = "en_us";
        if (source.isPlayer()) {
            ServerPlayer player = source.getPlayer();
            if (player != null) {
                lang = getPlayerLanguage(player.getUUID());
            }
        }

        boolean isChinese = lang.equals("zh_cn");

        String header = isChinese ? "§6=== SimpleProgress 命令帮助 ===" : "§6=== SimpleProgress Command Help ===";
        source.sendSuccess(() -> Component.literal(header), false);

        source.sendSuccess(() -> Component.literal("§e/progress help §7- " + (isChinese ? "显示此帮助信息" : "Show this help message")), false);
        source.sendSuccess(() -> Component.literal("§e/progress add <标题> §7- " + (isChinese ? "添加进度" : "Add progress")), false);
        source.sendSuccess(() -> Component.literal("  §7/progress add <标题> <目标> <数量> <kill|obtain|build>"), false);
        source.sendSuccess(() -> Component.literal("  §7" + (isChinese ? "示例: " : "Example: ") + "/progress add " +
                (isChinese ? "杀僵尸" : "Kill Zombies") + " minecraft:zombie 50 kill"), false);
        source.sendSuccess(() -> Component.literal("§e/progress list [页码] §7- " + (isChinese ? "列出所有进度" : "List all progresses")), false);
        source.sendSuccess(() -> Component.literal("§e/progress view <ID> §7- " + (isChinese ? "查看进度详情" : "View progress details")), false);
        source.sendSuccess(() -> Component.literal("§e/progress update <ID> <数量> §7- " + (isChinese ? "更新进度" : "Update progress")), false);
        source.sendSuccess(() -> Component.literal("§e/progress delete <ID> §7- " + (isChinese ? "删除进度" : "Delete progress")), false);
        source.sendSuccess(() -> Component.literal("§e/progress clear §7- " + (isChinese ? "清除所有进度" : "Clear all progresses")), false);
        source.sendSuccess(() -> Component.literal("§e/progress stats §7- " + (isChinese ? "查看统计信息" : "Show statistics")), false);
        source.sendSuccess(() -> Component.literal("§e/progress lang §7- " + (isChinese ? "语言设置" : "Language settings")), false);
        source.sendSuccess(() -> Component.literal("  §7/progress lang en_us §7- " + (isChinese ? "切换为英文" : "Switch to English")), false);
        source.sendSuccess(() -> Component.literal("  §7/progress lang zh_cn §7- " + (isChinese ? "切换为中文" : "Switch to Chinese")), false);
        source.sendSuccess(() -> Component.literal("  §7/progress lang reset §7- " + (isChinese ? "重置为默认" : "Reset to default")), false);
        source.sendSuccess(() -> Component.literal("§e/prog list §7- " + (isChinese ? "快捷列出进度" : "Quick list progresses")), false);
        source.sendSuccess(() -> Component.literal("§7" + (isChinese ? "版本: " : "Version: ") + "§a1.0.3 §7| " +
                (isChinese ? "开发者: " : "Developer: ") + "§e澜澈LanChe"), false);

        // 显示玩家当前进度数量
        if (source.isPlayer()) {
            var player = source.getPlayer();
            if (player != null) {
                var progresses = ProgressManager.getPlayerData(player);
                int completed = 0;
                for (var p : progresses) {
                    if (p.completed) completed++;
                }
                final int finalCompleted = completed;
                final int progressSize = progresses.size();
                final boolean finalIsChinese = isChinese;

                String progressText = finalIsChinese ?
                        "§7你的进度: §a" + finalCompleted + "§7/§e" + progressSize + " §7已完成" :
                        "§7Your progress: §a" + finalCompleted + "§7/§e" + progressSize + " §7completed";
                source.sendSuccess(() -> Component.literal(progressText), false);

                // 显示当前语言
                String currentLang = getPlayerLanguage(player.getUUID());
                String langText = currentLang.equals("zh_cn") ? "简体中文" : "English";
                source.sendSuccess(() -> Component.literal("§7" + (finalIsChinese ? "当前语言: " : "Current language: ") + "§e" + langText), false);
            }
        }
    }
}