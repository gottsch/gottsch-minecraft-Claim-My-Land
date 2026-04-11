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

import mod.gottsch.neo.claimmyland.core.estate.Estate;
import mod.gottsch.neo.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.neo.claimmyland.core.registry.PlayerRegistry;
import mod.gottsch.neo.claimmyland.core.util.ModUtil;
import mod.gottsch.neo.gottschcore.spatial.Box;
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

    public static Optional<ClaimResult> checkDirectSiblingOverlap(
            Parcel placing, Box parcelBox, Parcel parentParcel,
            String dimension) {

        return checkDirectSiblingOverlap(placing, parcelBox, parentParcel, dimension, true);
    }

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
            if (ModUtil.overlaps(parcelBox, existing.getBox())) {
                return Optional.of(ClaimResult.INTERSECTS);
            }
        }
        return Optional.empty();
    }

    private ParcelHelper() {}
}
