package animeshnic626.areaedit.init;

import animeshnic626.areaedit.Areaedit;
import animeshnic626.areaedit.item.SmartStickItem;
import animeshnic626.areaedit.item.SmartBucketItem;
import animeshnic626.areaedit.item.SmartLanternItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Areaedit.MODID);

    public static final RegistryObject<Item> SMART_STICK = ITEMS.register("pink_stick",
            () -> new SmartStickItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SMART_BUCKET = ITEMS.register("pink_bucket",
            () -> new SmartBucketItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SMART_LANTERN = ITEMS.register("pink_lantern",
            () -> new SmartLanternItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
