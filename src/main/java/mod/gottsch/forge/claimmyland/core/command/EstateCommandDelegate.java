/*
 * This file is part of Claim My Land.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
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
 *
 */

package mod.gottsch.forge.claimmyland.core.command;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.block.entity.BorderStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.estate.EstateContext;
import mod.gottsch.forge.claimmyland.core.item.Deed;
import mod.gottsch.forge.claimmyland.core.item.DeedFactory;
import mod.gottsch.forge.claimmyland.core.item.NationDeed;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.registry.EstateRegistry;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * @author by Mark Gottschling on 1/30/2026
 */
public class EstateCommandDelegate {

    public static int listEstatesByOwner(CommandSourceStack source, String ownerName) {
        Optional<UUID> playerUuid = CommandHelper.getPlayerUuid(source, ownerName);
        if (playerUuid.isPresent()) {
            return listEstatesByOwner(source, ownerName, playerUuid.get());
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return 1;
    }

    public static int listEstatesByOwner(CommandSourceStack source, ServerPlayer player) {
        return listEstatesByOwner(source, player.getName().getString(), player.getUUID());
    }

    public static int listEstatesByOwner(CommandSourceStack source, String playerName, UUID playerUuid) {
        List<Component> messages = new ArrayList<>();

        ParcelCommandDelegate.buildListTitle(messages, Component.translatable(LangUtil.chat("estate.list"), playerName));

//        formatEstateList(messages, EstateRegistry.getByOwner(playerUuid),
//                EstateRegistry.getByFriend(playerUuid));
        ParcelCommandDelegate.formatParcelList(messages, EstateRegistry.getByOwner(playerUuid),
                EstateRegistry.getByFriend(playerUuid));

        messages.forEach(component -> {
            source.sendSuccess(() -> component, false);
        });
        return 1;
    }

    @Deprecated
    static List<Component> formatEstateList(List<Component> messages, Set<Estate> estates, Set<Estate> friendsEstates) {

        if (estates.isEmpty() && friendsEstates.isEmpty()) {
            messages.add(Component.translatable(LangUtil.chat("estate.list.empty")).withStyle(ChatFormatting.GOLD));
        } else {
            estates.forEach(estate -> {
                messages.add(
                        Component.literal(estate.getName().toUpperCase()).withStyle(ChatFormatting.GOLD)
//                                .append(Component.literal(String.format(" [%s]: ", estate.getType().getSerializedName().charAt(0))).withStyle(ChatFormatting.WHITE))
//                                .append(Component.translatable(String.format("(%s) to (%s)",
//                                        formatCoords(estate.getMinCoords()),
//                                        formatCoords(estate.getMaxCoords()))).withStyle(ChatFormatting.GREEN)
//                                )
//                                .append(Component.translatable(", [" + formatCoords(ModUtil.getSize(estate.getSize())) + "]").withStyle(ChatFormatting.WHITE))
                );
                estate.getParcels().forEach(parcel -> {
                    messages.add(
                            Component.literal(LangUtil.INDENT2).append(Component.literal(String.format("[%s]: ", parcel.getType().getSerializedName().charAt(0))).withStyle(ChatFormatting.WHITE))
                                    .append(parcel.getName().toUpperCase()).withStyle(ChatFormatting.AQUA));
                });

//				[STYLE].withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + blockpos.getX() + " " + s1 + " " + blockpos.getZ())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip"))
            });
            // append all friends parcels
//            friendsEstates.forEach( parcel -> {
//                messages.add(
//                        Component.literal(parcel.getName().toUpperCase() + "*").withStyle(ChatFormatting.GRAY)
//                                .append(Component.literal(String.format(" [%s]: ", parcel.getType().getSerializedName().charAt(0))).withStyle(ChatFormatting.WHITE))
//                                .append(Component.translatable(String.format("(%s) to (%s)",
//                                        formatCoords(parcel.getMinCoords()),
//                                        formatCoords(parcel.getMaxCoords()))).withStyle(ChatFormatting.GREEN)
//                                )
//                                .append(Component.translatable(", [" + formatCoords(ModUtil.getSize(parcel.getSize())) + "]").withStyle(ChatFormatting.WHITE))
//                );
//            });
        }
        return messages;
    }

    public static int rename (CommandSourceStack source, String ownerName, String estateName, String newName){
        Optional<UUID> player = CommandHelper.getPlayerUuid(source, ownerName);
        if (player.isPresent()) {
            Set<Estate> estates = EstateRegistry.getByOwner(player.get());
            Optional<Estate> estate = estates.stream().filter(est -> est.getName().equalsIgnoreCase(estateName)).findFirst();
            if (estate.isPresent()) {
                // TODO ensure that the new name is unique across ALL estates ///////////////

                /// ////////////////////////
                estate.get().setName(newName);
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.rename.success")).withStyle(ChatFormatting.GREEN), false);
                CommandHelper.save(source.getLevel());
            } else {
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.rename.failure")).withStyle(ChatFormatting.RED), false);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return 1;
    }

    public static int removeEstate(CommandSourceStack source, String ownerName, String estateName){
        Optional<UUID> owner = CommandHelper.getPlayerUuid(source, ownerName);
        if (owner.isEmpty()) {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
            return -1;
        }

        Optional<Estate> estate = CommandHelper.getEstateByOwner(source, owner.get(), estateName);
        if (estate.isEmpty()) {
            source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.remove.failure")).withStyle(ChatFormatting.RED), false);
            return -1;
        }

        // remove all parcels
        estate.get().getParcels().forEach(parcel -> {
            // remove the border
            BlockEntity blockEntity = source.getLevel().getBlockEntity(parcel.getCoords().toPos());
            if (blockEntity instanceof FoundationStoneBlockEntity) {
                ((FoundationStoneBlockEntity) blockEntity).removeParcelBorder(source.getLevel(), parcel.getCoords());
            }
            // unregister the parcel
            ParcelRegistry.unregisterParcel(parcel);
        });
        // unregister the estate
        EstateRegistry.removeEstate(estate.get());

        source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.remove.success")).withStyle(ChatFormatting.GREEN), false);
        CommandHelper.save(source.getLevel());
        return 1;
    }

    /**
     * joins two estates together
     * @param source
     * @param ownerName
     * @param mainEstateName the estate being joined into - the joiner
     * @param otherEstateName the estated being joined - the joinee
     * @return
     */
    public static int join(CommandSourceStack source, String ownerName, String mainEstateName, String otherEstateName) {
        Optional<UUID> player = CommandHelper.getPlayerUuid(source, ownerName);
        if (player.isPresent()) {
            Set<Estate> estates = EstateRegistry.getByOwner(player.get());
            Optional<Estate> estate = estates.stream().filter(est -> est.getName().equalsIgnoreCase(mainEstateName)).findFirst();
            Optional<Estate> otherEstate = estates.stream().filter(est2 -> est2.getName().equalsIgnoreCase(otherEstateName)).findFirst();
            if (estate.isPresent() && otherEstate.isPresent()) {
                if (estate.get().getId().equals(otherEstate.get().getId())) {
                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.join.same_estate.failure")).withStyle(ChatFormatting.RED), false);
                    return 1;
                }
                // find all parcels belonging to otherEstate
                Set<Parcel> parcels = ParcelRegistry.findAllByEstateId(otherEstate.get().getId());
                parcels.forEach(parcel -> {
                    parcel.setEstate(estate.get());
                });
                // remove other estate
                EstateRegistry.removeEstate(otherEstate.get());

                source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.join.success")).withStyle(ChatFormatting.GREEN), false);
                CommandHelper.save(source.getLevel());
            } else {
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.join.failure")).withStyle(ChatFormatting.RED), false);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return 1;
    }

    public static int split(CommandSourceStack source, String ownerName, String estateName, String parcelName) {
        Optional<UUID> player = CommandHelper.getPlayerUuid(source, ownerName);
        if (player.isPresent()) {
            Set<Estate> estates = EstateRegistry.getByOwner(player.get());
            Optional<Estate> estate = estates.stream().filter(est -> est.getName().equalsIgnoreCase(estateName)).findFirst();

            List<String> names = new ArrayList<>();
            if (estate.isPresent()) {
                Set<Parcel> parcels = ParcelRegistry.findAllByEstateId(estate.get().getId());
                if (parcels.size() <= 1) {
                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.split.single_parcel.failure")).withStyle(ChatFormatting.RED), false);
                    return -1;
                }
                Optional<Parcel> parcel = parcels.stream().filter(p -> p.getName().equalsIgnoreCase(parcelName)).findFirst();
                if (parcel.isPresent()) {
                    // create new estate
                    Estate estateContext = new EstateContext();
                    estateContext.setOwnerId(estate.get().getOwnerId());
                    estateContext.setName(estate.get().defaultName(player.get()));
                    estateContext.setBlockWhitelist(estate.get().getBlockWhitelist());
                    estateContext.setBlockTagWhitelist(estate.get().getBlockTagWhitelist());
                    estateContext.setItemWhitelist(estate.get().getItemWhitelist());
                    estateContext.setItemTagWhitelist(estate.get().getItemTagWhitelist());
                    // update parcel
                    parcel.get().setEstate(estateContext);
                    // register estate
                    EstateRegistry.register(estateContext);
                } else {
                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.split.failure")).withStyle(ChatFormatting.RED), false);
                }
            } else {
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.split.failure")).withStyle(ChatFormatting.RED), false);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return 1;
    }


    /**
     * player version
     */
    public static int transfer(CommandSourceStack source, String estateName, String newOwnerName) {
        Optional<UUID> playerUuid = CommandHelper.getPlayerUuid(source);
        if (playerUuid.isPresent()) {
            return transfer(source, playerUuid.get(), estateName, newOwnerName);
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source);
        }
        return -1;
    }

    /**
     *
     * ops version
     */
    public static int transfer(CommandSourceStack source, String ownerName, String estateName, String newOwnerName) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
        if (ownerUuid.isPresent()) {
            return transfer(source, ownerUuid.get(), estateName, newOwnerName);
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return -1;
    }

    /**
     * common version
     */
//    public static int transfer(CommandSourceStack source, UUID ownerUuid, String estateName, String newOwnerName) {
//
//        Optional<Estate> estate = CommandHelper.getEstateByOwner(source, ownerUuid, estateName);
//        if (estate.isPresent()) {
//            // get the new owner
//            if (StringUtils.isNotBlank(newOwnerName)) {
//                Optional<UUID> newOwner = CommandHelper.getPlayerUuid(source, newOwnerName);
//                if (newOwner.isPresent()) {
//                    EstateRegistry.transfer(estate.get(), newOwner.get());
//                    // can only set the owner time from here since it is available via the CommandSourceStack. (unless we want to pass it to the registry call -> don't like that)
//                    estate.get().getParcels().forEach(parcel -> parcel.setOwnerTime(source.getLevel().getGameTime()));
//                } else {
//                    CommandHelper.sendUnableToLocatePlayerMessage(source, newOwnerName);
//                    return -1;
//                }
//            }
//            source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.transfer.success")).withStyle(ChatFormatting.GREEN), false);
//            CommandHelper.save(source.getLevel());
//        } else {
//            source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.transfer.failure")).withStyle(ChatFormatting.RED), false);
//        }
//        return 1;
//    }

    public static int transfer(CommandSourceStack source, UUID ownerUuid, String estateName, String newOwnerName) {
        Optional<Estate> estate = CommandHelper.getEstateByOwner(source, ownerUuid, estateName);
        if (estate.isEmpty()) {
            source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.transfer.failure"))
                    .withStyle(ChatFormatting.RED), false);
            return 1;
        }

        if (StringUtils.isBlank(newOwnerName)) {
            CommandHelper.sendUnableToLocatePlayerMessage(source, newOwnerName);
            return -1;
        }

        Optional<UUID> newOwnerUuid = CommandHelper.getPlayerUuid(source, newOwnerName);
        if (newOwnerUuid.isEmpty()) {
            CommandHelper.sendUnableToLocatePlayerMessage(source, newOwnerName);
            return -1;
        }

        transfer(source.getLevel(), estate.get(), newOwnerUuid.get());

        source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.transfer.success"))
                .withStyle(ChatFormatting.GREEN), false);
        CommandHelper.save(source.getLevel());
        return 1;
    }

    private static void transfer(ServerLevel level, Estate estate, UUID newOwnerUuid) {
        Estate newEstate = EstateRegistry.transfer(estate, newOwnerUuid);

        // update owner time for all parcels
        long gameTime = level.getGameTime();
        newEstate.getParcels().forEach(parcel -> parcel.setOwnerTime(gameTime));
    }

    public static int demolish(CommandSourceStack source, String ownerName, String estateName) {
        try {
            ServerPlayer player = CommandHelper.getPlayer(source);
            if (player == null) {
                CommandHelper.sendUnableToLocatePlayerMessage(source);
                return 1;
            }

            ClaimMyLand.LOGGER.debug("command player -> {}", player.getName().getString());

            Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
            if (ownerUuid.isEmpty()) {
                CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
                return 1;
            }

            ClaimMyLand.LOGGER.debug("owner player uuid -> {}", ownerUuid.get());

            Optional<Estate> estate = CommandHelper.getEstateByOwner(source, ownerUuid.get(), estateName);
            if (estate.isEmpty()) {
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.demolish.failure"))
                        .withStyle(ChatFormatting.RED), false);
                return 1;
            }

            // for each parcel in estate, demolish the parcel
            estate.get().getParcels().forEach(parcel -> {
                demolishParcel(source, player, parcel);
            });

        } catch (Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred demolishing an estate:", e);
            source.sendFailure(Component.translatable(LangUtil.chat("unexpected_error"))
                    .withStyle(ChatFormatting.RED));
        }
        return 1;
    }


    private static int demolishParcel(CommandSourceStack source, ServerPlayer player, Parcel parcel) {
        ItemStack deed = createDeedForParcel(source.getLevel(), parcel);

        if (deed.isEmpty()) {
            source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.demolish.zone_cannot_demolish"))
                    .withStyle(ChatFormatting.RED), false);
            return 1;
        }

        // copy props over
        CompoundTag tag = deed.getOrCreateTag();
        tag.putUUID(Deed.PARCEL_ID, parcel.getId());
        if (parcel.getNationId() != null) {
            tag.putUUID(NationDeed.NATION_ID, parcel.getNationId());
        }

        player.getInventory().add(deed);
        ParcelRegistry.unregisterParcel(parcel);
        removeBorderStone(source.getLevel(), parcel);

        source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.demolish.success"))
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static ItemStack createDeedForParcel(ServerLevel level, Parcel parcel) {
        return switch (parcel.getType()) {
            case PLAYER -> DeedFactory.createPlayerDeed(parcel.getSize());
            case NATION -> DeedFactory.createNationDeed(level, parcel.getSize());
            case CITIZEN -> DeedFactory.createCitizenDeed(parcel.getSize(), parcel.getNationId());
            case ZONE, NONE -> ItemStack.EMPTY;
        };
    }

    private static void removeBorderStone(ServerLevel level, Parcel parcel) {
        // TODO this will only work if the border stone is at coords
        ICoords coords = parcel.getCoords();
        BlockEntity be = level.getBlockEntity(coords.toPos());
        if (be instanceof BorderStoneBlockEntity borderStone) {
            borderStone.removeParcelBorder(level, coords);
        }
    }
}
