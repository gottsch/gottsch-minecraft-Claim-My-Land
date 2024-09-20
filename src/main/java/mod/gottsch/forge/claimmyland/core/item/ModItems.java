package mod.gottsch.forge.claimmyland.core.item;

import mod.gottsch.forge.claimmyland.core.block.ModBlocks;
import mod.gottsch.forge.claimmyland.core.setup.Registration;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModItems {
    public static RegistryObject<Item> PLAYER_DEED = Registration.ITEMS.register("player_deed", () -> new PlayerDeed(new Item.Properties()));
    public static RegistryObject<Item> NATION_DEED = Registration.ITEMS.register("nation_deed", () -> new NationDeed(new Item.Properties()));
//    public static RegistryObject<Item> CITIZEN_DEED = Registration.ITEMS.register("citizen_deed", () -> new CitizenDeed(new Item.Properties()));
//

    public static RegistryObject<Item> BORDER_STONE = fromBorderStone(ModBlocks.BORDER_STONE, Item.Properties::new);

    public static void register(IEventBus bus) {
        // cycle through all block and create items
        Registration.registerItems(bus);
    }

    // convenience method: take a RegistryObject<Block> and make a corresponding RegistryObject<Item> from it
    public static <B extends Block> RegistryObject<Item> fromBlock(RegistryObject<B> block, Supplier<Item.Properties> itemProperties) {
        return Registration.ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), itemProperties.get()));
    }

    public static <B extends Block> RegistryObject<Item> fromBorderStone(RegistryObject<B> block, Supplier<Item.Properties> itemProperties) {
        return Registration.ITEMS.register(block.getId().getPath(), () -> new BorderStoneBlockItem(block.get(), itemProperties.get()));
    }
}
