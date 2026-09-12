package animeshnic626.areaedit;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Areaedit.MODID)
public class ServerEvents {

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.level.isClientSide) {
            SelectionData.tickDeletion(event.level);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide) return;

        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        Item item = stack.getItem();

        if (item instanceof SmartStickItem) {
            if (player.isShiftKeyDown()) {
                SelectionData.clearAll();
                player.sendSystemMessage(Component.literal("§cВыделение полностью очищено!"));
            } else {
                SelectionData.removeSingleColumn(event.getPos());
                player.sendSystemMessage(Component.literal("§eТочка убрана из выделения!"));
            }
            event.setCanceled(true);
        } else if (item instanceof SmartBucketItem) {
            SelectionData.clearAll();
            player.sendSystemMessage(Component.literal("§c§cЗалитая зона очищена!"));
            event.setCanceled(true);
        } else if (item instanceof SmartLanternItem) {
            SelectionData.executeUndo(event.getLevel());
            player.sendSystemMessage(Component.literal("§aПоследнее удаление отменено (Undo)!"));
            event.setCanceled(true);
        }
    }
}
