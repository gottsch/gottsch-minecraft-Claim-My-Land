package mod.gottsch.neo.claimmyland;

import com.google.common.reflect.TypeToken;
import mod.gottsch.neo.claimmyland.client.renderer.ParcelBorderRendererSetup;
import mod.gottsch.neo.claimmyland.core.block.ModBlocks;
import mod.gottsch.neo.claimmyland.core.block.entity.ModBlockEntities;
import mod.gottsch.neo.claimmyland.core.config.Config;
import mod.gottsch.neo.claimmyland.core.item.ModItems;
import mod.gottsch.neo.claimmyland.core.loot.ModLootModifiers;
import mod.gottsch.neo.claimmyland.core.network.CMLNetwork;
import mod.gottsch.neo.claimmyland.core.parcel.Parcel;
import mod.gottsch.neo.claimmyland.core.persistence.ParcelGson;
import mod.gottsch.neo.claimmyland.core.persistence.RollingJsonSaver;
import mod.gottsch.neo.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.neo.claimmyland.core.setup.ClientSetup;
import mod.gottsch.neo.claimmyland.core.setup.CommonSetup;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Type;
import java.nio.file.Path;
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
    private static Path serverDataPath;

    /**
     *
     */
    public ClaimMyLand(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);

        // register the deferred registries
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModLootModifiers.register(modEventBus);
        CMLNetwork.register(modEventBus);

        // register 'ModSetup::init' to be called at mod setup time (server and client)
        modEventBus.addListener(CommonSetup::init);
        modEventBus.addListener(ClientSetup::init);
        modEventBus.addListener(this::onConfigReload);

        // register NEOFORGE bus events separately
        NeoForge.EVENT_BUS.register(new ModEventHandler());
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.register(ParcelBorderRendererSetup.class);
        }
    }

    public void onServerStarting(ServerStartingEvent event) {
        serverDataPath = event.getServer()
                .getWorldPath(LevelResource.ROOT)
                .resolve("data/claimmyland");
        ClaimMyLand.reinitBackup();
    }

    /**
     * @author Mark Gottschling on March 26, 2026
     */
    public void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getModId().equals(MOD_ID)) {
            ClaimMyLand.reinitBackup();
        }
    }

    /**
     * Constructs or reconstructs the RollingJsonSaver from current config values.
     * Called once at startup (after serverDataPath is set) and again on config reload.
     */
    public static void reinitBackup() {
        if (serverDataPath == null) return;
        if (Config.SERVER.backup.enabled.get()) {
            Type listType = new TypeToken<List<Parcel>>(){}.getType();
            parcelSaver = new RollingJsonSaver<>(
                    serverDataPath.toFile(),
                    "parcels",
                    Config.SERVER.backup.maxFiles.get(),
                    Config.SERVER.backup.intervalMinutes.get(),
                    ParcelRegistry::getParcels,
                    listType,
                    ParcelGson.create()
            );
        } else {
            parcelSaver = null;
        }
    }

    public static class ModEventHandler {
        @SubscribeEvent
        public void onServerTick(ServerTickEvent.Post event) {
            if (parcelSaver != null) {
                parcelSaver.tick();
            }
        }
    }

    public static RollingJsonSaver<List<Parcel>> getParcelSaver() {
        return parcelSaver;
    }
}
