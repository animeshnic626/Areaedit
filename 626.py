import os

JAVA_DIR = os.path.join("src", "main", "java", "animeshnic626", "areaedit")

files = {
    # 1. Packet logic to send customMaxY to server
    os.path.join(JAVA_DIR, "network", "ServerboundAreaActionPacket.java"): """package animeshnic626.areaedit.network;

import animeshnic626.areaedit.execution.BatchBlockExecutor;
import animeshnic626.areaedit.selection.ColumnPos;
import animeshnic626.areaedit.selection.ColumnSelection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ServerboundAreaActionPacket {
    public enum Action {
        DELETE,
        UNDO,
        REDO
    }

    private final Action action;
    private final List<ColumnSelection> columns;
    private final Integer customMaxY;

    public ServerboundAreaActionPacket(Action action, List<ColumnSelection> columns, Integer customMaxY) {
        this.action = action;
        this.columns = columns != null ? columns : new ArrayList<>();
        this.customMaxY = customMaxY;
    }

    public ServerboundAreaActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        int size = buf.readVarInt();
        this.columns = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int x = buf.readInt();
            int z = buf.readInt();
            int yMin = buf.readInt();
            int yMax = buf.readInt();
            this.columns.add(new ColumnSelection(new ColumnPos(x, z), yMin, yMax));
        }
        if (buf.readBoolean()) {
            this.customMaxY = buf.readInt();
        } else {
            this.customMaxY = null;
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeVarInt(columns.size());
        for (ColumnSelection col : columns) {
            buf.writeInt(col.getPos().x());
            buf.writeInt(col.getPos().z());
            buf.writeInt(col.getYMin());
            buf.writeInt(col.getYMax());
        }
        if (customMaxY != null) {
            buf.writeBoolean(true);
            buf.writeInt(customMaxY);
        } else {
            buf.writeBoolean(false);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                switch (action) {
                    case DELETE -> BatchBlockExecutor.executeServerDeletion(player.serverLevel(), columns, customMaxY);
                    case UNDO -> BatchBlockExecutor.performServerUndo(player.serverLevel());
                    case REDO -> BatchBlockExecutor.performServerRedo(player.serverLevel());
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
""",

    # 2. BatchBlockExecutor (Uses customMaxY from client/server context)
    os.path.join(JAVA_DIR, "execution", "BatchBlockExecutor.java"): """package animeshnic626.areaedit.execution;

import animeshnic626.areaedit.selection.ColumnSelection;
import animeshnic626.areaedit.selection.SelectionManager;
import animeshnic626.areaedit.selection.SelectionState;
import animeshnic626.areaedit.undo.BlockSnapshot;
import animeshnic626.areaedit.undo.UndoHistory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class BatchBlockExecutor {

    public static void executeServerDeletion(ServerLevel level, List<ColumnSelection> columns, Integer clientMaxY) {
        if (columns == null || columns.isEmpty()) return;

        List<BlockSnapshot> removedBlocks = new ArrayList<>();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (ColumnSelection sel : columns) {
            int x = sel.getPos().x();
            int z = sel.getPos().z();
            int yMin = sel.getYMin();
            int yMax = sel.getYMax();

            if (SelectionState.isExtendedTo626()) {
                yMax = 626;
            } else if (clientMaxY != null) {
                yMax = Math.min(yMax, clientMaxY);
            } else if (SelectionState.getCustomMaxY() != null) {
                yMax = Math.min(yMax, SelectionState.getCustomMaxY());
            }

            for (int y = yMin; y <= yMax; y++) {
                mutablePos.set(x, y, z);
                BlockState state = level.getBlockState(mutablePos);

                if (!state.isAir()) {
                    removedBlocks.add(new BlockSnapshot(mutablePos.immutable(), state, null));
                    level.setBlock(mutablePos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        if (!removedBlocks.isEmpty()) {
            UndoHistory.pushUndo(removedBlocks, SelectionManager.createSnapshot());
            SelectionManager.clearAll();
        }
    }

    public static void performServerUndo(ServerLevel level) {
        UndoHistory.UndoEntry entry = UndoHistory.popUndo();
        if (entry == null) return;

        for (BlockSnapshot snapshot : entry.blocks()) {
            level.setBlock(snapshot.pos(), snapshot.state(), 3);
        }

        SelectionManager.restoreSnapshot(entry.selection());
    }

    public static void performServerRedo(ServerLevel level) {
        UndoHistory.UndoEntry entry = UndoHistory.popRedo();
        if (entry == null) return;

        for (BlockSnapshot snapshot : entry.blocks()) {
            level.setBlock(snapshot.pos(), Blocks.AIR.defaultBlockState(), 3);
        }

        SelectionManager.clearAll();
    }
}
""",

    # 3. SmartLanternItem sending customMaxY
    os.path.join(JAVA_DIR, "item", "SmartLanternItem.java"): """package animeshnic626.areaedit.item;

import animeshnic626.areaedit.network.ModNetwork;
import animeshnic626.areaedit.network.ServerboundAreaActionPacket;
import animeshnic626.areaedit.selection.SelectionManager;
import animeshnic626.areaedit.selection.SelectionState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.ArrayList;

public class SmartLanternItem extends Item {
    public SmartLanternItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() && hand == InteractionHand.MAIN_HAND) {
            if (!SelectionManager.getSelectedColumns().isEmpty()) {
                ModNetwork.sendToServer(new ServerboundAreaActionPacket(
                        ServerboundAreaActionPacket.Action.DELETE,
                        new ArrayList<>(SelectionManager.getSelectedColumns().values()),
                        SelectionState.getCustomMaxY()
                ));
                SelectionManager.clearAll();
            }
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide() && context.getHand() == InteractionHand.MAIN_HAND) {
            if (!SelectionManager.getSelectedColumns().isEmpty()) {
                ModNetwork.sendToServer(new ServerboundAreaActionPacket(
                        ServerboundAreaActionPacket.Action.DELETE,
                        new ArrayList<>(SelectionManager.getSelectedColumns().values()),
                        SelectionState.getCustomMaxY()
                ));
                SelectionManager.clearAll();
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean onBlockStartBreak(ItemStack stack, net.minecraft.core.BlockPos pos, Player player) {
        return true;
    }
}
""",

    # 4. ClientEventHandler Keybind Listener
    os.path.join(JAVA_DIR, "client", "ClientEventHandler.java"): """package animeshnic626.areaedit.client;

import animeshnic626.areaedit.init.ModKeyBinds;
import animeshnic626.areaedit.item.SmartBucketItem;
import animeshnic626.areaedit.item.SmartLanternItem;
import animeshnic626.areaedit.item.SmartStickItem;
import animeshnic626.areaedit.network.ModNetwork;
import animeshnic626.areaedit.network.ServerboundAreaActionPacket;
import animeshnic626.areaedit.render.SelectionRenderer;
import animeshnic626.areaedit.selection.ColumnPos;
import animeshnic626.areaedit.selection.SelectionManager;
import animeshnic626.areaedit.selection.SelectionState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = "areaedit", value = Dist.CLIENT)
public class ClientEventHandler {

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
"""
}

def apply_height_cap():
    for path, content in files.items():
        with open(path, "w", encoding="utf-8") as f:
            f.write(content)
        print(f"[+] Обновлен файл: {path}")

if __name__ == "__main__":
    apply_height_cap()