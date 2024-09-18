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
import mod.gottsch.forge.claimmyland.core.block.IBorderBlock;
import mod.gottsch.forge.claimmyland.core.block.ModBlocks;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.item.Deed;
import mod.gottsch.forge.claimmyland.core.parcel.NationParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Create by Mark Gottschling on Sep 14, 2024
 */
public abstract class FoundationStoneBlockEntity extends BlockEntity {
    private static final String PARCEL_ID = "parcel_id";
    private static final String OWNER_ID = "owner_id";
    private static final String DEED_ID = "deed_id";
    private static final String NATION_ID = "nation_id";
    private static final String PARCEL_TYPE = "parcel_type";
    private static final String COORDS = "coords";
    private static final String SIZE = "size";
    private static final String OVERLAPS = "overlaps";
    private static final String EXPIRE_TIME = "expire_time";
    private static final String HAS_PARCEL = "has_parcel";

    private static final int TICKS_PER_SECOND = 20;
    private static final int FIVE_SECONDS = 5 * TICKS_PER_SECOND;
    private static final int ONE_MINUTE = 60 * TICKS_PER_SECOND;
    private static final int FIVE_MINUTES = 5 * ONE_MINUTE;

    /*
     * relative box coords around (0, 0, 0)
     * ie a size of (0, -5, 0) -> (5, 5, 5) = (5, 11, 5).
     * when foundation stone is at (1, 1, 1), then the box
     * is (1, -4, 1) -> (6, 6, 6).
     */
    private Box relativeBox;

    // unique id of the parcel this block entity represents
    private UUID parcelId;

    // unique id of the owner
    private UUID ownerId;

    // unique id of of deed that created this block / block entity
    // TODO might not be necessary when adding TransferDeed
    private UUID deedId;

    // unique id of the nation this parcel belong to
    private UUID nationId;

    // type of parcel this block entity represents
    private String parcelType;

    // starting/min coords
    private ICoords coords;

    // transient list of overlapping parcel boxes
    private List<Box> overlaps;

    // time when this block entity expires
    private long expireTime;

    // flag if this block entity has an existing parcel ie this was placed to display borders of a parcel
    private boolean hasParcel;

    public FoundationStoneBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
//        super(ModBlockEntities.FOUNDATION_STONE_ENTITY_TYPE.get(), pos, state);
        super(type, pos, state);
    }

    /**
     * gets an absolute box at a coords using the block entity
     * @return
     */
    public Box getAbsoluteBox() {
        ICoords myCoords = Coords.of(this.worldPosition);
        return new Box(myCoords.add(getRelativeBox().getMinCoords()),
                myCoords.add(getRelativeBox().getMaxCoords()));
    }

    /**
     * gets an absolute box at a given coords using the parcel box
     *
     * @param coords
     * @return
     */
    public Box getAbsoluteBox(ICoords coords) {
        return new Box(coords.add(getRelativeBox().getMinCoords()),
                coords.add(getRelativeBox().getMaxCoords()));
    }

    /**
     * get the absolute box for display purposes.
     * nation parcel have special rules and only display 10 blocks in either y direction,
     * because a nation parcel has max y values.
     * @param coords
     * @return
     */
    public Box getBorderDisplayBox(ICoords coords) {

        ParcelType parcelType = getParcelType() != null ? ParcelType.valueOf(getParcelType()) : ParcelType.PERSONAL;

        Box box;
        // check for nation block and make the box only +/-10 in height
        if (parcelType == ParcelType.NATION) {
            box = new Box(coords.add(getRelativeBox().getMinCoords().withY(-10)),
                    coords.add(getRelativeBox().getMaxCoords().withY(9))); // 10-1
        }
        else {
            box = getAbsoluteBox(coords);
        }
        return box;
    }

    /**
     * get the size of the buffer radius for the parcel type
     * @return
     */
    public int getBufferSize() {
        ParcelType parcelType = getParcelType() != null ? ParcelType.valueOf(getParcelType()) : ParcelType.PERSONAL;
        return switch (parcelType) {
            case PERSONAL -> Config.SERVER.general.parcelBufferRadius.get();
            case NATION -> Config.SERVER.general.nationParcelBufferRadius.get();
            case CITIZEN, CITIZEN_CLAIM_ZONE -> 0;
        };
    }

    /*
     * border cud operations
     */
    public void placeParcelBorder() {
        Level level = getLevel();
        Optional<Parcel> parcel = ParcelRegistry.findByParcelId(getParcelId());

        ICoords coords;
        int bufferRadius = 1;
        if (parcel.isPresent()) {
            ClaimMyLand.LOGGER.debug("place parcel border, parcel by id -> {}", parcel.get());
            coords = parcel.get().getCoords();
            if (parcel.get() instanceof NationParcel) {
                coords = coords.withY(getBlockPos().getY());
            }
            bufferRadius = parcel.get().getBufferSize();
        } else {
            coords = new Coords(this.getBlockPos());
            bufferRadius = getBufferSize();
        }
        ClaimMyLand.LOGGER.debug("using coords for outlines -> {}", coords);
        // add the border
        Box box = getBorderDisplayBox(coords);
        BlockState borderState = getBorderBlockState(box);
        placeParcelBorder(box, borderState);

        // inflate the box
        Box bufferedBox = ModUtil.inflate(box, bufferRadius);
        BlockState bufferState = getBufferBlockState(box, bufferedBox);
        placeParcelBorder(bufferedBox, bufferState);
    }

    public void placeParcelBorder(Box box, BlockState state) {
        // TODO AIR should be a tag and can replace air, water, and BorderBlocks
        addParcelBorder(box, Blocks.AIR, state);
    }

    /**
     *
     * @param box
     * @param removeBlock
     * @param blockState
     */
    private void addParcelBorder(Box box, Block removeBlock, BlockState blockState) {
        /* NOTE the for loops.
         * for x is "<=" because the Box was reduced by 1 during creation to ensure
         * it is the right size when including the origin.
         * thus y & z are "<" because we are iterating 2 less (1 on each side) because
         * the border is already generated by the x for loop.
         */
        // only iterate over the outline coords
        for (int x = 0; x < ModUtil.getSize(box).getX(); x++) {
            BlockPos pos = box.getMinCoords().toPos().offset(x, 0, 0);
            BlockState borderState = level.getBlockState(pos);
            replaceParcelBorderBlock(level, pos, removeBlock, blockState);

            BlockPos pos2 = pos.offset(0, ModUtil.getSize(box).getY()-1, 0);
            replaceParcelBorderBlock(level, pos2, removeBlock, blockState);

            BlockPos pos3 = pos.offset(0, 0, ModUtil.getSize(box).getZ()-1);
            replaceParcelBorderBlock(level, pos3, removeBlock, blockState);

            BlockPos pos4 = pos.offset(0, ModUtil.getSize(box).getY()-1, ModUtil.getSize(box).getZ()-1);
            replaceParcelBorderBlock(level, pos4, removeBlock, blockState);
        }

        for (int z = 1; z < ModUtil.getSize(box).getZ(); z++) {
            BlockPos pos = box.getMinCoords().toPos().offset(0, 0, z);
            BlockState borderState = level.getBlockState(pos);
            replaceParcelBorderBlock(level, pos, removeBlock, blockState);

            BlockPos pos2 = pos.offset(0, ModUtil.getSize(box).getY()-1, 0);
            replaceParcelBorderBlock(level, pos2, removeBlock, blockState);

            BlockPos pos3 = pos.offset(ModUtil.getSize(box).getX()-1, 0, 0);
            replaceParcelBorderBlock(level, pos3, removeBlock, blockState);

            BlockPos pos4 = pos.offset(ModUtil.getSize(box).getX()-1, ModUtil.getSize(box).getY()-1, 0);
            replaceParcelBorderBlock(level, pos4, removeBlock, blockState);
        }

        for (int y = 1; y < ModUtil.getSize(box).getY(); y++) {
            BlockPos pos = box.getMinCoords().toPos().offset(0, y, 0);
            BlockState borderState = level.getBlockState(pos);
            replaceParcelBorderBlock(level, pos, removeBlock, blockState);

            BlockPos pos2 = pos.offset(ModUtil.getSize(box).getX()-1, 0, 0);
            replaceParcelBorderBlock(level, pos2, removeBlock, blockState);

            BlockPos pos3 = pos.offset(0, 0, ModUtil.getSize(box).getZ()-1);
            replaceParcelBorderBlock(level, pos3, removeBlock, blockState);

            BlockPos pos4 = pos.offset(ModUtil.getSize(box).getX()-1, 0, ModUtil.getSize(box).getZ()-1);
            replaceParcelBorderBlock(level, pos4, removeBlock, blockState);
        }
    }

    private void replaceParcelBorderBlock(Level level, BlockPos pos, Block removeBlock, BlockState blockState) {
        BlockState borderState = level.getBlockState(pos);
        if ((borderState instanceof IBorderBlock) || borderState.is(removeBlock) || borderState.canBeReplaced()) {
            level.setBlockAndUpdate(pos, blockState);
        }
    }

    /**
     * removes the border blocks from the border coords
     */
    public void removeParcelBorder() {
        Level level = getLevel();
        Optional<Parcel> parcel = ParcelRegistry.findByParcelId(getParcelId());
        ICoords coords = new Coords(this.getBlockPos());
        if (parcel.isPresent()) {
            coords = new Coords(parcel.get().getCoords());
            if (parcel.get() instanceof NationParcel) {
                coords = coords.withY(getBlockPos().getY());
            }
        }
        removeParcelBorder(coords);
    }

    public void removeParcelBorder(ICoords coords) {
        Box box = getBorderDisplayBox(coords);
        BlockState borderState = getBorderBlockState(box);

        addParcelBorder(box, borderState.getBlock(), Blocks.AIR.defaultBlockState());
        box = ModUtil.inflate(box, getBufferSize());
        addParcelBorder(box, ModBlocks.BUFFER.get(), Blocks.AIR.defaultBlockState());
    }

    /**
     * @param box
     * @return
     */
    protected abstract BlockState getBorderBlockState(Box box);

    // determines what state the buffer block is
    protected abstract BlockState getBufferBlockState(Box box, Box bufferedBox);

    // TODO this is a dangerous call.
    // the intent to to take a non-buffered parcel box and test against the buffered list
    // if overlaps with a buffered parcel and not owner by the same owner, then fail
    // HOWEVER one could pass in an non-buffered list and if owned by the same person,
    // you could have overlapping parcels, essentially reducing the size of one parcel.
    // TODO SOLUTION to make this work, the list should not be passed in, but performed with the method.
    // however then you would not be able to perform any filters or anything.

    public boolean hasBoxToBufferedIntersections(Box box, List<Parcel> parcels) {
        for (Parcel overlapParcel : parcels) {
            /*
             * if parcel of foundation stone has same owner as parcel in world, ignore buffers,
             * but check border overlaps. parcels owned by the same player can be touching.
             */
            if (getOwnerId().equals(overlapParcel.getOwnerId())) {
                // get the existing owned parcel
                Optional<Parcel> optionalOwnedParcel = ParcelRegistry.findByParcelId(overlapParcel.getId());

                // test if the parcels intersect
                if (optionalOwnedParcel.isPresent() && ModUtil.intersects(box, optionalOwnedParcel.get().getBox())) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        if (ObjectUtils.isNotEmpty(getRelativeBox())) {
            CompoundTag sizeTag = new CompoundTag();
            getRelativeBox().save(sizeTag);
            tag.put(SIZE, sizeTag); // TODO rename SIZE to RELATIVE_BOX
        }

        if (ObjectUtils.isNotEmpty(getParcelId())) {
            tag.putUUID(PARCEL_ID, getParcelId());
        }

        if (ObjectUtils.isNotEmpty(getOwnerId())) {
            tag.putUUID(OWNER_ID, getOwnerId());
        }

        if (ObjectUtils.isNotEmpty(getDeedId())) {
            tag.putUUID(DEED_ID, getDeedId());
        }

        if (ObjectUtils.isNotEmpty(getNationId())) {
            tag.putUUID(NATION_ID, getNationId());
        }

        if (StringUtils.isNotBlank(getParcelType())) {
            tag.putString(PARCEL_TYPE, getParcelType());
        }

        if (ObjectUtils.isNotEmpty(getCoords())) {
            CompoundTag coordsTag = new CompoundTag();
            getCoords().save(coordsTag);
            tag.put(COORDS, coordsTag);
        }

        // TODO why are we saving this if it is transient
        ListTag list = new ListTag();
        getOverlaps().forEach(box -> {
            CompoundTag element = new CompoundTag();
            box.save(element);
            list.add(element);
        });
        tag.put(OVERLAPS, list);

        tag.putLong(EXPIRE_TIME, getExpireTime());

        tag.putBoolean(HAS_PARCEL, hasParcel());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.contains(SIZE)) {
            setRelativeBox(Box.load(tag.getCompound(SIZE)));
        } else {
            setRelativeBox(Deed.DEFAULT_SIZE);
            ClaimMyLand.LOGGER.warn("size of parcel was not found. using default size.");
        }

        if (tag.contains(PARCEL_ID)) {
            setParcelId(tag.getUUID(PARCEL_ID));
        }
        if (tag.contains(OWNER_ID)) {
            setOwnerId(tag.getUUID(OWNER_ID));
        }
        if (tag.contains(DEED_ID)) {
            setDeedId(tag.getUUID(DEED_ID));
        }
        if (tag.contains(NATION_ID)) {
            setNationId(tag.getUUID(NATION_ID));
        }
        if (tag.contains(PARCEL_TYPE)) {
            setParcelType(tag.getString(PARCEL_TYPE));
        }
        if (tag.contains(COORDS)) {
            setCoords(Coords.EMPTY.load((CompoundTag) tag.get(COORDS)));
        }

        // TODO why are we loading this if it is transient?
        getOverlaps().clear();
        if (tag.contains(OVERLAPS)) {
            ListTag list = tag.getList(OVERLAPS, Tag.TAG_COMPOUND);
            list.forEach(element -> {
                Box box = Box.load((CompoundTag)element);
                if (box != null) {
                    getOverlaps().add(box);
                }
            });
        }

        if (tag.contains(EXPIRE_TIME)) {
            setExpireTime(tag.getLong(EXPIRE_TIME));
        }

        if (tag.contains(HAS_PARCEL)) {
            setHasParcel(tag.getBoolean(HAS_PARCEL));
        }
    }

    /*
     * Get the render bounding box. Typical block is 1x1x1.
     */
//    @Override
//    public AABB getRenderBoundingBox() {
//        // always render regardless if TE is in FOV.
//        return INFINITE_EXTENT_AABB;
//    }

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

    public UUID getParcelId() {
        return parcelId;
    }
    public void setParcelId(UUID parcelId) {
        this.parcelId = parcelId;
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

    public String getParcelType() {
        return parcelType;
    }
    public void setParcelType(String parcelType) {
        this.parcelType = parcelType;
    }

    // TODO rename getRelativeBox()
    public Box getRelativeBox() {
        return relativeBox;
    }
    public void setRelativeBox(Box relativeBox) {
        this.relativeBox = relativeBox;
    }

    public UUID getOwnerId() {
        return ownerId;
    }
    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public ICoords getCoords() {
        return coords;
    }
    public void setCoords(ICoords coords) {
        this.coords = coords;
    }

    //    @Override
    public List<Box> getOverlaps() {
        if (overlaps == null) {
            overlaps = new ArrayList<>();
        }
        return overlaps;
    }
    public void setOverlaps(List<Box> overlaps) {
        this.overlaps = overlaps;
    }

    public long getExpireTime() {
        return expireTime;
    }
    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public boolean hasParcel() {
        return hasParcel;
    }
    public void setHasParcel(boolean hasParcel) {
        this.hasParcel = hasParcel;
    }
}
