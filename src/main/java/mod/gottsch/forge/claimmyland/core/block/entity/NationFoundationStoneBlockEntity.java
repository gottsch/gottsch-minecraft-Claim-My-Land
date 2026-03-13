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
package mod.gottsch.forge.claimmyland.core.block.entity;

import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * @author Mark Gottschling on Sep 20, 2204
 */
public class NationFoundationStoneBlockEntity extends FoundationStoneBlockEntity {

    /**
     *
     * @param pos
     * @param state
     */
    public NationFoundationStoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NATION_FOUNDATION_STONE_ENTITY_TYPE.get(), pos, state);
    }

    @Override
    public int getBufferSize(ParcelType type) {
      return Config.SERVER.general.nationParcelBufferRadius.get();
    }

}
