package animeshnic626.areaedit.command;

import animeshnic626.areaedit.config.AreaEditConfig;
import animeshnic626.areaedit.selection.ColumnPos;
import animeshnic626.areaedit.selection.ColumnSelection;
import animeshnic626.areaedit.selection.SelectionManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "areaedit")
public class AreaEditCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("areaedit")
                        // Подкоманда: /areaedit speed [число]
                        .then(Commands.literal("speed")
                                .executes(context -> {
                                    AreaEditConfig.setSpeedMultiplier(1);
                                    context.getSource().sendSuccess(() ->
                                            Component.literal("§a[AreaEdit] Скорость сброшена на дефолт: §e1"), true
                                    );
                                    return 1;
                                })
                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 100))
                                        .executes(context -> {
                                            int newSpeed = IntegerArgumentType.getInteger(context, "value");
                                            AreaEditConfig.setSpeedMultiplier(newSpeed);

                                            context.getSource().sendSuccess(() ->
                                                    Component.literal("§a[AreaEdit] Скорость изменена на: §e" + newSpeed), true
                                            );
                                            return 1;
                                        })
                                )
                        )
                        // Подкоманда: /areaedit test [радиус]
                        .then(Commands.literal("test")
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 100))
                                        .executes(context -> {
                                            CommandSourceStack source = context.getSource();
                                            BlockPos playerPos = source.getPlayerOrException().blockPosition();
                                            ServerLevel level = source.getLevel();
                                            int radius = IntegerArgumentType.getInteger(context, "radius");

                                            SelectionManager.clearAll();

                                            Map<ColumnPos, ColumnSelection> circleColumns = new HashMap<>();
                                            int px = playerPos.getX();
                                            int pz = playerPos.getZ();
                                            int py = playerPos.getY();

                                            // Самое дно мира (-64 в обычных мирах 1.20.1)
                                            int minY = level.getMinBuildHeight();
                                            // Верхняя граница — голова игрока + 5 блоков
                                            int maxY = py + 5;

                                            for (int x = -radius; x <= radius; x++) {
                                                for (int z = -radius; z <= radius; z++) {
                                                    if (x * x + z * z <= radius * radius) {
                                                        ColumnPos colPos = new ColumnPos(px + x, pz + z);
                                                        circleColumns.put(colPos, new ColumnSelection(colPos, minY, maxY));
                                                    }
                                                }
                                            }

                                            SelectionManager.getSelectedColumns().putAll(circleColumns);

                                            source.sendSuccess(() ->
                                                    Component.literal("§a[AreaEdit] Тестовый круг создан от Y=" + minY + " до Y=" + maxY + "! Радиус: §e" + radius), true
                                            );
                                            return 1;
                                        })
                                )
                        )
        );
    }
}