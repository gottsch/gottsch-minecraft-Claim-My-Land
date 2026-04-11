package mod.gottsch.neo.claimmyland.core.event;

import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.block.FoundationStone;
import mod.gottsch.neo.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.neo.claimmyland.core.integration.journeymap.ParcelPolygonOverlayFactory;
import mod.gottsch.neo.claimmyland.core.parcel.ClientParcel;
import mod.gottsch.neo.claimmyland.core.registry.ClientParcelRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

/**
 * Client-side event handler that refreshes conflict highlights when the player
 * right-clicks an already-placed Foundation Stone.
 *
 * <p>Initial conflict detection on stone placement (via Deed) is handled by
 * {@code SyncParcelPacket.handle()} when the preview parcel arrives.
 * This handler covers the case where the player right-clicks the stone after
 * placement to get an updated conflict check (e.g. after a competing player
 * commits their claim).</p>
 *
 * @author Mark Gottschling on March 20, 2026
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = ClaimMyLand.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class FoundationStoneEvents {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!ModList.get().isLoaded("journeymap")) return;
        if (event.getEntity() instanceof FakePlayer) return;

        Level level = event.getLevel();
        if (!level.isClientSide()) return;

        BlockPos pos = event.getPos();
        if (!(level.getBlockState(pos).getBlock() instanceof FoundationStone)) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof FoundationStoneBlockEntity fsbe)) return;
        if (fsbe.getParcelId() == null) return;
        if (fsbe.getAbsoluteBox() == null) return;

        ClientParcel preview = ClientParcelRegistry.findById(fsbe.getParcelId()).orElse(null);
        if (preview == null) return;

        List<ClientParcel> conflicting = ClientParcelRegistry.findConflicting(preview);

        int minX = fsbe.getAbsoluteBox().getMinCoords().getX();
        int minY = fsbe.getAbsoluteBox().getMinCoords().getY();
        int minZ = fsbe.getAbsoluteBox().getMinCoords().getZ();
        int maxX = fsbe.getAbsoluteBox().getMaxCoords().getX();
        int maxY = fsbe.getAbsoluteBox().getMaxCoords().getY();
        int maxZ = fsbe.getAbsoluteBox().getMaxCoords().getZ();

        ResourceKey<Level> dimensionKey = level.dimension();
        ParcelPolygonOverlayFactory.showPreviewOverlay(
                fsbe.getParcelId(),
                minX, minY, minZ,
                maxX, maxY, maxZ,
                !conflicting.isEmpty(),
                dimensionKey);
    }
}