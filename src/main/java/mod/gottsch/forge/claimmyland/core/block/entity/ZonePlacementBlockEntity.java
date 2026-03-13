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
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nullable;

/**
 * @author Mark Gottschling on Oct 14, 2204
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
