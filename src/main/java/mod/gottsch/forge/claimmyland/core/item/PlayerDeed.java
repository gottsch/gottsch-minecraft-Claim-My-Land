package mod.gottsch.forge.claimmyland.core.item;

import mod.gottsch.forge.claimmyland.core.block.ModBlocks;
import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.parcel.CitizenParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 *
 * @author Mark Gottschling on Sep 14, 2024
 *
 */
public class PlayerDeed extends Deed {

    public PlayerDeed(Properties properties) {
        super(properties);
        setParcelType(ParcelType.PLAYER);
    }

    @Override
    protected void populateFoundationStone(FoundationStoneBlockEntity blockEntity, ItemStack deed, BlockPos pos, Player player) {
        super.populateFoundationStone(blockEntity, deed, pos, player);

        ParcelRegistry.findLeastSignificant(Coords.of(pos))
                .filter(parcel -> parcel.isCitizen() || parcel.isPlayer())
                .ifPresent(parcel -> applyExistingParcelProperties(blockEntity, parcel));
    }

    private void applyExistingParcelProperties(FoundationStoneBlockEntity blockEntity, Parcel parcel) {
        blockEntity.setParcelId(parcel.getId());
        blockEntity.setRelativeBox(parcel.getSize());
        blockEntity.setCoords(parcel.getCoords());

        if (parcel.isCitizen()) {
            blockEntity.setNationEstateId(((CitizenParcel) parcel).getNationEstate().getId());
        }
    }

    @Override
    public Block getFoundationStone() {
        return ModBlocks.PLAYER_FOUNDATION_STONE.get();
    }

    @Override
    public void appendUsageHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(LangUtil.tooltip("player_deed.usage")).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(LangUtil.NEWLINE));
    }
}
