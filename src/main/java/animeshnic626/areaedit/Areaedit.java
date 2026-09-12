package animeshnic626.areaedit;

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