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

import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.registry.PlayerRegistry;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author by Mark Gottschling on 3/5/2026
 */
public class ParcelHelper {

    public static String buildName(ServerLevel level, Estate estate) {
        String playerName = PlayerRegistry.getPlayerName(level, estate.getOwnerId())
                .orElse(estate.getOwnerId().toString().substring(0, 8));
        String base = playerName + "-parcel-";
        Set<Parcel> existing = estate.findParcels();
        int i = 1;
        while (true) {
            final int index = i;
            if (existing.stream().noneMatch(p -> p.getName().equals(base + index))) {
                return base + index;
            }
            i++;
        }
    }

    /**
     * Direct box overlap check against sibling parcels within a parent parcel.
     * Mirrors the direct overlap logic in Parcel.handleClaim() for top-level parcels,
     * closing the parity gap between top-level and embedded claim validation.
     *
     * @param placing      the parcel being claimed
     * @param parcelBox    the box of the parcel being claimed
     * @param parentParcel the enclosing parent parcel (filtered out of results)
     * @param dimension    the dimension string
     * @param excludeZones if true, zone parcels are excluded from the sibling check —
     *                     use true for Citizen/Player (zone is a valid parent, not a
     *                     conflict), false for Zone (sibling zones inside a Nation must
     *                     be checked against each other)
     * @return INTERSECTS or NOT_IN_PARENT if a conflict is found, empty if clear
     */
    public static Optional<ClaimResult> checkDirectSiblingOverlap(
            Parcel placing, Box parcelBox, Parcel parentParcel,
            String dimension, boolean excludeZones) {

        List<Parcel> directOverlaps = ParcelRegistry.find(parcelBox, dimension).stream()
                .filter(p -> !p.getId().equals(parentParcel.getId()))
                .filter(p -> !p.isNation())
                .filter(p -> !excludeZones || !p.isZone())
                .toList();

        for (Parcel existing : directOverlaps) {
            boolean hierarchical = ParcelType.isAllowedAncestor(existing.getType(), placing.getType())
                    || ParcelType.isAllowedDescendant(existing.getType(), placing.getType());
            if (hierarchical) {
                boolean ancestorContains = ParcelType.isAllowedAncestor(existing.getType(), placing.getType())
                        && ModUtil.contains(existing.getBox(), parcelBox);
                boolean placingContains = ParcelType.isAllowedDescendant(existing.getType(), placing.getType())
                        && ModUtil.contains(parcelBox, existing.getBox());
                if (!ancestorContains && !placingContains) return Optional.of(ClaimResult.NOT_IN_PARENT);
                continue;
            }
            if (ParcelConflictResolver.isConflict(placing.getType(), existing.getType(),
                    placing.getOwnerId(), existing.getOwnerId())) {
                return Optional.of(ClaimResult.INTERSECTS);
            }
            // same-owner same-type sibling: touching is fine, direct overlap is a conflict
            if (ModUtil.overlaps(parcelBox, existing.getBox())) {
                return Optional.of(ClaimResult.INTERSECTS);
            }
        }
        return Optional.empty();
    }

    /**
     * Convenience overload with excludeZones=true — correct default for Citizen and
     * Player parcels whose containing Zone is a valid parent, not a sibling conflict.
     */
    public static Optional<ClaimResult> checkDirectSiblingOverlap(
            Parcel placing, Box parcelBox, Parcel parentParcel,
            String dimension) {
        return checkDirectSiblingOverlap(placing, parcelBox, parentParcel, dimension, true);
    }

    private ParcelHelper() {}
}
