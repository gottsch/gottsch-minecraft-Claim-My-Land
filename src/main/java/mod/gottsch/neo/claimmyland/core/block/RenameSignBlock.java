/*
 * This file is part of  Claim My Land.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
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
package mod.gottsch.neo.claimmyland.core.block;

import mod.gottsch.neo.claimmyland.core.block.entity.RenameSignBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;

/**
 * A temporary wall-sign block used exclusively for the Border Stone parcel-rename
 * workflow. It is never in a player's inventory — the block is placed programmatically
 * by {@link BorderStoneBlock#useItemOn} when a player right-clicks a Border Stone while
 * holding any sign item, and is removed by {@link RenameSignBlockEntity#updateText}
 * after the player submits the sign editor.
 *
 * <p>Uses {@link WoodType#OAK} as the wood type because the block is never rendered
 * from inventory and does not need its own wood-type appearance. A placeholder block
 * model is sufficient (see resources note in the patch guide).
 *
 * @author Mark Gottschling on Apr 16, 2026
 */
public class RenameSignBlock extends WallSignBlock {

    /**
     * @param properties block behaviour properties — should have no collision, no
     *                   occlusion, and not be obtainable by players
     */
    public RenameSignBlock(BlockBehaviour.Properties properties) {
        super(WoodType.OAK, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RenameSignBlockEntity(pos, state);
    }
}