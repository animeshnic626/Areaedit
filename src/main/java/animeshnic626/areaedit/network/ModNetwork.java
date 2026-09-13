package animeshnic626.areaedit.network;

import animeshnic626.areaedit.Areaedit;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Areaedit.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.messageBuilder(ServerboundAreaActionPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(ServerboundAreaActionPacket::new)
                .encoder(ServerboundAreaActionPacket::encode)
                .consumerMainThread(ServerboundAreaActionPacket::handle)
                .add();
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }
}
