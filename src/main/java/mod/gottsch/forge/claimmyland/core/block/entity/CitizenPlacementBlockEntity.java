package mod.gottsch.forge.claimmyland.core.block.entity;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.block.BorderBlock;
import mod.gottsch.forge.claimmyland.core.block.BorderStatus;
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
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

// TODO abstract most of this out to PlacementBlockEntity
/**
 * Create by Mark Gottschling on Oct 18, 2204
 */
public class CitizenPlacementBlockEntity extends BorderStoneBlockEntity {

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
    public CitizenPlacementBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CITIZEN_PLACEMENT_ENTITY_TYPE.get(), pos, state);
    }

    // don't tick
    public void tickServer() {

    }

    @Override
    public int getBufferSize(ParcelType type) {
      return Config.SERVER.general.parcelBufferRadius.get();
    }

    @Override
    public Block getBorderBlock() {
        return ModBlocks.CITIZEN_BORDER.get();
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
            // if a citizen placement is not within a zone or a nation then bad
            blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
        } else {
            // determine what parcel type is the zone in
            if (registryParcel.get().getType() != ParcelType.NATION
                    && registryParcel.get().getType() != ParcelType.ZONE) {
                blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
            } else {
                // if any part of the parcel is outside the nation, then bad
                if (!ModUtil.contains(registryParcel.get().getBox(), box)) {
                    blockState = blockState.setValue(BorderBlock.INTERSECTS, BorderStatus.BAD);
                } else {
                    // proceed with a normal overlaps check, filtering out nation parcels
                    List<Parcel> overlaps = ParcelRegistry.findBuffer(box);
                    overlaps = overlaps.stream()
                            .filter(p -> !(p instanceof NationParcel || p instanceof ZoneParcel))
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
    protected BlockState getBufferBlockState(Box box, Box bufferedBox) {
        return Blocks.AIR.defaultBlockState();
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
