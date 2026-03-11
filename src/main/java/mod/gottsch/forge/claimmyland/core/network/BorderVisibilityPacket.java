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
package mod.gottsch.forge.claimmyland.core.network;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.registry.ClientParcelRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

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
public class BorderVisibilityPacket {

    private final UUID parcelId;
    private final boolean isBorderVisible;
    private final int conflictState;
    private final int borderStoneY;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public BorderVisibilityPacket(UUID parcelId, boolean isBorderVisible, int conflictState, int borderStoneY) {
        this.parcelId = parcelId;
        this.isBorderVisible = isBorderVisible;
        this.conflictState = conflictState;
        this.borderStoneY = borderStoneY;
    }

    // -------------------------------------------------------------------------
    // Encode / Decode
    // -------------------------------------------------------------------------

    public static void encode(BorderVisibilityPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.parcelId);
        buf.writeBoolean(packet.isBorderVisible);
        buf.writeInt(packet.conflictState);
        buf.writeInt(packet.borderStoneY);
    }

    public static BorderVisibilityPacket decode(FriendlyByteBuf buf) {
        UUID parcelId = buf.readUUID();
        boolean isBorderVisible = buf.readBoolean();
        int conflictState = buf.readInt();
        int borderStoneY = buf.readInt();
        return new BorderVisibilityPacket(parcelId, isBorderVisible, conflictState, borderStoneY);
    }

    // -------------------------------------------------------------------------
    // Handle
    // -------------------------------------------------------------------------

    public static void handle(BorderVisibilityPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ClaimMyLand.LOGGER.debug("BorderVisibilityPacket.handle: parcelId={}, visible={}, conflictState={}, stoneY={}",
                packet.parcelId, packet.isBorderVisible, packet.conflictState, packet.borderStoneY);
        ctx.get().enqueueWork(() -> {
            ClientParcelRegistry.findById(packet.parcelId).ifPresent(parcel -> {
                ClientParcelRegistry.register(
                        parcel.withBorderVisibility(packet.isBorderVisible, packet.conflictState, packet.borderStoneY)
                );
            });
        });
        ctx.get().setPacketHandled(true);
    }
}