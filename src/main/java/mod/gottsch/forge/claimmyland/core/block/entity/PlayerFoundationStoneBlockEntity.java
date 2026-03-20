package mod.gottsch.forge.claimmyland.core.block.entity;

import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * @author Mark Gottschling on Sep 18, 2204
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
    public int getBufferSize(ParcelType type) {
      return Config.SERVER.general.parcelBufferRadius.get();
    }

}
