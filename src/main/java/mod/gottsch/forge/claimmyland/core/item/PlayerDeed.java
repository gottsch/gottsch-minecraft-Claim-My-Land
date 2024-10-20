package mod.gottsch.forge.claimmyland.core.item;

import mod.gottsch.forge.claimmyland.core.block.ModBlocks;
import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.parcel.CitizenParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.parcel.PlayerParcel;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

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
            if (registryParcel.get().getType() == ParcelType.CITIZEN || registryParcel.get().getType() == ParcelType.PLAYER) {
                // update block entity with properties of that of the existing citizen parcel
                blockEntity.setParcelId(registryParcel.get().getId());
                if (registryParcel.get().getType() == ParcelType.CITIZEN) {
                    blockEntity.setNationId(((CitizenParcel) registryParcel.get()).getNationId());
                }
                blockEntity.setRelativeBox(registryParcel.get().getSize());
                blockEntity.setCoords(registryParcel.get().getCoords());
            }
        }
    }

    public Block getFoundationStone() {
        return ModBlocks.PLAYER_FOUNDATION_STONE.get();
    }


    @Override
    public void appendUsageHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(LangUtil.tooltip("player_deed.usage")).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(LangUtil.NEWLINE));
    }
}
