package mod.gottsch.forge.claimmyland;

import com.google.common.reflect.TypeToken;
import mod.gottsch.forge.claimmyland.client.hud.ParcelHud;
import mod.gottsch.forge.claimmyland.client.renderer.ParcelBorderRenderer;
import mod.gottsch.forge.claimmyland.client.renderer.ParcelBorderRendererSetup;
import mod.gottsch.forge.claimmyland.core.block.ModBlocks;
import mod.gottsch.forge.claimmyland.core.block.entity.ModBlockEntities;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.item.ModItems;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.persistence.RollingJsonSaver;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.setup.ClientSetup;
import mod.gottsch.forge.claimmyland.core.setup.CommonSetup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
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

        // register 'ModSetup::init' to be called at mod setup time (server and client)
        modEventBus.addListener(CommonSetup::init);
        modEventBus.addListener(ClientSetup::init);

//        File saveDir = new File("world/data/claimmyland"); // TODO config option
//        Type listType = new TypeToken<List<Parcel>>(){}.getType();
//
//        // save every 5 minutes, keep 20 most recent files
//        parcelSaver = new RollingJsonSaver<>(
//                saveDir,
//                "parcels",  // TODO config option
//                20,                                // TODO config option
//                10,                                 // TODO config option
//                ParcelRegistry::getParcels,  // supplier that returns current parcel list
//                listType
//        );

//        // Load latest on startup
//        List<Parcel> loaded = parcelSaver.loadLatest();
//        if (loaded != null) {
//            parcelList = loaded;
//        }

        // register FORGE bus events separately
        MinecraftForge.EVENT_BUS.register(new ForgeEventHandler());

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.register(ParcelBorderRendererSetup.class);
        }

    }

    // In ClaimMyLand.java ForgeEventHandler — add a new handler:
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        ClaimMyLand.reinitBackup();
    }

    /**
     * constructs or reconstructs the RollingJsonSaver from current config values.
     * called once at startup and again on config reload.
     */
    public static void reinitBackup() {
        if (Config.SERVER.backup.enabled.get()) {
            File saveDir = new File("world/data/claimmyland");
            Type listType = new TypeToken<List<Parcel>>(){}.getType();
            parcelSaver = new RollingJsonSaver<>(
                    saveDir,
                    "parcels",
                    Config.SERVER.backup.maxFiles.get(),
                    Config.SERVER.backup.intervalMinutes.get(),
                    ParcelRegistry::getParcels,
                    listType
            );
        } else {
            parcelSaver = null;
        }
    }

    public static class ForgeEventHandler {
        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                if (parcelSaver != null) {
                    parcelSaver.tick();
                }
            }
        }
    }

    public static RollingJsonSaver<List<Parcel>> getParcelSaver() {
        return parcelSaver;
    }
}
