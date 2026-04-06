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
package mod.gottsch.forge.claimmyland.core.block;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.block.entity.BorderStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.network.CMLNetwork;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.registry.ActiveBorderStoneRegistry;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * @author Mark Gottschling on Sep 18, 2024.
 * a Border Stone is used to display a border.
 *
 */
public class BorderStone extends BaseEntityBlock implements EntityBlock {
    public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class);

    private static final VoxelShape BOTTOM = Block.box(0, 0, 0, 16, 3, 16);
    private static final VoxelShape MIDDLE = Block.box(1, 3, 1, 15, 7, 15);
    private static final VoxelShape TOP = Block.box(0, 7, 0, 16, 10, 16);

    // TODO finish
    private static final VoxelShape SHAPE = Shapes.or(BOTTOM, MIDDLE, TOP);

    /**
     *
     * @param properties
     */
    public BorderStone(Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        BorderStoneBlockEntity blockEntity = null;
        try {
            blockEntity = new BorderStoneBlockEntity(pos, state);
        } catch (Exception e) {
            ClaimMyLand.LOGGER.error("error", e);
        }

        return blockEntity;
    }

    /**
     * execute the block entity ticker
     * @param level
     * @param state
     * @param type
     * @return
     * @param <T>
     */
    @javax.annotation.Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return (lvl, pos, blockState, t) -> {
                if (t instanceof BorderStoneBlockEntity entity) { // test and cast
                    entity.tickServer();
                }
            };
        } else {
            return null;
        }
    }

//    @Override
//    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
//        BorderStoneBlockEntity blockEntity = (BorderStoneBlockEntity) level.getBlockEntity(pos);
//        if (blockEntity != null) {
//            populateBlockEntity(level, blockEntity, Coords.of(pos));
//        }
//        super.onPlace(state, level, pos, oldState, isMoving);
//    }

    // first
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity entity, ItemStack itemStack) {

        super.setPlacedBy(level, pos, state, entity, itemStack);
        if (level.isClientSide() || !(entity instanceof ServerPlayer placingPlayer)) return;
        BorderStoneBlockEntity blockEntity = (BorderStoneBlockEntity) level.getBlockEntity(pos);
        if (blockEntity != null) {
            blockEntity.setPlacingPlayer(placingPlayer);
        }
    }

    // second
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        BorderStoneBlockEntity blockEntity = (BorderStoneBlockEntity) level.getBlockEntity(pos);
        if (blockEntity != null) {
            populateBlockEntity(level, blockEntity, Coords.of(pos));
            blockEntity.placeParcelBorder(blockEntity.getPlacingPlayer());
            blockEntity.setPlacingPlayer(null); // clear after use
        }
        super.onPlace(state, level, pos, oldState, isMoving);
    }

    // THIS occurs first, so populateBlockEntity is not call, and thus getParcelId = null. No border
//    @Override
//    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
//                            @Nullable LivingEntity entity, ItemStack itemStack) {
//
//        super.setPlacedBy(level, pos, state, entity, itemStack);
//
//        if (level.isClientSide() || !(entity instanceof ServerPlayer placingPlayer)) return;
//        BorderStoneBlockEntity blockEntity = (BorderStoneBlockEntity) level.getBlockEntity(pos);
//        if (blockEntity != null) {
//            blockEntity.placeParcelBorder(placingPlayer);
//        }
//    }
//    @Override
//    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
//                            @Nullable LivingEntity entity, ItemStack itemStack) {
//        super.setPlacedBy(level, pos, state, entity, itemStack);
//
//        if (level.isClientSide() || !(entity instanceof ServerPlayer placingPlayer)) return;
//
//        BorderStoneBlockEntity blockEntity = (BorderStoneBlockEntity) level.getBlockEntity(pos);
//        if (blockEntity == null || blockEntity.getParcelId() == null) return;
//
//        // if the placing player is not the registered estate owner (e.g. reclaiming
//        // a relinquished parcel), re-fire placeParcelBorder() with the correct player
//        // so the border visibility packet reaches them
//        Optional<Parcel> parcel = ParcelRegistry.findByParcelId(blockEntity.getParcelId());
//        boolean placerIsEstateOwner = parcel
//                .map(p -> p.getEstate().getOwnerId().equals(placingPlayer.getUUID()))
//                .orElse(true);
//
//        if (!placerIsEstateOwner) {
//            blockEntity.placeParcelBorder(placingPlayer);
//        }
//    }
//
//    @Override
//    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
//        BorderStoneBlockEntity blockEntity = (BorderStoneBlockEntity) level.getBlockEntity(pos);
//        if (blockEntity != null) {
//            populateBlockEntity(level, blockEntity, Coords.of(pos));
//        }
//        super.onPlace(state, level, pos, oldState, isMoving);
//    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean b) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            if (!serverLevel.getServer().isRunning()) return;
            BorderStoneBlockEntity blockEntity =
                    (BorderStoneBlockEntity) level.getBlockEntity(pos);
            if (blockEntity != null && blockEntity.getParcelId() != null) {
                Optional<Parcel> parcel = ParcelRegistry.findByParcelId(blockEntity.getParcelId());
                ClaimMyLand.LOGGER.debug("BorderStone.onRemove: parcelId={}, parcelPresent={}",
                        blockEntity.getParcelId(), parcel.isPresent());
                if (parcel.isPresent()) {
                    // committed parcel — hide the visual border across the dimension
                    CMLNetwork.syncBorderHiddenToDimension(serverLevel, parcel.get());
                    ActiveBorderStoneRegistry.remove(blockEntity);
                } else {
                    // phase 1 preview — parcel never committed; remove from client registries
                    CMLNetwork.removePreviewParcelFromTracking(
                            serverLevel, blockEntity.getParcelId(), pos);
                }
            } else if (blockEntity == null) {
                ClaimMyLand.LOGGER.debug("BorderStone.onRemove: blockEntity is NULL at pos={}", pos);
            } else {
                ClaimMyLand.LOGGER.debug("BorderStone.onRemove: parcelId is null");
            }
        }
        super.onRemove(state, level, pos, newState, b);
    }

    /**
     * set all the values of the block entity
     * @param blockEntity
     * @param coords
     */
    private void populateBlockEntity(Level level, BorderStoneBlockEntity blockEntity, ICoords coords) {
//        Optional<Parcel> parcel = ParcelRegistry.findLeastSignificant(coords);
        String dimension = level.dimension().location().toString();
        Optional<Parcel> parcel = ParcelRegistry.findLeastSignificant(coords, dimension);

        if (parcel.isPresent()) {
            blockEntity.setParcelId(parcel.get().getId());
            blockEntity.setOwnerId(parcel.get().getOwnerId());
            blockEntity.setParcelType(parcel.get().getType().getSerializedName());
            blockEntity.setCoords(parcel.get().getCoords());
            blockEntity.setRelativeBox(parcel.get().getSize());
            blockEntity.setExpireTime(level.getGameTime() + Config.SERVER.borders.borderStoneLifeSpan.get());

        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING,
                context.getHorizontalDirection().getOpposite());
    }
}
