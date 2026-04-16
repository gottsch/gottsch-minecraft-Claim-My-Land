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
package mod.gottsch.neo.claimmyland.core.item;

import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.block.FoundationStone;
import mod.gottsch.neo.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.neo.claimmyland.core.command.helper.PlayerMessageHelper;
import mod.gottsch.neo.claimmyland.core.config.Config;
import mod.gottsch.neo.claimmyland.core.network.CMLNetwork;
import mod.gottsch.neo.claimmyland.core.parcel.*;
import mod.gottsch.neo.claimmyland.core.persistence.PersistedData;
import mod.gottsch.neo.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.neo.claimmyland.core.registry.PlayerRegistry;
import mod.gottsch.neo.claimmyland.core.util.CelebrationHelper;
import mod.gottsch.neo.claimmyland.core.util.LangUtil;
import mod.gottsch.neo.claimmyland.core.util.ModUtil;
import mod.gottsch.neo.gottschcore.block.BlockContext;
import mod.gottsch.neo.gottschcore.spatial.Box;
import mod.gottsch.neo.gottschcore.spatial.Coords;
import mod.gottsch.neo.gottschcore.spatial.ICoords;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

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

    public static final String ESTATE_ID = "estate_id";
    public static final String NATION_ESTATE_ID = "nation_state_id";
    public static final String NATION_ESTATE_NAME = "nationEstateName";

    // default size = 1 chunk (16x16), but it is not necessarily aligned with a chunk
    public static final Box DEFAULT_SIZE = new Box(Coords.of(0, -15, 0), Coords.of(16, 16, 16));
    private ParcelType parcelType;

    /**
     *
     * @param properties
     */
    public Deed(Properties properties) {

        super(properties.stacksTo(1));
    }

//    public abstract Parcel createParcel();
//
//    public abstract Parcel createParcel(Player player);

    // TODO should return Optional<>
    /**
     * creates a parcel from an itemStack
     *
     * @param deedStack
     * @param coords
     * @param player
     * @return
     */
    public Optional<Parcel> createParcel(ItemStack deedStack, ICoords coords, Player player) {
        CompoundTag tag = deedStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        Optional<Parcel> optionalParcel = ParcelTypeRegistry.create(getParcelType());

        if (optionalParcel.isEmpty()) {
            return optionalParcel;
        }
        Parcel parcel = optionalParcel.orElseThrow(IllegalStateException::new);

        // update parcel properties if deed contains them
        if (tag.contains(PARCEL_ID)) {
            parcel.setId(tag.getUUID(PARCEL_ID));
        } else {
            UUID uuid = UUID.randomUUID();
            parcel.setId(uuid);
            tag.putUUID(PARCEL_ID, uuid);
        }
        if (tag.contains(DEED_ID)) {
            parcel.setDeedId(tag.getUUID(DEED_ID));
        }
        // 3/10/2025 added if deed id is blank
        else {
            UUID uuid = UUID.randomUUID();
            parcel.setDeedId(uuid);
            tag.putUUID(DEED_ID, uuid);
        }

        if (tag.contains(OWNER_ID)) {
            parcel.setOwnerId(tag.getUUID(OWNER_ID));
        } else {
            parcel.setOwnerId(player.getUUID());
        }

        // update deed item
        deedStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        parcel.setSize(getSize(tag));
        parcel.setCoords(coords);
        parcel.setName(parcel.defaultName(player));

        return optionalParcel;
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
            PlayerMessageHelper.sendFailure(player, "parcel.outside_world_boundaries");
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
            PlayerMessageHelper.sendFailure(player, "parcel.max_reached");
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

        ResourceLocation dimId = context.getLevel().dimension().location();
        List<? extends String> excluded = Config.SERVER.dimensions.excludedDimensions.get();
        if (excluded.stream().anyMatch(e -> e.equals(dimId.toString()))) {
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
        Optional<Parcel> optionalParcel = createParcel(context.getItemInHand(), targetCoords, context.getPlayer());
        if (optionalParcel.isEmpty()) {
            PlayerMessageHelper.sendFailure(context.getPlayer(), "deed.invalid");
            return InteractionResult.FAIL;
        }

        Parcel parcel = optionalParcel.get();

        // validate that the parcel's owner id == player id
        if (!parcel.isOwner(context.getPlayer().getUUID())) {
            // send not owner of deed message
            PlayerMessageHelper.sendFailure(context.getPlayer(),"deed.not_owner");
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
                    PlayerMessageHelper.sendFailure(context.getPlayer(),"foundation_stone.incorrect_deed");
                    return InteractionResult.FAIL;
                }

                /*
                 * check if parcel is within another existing parcel
                 */
                String dimension = context.getLevel().dimension().location().toString();
                Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(targetCoords, dimension);

                // Blacklist check — deny if player is blacklisted from the enclosing nation
                if (ParcelRegistry.isBlacklistedFromNation(
                        Coords.of(context.getClickedPos()),
                        context.getPlayer().getUUID(),
                        dimension)) {
                    PlayerMessageHelper.sendFailure(context.getPlayer(),
                            "deed.claim.access_denied");
                    return InteractionResult.FAIL;
                }

                Box parcelBox = foundationStoneBlockEntity.getAbsoluteBox();
                applyWorldPosition(parcel, foundationStoneBlockEntity);

                ClaimResult claimResult = registryParcel.map(parentParcel -> parcel.handleEmbeddedClaim(context.getLevel(), parentParcel)).orElseGet(() -> parcel.handleClaim(context.getLevel()));

                if (claimResult.isSuccess()) {
                    /*
                     * Re-resolve the registered parcel by ID. The local `parcel` variable is the
                     * transient pre-claim object built from the deed item; in the normal claim path
                     * it matches the registered parcel, but in the PlayerParcel→CitizenParcel
                     * conversion (PlayerParcel.claimWithinZone) and the relinquished-Citizen reclaim
                     * (transferParcelOwnership) paths, the registered parcel is a different object
                     * with a different type and estate. Reading celebration data off the stale local
                     * caused PlayerDeed→Citizen conversions to fire green Player fireworks instead
                     * of purple Citizen fireworks. The parcel UUID is stable across both conversion
                     * paths, so findByParcelId() is the right key.
                     */
                    Parcel registeredParcel = ParcelRegistry.findByParcelId(parcel.getId()).orElse(parcel);

                    // register user name
                    PlayerRegistry.register(context.getPlayer().getUUID(), context.getPlayer().getScoreboardName());

                    PersistedData savedData = PersistedData.get(context.getLevel());
                    if (savedData != null) {
                        savedData.setDirty();
                    }

                    // if creative force removal of item
                    if (context.getPlayer().isCreative()) {
                        context.getPlayer().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    } else {
                        context.getItemInHand().shrink(1);
                    }

                    // remove the foundation stone
                    blockEntity.getLevel().setBlock(context.getClickedPos(), Blocks.AIR.defaultBlockState(), 3);

                    // Celebrate the claim — perimeter particle wave on the claiming player's client only
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer
                            && context.getLevel() instanceof ServerLevel) {
                        CMLNetwork.sendClaimCelebration(serverPlayer, registeredParcel, context.getClickedPos());
                    }

                    if (context.getLevel() instanceof ServerLevel serverLevel) {
                        CelebrationHelper.spawnFireworks(serverLevel, registeredParcel);
                    }

                    PlayerMessageHelper.sendSuccess(context.getPlayer(),
                            "deed.claim.success",
                            "deed.claim.success.detail",
                            registeredParcel.getMinCoords().toShortString(),
                            ModUtil.getSize(registeredParcel.getBox()).toShortString());

                    if (claimResult == ClaimResult.SUCCESS_WITH_WARNINGS) {
                        PlayerMessageHelper.sendWarning(context.getPlayer(), "deed.claim.structure_warning");
                    }
                }
                else {
                    switch (claimResult) {
                        case STRUCTURE_DENIED -> {
                            PlayerMessageHelper.sendFailure(context.getPlayer(),
                                    "deed.claim.structure_denied");
                        }
                        case INTERSECTS -> {
                            PlayerMessageHelper.sendFailure(context.getPlayer(),
                                    "deed.claim.intersects");
                        }
                        case INSUFFICIENT_SIZE -> {
                            PlayerMessageHelper.sendFailure(context.getPlayer(),
                                    "deed.claim.insufficient_size",
                                    "deed.claim.insufficient_size.detail",
                                    parcel.getSize(),
                                    ModUtil.getSize(((FoundationStoneBlockEntity) blockEntity).getRelativeBox()).toShortString());
                        }
                        default -> {
                            PlayerMessageHelper.sendFailure(context.getPlayer(),
                                    "deed.claim.unable_to_claim",
                                    "deed.claim.unable_to_claim.detail",
                                    parcel.getMinCoords().toShortString(),
                                    ModUtil.getSize(parcel.getBox()).toShortString());
                        }
                    }

                }
                return claimResult.isSuccess()  ? InteractionResult.CONSUME : InteractionResult.FAIL;
            }
        }
        else {
            /*
             * place foundation stone
             */
            Block foundationStone = getFoundationStone();
            if (foundationStone == null) {
                PlayerMessageHelper.sendFailure(context.getPlayer(), "foundation_stone.unable_to_locate");
                ClaimMyLand.LOGGER.warn("unable to location foundation stone for deed -> {}", parcel.getDeedId());
                return InteractionResult.FAIL;
            }

            // validate size of Deed's dimensions
            // an Ops or mod could create a Deed via NBT with wrong values
            Box size = getSize(context.getItemInHand().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
            if (size.getSize().getX() < 2 || size.getSize().getY() < 2 || size.getSize().getZ() < 2) {
                PlayerMessageHelper.sendFailure(context.getPlayer(), "deed.too_small");
                return InteractionResult.FAIL;
            }

            BlockPlaceContext placeContext = new BlockPlaceContext(context);
            ICoords placeTargetCoords = Coords.of(placeContext.getClickedPos());

            PlacementResult placement = parcel.canPlaceAt(context.getLevel(), placeTargetCoords);
            if (!placement.isSuccess()) {
                PlayerMessageHelper.sendFailure(context.getPlayer(), placementLangKey(placement));
                return InteractionResult.FAIL;
            }

            boolean placed = placeBlock(placeContext, foundationStone.defaultBlockState());
            return placed ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        return super.useOn(context);
    }

    /**
     * Maps a {@link PlacementResult} failure value to its lang key. SUCCESS is
     * not handled — callers must check {@code result.isSuccess()} first.
     */
    private static String placementLangKey(PlacementResult result) {
        return switch (result) {
            case OUTSIDE_WORLD        -> "deed.place.outside_world";
            case NATION_CLOSED        -> "deed.place.nation_closed";
            case NATION_BLACKLISTED   -> "deed.place.nation_blacklisted";
            case OUTSIDE_VALID_PARENT -> "deed.place.outside_valid_parent";
            case INVALID_PARENT_TYPE  -> "deed.place.invalid_parent_type";
            case ACCESS_DENIED        -> "deed.place.access_denied";
            case UNKNOWN_FAILURE      -> "deed.place.failure";
            case SUCCESS              -> throw new IllegalArgumentException(
                    "placementLangKey called with SUCCESS");
        };
    }

    protected void applyWorldPosition(Parcel parcel, FoundationStoneBlockEntity fbe) {
        Box parcelBox = fbe.getAbsoluteBox();
        parcel.setCoords(parcelBox.getMinCoords());
        parcel.setSize(new Box(
                Coords.of(0, 0, 0),
                Coords.of(
                        parcelBox.getMaxCoords().getX() - parcelBox.getMinCoords().getX(),
                        parcelBox.getMaxCoords().getY() - parcelBox.getMinCoords().getY(),
                        parcelBox.getMaxCoords().getZ() - parcelBox.getMinCoords().getZ()
                )
        ));
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
            CompoundTag tag = context.getItemInHand().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

            // get the size
            Box size = getSize(tag);

            if (!validateWorldPlacement(context.getLevel(), targetPos, size, context.getPlayer())) {
                return false;
            }

            // get the old stone coords if any
            ICoords oldFoundationStoneCoords = getPreviousCoords(context.getLevel(), tag);

            /*
             * TEMP - for some reason getStateForPlacement() is not being called on the block,
             * so have to setup the facing value here.
             */
            state = state.setValue(FoundationStone.FACING, context.getHorizontalDirection().getOpposite());
            /*
             * add the foundation stone to the world
             */
            boolean result = context.getLevel().setBlock(targetPos, state, 3);//26);
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
        FoundationStoneBlockEntity blockEntity = (FoundationStoneBlockEntity) level.getBlockEntity(pos);
        if (blockEntity != null) {
            populateFoundationStone(blockEntity, deed, pos, player);
            if (previousCoords != Coords.EMPTY) {
                removePreviousLocation(level, deed, previousCoords);
            }
            storeCurrentLocation(level, deed, Coords.of(pos));
            blockEntity.placeParcelBorder((ServerPlayer) player);
        }
    }
//    protected void handleBlockPlaced(Level level, BlockPos pos, Player player, ItemStack deed, ICoords previousCoords) {
//        // get the block entity
//        FoundationStoneBlockEntity blockEntity = (FoundationStoneBlockEntity) level.getBlockEntity(pos);
//        if (blockEntity != null) {
//
//            // update data from deed or existing parcel
//            populateFoundationStone(blockEntity, deed, pos, player);
//
//            //check if there is a stored position of foundation stone.
//            if (previousCoords != Coords.EMPTY) {
//                removePreviousLocation(level, deed, previousCoords);
//            }
//
//            // store position of new foundation stone
//            storeCurrentLocation(level, deed, Coords.of(pos));
//
//            /*
//             * NOTE foundation stone is non-craftable nor in the crafting tab
//             * so need to initiate the borders manually.
//             */
//            // place border blocks
//            blockEntity.placeParcelBorder((ServerPlayer) player);
////            blockEntity.placeParcelHorizontalArea();
//        }
//    }

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
        CompoundTag tag = deed.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        Box size = getSize(tag);

        // create IDs on first use
        if (!tag.contains(PARCEL_ID)) {
            tag.putUUID(PARCEL_ID, UUID.randomUUID());
        }
        if (!tag.contains(DEED_ID)) {
            tag.putUUID(DEED_ID, UUID.randomUUID());
        }

        // transfer properties to block entity
        blockEntity.setParcelId(tag.contains(PARCEL_ID) ? tag.getUUID(PARCEL_ID) : null);
        blockEntity.setDeedId(tag.contains(DEED_ID) ? tag.getUUID(DEED_ID) : null);
        blockEntity.setOwnerId(tag.contains(OWNER_ID) ? tag.getUUID(OWNER_ID) : player.getUUID());

        // TODO update to use getParcelType()
        String parcelType;
        if (tag.contains(PARCEL_TYPE)) {
            parcelType = tag.getString(PARCEL_TYPE);
        } else {
            parcelType = ((Deed) deed.getItem()).getParcelType().getSerializedName();
        }

        // update deed item properties
        deed.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        blockEntity.setParcelType(parcelType);
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
            CompoundTag tag = deed.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            tag.remove("pos");
            deed.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    protected void storeCurrentLocation(Level level, ItemStack deed, ICoords coords) {
        CompoundTag posTag = new CompoundTag();
        CompoundTag tag = deed.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.put("pos", coords.save(posTag));
        deed.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {

        super.appendHoverText(stack, context, tooltip, flag);

        appendUsageHoverText(stack, context.level(), tooltip, flag);
        appendDetailsHoverText(stack, context.level(), tooltip, flag);

        // how to use
        LangUtil.appendAdvancedHoverText(tooltip, tt -> {
            appendHowToHoverText(stack, context.level(), tooltip, flag);
        });
    }

    public void appendUsageHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag flag) {

    }

    public void appendDetailsHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(LangUtil.tooltip("deed.type"), ChatFormatting.BLUE + getParcelType().name())); //stack.getTag().getString(Deed.PARCEL_TYPE)));

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(Deed.SIZE)) {
            appendSizeHoverText(stack, level, tooltip, flag);
        }
    }

    public void appendSizeHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag flag) {
        Box size = getSize(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
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


    public ParcelType getParcelType() {
        return parcelType;
    }

    public void setParcelType(ParcelType type) {
        this.parcelType = type;
    }
}
