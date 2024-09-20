package mod.gottsch.forge.claimmyland.core.block.entity;

import mod.gottsch.forge.claimmyland.core.block.BorderBlock;
import mod.gottsch.forge.claimmyland.core.block.BorderStatus;
import mod.gottsch.forge.claimmyland.core.block.BufferBlock;
import mod.gottsch.forge.claimmyland.core.block.ModBlocks;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.parcel.CitizenZoneParcel;
import mod.gottsch.forge.claimmyland.core.parcel.NationParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

/**
 * Create by Mark Gottschling on Sep 18, 2204
 */
public class PlayerFoundationStoneBlockEntity extends FoundationStoneBlockEntity {

    /**
     *
     * @param pos
     * @param state
     */
    public PlayerFoundationStoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLAYER_FOUNDATION_STONE_ENTITY_TYPE.get(), pos, state);
    }

    @Override
    public int getBufferSize() {
      return Config.SERVER.general.parcelBufferRadius.get();
    }

    @Override
    public Block getBorderBlock() {
        return ModBlocks.PLAYER_BORDER.get();
    }

    @Override
    protected BlockState getBorderBlockState(Box box) {
        // get the default block state of the border block
        BlockState blockState = ModBlocks.PLAYER_BORDER.get().defaultBlockState();

        /*
         * check if parcel is within another existing parcel
         */
        Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(box.getMinCoords());

        if (registryParcel.isEmpty()) {
            // find overlaps of the parcel with buffered registry parcels.
            // this ensure that the parcel boundaries are not overlapping the buffer area of another parcel
            List<Parcel> overlaps = ParcelRegistry.findBuffer(box);
            if (!overlaps.isEmpty()) {
                for (Parcel overlapParcel : overlaps) {
                    /*
                     * if parcel of foundation stone has same owner as parcel in world, ignore buffers,
                     * but check border overlaps. parcels owned by the same player can be touching.
                     */
                    if (getOwnerId().equals(overlapParcel.getOwnerId())) {
                        // get the existing owned parcel
                        Optional<Parcel> optionalOwnedParcel = ParcelRegistry.findByParcelId(overlapParcel.getId());

                        // test if the non-buffered parcels intersect
                        if (optionalOwnedParcel.isPresent() && ModUtil.intersects(box, optionalOwnedParcel.get().getBox())) {
                            blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
                            break;
                        }
                    } else {
                        blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
                        break;
                    }
                }
            }
        } else {
            // determine what parcel type is the foundation stone in
            if (registryParcel.get().getType() == ParcelType.CITIZEN) {
                // do nothing - uses good state
            } else if (registryParcel.get().getType() == ParcelType.NATION) {
                // NOTE this shouldn't be allowed in the first place due to placement rules.
                blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
            } else if (registryParcel.get().getType() == ParcelType.CITIZEN_ZONE) {
                // if any part of the parcel is outside the claim zone, then bad
                if (!ModUtil.contains(registryParcel.get().getBox(), box)) {
                    blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
                } else {
                    // proceed with a normal overlaps check, filtering out nation parcels
                    List<Parcel> overlaps = ParcelRegistry.findBuffer(box);
                    overlaps = overlaps.stream()
                            .filter(p -> !(p instanceof NationParcel || p instanceof CitizenZoneParcel))
                            .toList();

                     if(hasBoxToBufferedIntersections(box, overlaps)) {
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
        // get the default block state of the border block
        BlockState blockState = ModBlocks.BUFFER.get().defaultBlockState();

        /*
         * check if box/parcel is within another existing parcel
         */
        Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(box.getMinCoords());

        // not within another parcel
        if (registryParcel.isEmpty()) {

            // find overlaps of the buffered box with unbuffered parcels
            // this ensure that the buffered boundaries are not overlapping the area of another parcel - too close!
            // filter out the parcel that the buffer belongs to
            List<Parcel> overlaps = ParcelRegistry.find(bufferedBox).stream()
                    .filter(p -> !p.getId().equals(getParcelId())).toList();

            if (!overlaps.isEmpty()) {
                for (Parcel overlapParcel : overlaps) {
                    // the parcels are owned by the same person. they can be closer or touching,
                    // ie. ignore buffers, only the parcels themselves can't overlap
                    if (!getOwnerId().equals(overlapParcel.getOwnerId())) {
                        blockState = blockState.setValue(BufferBlock.INTERSECTS, BorderStatus.BAD);
                        break;
                    }
                }
            }
        } else {
            // determine what parcel type is the foundation stone in
            if (registryParcel.get().getType() == ParcelType.CITIZEN) {
                // do nothing - uses good state
            } else if (registryParcel.get().getType() == ParcelType.NATION) {
                // NOTE this shouldn't be allowed in the first place due to placement rules.
                blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
            } else if (registryParcel.get().getType() == ParcelType.CITIZEN_ZONE) {

                // preform a normal overlaps check, filtering out nation and claim zone parcels
                List<Parcel> overlaps = ParcelRegistry.find(bufferedBox).stream()
                        .filter(p -> !p.getId().equals(getParcelId()))
                        .filter(p -> !(p instanceof NationParcel || p instanceof CitizenZoneParcel))
                        .toList();

                if (!overlaps.isEmpty()) {
                    for (Parcel overlapParcel : overlaps) {
                        // the parcels are owned by the same person. they can be closer or touching,
                        // ie. ignore buffers, only the parcels themselves can't overlap
                        if (!getOwnerId().equals(overlapParcel.getOwnerId())) {
                            blockState = blockState.setValue(BufferBlock.INTERSECTS, BorderStatus.BAD);
                            break;
                        }
                    }
                }
            }
        }
        return blockState;
    }

    @Override
    public Box getBorderDisplayBox(ICoords coords) {
        return getAbsoluteBox(coords);
    }

}
