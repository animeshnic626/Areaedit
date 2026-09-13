package animeshnic626.areaedit.item;

import animeshnic626.areaedit.network.ModNetwork;
import animeshnic626.areaedit.network.ServerboundAreaActionPacket;
import animeshnic626.areaedit.selection.SelectionManager;
import animeshnic626.areaedit.selection.SelectionState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.ArrayList;

public class SmartLanternItem extends Item {
    public SmartLanternItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() && hand == InteractionHand.MAIN_HAND) {
            if (!SelectionManager.getSelectedColumns().isEmpty()) {
                ModNetwork.sendToServer(new ServerboundAreaActionPacket(
                        ServerboundAreaActionPacket.Action.DELETE,
                        new ArrayList<>(SelectionManager.getSelectedColumns().values()),
                        SelectionState.getCustomMaxY()
                ));
                SelectionManager.clearAll();
            }
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide() && context.getHand() == InteractionHand.MAIN_HAND) {
            if (!SelectionManager.getSelectedColumns().isEmpty()) {
                ModNetwork.sendToServer(new ServerboundAreaActionPacket(
                        ServerboundAreaActionPacket.Action.DELETE,
                        new ArrayList<>(SelectionManager.getSelectedColumns().values()),
                        SelectionState.getCustomMaxY()
                ));
                SelectionManager.clearAll();
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean onBlockStartBreak(ItemStack stack, net.minecraft.core.BlockPos pos, Player player) {
        return true;
    }
}
