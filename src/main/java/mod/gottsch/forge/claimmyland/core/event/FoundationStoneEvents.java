package mod.gottsch.forge.claimmyland.core.event;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.block.FoundationStone;
import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
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

/**
 * Client-side event handler for the JourneyMap Foundation Stone preview overlay.
 *
 * When the player right-clicks a Foundation Stone block, reads the proposed parcel
 * bounds from the block entity (which are synced to the client via getUpdateTag()),
 * runs a client-side intersection check against ClientParcelRegistry, and delegates
 * to ParcelPolygonOverlayFactory to show or refresh the preview overlay.
 *
 * Preview overlay cleanup is handled by notifyParcelRemoved() in
 * ParcelPolygonOverlayFactory — the Foundation Stone's removal always results in a
 * RemoveParcelPacket (for the preview parcel), which flows through that path.
 *
 * @author Mark Gottschling on Mar 11, 2026
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = ClaimMyLand.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class FoundationStoneEvents {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!ModList.get().isLoaded("journeymap")) return;
        if (event.getEntity() instanceof FakePlayer) return;

        Level level = event.getLevel();
        if (!level.isClientSide()) return;

        BlockPos pos = event.getPos();

        // Only act when the targeted block is a Foundation Stone
        if (!(level.getBlockState(pos).getBlock() instanceof FoundationStone)) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof FoundationStoneBlockEntity fsbe)) return;

        // Block entity must have a parcel ID — set at placement time, synced via getUpdateTag()
        if (fsbe.getParcelId() == null) return;

        int minX = fsbe.getAbsoluteBox().getMinCoords().getX();
        int minY = fsbe.getAbsoluteBox().getMinCoords().getY();
        int minZ = fsbe.getAbsoluteBox().getMinCoords().getZ();
        int maxX = fsbe.getAbsoluteBox().getMaxCoords().getX();
        int maxY = fsbe.getAbsoluteBox().getMaxCoords().getY();
        int maxZ = fsbe.getAbsoluteBox().getMaxCoords().getZ();

        boolean intersects = clientSideIntersects(minX, minY, minZ, maxX, maxY, maxZ);
        ResourceKey<Level> dimensionKey = level.dimension();

        ParcelPolygonOverlayFactory.showPreviewOverlay(
                fsbe.getParcelId(),
                minX, minY, minZ,
                maxX, maxY, maxZ,
                intersects,
                dimensionKey);
    }

    /**
     * Checks whether the proposed parcel bounds overlap any parcel in ClientParcelRegistry.
     *
     * Intentionally dimension-blind to stay consistent with ParcelRegistry.intersectsParcel()
     * behaviour — native dimension support is deferred to the v2.3 BST refactor.
     *
     * Uses simple AABB overlap: two boxes overlap iff neither is fully outside the other
     * on any axis.
     */
    private static boolean clientSideIntersects(int minX, int minY, int minZ,
                                                int maxX, int maxY, int maxZ) {
        for (ClientParcel parcel : ClientParcelRegistry.getAll()) {
            if (parcel.maxX() < minX || parcel.minX() > maxX) continue;
            if (parcel.maxY() < minY || parcel.minY() > maxY) continue;
            if (parcel.maxZ() < minZ || parcel.minZ() > maxZ) continue;
            return true;
        }
        return false;
    }
}