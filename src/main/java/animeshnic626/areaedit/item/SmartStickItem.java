package animeshnic626.areaedit.item;

import animeshnic626.areaedit.selection.ColumnPos;
import animeshnic626.areaedit.selection.SelectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class SmartStickItem extends Item {
    public SmartStickItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            BlockPos clickedPos = context.getClickedPos();
            int x = clickedPos.getX();
            int z = clickedPos.getZ();

            int minBuildHeight = level.getMinBuildHeight();
            int maxBuildHeight = level.getMaxBuildHeight();

            int lowestY = -1;
            int highestY = -1;

            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

            for (int y = minBuildHeight; y < maxBuildHeight; y++) {
                mutablePos.set(x, y, z);
                if (!level.getBlockState(mutablePos).isAir()) {
                    if (lowestY == -1) {
                        lowestY = y;
                    }
                    highestY = y;
                }
            }

            ColumnPos colPos = new ColumnPos(x, z);
            if (lowestY != -1 && highestY != -1) {
                SelectionManager.addColumn(colPos, lowestY, highestY);
            } else {
                SelectionManager.addColumn(colPos, clickedPos.getY(), clickedPos.getY());
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, Player player) {
        // Блокируем разрушение на случай, если клик дошел до стадии ломания
        return true;
    }
}
