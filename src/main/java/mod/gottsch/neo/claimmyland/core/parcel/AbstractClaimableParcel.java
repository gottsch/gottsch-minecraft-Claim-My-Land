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

import mod.gottsch.neo.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.neo.claimmyland.core.util.ModUtil;
import mod.gottsch.neo.gottschcore.spatial.Box;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * @author by Mark Gottschling on 2/26/2026
 */
public abstract class AbstractClaimableParcel extends AbstractParcel {

    protected boolean isRelinquishedCitizenClaim(Parcel parentParcel, Box parcelBox) {
        return parentParcel.isCitizen() && parentParcel.getEstate().isRelinquished();
    }

    protected ClaimResult claimRelinquishedCitizenParcel(Level level, Parcel parentParcel, Box parcelBox) {
        if (ModUtil.getVolume(parcelBox) < parentParcel.getArea()) {
            return ClaimResult.INSUFFICIENT_SIZE;
        }
        ParcelRegistry.transferParcelOwnership((ServerLevel) level, parentParcel, getOwnerId());
        return ClaimResult.SUCCESS;
    }

    protected abstract ClaimResult claimWithinZone(Level level, Parcel parentParcel, Box parcelBox);

    /**
     * default handle from Parcel interface
     * @param level
     * @param parentParcel
     * @return
     */
    @Override
    public ClaimResult handleEmbeddedClaim(Level level, Parcel parentParcel) { //}, Box parcelBox) {
        ClaimResult result = ClaimResult.FAILURE;

        // claiming an existing and relinquished citizen parcel
        if (isRelinquishedCitizenClaim(parentParcel, getBox())) {
            return claimRelinquishedCitizenParcel(level, parentParcel, getBox());
        }

        // placing a parcel within a zone
        if (isValidParentParcel(parentParcel)) {
            return claimWithinZone(level, parentParcel, getBox());
        }
        return ClaimResult.FAILURE;
    }

    protected boolean isValidParentParcel(Parcel parcel) {
        return parcel.isZone();
    }
}
