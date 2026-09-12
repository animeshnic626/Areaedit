package animeshnic626.areaedit.client;

import animeshnic626.areaedit.Areaedit;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Areaedit.MODID, value = Dist.CLIENT)
public class KeyBindings {
    public static KeyMapping toggleKey;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        toggleKey = new KeyMapping("key.areaedit.toggle_626", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.world");
        event.register(toggleKey);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && toggleKey != null && toggleKey.consumeClick()) {
            Areaedit.height626Mode = !Areaedit.height626Mode;
            mc.player.sendSystemMessage(Component.literal("§d[AreaEdit] Режим 626 высоты: " + (Areaedit.height626Mode ? "ВКЛ" : "ВЫКЛ")));
        }
    }
}