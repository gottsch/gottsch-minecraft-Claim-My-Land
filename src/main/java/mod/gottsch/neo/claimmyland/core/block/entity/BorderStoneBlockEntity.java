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
package mod.gottsch.neo.claimmyland.core.block.entity;

import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.config.Config;
import mod.gottsch.neo.claimmyland.core.item.Deed;
import mod.gottsch.neo.claimmyland.core.network.CMLNetwork;
import mod.gottsch.neo.claimmyland.core.parcel.Parcel;
import mod.gottsch.neo.claimmyland.core.parcel.ParcelType;
import mod.gottsch.neo.claimmyland.core.registry.ActiveBorderStoneRegistry;
import mod.gottsch.neo.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.neo.gottschcore.spatial.Box;
import mod.gottsch.neo.gottschcore.spatial.Coords;
import mod.gottsch.neo.gottschcore.spatial.ICoords;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Mark Gottschling on Sep 18, 2024
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

    @Deprecated
    private long expireTime;

    // transient — not saved to NBT, only used during placement
    @Nullable
    private ServerPlayer placingPlayer;

    public BorderStoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BORDER_STONE_ENTITY_TYPE.get(), pos, state);
    }

    public BorderStoneBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void tickServer() {
//        // expireTime == 0 means it was never initialized — skip until populateBlockEntity sets it
//        if (getExpireTime() == 0) return;
//
//        if (getLevel().getGameTime() > getExpireTime()) {
//            if (getLevel() instanceof ServerLevel serverLevel && getParcelId() != null) {
//                Optional<Parcel> parcel = ParcelRegistry.findByParcelId(getParcelId());
//                if (parcel.isPresent()) {
//                    // committed parcel — hide the visual border
//                    CMLNetwork.syncBorderVisibilityToTrackingPlayers(
//                            serverLevel, parcel.get(), false, 0, getBlockPos().getY());
//                    ACTIVE_BORDER_STONES.remove(this);
//                } else {
//                    // phase 1 preview — parcel never committed; remove from client registries
//                    CMLNetwork.removePreviewParcelFromTracking(serverLevel, getParcelId(), getBlockPos());
//                }
//            }
//            selfDestruct();
//        }
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
            default -> 0;
        };
    }

    /**
     * gets an absolute box at a coords using the block entity
     * @return
     */
    public Box getAbsoluteBox(Parcel parcel) {
        return parcel.getBox();
    }

//    public Box getAbsoluteBox() {
//        if (getParcelId() != null) {
//            String dimension = level.dimension().location().toString();
//            Optional<Parcel> parcel = ParcelRegistry.findByParcelId(getParcelId());
//            if (parcel.isPresent()) {
//                return parcel.get().getBox();
//            }
//        }
//        // phase 1 preview — foundation stone IS the origin
//        ICoords myCoords = Coords.of(this.worldPosition);
//        return new Box(myCoords.add(getRelativeBox().getMinCoords()),
//                myCoords.add(getRelativeBox().getMaxCoords()));
//    }
public Box getAbsoluteBox() {
    if (relativeBox == null || coords == null) return null;
    if (Config.SERVER.general.foundationStoneCentered.get()) {
        int sizeX = relativeBox.getMaxCoords().getX() - relativeBox.getMinCoords().getX() + 1;
        int sizeZ = relativeBox.getMaxCoords().getZ() - relativeBox.getMinCoords().getZ() + 1;
        int halfX = sizeX / 2;
        int halfZ = sizeZ / 2;
        ICoords min = Coords.of(
                coords.getX() - halfX,
                coords.getY() + relativeBox.getMinCoords().getY(),
                coords.getZ() - halfZ);
        ICoords max = Coords.of(
                coords.getX() - halfX + sizeX - 1,
                coords.getY() + relativeBox.getMaxCoords().getY(),
                coords.getZ() - halfZ + sizeZ - 1);
        return new Box(min, max);
    }
    // legacy: stone is min corner
    return new Box(
            coords.withY(coords.getY() + relativeBox.getMinCoords().getY())
                    .add(relativeBox.getMinCoords().withY(0)),
            coords.withY(coords.getY() + relativeBox.getMaxCoords().getY())
                    .add(relativeBox.getMaxCoords().withY(0)));
}

    /**
     * gets an absolute box at a given coords using the parcel box
     *
     * @param coords
     * @return
     */
//    public Box getAbsoluteBox(ICoords coords) {
//        return new Box(coords.add(getRelativeBox().getMinCoords()),
//                coords.add(getRelativeBox().getMaxCoords()));
//    }

    /**
     * Sends border visibility to clients. All parcels use the visual renderer —
     * no physical blocks are placed.
     * @author Mark Gottschling on Mar 11, 2026
     */
    public void placeParcelBorder(ServerPlayer placingPlayer) {
//        ClaimMyLand.LOGGER.debug("parcel id -> {}", getParcelId());
        if (!(level instanceof ServerLevel serverLevel) || getParcelId() == null) return;

        Optional<Parcel> parcel = ParcelRegistry.findByParcelId(getParcelId());
        Box absoluteBox = parcel.isPresent() ? getAbsoluteBox(parcel.get()) : getAbsoluteBox();
//        UUID ownerId = parcel.isPresent()
//                ? parcel.get().getEstate().getOwnerId()
//                : (placingPlayer != null ? placingPlayer.getUUID() : getOwnerId());

        // resolve the effective owner for border visibility:
        // reclaiming player takes precedence over the registered estate owner when they differ
        UUID ownerId = parcel.isPresent()
                ? (parcel.get().getEstate().isRelinquished() && placingPlayer != null
                ? placingPlayer.getUUID()
                : parcel.get().getEstate().getOwnerId())
                : (placingPlayer != null ? placingPlayer.getUUID() : getOwnerId());

//        ClaimMyLand.LOGGER.debug("ownerId -> {}", String.valueOf(ownerId));

        String dimension = level.dimension().location().toString();
        int conflictState = ParcelRegistry.resolveConflictState(absoluteBox, ownerId, parcel.isPresent() ? getParcelId() : null,
                parcel.map(Parcel::getType).orElse(ParcelType.fromString(getParcelType())), dimension);

//        ClaimMyLand.LOGGER.debug("placeParcelBorder: parcelId={}, parcelPresent={}, conflictState={}, stoneY={}, player={}",
//                getParcelId(), parcel.isPresent(), conflictState, getBlockPos().getY(),
//                placingPlayer != null ? placingPlayer.getName().getString() : "null");

        if (parcel.isPresent()) {
            if (placingPlayer != null) {
//                ClaimMyLand.LOGGER.debug("syncBorderVisibilityToTrakcingPlayersAndSelf...");
                CMLNetwork.syncBorderVisibilityToTrackingPlayersAndSelf(
                        serverLevel, placingPlayer, parcel.get(), conflictState, getBlockPos().getY());
            } else {
//                ClaimMyLand.LOGGER.debug("syncBorderVisibilityToTrackingPlayers...");
                CMLNetwork.syncBorderVisibilityToTrackingPlayers(
                        serverLevel, parcel.get(), conflictState, getBlockPos().getY());
            }
            ActiveBorderStoneRegistry.add(this);
        } else if (placingPlayer != null) {
//            ClaimMyLand.LOGGER.debug("syncPreviewParcelToTrackingPlayersAndSelf...");
            // phase 1 preview — parcel not yet registered; register on client first
//            CMLNetwork.syncPreviewParcelToTrackingPlayersAndSelf(
            CMLNetwork.syncPreviewParcelToOwner(
                    serverLevel, placingPlayer.getUUID(),
                    getParcelId(), getParcelId(),   // estateId = parcelId (throwaway for preview)
                    ParcelType.fromString(getParcelType()),
                    absoluteBox, getBlockPos().getY(),
                    dimension, conflictState);
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

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
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

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
//            ClaimMyLand.LOGGER.warn("size of parcel was not found. using default size.");
        }
        if (tag.contains(EXPIRE_TIME)) {
            setExpireTime(tag.getLong(EXPIRE_TIME));
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (!(level instanceof ServerLevel serverLevel) || getParcelId() == null) return;
        // Guard: if the block entity is already marked for removal, do nothing.
        // This prevents a spurious isBorderVisible=true send when the Foundation
        // Stone is removed immediately after a successful claim commit.
        if (isRemoved()) return;

        Optional<Parcel> parcel = ParcelRegistry.findByParcelId(getParcelId());

        if (parcel.isPresent()) {
            // Committed parcel — restore border visibility to owner (and placer if different).
            ActiveBorderStoneRegistry.add(this);

            if (this instanceof FoundationStoneBlockEntity foundationStoneBlockEntity
                    && foundationStoneBlockEntity.getPlacingPlayerId() != null) {
                CMLNetwork.syncBorderVisibleToOwnerAndPlacer(
                        serverLevel, parcel.get(), getBlockPos().getY(), 0,
                        foundationStoneBlockEntity.getPlacingPlayerId());
            } else {
                CMLNetwork.syncBorderVisibleToOwner(
                        serverLevel, parcel.get(), getBlockPos().getY(), 0);
            }

        } else if (this instanceof FoundationStoneBlockEntity fsbe) {
            // Preview Foundation Stone — no committed parcel yet, re-sync preview to owner.
            Box absoluteBox = fsbe.getAbsoluteBox();
            if (absoluteBox != null && getOwnerId() != null) {
                String dimension = serverLevel.dimension().location().toString();
                int conflictState = ParcelRegistry.resolveConflictState(
                        absoluteBox, getOwnerId(), null,
                        ParcelType.fromString(getParcelType()), dimension);
                CMLNetwork.syncPreviewParcelToOwner(
                        serverLevel,
                        getOwnerId(),
                        getParcelId(), getParcelId(),
                        ParcelType.fromString(getParcelType()),
                        absoluteBox, getBlockPos().getY(),
                        dimension, conflictState);
            }
        }
    }

    /**
     * Sync client and server states
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag != null) {
            loadAdditional(tag, registries);
        }
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        handleUpdateTag(tag, registries);
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
    @Deprecated
    public long getExpireTime() {
        return expireTime;
    }
    @Deprecated
    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    @Nullable
    public ServerPlayer getPlacingPlayer() {
        return placingPlayer;
    }

    public void setPlacingPlayer(@Nullable ServerPlayer placingPlayer) {
        this.placingPlayer = placingPlayer;
    }
}
