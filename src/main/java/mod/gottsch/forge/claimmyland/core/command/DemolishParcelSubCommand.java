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

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.item.Deed;
import mod.gottsch.forge.claimmyland.core.item.DeedFactory;
import mod.gottsch.forge.claimmyland.core.item.NationDeed;
import mod.gottsch.forge.claimmyland.core.parcel.NationalizedParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

import static mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper.*;

/**
 * TODO shares a lot of code with remove - should have a common parent class
 * @author by Mark Gottschling on 2/22/2026
 */
public class DemolishParcelSubCommand implements SubCommand {
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal(DEMOLISH)
                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                        .suggests(OWNER_ESTATE_NAMES)
                        .then(Commands.argument(PARCEL_NAME, StringArgumentType.string())
                                .suggests(OWNER_ESTATE_PARCEL_NAMES)
                                .executes(source -> {
                                    return demolishParcel(source.getSource(),
                                            StringArgumentType.getString(source, ESTATE_NAME),
                                            StringArgumentType.getString(source, PARCEL_NAME));
                                })
                        )
                );
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {

        return Commands.literal(DEMOLISH)
                .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                        .suggests(OPS_OWNER_NAMES)
                        .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                .suggests(OPS_OWNER_ESTATE_NAMES)
                                .suggests(OPS_OWNER_NAMES)
                                .then(Commands.argument(PARCEL_NAME, StringArgumentType.string())
                                        .suggests(OPS_OWNER_ESTATE_PARCEL_NAMES)
                                        .executes(source -> {
                                            return demolishParcel(source.getSource(),
                                                    StringArgumentType.getString(source, OWNER_NAME),
                                                    StringArgumentType.getString(source, ESTATE_NAME),
                                                    StringArgumentType.getString(source, PARCEL_NAME));
                                        })
                                )
                        )
                );
    }

    public int demolishParcel(CommandSourceStack source, String estateName, String parcelName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            return demolishParcel(source, player.getScoreboardName(), estateName, parcelName);
        } catch(Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred demonishing parcel:", e);
            unexpectedError(source);
            return -1;
        }
    }

    /**
     *
     * @param source
     * @param ownerName
     * @param parcelName
     * @return
     */
    public int demolishParcel(CommandSourceStack source, String ownerName, String estateName, String parcelName){
        try {
            ServerPlayer player = CommandHelper.getPlayer(source);
            Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);

            ClaimMyLand.LOGGER.debug("command player -> {}", player.getName().getString());
            if (ownerUuid.isEmpty()) {
                sendUnableToLocatePlayerMessage(source, ownerName);
                return -1;
            }
            ClaimMyLand.LOGGER.debug("owner player uuid -> {}", ownerUuid.get());

            // get the parcel
            Optional<Parcel> optionalParcel = CommandHelper.findParcelByOwnerEstate(source, ownerUuid.get(), estateName, parcelName);
            if (optionalParcel.isEmpty()) {
                failure(source,"parcel.unable_to_locate");
                return -1;
            }
            Parcel parcel = optionalParcel.get();

            // get the type
            ParcelType type = parcel.getType();

            // TODO refactor the DeedFactory
            ItemStack deed = switch (type) {
                case PLAYER -> DeedFactory.createPlayerDeed(parcel.getSize());
                case NATION -> DeedFactory.createNationDeed(source.getLevel(), parcel.getSize());
                // requires the NATION_ID
                case CITIZEN ->
                        DeedFactory.createCitizenDeed(parcel.getSize(), ((NationalizedParcel)parcel).getNationEstate().getId());
                case ZONE -> ItemStack.EMPTY;
                default -> ItemStack.EMPTY;
            };

            // give deed to player
            if (deed != ItemStack.EMPTY) {
                // copy props over
                CompoundTag tag = deed.getOrCreateTag();
                tag.putUUID(Deed.PARCEL_ID, parcel.getId());
                if (parcel instanceof NationalizedParcel nationalizedParcel) {
                    tag.putUUID(NationDeed.NATION_ESTATE_ID, nationalizedParcel.getNationEstate().getId());
                }

                player.getInventory().add(deed);
            }

            // remove the parcel
            removeParcel(source, parcel);

            sendSuccess(source, "parcel.demolish.success");
            save(source.getLevel());

        } catch (Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred demolishing a parcels:", e);
            failure(source, "unexpected_error");
        }
        return 1;
    }

    private void removeParcel(CommandSourceStack source, Parcel parcel) {

        // remove the border
        removeBorder(source.getLevel(), parcel);
        // unregister the parcel
        ParcelRegistry.unregisterParcel(source.getLevel(), parcel);

        // if a Nation parcel then remove all Zone tenant estates and conver all Citizen tenant estates to Player
        if (parcel.getType() == ParcelType.NATION) {
            // find all parcel within the boundary of the parcel
            List<Parcel> tenantParcels = ParcelRegistry.find(parcel.getBox()).stream().filter(p -> p instanceof NationalizedParcel).toList();
            // estate list of tenant parcels
            Set<Estate> removeEstates = new HashSet<>();
            tenantParcels.stream()
                    .map(p -> (NationalizedParcel) p)
                    .forEach(nationalizedParcel -> {
                        removeBorder(source.getLevel(), nationalizedParcel);
                        ParcelRegistry.unregisterParcel(source.getLevel(), nationalizedParcel);
                    });
        }
    }

    private void removeBorder(ServerLevel level, Parcel parcel) {
//        BlockEntity blockEntity = level.getBlockEntity(parcel.getCoords().toPos());
//        if (blockEntity instanceof FoundationStoneBlockEntity) {
//            ((FoundationStoneBlockEntity) blockEntity).removeParcelBorder(level, parcel.getCoords());
//        }
    }
}
