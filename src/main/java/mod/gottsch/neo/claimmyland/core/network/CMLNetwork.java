/*
 * This file is part of  Claim My Land.
 * Copyright (c) 2024 Mark Gottschling (gottsch)
 *
 * All rights reserved.
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
 */
package mod.gottsch.neo.claimmyland.core.network;

import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.block.entity.BorderStoneBlockEntity;
import mod.gottsch.neo.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.neo.claimmyland.core.parcel.Parcel;
import mod.gottsch.neo.claimmyland.core.parcel.ParcelType;
import mod.gottsch.neo.claimmyland.core.registry.ActiveBorderStoneRegistry;
import mod.gottsch.neo.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.neo.claimmyland.core.registry.PlayerRegistry;
import mod.gottsch.neo.gottschcore.spatial.Box;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.*;

/**
 * @author Mark Gottschling on March 3, 2026
 */
public class CMLNetwork {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(CMLNetwork::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ClaimMyLand.MOD_ID).versioned("2");

        registrar.playToClient(
                CacheSyncPacket.TYPE,
                CacheSyncPacket.STREAM_CODEC,
                CacheSyncPacket::handle
        );
        registrar.playToClient(
                SyncParcelPacket.TYPE,
                SyncParcelPacket.STREAM_CODEC,
                SyncParcelPacket::handle
        );
        registrar.playToClient(
                SyncAllParcelsPacket.TYPE,
                SyncAllParcelsPacket.STREAM_CODEC,
                SyncAllParcelsPacket::handle
        );
        registrar.playToClient(
                BorderVisibilityPacket.TYPE,
                BorderVisibilityPacket.STREAM_CODEC,
                BorderVisibilityPacket::handle
        );
        registrar.playToClient(
                RemoveParcelPacket.TYPE,
                RemoveParcelPacket.STREAM_CODEC,
                RemoveParcelPacket::handle
        );
        registrar.playToClient(
                ClaimCelebrationPacket.TYPE,
                ClaimCelebrationPacket.STREAM_CODEC,
                ClaimCelebrationPacket::handle
        );
        registrar.playToClient(
                ServerConfigSyncPacket.TYPE,
                ServerConfigSyncPacket.STREAM_CODEC,
                ServerConfigSyncPacket::handle
        );
    }

    // =========================================================================
    // Cache sync — sends the parcel the player is currently standing in
    // =========================================================================

    public static void syncCacheToPlayer(ServerPlayer player, Parcel parcel) {
        String ownerName = null;
        if (parcel != null && parcel.getEstate().getOwnerId() != null) {
            ownerName = PlayerRegistry.getPlayerName(player.serverLevel(), parcel.getEstate().getOwnerId())
                    .orElse(null);
        }
        PacketDistributor.sendToPlayer(player, new CacheSyncPacket(parcel, ownerName));
    }

    public static void syncWildernessToPlayer(ServerPlayer player) {
        syncCacheToPlayer(player, null);
    }

    // -------------------------------------------------------------------------
    // Parcel sync
    // -------------------------------------------------------------------------

    public static void syncParcelToPlayer(ServerLevel level, ServerPlayer player, Parcel parcel) {
        String ownerName = resolveOwnerName(level, parcel.getEstate().getOwnerId());
        PacketDistributor.sendToPlayer(player, new SyncParcelPacket(parcel, ownerName));
    }

    public static void syncParcelToPlayer(ServerLevel level, ServerPlayer player,
                                          Parcel parcel, int borderStoneY) {
        String ownerName = resolveOwnerName(level, parcel.getEstate().getOwnerId());
        PacketDistributor.sendToPlayer(player, new SyncParcelPacket(parcel, ownerName, borderStoneY));
    }

    public static void syncParcelToTrackingPlayers(ServerLevel level, Parcel parcel) {
        syncParcelToTrackingPlayers(level, parcel,
                resolveOwnerName(level, parcel.getEstate().getOwnerId()));
    }

    public static void syncParcelToTrackingPlayers(ServerLevel level, Parcel parcel,
                                                   String ownerName) {
        SyncParcelPacket packet = new SyncParcelPacket(parcel, ownerName);
        PacketDistributor.sendToPlayersTrackingChunk(
                level,
                level.getChunkAt(new BlockPos(
                        parcel.getMinCoords().getX(),
                        parcel.getMinCoords().getY(),
                        parcel.getMinCoords().getZ())).getPos(),
                packet);
    }

    public static void syncParcelToTrackingPlayers(ServerLevel level, Parcel parcel,
                                                   int borderStoneY) {
        syncParcelToTrackingPlayers(level, parcel,
                resolveOwnerName(level, parcel.getEstate().getOwnerId()), borderStoneY);
    }

    public static void syncParcelToTrackingPlayers(ServerLevel level, Parcel parcel,
                                                   String ownerName, int borderStoneY) {
        SyncParcelPacket packet = new SyncParcelPacket(parcel, ownerName, borderStoneY);
        PacketDistributor.sendToPlayersTrackingChunk(
                level,
                level.getChunkAt(new BlockPos(
                        parcel.getMinCoords().getX(),
                        parcel.getMinCoords().getY(),
                        parcel.getMinCoords().getZ())).getPos(),
                packet);
    }

    // -------------------------------------------------------------------------
    // Border visibility sync
    // -------------------------------------------------------------------------

    public static void syncBorderVisibilityToTrackingPlayers(ServerLevel level, Parcel parcel,
                                                             int conflictState, int borderStoneY) {
        String ownerName = resolveOwnerName(level, parcel.getEstate().getOwnerId());
        PacketDistributor.sendToPlayersTrackingChunk(
                level,
                level.getChunkAt(new BlockPos(
                        parcel.getMinCoords().getX(),
                        parcel.getMinCoords().getY(),
                        parcel.getMinCoords().getZ())).getPos(),
                SyncParcelPacket.forBorderVisible(parcel, ownerName, borderStoneY, conflictState, null));
    }

    public static void syncBorderVisibilityToPlayer(ServerPlayer player, UUID parcelId,
                                                    int conflictState, int borderStoneY) {
        ParcelRegistry.findByParcelId(parcelId).ifPresent(parcel -> {
            String ownerName = resolveOwnerName(player.serverLevel(), parcel.getEstate().getOwnerId());
            PacketDistributor.sendToPlayer(player,
                    SyncParcelPacket.forBorderVisible(parcel, ownerName, borderStoneY, conflictState, null));
        });
    }

    public static void syncBorderVisibilityToTrackingPlayersAndSelf(ServerLevel level,
                                                                    ServerPlayer player,
                                                                    Parcel parcel,
                                                                    int conflictState,
                                                                    int borderStoneY) {
        syncBorderVisibilityToTrackingPlayers(level, parcel, conflictState, borderStoneY);
        String ownerName = resolveOwnerName(level, parcel.getEstate().getOwnerId());
        PacketDistributor.sendToPlayer(player,
                SyncParcelPacket.forBorderVisible(parcel, ownerName, borderStoneY, conflictState, player.getUUID()));
    }

    public static void syncBorderVisibleToOwner(ServerLevel level, Parcel parcel,
                                                int borderStoneY) {
        UUID ownerId = parcel.getEstate().getOwnerId();
        if (ownerId == null) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) return;

        String dimension = parcel.getDimension() != null
                ? parcel.getDimension()
                : level.dimension().location().toString();
        int conflictState = ParcelRegistry.resolveConflictState(
                parcel.getBox(), ownerId, parcel.getId(), parcel.getType(), dimension);
        String ownerName = resolveOwnerName(level, ownerId);
        PacketDistributor.sendToPlayer(owner,
                SyncParcelPacket.forBorderVisible(parcel, ownerName, borderStoneY, conflictState, null));
    }

    public static void syncBorderVisibleToOwner(ServerLevel level, Parcel parcel,
                                                int borderStoneY, int conflictState) {
        UUID ownerId = parcel.getEstate().getOwnerId();
        if (ownerId == null) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) return;

        String ownerName = resolveOwnerName(level, ownerId);
        PacketDistributor.sendToPlayer(owner,
                SyncParcelPacket.forBorderVisible(parcel, ownerName, borderStoneY, conflictState, null));
    }

    public static void syncBorderVisibleToOwnerAndPlacer(ServerLevel level, Parcel parcel,
                                                         int borderStoneY, UUID placingPlayerId) {
        UUID ownerId = parcel.getEstate().getOwnerId();
        if (ownerId == null) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) return;

        String dimension = parcel.getDimension() != null
                ? parcel.getDimension()
                : level.dimension().location().toString();
        int conflictState = ParcelRegistry.resolveConflictState(
                parcel.getBox(), ownerId, parcel.getId(), parcel.getType(), dimension);
        String ownerName = resolveOwnerName(level, ownerId);
        SyncParcelPacket packet = SyncParcelPacket.forBorderVisible(
                parcel, ownerName, borderStoneY, conflictState, placingPlayerId);

        PacketDistributor.sendToPlayer(owner, packet);
        ServerPlayer placingPlayer = level.getServer().getPlayerList().getPlayer(placingPlayerId);
        if (placingPlayer == null) return;
        PacketDistributor.sendToPlayer(placingPlayer, packet);
    }

    public static void syncBorderVisibleToOwnerAndPlacer(ServerLevel level, Parcel parcel,
                                                         int borderStoneY, int conflictState,
                                                         UUID placingPlayerId) {
        UUID ownerId = parcel.getEstate().getOwnerId();
        if (ownerId == null) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) return;

        String ownerName = resolveOwnerName(level, ownerId);
        SyncParcelPacket packet = SyncParcelPacket.forBorderVisible(
                parcel, ownerName, borderStoneY, conflictState, placingPlayerId);

        PacketDistributor.sendToPlayer(owner, packet);
        ServerPlayer placingPlayer = level.getServer().getPlayerList().getPlayer(placingPlayerId);
        if (placingPlayer == null) return;
        PacketDistributor.sendToPlayer(placingPlayer, packet);
    }

    public static void syncBorderHiddenToDimension(ServerLevel level, Parcel parcel) {
        String ownerName = resolveOwnerName(level, parcel.getEstate().getOwnerId());
        PacketDistributor.sendToPlayersInDimension(level,
                SyncParcelPacket.forBorderHidden(parcel, ownerName));
    }

    // -------------------------------------------------------------------------
    // Preview parcel sync
    // -------------------------------------------------------------------------

    public static void syncPreviewParcelToTrackingPlayersAndSelf(ServerLevel level,
                                                                 ServerPlayer placingPlayer,
                                                                 UUID parcelId, UUID estateId,
                                                                 UUID ownerId, ParcelType parcelType,
                                                                 Box box, int stoneY,
                                                                 String dimension, int conflictState) {
        String ownerName = resolveOwnerName(level, ownerId);
        SyncParcelPacket packet = SyncParcelPacket.forPreview(
                parcelId, estateId, ownerId, ownerName, parcelType, box, stoneY, dimension, conflictState, null);
        PacketDistributor.sendToPlayersTrackingChunk(
                level,
                level.getChunkAt(new BlockPos(
                        box.getMinCoords().getX(),
                        box.getMinCoords().getY(),
                        box.getMinCoords().getZ())).getPos(),
                packet);
        PacketDistributor.sendToPlayer(placingPlayer, packet);
    }

    public static void syncPreviewParcelToOwner(ServerLevel level,
                                                UUID ownerId,
                                                UUID parcelId, UUID estateId,
                                                ParcelType parcelType,
                                                Box box, int stoneY,
                                                String dimension, int conflictState) {
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) return;
        String ownerName = resolveOwnerName(level, ownerId);
        SyncParcelPacket packet = SyncParcelPacket.forPreview(
                parcelId, estateId, ownerId, ownerName, parcelType, box, stoneY, dimension, conflictState, null);
        PacketDistributor.sendToPlayer(owner, packet);
    }

    // -------------------------------------------------------------------------
    // Remove parcel
    // -------------------------------------------------------------------------

    public static void removeParcelFromTracking(ServerLevel level, Parcel parcel) {
        PacketDistributor.sendToPlayersTrackingChunk(
                level,
                level.getChunkAt(new BlockPos(
                        parcel.getMinCoords().getX(),
                        parcel.getMinCoords().getY(),
                        parcel.getMinCoords().getZ())).getPos(),
                new RemoveParcelPacket(parcel.getId()));
    }

    public static void removeParcelFromTracking(ServerLevel level, UUID parcelId, BlockPos pos) {
        PacketDistributor.sendToPlayersTrackingChunk(
                level,
                level.getChunkAt(pos).getPos(),
                new RemoveParcelPacket(parcelId));
    }

    public static void removePreviewParcelFromTracking(ServerLevel level, UUID parcelId, BlockPos pos) {
        PacketDistributor.sendToPlayersInDimension(level, new RemoveParcelPacket(parcelId));
    }

    // -------------------------------------------------------------------------
    // Full sync
    // -------------------------------------------------------------------------

    public static void syncAllParcelsToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, ServerConfigSyncPacket.fromServerConfig());

        Map<UUID, BorderStoneBlockEntity> visibleStones = new HashMap<>();
        Iterator<BorderStoneBlockEntity> iter = ActiveBorderStoneRegistry.getAll().iterator();
        while (iter.hasNext()) {
            BorderStoneBlockEntity stone = iter.next();
            if (stone.getParcelId() == null || stone.getLevel() == null) {
                ActiveBorderStoneRegistry.remove(stone);
                continue;
            }
            BlockEntity worldBE = stone.getLevel().getBlockEntity(stone.getBlockPos());
            if (worldBE != stone) {
                ActiveBorderStoneRegistry.remove(stone);
                continue;
            }
            ParcelRegistry.findByParcelId(stone.getParcelId()).ifPresent(parcel -> {
                UUID placingPlayerId = resolvePlacingPlayerId(stone);
                boolean isOwner = player.getUUID().equals(parcel.getEstate().getOwnerId());
                boolean isPlacer = placingPlayerId != null && player.getUUID().equals(placingPlayerId);
                if (isOwner || isPlacer) {
                    visibleStones.put(parcel.getId(), stone);
                }
            });
        }

        List<SyncParcelPacket> packets = ParcelRegistry.getParcels().stream()
                .map(parcel -> {
                    String ownerName = resolveOwnerName(player.serverLevel(), parcel.getEstate().getOwnerId());
                    String parcelDimension = parcel.getDimension() != null
                            ? parcel.getDimension()
                            : player.serverLevel().dimension().location().toString();

                    BorderStoneBlockEntity stone = visibleStones.get(parcel.getId());
                    if (stone != null) {
                        UUID placingPlayerId = resolvePlacingPlayerId(stone);
                        int conflictState = ParcelRegistry.resolveConflictState(
                                parcel.getBox(), parcel.getEstate().getOwnerId(),
                                parcel.getId(), parcel.getType(), parcelDimension);
                        return SyncParcelPacket.forBorderVisible(
                                parcel, ownerName,
                                stone.getBlockPos().getY(), conflictState, placingPlayerId);
                    } else {
                        int conflictState = ParcelRegistry.resolveConflictState(
                                parcel.getBox(),
                                parcel.getEstate().getOwnerId(),
                                parcel.getId(),
                                parcel.getType(),
                                parcelDimension);
                        return new SyncParcelPacket(parcel, ownerName, 0, conflictState);
                    }
                })
                .toList();

        PacketDistributor.sendToPlayer(player, new SyncAllParcelsPacket(packets));
    }

    public static void periodicResync(ServerLevel level) {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return;
        for (ServerPlayer player : players) {
            syncAllParcelsToPlayer(player);
        }
    }

    // -------------------------------------------------------------------------
    // Celebration
    // -------------------------------------------------------------------------

    public static void sendClaimCelebration(ServerPlayer player, Parcel parcel, BlockPos stonePos) {
        PacketDistributor.sendToPlayer(player, new ClaimCelebrationPacket(
                parcel.getMinCoords().getX(),
                parcel.getMinCoords().getZ(),
                parcel.getMaxCoords().getX(),
                parcel.getMaxCoords().getZ(),
                stonePos.getX(),
                stonePos.getY(),
                stonePos.getZ()));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String resolveOwnerName(ServerLevel level, UUID ownerId) {
        if (ownerId == null) return "";
        return PlayerRegistry.getPlayerName(level, ownerId).orElse("");
    }

    public static UUID resolvePlacingPlayerId(BorderStoneBlockEntity stone) {
        if (stone instanceof FoundationStoneBlockEntity foundationStone) {
            return foundationStone.getPlacingPlayerId();
        }
        return stone.getPlacingPlayer() != null ? stone.getPlacingPlayer().getUUID() : null;
    }
}