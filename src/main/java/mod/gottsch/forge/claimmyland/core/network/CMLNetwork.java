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
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.registry.PlayerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;
import java.util.UUID;

/**
 * central network channel for Claim My Land.
 * handles all packet registration and sending helpers.
 *
 * @author Mark Gottschling on 3/3/2026
 */
public class CMLNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static SimpleChannel CHANNEL;

    /**
     * Registers the network channel and all packets.
     * Call from CommonSetup.init() inside event.enqueueWork().
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

        ClaimMyLand.LOGGER.debug("CMLNetwork registered {} packet(s)", id);
    }

    // -------------------------------------------------------------------------
    // Sending helpers
    // -------------------------------------------------------------------------

    /**
     * Sends a cache sync packet to a single player.
     * Use after a BST hit in resolveParcelCached(), or on player login.
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
     * Used when the executing player would be excluded by TRACKING_CHUNK.
     */
    public static void syncParcelToPlayer(ServerLevel level, ServerPlayer player, Parcel parcel) {
        String ownerName = resolveOwnerName(level, parcel.getEstate().getOwnerId());
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncParcelPacket(parcel, ownerName));
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

    /**
     * sends a remove notification to all players tracking the chunk the parcel was in.
     * Call from ParcelRegistry.unregisterParcel().
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
     * Sends all parcels to a player on login.
     * Call from PlayerEvent.PlayerLoggedInEvent in ModEvents.
     */
    public static void syncAllParcelsToPlayer(ServerPlayer player) {
        List<SyncParcelPacket> packets = ParcelRegistry.getParcels().stream()
                .map(parcel -> new SyncParcelPacket(parcel,
                        resolveOwnerName(player.serverLevel(), parcel.getEstate().getOwnerId())))
                .toList();
//        CHANNEL.sendTo(
//                new SyncAllParcelsPacket(packets),
//                player.connection.connection,
//                NetworkDirection.PLAY_TO_CLIENT
//        );
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncAllParcelsPacket(packets));

        ClaimMyLand.LOGGER.debug("CMLNetwork: synced {} parcel(s) to player {}",
                packets.size(), player.getScoreboardName());
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
}