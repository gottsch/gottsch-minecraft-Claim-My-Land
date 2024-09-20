/*
 * This file is part of  Claim My Land.
 * Copyright (c) 2024 Mark Gottschling (gottsch)
 *
 * All rights reserved.
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
package mod.gottsch.forge.claimmyland.core.item;

import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/**
 * Created by Mark Gottschling on Sep 20, 2024
 */
public class BorderStoneBlockItem extends BlockItem {
    public BorderStoneBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        // need to check to ensure you're in an existing parcel
        // NOTE this causes a redundant parcel registry search and
        // prevents the block being placed outside a parcel boundary.
        // to/change prevent this the BlockEntity code would have to change
        // in some way to check for non-existing parcel - results in having different
        // code in the BorderStoneBE and the FoundationStoneBE
        Optional<Parcel> parcel = ParcelRegistry.findLeastSignificant(Coords.of(context.getClickedPos()));
        if (parcel.isEmpty()) {
            return InteractionResult.FAIL;
        } else {
            return super.place(context);
        }
    }
}
