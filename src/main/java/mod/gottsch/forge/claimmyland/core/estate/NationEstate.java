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

package mod.gottsch.forge.claimmyland.core.estate;

import mod.gottsch.forge.claimmyland.core.parcel.NationAccessType;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;

import java.util.Set;
import java.util.UUID;

/**
 * @author by Mark Gottschling on 2/9/2026
 */
public interface NationEstate extends Estate {
    public static final String ACCESS_TYPE_KEY = "accessType";
    public static final String PLAYER_BLACKLIST_KEY = "playerBlacklist";

    Set<Parcel> findTenantParcels();

    NationAccessType getAccessType();
    void setAccessType(NationAccessType type);

    Set<UUID> getPlayerBlacklist();

    /**
     * Returns true if the given player UUID is on this nation's blacklist.
     * The nation owner is never considered blacklisted regardless of list contents.
     *
     * @param playerId the UUID to test
     * @return true if blacklisted and not the owner
     */
    default boolean isBlacklisted(UUID playerId) {
        if (playerId == null) return false;
        // Owner is always exempt
        if (playerId.equals(getOwnerId())) return false;
        return getPlayerBlacklist().contains(playerId);
    }
}
