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

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * @author by Mark Gottschling on 1/29/2026
 */
public class EstateContext extends AbstractEstate {

    public EstateContext() {
        super();
    }

    public EstateContext(Player player) {
        super(player);
    }

    public EstateContext(UUID ownerUuid) {
        super(ownerUuid);
    }

    @Override
    public ResourceLocation getType() {
        return EstateTypeRegistry.ESTATE_TYPE;
    }

//    @Override
//    public boolean canJoin(Estate estate) {
//        Parcel p1 = getParcels().iterator().next();
//        Parcel p2 = estate.getParcels().iterator().next();
//        return !getId().equals(estate.getId())
//                && getOwnerId().equals(estate.getOwnerId())
//                && p1.getType() == p2.getType()
//                && !isRelinquished() && !estate.isRelinquished();
//    }
    @Override
    public boolean canJoin(Estate estate) {
        if (getId().equals(estate.getId()) || isRelinquished() || estate.isRelinquished()) {
            return false;
        }

        if (!getOwnerId().equals(estate.getOwnerId())) {
            return false;
        }

        Optional<Parcel> p1 = findParcels().stream().findFirst();
        if (p1.isEmpty()) {
            return false;
        }

        return estate.findParcels().stream()
                .findFirst()
                .map(p2 -> p1.get().getType() == p2.getType())
                .orElse(false);
    }
}
