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

package mod.gottsch.neo.claimmyland.core.command;

import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.estate.Estate;
import mod.gottsch.neo.claimmyland.core.item.DeedFactory;
import mod.gottsch.neo.claimmyland.core.parcel.ParcelType;
import mod.gottsch.neo.claimmyland.core.registry.EstateRegistry;
import mod.gottsch.neo.gottschcore.spatial.Box;
import mod.gottsch.neo.gottschcore.spatial.Coords;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

import static mod.gottsch.neo.claimmyland.core.command.helper.CommandHelper.failure;

/**
 * @author by Mark Gottschling on 3/2/2026
 */
public abstract class DeedsSubCommand implements ArgumentSubCommand {

    public static final String DEED_TYPE = "deed_type";
    public static final String POS = "pos";
    public static final String X_SIZE = "x_size";
    public static final String Y_SIZE_UP = "y_size_up";
    public static final String Y_SIZE_DOWN = "y_size_down";
    public static final String Z_SIZE = "z_size";


    /*
     * ops versions
     */
    protected int generateDeed(CommandSourceStack source, ParcelType deedType, int xSize, int ySizeUp, int ySizeDown, int zSize) {
        return generateDeed(source, deedType, xSize, ySizeUp, ySizeDown, zSize, null);
    }

    protected int generateDeed(CommandSourceStack source, ParcelType type, int xSize, int ySizeUp, int ySizeDown, int zSize, String nationName) {
        Optional<Estate> optionalEstate = EstateRegistry.findByName(nationName);

        // validations
        if ((type == ParcelType.CITIZEN) && optionalEstate.isEmpty()) {
            failure(source, "deed.citizen.nationId_required");
            return -1;
        }

        // validate sizes
        if (!isValidSize(source, xSize, ySizeUp, ySizeDown, zSize)) {
            return -1;
        }

        // resolve nation estate only when needed
        Result<Estate> nationResult = resolveNationEstate(source, type, nationName);
        if (nationResult.isFailure()) {
            return -1;
        }

        // create a relative sized Box
        Box size = new Box(Coords.of(0, -ySizeDown, 0), Coords.of(xSize-1, ySizeUp-1, zSize-1));

        // pass the result straight through - spawnDeed only calls getValue() for CITIZEN type
        return spawnDeed(source, type, size, nationResult);
    }

    private boolean isValidSize(CommandSourceStack source, int xSize, int ySizeUp, int ySizeDown, int zSize) {
        if (xSize < 2 || (ySizeUp + ySizeDown) < 2 || zSize < 2) {
            failure(source, "deed.too_small");
            return false;
        }
        if (source.getLevel().isOutsideBuildHeight(ySizeUp + ySizeDown)) {
            failure(source, "deed.outside_world_boundaries");
            return false;
        }
        return true;
    }

    private Result<Estate> resolveNationEstate(CommandSourceStack source, ParcelType type, String nationName) {
        if (type != ParcelType.CITIZEN) {
            return Result.empty();
        }
        if (nationName == null || nationName.isBlank()) {
             failure(source, "deed.citizen.nationId_required");
            return Result.failure();
        }
        Optional<Estate> estate = EstateRegistry.findByName(nationName);
        if (estate.isEmpty()) {
            failure(source, "deed.citizen.nation_not_found");
            return Result.failure();
        }
        return Result.success(estate.get());
    }

    private int spawnDeed(CommandSourceStack source, ParcelType type, Box size, Result<Estate> nationResult) {
        try {
            ItemStack deed = switch (type) {
                case PLAYER -> DeedFactory.createPlayerDeed(size);
                case NATION -> DeedFactory.createNationDeed(source.getLevel(), size);
                case CITIZEN -> DeedFactory.createCitizenDeed(size, nationResult.getValue().getId(), nationResult.getValue().getName());
                case ZONE -> ItemStack.EMPTY;
                default -> ItemStack.EMPTY;
            };

            if (deed != ItemStack.EMPTY) {
                source.getPlayerOrException().getInventory().add(deed);
            }
        } catch (Exception e) {
            ClaimMyLand.LOGGER.error("error while generating deed:", e);
            failure(source, "deed.generate.failure");
            return -1;
        }
        return 1;
    }
}
