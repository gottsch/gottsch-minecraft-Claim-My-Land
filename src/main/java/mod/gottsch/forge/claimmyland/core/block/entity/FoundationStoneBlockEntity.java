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
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Create by Mark Gottschling on Sep 14, 2024
 */
public abstract class FoundationStoneBlockEntity extends BorderStoneBlockEntity {
    private static final String DEED_ID = "deed_id";
    private static final String NATION_ID = "nation_id";

    // unique id of of deed that created this block / block entity
    // TODO might not be necessary when adding TransferDeed
    private UUID deedId;

    // unique id of the nation this parcel belong to
    private UUID nationId;

    /**
     *
     * @param type
     * @param pos
     * @param state
     */
    public FoundationStoneBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     *
     */
    public void tickServer() {
        if (getLevel().getGameTime() > getExpireTime()) {
            // remove border
            removeParcelBorder(getLevel(), getCoords());
            // self destruct
            selfDestruct();
        }
    }

    /**
     *
     */
//    private void selfDestruct() {
//        ClaimMyLand.LOGGER.debug("self-destructing @ {}", this.getBlockPos());
//        this.getLevel().setBlock(this.getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
//        this.getLevel().removeBlockEntity(this.getBlockPos());
//    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        if (ObjectUtils.isNotEmpty(getDeedId())) {
            tag.putUUID(DEED_ID, getDeedId());
        }

        if (ObjectUtils.isNotEmpty(getNationId())) {
            tag.putUUID(NATION_ID, getNationId());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.contains(DEED_ID)) {
            setDeedId(tag.getUUID(DEED_ID));
        }
        if (tag.contains(NATION_ID)) {
            setNationId(tag.getUUID(NATION_ID));
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

    public UUID getDeedId() {
        return deedId;
    }
    public void setDeedId(UUID deedId) {
        this.deedId = deedId;
    }

    public UUID getNationId() {
        return nationId;
    }
    public void setNationId(UUID nationId) {
        this.nationId = nationId;
    }
}
