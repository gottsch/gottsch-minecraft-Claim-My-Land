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

package mod.gottsch.neo.claimmyland.core.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.neo.claimmyland.core.estate.Estate;
import mod.gottsch.neo.claimmyland.core.item.Deed;
import mod.gottsch.neo.claimmyland.core.item.DeedFactory;
import mod.gottsch.neo.claimmyland.core.parcel.NationalizedParcel;
import mod.gottsch.neo.claimmyland.core.parcel.Parcel;
import mod.gottsch.neo.claimmyland.core.registry.ParcelRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;
import java.util.UUID;

import static mod.gottsch.neo.claimmyland.core.command.helper.CommandHelper.*;

/**
 * TODO shares a lot of code with remove - should have a common parent class
 *
 * @author by Mark Gottschling on 2/27/2026
 */
public class DemolishEstateSubCommand implements SubCommand {
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal(DEMOLISH)
                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                        .suggests(OWNER_ESTATE_NAMES)
                            .executes(source -> {
                                return demolishEstate(source.getSource(),
                                        StringArgumentType.getString(source, ESTATE_NAME));
                            })
                );
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {

        return Commands.literal(DEMOLISH)
                .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                        .suggests(OPS_OWNER_NAMES)
                        .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                .suggests(OPS_OWNER_ESTATE_NAMES)
                                .then(Commands.argument(PARCEL_NAME, StringArgumentType.string())
                                        .suggests(OPS_OWNER_ESTATE_PARCEL_NAMES)
                                        .executes(source -> {
                                            return demolish(source.getSource(),
                                                    StringArgumentType.getString(source, OWNER_NAME),
                                                    StringArgumentType.getString(source, ESTATE_NAME));
                                        })
                                )
                        )
                );
    }

    public int demolishEstate(CommandSourceStack source, String estateName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            return demolish(source, player.getScoreboardName(), estateName);
        } catch(Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred demolishing estate:", e);
            unexpectedError(source);
            return 0;
        }
    }

    public int demolish(CommandSourceStack source, String ownerName, String estateName) {
        try {
            ServerPlayer player = CommandHelper.getPlayer(source);

            Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
            if (ownerUuid.isEmpty()) {
               sendUnableToLocatePlayerMessage(source, ownerName);
                return -1;
            }

            Optional<Estate> estate = CommandHelper.getEstateByOwner(source, ownerUuid.get(), estateName);
            if (estate.isEmpty()) {
                failure(source, "estate.demolish.failure");
                return -1;
            }

            // for each parcel in estate, demolish the parcel
            estate.get().findParcels().forEach(parcel -> {
                demolishParcel(source, player, parcel);
            });
            // cleanup nation tenant parcels (Citizens, Zones)
            CommandHelper.cleanupNationTenants(source.getLevel(), estate.get());

            save(source.getLevel());

        } catch (Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred demolishing an estate:", e);
            unexpectedError(source);
        }
        return -1;
    }

    private int demolishParcel(CommandSourceStack source, ServerPlayer player, Parcel parcel) {
        ItemStack deed = createDeedForParcel(source.getLevel(), parcel);

        if (deed.isEmpty()) {
            failure(source, "estate.demolish.zone_cannot_demolish");
            return -1;
        }

        // copy props over
        CompoundTag tag = new CompoundTag();
        tag.putUUID(Deed.PARCEL_ID, parcel.getId());
        deed.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        if (parcel instanceof NationalizedParcel nationalizedParcel) {
            tag.putUUID(Deed.NATION_ESTATE_ID, nationalizedParcel.getNationEstate().getId());
        }
        player.getInventory().add(deed);

        ParcelRegistry.unregisterParcel(source.getLevel(), parcel);
        removeBorderStone(source.getLevel(), parcel);

        sendSuccess(source, "estate.demolish.success");
        return 1;
    }

    private ItemStack createDeedForParcel(ServerLevel level, Parcel parcel) {
        return switch (parcel.getType()) {
            case PLAYER -> DeedFactory.createPlayerDeed(parcel.getSize());
            case NATION -> DeedFactory.createNationDeed(level, parcel.getSize());
            case CITIZEN -> DeedFactory.createCitizenDeed(parcel.getSize(), parcel.getEstate().getId(), parcel.getEstate().getName());
            case ZONE, NONE -> ItemStack.EMPTY;
        };
    }

    private void removeBorderStone(ServerLevel level, Parcel parcel) {
//        // NOTE this will only work if the border stone is at coords
//        ICoords coords = parcel.getCoords();
//        BlockEntity be = level.getBlockEntity(coords.toPos());
//        if (be instanceof BorderStoneBlockEntity borderStone) {
//            borderStone.removeParcelBorder(level, coords);
//        }
    }
}
