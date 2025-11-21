package com.lanche.simpleprogress;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("simpleprogress")
public class SimpleProgress {
    public static final String MODID = "simpleprogress";
    public static final Logger LOGGER = LogManager.getLogger();

    @SuppressWarnings("removal")
    public SimpleProgress() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::clientSetup);

        ModMenus.MENUS.register(bus);

        // 注册事件处理器
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SuppressWarnings("removal")
    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.PROGRESS_JOURNAL.get(), ProgressJournalScreen::new);

            // 初始化语言管理器
            LanguageManager.initialize();

            // 1.20.1 中配置屏幕注册方式
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new ModConfigScreen(parent))
            );
        });
    }
}