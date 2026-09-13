package animeshnic626.areaedit.item;

import animeshnic626.areaedit.selection.ColumnPos;
import animeshnic626.areaedit.selection.ColumnSelection;
import animeshnic626.areaedit.selection.SelectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.*;

public class SmartBucketItem extends Item {
    private static final int MAX_FILL_LIMIT = 626626626;
    private static final int MAX_RADIUS = 626626626;

    public SmartBucketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            BlockPos clickedPos = context.getClickedPos();
            ColumnPos startCol = new ColumnPos(clickedPos.getX(), clickedPos.getZ());

            if (SelectionManager.isStickColumn(startCol)) {
                return InteractionResult.SUCCESS;
            }

            Set<ColumnPos> filledRegion = floodFill(startCol);
            if (filledRegion != null && !filledRegion.isEmpty()) {
                // Находим максимальную и минимальную высоту среди всех палок контура
                int contourMinY = Integer.MAX_VALUE;
                int contourMaxY = Integer.MIN_VALUE;

                for (Map.Entry<ColumnPos, ColumnSelection> entry : SelectionManager.getSelectedColumns().entrySet()) {
                    if (SelectionManager.isStickColumn(entry.getKey())) {
                        contourMinY = Math.min(contourMinY, entry.getValue().getYMin());
                        contourMaxY = Math.max(contourMaxY, entry.getValue().getYMax());
                    }
                }

                // Если палок нет, берем высоту кликнутого блока
                int fillMinY = (contourMinY != Integer.MAX_VALUE) ? contourMinY : clickedPos.getY();
                int fillMaxY = (contourMaxY != Integer.MIN_VALUE) ? contourMaxY : clickedPos.getY();

                SelectionManager.addBucketFill(filledRegion, fillMinY, fillMaxY);
            }
        }
        return InteractionResult.SUCCESS;
    }

    private Set<ColumnPos> floodFill(ColumnPos startPos) {
        Set<ColumnPos> visited = new HashSet<>();
        Queue<ColumnPos> queue = new LinkedList<>();

        queue.add(startPos);
        visited.add(startPos);

        int[] dx = {1, -1, 0, 0};
        int[] dz = {0, 0, 1, -1};

        int startX = startPos.x();
        int startZ = startPos.z();

        while (!queue.isEmpty()) {
            ColumnPos curr = queue.poll();

            if (visited.size() > MAX_FILL_LIMIT ||
                Math.abs(curr.x() - startX) > MAX_RADIUS ||
                Math.abs(curr.z() - startZ) > MAX_RADIUS) {
                return null;
            }

            for (int i = 0; i < 4; i++) {
                int nx = curr.x() + dx[i];
                int nz = curr.z() + dz[i];
                ColumnPos neighbor = new ColumnPos(nx, nz);

                if (SelectionManager.isStickColumn(neighbor)) {
                    continue;
                }

                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return visited;
    }

    @Override
    public boolean onBlockStartBreak(net.minecraft.world.item.ItemStack stack, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        return true;
    }
}
