package animeshnic626.areaedit.undo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;

public record BlockSnapshot(BlockPos pos, BlockState state, CompoundTag nbt) {
}
