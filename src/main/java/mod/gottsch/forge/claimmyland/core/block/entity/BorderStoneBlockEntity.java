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
import mod.gottsch.forge.claimmyland.core.block.*;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.item.Deed;
import mod.gottsch.forge.claimmyland.core.parcel.NationParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.block.FacingBlock;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Mark Gottschling on Sep 18, 2024
 */
public class BorderStoneBlockEntity extends BlockEntity {
    private static final String PARCEL_ID = "parcel_id";
    private static final String OWNER_ID = "owner_id";
    private static final String PARCEL_TYPE = "parcel_type";
    private static final String COORDS = "coords";
    private static final String EXPIRE_TIME = "expire_time";

    private static final int TICKS_PER_SECOND = 20;
    private static final int FIVE_SECONDS = 5 * TICKS_PER_SECOND;
    private static final int ONE_MINUTE = 60 * TICKS_PER_SECOND;
    private static final int FIVE_MINUTES = 5 * ONE_MINUTE;

    // TODO rename RELATIVE_BOX
    private static final String SIZE = "size";

    // unique id of the parcel this block entity represents
    private UUID parcelId;

    // unique id of the owner
    private UUID ownerId;

    // type of parcel this block entity represents
    private String parcelType;

    // starting/min coords
    private ICoords coords;
    /*
     * relative box coords around (0, 0, 0)
     * ie a size of (0, -5, 0) -> (5, 5, 5) = (5, 11, 5).
     * when foundation stone is at (1, 1, 1), then the box
     * is (1, -4, 1) -> (6, 6, 6).
     */
    private Box relativeBox;
    //
    private long expireTime;

    public BorderStoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BORDER_STONE_ENTITY_TYPE.get(), pos, state);
    }

    public BorderStoneBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     *
     */
    public void tickServer() {
        // refresh the borders
        if (getLevel().getGameTime() % Config.SERVER.borders.ticksPerBorderStoneRefresh.get() == 0) {
            placeParcelBorder();
        }

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
    private void selfDestruct() {
        ClaimMyLand.LOGGER.debug("self-destructing @ {}", this.getBlockPos());
        this.getLevel().setBlock(this.getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
        this.getLevel().removeBlockEntity(this.getBlockPos());
    }

    /**
     *
     * @return
     */
    public Block getBorderBlock() {
        // default return personal border.
        // override method to return specific borders
//        return ((BorderStone)getBlockState().getBlock()).getBorderBlock();
//        return ModBlocks.PERSONAL_BORDER.get();

        ParcelType parcelType = getParcelType() != null ? ParcelType.valueOf(getParcelType()) : ParcelType.PLAYER;
        return switch (parcelType) {
            case PLAYER -> ModBlocks.PLAYER_BORDER.get();
            case NATION -> ModBlocks.NATION_BORDER.get();
            case CITIZEN, ZONE -> ModBlocks.CITIZEN_BORDER.get();
        };
    }

    public int getBufferSize(String type) {
        ParcelType parcelType = StringUtils.isNotBlank(type) ? ParcelType.valueOf(type) : ParcelType.PLAYER;
        return getBufferSize(parcelType);
    }

    /**
     * get the size of the buffer radius for the parcel type
     * @return
     */
    public int getBufferSize(ParcelType parcelType) {
//        ParcelType parcelType = getParcelType() != null ? ParcelType.valueOf(getParcelType()) : ParcelType.PLAYER;
        return switch (parcelType) {
            case PLAYER, CITIZEN -> Config.SERVER.general.parcelBufferRadius.get();
            case NATION -> Config.SERVER.general.nationParcelBufferRadius.get();
            case ZONE -> 0;
        };
    }

    /**
     * thus there is no need for check against overlaps etc.
     * @param box
     * @return
     */
    protected BlockState getBorderBlockState(Box box) {
        return getBorderBlock().defaultBlockState().setValue(BorderBlock.INTERSECTS, BorderStatus.GOOD);
    }

    /**
     *
     * @param box
     * @param bufferedBox
     * @return
     */
    // determines what state the buffer block is
    protected BlockState getBufferBlockState(Box box, Box bufferedBox) {
        return ModBlocks.BUFFER.get().defaultBlockState().setValue(BorderBlock.INTERSECTS, BorderStatus.GOOD);
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
     * TODO should this be pushed to the concrete classes?
     * get the absolute box for display purposes.
     * nation parcel have special rules and only display 10 blocks in either y direction,
     * because a nation parcel has max y values.
     * @param coords
     * @return
     */
    public Box getBorderDisplayBox(ICoords coords) {
        ParcelType parcelType = getParcelType() != null ? ParcelType.valueOf(getParcelType()) : ParcelType.PLAYER;

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

    /*
     * border cud operations
     */
    public void placeParcelBorder() {
        Level level = getLevel();
        Optional<Parcel> parcel = ParcelRegistry.findByParcelId(getParcelId());

        ICoords coords;
        int bufferRadius = 1;
        if (parcel.isPresent()) {
//            ClaimMyLand.LOGGER.debug("place parcel border, parcel by id -> {}", parcel.get());
            coords = parcel.get().getCoords();
            if (parcel.get() instanceof NationParcel) {
                coords = coords.withY(getBlockPos().getY());
            }
            bufferRadius = parcel.get().getBufferSize();
        } else {
            coords = new Coords(this.getBlockPos());
            bufferRadius = getBufferSize(getParcelType());
        }
//        ClaimMyLand.LOGGER.debug("using coords for outlines -> {}", coords);
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
    * intended to replace world block (ex Air) with a BorderBlock
     * whose blockState will be modified for the specific position it is in.
     * therefor the BlockState must be of a Border/BufferBlock with only the
     * INTERSECTS state value set. the others (FACING, POSITION) will be set here.
     * TODO could possibly extends BlockState (BorderBlockState) so that the method
     * signature is specific to a BorderBlockState
     * @param box
     * @param removeBlock
     * @param intersectsBlockState
     */
    private void addParcelBorder(Box box, Block removeBlock, BlockState intersectsBlockState) {
        /* NOTE the for loops.
         * for x is "<=" because the Box was reduced by 1 during creation to ensure
         * it is the right size when including the origin.
         * thus y & z are "<" because we are iterating 2 less (1 on each side) because
         * the border is already generated by the x for loop.
         */

        // only iterate over the outline coords
        for (int x = 0; x < ModUtil.getSize(box).getX(); x++) {
            // north, bottom
            BlockPos pos = box.getMinCoords().toPos().offset(x, 0, 0);
            replaceParcelBorderBlock(level, pos, removeBlock, intersectsBlockState.setValue(FacingBlock.FACING, Direction.NORTH).setValue(BorderBlock.POSITION, BorderPosition.BOTTOM));

            // north, top
            BlockPos pos2 = pos.offset(0, ModUtil.getSize(box).getY()-1, 0);
            replaceParcelBorderBlock(level, pos2, removeBlock, intersectsBlockState.setValue(FacingBlock.FACING, Direction.NORTH).setValue(BorderBlock.POSITION, BorderPosition.TOP));

            // south, bottom
            BlockPos pos3 = pos.offset(0, 0, ModUtil.getSize(box).getZ()-1);
            replaceParcelBorderBlock(level, pos3, removeBlock, intersectsBlockState.setValue(FacingBlock.FACING, Direction.SOUTH).setValue(BorderBlock.POSITION, BorderPosition.BOTTOM));

            // south, top
            BlockPos pos4 = pos.offset(0, ModUtil.getSize(box).getY()-1, ModUtil.getSize(box).getZ()-1);
            replaceParcelBorderBlock(level, pos4, removeBlock, intersectsBlockState.setValue(FacingBlock.FACING, Direction.SOUTH).setValue(BorderBlock.POSITION, BorderPosition.TOP));
        }

        for (int z = 1; z < ModUtil.getSize(box).getZ(); z++) {
            BlockPos pos = box.getMinCoords().toPos().offset(0, 0, z);
            replaceParcelBorderBlock(level, pos, removeBlock, intersectsBlockState.setValue(FacingBlock.FACING, Direction.WEST).setValue(BorderBlock.POSITION, BorderPosition.BOTTOM));

            BlockPos pos2 = pos.offset(0, ModUtil.getSize(box).getY()-1, 0);
            replaceParcelBorderBlock(level, pos2, removeBlock, intersectsBlockState.setValue(FacingBlock.FACING, Direction.WEST).setValue(BorderBlock.POSITION, BorderPosition.TOP));

            BlockPos pos3 = pos.offset(ModUtil.getSize(box).getX()-1, 0, 0);
            replaceParcelBorderBlock(level, pos3, removeBlock, intersectsBlockState.setValue(FacingBlock.FACING, Direction.EAST).setValue(BorderBlock.POSITION, BorderPosition.BOTTOM));

            BlockPos pos4 = pos.offset(ModUtil.getSize(box).getX()-1, ModUtil.getSize(box).getY()-1, 0);
            replaceParcelBorderBlock(level, pos4, removeBlock, intersectsBlockState.setValue(FacingBlock.FACING, Direction.EAST).setValue(BorderBlock.POSITION, BorderPosition.TOP));
        }

        // vertical edges
        for (int y = 1; y < ModUtil.getSize(box).getY(); y++) {
            BlockPos pos = box.getMinCoords().toPos().offset(0, y, 0);
            replaceParcelBorderBlock(level, pos, removeBlock, intersectsBlockState.setValue(FacingBlock.FACING, Direction.NORTH).setValue(BorderBlock.POSITION, BorderPosition.LEFT));

            BlockPos pos2 = pos.offset(ModUtil.getSize(box).getX()-1, 0, 0);
            replaceParcelBorderBlock(level, pos2, removeBlock, intersectsBlockState.setValue(FacingBlock.FACING, Direction.NORTH).setValue(BorderBlock.POSITION, BorderPosition.RIGHT));

            BlockPos pos3 = pos.offset(0, 0, ModUtil.getSize(box).getZ()-1);
            replaceParcelBorderBlock(level, pos3, removeBlock, intersectsBlockState.setValue(FacingBlock.FACING, Direction.SOUTH).setValue(BorderBlock.POSITION, BorderPosition.RIGHT));

            BlockPos pos4 = pos.offset(ModUtil.getSize(box).getX()-1, 0, ModUtil.getSize(box).getZ()-1);
            replaceParcelBorderBlock(level, pos4, removeBlock, intersectsBlockState.setValue(FacingBlock.FACING, Direction.SOUTH ).setValue(BorderBlock.POSITION, BorderPosition.LEFT));
        }

        // NOTE there is a mismatch of _LEFT | _RIGHT positions depending on your perspective.
        // the Left currently is used in the Right world position ie at point (x, y, z) facing south (you as an observer of the outline), and vice versa
        // the models are backwards as well.
        // however, if you ARE the outline, then the positions align to the correct directions.

        // corners
        BlockPos pos = box.getMinCoords().toPos().offset(0, ModUtil.getSize(box).getY()-1, 0);
        replaceParcelBorderBlock(level, pos, removeBlock, intersectsBlockState.setValue(FacingBlock.FACING, Direction.NORTH).setValue(BorderBlock.POSITION, BorderPosition.TOP_LEFT));

        pos = box.getMinCoords().toPos().offset(0, 0, 0);
        replaceParcelBorderBlock(level, pos, removeBlock, intersectsBlockState.setValue(FacingBlock.FACING, Direction.NORTH).setValue(BorderBlock.POSITION, BorderPosition.BOTTOM_LEFT));

        pos = box.getMinCoords().toPos().offset(ModUtil.getSize(box).getX()-1, ModUtil.getSize(box).getY()-1, 0);
        replaceParcelBorderBlock(level, pos, removeBlock, intersectsBlockState.setValue(FacingBlock.FACING, Direction.NORTH).setValue(BorderBlock.POSITION, BorderPosition.TOP_RIGHT));

        pos = box.getMinCoords().toPos().offset(ModUtil.getSize(box).getX()-1, 0, 0);
        replaceParcelBorderBlock(level, pos, removeBlock, intersectsBlockState.setValue(FacingBlock.FACING, Direction.NORTH).setValue(BorderBlock.POSITION, BorderPosition.BOTTOM_RIGHT));

        // south corners
        pos = box.getMinCoords().toPos().offset(0, ModUtil.getSize(box).getY()-1, ModUtil.getSize(box).getZ()-1);
        replaceParcelBorderBlock(level, pos, removeBlock, intersectsBlockState.setValue(FacingBlock.FACING, Direction.SOUTH).setValue(BorderBlock.POSITION, BorderPosition.TOP_RIGHT));

        pos = box.getMinCoords().toPos().offset(0, 0, ModUtil.getSize(box).getZ()-1);
        replaceParcelBorderBlock(level, pos, removeBlock, intersectsBlockState.setValue(FacingBlock.FACING, Direction.SOUTH).setValue(BorderBlock.POSITION, BorderPosition.BOTTOM_RIGHT));

        pos = box.getMinCoords().toPos().offset(ModUtil.getSize(box).getX()-1, ModUtil.getSize(box).getY()-1, ModUtil.getSize(box).getZ()-1);
        replaceParcelBorderBlock(level, pos, removeBlock, intersectsBlockState.setValue(FacingBlock.FACING, Direction.SOUTH).setValue(BorderBlock.POSITION, BorderPosition.TOP_LEFT));

        pos = box.getMinCoords().toPos().offset(ModUtil.getSize(box).getX()-1, 0, ModUtil.getSize(box).getZ()-1);
        replaceParcelBorderBlock(level, pos, removeBlock, intersectsBlockState.setValue(FacingBlock.FACING, Direction.SOUTH).setValue(BorderBlock.POSITION, BorderPosition.BOTTOM_LEFT));

    }

    /**
     *
     * @param level
     * @param pos
     * @param removeBlock
     * @param newState
     */
    private static void replaceParcelBorderBlock(Level level, BlockPos pos, Block removeBlock, BlockState newState) {
        BlockState borderState = level.getBlockState(pos);
        if ((borderState instanceof IBorderBlock) || borderState.is(removeBlock) || borderState.canBeReplaced()) {
            level.setBlockAndUpdate(pos, newState);
        }
    }

    /**
     * this is intended to remove any BorderBlocks with another Block
     * that only uses a pre-setup blockState.
     * @param box
     * @param removeBlock
     * @param blockState
     */
    private static void replaceParcelBorder(Level level, Box box, Block removeBlock, BlockState blockState) {
        // only iterate over the outline coords
        for (int x = 0; x < ModUtil.getSize(box).getX(); x++) {
            BlockPos pos = box.getMinCoords().toPos().offset(x, 0, 0);
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
        removeParcelBorder(getLevel(), coords);
    }

    public void removeParcelBorder(Level level, ICoords coords) {
        Box box = getBorderDisplayBox(coords);
        replaceParcelBorder(level, box, getBorderBlock(), Blocks.AIR.defaultBlockState());
        box = ModUtil.inflate(box, getBufferSize(getParcelType()));
        replaceParcelBorder(level, box, ModBlocks.BUFFER.get(), Blocks.AIR.defaultBlockState());
    }

    /**
     * static variant where all values are provided
     */
    public static void removeParcelBorder(Level level, Box box, Block borderBlock, int bufferSize) {
        replaceParcelBorder(level, box, borderBlock, Blocks.AIR.defaultBlockState());
        box = ModUtil.inflate(box, bufferSize);
        replaceParcelBorder(level, box, ModBlocks.BUFFER.get(), Blocks.AIR.defaultBlockState());
    }

//    // TODO move to Parcel
//    // TODO this is a dangerous call.
//    // the intent to to take a non-buffered parcel box and test against the buffered list
//    // if overlaps with a buffered parcel and not owner by the same owner, then fail
//    // HOWEVER one could pass in an non-buffered list and if owned by the same person,
//    // you could have overlapping parcels, essentially reducing the size of one parcel.
//    // TODO SOLUTION to make this work, the list should not be passed in, but performed with the method.
//    // however then you would not be able to perform any filters or anything.
//    // TODO 9-18-2024 SOLUTION use as intended with a proper method name
//    // and add method comments to indicate what is expected
//    public boolean hasBoxToBufferedIntersections(Box box, List<Parcel> parcels) {
//        for (Parcel overlapParcel : parcels) {
//            /*
//             * if parcel of foundation stone has same owner as parcel in world, ignore buffers,
//             * but check border overlaps. parcels owned by the same player can be touching.
//             */
//            if (getOwnerId().equals(overlapParcel.getOwnerId())) {
//                // get the existing owned parcel
//                Optional<Parcel> optionalOwnedParcel = ParcelRegistry.findByParcelId(overlapParcel.getId());
//
//                // test if the parcels intersect
//                if (optionalOwnedParcel.isPresent() && ModUtil.touching(box, optionalOwnedParcel.get().getBox())) {
//                    return true;
//                }
//            } else {
//                return true;
//            }
//        }
//        return false;
//    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        if (ObjectUtils.isNotEmpty(getParcelId())) {
            tag.putUUID(PARCEL_ID, getParcelId());
        }

        if (ObjectUtils.isNotEmpty(getOwnerId())) {
            tag.putUUID(OWNER_ID, getOwnerId());
        }

        if (StringUtils.isNotBlank(getParcelType())) {
            tag.putString(PARCEL_TYPE, getParcelType());
        }

        if (ObjectUtils.isNotEmpty(getCoords())) {
            CompoundTag coordsTag = new CompoundTag();
            getCoords().save(coordsTag);
            tag.put(COORDS, coordsTag);
        }

        if (ObjectUtils.isNotEmpty(getRelativeBox())) {
            CompoundTag sizeTag = new CompoundTag();
            getRelativeBox().save(sizeTag);
            tag.put(SIZE, sizeTag); // TODO rename SIZE to RELATIVE_BOX
        }

        tag.putLong(EXPIRE_TIME, getExpireTime());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.contains(PARCEL_ID)) {
            setParcelId(tag.getUUID(PARCEL_ID));
        }
        if (tag.contains(OWNER_ID)) {
            setOwnerId(tag.getUUID(OWNER_ID));
        }
        if (tag.contains(PARCEL_TYPE)) {
            setParcelType(tag.getString(PARCEL_TYPE));
        }
        if (tag.contains(COORDS)) {
            setCoords(Coords.EMPTY.load((CompoundTag) tag.get(COORDS)));
        }
        if (tag.contains(SIZE)) {
            setRelativeBox(Box.load(tag.getCompound(SIZE)));
        } else {
            setRelativeBox(Deed.DEFAULT_SIZE);
            ClaimMyLand.LOGGER.warn("size of parcel was not found. using default size.");
        }
        if (tag.contains(EXPIRE_TIME)) {
            setExpireTime(tag.getLong(EXPIRE_TIME));
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

    public UUID getParcelId() {
        return parcelId;
    }
    public void setParcelId(UUID parcelId) {
        this.parcelId = parcelId;
    }
    public UUID getOwnerId() {
        return ownerId;
    }
    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }
    public String getParcelType() {
        return parcelType;
    }
    public void setParcelType(String parcelType) {
        this.parcelType = parcelType;
    }
    public ICoords getCoords() {
        return coords;
    }
    public void setCoords(ICoords coords) {
        this.coords = coords;
    }
    public Box getRelativeBox() {
        return relativeBox;
    }
    public void setRelativeBox(Box relativeBox) {
        this.relativeBox = relativeBox;
    }
    public long getExpireTime() {
        return expireTime;
    }
    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }
}
