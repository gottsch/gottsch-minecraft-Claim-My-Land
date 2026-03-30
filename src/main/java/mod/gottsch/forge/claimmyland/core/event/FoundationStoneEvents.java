package mod.gottsch.forge.claimmyland.core.event;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.block.FoundationStone;
import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.claimmyland.client.renderer.ParcelBorderRenderer;
import mod.gottsch.forge.claimmyland.core.config.ClientServerConfig;
import mod.gottsch.forge.claimmyland.core.integration.journeymap.ParcelPolygonOverlayFactory;
import mod.gottsch.forge.claimmyland.core.parcel.ClientParcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.ClientParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.DimensionHelper;
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
@Mod.EventBusSubscriber(modid = ClaimMyLand.MOD_ID, value = Dist.CLIENT)
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

        int minX = fsbe.getAbsoluteBox().getMinCoords().getX();
        int minY = fsbe.getAbsoluteBox().getMinCoords().getY();
        int minZ = fsbe.getAbsoluteBox().getMinCoords().getZ();
        int maxX = fsbe.getAbsoluteBox().getMaxCoords().getX();
        int maxY = fsbe.getAbsoluteBox().getMaxCoords().getY();
        int maxZ = fsbe.getAbsoluteBox().getMaxCoords().getZ();

        ParcelType proposedType = ParcelType.fromString(fsbe.getParcelType());
        boolean intersects = clientSideIntersects(minX, minY, minZ, maxX, maxY, maxZ, proposedType);

        ResourceKey<Level> dimensionKey = level.dimension();
        ParcelPolygonOverlayFactory.showPreviewOverlay(
                fsbe.getParcelId(),
                minX, minY, minZ,
                maxX, maxY, maxZ,
                intersects,
                dimensionKey);
    }

    private static boolean clientSideIntersects(int minX, int minY, int minZ,
                                                int maxX, int maxY, int maxZ,
                                                ParcelType proposedType) {
        int proposedBuffer = switch (proposedType) {
            case NATION -> ClientServerConfig.getNationParcelBufferRadius();
            case PLAYER, CITIZEN -> ClientServerConfig.getParcelBufferRadius();
            default -> 0;
        };

        for (ClientParcel parcel : ClientParcelRegistry.getAll()) {
            if (parcel.isPreview()) continue;

            int existingBuffer = switch (parcel.parcelType()) {
                case NATION -> ClientServerConfig.getNationParcelBufferRadius();
                case PLAYER, CITIZEN -> ClientServerConfig.getParcelBufferRadius();
                default -> 0;
            };

            // Rule 1: direct box overlap
            if (boxesOverlap(parcel.minX(), parcel.minY(), parcel.minZ(),
                    parcel.maxX(), parcel.maxY(), parcel.maxZ(),
                    minX, minY, minZ, maxX, maxY, maxZ)) return true;

            // Rule 2a: existing parcel's buffer reaches proposed box
            if (existingBuffer > 0) {
                if (boxesOverlap(parcel.minX() - existingBuffer, parcel.minY() - existingBuffer, parcel.minZ() - existingBuffer,
                        parcel.maxX() + existingBuffer, parcel.maxY() + existingBuffer, parcel.maxZ() + existingBuffer,
                        minX, minY, minZ, maxX, maxY, maxZ)) return true;
            }

            // Rule 2b: proposed parcel's own buffer reaches existing box
            if (proposedBuffer > 0) {
                if (boxesOverlap(parcel.minX(), parcel.minY(), parcel.minZ(),
                        parcel.maxX(), parcel.maxY(), parcel.maxZ(),
                        minX - proposedBuffer, minY - proposedBuffer, minZ - proposedBuffer,
                        maxX + proposedBuffer, maxY + proposedBuffer, maxZ + proposedBuffer)) return true;
            }
        }
        return false;
    }

    private static boolean boxesOverlap(int aMinX, int aMinY, int aMinZ, int aMaxX, int aMaxY, int aMaxZ,
                                        int bMinX, int bMinY, int bMinZ, int bMaxX, int bMaxY, int bMaxZ) {
        return aMaxX >= bMinX && aMinX <= bMaxX
                && aMaxY >= bMinY && aMinY <= bMaxY
                && aMaxZ >= bMinZ && aMinZ <= bMaxZ;
    }
}