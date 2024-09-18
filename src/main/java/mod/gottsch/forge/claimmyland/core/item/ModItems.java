package mod.gottsch.forge.claimmyland.core.item;

import mod.gottsch.forge.claimmyland.core.block.ModBlocks;
import mod.gottsch.forge.claimmyland.core.setup.Registration;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static RegistryObject<Item> PERSONAL_DEED = Registration.ITEMS.register("personal_deed", () -> new PersonalDeed(new Item.Properties()));
//    public static RegistryObject<Item> NATION_DEED = Registration.ITEMS.register("nation_deed", () -> new NationDeed(new Item.Properties()));
//    public static RegistryObject<Item> CITIZEN_DEED = Registration.ITEMS.register("citizen_deed", () -> new CitizenDeed(new Item.Properties()));
//

    public static void register(IEventBus bus) {
        // cycle through all block and create items
        Registration.registerItems(bus);
    }
}
