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
 */
package mod.gottsch.neo.claimmyland.core.network;

import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.registry.ClientParcelRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;


/**
 * Server → Client packet. Sent when a Border or Foundation Stone's visibility
 * state changes — on claim commit, conflict re-evaluation, TTL expiry, or
 * stone removal.
 *
 * <p>Both {@code isBorderVisible} and {@code conflictState} are always
 * transmitted together since conflict re-evaluation happens on the same tick
 * as visibility state changes.</p>
 *
 * @author Mark Gottschling on March 10, 2026
 */
public class BorderVisibilityPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BorderVisibilityPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ClaimMyLand.MOD_ID, "border_visibility"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BorderVisibilityPacket> STREAM_CODEC =
            StreamCodec.of(BorderVisibilityPacket::encode, BorderVisibilityPacket::decode);

    private final UUID parcelId;
    private final boolean isBorderVisible;
    private final int conflictState;
    private final int borderStoneY;
    private final UUID placingPlayerId;

    public BorderVisibilityPacket(UUID parcelId, boolean isBorderVisible, int conflictState,
                                  int borderStoneY, UUID placingPlayerId) {
        this.parcelId        = parcelId;
        this.isBorderVisible = isBorderVisible;
        this.conflictState   = conflictState;
        this.borderStoneY    = borderStoneY;
        this.placingPlayerId = placingPlayerId;
    }

    private static void encode(RegistryFriendlyByteBuf buf, BorderVisibilityPacket packet) {
        buf.writeUUID(packet.parcelId);
        buf.writeBoolean(packet.isBorderVisible);
        buf.writeInt(packet.conflictState);
        buf.writeInt(packet.borderStoneY);
        if (packet.placingPlayerId == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeUUID(packet.placingPlayerId);
        }
    }

    private static BorderVisibilityPacket decode(RegistryFriendlyByteBuf buf) {
        UUID parcelId        = buf.readUUID();
        boolean isBorderVisible = buf.readBoolean();
        int conflictState    = buf.readInt();
        int borderStoneY     = buf.readInt();
        boolean hasPlacingPlayer = buf.readBoolean();
        UUID placingPlayer   = hasPlacingPlayer ? buf.readUUID() : null;
        return new BorderVisibilityPacket(parcelId, isBorderVisible, conflictState, borderStoneY, placingPlayer);
    }

    public static void handle(BorderVisibilityPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() ->
                ClientParcelRegistry.findById(packet.parcelId).ifPresent(parcel ->
                        ClientParcelRegistry.register(
                                parcel.withBorderVisibility(packet.isBorderVisible, packet.conflictState,
                                        packet.borderStoneY, packet.placingPlayerId)
                        )
                )
        );
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}