package animeshnic626.areaedit.selection;

import java.util.*;

public class SelectionManager {
    private static final Map<ColumnPos, ColumnSelection> selectedColumns = new HashMap<>();

    private static final Set<ColumnPos> stickColumns = new HashSet<>();
    private static final Set<ColumnPos> bucketColumns = new HashSet<>();

    private static final Stack<Map<ColumnPos, ColumnSelection>> redoStack = new Stack<>();

    // Очереди для плавной (постепенной) обработки по 10 элементов
    private static final Queue<Map.Entry<ColumnPos, ColumnSelection>> pendingAddQueue = new LinkedList<>();
    private static final Queue<ColumnPos> pendingRemovalQueue = new LinkedList<>();
    private static int pendingFillMinY = 0;
    private static int pendingFillMaxY = 0;

    public record SelectionSnapshot(
            Map<ColumnPos, ColumnSelection> selectedColumns,
            Set<ColumnPos> stickColumns,
            Set<ColumnPos> bucketColumns
    ) {}

    public static SelectionSnapshot createSnapshot() {
        return new SelectionSnapshot(
                new HashMap<>(selectedColumns),
                new HashSet<>(stickColumns),
                new HashSet<>(bucketColumns)
        );
    }

    public static void restoreSnapshot(SelectionSnapshot snapshot) {
        selectedColumns.clear();
        stickColumns.clear();
        bucketColumns.clear();
        pendingAddQueue.clear();
        pendingRemovalQueue.clear();

        if (snapshot != null) {
            selectedColumns.putAll(snapshot.selectedColumns());
            stickColumns.addAll(snapshot.stickColumns());
            bucketColumns.addAll(snapshot.bucketColumns());
        }
    }

    public static Map<ColumnPos, ColumnSelection> getSelectedColumns() {
        return selectedColumns;
    }

    public static boolean hasColumn(ColumnPos pos) {
        return selectedColumns.containsKey(pos);
    }

    public static boolean isStickColumn(ColumnPos pos) {
        return stickColumns.contains(pos);
    }

    public static boolean isBucketColumn(ColumnPos pos) {
        return bucketColumns.contains(pos);
    }

    public static void addColumn(ColumnPos pos, int yMin, int yMax) {
        ColumnSelection sel = new ColumnSelection(pos, yMin, yMax);
        selectedColumns.put(pos, sel);
        stickColumns.add(pos);
    }

    public static void removeColumn(ColumnPos pos) {
        selectedColumns.remove(pos);
        stickColumns.remove(pos);
        bucketColumns.remove(pos);
    }

    public static void toggleColumn(ColumnPos pos, int yMin, int yMax) {
        if (selectedColumns.containsKey(pos)) {
            removeColumn(pos);
        } else {
            addColumn(pos, yMin, yMax);
        }
    }

    // Запуск постепенного добавления заливки (без лимитов, порциями)
    public static void addBucketFillGradual(Set<ColumnPos> filledPositions, int yMin, int yMax) {
        pendingAddQueue.clear();
        for (ColumnPos pos : filledPositions) {
            pendingAddQueue.add(new AbstractMap.SimpleEntry<>(pos, new ColumnSelection(pos, yMin, yMax)));
        }
        pendingFillMinY = yMin;
        pendingFillMaxY = yMax;
        redoStack.clear();
    }

    // Метод для тиков клиента/сервера, добавляющий по 10 колонок за раз
    public static void processAddBatch() {
        for (int i = 0; i < 10 && !pendingAddQueue.isEmpty(); i++) {
            Map.Entry<ColumnPos, ColumnSelection> entry = pendingAddQueue.poll();
            if (entry != null) {
                selectedColumns.put(entry.getKey(), entry.getValue());
                bucketColumns.add(entry.getKey());
            }
        }
    }

    public static void addBucketFill(Set<ColumnPos> filledPositions, int yMin, int yMax) {
        addBucketFillGradual(filledPositions, yMin, yMax);
    }

    // Удаление группы заливки порциями по 10 колонок
    public static boolean removeBucketFillAt(ColumnPos startPos) {
        if (!bucketColumns.contains(startPos)) {
            return false;
        }

        Map<ColumnPos, ColumnSelection> removedGroup = new HashMap<>();
        Queue<ColumnPos> queue = new LinkedList<>();

        queue.add(startPos);
        removedGroup.put(startPos, selectedColumns.get(startPos));

        int[] dx = {1, -1, 0, 0, 1, 1, -1, -1};
        int[] dz = {0, 0, 1, -1, 1, -1, 1, -1};

        while (!queue.isEmpty()) {
            ColumnPos curr = queue.poll();
            for (int i = 0; i < 8; i++) {
                ColumnPos neighbor = new ColumnPos(curr.x() + dx[i], curr.z() + dz[i]);
                if (bucketColumns.contains(neighbor) && !removedGroup.containsKey(neighbor)) {
                    removedGroup.put(neighbor, selectedColumns.get(neighbor));
                    queue.add(neighbor);
                }
            }
        }

        // Заполняем очередь на постепенное удаление
        pendingRemovalQueue.clear();
        for (ColumnPos pos : removedGroup.keySet()) {
            pendingRemovalQueue.add(pos);
        }

        for (ColumnPos pos : removedGroup.keySet()) {
            selectedColumns.remove(pos);
            bucketColumns.remove(pos);
        }

        redoStack.push(removedGroup);
        return true;
    }

    // Метод для плавной обработки удаления по 10 колонок
    public static void processRemovalBatch() {
        for (int i = 0; i < 10 && !pendingRemovalQueue.isEmpty(); i++) {
            ColumnPos pos = pendingRemovalQueue.poll();
            if (pos != null) {
                selectedColumns.remove(pos);
                bucketColumns.remove(pos);
            }
        }
    }

    public static boolean redoBucketFill() {
        if (!redoStack.isEmpty()) {
            Map<ColumnPos, ColumnSelection> restored = redoStack.pop();
            for (Map.Entry<ColumnPos, ColumnSelection> entry : restored.entrySet()) {
                selectedColumns.put(entry.getKey(), entry.getValue());
                bucketColumns.add(entry.getKey());
            }
            return true;
        }
        return false;
    }

    public static void clear() {
        clearAll();
    }

    public static void clearAll() {
        selectedColumns.clear();
        stickColumns.clear();
        bucketColumns.clear();
        redoStack.clear();
        pendingAddQueue.clear();
        pendingRemovalQueue.clear();
    }
}