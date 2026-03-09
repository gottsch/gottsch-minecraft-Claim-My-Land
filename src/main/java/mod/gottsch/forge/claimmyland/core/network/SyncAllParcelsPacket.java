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
import mod.gottsch.forge.claimmyland.core.registry.ClientParcelRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server → Client packet. Bulk syncs all parcels to a player on login or
 * dimension change. Clears the client registry first, then populates it.
 *
 * <p>Each parcel in the list uses the same wire format as
 * {@link SyncParcelPacket}, encoded inline without packet headers.</p>
 *
 * @author Mark Gottschling on 3/3/2026
 */
public class SyncAllParcelsPacket {

    private final List<SyncParcelPacket> parcels;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public SyncAllParcelsPacket(List<SyncParcelPacket> parcels) {
        this.parcels = parcels;
    }

    // -------------------------------------------------------------------------
    // Encode / Decode
    // -------------------------------------------------------------------------

    public static void encode(SyncAllParcelsPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.parcels.size());
        for (SyncParcelPacket p : packet.parcels) {
            SyncParcelPacket.encode(p, buf);
        }
    }

    public static SyncAllParcelsPacket decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<SyncParcelPacket> parcels = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            parcels.add(SyncParcelPacket.decode(buf));
        }
        return new SyncAllParcelsPacket(parcels);
    }

    // -------------------------------------------------------------------------
    // Handle
    // -------------------------------------------------------------------------

    public static void handle(SyncAllParcelsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Clear stale data from previous session or dimension
            ClientParcelRegistry.clear();
            // Bulk register all parcels
            ClientParcelRegistry.registerAll(
                    packet.parcels.stream()
                            .map(SyncParcelPacket::toClientParcel)
                            .toList()
            );
            ClaimMyLand.LOGGER.debug("SyncAllParcelsPacket: synced {} parcel(s) to client",
                    packet.parcels.size());
        });
        ctx.get().setPacketHandled(true);
    }
}
