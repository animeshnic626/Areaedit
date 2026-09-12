package animeshnic626.areaedit;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;

public class SmartBucketItem extends Item {

    public SmartBucketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide && context.getPlayer() != null) {
            // Исправлено: вызываем правильный метод fillBoundedArea вместо smartFloodFill
            SelectionData.fillBoundedArea(context.getLevel(), context.getClickedPos(), context.getPlayer());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
