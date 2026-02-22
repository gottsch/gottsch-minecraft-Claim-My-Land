package mod.gottsch.forge.claimmyland;

import com.google.common.reflect.TypeToken;
import mod.gottsch.forge.claimmyland.core.block.ModBlocks;
import mod.gottsch.forge.claimmyland.core.block.entity.ModBlockEntities;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.item.ModItems;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.persistence.RollingJsonSaver;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.setup.CommonSetup;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.reflect.Type;
import java.util.List;

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

    private static RollingJsonSaver<List<Parcel>> parcelSaver;

    /**
     *
     */
    public ClaimMyLand() {
        Config.register();

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // register the deferred registries
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);

        // register the setup method for mod loading

        // register 'ModSetup::init' to be called at mod setup time (server and client)
        modEventBus.addListener(CommonSetup::init);
//        modEventBus.addListener(this::config);

        // register 'ClientSetup::init' to be called at mod setup time (client only)
//        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modEventBus.addListener(ClientSetup::init));

//        modEventBus.addListener(this::onKeyRegister);


        File saveDir = new File("world/data/claimmyland");
        Type listType = new TypeToken<List<Parcel>>(){}.getType();

        // save every 5 minutes, keep 20 most recent files
        parcelSaver = new RollingJsonSaver<>(
                saveDir,
                "parcels",
                20,
                3,
                ParcelRegistry::getParcels,  // supplier that returns current parcel list
                listType
        );

//        // Load latest on startup
//        List<Parcel> loaded = parcelSaver.loadLatest();
//        if (loaded != null) {
//            parcelList = loaded;
//        }

        // register FORGE bus events separately
        MinecraftForge.EVENT_BUS.register(new ForgeEventHandler());
    }

    public static class ForgeEventHandler {
        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                parcelSaver.tick();
            }
        }
    }

//    private void onKeyRegister(RegisterKeyMappingsEvent event) {
//        System.out.println("Registering key mappings!");
//        System.out.println("Toggle HUD key: " + KeyBindings.TOGGLE_HUD.getName());
//        System.out.println("Scroll Up key: " + KeyBindings.SCROLL_HUD_UP.getName());
//        System.out.println("Scroll Down key: " + KeyBindings.SCROLL_HUD_DOWN.getName());
//    }
}
