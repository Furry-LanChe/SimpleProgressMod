package com.lanche.simpleprogress;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, SimpleProgress.MODID);

    @SuppressWarnings("removal")
    public static final RegistryObject<MenuType<ProgressJournalMenu>> PROGRESS_JOURNAL =
            MENUS.register("progress_journal",
                    () -> IForgeMenuType.create((windowId, inv, data) -> new ProgressJournalMenu(windowId, inv)));
}