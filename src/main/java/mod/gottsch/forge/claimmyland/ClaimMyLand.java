package mod.gottsch.forge.claimmyland;

import mod.gottsch.forge.claimmyland.core.block.ModBlocks;
import mod.gottsch.forge.claimmyland.core.block.entity.ModBlockEntities;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.item.ModItems;
import mod.gottsch.forge.claimmyland.core.setup.ClientSetup;
import mod.gottsch.forge.claimmyland.core.setup.CommonSetup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Mark Gottschling on Sep 14, 2024
 *
 */
@Mod(value = ClaimMyLand.MOD_ID)
public class ClaimMyLand {
    // logger
    public static Logger LOGGER = LogManager.getLogger(ClaimMyLand.MOD_ID);

    // constants
    public static final String MOD_ID = "claimmyland";

    /**
     *
     */
    public ClaimMyLand() {
        Config.register();
        // create the default configs
//        createServerConfig(Config.CHESTS_CONFIG_SPEC, "chests", CHESTS_CONFIG_VERSION);
//        createServerConfig(Config.STRUCTURE_CONFIG_SPEC, "structures", STRUCTURES_CONFIG_VERSION);
//        createServerConfig(Config.MOBS_CONFIG_SPEC, "mobs", MOBS_CONFIG_VERSION);

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // register the deferred registries
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
//        TreasureContainers.register(modEventBus);
//        TreasureParticles.register(modEventBus);
//        TreasureEntities.register(modEventBus);
//        TreasureConfiguredFeatures.register(modEventBus);
//        TreasureSounds.register(modEventBus);
//        TreasureLootModifiers.register(modEventBus);
//        TreasureCreativeModeTabs.TABS.register(modEventBus);

        // register the setup method for mod loading

        // register 'ModSetup::init' to be called at mod setup time (server and client)
        modEventBus.addListener(CommonSetup::init);
//        modEventBus.addListener(this::config);

        // register 'ClientSetup::init' to be called at mod setup time (client only)
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modEventBus.addListener(ClientSetup::init));

    }
}
