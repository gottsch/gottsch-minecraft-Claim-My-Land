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

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.block.BorderBlock;
import mod.gottsch.forge.claimmyland.core.block.BorderStatus;
import mod.gottsch.forge.claimmyland.core.block.BufferBlock;
import mod.gottsch.forge.claimmyland.core.block.ModBlocks;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.parcel.NationParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.parcel.ZoneParcel;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Create by Mark Gottschling on Oct 14, 2204
 */
public class ZonePlacementBlockEntity extends BorderStoneBlockEntity {

    private static final String COORDS1 = "coords1";
    private static final String COORDS2 = "coords2";

    // TODO add Coords1, Coords2 properties
    private ICoords coords1;
    private ICoords coords2;

    /**
     *
     * @param pos
     * @param state
     */
    public ZonePlacementBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ZONE_PLACEMENT_ENTITY_TYPE.get(), pos, state);
    }

    // don't tick
    public void tickServer() {

    }

    // TODO need to separate all the border code from the ticking code
    @Override
    public int getBufferSize(ParcelType type) {
      return 0;
    }

    @Override
    public Block getBorderBlock() {
        return ModBlocks.ZONE_BORDER.get();
    }

    @Override
    protected BlockState getBorderBlockState(Box box) {
        Block borderBlock = getBorderBlock();
        // get the default block state of the border block
        BlockState blockState = borderBlock.defaultBlockState();

        /*
         * check if parcel is within another existing parcel
         */
        Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(box.getMinCoords());

        if (registryParcel.isEmpty()) {
            // if a zone placement is not within a nation then bad
            blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
        } else {
            // determine what parcel type is the zone in
            if (registryParcel.get().getType() != ParcelType.NATION) {
                blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
            } else {
                // if any part of the parcel is outside the nation, then bad
                if (!ModUtil.contains(registryParcel.get().getBox(), box)) {
                    blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
                } else {
                    // proceed with a normal overlaps check, filtering out nation parcels
                    List<Parcel> overlaps = ParcelRegistry.findBuffer(box);
                    overlaps = overlaps.stream()
                            .filter(p -> !(p instanceof NationParcel))
                            .toList();

                     if(Parcel.hasBoxToBufferedIntersections(box, getOwnerId(), overlaps)) {
                         blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
                     }
                }
            }
        }
        return blockState;
    }

    @Override
    // determines what state the buffer block is
    protected BlockState getBufferBlockState(Box box, Box bufferedBox) {
        // TODO return null - a zone shouldn't have a buffer
        return Blocks.AIR.defaultBlockState();
//        // get the default block state of the border block
//        BlockState blockState = ModBlocks.BUFFER.get().defaultBlockState();
//
//        /*
//         * check if box/parcel is within another existing parcel
//         */
//        Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(box.getMinCoords());
//
//        // not within another parcel
//        if (registryParcel.isEmpty()) {
//
//            // find overlaps of the buffered box with unbuffered parcels
//            // this ensure that the buffered boundaries are not overlapping the area of another parcel - too close!
//            // filter out the parcel that the buffer belongs to
//            List<Parcel> overlaps = ParcelRegistry.find(bufferedBox).stream()
//                    .filter(p -> !p.getId().equals(getParcelId())).toList();
//
//            if (!overlaps.isEmpty()) {
//                for (Parcel overlapParcel : overlaps) {
//                    // the parcels are owned by the same person. they can be closer or touching,
//                    // ie. ignore buffers, only the parcels themselves can't overlap
//                    if (!getOwnerId().equals(overlapParcel.getOwnerId())) {
//                        blockState = blockState.setValue(BufferBlock.INTERSECTS, BorderStatus.BAD);
//                        break;
//                    }
//                }
//            }
//        } else {
//            // determine what parcel type is the foundation stone in
//            if (registryParcel.get().getType() == ParcelType.CITIZEN) {
//                // do nothing - uses good state
//            } else if (registryParcel.get().getType() == ParcelType.NATION) {
//                // NOTE this shouldn't be allowed in the first place due to placement rules.
//                blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
//            } else if (registryParcel.get().getType() == ParcelType.ZONE) {
//
//                // TODO replace with Parcel.hasBoxToBufferedIntersections()...
//                // preform a normal overlaps check, filtering out nation and claim zone parcels
//                List<Parcel> overlaps = ParcelRegistry.find(bufferedBox).stream()
//                        .filter(p -> !p.getId().equals(getParcelId()))
//                        .filter(p -> !(p instanceof NationParcel || p instanceof ZoneParcel))
//                        .toList();
//
//                if(Parcel.hasBoxToBufferedIntersections(box, getOwnerId(), overlaps)) {
//                    blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
//                }
//            }
//        }
//        return blockState;
    }

    /**
     * like that of BorderStoneBlockEntity, but doesn't add the buffer border
     */
    @Override
    public void placeParcelBorder() {
        // add the border
        Box box = new Box(getCoords1(), getCoords2());
        BlockState borderState = getBorderBlockState(box);
        placeParcelBorder(box, borderState);
    }

    @Override
    public void removeParcelBorder(Level level, ICoords coords) {
        Box box = getBorderDisplayBox(coords);
        replaceParcelBorder(level, box, getBorderBlock(), Blocks.AIR.defaultBlockState());
    }

    /**
     * static variant where all values are provided
     */
    public static void removeParcelBorder(Level level, Box box, Block borderBlock) {
        replaceParcelBorder(level, box, borderBlock, Blocks.AIR.defaultBlockState());
    }

    @Override
    public Box getBorderDisplayBox(ICoords coords) {
        return getAbsoluteBox(coords);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        if (ObjectUtils.isNotEmpty(getCoords1())) {
            tag.put(COORDS1, getCoords1().save(new CompoundTag()));
        }

        if (ObjectUtils.isNotEmpty(getCoords2())) {
            tag.put(COORDS2, getCoords2().save(new CompoundTag()));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        try {
            if (tag.contains(COORDS1) && tag.get(COORDS1) != null) {
                setCoords1(Coords.EMPTY.load(tag.getCompound(COORDS1)));
            }
            if (tag.contains(COORDS2) && tag.get(COORDS2) != null) {
                setCoords2(Coords.EMPTY.load(tag.getCompound(COORDS2)));
            }
        } catch(Exception e) {
            ClaimMyLand.LOGGER.error("error loading coords", e);
        }
    }

    /**
     * Sync client and server states
     */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        if (tag != null) {
            load(tag);
        }
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        handleUpdateTag(tag);
    }

    public ICoords getCoords1() {
        return coords1;
    }

    public void setCoords1(ICoords coords1) {
        this.coords1 = coords1;
    }

    public ICoords getCoords2() {
        return coords2;
    }

    public void setCoords2(ICoords coords2) {
        this.coords2 = coords2;
    }
}
