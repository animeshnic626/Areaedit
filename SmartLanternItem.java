package animeshnic626.areaedit;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;

public class SmartLanternItem extends Item {

    public SmartLanternItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            SelectionData.executeDeletion(level);
            player.sendSystemMessage(Component.literal("§cЗапущено удаление всех выделенных зон до бедрока!"));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide && context.getPlayer() != null) {
            SelectionData.executeDeletion(context.getLevel());
            context.getPlayer().sendSystemMessage(Component.literal("§cЗапущено удаление всех выделенных зон до бедрока!"));
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
