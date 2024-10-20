/*
 * This file is part of  Claim My Land.
 * Copyright (c) 2024 Mark Gottschling (gottsch)
 *
 * All rights reserved.
 *
 * Claim My Land is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Claim My Land is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Claim My Land.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.forge.claimmyland.core.item;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.block.FoundationStone;
import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.gui.chat.ChatMessages;
import mod.gottsch.forge.claimmyland.core.parcel.ClaimResult;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.persistence.PersistedData;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.registry.PlayerRegistry;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.block.BlockContext;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 *
 * @author Mark Gottschling on Sep 14, 2024
 *
 */
public abstract class Deed extends Item {
    public static final String PARCEL_ID = "parcel_id";
    public static final String DEED_ID = "deed_id";
    public static final String OWNER_ID = "owner_id";
    public static final String PARCEL_TYPE = "parcel_type";
    public static final String SIZE = "size";

    // default size = 1 chunk (16x16), but it is not necessarily aligned with a chunk
    public static final Box DEFAULT_SIZE = new Box(Coords.of(0, -15, 0), Coords.of(16, 16, 16));

    /**
     *
     * @param properties
     */
    public Deed(Properties properties) {

        super(properties.stacksTo(1));
    }

    public abstract Parcel createParcel();

    /**
     * creates a parcel from an itemStack
     * @param deedStack
     * @param coords
     * @param player
     * @return
     */
    public Parcel createParcel(ItemStack deedStack, ICoords coords, Player player) {
        Parcel parcel = createParcel();

        CompoundTag tag = deedStack.getOrCreateTag();
        if (tag.contains(PARCEL_ID)) {
            parcel.setId(tag.getUUID(PARCEL_ID));
        } else {
            parcel.setId(UUID.randomUUID());
        }
        if (tag.contains(DEED_ID)) {
            parcel.setDeedId(tag.getUUID(DEED_ID));
        }
        if (tag.contains(OWNER_ID)) {
            parcel.setOwnerId(tag.getUUID(OWNER_ID));
        } else {
            parcel.setOwnerId(player.getUUID());
        }

        parcel.setSize(getSize(tag));
        parcel.setCoords(coords);
        parcel.setName(parcel.randomName());

        return parcel;
    }

    /**
     * checks if the world location is valid
     * @param level
     * @param pos
     * @param size
     * @param player
     * @return
     */
    protected boolean validateWorldPlacement(Level level, BlockPos pos, Box size, Player player) {
        if (level.isOutsideBuildHeight(pos.offset(size.getMinCoords().toPos()))
                        || level.isOutsideBuildHeight(pos.offset(size.getMaxCoords().toPos()))) {
            player.sendSystemMessage((Component.translatable(LangUtil.chat("parcel.outside_world_boundaries"))
                    .withStyle(new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.ITALIC})));
            return false;
        }
        return true;
    }

    /**
     * checks if the player has met their parcel limit threshold
     * @param player
     * @return
     */
    protected boolean validateParcelThreshold(Player player) {
        // gather the number of parcels the player has
        List<Parcel> parcels = ParcelRegistry.findByOwner(player.getUUID());
        if (parcels.size() >= Config.SERVER.general.parcelsPerPlayer.get() && !player.hasPermissions(Config.SERVER.general.opsPermissionLevel.get())) {
            // TODO create a class ChatHelper that has premade color formatters
            player.sendSystemMessage(Component.translatable(LangUtil.chat("parcel.max_reached")).withStyle(ChatFormatting.RED));
            return false;
        }
        return true;
    }

    /**
     *
     * @param context
     * @return
     */
    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {

        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (context.getLevel().dimensionTypeId() != BuiltinDimensionTypes.OVERWORLD) {
            return InteractionResult.SUCCESS;
        }

        /*
         * check if the player has reached their max parcels already
         */
        boolean isParcelThresholdValid = validateParcelThreshold(context.getPlayer());
        if (!isParcelThresholdValid) {
            return InteractionResult.FAIL;
        }

        // wrapped BlockPos
        ICoords targetCoords = Coords.of(context.getClickedPos());

        // create a parcel object from the deed itemStack and context info
        Parcel parcel = createParcel(context.getItemInHand(), targetCoords, context.getPlayer());

        // validate that the parcel's owner id == player id
        if (!parcel.isOwner(context.getPlayer().getUUID())) {
            // send not owner of deed message
            context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.chat("deed.not_owner")).withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        // if using the deed on a foundation stone
        if (context.getLevel().getBlockState(context.getClickedPos()).is(getFoundationStone())) {
            /*
             * validate location for acceptance as parcel
             */
            // validate
            BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
            if (blockEntity instanceof FoundationStoneBlockEntity foundationStoneBlockEntity) {

                // ensure deed/parcel has access to the block entity
                if (!parcel.hasAccessTo(foundationStoneBlockEntity)) {
                    context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.chat("foundation_stone.incorrect_deed")).withStyle(ChatFormatting.RED));
                    return InteractionResult.FAIL;
                }

                /*
                 * check if parcel is within another existing parcel
                 */
                Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(targetCoords);

                Box parcelBox = foundationStoneBlockEntity.getAbsoluteBox();
                ClaimResult claimResult = registryParcel.map(parentParcel -> parcel.handleEmbeddedClaim(context.getLevel(), parentParcel, parcelBox)).orElseGet(() -> parcel.handleClaim(context.getLevel(), parcelBox));

                if (claimResult.isSuccess()) {
                    if (parcel.getFoundedTime() == 0) {
                        parcel.setFoundedTime(context.getLevel().getGameTime());
                    }
                    parcel.setOwnerTime(context.getLevel().getGameTime());
                    // clear abandonedTime (if any)
                    parcel.setAbandonedTime(0L);

                    // register user name
                    PlayerRegistry.register(context.getPlayer().getUUID(), context.getPlayer().getScoreboardName());

                    PersistedData savedData = PersistedData.get(context.getLevel());
                    // mark data as dirty
                    if (savedData != null) {
                        savedData.setDirty();
                    }

                    // if creative force removal of item
                    if (context.getPlayer().isCreative()) {
                        context.getPlayer().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    } else {
                        // consume item
                        context.getItemInHand().shrink(1);
                    }

                    // remove the border
                    ((FoundationStoneBlockEntity) blockEntity).removeParcelBorder();
                    // remove the foundation stone
                    blockEntity.getLevel().setBlock(context.getClickedPos(), Blocks.AIR.defaultBlockState(), 3);

                    // TODO add particle effects or place construction tap around border or border display blcok

                    // send success message
                    context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.chat("deed.claim.success"),
                            parcel.getMinCoords().toShortString(),
                            ModUtil.getSize(parcel.getBox()).toShortString()).withStyle(ChatFormatting.GREEN));

                } else {
                    // send the respective failure message.
                    switch (claimResult) {
                        case INTERSECTS -> {
                            context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.chat("deed.claim.intersects")).withStyle(ChatFormatting.RED));
                        }
                        case INSUFFICIENT_SIZE -> {
                            context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.chat("deed.claim.insufficient_size"),
                                    parcel.getSize(),
                                    ModUtil.getSize(((FoundationStoneBlockEntity) blockEntity).getRelativeBox()).toShortString()).withStyle(ChatFormatting.RED));
                        }
                        default -> {
                            ChatMessages.unableToClaim(context.getPlayer(), parcel.getMinCoords(),
                                    ModUtil.getSize(parcel.getBox()));
                        }
                    }
                }
                return claimResult.isSuccess()  ? InteractionResult.CONSUME : InteractionResult.FAIL;
            }
        } else {
            /*
             * place foundation stone
             */
            Block foundationStone = getFoundationStone();
            if (foundationStone == null) {
               context.getPlayer().sendSystemMessage(Component.translatable(LangUtil.chat("foundation_stone.unable_to_locate")).withStyle(ChatFormatting.RED));
               ClaimMyLand.LOGGER.warn("unable to location foundation stone for deed -> {}", parcel.getDeedId());
               return InteractionResult.FAIL;
            }

            BlockPlaceContext placeContext = new BlockPlaceContext(context);
            ICoords placeTargetCoords = Coords.of(placeContext.getClickedPos());
            return parcel.canPlaceAt(context.getLevel(), placeTargetCoords)
                    && this.placeBlock(placeContext, foundationStone.defaultBlockState())
                    ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        return super.useOn(context);
    }

    /**
     *
     * @return
     */
    public abstract Block getFoundationStone();

    /**
     *
     * @param context
     * @param state
     * @return
     */
    protected boolean placeBlock(@NotNull BlockPlaceContext context, BlockState state) {
        if (context.getLevel().isClientSide()) {
            return true;
        }

        // get the target position
        BlockPos targetPos = context.getClickedPos();
        BlockContext blockContext = new BlockContext(context.getLevel(), targetPos);
        if (blockContext.isAir() || blockContext.isReplaceable()) {
            CompoundTag tag = context.getItemInHand().getOrCreateTag();

            // get the size
            Box size = getSize(tag);

            if (!validateWorldPlacement(context.getLevel(), targetPos, size, context.getPlayer())) {
                return false;
            }

            // get the old stone coords if any
            ICoords oldFoundationStoneCoords = getPreviousCoords(context.getLevel(), tag);

            /*
             * add the foundation stone to the world
             */
            boolean result = context.getLevel().setBlock(targetPos, state, 26);
            if (result) {
                // if successful handle post placement
                handleBlockPlaced(context.getLevel(), targetPos, context.getPlayer(), context.getItemInHand(), oldFoundationStoneCoords);
            }
            return true;
        }
        return false;
    }

    /**
     *
     * @param level
     * @param pos
     * @param player
     * @param deed
     * @param previousCoords
     */
    protected void handleBlockPlaced(Level level, BlockPos pos, Player player, ItemStack deed, ICoords previousCoords) {
        // get the block entity
        FoundationStoneBlockEntity blockEntity = (FoundationStoneBlockEntity) level.getBlockEntity(pos);
        if (blockEntity != null) {

            // update data from deed or existing parcel
            populateFoundationStone(blockEntity, deed, pos, player);

            //check if there is a stored position of foundation stone.
            if (previousCoords != Coords.EMPTY) {
                removePreviousLocation(level, deed, previousCoords);
            }

            // store position of new foundation stone
            storeCurrentLocation(level, deed, Coords.of(pos));

            /*
             * NOTE foundation stone is non-craftable nor in the crafting tab
             * so need to initiate the borders manually.
             */
            // place border blocks
            blockEntity.placeParcelBorder();
        }
    }

    /**
     *
     * @param level
     * @param tag
     * @return
     */
    protected ICoords getPreviousCoords(Level level, CompoundTag tag) {
        // get the previous coords from tag if they exist
        ICoords coords = Coords.EMPTY;
        if (tag.contains("pos")) {
            CompoundTag posTag = tag.getCompound("pos");
            coords = Coords.EMPTY.load(posTag);
        }

        /*
         * check if deed has old info. ie foundation stone was destroyed by player
         * instead of destroy by using the deed somewhere else.
         */
        // get the old block entity if exists
        BlockEntity oldBlockEntity = level.getBlockEntity(coords.toPos());
        if (!(oldBlockEntity instanceof FoundationStoneBlockEntity)){
            // clean deed as a stone doesn't exist at pos
            tag.remove("pos");
            // reset pos to empty ie there isn't an old foundation stone position.
            coords = Coords.EMPTY;
        }

        return coords;
    }

    protected void populateFoundationStone(FoundationStoneBlockEntity blockEntity, ItemStack deed, BlockPos pos, Player player) {
        // default behaviour
        CompoundTag tag = deed.getOrCreateTag();
        Box size = getSize(tag);

        blockEntity.setParcelId(tag.contains(PARCEL_ID) ? tag.getUUID(PARCEL_ID) : null);
        blockEntity.setDeedId(tag.contains(DEED_ID) ? tag.getUUID(DEED_ID) : null);
        blockEntity.setOwnerId(tag.contains(OWNER_ID) ? tag.getUUID(OWNER_ID) : player.getUUID());
        blockEntity.setParcelType(tag.contains(PARCEL_TYPE) ? tag.getString(PARCEL_TYPE) : null);
        blockEntity.setCoords(new Coords(pos));
        blockEntity.setRelativeBox(size);
        blockEntity.setExpireTime(blockEntity.getLevel().getGameTime() + Config.SERVER.borders.foundationStoneLifeSpan.get());
    }

    protected void removePreviousLocation(Level level, ItemStack deed, ICoords previousCoords) {
        if (level.getBlockState(previousCoords.toPos()).is(getFoundationStone())) {
            /*
             * destroy old foundationStone
             */
            level.destroyBlock(previousCoords.toPos(), false);
            // remove old pos
            deed.getOrCreateTag().remove("pos");
        }
    }

    protected void storeCurrentLocation(Level level, ItemStack deed, ICoords coords) {
        CompoundTag posTag = new CompoundTag();
        deed.getOrCreateTag().put("pos", coords.save(posTag));
    }

    /**
     * the dimensions of the deed/parcel
     * @param tag
     * @return
     */
    public Box getSize(CompoundTag tag) {
        Box size = Box.EMPTY;
        if (tag.contains(SIZE)) {
            CompoundTag sizeTag = tag.getCompound(SIZE);
            size = Box.load(sizeTag);
        } else {
            size = DEFAULT_SIZE;
        }
        return size;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        appendUsageHoverText(stack, level, tooltip, flag);
        appendDetailsHoverText(stack, level, tooltip, flag);

        // how to use
        LangUtil.appendAdvancedHoverText(tooltip, tt -> {
            appendHowToHoverText(stack, level, tooltip, flag);
        });
    }

    public void appendUsageHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag flag) {

    }

    public void appendDetailsHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (stack.getTag() != null && stack.getTag().contains(Deed.PARCEL_TYPE)) {
            tooltip.add(Component.translatable(LangUtil.tooltip("deed.type"), ChatFormatting.BLUE + stack.getTag().getString(Deed.PARCEL_TYPE)));
        }

        if (stack.getTag() != null && stack.getTag().contains(Deed.SIZE)) {
            appendSizeHoverText(stack, level, tooltip, flag);
        }
    }

    public void appendSizeHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag flag) {
        Box size = getSize(stack.getTag());
        tooltip.add(Component.translatable(LangUtil.tooltip("deed.size"), ChatFormatting.BLUE + ModUtil.getSize(size).toShortString()));
        tooltip.add(Component.literal(LangUtil.INDENT2).append(Component.literal("X: ").append(Component.literal(String.valueOf(size.getMaxCoords().getX() - size.getMinCoords().getX() + 1)).withStyle(ChatFormatting.AQUA))));
        tooltip.add(Component.literal(LangUtil.INDENT2).append(Component.literal("Up: ").append(Component.literal(String.valueOf(size.getMaxCoords().getY()+1)).withStyle(ChatFormatting.AQUA))));
        tooltip.add(Component.literal(LangUtil.INDENT2).append(Component.literal("Down: ").append(Component.literal(String.valueOf(Math.abs(size.getMinCoords().getY()))).withStyle(ChatFormatting.AQUA))));
        tooltip.add(Component.literal(LangUtil.INDENT2).append(Component.literal("Z: ").append(Component.literal(String.valueOf(size.getMaxCoords().getZ() - size.getMinCoords().getZ() + 1)).withStyle(ChatFormatting.AQUA))));
    }

    public void appendHowToHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(LangUtil.NEWLINE));
        // howto may be multiple lines, so separate on ~ and add to tooltip
        Component lore = Component.translatable(LangUtil.tooltip("deed.howto"));
        for (String s : lore.getString().split("~")) {
            tooltip.add(Component.literal(LangUtil.INDENT2)
                    .append(Component.translatable(s)).withStyle(ChatFormatting.DARK_PURPLE).withStyle(ChatFormatting.ITALIC));
        }
        tooltip.add(Component.literal(LangUtil.NEWLINE));
    }

}
