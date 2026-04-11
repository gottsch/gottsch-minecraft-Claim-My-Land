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
import mod.gottsch.neo.claimmyland.core.cache.ClientParcelCache;
import mod.gottsch.neo.claimmyland.core.integration.journeymap.ParcelPolygonOverlayFactory;
import mod.gottsch.neo.claimmyland.core.registry.ClientParcelRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * Server → Client packet. Removes a single parcel from the client's
 * {@link ClientParcelRegistry} when it is demolished on the server.
 *
 * <p>Also clears {@link ClientParcelCache} if the removed parcel is
 * the one currently cached — the player is now in wilderness.</p>
 *
 * @author Mark Gottschling on 3/3/2026
 */
public class RemoveParcelPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RemoveParcelPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ClaimMyLand.MOD_ID, "remove_parcel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveParcelPacket> STREAM_CODEC =
            StreamCodec.of(RemoveParcelPacket::encode, RemoveParcelPacket::decode);

    private final UUID parcelId;

    public RemoveParcelPacket(UUID parcelId) {
        this.parcelId = parcelId;
    }

    private static void encode(RegistryFriendlyByteBuf buf, RemoveParcelPacket packet) {
        buf.writeUUID(packet.parcelId);
    }

    private static RemoveParcelPacket decode(RegistryFriendlyByteBuf buf) {
        return new RemoveParcelPacket(buf.readUUID());
    }

    public static void handle(RemoveParcelPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientParcelRegistry.unregister(packet.parcelId);

            ClientParcelCache.Entry current = ClientParcelCache.get();
            if (current != null && packet.parcelId.equals(current.getParcelId())) {
                ClientParcelCache.setWilderness();
//                ClaimMyLand.LOGGER.debug("RemoveParcelPacket: cleared client cache (parcel was cached)");
            }

            if (ModList.get().isLoaded("journeymap")) {
                ParcelPolygonOverlayFactory.notifyParcelRemoved(packet.parcelId);
            }

//            ClaimMyLand.LOGGER.debug("RemoveParcelPacket: removed parcel [{}]", packet.parcelId);
        });
    }

    public UUID getParcelId() { return parcelId; }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}