package mod.gottsch.forge.claimmyland.core.item;

import mod.gottsch.forge.claimmyland.core.block.ModBlocks;
import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.parcel.NationParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelFactory;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 *
 * @author Mark Gottschling on Mar 27, 2024
 *
 */
public class NationDeed extends Deed {
    public static final String NATION_ID = "nation_id";

    public NationDeed(Properties properties) {
        super(properties);
    }

    @Override
    public Parcel createParcel() {
        return ParcelFactory.create(ParcelType.NATION).orElse(new NationParcel());
    }

    @Override
    public Parcel createParcel(ItemStack deedStack, ICoords coords, Player player) {
        NationParcel parcel = (NationParcel)super.createParcel(deedStack, coords, player);

        CompoundTag tag = deedStack.getOrCreateTag();

        // TODO this should be moot now.
        // add nation id
        if (tag.contains(NATION_ID)) {
            parcel.setNationId(tag.getUUID(NATION_ID));
        }

        // override coords
        parcel.setCoords(parcel.getCoords().withY(0));

        return parcel;
    }

    @Override
    public Block getFoundationStone() {
        return ModBlocks.NATION_FOUNDATION_STONE.get();
    }

    @Override
    protected boolean validateWorldPlacement(Level level, BlockPos pos, Box size, Player player) {
        if (level.isOutsideBuildHeight(size.getMinCoords().getY())
            || level.isOutsideBuildHeight(size.getMaxCoords().getY())) {

            player.sendSystemMessage((Component.translatable(LangUtil.chat("parcel.outside_world_boundaries"))
                    .withStyle(new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.ITALIC})));
            return false;
        }
        return true;
    }

    @Override
    protected void populateFoundationStone(FoundationStoneBlockEntity blockEntity, ItemStack deed, BlockPos pos, Player player) {
        super.populateFoundationStone(blockEntity, deed, pos, player);

        // default behaviour
        CompoundTag tag = deed.getOrCreateTag();
        Box size = getSize(tag);

        blockEntity.setNationId(tag.contains(NATION_ID) ? tag.getUUID(NATION_ID) : null);
    }

    /*
     * cannot use deed to place foundation stone that is embedded in another parcel
     */
    @Override
    public boolean handleEmbeddedPlacementRules(Parcel parcel, Parcel registryParcel) {
        return false;
    }

    /**
     * cannot use deed on foundation stone that is embedded in another parcel
     * NOTE this should not happen as it should fail the placement rules.
     * @param context
     * @param parcel
     * @param parentParcel
     * @param blockEntity
     * @return
     */
    @Override
    protected InteractionResult handleEmbeddedClaim(UseOnContext context, Parcel parcel, Parcel parentParcel, FoundationStoneBlockEntity blockEntity) {
        return InteractionResult.FAIL;
    }

    // TODO any unique data for appendHoverText

}
