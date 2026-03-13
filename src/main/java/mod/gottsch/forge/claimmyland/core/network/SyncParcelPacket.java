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
import mod.gottsch.forge.claimmyland.core.integration.journeymap.ParcelPolygonOverlayFactory;
import mod.gottsch.forge.claimmyland.core.parcel.ClientParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.ClientParcelRegistry;
import mod.gottsch.forge.gottschcore.spatial.Box;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Server → Client packet. Adds or updates a single parcel in the client's
 * {@link ClientParcelRegistry}. Sent when a parcel is claimed or modified.
 *
 * <p>Uses the same wire format as {@link CacheSyncPacket} for consistency,
 * but targets the full registry rather than the single-entry position cache.</p>
 *
 * @author Mark Gottschling on 3/3/2026
 */
public class SyncParcelPacket {

    private final UUID parcelId;
    private final UUID estateId;
    private final String parcelName;
    private final String estateName;
    private final String ownerName;
    private final UUID ownerId;
    private final ParcelType parcelType;
    private final boolean relinquished;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    private final String dimension;
    private final boolean isBorderVisible;
    private final int conflictState;
    private final int borderStoneY;
    private final boolean isPreview;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public SyncParcelPacket(Parcel parcel, String resolvedOwnerName) {
        this(parcel, resolvedOwnerName, 0);
    }

    /**
     * server-side constructor — build from a live Parcel and pre-resolved owner name.
     * owner name should be resolved via PlayerRegistry.getPlayerName() before
     * constructing this packet, as done in CMLNetwork.
     */
    public SyncParcelPacket(Parcel parcel, String resolvedOwnerName, int borderStoneY) {
        this.parcelId    = parcel.getId();
        this.estateId    = parcel.getEstate().getId();
        this.parcelName  = parcel.getName() != null ? parcel.getName() : "";
        this.estateName  = parcel.getEstate().getName() != null ? parcel.getEstate().getName() : "";
        this.ownerName   = resolvedOwnerName != null ? resolvedOwnerName : "";
        this.ownerId = parcel.getEstate().getOwnerId();
        this.parcelType  = parcel.getType() != null ? parcel.getType() : ParcelType.NONE;
        this.relinquished = parcel.getEstate().isRelinquished();
        this.minX = parcel.getMinCoords().getX();
        this.minY = parcel.getMinCoords().getY();
        this.minZ = parcel.getMinCoords().getZ();
        this.maxX = parcel.getMaxCoords().getX();
        this.maxY = parcel.getMaxCoords().getY();
        this.maxZ = parcel.getMaxCoords().getZ();
        this.dimension   = parcel.getDimension();
        this.borderStoneY = borderStoneY;
        this.isPreview = false;
        this.isBorderVisible = false;
        this.conflictState = 0;
    }

    /**
     * network decode constructor.
     */
    SyncParcelPacket(
            UUID parcelId, UUID estateId,
            String parcelName, String estateName, String ownerName,
            UUID ownerId, ParcelType parcelType, boolean relinquished,
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ,
            String dimension,
            boolean isBorderVisible,
            int conflictState,
            int borderStoneY,
            boolean isPreview) {
        this.parcelId    = parcelId;
        this.estateId    = estateId;
        this.parcelName  = parcelName;
        this.estateName  = estateName;
        this.ownerName   = ownerName;
        this.ownerId = ownerId;
        this.parcelType  = parcelType;
        this.relinquished = relinquished;
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
        this.dimension   = dimension;
        this.isBorderVisible = isBorderVisible;
        this.conflictState = conflictState;
        this.borderStoneY = borderStoneY;
        this.isPreview = isPreview;
    }

    // -------------------------------------------------------------------------
    // encode / decode
    // -------------------------------------------------------------------------

    public static void encode(SyncParcelPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.parcelId);
        buf.writeUUID(packet.estateId);
        buf.writeUtf(packet.parcelName);
        buf.writeUtf(packet.estateName);
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
    }

    public static SyncParcelPacket decode(FriendlyByteBuf buf) {
        UUID parcelId    = buf.readUUID();
        UUID estateId = buf.readUUID();
        String parcelName  = buf.readUtf();
        String estateName  = buf.readUtf();
        String ownerName   = buf.readUtf();
        UUID ownerId = buf.readUUID();
        ParcelType type    = ParcelType.fromString(buf.readUtf());
        boolean relinquished = buf.readBoolean();
        int minX = buf.readInt(), minY = buf.readInt(), minZ = buf.readInt();
        int maxX = buf.readInt(), maxY = buf.readInt(), maxZ = buf.readInt();
        String dimension   = buf.readUtf();
        boolean isBorderVisible = buf.readBoolean();
        int conflictState = buf.readInt();
        int borderStoneY = buf.readInt();
        boolean isPreview = buf.readBoolean();

        return new SyncParcelPacket(
                parcelId, estateId,
                parcelName, estateName, ownerName,
                ownerId, type, relinquished,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                dimension,
                isBorderVisible,
                conflictState,
                borderStoneY,
                isPreview
        );
    }

    // -------------------------------------------------------------------------
    // handle
    // -------------------------------------------------------------------------

    public static void handle(SyncParcelPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientParcel existing = ClientParcelRegistry.findById(packet.parcelId).orElse(null);
            boolean borderVisible = existing != null ? existing.isBorderVisible() : packet.isBorderVisible;
            int conflictState = existing != null ? existing.conflictState() : packet.conflictState;
            int borderStoneY = existing != null && existing.borderStoneY() != 0
                    ? existing.borderStoneY() : packet.borderStoneY;

            ClientParcel clientParcel = new ClientParcel(
                    packet.parcelId,
                    packet.estateId,
                    packet.parcelName,
                    packet.estateName,
                    packet.ownerName,
                    packet.ownerId,
                    packet.parcelType,
                    packet.relinquished,
                    packet.minX, packet.minY, packet.minZ,
                    packet.maxX, packet.maxY, packet.maxZ,
                    packet.dimension,
                    borderVisible, conflictState, borderStoneY,
                    packet.isPreview
            );
            ClientParcelRegistry.register(clientParcel);

            if (ModList.get().isLoaded("journeymap")) {
                ParcelPolygonOverlayFactory.notifyParcelAdded(clientParcel);
            }

            ClaimMyLand.LOGGER.debug("SyncParcelPacket: registered parcel '{}' [{}]",
                    packet.parcelName, packet.parcelId);
        });
        ctx.get().setPacketHandled(true);
    }

    // -------------------------------------------------------------------------
    // accessor — used by SyncAllParcelsPacket to build ClientParcel directly
    // -------------------------------------------------------------------------

    public ClientParcel toClientParcel() {
        return new ClientParcel(
                parcelId, estateId,
                parcelName, estateName, ownerName,
                ownerId, parcelType, relinquished,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                dimension,
                isBorderVisible,
                conflictState, borderStoneY, isPreview
        );
    }

    /**
     * builds a preview packet from raw block entity state.
     * No server-side Parcel object is required — called before the parcel is claimed.
     */
    public static SyncParcelPacket forPreview(UUID parcelId, UUID estateId, UUID ownerId, String ownerName,
                                              ParcelType parcelType, Box box,
                                              int stoneY, String dimension, int conflictState) {
        return new SyncParcelPacket(
                parcelId,
                estateId,
                "",             // parcelName
                "",             // estateName
                ownerName != null ? ownerName : "",
                ownerId,
                parcelType != null ? parcelType : ParcelType.PLAYER,
                false,          // relinquished
                box.getMinCoords().getX(), box.getMinCoords().getY(), box.getMinCoords().getZ(),
                box.getMaxCoords().getX(), box.getMaxCoords().getY(), box.getMaxCoords().getZ(),
                dimension,
                true,           // isBorderVisible
                conflictState,
                stoneY,
                true            // isPreview
        );
    }
}