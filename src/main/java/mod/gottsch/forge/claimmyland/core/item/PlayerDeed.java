package mod.gottsch.forge.claimmyland.core.item;

import mod.gottsch.forge.claimmyland.core.block.ModBlocks;
import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.parcel.*;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author Mark Gottschling on Sep 14, 2024
 *
 */
public class PlayerDeed extends Deed {

    public PlayerDeed(Properties properties) {
        super(properties);
    }

    // TODO should these call the factory
    @Override
    public Parcel createParcel() {
        return new PlayerParcel();
    }

    @Override
    protected void populateFoundationStone(FoundationStoneBlockEntity blockEntity, ItemStack deed, BlockPos pos, Player player) {
        super.populateFoundationStone(blockEntity, deed, pos, player);

        CompoundTag tag = deed.getOrCreateTag();

        // check if parcel is within another existing parcel
        Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(Coords.of(pos));

        // override some properties if within another parcel
        if (registryParcel.isPresent()) {
            if (registryParcel.get().getType() == ParcelType.CITIZEN) {
                // update block entity with properties of that of the existing citizen parcel
                blockEntity.setParcelId(registryParcel.get().getId());
                blockEntity.setNationId(((CitizenParcel) registryParcel.get()).getNationId());
                blockEntity.setRelativeBox(registryParcel.get().getSize());
                blockEntity.setCoords(registryParcel.get().getCoords());
            }
        }
    }

    public Block getFoundationStone() {
        return ModBlocks.PLAYER_FOUNDATION_STONE.get();
    }

// TODO any unique data for appendHoverText
}
