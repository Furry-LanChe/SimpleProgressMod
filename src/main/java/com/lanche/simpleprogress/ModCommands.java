package com.lanche.simpleprogress;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = SimpleProgress.MODID)
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("progress")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    if (source.getEntity() instanceof ServerPlayer) {
                        ServerPlayer player = (ServerPlayer) source.getEntity();

                        MenuProvider menuProvider = new MenuProvider() {
                            @Override
                            public Component getDisplayName() {
                                return Component.translatable("simpleprogress.gui.progress_journal");
                            }

                            @Nullable
                            @Override
                            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                                return new ProgressJournalMenu(containerId, playerInventory);
                            }
                        };

                        NetworkHooks.openScreen(player, menuProvider);
                        player.sendSystemMessage(Component.literal(LanguageManager.getTranslation("simpleprogress.command.open_gui")));
                    }
                    return 1;
                })
        );
    }
}