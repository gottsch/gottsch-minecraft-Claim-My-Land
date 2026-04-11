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

package mod.gottsch.neo.claimmyland.core.network;

import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.client.packet.handler.ClientSyncParcelHandler;
import mod.gottsch.neo.claimmyland.client.renderer.ParcelBorderRenderer;
import mod.gottsch.neo.claimmyland.core.integration.journeymap.ParcelPolygonOverlayFactory;
import mod.gottsch.neo.claimmyland.core.parcel.ClientParcel;
import mod.gottsch.neo.claimmyland.core.parcel.NationalizedParcel;
import mod.gottsch.neo.claimmyland.core.parcel.Parcel;
import mod.gottsch.neo.claimmyland.core.parcel.ParcelType;
import mod.gottsch.neo.claimmyland.core.registry.ClientParcelRegistry;
import mod.gottsch.neo.claimmyland.core.util.DimensionHelper;
import mod.gottsch.neo.gottschcore.spatial.Box;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.UUID;

/**
 * Server → Client packet. Adds or updates a single parcel in the client's
 * {@link ClientParcelRegistry}. Sent when a parcel is claimed or modified.
 *
 * <p>Uses the same wire format as {@link CacheSyncPacket} for consistency,
 * but targets the full registry rather than the single-entry position cache.</p>
 *
 * @author Mark Gottschling on 3/3/2026
 */
public class SyncParcelPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncParcelPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ClaimMyLand.MOD_ID, "sync_parcel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncParcelPacket> STREAM_CODEC =
            StreamCodec.of(SyncParcelPacket::encode, SyncParcelPacket::decode);

    final UUID parcelId;
    final UUID estateId;
    final String parcelName;
    final String estateName;
    final String nationName;
    final String ownerName;
    final UUID ownerId;
    final ParcelType parcelType;
    final boolean relinquished;
    final int minX, minY, minZ;
    final int maxX, maxY, maxZ;
    final String dimension;
    final boolean isBorderVisible;
    final int conflictState;
    final int borderStoneY;
    final boolean isPreview;
    final UUID placingPlayerId;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public SyncParcelPacket(Parcel parcel, String resolvedOwnerName) {
        this(parcel, resolvedOwnerName, 0);
    }

    public SyncParcelPacket(Parcel parcel, String resolvedOwnerName, int borderStoneY) {
        this(parcel, resolvedOwnerName, borderStoneY, 0);
    }

    public SyncParcelPacket(Parcel parcel, String resolvedOwnerName, int borderStoneY, int conflictState) {
        this.parcelId        = parcel.getId();
        this.estateId        = parcel.getEstate().getId();
        this.parcelName      = parcel.getName() != null ? parcel.getName() : "";
        this.estateName      = parcel.getEstate().getName() != null ? parcel.getEstate().getName() : "";
        this.nationName      = (parcel instanceof NationalizedParcel np) ? np.getNationEstate().getName() : null;
        this.ownerName       = resolvedOwnerName != null ? resolvedOwnerName : "";
        this.ownerId         = parcel.getEstate().getOwnerId();
        this.parcelType      = parcel.getType() != null ? parcel.getType() : ParcelType.NONE;
        this.relinquished    = parcel.getEstate().isRelinquished();
        this.minX = parcel.getMinCoords().getX();
        this.minY = parcel.getMinCoords().getY();
        this.minZ = parcel.getMinCoords().getZ();
        this.maxX = parcel.getMaxCoords().getX();
        this.maxY = parcel.getMaxCoords().getY();
        this.maxZ = parcel.getMaxCoords().getZ();
        this.dimension       = parcel.getDimension();
        this.borderStoneY    = borderStoneY;
        this.isPreview       = false;
        this.isBorderVisible = false;
        this.conflictState   = conflictState;
        this.placingPlayerId = null;
    }

    SyncParcelPacket(
            UUID parcelId, UUID estateId,
            String parcelName, String estateName, String nationName,
            String ownerName,
            UUID ownerId, ParcelType parcelType, boolean relinquished,
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ,
            String dimension,
            boolean isBorderVisible,
            int conflictState,
            int borderStoneY,
            boolean isPreview,
            UUID placingPlayerId) {
        this.parcelId        = parcelId;
        this.estateId        = estateId;
        this.parcelName      = parcelName;
        this.estateName      = estateName;
        this.nationName      = nationName;
        this.ownerName       = ownerName;
        this.ownerId         = ownerId;
        this.parcelType      = parcelType;
        this.relinquished    = relinquished;
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
        this.dimension       = dimension;
        this.isBorderVisible = isBorderVisible;
        this.conflictState   = conflictState;
        this.borderStoneY    = borderStoneY;
        this.isPreview       = isPreview;
        this.placingPlayerId = placingPlayerId;
    }

    // -------------------------------------------------------------------------
    // encode / decode
    // -------------------------------------------------------------------------

    static void encode(RegistryFriendlyByteBuf buf, SyncParcelPacket packet) {
        buf.writeUUID(packet.parcelId);
        buf.writeUUID(packet.estateId);
        buf.writeUtf(packet.parcelName);
        buf.writeUtf(packet.estateName);
        buf.writeUtf(packet.nationName != null ? packet.nationName : "");
        buf.writeUtf(packet.ownerName);
        buf.writeUUID(packet.ownerId);
        buf.writeUtf(packet.parcelType.getSerializedName());
        buf.writeBoolean(packet.relinquished);
        buf.writeInt(packet.minX); buf.writeInt(packet.minY); buf.writeInt(packet.minZ);
        buf.writeInt(packet.maxX); buf.writeInt(packet.maxY); buf.writeInt(packet.maxZ);
        buf.writeUtf(packet.dimension);
        buf.writeBoolean(packet.isBorderVisible);
        buf.writeInt(packet.conflictState);
        buf.writeInt(packet.borderStoneY);
        buf.writeBoolean(packet.isPreview);
        if (packet.placingPlayerId == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeUUID(packet.placingPlayerId);
        }
    }

    static SyncParcelPacket decode(RegistryFriendlyByteBuf buf) {
        UUID parcelId        = buf.readUUID();
        UUID estateId        = buf.readUUID();
        String parcelName    = buf.readUtf();
        String estateName    = buf.readUtf();
        String nationName    = buf.readUtf();
        if (nationName.isBlank()) nationName = null;
        String ownerName     = buf.readUtf();
        UUID ownerId         = buf.readUUID();
        ParcelType type      = ParcelType.fromString(buf.readUtf());
        boolean relinquished = buf.readBoolean();
        int minX = buf.readInt(), minY = buf.readInt(), minZ = buf.readInt();
        int maxX = buf.readInt(), maxY = buf.readInt(), maxZ = buf.readInt();
        String dimension        = buf.readUtf();
        boolean isBorderVisible = buf.readBoolean();
        int conflictState       = buf.readInt();
        int borderStoneY        = buf.readInt();
        boolean isPreview       = buf.readBoolean();
        boolean hasPlacingPlayer = buf.readBoolean();
        UUID placingPlayerId    = hasPlacingPlayer ? buf.readUUID() : null;

        return new SyncParcelPacket(
                parcelId, estateId,
                parcelName, estateName, nationName,
                ownerName, ownerId, type, relinquished,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                dimension,
                isBorderVisible,
                conflictState,
                borderStoneY,
                isPreview,
                placingPlayerId
        );
    }

    // -------------------------------------------------------------------------
    // Handle
    // -------------------------------------------------------------------------

    public static void handle(SyncParcelPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientParcel existing = ClientParcelRegistry.findById(packet.parcelId).orElse(null);
            boolean isNewPreview    = existing == null && packet.isPreview;
            boolean wasPreview      = existing != null && existing.isPreview();
            boolean isNowCommitted  = !packet.isPreview;

            ClientParcel clientParcel = new ClientParcel(
                    packet.parcelId, packet.estateId,
                    packet.parcelName, packet.estateName,
                    packet.nationName,
                    packet.ownerName, packet.ownerId,
                    packet.parcelType, packet.relinquished,
                    packet.minX, packet.minY, packet.minZ,
                    packet.maxX, packet.maxY, packet.maxZ,
                    packet.dimension,
                    packet.isBorderVisible, packet.conflictState, packet.borderStoneY,
                    packet.isPreview, packet.placingPlayerId
            );

            ClientParcelRegistry.register(clientParcel);

            if (wasPreview && isNowCommitted) {
                if (FMLEnvironment.dist == Dist.CLIENT) {
                    ParcelBorderRenderer.clearConflictHighlights();
                    if (ModList.get().isLoaded("journeymap")) {
                        ParcelPolygonOverlayFactory.clearConflictOverlays();
                    }
                }
            }

            if (ModList.get().isLoaded("journeymap")) {
                ParcelPolygonOverlayFactory.notifyParcelAdded(clientParcel);
            }

            if (packet.isPreview) {
                if (isNewPreview && FMLEnvironment.dist == Dist.CLIENT) {
                    ClientSyncParcelHandler.emitFoundationStoneParticles(
                            packet.borderStoneY,
                            (packet.minX + packet.maxX) / 2,
                            (packet.minZ + packet.maxZ) / 2);
                }

                List<ClientParcel> conflicting = ClientParcelRegistry.findConflicting(clientParcel);
                ParcelBorderRenderer.setConflictHighlights(
                        conflicting,
                        new BlockPos(
                                (packet.minX + packet.maxX) / 2,
                                packet.borderStoneY,
                                (packet.minZ + packet.maxZ) / 2));

                if (ModList.get().isLoaded("journeymap")) {
                    ResourceKey<Level> dimKey = DimensionHelper.dimensionKey(packet.dimension);
                    if (dimKey != null) {
                        ParcelPolygonOverlayFactory.showConflictOverlays(conflicting, dimKey);
                    }
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    public ClientParcel toClientParcel() {
        return new ClientParcel(
                parcelId, estateId,
                parcelName, estateName,
                nationName, ownerName,
                ownerId, parcelType, relinquished,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                dimension,
                isBorderVisible,
                conflictState, borderStoneY, isPreview,
                placingPlayerId
        );
    }

    public static SyncParcelPacket forPreview(UUID parcelId, UUID estateId, UUID ownerId, String ownerName,
                                              ParcelType parcelType, Box box,
                                              int stoneY, String dimension, int conflictState, UUID placingPlayerId) {
        return new SyncParcelPacket(
                parcelId, estateId,
                "", "", "",
                ownerName != null ? ownerName : "",
                ownerId,
                parcelType != null ? parcelType : ParcelType.PLAYER,
                false,
                box.getMinCoords().getX(), box.getMinCoords().getY(), box.getMinCoords().getZ(),
                box.getMaxCoords().getX(), box.getMaxCoords().getY(), box.getMaxCoords().getZ(),
                dimension,
                true,
                conflictState,
                stoneY,
                true,
                placingPlayerId
        );
    }

    public static SyncParcelPacket forBorderVisible(
            Parcel parcel, String ownerName,
            int borderStoneY, int conflictState, UUID placingPlayerId) {
        return new SyncParcelPacket(
                parcel.getId(), parcel.getEstate().getId(),
                parcel.getName() != null ? parcel.getName() : "",
                parcel.getEstate().getName() != null ? parcel.getEstate().getName() : "",
                (parcel instanceof NationalizedParcel np) ? np.getNationEstate().getName() : null,
                ownerName != null ? ownerName : "",
                parcel.getEstate().getOwnerId(),
                parcel.getType() != null ? parcel.getType() : ParcelType.NONE,
                parcel.getEstate().isRelinquished(),
                parcel.getMinCoords().getX(), parcel.getMinCoords().getY(), parcel.getMinCoords().getZ(),
                parcel.getMaxCoords().getX(), parcel.getMaxCoords().getY(), parcel.getMaxCoords().getZ(),
                parcel.getDimension(),
                true,
                conflictState,
                borderStoneY,
                false,
                placingPlayerId
        );
    }

    public static SyncParcelPacket forBorderHidden(Parcel parcel, String ownerName) {
        return new SyncParcelPacket(
                parcel.getId(), parcel.getEstate().getId(),
                parcel.getName() != null ? parcel.getName() : "",
                parcel.getEstate().getName() != null ? parcel.getEstate().getName() : "",
                (parcel instanceof NationalizedParcel np) ? np.getNationEstate().getName() : null,
                ownerName != null ? ownerName : "",
                parcel.getEstate().getOwnerId(),
                parcel.getType() != null ? parcel.getType() : ParcelType.NONE,
                parcel.getEstate().isRelinquished(),
                parcel.getMinCoords().getX(), parcel.getMinCoords().getY(), parcel.getMinCoords().getZ(),
                parcel.getMaxCoords().getX(), parcel.getMaxCoords().getY(), parcel.getMaxCoords().getZ(),
                parcel.getDimension(),
                false,
                0,
                0,
                false,
                null
        );
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}