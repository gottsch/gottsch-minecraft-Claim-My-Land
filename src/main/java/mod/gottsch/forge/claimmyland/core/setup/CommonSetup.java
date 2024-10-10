package mod.gottsch.forge.claimmyland.core.setup;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.item.ModItems;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 *
 * @author Mark Gottschling on Sep 14, 2024
 *
 */
@Mod.EventBusSubscriber(modid = ClaimMyLand.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonSetup {

    /**
     *
     * @param event
     */
    public static void init(final FMLCommonSetupEvent event) {
        // create a claimmyland specific log file
        Config.instance.addRollingFileAppender(ClaimMyLand.MOD_ID);
        ClaimMyLand.LOGGER.debug("file appender created");
    }

    @SubscribeEvent
    public static void registemItemsToTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.BORDER_STONE.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.accept(ModItems.PLAYER_DEED_10.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.accept(ModItems.PLAYER_DEED_16.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.accept(ModItems.PLAYER_DEED_32.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.accept(ModItems.NATION_DEED_100.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.accept(ModItems.ZONING_TOOL.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        }
    }
}
