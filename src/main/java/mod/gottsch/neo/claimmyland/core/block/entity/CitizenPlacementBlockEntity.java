package mod.gottsch.neo.claimmyland.core.block.entity;

import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.config.Config;
import mod.gottsch.neo.claimmyland.core.parcel.ParcelType;
import mod.gottsch.neo.gottschcore.spatial.Coords;
import mod.gottsch.neo.gottschcore.spatial.ICoords;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nullable;

// TODO abstract most of this out to PlacementBlockEntity
/**
 * @author Mark Gottschling on Oct 18, 2204
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
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        if (ObjectUtils.isNotEmpty(getCoords1())) {
            tag.put(COORDS1, getCoords1().save(new CompoundTag()));
        }

        if (ObjectUtils.isNotEmpty(getCoords2())) {
            tag.put(COORDS2, getCoords2().save(new CompoundTag()));
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

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
