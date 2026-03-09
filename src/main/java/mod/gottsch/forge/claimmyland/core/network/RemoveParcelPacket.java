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
import mod.gottsch.forge.claimmyland.core.registry.ClientParcelRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Server → Client packet. Removes a single parcel from the client's
 * {@link ClientParcelRegistry} when it is demolished on the server.
 *
 * <p>Also clears {@link ClientParcelCache} if the removed parcel is
 * the one currently cached — the player is now in wilderness.</p>
 *
 * @author Mark Gottschling on 3/3/2026
 */
public class RemoveParcelPacket {

    private final UUID parcelId;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public RemoveParcelPacket(UUID parcelId) {
        this.parcelId = parcelId;
    }

    // -------------------------------------------------------------------------
    // Encode / Decode
    // -------------------------------------------------------------------------

    public static void encode(RemoveParcelPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.parcelId);
    }

    public static RemoveParcelPacket decode(FriendlyByteBuf buf) {
        return new RemoveParcelPacket(buf.readUUID());
    }

    // -------------------------------------------------------------------------
    // Handle
    // -------------------------------------------------------------------------

    public static void handle(RemoveParcelPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Remove from the full registry
            ClientParcelRegistry.unregister(packet.parcelId);

            // If this was the cached parcel, clear the position cache too
            ClientParcelCache.Entry current = ClientParcelCache.get();
            if (current != null && packet.parcelId.equals(current.getParcelId())) {
                ClientParcelCache.setWilderness();
                ClaimMyLand.LOGGER.debug("RemoveParcelPacket: cleared client cache (parcel was cached)");
            }

            if (ModList.get().isLoaded("journeymap")) {
                ParcelPolygonOverlayFactory.notifyParcelRemoved(packet.parcelId);
            }

            ClaimMyLand.LOGGER.debug("RemoveParcelPacket: removed parcel [{}]", packet.parcelId);
        });
        ctx.get().setPacketHandled(true);
    }

    public UUID getParcelId() {
        return parcelId;
    }
}
