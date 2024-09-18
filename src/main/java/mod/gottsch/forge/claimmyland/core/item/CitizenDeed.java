package mod.gottsch.forge.claimmyland.core.item;

import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.parcel.CitizenParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.gottschcore.spatial.Box;
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
    protected boolean canPlaceBlock(Level level, ICoords coords, Parcel parcel) {
        // test if a parcel already exists for the deed id
        boolean canPlace = false;
        Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(coords);

        /*
         * inside a parcel.
         */
        if (registryParcel.isPresent()) {
            if (parcel.hasAccessTo(registryParcel.get())) {
                canPlace = true;
            }
        }
        return canPlace;
    }

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
        return null;
    }

    @Override
    protected InteractionResult handleEmbeddedClaim(UseOnContext context, Parcel parcel, Parcel parentParcel, FoundationStoneBlockEntity blockEntity) {
        return null;
    }

    // TODO any unique data for appendHoverText

}
