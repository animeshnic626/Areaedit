package animeshnic626.areaedit.selection;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SelectionSaveHandler {

    private static File getWorldSaveFile() {
        Minecraft minecraft = Minecraft.getInstance();
        File worldDir = null;

        // Пытаемся получить путь к папке одиночного мира
        if (minecraft.hasSingleplayerServer() && minecraft.getSingleplayerServer() != null) {
            try {
                worldDir = minecraft.getSingleplayerServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
            } catch (Exception ignored) {}
        }

        // Если это не одиночная игра или путь не определился, сохраняем для мультиплеера
        if (worldDir == null || !worldDir.exists()) {
            if (minecraft.getConnection() == null && minecraft.level == null) {
                return null;
            }
            String serverIp = minecraft.getCurrentServer() != null ? minecraft.getCurrentServer().ip.replaceAll("[^a-zA-Z0-9.-]", "_") : "multiplayer";
            worldDir = new File(minecraft.gameDirectory, "server_selections/" + serverIp);
        }

        File dataDir = new File(worldDir, "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        return new File(dataDir, "areaedit_selection.dat");
    }

    public static void saveSelection() {
        File file = getWorldSaveFile();
        if (file == null) return;

        CompoundTag mainTag = new CompoundTag();
        Map<ColumnPos, ColumnSelection> selected = SelectionManager.getSelectedColumns();

        ListTag listTag = new ListTag();
        for (Map.Entry<ColumnPos, ColumnSelection> entry : selected.entrySet()) {
            ColumnPos pos = entry.getKey();
            ColumnSelection sel = entry.getValue();

            CompoundTag tag = new CompoundTag();
            tag.putInt("X", pos.x());
            tag.putInt("Z", pos.z());
            tag.putInt("YMin", sel.getYMin());
            tag.putInt("YMax", sel.getYMax());
            tag.putBoolean("IsStick", SelectionManager.isStickColumn(pos));
            tag.putBoolean("IsBucket", SelectionManager.isBucketColumn(pos));
            listTag.add(tag);
        }

        mainTag.put("Selections", listTag);

        try {
            NbtIo.writeCompressed(mainTag, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadSelection() {
        File file = getWorldSaveFile();
        if (file == null || !file.exists()) {
            SelectionManager.clearAll();
            return;
        }

        try {
            CompoundTag mainTag = NbtIo.readCompressed(file);
            if (mainTag == null || !mainTag.contains("Selections", Tag.TAG_LIST)) {
                SelectionManager.clearAll();
                return;
            }

            Map<ColumnPos, ColumnSelection> loadedSelected = new HashMap<>();
            Set<ColumnPos> loadedStick = new HashSet<>();
            Set<ColumnPos> loadedBucket = new HashSet<>();

            ListTag listTag = mainTag.getList("Selections", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag tag = listTag.getCompound(i);
                int x = tag.getInt("X");
                int z = tag.getInt("Z");
                int yMin = tag.getInt("YMin");
                int yMax = tag.getInt("YMax");
                boolean isStick = tag.getBoolean("IsStick");
                boolean isBucket = tag.getBoolean("IsBucket");

                ColumnPos pos = new ColumnPos(x, z);
                ColumnSelection sel = new ColumnSelection(pos, yMin, yMax);

                loadedSelected.put(pos, sel);
                if (isStick) loadedStick.add(pos);
                if (isBucket) loadedBucket.add(pos);
            }

            // Передаем загруженные данные в SelectionManager через снепшот
            SelectionManager.restoreSnapshot(new SelectionManager.SelectionSnapshot(loadedSelected, loadedStick, loadedBucket));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}