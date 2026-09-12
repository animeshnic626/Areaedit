import os
from pathlib import Path

# Укажи путь к папке с пакетом (или запусти скрипт прямо в корне проекта)
BASE_DIR = Path("src/main/java/animeshnic626/areaedit")

# Код новых разделенных файлов
FILES = {
    BASE_DIR / "math/ColumnPos.java": '''package animeshnic626.areaedit.math;

import java.util.Objects;

public class ColumnPos {
    public final int x, z, minY, maxY;

    public ColumnPos(int x, int z, int minY, int maxY) {
        this.x = x;
        this.z = z;
        this.minY = minY;
        this.maxY = maxY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColumnPos col)) return false;
        return x == col.x && z == col.z && minY == col.minY && maxY == col.maxY;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z, minY, maxY);
    }
}
''',

    BASE_DIR / "math/Point2D.java": '''package animeshnic626.areaedit.math;

import java.util.Objects;

public class Point2D {
    public final int x, z;

    public Point2D(int x, int z) {
        this.x = x;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Point2D p)) return false;
        return x == p.x && z == p.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }
}
''',

    BASE_DIR / "math/AreaFillHandler.java": '''package animeshnic626.areaedit.math;

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
''',

    BASE_DIR / "init/ModItems.java": '''package animeshnic626.areaedit.init;

import animeshnic626.areaedit.Areaedit;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Areaedit.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Areaedit.MODID);

    public static final RegistryObject<Item> PINK_STICK = ITEMS.register("pink_stick",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> PINK_BUCKET = ITEMS.register("pink_bucket",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> PINK_LANTERN = ITEMS.register("pink_lantern",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CreativeModeTab> AREAEDIT_TAB = TABS.register("areaedit_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + Areaedit.MODID + ".areaedit_tab"))
                    .icon(() -> new ItemStack(PINK_BUCKET.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(PINK_STICK.get());
                        output.accept(PINK_BUCKET.get());
                        output.accept(PINK_LANTERN.get());
                    })
                    .build());
}
''',

    BASE_DIR / "client/KeyBindings.java": '''package animeshnic626.areaedit.client;

import animeshnic626.areaedit.Areaedit;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Areaedit.MODID, value = Dist.CLIENT)
public class KeyBindings {
    public static KeyMapping toggleKey;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        toggleKey = new KeyMapping("key.areaedit.toggle_626", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.world");
        event.register(toggleKey);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && toggleKey != null && toggleKey.consumeClick()) {
            Areaedit.height626Mode = !Areaedit.height626Mode;
            mc.player.sendSystemMessage(Component.literal("§d[AreaEdit] Режим 626 высоты: " + (Areaedit.height626Mode ? "ВКЛ" : "ВЫКЛ")));
        }
    }
}
''',

    BASE_DIR / "client/AreaRenderer.java": '''package animeshnic626.areaedit.client;

import animeshnic626.areaedit.Areaedit;
import animeshnic626.areaedit.math.ColumnPos;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = Areaedit.MODID, value = Dist.CLIENT)
public class AreaRenderer {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        VertexConsumer buffer = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());

        Frustum frustum = new Frustum(new Matrix4f(poseStack.last().pose()), new Matrix4f(event.getProjectionMatrix()));
        frustum.prepare(camPos.x, camPos.y, camPos.z);

        synchronized (Areaedit.selectedColumns) {
            for (ColumnPos col : Areaedit.selectedColumns) {
                int topY = Areaedit.height626Mode ? 626 : col.maxY;
                int minY = Math.min(col.minY, topY);
                int maxY = Math.max(col.minY, topY);

                AABB box = new AABB(col.x, minY, col.z, col.x + 1.0, maxY + 1.0, col.z + 1.0);
                if (frustum.isVisible(box)) {
                    LevelRenderer.renderLineBox(poseStack, buffer, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, 1.0f, 0.4f, 0.7f, 0.8f);
                }
            }
        }

        synchronized (Areaedit.filledBlocks) {
            for (BlockPos pos : Areaedit.filledBlocks) {
                AABB box = new AABB(pos);
                if (frustum.isVisible(box)) {
                    LevelRenderer.renderLineBox(poseStack, buffer, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, 1.0f, 0.2f, 0.5f, 0.6f);
                }
            }
        }

        mc.renderBuffers().bufferSource().endBatch(RenderType.lines());
        poseStack.popPose();
    }
}
''',

    BASE_DIR / "Areaedit.java": '''package animeshnic626.areaedit;

import animeshnic626.areaedit.init.ModItems;
import animeshnic626.areaedit.math.AreaFillHandler;
import animeshnic626.areaedit.math.ColumnPos;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.*;

@Mod(Areaedit.MODID)
public class Areaedit {
    public static final String MODID = "areaedit";

    public static final Set<ColumnPos> selectedColumns = Collections.synchronizedSet(new HashSet<>());
    public static final Set<BlockPos> filledBlocks = Collections.synchronizedSet(new HashSet<>());
    private static final List<Map<BlockPos, BlockState>> undoHistory = new ArrayList<>();

    public static boolean height626Mode = false;

    public Areaedit(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ModItems.ITEMS.register(modEventBus);
        ModItems.TABS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
    }

    public static boolean isHoldingModItem(Player player) {
        Item main = player.getMainHandItem().getItem();
        Item off = player.getOffhandItem().getItem();
        return main == ModItems.PINK_STICK.get() || main == ModItems.PINK_BUCKET.get() || main == ModItems.PINK_LANTERN.get() ||
               off == ModItems.PINK_STICK.get() || off == ModItems.PINK_BUCKET.get() || off == ModItems.PINK_LANTERN.get();
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getEntity();
        Level level = player.level();
        ItemStack stack = event.getItemStack();

        if (level.isClientSide()) return;

        if (player.isShiftKeyDown() && isHoldingModItem(player)) {
            if (event instanceof PlayerInteractEvent.LeftClickBlock || event instanceof PlayerInteractEvent.LeftClickEmpty) {
                if (!selectedColumns.isEmpty() || !filledBlocks.isEmpty()) {
                    selectedColumns.clear();
                    filledBlocks.clear();
                    player.sendSystemMessage(Component.translatable("message.areaedit.reset"));
                }
                event.setCanceled(true);
                return;
            }
        }

        if (stack.getItem() == ModItems.PINK_STICK.get()) {
            if (event instanceof PlayerInteractEvent.RightClickBlock rightEvent) {
                BlockPos pos = rightEvent.getPos();
                int minX = pos.getX();
                int minZ = pos.getZ();
                int minY = level.getMinBuildHeight();
                int maxY = level.getMaxBuildHeight() - 1;

                int lowestY = minY;
                int highestY = minY;
                boolean foundAny = false;

                for (int y = minY; y <= maxY; y++) {
                    BlockPos checkPos = new BlockPos(minX, y, minZ);
                    if (!level.isEmptyBlock(checkPos)) {
                        if (!foundAny) {
                            lowestY = y;
                            foundAny = true;
                        }
                        highestY = y;
                    }
                }
                if (!foundAny) {
                    lowestY = pos.getY();
                    highestY = pos.getY();
                }

                selectedColumns.add(new ColumnPos(minX, minZ, lowestY, highestY));
                event.setCanceled(true);
            } else if (event instanceof PlayerInteractEvent.LeftClickBlock leftEvent) {
                BlockPos pos = leftEvent.getPos();
                selectedColumns.removeIf(c -> c.x == pos.getX() && c.z == pos.getZ());
                event.setCanceled(true);
            }
        }

        if (stack.getItem() == ModItems.PINK_BUCKET.get()) {
            if (event instanceof PlayerInteractEvent.RightClickBlock) {
                filledBlocks.clear();
                filledBlocks.addAll(AreaFillHandler.calculateInnerArea(selectedColumns));
                event.setCanceled(true);
            } else if (event instanceof PlayerInteractEvent.LeftClickBlock) {
                filledBlocks.clear();
                event.setCanceled(true);
            }
        }

        if (stack.getItem() == ModItems.PINK_LANTERN.get()) {
            if (event instanceof PlayerInteractEvent.RightClickBlock) {
                Map<BlockPos, BlockState> removedBlocks = new HashMap<>();

                for (ColumnPos col : selectedColumns) {
                    int renderTop = height626Mode ? 626 : col.maxY;
                    for (int y = col.minY; y <= renderTop; y++) {
                        BlockPos p = new BlockPos(col.x, y, col.z);
                        if (!level.isEmptyBlock(p)) {
                            removedBlocks.put(p, level.getBlockState(p));
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
                for (BlockPos p : filledBlocks) {
                    if (!level.isEmptyBlock(p) && !removedBlocks.containsKey(p)) {
                        removedBlocks.put(p, level.getBlockState(p));
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    }
                }

                if (!removedBlocks.isEmpty()) {
                    undoHistory.add(removedBlocks);
                }
                selectedColumns.clear();
                filledBlocks.clear();
                event.setCanceled(true);
            } else if (event instanceof PlayerInteractEvent.LeftClickBlock) {
                if (!undoHistory.isEmpty()) {
                    Map<BlockPos, BlockState> lastAction = undoHistory.remove(undoHistory.size() - 1);
                    for (Map.Entry<BlockPos, BlockState> entry : lastAction.entrySet()) {
                        level.setBlock(entry.getKey(), entry.getValue(), 3);
                    }
                    player.sendSystemMessage(Component.translatable("message.areaedit.undo"));
                }
                event.setCanceled(true);
            }
        }
    }
}
'''
}

def setup_project():
    print("[+] Начинаю реструктуризацию исходников...")
    for path, code in FILES.items():
        # Создаем необходимые директории (math, init, client)
        path.parent.mkdir(parents=True, exist_ok=True)

        # Перезаписываем или создаем новый файл
        with open(path, "w", encoding="utf-8") as f:
            f.write(code.strip())
        print(f" -> Файл обновлен: {path}")

    print("\n[✔] Готово! Старый монолит разделен и очищен.")

if __name__ == "__main__":
    setup_project()