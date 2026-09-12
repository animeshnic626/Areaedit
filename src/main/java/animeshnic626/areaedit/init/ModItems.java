package animeshnic626.areaedit.init;

import animeshnic626.areaedit.Areaedit;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Areaedit.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Areaedit.MODID);

    public static final RegistryObject<Item> PINK_STICK = ITEMS.register("pink_stick",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> PINK_BUCKET = ITEMS.register("pink_bucket",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> PINK_LANTERN = ITEMS.register("pink_lantern",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CreativeModeTab> AREAEDIT_TAB = TABS.register("areaedit_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + Areaedit.MODID + ".areaedit_tab"))
                    .icon(() -> new ItemStack(PINK_BUCKET.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(PINK_STICK.get());
                        output.accept(PINK_BUCKET.get());
                        output.accept(PINK_LANTERN.get());
                    })
                    .build());
}