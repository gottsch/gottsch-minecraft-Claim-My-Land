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
package mod.gottsch.neo.claimmyland.core.parcel;

import java.util.UUID;

/**
 * Single authoritative source for parcel conflict determination.
 * All conflict checks in handleClaim(), resolveConflictState(), and
 * findConflicting() delegate here instead of duplicating ownership/type logic.
 *
 * Rules (applied in priority order):
 *   1. Hierarchical relationship (ancestor/descendant) → never a conflict
 *   2. Foreign owner                                   → always a conflict
 *   3. Same owner, same type (sibling)                 → never a buffer conflict
 *                                                         (raw overlap is still blocked by the caller)
 *   4. Same owner, different non-hierarchical type     → always a conflict
 *                                                         (e.g. PLAYER adjacent to own NATION/ZONE)
 *
 * @author Mark Gottschling on Apr 1, 2026
 */
public class ParcelConflictResolver {

    private ParcelConflictResolver() {}

    /**
     * Determines whether placing a parcel of {@code placingType} owned by
     * {@code placingOwner} conflicts with an existing parcel of {@code existingType}
     * owned by {@code existingOwner}.
     *
     * This method answers the ownership/type question only. Geometry (box overlap,
     * containment, buffer zone) is the caller's responsibility.
     *
     * Same-owner same-type siblings return {@code false} here, but the caller
     * must still block raw box overlap — siblings may touch but not intersect.
     *
     * @param placingType   the type of the parcel being placed
     * @param existingType  the type of the existing parcel
     * @param placingOwner  the owner UUID of the parcel being placed
     * @param existingOwner the owner UUID of the existing parcel
     * @return true if this is a conflict, false if it is permitted
     */
    public static boolean isConflict(ParcelType placingType, ParcelType existingType,
                                     UUID placingOwner, UUID existingOwner) {
        // Rule 1: hierarchical relationship → never a conflict regardless of owner
        if (ParcelType.isAllowedAncestor(existingType, placingType)) return false;
        if (ParcelType.isAllowedDescendant(existingType, placingType)) return false;

        // Rule 2: foreign owner, non-hierarchical → always a conflict
        if (!placingOwner.equals(existingOwner)) return true;

        // Rule 3: same owner, same type → sibling, not a buffer conflict
        // (caller must still reject raw box overlap)
        if (existingType == placingType) return false;

        // Rule 4: same owner, different non-hierarchical type → conflict
        // e.g. PLAYER parcel placed adjacent to own NATION/ZONE
        return true;
    }
}
