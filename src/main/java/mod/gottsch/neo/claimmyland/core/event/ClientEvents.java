/*
 * This file is part of Claim My Land.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
 *
 * Claim My Land is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Claim My Land is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Claim My Land.  If not, see <http://www.gnu.org/licenses/lgpl>.
 *
 */

package mod.gottsch.neo.claimmyland.core.event;

import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.client.hud.ParcelEntryTitleRenderer;
import mod.gottsch.neo.claimmyland.client.renderer.ParcelBorderRenderer;
import mod.gottsch.neo.claimmyland.core.cache.ClientParcelCache;
import mod.gottsch.neo.claimmyland.core.integration.journeymap.ParcelPolygonOverlayFactory;
import mod.gottsch.neo.claimmyland.core.item.Deed;
import mod.gottsch.neo.gottschcore.spatial.Coords;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;


/**
 * Client-side event handlers for Claim My Land.
 *
 * Performs instant protection checks against {@link ClientParcelCache} before
 * events reach the server, eliminating block break/place redraw glitches.
 *
 * All handlers are client-only (Dist.CLIENT) and guard with isClientSide()
 * to ensure they never run on a server thread.
 *
 * @author by Mark Gottschling on 3/3/2026
 */
@EventBusSubscriber(modid = ClaimMyLand.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientEvents {

    // -------------------------------------------------------------------------
    // Cache invalidation
    // -------------------------------------------------------------------------

    /**
     * Clear the client cache when the player disconnects or changes dimension.
     * The server will repopulate it via CacheSyncPacket after the transition.
     */
    @SubscribeEvent
    public static void onPlayerLogout(final PlayerEvent.PlayerLoggedOutEvent event) {
        ClientParcelCache.setWilderness();
        ParcelBorderRenderer.clearConflictHighlights();
        if (ModList.get().isLoaded("journeymap")) {
            ParcelPolygonOverlayFactory.clearConflictOverlays();
        }
        ParcelEntryTitleRenderer.clearCooldowns();
//        ClaimMyLand.LOGGER.debug("ClientEvents: cache cleared on logout");
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(final PlayerEvent.PlayerChangedDimensionEvent event) {
        ClientParcelCache.setWilderness();
        ParcelBorderRenderer.clearConflictHighlights();
        if (ModList.get().isLoaded("journeymap")) {
            ParcelPolygonOverlayFactory.clearConflictOverlays();
        }
        ParcelEntryTitleRenderer.clearCooldowns();
//        ClaimMyLand.LOGGER.debug("ClientEvents: cache cleared on dimension change");
    }

    // -------------------------------------------------------------------------
    // Block break — primary glitch prevention
    // -------------------------------------------------------------------------

    /**
     * Cancels a block break on the client if the target block is inside the
     * cached parcel. This prevents the break animation from starting and
     * eliminates the break-then-redraw glitch entirely.
     *
     * EventPriority.HIGH runs before other mods that might also cancel breaks,
     * matching the priority used in ModEvents on the server side.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockBreak(final BlockEvent.BreakEvent event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }

        if (!isLocalPlayer(event.getPlayer())) {
            return;
        }

        BlockPos pos = event.getPos();
        String dimension = getDimensionString(event.getPlayer());

        if (ClientParcelCache.isProtected(pos.getX(), pos.getY(), pos.getZ(), dimension)) {
            event.setCanceled(true);
//            ClaimMyLand.LOGGER.debug("ClientEvents: block break cancelled by client cache @ {}",
//                    Coords.of(pos).toShortString());
        }
    }

    // -------------------------------------------------------------------------
    // Block place — prevent placing into a cached protected parcel
    // -------------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockPlace(final BlockEvent.EntityPlaceEvent event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof Player player) || !isLocalPlayer(player)) {
            return;
        }

        BlockPos pos = event.getPos();
        String dimension = getDimensionString(player);

        if (ClientParcelCache.isProtected(pos.getX(), pos.getY(), pos.getZ(), dimension)) {
            event.setCanceled(true);
//            ClaimMyLand.LOGGER.debug("ClientEvents: block place cancelled by client cache @ {}",
//                    Coords.of(pos).toShortString());
        }
    }

    // -------------------------------------------------------------------------
    // Right-click block — prevent interactions in cached protected parcel
    // -------------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerInteractBlock(final PlayerInteractEvent.RightClickBlock event) {

        if (!event.getLevel().isClientSide()) {
//            ClaimMyLand.LOGGER.debug("onPlayerInteractBlock: SKIPPING — hand={}, reason=service side",
//                    event.getHand());
            return;
        }

        if (!isLocalPlayer(event.getEntity())) {
//            ClaimMyLand.LOGGER.debug("onPlayerInteractBlock: SKIPPING — hand={}, reason=not local player",
//                    event.getHand());
            return;
        }

        BlockPos pos = event.getPos();
        String dimension = getDimensionString(event.getEntity());

        if (ClientParcelCache.isProtected(pos.getX(), pos.getY(), pos.getZ(), dimension)) {
            /*
             * Deeds bypass the client-side cancel — a non-owner can legitimately
             * use a deed inside a protected parcel (e.g. placing a Citizen deed
             * inside another player's Nation to claim a Zone-embedded Citizen).
             * Server-side canPlaceAt → PlacementResult is the authoritative
             * access gate for deeds.
             *
             * The event fires once per hand (MAIN_HAND first, OFF_HAND after);
             * checking the main-hand item regardless of which hand fired
             * handles the double-cancel cleanly — if the main hand holds a
             * deed, that's what the player intends to use and neither hand's
             * event should cancel. The current-hand check covers the rare
             * empty-main-hand + off-hand-deed case.
             *
             * Other CML interaction items (CitizenTool, ZoningTool, name tags)
             * are owner-only and are never invoked inside a protected parcel
             * — no bypass needed.
             */
            Player player = event.getEntity();
            if (player.getMainHandItem().getItem() instanceof Deed
                    || event.getItemStack().getItem() instanceof Deed) {
                return;
            }
            event.setCanceled(true);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns true if the given player is the local client player.
     * Guards against split-screen or other edge cases where a non-local
     * player entity might trigger a client event.
     */
    private static boolean isLocalPlayer(Player player) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getUUID().equals(player.getUUID());
    }

    /**
     * Returns the dimension string for the given player's current level.
     * Matches the format used by AbstractParcel.DEFAULT_DIMENSION and
     * getDimensionString() in ModEvents.
     */
    private static String getDimensionString(Player player) {
        return player.level().dimension().location().toString();
    }
}