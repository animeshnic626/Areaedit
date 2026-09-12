package animeshnic626.areaedit;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.*;

@Mod(Areaedit.MODID)
public class Areaedit {
    public static final String MODID = "areaedit";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<Item> PINK_STICK = ITEMS.register("pink_stick",
            () -> new StickItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> PINK_BUCKET = ITEMS.register("pink_bucket",
            () -> new BucketItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> PINK_LANTERN = ITEMS.register("pink_lantern",
            () -> new LanternItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CreativeModeTab> AREAEDIT_TAB = TABS.register("areaedit_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MODID + ".areaedit_tab"))
                    .icon(() -> new ItemStack(PINK_BUCKET.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(PINK_STICK.get());
                        output.accept(PINK_BUCKET.get());
                        output.accept(PINK_LANTERN.get());
                    })
                    .build());

    public static final Set<ColumnPos> selectedColumns = Collections.synchronizedSet(new HashSet<>());
    public static final Set<BlockPos> filledBlocks = Collections.synchronizedSet(new HashSet<>());
    private static final List<Map<BlockPos, BlockState>> undoHistory = new ArrayList<>();

    public static boolean height626Mode = false;
    public static KeyMapping toggleKey;

    public Areaedit(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ITEMS.register(modEventBus);
        TABS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
    }

    public static boolean isHoldingModItem(Player player) {
        Item main = player.getMainHandItem().getItem();
        Item off = player.getOffhandItem().getItem();
        return main == PINK_STICK.get() || main == PINK_BUCKET.get() || main == PINK_LANTERN.get() ||
                off == PINK_STICK.get() || off == PINK_BUCKET.get() || off == PINK_LANTERN.get();
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

        if (stack.getItem() == PINK_STICK.get()) {
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

                ColumnPos col = new ColumnPos(minX, minZ, lowestY, highestY);
                selectedColumns.add(col);
                event.setCanceled(true);
            } else if (event instanceof PlayerInteractEvent.LeftClickBlock leftEvent) {
                BlockPos pos = leftEvent.getPos();
                selectedColumns.removeIf(c -> c.x == pos.getX() && c.z == pos.getZ());
                event.setCanceled(true);
            }
        }

        if (stack.getItem() == PINK_BUCKET.get()) {
            if (event instanceof PlayerInteractEvent.RightClickBlock) {
                fillInnerArea(level);
                event.setCanceled(true);
            } else if (event instanceof PlayerInteractEvent.LeftClickBlock) {
                filledBlocks.clear();
                event.setCanceled(true);
            }
        }

        if (stack.getItem() == PINK_LANTERN.get()) {
            if (event instanceof PlayerInteractEvent.RightClickBlock) {
                Map<BlockPos, BlockState> removedBlocks = new HashMap<>();

                for (ColumnPos col : selectedColumns) {
                    int renderTop = height626Mode ? 626 : col.maxY;
                    for (int y = col.minY; y <= renderTop; y++) {
                        BlockPos p = new BlockPos(col.x, y, col.z);
                        if (!level.isEmptyBlock(p)) {
                            removedBlocks.put(p, level.getBlockState(p));
                            level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
                for (BlockPos p : filledBlocks) {
                    if (!level.isEmptyBlock(p) && !removedBlocks.containsKey(p)) {
                        removedBlocks.put(p, level.getBlockState(p));
                        level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
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

    private void fillInnerArea(Level level) {
        if (selectedColumns.isEmpty()) return;

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
        Queue<Point2D> queue = new LinkedList<>();

        queue.add(new Point2D(minX, minZ));

        while (!queue.isEmpty()) {
            Point2D p = queue.poll();
            if (p.x < minX || p.x > maxX || p.z < minZ || p.z > maxZ) continue;
            if (contour.contains(p) || outside.contains(p)) continue;

            outside.add(p);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;

                    if (dx != 0 && dz != 0) {
                        Point2D side1 = new Point2D(p.x + dx, p.z);
                        Point2D side2 = new Point2D(p.x, p.z + dz);
                        if (contour.contains(side1) && contour.contains(side2)) {
                            continue;
                        }
                    }

                    queue.add(new Point2D(p.x + dx, p.z + dz));
                }
            }
        }

        int realMinX = contour.stream().mapToInt(p -> p.x).min().orElse(0);
        int realMaxX = contour.stream().mapToInt(p -> p.x).max().orElse(0);
        int realMinZ = contour.stream().mapToInt(p -> p.z).min().orElse(0);
        int realMaxZ = contour.stream().mapToInt(p -> p.z).max().orElse(0);

        for (int x = realMinX; x <= realMaxX; x++) {
            for (int z = realMinZ; z <= realMaxZ; z++) {
                Point2D pt = new Point2D(x, z);
                if (!contour.contains(pt) && !outside.contains(pt)) {
                    for (int y = minY; y <= maxY; y++) {
                        filledBlocks.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
    }

    public static class ColumnPos {
        public final int x, z, minY, maxY;
        public ColumnPos(int x, int z, int minY, int maxY) {
            this.x = x; this.z = z; this.minY = minY; this.maxY = maxY;
        }
    }

    public static class Point2D {
        public final int x, z;
        public Point2D(int x, int z) { this.x = x; this.z = z; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Point2D)) return false;
            Point2D p = (Point2D) o;
            return x == p.x && z == p.z;
        }
        @Override public int hashCode() { return Objects.hash(x, z); }
    }

    public static class StickItem extends Item { public StickItem(Properties p) { super(p); } }
    public static class BucketItem extends Item { public BucketItem(Properties p) { super(p); } }
    public static class LanternItem extends Item { public LanternItem(Properties p) { super(p); } }

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            toggleKey = new KeyMapping("key.areaedit.toggle_626", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.world");
            event.register(toggleKey);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientRenderer {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && toggleKey != null && toggleKey.consumeClick()) {
                height626Mode = !height626Mode;
                mc.player.sendSystemMessage(Component.literal("§d[AreaEdit] Режим 626 высоты: " + (height626Mode ? "ВКЛ" : "ВЫКЛ")));
            }
        }

        @SubscribeEvent
        public static void onRenderLevelStage(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
                Minecraft mc = Minecraft.getInstance();
                Camera camera = mc.gameRenderer.getMainCamera();
                Vec3 camPos = camera.getPosition();
                PoseStack poseStack = event.getPoseStack();

                poseStack.pushPose();
                poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

                // Используем безопасный буфер линий (RenderType.lines()), который идеально подходит для каркасов и столбов
                VertexConsumer buffer = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());

                Matrix4f viewMatrix = new Matrix4f(poseStack.last().pose());
                Matrix4f projMatrix = new Matrix4f(event.getProjectionMatrix());
                Frustum frustum = new Frustum(viewMatrix, projMatrix);
                frustum.prepare(camPos.x, camPos.y, camPos.z);

                synchronized (selectedColumns) {
                    for (ColumnPos col : selectedColumns) {
                        int topY = height626Mode ? 626 : col.maxY;
                        int minY = col.minY;
                        if (minY > topY) { int temp = minY; minY = topY; topY = temp; }

                        AABB boundingBox = new AABB(col.x, minY, col.z, col.x + 1.0, topY + 1.0, col.z + 1.0);
                        if (!frustum.isVisible(boundingBox)) continue;

                        if (height626Mode) {
                            renderColumnOutline(poseStack, buffer, col.x, minY, topY + 1, col.z, 1.0f, 0.4f, 0.7f, 0.8f);
                        } else {
                            for (int y = col.minY; y <= col.maxY; y++) {
                                LevelRenderer.renderLineBox(poseStack, buffer, col.x, y, col.z, col.x + 1.0, y + 1.0, col.z + 1.0, 1.0f, 0.4f, 0.7f, 0.8f);
                            }
                        }
                    }
                }

                synchronized (filledBlocks) {
                    for (BlockPos pos : filledBlocks) {
                        AABB boundingBox = new AABB(pos);
                        if (!frustum.isVisible(boundingBox)) continue;

                        LevelRenderer.renderLineBox(poseStack, buffer, pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0, 1.0f, 0.2f, 0.5f, 0.6f);
                    }
                }

                mc.renderBuffers().bufferSource().endBatch(RenderType.lines());
                poseStack.popPose();
            }
        }

        private static void renderColumnOutline(PoseStack poseStack, VertexConsumer buffer, int x, int minY, int maxY, int z, float r, float g, float b, float a) {
            // Рисуем четкий контур вертикального столба от minY до maxY
            LevelRenderer.renderLineBox(poseStack, buffer, x, minY, z, x + 1.0, maxY, z + 1.0, r, g, b, a);
        }
    }
}