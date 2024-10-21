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
package mod.gottsch.forge.claimmyland.core.command;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.block.entity.BorderStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.item.Deed;
import mod.gottsch.forge.claimmyland.core.item.DeedFactory;
import mod.gottsch.forge.claimmyland.core.item.NationDeed;
import mod.gottsch.forge.claimmyland.core.parcel.*;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.registry.PlayerRegistry;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * @author Mark Gottschling on Mar 28, 2024
 */
public class ParcelCommandDelegate {

    public static int listParcelsByAbandoned(CommandSourceStack source) {
        try {
            List<Component> messages = new ArrayList<>();
            buildListTitle(messages, Component.translatable(LangUtil.chat("parcel.list.abandoned")));

            List<Component> components = formatList(messages, ParcelRegistry.findAbandoned());
            components.forEach(component -> {
                source.sendSuccess(() -> component, false);
            });

        } catch (Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred listing abandoned parcels:", e);
            source.sendFailure(Component.translatable(LangUtil.chat("unexpected_error")).withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    /**
     * @param source
     * @param ownerName
     * @return
     */
    public static int listParcelsByOwner(CommandSourceStack source, String ownerName) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        return listParcelsByOwner(source, player);
    }

    // TODO mve to common class
    public static int listParcelsByOwner(CommandSourceStack source, ServerPlayer player) {
        List<Component> messages = new ArrayList<>();
//        messages.add(Component.literal(""));
//        messages.add(Component.translatable(LangUtil.chat("parcel.list"), player.getName().getString()).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD, ChatFormatting.WHITE));
//        messages.add(Component.literal(""));
        buildListTitle(messages, Component.translatable(LangUtil.chat("parcel.list"), player.getName().getString()));

        List<Component> components = formatList(messages, ParcelRegistry.findByOwner(player.getUUID()));
        components.forEach(component -> {
            source.sendSuccess(() -> component, false);
        });
        return 1;
    }

    public static int listParcelsByNation(CommandSourceStack source, String nationName) {
        try {
            // find the nation by name
            NationParcel nation = (NationParcel) ParcelRegistry.getNations().stream()
                    .filter(n -> nationName.equalsIgnoreCase(((NationParcel) n).getName()))
                    .findFirst().orElseThrow();

            List<Component> messages = new ArrayList<>();
            messages.add(Component.literal(""));
            messages.add(Component.translatable(LangUtil.chat("parcel.list"), nation.getName()).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD, ChatFormatting.WHITE));
            messages.add(Component.literal(""));

            List<Component> components = formatList(messages, ParcelRegistry.findChildrenByNationId(nation.getNationId()));
            components.forEach(component -> {
                source.sendSuccess(() -> component, false);
            });

        } catch (Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred listing parcels by nation:", e);
            source.sendFailure(Component.translatable(LangUtil.chat("unexpected_error")).withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    static List<Component> buildListTitle(List<Component> messages, Component title) {
        messages.add(Component.literal(""));
        messages.add(((MutableComponent) title).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD, ChatFormatting.WHITE));
        messages.add(Component.literal(""));

        return messages;
    }

    static List<Component> formatList(List<Component> messages, List<Parcel> list) {

        if (list.isEmpty()) {
            messages.add(Component.translatable(LangUtil.chat("parcel.list.empty")).withStyle(ChatFormatting.AQUA));
        } else {
            list.forEach(parcel -> {
                messages.add(
                        Component.literal(parcel.getName().toUpperCase()).withStyle(ChatFormatting.AQUA)
                                .append(Component.literal(String.format(" [ %s ]: ", parcel.getType().getSerializedName())).withStyle(ChatFormatting.WHITE))
                                .append(Component.translatable(String.format("(%s) to (%s)",
                                        formatCoords(parcel.getMinCoords()),
                                        formatCoords(parcel.getMaxCoords()))).withStyle(ChatFormatting.GREEN)
                                )
                                .append(Component.translatable(", [" + formatCoords(ModUtil.getSize(parcel.getSize())) + "]").withStyle(ChatFormatting.WHITE))
                );

//				[STYLE].withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + blockpos.getX() + " " + s1 + " " + blockpos.getZ())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip"))
            });
        }
        return messages;
    }

    private static String formatCoords(ICoords coords) {
        return String.format("%s %s %s", coords.getX(), coords.getY(), coords.getZ());
    }


    public static int addParcel(CommandSourceStack source, String ownerName, BlockPos pos, int xSize, int ySizeUp, int ySizeDown, int zSize, String parcelType) {
        return addParcel(source, ownerName, pos, xSize, ySizeUp, ySizeDown, zSize, parcelType, "");
    }

    public static int addParcel(CommandSourceStack source, String ownerName, BlockPos pos, int xSize, int ySizeUp, int ySizeDown, int zSize, String parcelType, String nationName) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);

        // find the nation by name
        UUID nationId = ParcelRegistry.getNations().stream()
                .filter(n -> nationName.equalsIgnoreCase(((NationParcel) n).getName()))
                .findFirst()
                .map(n -> ((NationParcel) n).getNationId()).orElse(null);

        ParcelType type = ParcelType.valueOf(parcelType);

        // validate size
        if (xSize < 2 || ySizeUp + ySizeDown < 2 || zSize < 2) {
            CommandHelper.failure(source, "parcel.add.failure_too_small");
        }

        // validate type
        if ((type == ParcelType.CITIZEN || type == ParcelType.ZONE) && nationId == null) {
            source.sendFailure(Component.translatable(LangUtil.chat("parcel.citizen.nationId_required")).withStyle(ChatFormatting.RED));
            return 0;
        }

        // TODO this is for current version where there is a 1-1 relationship of nation parcel and nationId.
        // in the future there will be a NationRegistry and you can have multiple nation parcel using the same nationId

        // validate nation name is unique if adding a nation
        if (type == ParcelType.NATION && StringUtils.isNotBlank(nationName)) {
            // if the name was found then fail
            if (nationId != null) {
                source.sendFailure(Component.translatable(LangUtil.chat("parcel.nation.nationName_already_exists")).withStyle(ChatFormatting.RED));
                return 0;
            }
        }

        if (player != null) {
            // create a relative sized Box
            Box size = new Box(Coords.of(0, -ySizeDown, 0), Coords.of(xSize - 1, ySizeUp - 1, zSize - 1));
            ICoords coords = Coords.of(pos);
            Optional<Parcel> parcelOptional = ParcelFactory.create(type, nationId);
            if (parcelOptional.isPresent()) {
                Parcel parcel = parcelOptional.get();
                parcel.setOwnerId(player.getUUID());
                parcel.setCoords(coords);
                // TODO size should be supplied to the factory
                parcel.setSize(size);

                // TODO this should be moved to the factory
                // parcel customizations
                if (parcel.getType() == ParcelType.NATION) {
                    if (StringUtils.isNotBlank(nationName)) {
                        ((NationParcel) parcel).setName(nationName);
                    }
                    // modify the size
                    size.setMinCoords(size.getMinCoords().withY(source.getLevel().getMinBuildHeight()));
                    size.setMaxCoords(size.getMaxCoords().withY(source.getLevel().getMaxBuildHeight() - 1));
                }

                // NOTE commands override the placement rules

                /*
                 * check if parcel is within another existing parcel
                 */
                Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(coords);

                ClaimResult successfulClaim = registryParcel.map(parentParcel -> parcel.handleEmbeddedClaim(source.getLevel(), parentParcel, parcel.getBox())).orElseGet(() -> parcel.handleClaim(source.getLevel(), parcel.getBox()));
                if (successfulClaim == ClaimResult.SUCCESS) {
                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.add.success")).withStyle(ChatFormatting.GREEN), false);
                    CommandHelper.save(source.getLevel());
                } else {
                    // TODO examine the claim result to determine the correct message.
                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.add.failure_with_overlaps")).withStyle(ChatFormatting.RED), false);
                }
            } else {
                ClaimMyLand.LOGGER.error("unable to create parcel with provided nationid -> {}", nationId);
                source.sendFailure(Component.translatable(LangUtil.chat("unexpected_error")).withStyle(ChatFormatting.RED));
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return 1;
    }

    public static int abandonParcel(CommandSourceStack source, String ownerName, String parcelName) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        if (player != null) {
            List<Parcel> parcels = ParcelRegistry.findByOwner(player.getUUID());
            Optional<Parcel> parcel = parcels.stream().filter(p -> p.getName().equalsIgnoreCase(parcelName)).findFirst();
            if (parcel.isPresent()) {
                // abandon the parcel in the register
                if (ParcelRegistry.abandonParcel(parcel.get().getId())) {
                    // set the abandon time
                    parcel.get().setAbandonedTime(source.getLevel().getGameTime());
                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.abandon.success")).withStyle(ChatFormatting.GREEN), false);
                    CommandHelper.save(source.getLevel());
                } else {
                    ClaimMyLand.LOGGER.error("unable to locate parcel by owner -> {}", ownerName);
                    failure(source, "parcel.unable_to_locate");
                }
            } else {
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.abandon.failure")).withStyle(ChatFormatting.RED), false);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return 1;
    }

    /**
     * TODO flesh out to save to file(s)
     *
     * @param source
     * @return
     */
    public static int backupParcels(CommandSourceStack source) {
        String dump = ParcelRegistry.toJson();
        ClaimMyLand.LOGGER.debug("backup -> {}", dump);
        return 1;
    }

    /**
     * Ops version
     *
     * @param source
     * @param nationName
     * @param borderType
     * @return
     */
    public static int borderType(CommandSourceStack source, String nationName, String borderType) {
        try {
            // get the border type
            NationBorderType type = NationBorderType.valueOf(borderType.toUpperCase());

            // find the nation by name
            ParcelRegistry.getNations().stream()
                    .filter(n -> nationName.equalsIgnoreCase(((NationParcel) n).getName()))
                    .findFirst()
                    .ifPresentOrElse(nation -> {
                        ((NationParcel) nation).setBorderType(type);
                        CommandHelper.save(source.getLevel());
                    }, () -> {
                        failure(source, "parcel.nation.unable_to_locate");
                    });

        } catch (Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred changing nation border type:", e);
            unexceptedError(source);
        }
        return 1;
    }

    public static int claimedBy(CommandSourceStack source, BlockPos pos) {

        try {
            ICoords posCoords;

            if (pos == null) {
                posCoords = Coords.of(source.getPosition());
            } else {
                posCoords = Coords.of(pos);
            }
            List<Parcel> parcels = new ArrayList<>(ParcelRegistry.find(posCoords).stream()
                    .filter(p -> p.getType() != ParcelType.ZONE).toList());

            if (!parcels.isEmpty()) {
                List<Component> messages = new ArrayList<>();

                // sort by volume using a comparator
                parcels.sort(Parcel.volumeComparator);
                parcels.forEach(p -> {
                    // locate the player name
                    String playerName;
                    Player player = source.getLevel().getPlayerByUUID(p.getOwnerId());
                    if (player != null) {
                        playerName = player.getScoreboardName();
                    } else {
                        Optional<String> optionalPlayerName = PlayerRegistry.get(p.getOwnerId());
                        playerName = optionalPlayerName.orElse(p.getOwnerId().toString());
                    }

                    // The block at (x, y, z) is claimed by [name].
                    // Name:
                    // Type:
                    // Coords: x y z
                    // Start: x y z
                    // End: x y z
                    // Size: x y z | x u d z
                    // Area: [x]m^3
                    // Border: [type]

                    // first line
                    Component location = Component.literal(posCoords.toShortString()).withStyle(ChatFormatting.AQUA);
                    Component name = Component.literal(playerName).withStyle(ChatFormatting.AQUA);
                    Component c = Component.translatable(LangUtil.chat("parcel.claimed_by"), location, name).withStyle(ChatFormatting.GOLD);
                    messages.add(c);

                    // name line
                    messages.add(
                       Component.translatable(LangUtil.chat("parcel.name"),
                               Component.literal(p.getName()).withStyle(ChatFormatting.BLUE)).withStyle(ChatFormatting.GRAY)
                    );
                    // type
                    messages.add(
                            Component.translatable(LangUtil.chat("parcel.type"),
                                    Component.literal(p.getType().getSerializedName().toLowerCase()).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY)
                    );

                    // coords
                    messages.add(
                            Component.translatable(LangUtil.chat("parcel.coords"),
                                    Component.literal(p.getCoords().toShortString()).withStyle(ChatFormatting.GREEN)).withStyle(ChatFormatting.GRAY)
                    );

                    // start
                    messages.add(
                            Component.translatable(LangUtil.chat("parcel.start"),
                                    Component.literal(p.getAbsoluteBox().getMinCoords().toShortString()).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY)
                    );

                    // end
                    messages.add(
                            Component.translatable(LangUtil.chat("parcel.end"),
                                    Component.literal(p.getAbsoluteBox().getMaxCoords().toShortString()).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY)
                    );

                    // size
                    messages.add(
                            Component.translatable(LangUtil.chat("parcel.size"),
                                    Component.literal(ModUtil.getSize(p.getBox()).toShortString()).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY)
                    );

                    // area
                    messages.add(
                            Component.translatable(LangUtil.chat("parcel.area"),
                                    Component.literal(String.valueOf(ModUtil.getArea(p.getBox()))).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY)
                    );

                    // volume
                    messages.add(
                            Component.translatable(LangUtil.chat("parcel.volume"),
                                    Component.literal(String.valueOf(ModUtil.getVolume(p.getBox()))).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY)
                    );

                    // border
                    if (p.getType() == ParcelType.NATION) {
                        messages.add(
                                Component.translatable(LangUtil.chat("parcel.border"),
                                        Component.literal(((NationParcel)p).getBorderType().getSerializedName().toLowerCase()).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY)
                        );
                    }
                    // add newline
                    messages.add(Component.literal(LangUtil.NEWLINE));
                });

                // send all the messages
                messages.forEach(message -> {
                    source.sendSuccess(() -> message, false);
                });
            }
        } catch (Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred retrieving claimed by:", e);
            unexceptedError(source);
        }
        return 1;
    }

    /**
     *
     * @param source
     * @param ownerName
     * @param parcelName
     * @return
     */
    public static int demolishParcel (CommandSourceStack source, String ownerName, String parcelName){
        try {
            ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
            if (player != null) {
                List<Parcel> parcels = ParcelRegistry.findByOwner(player.getUUID());
                // get the parcel
                Optional<Parcel> parcel = parcels.stream().filter(p -> p.getName().equalsIgnoreCase(parcelName)).findFirst();
                if (parcel.isPresent()) {
                    // get the type
                    ParcelType type = parcel.get().getType();

                    ItemStack deed = switch (type) {
                        case PLAYER -> DeedFactory.createPlayerDeed(parcel.get().getSize());
                        case NATION -> DeedFactory.createNationDeed(source.getLevel(), parcel.get().getSize());
                        // requires the NATION_ID
                        case CITIZEN ->
                                DeedFactory.createCitizenDeed(parcel.get().getSize(), parcel.get().getNationId());
                        case ZONE -> ItemStack.EMPTY;
                    };

                    // copy props over
                    CompoundTag tag = deed.getOrCreateTag();
                    tag.putUUID(Deed.PARCEL_ID, parcel.get().getId());
                    if (parcel.get().getNationId() != null) {
                        tag.putUUID(NationDeed.NATION_ID, parcel.get().getNationId());
                    }

                    // give parcel to player
                    if (deed != ItemStack.EMPTY) {
                        player.getInventory().add(deed);
                        // remove the parcel
                        ParcelRegistry.removeParcel(parcel.get());

                        // TODO this will only work if the border stone is at coords
                        // remove any borders
                        ICoords coords = parcel.get().getCoords();
                        // check if there is a border stone
                        BlockEntity be = source.getLevel().getBlockEntity(coords.toPos());
                        if (be instanceof BorderStoneBlockEntity) {
                            ((BorderStoneBlockEntity) be).removeParcelBorder(source.getLevel(), coords);
                        }
                    }
                } else {
                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.abandon.failure")).withStyle(ChatFormatting.RED), false);
                }
            } else {
                CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
            }
        } catch (Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred demolishing a parcels:", e);
            source.sendFailure(Component.translatable(LangUtil.chat("unexpected_error")).withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    /**
     *
     * @param source
     * @param ownerName
     * @param parcelName
     * @return
     */
    public static int removeParcel (CommandSourceStack source, String ownerName, String parcelName){
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        if (player != null) {
            List<Parcel> parcels = ParcelRegistry.findByOwner(player.getUUID());
            Optional<Parcel> parcel = parcels.stream().filter(p -> p.getName().equalsIgnoreCase(parcelName)).findFirst();
            if (parcel.isPresent()) {
                // remove the border
                BlockEntity blockEntity = source.getLevel().getBlockEntity(parcel.get().getCoords().toPos());
                if (blockEntity instanceof FoundationStoneBlockEntity) {
                    ((FoundationStoneBlockEntity) blockEntity).removeParcelBorder(source.getLevel(), parcel.get().getCoords());
                }
                // unregister the parcel
                ParcelRegistry.removeParcel(parcel.get());

                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.remove.success")).withStyle(ChatFormatting.GREEN), false);
                CommandHelper.save(source.getLevel());
            } else {
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.remove.failure")).withStyle(ChatFormatting.RED), false);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return 1;
    }

    public static int renameParcel (CommandSourceStack source, String ownerName, String parcelName, String newName){
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        if (player != null) {
            List<Parcel> parcels = ParcelRegistry.findByOwner(player.getUUID());
            Optional<Parcel> parcel = parcels.stream().filter(p -> p.getName().equalsIgnoreCase(parcelName)).findFirst();
            if (parcel.isPresent()) {
                // TODO ensure that the new name is unique across ALL parcels
                parcel.get().setName(newName);
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.rename.success")).withStyle(ChatFormatting.GREEN), false);
                CommandHelper.save(source.getLevel());
            } else {
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.rename.failure")).withStyle(ChatFormatting.RED), false);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return 1;
    }

    /**
     *
     * @param source
     * @param ownerName
     * @param parcelName
     * @param newOwnerName
     * @return
     */
    public static int transferParcel (CommandSourceStack source, String ownerName, String parcelName, String
            newOwnerName){
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        if (player != null) {
            List<Parcel> parcels = ParcelRegistry.findByOwner(player.getUUID());
            Optional<Parcel> parcel = parcels.stream().filter(p -> p.getName().equalsIgnoreCase(parcelName)).findFirst();
            if (parcel.isPresent()) {
                // get the new owner
                if (StringUtils.isNotBlank(newOwnerName)) {
                    ServerPlayer newOwner = source.getServer().getPlayerList().getPlayerByName(newOwnerName);
                    if (newOwner != null) {
                        ParcelRegistry.updateOwner(parcel.get().getId(), newOwner.getUUID());
                        parcel.get().setOwnerTime(source.getLevel().getGameTime());
                    } else {
                        CommandHelper.sendUnableToLocatePlayerMessage(source, newOwnerName);
                        return -1;
                    }
                }
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.transfer.success")).withStyle(ChatFormatting.GREEN), false);
                CommandHelper.save(source.getLevel());
            } else {
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.transfer.failure")).withStyle(ChatFormatting.RED), false);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return 1;
    }

    public static int clearOwnerParcels (CommandSourceStack source, String ownerName){
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        if (player != null) {
            // remove all properties from player
            ParcelRegistry.removeParcel(source.getLevel(), player.getUUID());
            source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.remove.success")).withStyle(ChatFormatting.GREEN), false);
            CommandHelper.save(source.getLevel());
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return 1;
    }

    public static int clearAllParcels (CommandSourceStack source){
        backupParcels(source);
        // TODO add an undo/restore command
        ParcelRegistry.clear();
        // TODO message
        return 1;
    }

    // TODO move to CommandHelper
    /**
     * convenience chat method
     * @param source
     */
    private static void unexceptedError (CommandSourceStack source){
        failure(source, "unexpected_error");
    }

    private static void failure (CommandSourceStack source, String key){
        source.sendFailure(Component.translatable(LangUtil.chat(key)).withStyle(ChatFormatting.RED));
    }
}
