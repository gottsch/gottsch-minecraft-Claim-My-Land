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
import mod.gottsch.neo.claimmyland.core.integration.journeymap.JourneyMapOverlayHandler;
import mod.gottsch.neo.claimmyland.core.integration.journeymap.ParcelPolygonOverlayFactory;
import mod.gottsch.neo.claimmyland.core.registry.ClientParcelRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → Client packet. Bulk syncs all parcels to a player on login or
 * dimension change. Clears the client registry first, then populates it.
 *
 * <p>Each parcel in the list uses the same wire format as
 * {@link SyncParcelPacket}, encoded inline without packet headers.</p>
 *
 * @author Mark Gottschling on 3/3/2026
 */
public class SyncAllParcelsPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncAllParcelsPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ClaimMyLand.MOD_ID, "sync_all_parcels"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncAllParcelsPacket> STREAM_CODEC =
            StreamCodec.of(SyncAllParcelsPacket::encode, SyncAllParcelsPacket::decode);

    private final List<SyncParcelPacket> parcels;

    public SyncAllParcelsPacket(List<SyncParcelPacket> parcels) {
        this.parcels = parcels;
    }

    private static void encode(RegistryFriendlyByteBuf buf, SyncAllParcelsPacket packet) {
        buf.writeInt(packet.parcels.size());
        for (SyncParcelPacket p : packet.parcels) {
            SyncParcelPacket.encode(buf, p);
        }
    }

    private static SyncAllParcelsPacket decode(RegistryFriendlyByteBuf buf) {
        int count = buf.readInt();
        List<SyncParcelPacket> parcels = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            parcels.add(SyncParcelPacket.decode(buf));
        }
        return new SyncAllParcelsPacket(parcels);
    }

    public static void handle(SyncAllParcelsPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientParcelRegistry.clear();

            if (ModList.get().isLoaded("journeymap")) {
                JourneyMapOverlayHandler.removeAllOverlays();
                ParcelPolygonOverlayFactory.clearCache();
            }

            ClientParcelRegistry.registerAll(
                    packet.parcels.stream()
                            .map(SyncParcelPacket::toClientParcel)
                            .toList()
            );

            if (ModList.get().isLoaded("journeymap")) {
                ClientParcelRegistry.getAll().forEach(
                        ParcelPolygonOverlayFactory::notifyParcelAdded);
            }
        });
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}