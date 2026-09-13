package animeshnic626.areaedit.execution;

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
