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
    protected InteractionResult handleEmbeddedClaim(UseOnContext context, Parcel parcel, Parcel parentParcel, FoundationStoneBlockEntity blockEntity) {
        if (parentParcel.getType() == ParcelType.CITIZEN) {
            if (ObjectUtils.isEmpty(parentParcel.getOwnerId())) {
                parentParcel.setOwnerId(parcel.getOwnerId());
            }
        }
        else if (parentParcel.getType() == ParcelType.CITIZEN_ZONE) {
            Box parcelBox = blockEntity.getAbsoluteBox();

            // find overlaps of the parcel with buffered registry parcels.
            // this ensure that the parcel boundaries are not overlapping the buffer area of another parcel
            // NOTE filter out the multicitizen and nation parcels
            List<Parcel> overlaps = ParcelRegistry.findBuffer(parcelBox).stream()
                    .filter(p -> !p.getId().equals(parentParcel.getId()))
                    .filter(p -> !p.getId().equals(((CitizenZoneParcel)parentParcel).getNationId()))
                    .toList();

            if (!overlaps.isEmpty()) {
                for (Parcel overlapParcel : overlaps) {
                    // if parcel in hand equals parcel in world then fail
                    /*
                     * NOTE this should be moot as the deed shouldn't exist at this point anymore (survival)
                     * as this can potentially only happen in creative.
                     */
                    if (parcel.getId().equals(overlapParcel.getId())) {
                        return InteractionResult.FAIL;
                    }

                    /*
                     * if parcel in hand has same owner as parcel in world, ignore buffers,
                     * but check border overlaps. parcels owned by the same player can be touching.
                     */
                    if (parcel.getOwnerId().equals(overlapParcel.getOwnerId())) {
                        // get the existing owned parcel
                        Optional<Parcel> optionalOwnedParcel = ParcelRegistry.findByParcelId(overlapParcel.getId());

                        // test if the non-buffered parcels intersect
                        if (optionalOwnedParcel.isPresent() && ModUtil.intersects(parcel.getBox(), optionalOwnedParcel.get().getBox())) {
                            return InteractionResult.FAIL;
                        }
                    } else {
                        return InteractionResult.FAIL;
                    }
                }
            }

            // validate placement. transform personal into citizen parcel
            Optional<Parcel> optionalCitizenParcel = ParcelFactory.create(ParcelType.CITIZEN);
            if (optionalCitizenParcel.isPresent()) {
                CitizenParcel citizenParcel = (CitizenParcel) optionalCitizenParcel.get();
                citizenParcel.setNationId(((CitizenZoneParcel) parentParcel).getNationId());
                citizenParcel.setId(parcel.getId());
                citizenParcel.setSize(parcel.getSize());
                citizenParcel.setCoords(blockEntity.getCoords());
                citizenParcel.setOwnerId(parcel.getOwnerId());
                // add to the registry
                ParcelRegistry.add(citizenParcel);
            }
        }

        return InteractionResult.CONSUME;
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
