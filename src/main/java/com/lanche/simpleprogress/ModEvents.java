package com.lanche.simpleprogress;

import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SimpleProgressMod.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        // 服务器启动时的初始化
        SimpleProgressMod.LOGGER.info("Simple Progress 服务器启动完成");
    }
}