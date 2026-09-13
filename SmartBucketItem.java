package animeshnic626.areaedit.item;

import animeshnic626.areaedit.selection.ColumnPos;
import animeshnic626.areaedit.selection.ColumnSelection;
import animeshnic626.areaedit.selection.SelectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.*;

public class SmartBucketItem extends Item {

    public SmartBucketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            BlockPos clickedPos = context.getClickedPos();
            ColumnPos startCol = new ColumnPos(clickedPos.getX(), clickedPos.getZ());

            if (SelectionManager.isStickColumn(startCol)) {
                return InteractionResult.SUCCESS;
            }

            // Запускаем постепенную заливку порциями по 10 колонок без лимитов радиуса
            SelectionManager.startGradualFloodFill(startCol, clickedPos.getY());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onBlockStartBreak(net.minecraft.world.item.ItemStack stack, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        return true;
    }
}
