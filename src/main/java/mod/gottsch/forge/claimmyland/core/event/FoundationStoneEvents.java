package mod.gottsch.forge.claimmyland.core.event;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.block.FoundationStone;
import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.claimmyland.client.renderer.ParcelBorderRenderer;
import mod.gottsch.forge.claimmyland.core.integration.journeymap.ParcelPolygonOverlayFactory;
import mod.gottsch.forge.claimmyland.core.parcel.ClientParcel;
import mod.gottsch.forge.claimmyland.core.registry.ClientParcelRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side event handler that triggers the JourneyMap Foundation Stone
 * preview overlay on right-click, and highlights any conflicting parcels
 * both on the JourneyMap and in-world via ParcelBorderRenderer.
 *
 * @author Mark Gottschling on March 20, 2026
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = ClaimMyLand.MOD_ID, value = Dist.CLIENT)
public class FoundationStoneEvents {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof FakePlayer) return;

        Level level = event.getLevel();
        if (!level.isClientSide()) return;

        BlockPos pos = event.getPos();
        if (!(level.getBlockState(pos).getBlock() instanceof FoundationStone)) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof FoundationStoneBlockEntity fsbe)) return;
        if (fsbe.getParcelId() == null) return;
        if (fsbe.getAbsoluteBox() == null) return;

        int minX = fsbe.getAbsoluteBox().getMinCoords().getX();
        int minY = fsbe.getAbsoluteBox().getMinCoords().getY();
        int minZ = fsbe.getAbsoluteBox().getMinCoords().getZ();
        int maxX = fsbe.getAbsoluteBox().getMaxCoords().getX();
        int maxY = fsbe.getAbsoluteBox().getMaxCoords().getY();
        int maxZ = fsbe.getAbsoluteBox().getMaxCoords().getZ();

        List<ClientParcel> conflicting = findConflicting(minX, minY, minZ, maxX, maxY, maxZ);
        boolean intersects = !conflicting.isEmpty();

        // --- In-world orange highlights (Feature 3b) ---
        // Pass the player's current position so the renderer can apply
        // the distance-based clear check.
        ParcelBorderRenderer.setConflictHighlights(conflicting, event.getEntity().blockPosition());

        // --- JourneyMap preview + conflict overlays (Feature 3a) ---
        if (ModList.get().isLoaded("journeymap")) {
            ResourceKey<Level> dimensionKey = level.dimension();
            ParcelPolygonOverlayFactory.showPreviewOverlay(
                    fsbe.getParcelId(),
                    minX, minY, minZ,
                    maxX, maxY, maxZ,
                    intersects,
                    dimensionKey);
            ParcelPolygonOverlayFactory.showConflictOverlays(conflicting, dimensionKey);
        }
    }

    /**
     * Returns all committed (non-preview) parcels in the client registry whose
     * bounds overlap the proposed parcel bounds.
     *
     * <p>Preview parcels are intentionally excluded — two players are allowed to
     * propose overlapping claims simultaneously; only the first to commit wins.
     * Conflict against previews would be a false-positive because neither parcel
     * exists yet in the server-side BST.</p>
     *
     * <p>Known limitation: after a competing player commits their claim, the
     * surviving preview will not show the conflict until the next right-click on
     * this foundation stone. The committed parcel is broadcast via SyncParcelPacket
     * (isPreview=false) immediately on commit, so the very next trigger will detect
     * it correctly.</p>
     */
    private static List<ClientParcel> findConflicting(int minX, int minY, int minZ,
                                                      int maxX, int maxY, int maxZ) {
        List<ClientParcel> result = new ArrayList<>();
        for (ClientParcel parcel : ClientParcelRegistry.getAll()) {
            if (parcel.isPreview()) continue;
            if (parcel.maxX() < minX || parcel.minX() > maxX) continue;
            if (parcel.maxY() < minY || parcel.minY() > maxY) continue;
            if (parcel.maxZ() < minZ || parcel.minZ() > maxZ) continue;
            result.add(parcel);
        }
        return result;
    }
}