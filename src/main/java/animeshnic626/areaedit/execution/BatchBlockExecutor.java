package animeshnic626.areaedit.execution;

import animeshnic626.areaedit.config.AreaEditConfig;
import animeshnic626.areaedit.selection.ColumnSelection;
import animeshnic626.areaedit.selection.SelectionManager;
import animeshnic626.areaedit.selection.SelectionState;
import animeshnic626.areaedit.undo.BlockSnapshot;
import animeshnic626.areaedit.undo.UndoHistory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = "areaedit")
public class BatchBlockExecutor {

    private static final Queue<ServerDeletionTask> pendingServerDeletions = new LinkedList<>();

    private record ServerDeletionTask(ServerLevel level, List<ColumnSelection> columns, Integer clientMaxY) {}

    public static void executeServerDeletion(ServerLevel level, List<ColumnSelection> columns, Integer clientMaxY) {
        if (columns == null || columns.isEmpty()) return;
        pendingServerDeletions.add(new ServerDeletionTask(level, columns, clientMaxY));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !pendingServerDeletions.isEmpty()) {
            ServerDeletionTask task = pendingServerDeletions.peek();
            if (task == null || task.level().isClientSide()) {
                pendingServerDeletions.poll();
                return;
            }

            ServerLevel level = task.level();
            List<ColumnSelection> columns = task.columns();

            List<BlockSnapshot> removedBlocks = new ArrayList<>();
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

            // Базовые 10 колонок умножаются на твое число из команды (например, /areaedit 4)
            int limitPerTick = 10 * AreaEditConfig.getSpeedMultiplier();
            int columnsProcessed = 0;

            while (!columns.isEmpty() && columnsProcessed < limitPerTick) {
                ColumnSelection sel = columns.remove(0);
                columnsProcessed++;

                int x = sel.getPos().x();
                int z = sel.getPos().z();
                int yMin = sel.getYMin();
                int yMax = sel.getYMax();

                if (SelectionState.isExtendedTo626()) {
                    yMax = 626;
                } else if (task.clientMaxY() != null) {
                    yMax = Math.min(yMax, task.clientMaxY());
                }

                for (int y = yMin; y <= yMax; y++) {
                    mutablePos.set(x, y, z);
                    BlockState state = level.getBlockState(mutablePos);

                    // Сохраняем состояние (даже если это вода или воздух) и заменяем на AIR.
                    // Флаг 3 обновляет клиент и триггерит физику, чтобы вода корректно стекала/удалялась.
                    removedBlocks.add(new BlockSnapshot(mutablePos.immutable(), state, null));
                    level.setBlock(mutablePos, Blocks.AIR.defaultBlockState(), 3);
                }
            }

            if (!removedBlocks.isEmpty()) {
                UndoHistory.pushUndo(removedBlocks, SelectionManager.createSnapshot());
            }

            if (columns.isEmpty()) {
                pendingServerDeletions.poll();
                SelectionManager.clearAll();
            }
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