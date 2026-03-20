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
import mod.gottsch.forge.claimmyland.core.cache.ClientParcelCache;
import mod.gottsch.forge.claimmyland.core.integration.journeymap.ParcelPolygonOverlayFactory;
import mod.gottsch.forge.claimmyland.core.parcel.ClientParcel;
import mod.gottsch.forge.claimmyland.core.parcel.NationalizedParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.ClientParcelRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Server → Client packet. Tells the client the parcel the player is currently
 * inside so the client can perform instant protection checks without a round-trip.
 *
 * <p>Carries only the fields the client needs:</p>
 * <ul>
 *   <li>parcelId — UUID, nullable (null = wilderness)</li>
 *   <li>estateId — UUID, nullable</li>
 *   <li>parcelName — String</li>
 *   <li>estateName — String</li>
 *   <li>ownerName — String (display name, for HUD)</li>
 *   <li>parcelType — ParcelType enum</li>
 *   <li>minX/Y/Z, maxX/Y/Z — int (absolute world coords)</li>
 *   <li>dimension — String</li>
 * </ul>
 *
 * <p>Approximate wire size: ~120-150 bytes per packet.</p>
 *
 * @author by Mark Gottschling on 3/3/2026
 */
public class CacheSyncPacket {

    // Sentinel — written to the buffer when parcel is null (wilderness).
    private static final String WILDERNESS = "";

    // -------------------------------------------------------------------------
    // Packet fields — all nullable for the wilderness case
    // -------------------------------------------------------------------------
    @Nullable private final UUID parcelId;
    @Nullable private final UUID estateId;
    private final String parcelName;
    private final String estateName;
    private final String nationName;
    private final String ownerName;
    private final UUID ownerId;
    private final ParcelType parcelType;
    private final boolean relinquished;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    private final String dimension;
    private final int borderStoneY;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * server-side constructor — build from a live Parcel (or null for wilderness).
     */
    public CacheSyncPacket(@Nullable Parcel parcel, @Nullable String resolvedOwnerName) {
        if (parcel == null) {            // Wilderness — zero/empty everything
            this.parcelId  = null;
            this.estateId  = null;
            this.parcelName  = WILDERNESS;
            this.estateName  = WILDERNESS;
            this.nationName = WILDERNESS;
            this.ownerName   = WILDERNESS;
            this.ownerId = null;
            this.parcelType   = ParcelType.NONE;
            this.relinquished = false;
            this.minX = this.minY = this.minZ = 0;
            this.maxX = this.maxY = this.maxZ = 0;
            this.dimension = WILDERNESS;
            this.borderStoneY = 0;
        } else {
            this.parcelId   = parcel.getId();
            this.estateId   = parcel.getEstate().getId();
            this.parcelName = parcel.getName() != null ? parcel.getName() : WILDERNESS;
            this.estateName = parcel.getEstate().getName() != null ? parcel.getEstate().getName() : WILDERNESS;
            this.nationName = (parcel instanceof NationalizedParcel n) ? n.getNationEstate().getName() : WILDERNESS;
            // Owner name is resolved by the caller (CMLNetwork.syncCacheToPlayer)
            // using PlayerRegistry.getPlayerName(ServerLevel, UUID) before the
            // packet is constructed, so it arrives here already resolved.
            this.ownerName = resolvedOwnerName != null ? resolvedOwnerName : WILDERNESS;
            this.ownerId = parcel.getEstate().getOwnerId();
            this.parcelType = parcel.getType() != null ? parcel.getType() : ParcelType.NONE;
            this.relinquished = parcel.getEstate().isRelinquished();
            this.minX = parcel.getMinCoords().getX();
            this.minY = parcel.getMinCoords().getY();
            this.minZ = parcel.getMinCoords().getZ();
            this.maxX = parcel.getMaxCoords().getX();
            this.maxY = parcel.getMaxCoords().getY();
            this.maxZ = parcel.getMaxCoords().getZ();
            this.dimension = parcel.getDimension();
            this.borderStoneY = parcel.getCoords().getY();
        }
    }

    /**
     * network decode constructor — called by {@link #decode(FriendlyByteBuf)}.
     */
    private CacheSyncPacket(
            @Nullable UUID parcelId, @Nullable UUID estateId,
            String parcelName, String estateName,
            String nationName, String ownerName,
            UUID ownerId, ParcelType parcelType, boolean relinquished,
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ,
            String dimension,
            int borderStoneY) {
        this.parcelId   = parcelId;
        this.estateId   = estateId;
        this.parcelName = parcelName;
        this.estateName = estateName;
        this.nationName = nationName;
        this.ownerName  = ownerName;
        this.ownerId = ownerId;
        this.parcelType = parcelType;
        this.relinquished = relinquished;
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
        this.dimension  = dimension;
        this.borderStoneY = borderStoneY;
    }

    // -------------------------------------------------------------------------
    // encode / decode
    // -------------------------------------------------------------------------

    public static void encode(CacheSyncPacket packet, FriendlyByteBuf buf) {
        // Write a boolean flag first: true = has parcel, false = wilderness
        boolean hasParcel = packet.parcelId != null;
        buf.writeBoolean(hasParcel);

        if (hasParcel) {
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
            buf.writeInt(packet.borderStoneY);
        }
        // No else — wilderness is fully represented by hasParcel = false
    }

    public static CacheSyncPacket decode(FriendlyByteBuf buf) {
        boolean hasParcel = buf.readBoolean();

        if (!hasParcel) {
            return new CacheSyncPacket(null, null);
        }

        UUID parcelId   = buf.readUUID();
        UUID estateId   = buf.readUUID();
        String parcelName  = buf.readUtf();
        String estateName  = buf.readUtf();
        String nationName = buf.readUtf();
        if (nationName.isEmpty()) nationName = null;
        String ownerName   = buf.readUtf();
        UUID ownerId = buf.readUUID();
        ParcelType type    = ParcelType.fromString(buf.readUtf());
        boolean relinquished = buf.readBoolean();
        int minX = buf.readInt(), minY = buf.readInt(), minZ = buf.readInt();
        int maxX = buf.readInt(), maxY = buf.readInt(), maxZ = buf.readInt();
        String dimension   = buf.readUtf();
        int borderStoneY = buf.readInt();

        return new CacheSyncPacket(
                parcelId, estateId,
                parcelName, estateName,
                nationName, ownerName,
                ownerId, type, relinquished,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                dimension,
                borderStoneY
        );
    }

    // -------------------------------------------------------------------------
    // Handle
    // -------------------------------------------------------------------------

    public static void handle(CacheSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (packet.parcelId == null) {
                ClientParcelCache.setWilderness();
                ClaimMyLand.LOGGER.debug("CacheSyncPacket: client cache set to wilderness");
            } else {
                // Update the single-entry position cache
                ClientParcelCache.update(
                        packet.parcelId,
                        packet.estateId,
                        packet.parcelName,
                        packet.estateName,
                        packet.nationName,
                        packet.ownerName,
                        packet.ownerId,
                        packet.parcelType,
                        packet.minX, packet.minY, packet.minZ,
                        packet.maxX, packet.maxY, packet.maxZ,
                        packet.dimension
                );
                // also ensure the full registry knows about this parcel.
                // the player is inside it, so it must be registered for HUD queries.

                // look up existing entry to preserve isBorderVisible
                ClientParcel existing = ClientParcelRegistry.findById(packet.parcelId).orElse(null);

                // if the existing registry entry is a preview parcel, don't overwrite it —
                // the preview entry is owned by SyncParcelPacket and CacheSyncPacket must not stomp it
                if (existing != null && existing.isPreview()) {
                    ClaimMyLand.LOGGER.debug("CacheSyncPacket.handle: skipping registry update for preview parcel {}", packet.parcelId);
                    return;
                }

                boolean borderVisible = existing != null && existing.isBorderVisible();
                int conflictState = existing != null ? existing.conflictState() : 0;
                int borderStoneY = existing != null && existing.borderStoneY() != 0
                        ? existing.borderStoneY()
                        : packet.borderStoneY;
                UUID placingPlayer = existing != null ? existing.placingPlayer() : null;
//                boolean isPreview = existing != null && existing.isPreview(); // preserve preview state
                String nationName     = packet.nationName != null   // prefer fresh packet value
                        ? packet.nationName
                        : (existing != null ? existing.nationName() : null);

                ClientParcel clientParcel = new ClientParcel(
                        packet.parcelId,
                        packet.estateId,
                        packet.parcelName,
                        packet.estateName,
                        nationName,
                        packet.ownerName,
                        packet.ownerId,
                        packet.parcelType,
                        packet.relinquished,
                        packet.minX, packet.minY, packet.minZ,
                        packet.maxX, packet.maxY, packet.maxZ,
                        packet.dimension,
                borderVisible, conflictState, borderStoneY,
                        false, placingPlayer
                );
                ClaimMyLand.LOGGER.debug("CacheSyncPacket.handle: parcelId={}, borderVisible={} (from existing={})",
                        packet.parcelId, borderVisible, existing != null);
                ClientParcelRegistry.register(clientParcel);

                if (ModList.get().isLoaded("journeymap")) {
                    ParcelPolygonOverlayFactory.notifyParcelAdded(clientParcel);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    // -------------------------------------------------------------------------
    // Accessors (for ClientParcelCache and HUD use)
    // -------------------------------------------------------------------------

    @Nullable public UUID getParcelId()   { return parcelId; }
    @Nullable public UUID getEstateId()   { return estateId; }
    public String getParcelName()         { return parcelName; }
    public String getEstateName()         { return estateName; }
    public String getNationName() { return nationName; }
    public String getOwnerName()          { return ownerName; }
    public ParcelType getParcelType()     { return parcelType; }
    public int getMinX()                  { return minX; }
    public int getMinY()                  { return minY; }
    public int getMinZ()                  { return minZ; }
    public int getMaxX()                  { return maxX; }
    public int getMaxY()                  { return maxY; }
    public int getMaxZ()                  { return maxZ; }
    public String getDimension()          { return dimension; }
    public boolean isWilderness()         { return parcelId == null; }
}