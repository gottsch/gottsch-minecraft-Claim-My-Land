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

package mod.gottsch.forge.claimmyland.core.network;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.block.entity.BorderStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.registry.PlayerRegistry;
import mod.gottsch.forge.gottschcore.spatial.Box;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * central network channel for Claim My Land.
 * handles all packet registration and sending helpers.
 *
 * @author Mark Gottschling on 3/3/2026
 */
public class CMLNetwork {
    private static final Set<UUID> PENDING_BORDER_VISIBLE = ConcurrentHashMap.newKeySet();

    private static final String PROTOCOL_VERSION = "1";

    public static SimpleChannel CHANNEL;

    /**
     * registers the network channel and all packets.
     * call from CommonSetup.init() inside event.enqueueWork().
     */
    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(ClaimMyLand.MOD_ID, "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );

        int id = 0;

        // Cache sync — server → client. Tells the client which parcel they are
        // currently inside (or null if wilderness).
        CHANNEL.registerMessage(
                id++,
                CacheSyncPacket.class,
                CacheSyncPacket::encode,
                CacheSyncPacket::decode,
                CacheSyncPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        // Single parcel sync — sent on claim/demolish to nearby players.
        CHANNEL.registerMessage(
                id++,
                SyncParcelPacket.class,
                SyncParcelPacket::encode,
                SyncParcelPacket::decode,
                SyncParcelPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        // Bulk parcel sync — sent to a player on login.
        CHANNEL.registerMessage(
                id++,
                SyncAllParcelsPacket.class,
                SyncAllParcelsPacket::encode,
                SyncAllParcelsPacket::decode,
                SyncAllParcelsPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        // Remove parcel — sent when a parcel is demolished.
        CHANNEL.registerMessage(
                id++,
                RemoveParcelPacket.class,
                RemoveParcelPacket::encode,
                RemoveParcelPacket::decode,
                RemoveParcelPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        // Border visibility — sent when a Border/Foundation Stone state changes.
        CHANNEL.registerMessage(
                id++,
                BorderVisibilityPacket.class,
                BorderVisibilityPacket::encode,
                BorderVisibilityPacket::decode,
                BorderVisibilityPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        ClaimMyLand.LOGGER.debug("CMLNetwork registered {} packet(s)", id);
    }

    // -------------------------------------------------------------------------
    // Sending helpers
    // -------------------------------------------------------------------------

    /**
     * sends a cache sync packet to a single player.
     * use after a BST hit in resolveParcelCached(), or on player login.
     *
     * @param player the target player
     * @param parcel the parcel the player is now inside, or null for wilderness
     */
    public static void syncCacheToPlayer(ServerPlayer player, Parcel parcel) {
        // resolve owner name here where ServerLevel is available.
        // getPlayerName() checks in-memory first, then falls back to Mojang API.
        String ownerName = null;
        if (parcel != null && parcel.getEstate().getOwnerId() != null) {
            ownerName = PlayerRegistry.getPlayerName(player.serverLevel(), parcel.getEstate().getOwnerId())
                    .orElse(null);
        }
//        CHANNEL.sendTo(
//                new CacheSyncPacket(parcel, ownerName),
//                player.connection.connection,
//                NetworkDirection.PLAY_TO_CLIENT
//        );
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new CacheSyncPacket(parcel, ownerName)
        );
    }

    public static void syncWildernessToPlayer(ServerPlayer player) {
        syncCacheToPlayer(player, null);
    }

    /**
     * send a single parcel sync directly to one player.
     * used when the executing player would be excluded by TRACKING_CHUNK.
     */
    public static void syncParcelToPlayer(ServerLevel level, ServerPlayer player, Parcel parcel) {
        String ownerName = resolveOwnerName(level, parcel.getEstate().getOwnerId());
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncParcelPacket(parcel, ownerName));
    }
    public static void syncParcelToPlayer(ServerLevel level, ServerPlayer player, Parcel parcel, int borderStoneY) {
        String ownerName = resolveOwnerName(level, parcel.getEstate().getOwnerId());
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncParcelPacket(parcel, ownerName, borderStoneY));
    }


    /** send to all players tracking the chunk, resolving owner name internally. */
    public static void syncParcelToTrackingPlayers(ServerLevel level, Parcel parcel) {
        syncParcelToTrackingPlayers(level, parcel,
                resolveOwnerName(level, parcel.getEstate().getOwnerId()));
    }

    public static void syncParcelToTrackingPlayers(ServerLevel level, Parcel parcel, String ownerName) {
        SyncParcelPacket packet = new SyncParcelPacket(parcel, ownerName);
        CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() ->
                        level.getChunkAt(new BlockPos(
                                parcel.getMinCoords().getX(),
                                parcel.getMinCoords().getY(),
                                parcel.getMinCoords().getZ()))),
                packet
        );
    }

    public static void syncParcelToTrackingPlayers(ServerLevel level, Parcel parcel, int borderStoneY) {
        syncParcelToTrackingPlayers(level, parcel,
                resolveOwnerName(level, parcel.getEstate().getOwnerId()), borderStoneY);
    }

    public static void syncParcelToTrackingPlayers(ServerLevel level, Parcel parcel, String ownerName, int borderStoneY) {
        SyncParcelPacket packet = new SyncParcelPacket(parcel, ownerName, borderStoneY);
        CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() ->
                        level.getChunkAt(new BlockPos(
                                parcel.getMinCoords().getX(),
                                parcel.getMinCoords().getY(),
                                parcel.getMinCoords().getZ()))),
                packet
        );
    }

    /**
     * sends a remove notification to all players tracking the chunk the parcel was in.
     * call from ParcelRegistry.unregisterParcel().
     */
    public static void removeParcelFromTracking(ServerLevel level, Parcel parcel) {
        CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() ->
                        level.getChunkAt(
                                new BlockPos(
                                        parcel.getMinCoords().getX(),
                                        parcel.getMinCoords().getY(),
                                        parcel.getMinCoords().getZ()))),
                new RemoveParcelPacket(parcel.getId())
        );
    }

    /**
     * Sends a RemoveParcelPacket to all players tracking the chunk at pos.
     * Used when no Parcel object is available (e.g. FoundationStone TTL expiry).
     */
    public static void removeParcelFromTracking(ServerLevel level, UUID parcelId, BlockPos pos) {
        CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)),
                new RemoveParcelPacket(parcelId)
        );
    }

    /**
     * sends a border visibility update to all players tracking the chunk.
     * call from the Border/Foundation Stone BlockEntity tick on state change.
     *
     * @param level       the server level
     * @param parcel      the parcel whose visibility state changed
     * @param visible     whether the border should be rendered
     * @param conflictState 0 = NONE, 1 = CONFLICT
     */
    public static void syncBorderVisibilityToTrackingPlayers(ServerLevel level, Parcel parcel,
                                                             boolean visible, int conflictState,
                                                             int borderStoneY) {
        CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() ->
                        level.getChunkAt(new BlockPos(
                                parcel.getMinCoords().getX(),
                                parcel.getMinCoords().getY(),
                                parcel.getMinCoords().getZ()))),
                new BorderVisibilityPacket(parcel.getId(), visible, conflictState, borderStoneY)
        );
    }

    /**
     * sends all parcels to a player on login.
     * call from PlayerEvent.PlayerLoggedInEvent in ModEvents.
     */
    public static void syncAllParcelsToPlayer(ServerPlayer player) {
        List<SyncParcelPacket> packets = ParcelRegistry.getParcels().stream()
                .map(parcel -> new SyncParcelPacket(parcel,
                        resolveOwnerName(player.serverLevel(), parcel.getEstate().getOwnerId())))
                .toList();
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncAllParcelsPacket(packets));

        ClaimMyLand.LOGGER.debug("CMLNetwork: synced {} parcel(s) to player {}",
                packets.size(), player.getScoreboardName());

        // sync border visibility for all active Tier 2 border stones
        for (BorderStoneBlockEntity stone : BorderStoneBlockEntity.ACTIVE_TIER2) {
            if (stone.getParcelId() == null || stone.getLevel() == null) continue;
            ParcelRegistry.findByParcelId(stone.getParcelId()).ifPresent(parcel ->
                    CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new BorderVisibilityPacket(
                                    parcel.getId(), true, 0, stone.getBlockPos().getY()))
            );
        }
    }

    public static void syncPreviewParcelToTrackingPlayers(ServerLevel level, UUID parcelId,
                                                          UUID estateId, UUID ownerId,
                                                          ParcelType parcelType,
                                                          Box box, int stoneY, String dimension) {
        String ownerName = resolveOwnerName(level, ownerId);
        SyncParcelPacket packet = SyncParcelPacket.forPreview(
                parcelId, estateId, ownerId, ownerName, parcelType, box, stoneY, dimension);
        CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() ->
                        level.getChunkAt(new BlockPos(
                                box.getMinCoords().getX(),
                                box.getMinCoords().getY(),
                                box.getMinCoords().getZ()))),
                packet
        );
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    @Deprecated
    private static String resolveOwnerName(ServerPlayer player, Parcel parcel) {
        if (parcel.getEstate().getOwnerId() == null) return "";
        return PlayerRegistry.getPlayerName(player.serverLevel(), parcel.getEstate().getOwnerId())
                .orElse("");
    }

    @Deprecated
    private static String resolveOwnerName(ServerLevel level, Parcel parcel) {
        if (parcel.getEstate().getOwnerId() == null) return ""; // NOTE this should never be null
        return PlayerRegistry.getPlayerName(level, parcel.getEstate().getOwnerId())
                .orElse("");
    }

    private static String resolveOwnerName(ServerLevel level, UUID ownerId) {
        if (ownerId == null) return "";
        return PlayerRegistry.getPlayerName(level, ownerId).orElse("");
    }

    public static void queueBorderVisible(UUID parcelId) {
        PENDING_BORDER_VISIBLE.add(parcelId);
    }
}