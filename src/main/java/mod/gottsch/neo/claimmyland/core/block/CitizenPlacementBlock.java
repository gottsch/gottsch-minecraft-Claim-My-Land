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
package mod.gottsch.neo.claimmyland.core.block;

import com.mojang.serialization.MapCodec;
import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.block.entity.CitizenPlacementBlockEntity;
import mod.gottsch.neo.claimmyland.core.network.CMLNetwork;
import mod.gottsch.neo.gottschcore.spatial.Coords;
import mod.gottsch.neo.gottschcore.spatial.ICoords;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * @author Mark Gottschling on Oct 11, 2024
 */
public class CitizenPlacementBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11D, 16D, 11D);


    public CitizenPlacementBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return ModBlocks.CITIZEN_PLACEMENT_BLOCK.get().codec();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        CitizenPlacementBlockEntity blockEntity = null;
        try {
            blockEntity = new CitizenPlacementBlockEntity(pos, state);
        }
        catch(Exception e) {
            ClaimMyLand.LOGGER.error("error", e);
        }

        return blockEntity;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

@Override
public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
    if (state.getBlock() != newState.getBlock()) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CitizenPlacementBlockEntity zbe
                && zbe.getCoords1() != null && zbe.getCoords1() != Coords.EMPTY
                && zbe.getCoords2() != null && zbe.getCoords2() != Coords.EMPTY) {

            // remove visual preview border from client
            if (level instanceof ServerLevel serverLevel && zbe.getParcelId() != null) {
                CMLNetwork.removePreviewParcelFromTracking(serverLevel, zbe.getParcelId(), pos);
            }

            // remove the companion placement block
            ICoords coords1 = zbe.getCoords1();
            ICoords coords2 = zbe.getCoords2();
            if (!coords1.toPos().equals(pos) && level.getBlockState(coords1.toPos()).is(ModBlocks.CITIZEN_PLACEMENT_BLOCK.get())) {
                level.setBlock(coords1.toPos(), Blocks.AIR.defaultBlockState(), 3);
            }
            if (!coords2.toPos().equals(pos) && level.getBlockState(coords2.toPos()).is(ModBlocks.CITIZEN_PLACEMENT_BLOCK.get())) {
                level.setBlock(coords2.toPos(), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
}
