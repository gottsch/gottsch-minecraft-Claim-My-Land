package mod.gottsch.forge.claimmyland.core.setup;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.config.Config;
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
}
