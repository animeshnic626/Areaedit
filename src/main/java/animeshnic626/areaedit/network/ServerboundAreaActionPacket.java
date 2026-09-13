package animeshnic626.areaedit.network;

import animeshnic626.areaedit.execution.BatchBlockExecutor;
import animeshnic626.areaedit.selection.ColumnPos;
import animeshnic626.areaedit.selection.ColumnSelection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ServerboundAreaActionPacket {
    public enum Action {
        DELETE,
        UNDO,
        REDO
    }

    private final Action action;
    private final List<ColumnSelection> columns;
    private final Integer customMaxY;

    public ServerboundAreaActionPacket(Action action, List<ColumnSelection> columns, Integer customMaxY) {
        this.action = action;
        this.columns = columns != null ? columns : new ArrayList<>();
        this.customMaxY = customMaxY;
    }

    public ServerboundAreaActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        int size = buf.readVarInt();
        this.columns = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int x = buf.readInt();
            int z = buf.readInt();
            int yMin = buf.readInt();
            int yMax = buf.readInt();
            this.columns.add(new ColumnSelection(new ColumnPos(x, z), yMin, yMax));
        }
        if (buf.readBoolean()) {
            this.customMaxY = buf.readInt();
        } else {
            this.customMaxY = null;
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeVarInt(columns.size());
        for (ColumnSelection col : columns) {
            buf.writeInt(col.getPos().x());
            buf.writeInt(col.getPos().z());
            buf.writeInt(col.getYMin());
            buf.writeInt(col.getYMax());
        }
        if (customMaxY != null) {
            buf.writeBoolean(true);
            buf.writeInt(customMaxY);
        } else {
            buf.writeBoolean(false);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                switch (action) {
                    case DELETE -> BatchBlockExecutor.executeServerDeletion(player.serverLevel(), columns, customMaxY);
                    case UNDO -> BatchBlockExecutor.performServerUndo(player.serverLevel());
                    case REDO -> BatchBlockExecutor.performServerRedo(player.serverLevel());
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
