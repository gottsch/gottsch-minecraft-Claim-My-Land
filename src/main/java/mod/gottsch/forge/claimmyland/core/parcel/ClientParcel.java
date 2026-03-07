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

package mod.gottsch.forge.claimmyland.core.parcel;

import java.util.UUID;

/**
 * Lightweight immutable client-side representation of a parcel.
 * Populated from {@link mod.gottsch.forge.claimmyland.core.network.SyncParcelPacket}
 * and stored in {@link mod.gottsch.forge.claimmyland.core.registry.ClientParcelRegistry}.
 *
 * <p>Contains only the fields needed for HUD display and look-based queries.
 * Not related to the server-side {@link Parcel} hierarchy.</p>
 *
 * @author Mark Gottschling on 3/3/2026
 */
public record ClientParcel(
        UUID parcelId,
        UUID estateId,
        String parcelName,
        String estateName,
        String ownerName,
        UUID ownerId,
        ParcelType parcelType,
        boolean relinquished,
        int minX, int minY, int minZ,
        int maxX, int maxY, int maxZ,
        String dimension
) {
    /**
     * Returns true if the given block coords fall within this parcel's bounds
     * in the given dimension.
     */
    public boolean contains(int x, int y, int z, String dim) {
        return dimension.equals(dim)
                && x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    /**
     * Returns true if this parcel's estate has been relinquished.
     * Driven by the relinquished flag on the Estate, not by owner name.
     */
    public boolean isRelinquished() {
        return relinquished;
    }
}
