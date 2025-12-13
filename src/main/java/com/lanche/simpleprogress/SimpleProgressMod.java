package com.lanche.simpleprogress;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;

@Mod(SimpleProgressMod.MOD_ID)
public class SimpleProgressMod {
    public static final String MOD_ID = "simpleprogress_tracker";
    public static final String MOD_NAME = "Simple Progress Tracker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
    public static final Path CONFIG_DIR = Path.of("config/simpleprogress");

    public SimpleProgressMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册生命周期事件
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        // 注册到Forge事件总线
        MinecraftForge.EVENT_BUS.register(this);

        // 确保配置目录存在
        ensureConfigDirectory();

        LOGGER.info("{} v{} initializing", MOD_NAME, "1.0.3");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 初始化配置
        ModConfig.init();

        // 初始化语言管理器
        LanguageManager.initialize();

        LOGGER.info("{} common setup complete!", MOD_NAME);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        // 客户端初始化
        LOGGER.info("{} client setup complete!", MOD_NAME);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // 服务器启动时初始化
        LOGGER.info("{} server setup complete!", MOD_NAME);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ProgressCommand.register(event.getDispatcher());
        LOGGER.info("Simple Progress 命令已注册: /progress, /prog");
    }

    private void ensureConfigDirectory() {
        File configDir = new File("config/simpleprogress");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
    }

    public static String getModId() {
        return MOD_ID;
    }
}