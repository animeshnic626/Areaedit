package animeshnic626.areaedit.client;

import animeshnic626.areaedit.init.ModKeyBinds;
import animeshnic626.areaedit.item.SmartBucketItem;
import animeshnic626.areaedit.item.SmartLanternItem;
import animeshnic626.areaedit.item.SmartStickItem;
import animeshnic626.areaedit.network.ModNetwork;
import animeshnic626.areaedit.network.ServerboundAreaActionPacket;
import animeshnic626.areaedit.render.SelectionRenderer;
import animeshnic626.areaedit.selection.ColumnPos;
import animeshnic626.areaedit.selection.SelectionManager;
import animeshnic626.areaedit.selection.SelectionSaveHandler;
import animeshnic626.areaedit.selection.SelectionState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = "areaedit", value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // Плавная обработка добавления и удаления блоков по 10 за тик
            SelectionManager.processAddBatch();
            SelectionManager.processRemovalBatch();
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        // Загружаем выделенные зоны при входе в мир
        SelectionSaveHandler.loadSelection();
    }

    @SubscribeEvent
    public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        // Сохраняем зоны при выходе из мира
        SelectionSaveHandler.saveSelection();
        SelectionManager.clearAll();
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        while (ModKeyBinds.EXTEND_HEIGHT_KEY.consumeClick()) {
            SelectionState.toggleExtendedTo626();
        }

        while (ModKeyBinds.CAP_PLAYER_Y_KEY.consumeClick()) {
            int playerBlockY = mc.player.getBlockX() != 0 ? mc.player.getBlockY() : (int) Math.floor(mc.player.getY());
            if (SelectionState.getCustomMaxY() != null && SelectionState.getCustomMaxY() == playerBlockY) {
                SelectionState.setCustomMaxY(null);
            } else {
                SelectionState.setCustomMaxY(playerBlockY);
            }
        }

        while (ModKeyBinds.CLEAR_SELECTION_KEY.consumeClick()) {
            SelectionManager.clearAll();
            SelectionSaveHandler.saveSelection(); // Обновляем сохранение при очистке
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            SelectionRenderer.render(event.getPoseStack(), event.getFrustum());
        }
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        Player player = event.getEntity();
        if (player != null && player.getMainHandItem().getItem() instanceof SmartLanternItem) {
            ServerboundAreaActionPacket.Action action = player.isShiftKeyDown() ?
                    ServerboundAreaActionPacket.Action.REDO :
                    ServerboundAreaActionPacket.Action.UNDO;

            ModNetwork.sendToServer(new ServerboundAreaActionPacket(action, new ArrayList<>(), SelectionState.getCustomMaxY()));
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide()) {
            Player player = event.getEntity();
            if (player == null) return;

            if (player.getMainHandItem().getItem() instanceof SmartStickItem) {
                BlockPos pos = event.getPos();
                ColumnPos colPos = new ColumnPos(pos.getX(), pos.getZ());

                if (SelectionManager.hasColumn(colPos)) {
                    SelectionManager.removeColumn(colPos);
                }

                BlockState state = event.getLevel().getBlockState(pos);
                Minecraft.getInstance().particleEngine.destroy(pos, state);
                event.setCanceled(true);
            }
            else if (player.getMainHandItem().getItem() instanceof SmartBucketItem) {
                BlockPos pos = event.getPos();
                ColumnPos colPos = new ColumnPos(pos.getX(), pos.getZ());

                if (player.isShiftKeyDown()) {
                    SelectionManager.redoBucketFill();
                } else {
                    SelectionManager.removeBucketFillAt(colPos);
                }

                BlockState state = event.getLevel().getBlockState(pos);
                Minecraft.getInstance().particleEngine.destroy(pos, state);
                event.setCanceled(true);
            }
            else if (player.getMainHandItem().getItem() instanceof SmartLanternItem) {
                ServerboundAreaActionPacket.Action action = player.isShiftKeyDown() ?
                        ServerboundAreaActionPacket.Action.REDO :
                        ServerboundAreaActionPacket.Action.UNDO;

                ModNetwork.sendToServer(new ServerboundAreaActionPacket(action, new ArrayList<>(), SelectionState.getCustomMaxY()));
                event.setCanceled(true);
            }
        }
    }
}