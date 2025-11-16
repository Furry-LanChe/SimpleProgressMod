package com.lanche.simpleprogress;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = SimpleProgress.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {

    @SubscribeEvent
    public static void onEntityKilled(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player) {
            Player player = (Player) event.getSource().getEntity();
            String entityId = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType()).toString();
            ProgressManager.updateProgress(player, entityId, ProgressManager.ProgressType.KILL);
        }
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        Player player = event.getEntity();
        String itemId = ForgeRegistries.ITEMS.getKey(event.getItem().getItem().getItem()).toString();
        ProgressManager.updateProgress(player, itemId, ProgressManager.ProgressType.OBTAIN);
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        // 1.20.1 中获取维度信息的方式已改变
        String dimension = player.level().dimension().location().toString();
        ProgressManager.updateExploreProgress(player, dimension, (int)player.getX(), (int)player.getZ());
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            String blockId = ForgeRegistries.BLOCKS.getKey(event.getState().getBlock()).toString();
            ProgressManager.updateBuildProgress(player, blockId);
        }
    }
}