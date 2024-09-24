package mod.gottsch.forge.claimmyland.core.item;

import mod.gottsch.forge.claimmyland.core.block.ModBlocks;
import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.parcel.CitizenParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/**
 *
 * @author Mark Gottschling on Sep 14, 2024
 *
 */
public class CitizenDeed extends Deed {

    public CitizenDeed(Properties properties) {
        super(properties);
    }

    @Override
    public Parcel createParcel() {
        return new CitizenParcel();
    }

    @Override
    public Parcel createParcel(ItemStack deedStack, ICoords coords, Player player) {
        CitizenParcel parcel = (CitizenParcel) super.createParcel(deedStack, coords, player);

        CompoundTag tag = deedStack.getOrCreateTag();

        // add nation id
        if (tag.contains(NationDeed.NATION_ID)) {
            parcel.setNationId(tag.getUUID(NationDeed.NATION_ID));
        }

        return parcel;
    }

    @Override
    protected void populateFoundationStone(FoundationStoneBlockEntity blockEntity, ItemStack deed, BlockPos pos, Player player) {
        super.populateFoundationStone(blockEntity, deed, pos, player);

        CompoundTag tag = deed.getOrCreateTag();

        // check if parcel is within another existing parcel
        Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(Coords.of(pos));

        // override some properties if within another parcel
        // if claiming an existing citizen parcel
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
//    protected boolean canPlaceBlock(Level level, ICoords coords, Parcel parcel) {
//        // test if a parcel already exists for the deed id
//        boolean canPlace = false;
//        Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(coords);
//
//        /*
//         * inside a parcel.
//         */
//        if (registryParcel.isPresent()) {
//            if (parcel.hasAccessTo(registryParcel.get())) {
//                canPlace = true;
//            }
//        }
//        return canPlace;
//    }

    @Override
    protected boolean validateWorldPlacement(Level level, BlockPos pos, Box size, Player player) {
        boolean result = super.validateWorldPlacement(level, pos, size, player);
        if (!result) {
            return false;
        }

        // TODO ensure the citizen parcel is wholly within a nation parcel

        return true;
    }

    @Override
    public Block getFoundationStone() {
        return ModBlocks.CITIZEN_FOUNDATION_STONE.get();
    }



    // TODO any unique data for appendHoverText

}
