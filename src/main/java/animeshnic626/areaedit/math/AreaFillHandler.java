package animeshnic626.areaedit.math;

import net.minecraft.core.BlockPos;
import java.util.*;

public class AreaFillHandler {

    public static Set<BlockPos> calculateInnerArea(Set<ColumnPos> selectedColumns) {
        Set<BlockPos> result = new HashSet<>();
        if (selectedColumns.isEmpty()) return result;

        Set<Point2D> contour = new HashSet<>();
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (ColumnPos col : selectedColumns) {
            contour.add(new Point2D(col.x, col.z));
            if (col.minY < minY) minY = col.minY;
            if (col.maxY > maxY) maxY = col.maxY;
        }

        int minX = contour.stream().mapToInt(p -> p.x).min().orElse(0) - 1;
        int maxX = contour.stream().mapToInt(p -> p.x).max().orElse(0) + 1;
        int minZ = contour.stream().mapToInt(p -> p.z).min().orElse(0) - 1;
        int maxZ = contour.stream().mapToInt(p -> p.z).max().orElse(0) + 1;

        Set<Point2D> outside = new HashSet<>();
        Queue<Point2D> queue = new ArrayDeque<>();

        Point2D start = new Point2D(minX, minZ);
        queue.add(start);
        outside.add(start);

        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        while (!queue.isEmpty()) {
            Point2D p = queue.poll();

            for (int[] d : dirs) {
                int nx = p.x + d[0];
                int nz = p.z + d[1];

                if (nx < minX || nx > maxX || nz < minZ || nz > maxZ) continue;
                Point2D next = new Point2D(nx, nz);

                if (!contour.contains(next) && outside.add(next)) {
                    queue.add(next);
                }
            }
        }

        for (int x = minX + 1; x < maxX; x++) {
            for (int z = minZ + 1; z < maxZ; z++) {
                Point2D pt = new Point2D(x, z);
                if (!contour.contains(pt) && !outside.contains(pt)) {
                    for (int y = minY; y <= maxY; y++) {
                        result.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return result;
    }
}