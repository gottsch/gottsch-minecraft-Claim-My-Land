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
                // instruct the client-side only processing of packet
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
                new BorderVisibilityPacket(parcel.getId(), visible, conflictState, borderStoneY, null)
        );
    }

    /**
     * sends all parcels to a player on login.
     * call from PlayerEvent.PlayerLoggedInEvent in ModEvents.
     */
    /**
     * @author Mark Gottschling on Mar 11, 2026
     */
    public static void syncAllParcelsToPlayer(ServerPlayer player) {
        // Sync server config values to client first so they are in place
        // before any parcel data is processed client-side
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                ServerConfigSyncPacket.fromServerConfig()
        );

        List<SyncParcelPacket> packets = ParcelRegistry.getParcels().stream()
                .map(parcel -> new SyncParcelPacket(parcel,
                        resolveOwnerName(player.serverLevel(), parcel.getEstate().getOwnerId())))
                .toList();

        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncAllParcelsPacket(packets));

        ClaimMyLand.LOGGER.debug("CMLNetwork: synced {} parcel(s) to player {}",
                packets.size(), player.getScoreboardName());

        // send BorderVisibilityPacket for each active stone — but only if the stone block
        // still physically exists in the world (guards against stale ACTIVE_BORDER_STONES entries)
        for (BorderStoneBlockEntity stone : ActiveBorderStoneRegistry.getAll()) {
            if (stone.getParcelId() == null || stone.getLevel() == null) {
                ActiveBorderStoneRegistry.remove(stone);
                continue;
            }
            BlockEntity worldBE = stone.getLevel().getBlockEntity(stone.getBlockPos());
            if (worldBE != stone) {
                ActiveBorderStoneRegistry.remove(stone);
                continue;
            }
            ClaimMyLand.LOGGER.debug("CMLNetwork: processing border stone...");
            ParcelRegistry.findByParcelId(stone.getParcelId()).ifPresent(parcel -> {

                // Only send to the owner of this parcel
//                if (!parcel.getEstate().isRelinquished() && !player.getUUID().equals(parcel.getEstate().getOwnerId())) return;
//                UUID playingPlayerId = resolvePlacingPlayerId(stone);
//                if (!player.getUUID().equals(playingPlayerId) && !player.getUUID().equals(parcel.getEstate().getOwnerId())) return;

                UUID placingPlayerId = resolvePlacingPlayerId(stone);
                boolean isOwner = player.getUUID().equals(parcel.getEstate().getOwnerId());
                boolean isPlacer = placingPlayerId != null && player.getUUID().equals(placingPlayerId);
                if (!isOwner && !isPlacer) return;

                int conflictState = ParcelRegistry.resolveConflictState(
                        stone.getAbsoluteBox(), parcel.getEstate().getOwnerId(), parcel.getId(), parcel.getType());

                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new BorderVisibilityPacket(parcel.getId(), true, conflictState, stone.getBlockPos().getY(), placingPlayerId));
            });
        }
    }

    /**
     * Sends a preview SyncParcelPacket to tracking players AND directly to the
     * placing player. Required because TRACKING_CHUNK excludes the sender's chunk.
     * @author Mark Gottschling on Mar 11, 2026
     */
    public static void syncPreviewParcelToTrackingPlayersAndSelf(ServerLevel level,
                                                                 ServerPlayer placingPlayer,
                                                                 UUID parcelId, UUID estateId,
                                                                 UUID ownerId, ParcelType parcelType,
                                                                 Box box, int stoneY,
                                                                 String dimension, int conflictState) {
        String ownerName = resolveOwnerName(level, ownerId);
        SyncParcelPacket packet = SyncParcelPacket.forPreview(
                parcelId, estateId, ownerId, ownerName, parcelType, box, stoneY, dimension, conflictState);
        CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() ->
                        level.getChunkAt(new BlockPos(
                                box.getMinCoords().getX(),
                                box.getMinCoords().getY(),
                                box.getMinCoords().getZ()))),
                packet);
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> placingPlayer),
                packet);
    }

    /**
     * Sends a BorderVisibilityPacket directly to a single player.
     * Used by chunk-load resync to update players returning to a previously loaded area.
     *
     * @author Mark Gottschling on March 18, 2026
     */
    public static void syncBorderVisibilityToPlayer(ServerPlayer player, UUID parcelId,
                                                    boolean visible, int conflictState,
                                                    int borderStoneY) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new BorderVisibilityPacket(parcelId, visible, conflictState, borderStoneY, null));
    }

    /**
     * sends a border visibility packet to all tracking players AND directly to
     * the specified player. use this overload when the acting player is known
     * (e.g. fresh border stone placement) since TRACKING_CHUNK excludes the
     * sender's own chunk.
     * @author Mark Gottschling on Mar 11, 2026
     */
    public static void syncBorderVisibilityToTrackingPlayersAndSelf(ServerLevel level,
                                                                    ServerPlayer player,
                                                                    Parcel parcel,
                                                                    boolean visible,
                                                                    int conflictState,
                                                                    int borderStoneY) {
        syncBorderVisibilityToTrackingPlayers(level, parcel, visible, conflictState, borderStoneY);
        ClaimMyLand.LOGGER.debug("plaery -> {}", player);
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new BorderVisibilityPacket(parcel.getId(), visible, conflictState, borderStoneY, player.getUUID())
        );
    }

    /**
     * Sends a border visibility packet to ALL players in the dimension.
     * Use for hide packets fired from onRemove() where no player reference is available.
     * @author Mark Gottschling on Mar 11, 2026
     */
    public static void syncBorderVisibilityToDimension(ServerLevel level, Parcel parcel,
                                                       boolean visible, int conflictState, int borderStoneY) {
        CHANNEL.send(
                PacketDistributor.DIMENSION.with(level::dimension),
                new BorderVisibilityPacket(parcel.getId(), visible, conflictState, borderStoneY, null));
    }

    /**
     * Sends a BorderVisibilityPacket(true) to the parcel owner if they are currently online.
     * Called from BorderStoneBlockEntity.onLoad() for the late-chunk-load case.
     * If the owner is not yet online, the syncAllParcelsToPlayer() drain on login covers it.
     *
     * @author Mark Gottschling on Mar 12, 2026
     */
    public static void syncBorderVisibleToOwner(ServerLevel level, Parcel parcel, int borderStoneY) {
        UUID ownerId = parcel.getEstate().getOwnerId();
        if (ownerId == null) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) return;
        int conflictState = ParcelRegistry.resolveConflictState(
                parcel.getBox(), ownerId, parcel.getId(), parcel.getType());

        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> owner),
                new BorderVisibilityPacket(parcel.getId(), true, conflictState, borderStoneY, null));
    }

    public static void syncBorderVisibleToOwnerAndPlacer(ServerLevel level, Parcel parcel, int borderStoneY, UUID placingPlayerId) {
        ClaimMyLand.LOGGER.debug("syncing border visible to placer");
        UUID ownerId = parcel.getEstate().getOwnerId();
        if (ownerId == null) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        ClaimMyLand.LOGGER.debug("owner -> {}", owner);
        if (owner == null) return;
        int conflictState = ParcelRegistry.resolveConflictState(
                parcel.getBox(), ownerId, parcel.getId(), parcel.getType());

        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> owner),
                new BorderVisibilityPacket(parcel.getId(), true, conflictState, borderStoneY, placingPlayerId));

        ServerPlayer placingPlayer = level.getServer().getPlayerList().getPlayer(placingPlayerId);
        ClaimMyLand.LOGGER.debug("placingPlayer -> {}", placingPlayer);
        if (placingPlayer == null) return;
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> placingPlayer),
                new BorderVisibilityPacket(parcel.getId(), true, conflictState, borderStoneY, placingPlayerId));
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
                parcelId, estateId, ownerId, ownerName, parcelType, box, stoneY, dimension, conflictState);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> owner), packet);
    }

    /**
     * Removes a preview parcel from all tracking clients and the placing player.
     * Used when a foundation stone is broken before the claim is committed.
     * @author Mark Gottschling on Mar 11, 2026
     */
    public static void removePreviewParcelFromTracking(ServerLevel level, UUID parcelId, BlockPos pos) {
        RemoveParcelPacket packet = new RemoveParcelPacket(parcelId);
        CHANNEL.send(
                PacketDistributor.DIMENSION.with(() -> level.dimension()), packet);
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

    public static UUID resolvePlacingPlayerId(BorderStoneBlockEntity stone) {
        if (stone instanceof FoundationStoneBlockEntity foundationStone) {
            return foundationStone.getPlacingPlayerId();
        }
        return stone.getPlacingPlayer() != null ? stone.getPlacingPlayer().getUUID() : null;
    }

    /**
     * Performs a full parcel state resync to all players currently in the given level.
     * Called periodically from ModEvents to ensure clients that were out of range
     * during state changes (renames, transfers, border visibility) are brought back
     * in sync.
     *
     * @author Mark Gottschling on March 18, 2026
     */
    public static void periodicResync(ServerLevel level) {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return;
        for (ServerPlayer player : players) {
            syncAllParcelsToPlayer(player);
        }
    }

    /**
     * Sends a {@link ClaimCelebrationPacket} to the claiming player only.
     * The celebration is intentionally private — it avoids revealing the claiming
     * player's position to others on PvP servers.
     *
     * @param player       the player who just committed the claim
     * @param parcel       the newly registered parcel
     * @param stonePos     BlockPos of the Foundation Stone that was used
     */
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

}