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
package mod.gottsch.forge.claimmyland.core.network;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.client.packet.handler.ClientCelebrationHandler;
import mod.gottsch.forge.claimmyland.core.block.entity.BorderStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.ActiveBorderStoneRegistry;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.registry.PlayerRegistry;
import mod.gottsch.forge.gottschcore.spatial.Box;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.*;

/**
 * @author Mark Gottschling on March 3, 2026
 */
public class CMLNetwork {

    private static final String PROTOCOL_VERSION = "2";

    public static SimpleChannel CHANNEL;

    /**
     * Registers all server-to-client packets.
     * BorderVisibilityPacket has been removed — border visibility is now carried
     * by SyncParcelPacket via the forBorderVisible() factory.
     */
    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(ClaimMyLand.MOD_ID, "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );

        int id = 0;

        CHANNEL.registerMessage(
                id++,
                CacheSyncPacket.class,
                CacheSyncPacket::encode,
                CacheSyncPacket::decode,
                CacheSyncPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                id++,
                SyncParcelPacket.class,
                SyncParcelPacket::encode,
                SyncParcelPacket::decode,
                SyncParcelPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                id++,
                SyncAllParcelsPacket.class,
                SyncAllParcelsPacket::encode,
                SyncAllParcelsPacket::decode,
                SyncAllParcelsPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                id++,
                RemoveParcelPacket.class,
                RemoveParcelPacket::encode,
                RemoveParcelPacket::decode,
                RemoveParcelPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                id++,
                ServerConfigSyncPacket.class,
                ServerConfigSyncPacket::encode,
                ServerConfigSyncPacket::decode,
                ServerConfigSyncPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                id++,
                ClaimCelebrationPacket.class,
                ClaimCelebrationPacket::encode,
                ClaimCelebrationPacket::decode,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() ->
                            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                                    ClientCelebrationHandler.handle(packet)
                            )
                    );
                    ctx.get().setPacketHandled(true);
                },
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        ClaimMyLand.LOGGER.debug("CMLNetwork registered {} packet(s)", id);
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
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new CacheSyncPacket(parcel, ownerName)
        );
    }

    public static void syncWildernessToPlayer(ServerPlayer player) {
        syncCacheToPlayer(player, null);
    }

    // =========================================================================
    // Parcel sync — sends full parcel data (isBorderVisible=false)
    // =========================================================================

    public static void syncParcelToPlayer(ServerLevel level, ServerPlayer player, Parcel parcel) {
        String ownerName = resolveOwnerName(level, parcel.getEstate().getOwnerId());
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncParcelPacket(parcel, ownerName));
    }

    public static void syncParcelToPlayer(ServerLevel level, ServerPlayer player,
                                          Parcel parcel, int borderStoneY) {
        String ownerName = resolveOwnerName(level, parcel.getEstate().getOwnerId());
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncParcelPacket(parcel, ownerName, borderStoneY));
    }

    public static void syncParcelToTrackingPlayers(ServerLevel level, Parcel parcel) {
        syncParcelToTrackingPlayers(level, parcel,
                resolveOwnerName(level, parcel.getEstate().getOwnerId()));
    }

    public static void syncParcelToTrackingPlayers(ServerLevel level, Parcel parcel,
                                                   String ownerName) {
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

    public static void syncParcelToTrackingPlayers(ServerLevel level, Parcel parcel,
                                                   int borderStoneY) {
        syncParcelToTrackingPlayers(level, parcel,
                resolveOwnerName(level, parcel.getEstate().getOwnerId()), borderStoneY);
    }

    public static void syncParcelToTrackingPlayers(ServerLevel level, Parcel parcel,
                                                   String ownerName, int borderStoneY) {
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

    // =========================================================================
    // Border visibility sync — sends SyncParcelPacket with isBorderVisible=true.
    // Border visibility is always true when these are called; to hide a border
    // the parcel is removed via RemoveParcelPacket.
    // =========================================================================

    /**
     * Sends border visibility to all players tracking the chunk that contains
     * the parcel. Used when a border stone is placed or a neighbour changes.
     */
    public static void syncBorderVisibilityToTrackingPlayers(ServerLevel level, Parcel parcel,
                                                             int conflictState, int borderStoneY) {
        String ownerName = resolveOwnerName(level, parcel.getEstate().getOwnerId());
        CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() ->
                        level.getChunkAt(new BlockPos(
                                parcel.getMinCoords().getX(),
                                parcel.getMinCoords().getY(),
                                parcel.getMinCoords().getZ()))),
                SyncParcelPacket.forBorderVisible(parcel, ownerName, borderStoneY, conflictState, null)
        );
    }

    /**
     * Sends border visibility to a specific player. Looks up the parcel by id;
     * if not found (stale id), the send is silently skipped.
     */
    public static void syncBorderVisibilityToPlayer(ServerPlayer player, UUID parcelId,
                                                    int conflictState, int borderStoneY) {
        ClaimMyLand.LOGGER.debug("syncBorderVisibleToPlayer: parcelId={} conflictState={}",
                parcelId, conflictState);
        ParcelRegistry.findByParcelId(parcelId).ifPresent(parcel -> {
            String ownerName = resolveOwnerName(player.serverLevel(), parcel.getEstate().getOwnerId());
            CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    SyncParcelPacket.forBorderVisible(parcel, ownerName, borderStoneY, conflictState, null)
            );
        });
    }

    /**
     * Sends border visibility to all tracking players and also directly to the
     * placing player (who may not be in the tracking set).
     */
    public static void syncBorderVisibilityToTrackingPlayersAndSelf(ServerLevel level,
                                                                    ServerPlayer player,
                                                                    Parcel parcel,
                                                                    int conflictState,
                                                                    int borderStoneY) {
        syncBorderVisibilityToTrackingPlayers(level, parcel, conflictState, borderStoneY);

        String ownerName = resolveOwnerName(level, parcel.getEstate().getOwnerId());
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                SyncParcelPacket.forBorderVisible(parcel, ownerName, borderStoneY, conflictState, player.getUUID())
        );
    }

    /**
     * Sends border visibility to all players in the dimension.
     * Used for dimension-wide border updates.
     */
//    public static void syncBorderVisibilityToDimension(ServerLevel level, Parcel parcel,
//                                                       int conflictState, int borderStoneY) {
//        String ownerName = resolveOwnerName(level, parcel.getEstate().getOwnerId());
//        CHANNEL.send(
//                PacketDistributor.DIMENSION.with(level::dimension),
//                SyncParcelPacket.forBorderVisible(parcel, ownerName, borderStoneY, conflictState, null)
//        );
//    }

    /**
     * Sends border visibility to the parcel owner only.
     * Overload 1: computes conflictState from the parcel's own dimension.
     */
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

        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> owner),
                SyncParcelPacket.forBorderVisible(parcel, ownerName, borderStoneY, conflictState, null)
        );
    }

    /**
     * Sends border visibility to the parcel owner only.
     * Overload 2: conflictState already computed by the caller.
     */
    public static void syncBorderVisibleToOwner(ServerLevel level, Parcel parcel,
                                                int borderStoneY, int conflictState) {
        ClaimMyLand.LOGGER.debug("syncBorderVisibleToOwner: parcelId={} conflictState={}",
                parcel.getId(), conflictState);
        UUID ownerId = parcel.getEstate().getOwnerId();
        if (ownerId == null) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) return;

        String ownerName = resolveOwnerName(level, ownerId);
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> owner),
                SyncParcelPacket.forBorderVisible(parcel, ownerName, borderStoneY, conflictState, null)
        );
    }

    /**
     * Sends border visibility to the parcel owner and to the placing player
     * (who may be a different player, e.g. an ops player placing on behalf of another).
     * Overload 1: computes conflictState from the parcel's own dimension.
     */
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

        CHANNEL.send(PacketDistributor.PLAYER.with(() -> owner), packet);

        ServerPlayer placingPlayer = level.getServer().getPlayerList().getPlayer(placingPlayerId);
        if (placingPlayer == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> placingPlayer), packet);
    }

    /**
     * Sends border visibility to the parcel owner and to the placing player.
     * Overload 2: conflictState already computed by the caller.
     */
    public static void syncBorderVisibleToOwnerAndPlacer(ServerLevel level, Parcel parcel,
                                                         int borderStoneY, int conflictState,
                                                         UUID placingPlayerId) {

        ClaimMyLand.LOGGER.debug("syncBorderVisibleToOwnerAndPlacer: parcelId={} conflictState={}",
                parcel.getId(), conflictState);

        UUID ownerId = parcel.getEstate().getOwnerId();
        if (ownerId == null) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) return;

        String ownerName = resolveOwnerName(level, ownerId);
        SyncParcelPacket packet = SyncParcelPacket.forBorderVisible(
                parcel, ownerName, borderStoneY, conflictState, placingPlayerId);

        CHANNEL.send(PacketDistributor.PLAYER.with(() -> owner), packet);

        ServerPlayer placingPlayer = level.getServer().getPlayerList().getPlayer(placingPlayerId);
        if (placingPlayer == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> placingPlayer), packet);
    }

    // =========================================================================
    // Preview parcel sync
    // =========================================================================

    /**
     * Sends a preview parcel to all tracking players and directly to the placing
     * player. Used when a Foundation Stone is first placed.
     */
    public static void syncPreviewParcelToTrackingPlayersAndSelf(ServerLevel level,
                                                                 ServerPlayer placingPlayer,
                                                                 UUID parcelId, UUID estateId,
                                                                 UUID ownerId, ParcelType parcelType,
                                                                 Box box, int stoneY,
                                                                 String dimension, int conflictState) {
        String ownerName = resolveOwnerName(level, ownerId);
        SyncParcelPacket packet = SyncParcelPacket.forPreview(
                parcelId, estateId, ownerId, ownerName, parcelType, box, stoneY, dimension, conflictState, null);
        CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() ->
                        level.getChunkAt(new BlockPos(
                                box.getMinCoords().getX(),
                                box.getMinCoords().getY(),
                                box.getMinCoords().getZ()))),
                packet);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> placingPlayer), packet);
    }

    /**
     * Sends a preview parcel to the owner only. Used on chunk re-load when the
     * Foundation Stone's parcel has not yet been committed.
     */
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
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> owner), packet);
    }

    // =========================================================================
    // Remove parcel
    // =========================================================================

    public static void removeParcelFromTracking(ServerLevel level, Parcel parcel) {
        CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() ->
                        level.getChunkAt(new BlockPos(
                                parcel.getMinCoords().getX(),
                                parcel.getMinCoords().getY(),
                                parcel.getMinCoords().getZ()))),
                new RemoveParcelPacket(parcel.getId())
        );
    }

    public static void removeParcelFromTracking(ServerLevel level, UUID parcelId, BlockPos pos) {
        CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)),
                new RemoveParcelPacket(parcelId)
        );
    }

    public static void removePreviewParcelFromTracking(ServerLevel level, UUID parcelId, BlockPos pos) {
        CHANNEL.send(
                PacketDistributor.DIMENSION.with(() -> level.dimension()),
                new RemoveParcelPacket(parcelId));
    }

    // =========================================================================
    // Login sync — sends all parcels and border states to a joining player
    // =========================================================================

    /**
     * Full sync for a player logging in or relogging.
     *
     * Step 1 — Config: send server config so client buffer radii are correct
     *           before any conflict state is evaluated client-side.
     *
     * Step 2 — Parcels: send all parcels with fresh server-computed conflictState
     *           and isBorderVisible=false. Border visibility is handled in step 3.
     *
     * Step 3 — Borders: for each active border stone the player owns or placed,
     *           send a SyncParcelPacket with isBorderVisible=true and fresh
     *           conflictState derived from the stone's absolute box.
     */
    public static void syncAllParcelsToPlayer(ServerPlayer player) {
        // Step 1: sync server config
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                ServerConfigSyncPacket.fromServerConfig()
        );

        // Pre-compute which parcels have visible borders for this player
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
                boolean isOwner  = player.getUUID().equals(parcel.getEstate().getOwnerId());
                boolean isPlacer = placingPlayerId != null && player.getUUID().equals(placingPlayerId);
                if (isOwner || isPlacer) {
                    visibleStones.put(parcel.getId(), stone);
                }
            });
        }

        // Step 2: sync all parcels with border visibility baked in
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

        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncAllParcelsPacket(packets));

        ClaimMyLand.LOGGER.debug("CMLNetwork: synced {} parcel(s) to player {} ({} with visible borders)",
                packets.size(), player.getScoreboardName(), visibleStones.size());
    }

    // =========================================================================
    // Claim celebration
    // =========================================================================

    public static void sendClaimCelebration(ServerPlayer player, Parcel parcel, BlockPos stonePos) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ClaimCelebrationPacket(
                        parcel.getMinCoords().getX(),
                        parcel.getMinCoords().getZ(),
                        parcel.getMaxCoords().getX(),
                        parcel.getMaxCoords().getZ(),
                        stonePos.getX(),
                        stonePos.getY(),
                        stonePos.getZ())
        );
    }

    // =========================================================================
    // Periodic resync (server tick)
    // =========================================================================

    public static void periodicResync(ServerLevel level) {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return;
        for (ServerPlayer player : players) {
            syncAllParcelsToPlayer(player);
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

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

    /**
     * Hides the border for a committed parcel across the entire dimension.
     * Called when a Border Stone or Foundation Stone is removed.
     */
    public static void syncBorderHiddenToDimension(ServerLevel level, Parcel parcel) {
        ClaimMyLand.LOGGER.debug("syncBorderVisibleToDimension: parcelId={}",
                parcel.getId());
        String ownerName = resolveOwnerName(level, parcel.getEstate().getOwnerId());
        CHANNEL.send(
                PacketDistributor.DIMENSION.with(level::dimension),
                SyncParcelPacket.forBorderHidden(parcel, ownerName)
        );
    }
}