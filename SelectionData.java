package animeshnic626.areaedit;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

import java.util.*;

public class SelectionData {
    public record Column(int x, int z, int maxY) {}

    private static final Long2ObjectMap<Column> selectedZone = new Long2ObjectOpenHashMap<>();
    private static final LinkedList<Map<BlockPos, BlockState>> undoHistory = new LinkedList<>();
    private static final int MAX_UNDO_STEPS = 5;
    private static final Queue<BlockPos> deletionQueue = new LinkedList<>();
    private static Map<BlockPos, BlockState> currentUndoBatch = null;

    public static Collection<Column> getSelectedColumns() { return selectedZone.values(); }

    public static void clearAll() { selectedZone.clear(); }

    public static long packXZ(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32);
    }

    private static int getSurfaceY(Level level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        return Math.max(level.getMinBuildHeight(), y);
    }

    public static void selectSingleColumn(Level level, BlockPos pos) {
        int topY = getSurfaceY(level, pos.getX(), pos.getZ());
        long packed = packXZ(pos.getX(), pos.getZ());
        selectedZone.put(packed, new Column(pos.getX(), pos.getZ(), topY));
    }

    public static void removeSingleColumn(BlockPos pos) {
        long packed = packXZ(pos.getX(), pos.getZ());
        selectedZone.remove(packed);
    }

    public static void fillBoundedArea(Level level, BlockPos startPos, Player player) {
        long startPacked = packXZ(startPos.getX(), startPos.getZ());

        if (selectedZone.containsKey(startPacked)) {
            player.sendSystemMessage(Component.literal("§cКликните внутри пустой области, а не по самой границе!"));
            return;
        }

        Queue<Long> queue = new LinkedList<>();
        LongSet visited = new LongOpenHashSet();
        LongSet newSelection = new LongOpenHashSet();

        queue.add(startPacked);
        visited.add(startPacked);

        int[][] directions = {
                {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };
        int maxAreaLimit = 15000;

        while (!queue.isEmpty()) {
            long curr = queue.poll();
            newSelection.add(curr);

            if (newSelection.size() > maxAreaLimit) {
                player.sendSystemMessage(Component.literal("§cОшибка: Контур не замкнут (есть дырка)! Проверьте линии из палок."));
                return;
            }

            int cx = (int) (curr & 0xFFFFFFFFL);
            int cz = (int) ((curr >> 32) & 0xFFFFFFFFL);

            for (int[] dir : directions) {
                int nx = cx + dir[0];
                int nz = cz + dir[1];
                long nPacked = packXZ(nx, nz);

                if (!visited.contains(nPacked) && !selectedZone.containsKey(nPacked)) {
                    visited.add(nPacked);
                    queue.add(nPacked);
                }
            }
        }

        for (long packed : newSelection) {
            int cx = (int) (packed & 0xFFFFFFFFL);
            int cz = (int) ((packed >> 32) & 0xFFFFFFFFL);
            int topY = getSurfaceY(level, cx, cz);
            selectedZone.put(packed, new Column(cx, cz, topY));
        }
        player.sendSystemMessage(Component.literal("§dЗамкнутая область успешно залита!"));
    }

    public static void executeDeletion(Level level) {
        deletionQueue.clear();
        currentUndoBatch = new HashMap<>();
        int minY = level.getMinBuildHeight();

        for (Long2ObjectMap.Entry<Column> entry : selectedZone.long2ObjectEntrySet()) {
            Column col = entry.getValue();
            for (int y = col.maxY(); y >= minY; y--) {
                BlockPos pos = new BlockPos(col.x(), y, col.z());
                BlockState state = level.getBlockState(pos);
                if (!state.isAir()) {
                    deletionQueue.add(pos);
                }
            }
        }
        selectedZone.clear();
    }

    public static void tickDeletion(Level level) {
        if (deletionQueue.isEmpty()) {
            if (currentUndoBatch != null && !currentUndoBatch.isEmpty()) {
                if (undoHistory.size() >= MAX_UNDO_STEPS) undoHistory.removeFirst();
                undoHistory.addLast(currentUndoBatch);
                currentUndoBatch = null;
            }
            return;
        }

        int processed = 0;
        while (!deletionQueue.isEmpty() && processed < 400) {
            BlockPos pos = deletionQueue.poll();
            BlockState state = level.getBlockState(pos);

            if (!state.isAir()) {
                if (currentUndoBatch != null) currentUndoBatch.put(pos, state);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
                processed++;
            }
        }
    }

    public static void executeUndo(Level level) {
        if (!undoHistory.isEmpty()) {
            Map<BlockPos, BlockState> lastState = undoHistory.removeLast();
            lastState.forEach((pos, state) -> level.setBlock(pos, state, 3));
        }
    }
}
