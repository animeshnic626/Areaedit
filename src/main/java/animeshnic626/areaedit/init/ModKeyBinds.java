package animeshnic626.areaedit.init;

import animeshnic626.areaedit.Areaedit;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Areaedit.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModKeyBinds {
    public static final String KEY_CATEGORY = "key.categories.areaedit";

    public static final KeyMapping EXTEND_HEIGHT_KEY = new KeyMapping(
            "key.areaedit.extend_height",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            KEY_CATEGORY
    );

    public static final KeyMapping CAP_PLAYER_Y_KEY = new KeyMapping(
            "key.areaedit.cap_player_y",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            KEY_CATEGORY
    );

    public static final KeyMapping CLEAR_SELECTION_KEY = new KeyMapping(
            "key.areaedit.clear_selection",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            KEY_CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(EXTEND_HEIGHT_KEY);
        event.register(CAP_PLAYER_Y_KEY);
        event.register(CLEAR_SELECTION_KEY);
    }
}
