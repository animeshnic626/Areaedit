package animeshnic626.areaedit.init;

import animeshnic626.areaedit.Areaedit;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Areaedit.MODID);

    public static final RegistryObject<CreativeModeTab> AREAEDIT_TAB = CREATIVE_MODE_TABS.register("areaedit_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.SMART_STICK.get()))
                    .title(Component.translatable("creativetab.areaedit_tab"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.SMART_STICK.get());
                        output.accept(ModItems.SMART_BUCKET.get());
                        output.accept(ModItems.SMART_LANTERN.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
