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
import mod.gottsch.neo.claimmyland.core.command.helper.CommandResponseFormatter;
import mod.gottsch.neo.claimmyland.core.estate.Estate;
import mod.gottsch.neo.claimmyland.core.estate.EstateTypeRegistry;
import mod.gottsch.neo.claimmyland.core.parcel.Parcel;
import mod.gottsch.neo.claimmyland.core.registry.ParcelRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

import static mod.gottsch.neo.claimmyland.core.command.helper.CommandHelper.*;


/**
 * relinquish a Citizen parcel.
 * citizen parcels are contained within Nation estates.
 * @author by Mark Gottschling on 2/17/2026
 */
public class RelinquishParcelSubCommand implements SubCommand {

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal(RELINQUISH)
                .then(Commands.argument(NATION_NAME, StringArgumentType.string())
                        .suggests(OWNER_ESTATE_NATION_NAMES)
                        .then(Commands.argument(CITIZEN_ESTATE_NAME, StringArgumentType.string())
                                .suggests(OWNER_CITIZEN_ESTATE_NAMES)
                                .then(Commands.argument(PARCEL_NAME, StringArgumentType.string())
                                        .suggests(OWNER_CITIZEN_PARCEL_NAMES)
                                        .executes(source -> {
                                            return relinquishParcel(source.getSource(),
                                                    StringArgumentType.getString(source, CITIZEN_ESTATE_NAME),
                                                    StringArgumentType.getString(source, PARCEL_NAME));
                                        })
                                )
                        )
                );
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {
        return Commands.literal(RELINQUISH)
                .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                        .suggests(OPS_OWNER_NAMES)
                        .then(Commands.argument(NATION_NAME, StringArgumentType.string())
                                .suggests(OPS_OWNER_ESTATE_NATION_NAMES)
                                .then(Commands.argument(CITIZEN_ESTATE_NAME, StringArgumentType.string())
                                        .suggests(OPS_OWNER_CITIZEN_ESTATE_NAMES)
                                        .then(Commands.argument(PARCEL_NAME, StringArgumentType.string())
                                                .suggests(OPS_OWNER_CITIZEN_PARCEL_NAMES)
                                                .executes(source -> {
                                                    return relinquishParcel(source.getSource(),
                                                            StringArgumentType.getString(source, OWNER_NAME),
                                                            StringArgumentType.getString(source, CITIZEN_ESTATE_NAME),
                                                            StringArgumentType.getString(source, PARCEL_NAME));
                                                })
                                        )
                                )
                        )
                );
    }

    // player version
    public static int relinquishParcel(CommandSourceStack source, String estateName, String parcelName) {
        ServerPlayer player = source.getPlayer();
        return relinquishParcel(source, player.getScoreboardName(), estateName, parcelName);
    }

    // ops version
    public static int relinquishParcel(CommandSourceStack source, String ownerName, String estateName, String parcelName) {
        Optional<UUID> player = getPlayerUuid(source, ownerName);
        if (player.isEmpty()) {
            sendUnableToLocatePlayerMessage(source, ownerName);
            return -1;
        }

        // get the original estate
        Optional<Estate> optionalEstate = CommandHelper.getEstateByOwner(source, player.get(), estateName);
        if (optionalEstate.isEmpty()) {
            failure(source, "parcel.relinquish.failure");
            return -1;
        }
        Estate estate = optionalEstate.get();

        // get the parcel in the estate
        Optional<Parcel> optionalParcel = estate.findParcels().stream().filter(p -> p.getName().equalsIgnoreCase(parcelName)).findFirst();
        if (optionalParcel.isEmpty()) {
            failure(source, "parcel.relinquish.failure");
            return -1;
        }
        Parcel parcel = optionalParcel.get();

        if (!estate.canRelinquish()) {
            CommandHelper.sendLines(source,
                    CommandResponseFormatter.formatFailureWithReasons(
                            "estate.relinquish.disallowed.failure",
                            "estate.relinquish.disallowed.reasons"));
            return -1;
        }

        try {
            // unregister parcel
            ParcelRegistry.unregisterParcel(source.getLevel(), parcel);

            // save old estate
            Estate oldEstate = parcel.getEstate();

            // create new estate (clears all whitelists)
//            Estate newEstate = new EstateContext(estate.getOwnerId());
            Estate newEstate = EstateTypeRegistry.create(estate.getType());
            newEstate.setOwnerId(estate.getOwnerId());
            // NOTE do NOT setName() to the oldEstate name. that can cause duplicates in the EstateRegistry.
            // generate a unique name for the split-off estate. Suggestion providers key on
            // estate name, so reusing the old name causes two estates to collapse to one
            // entry in command suggestions. defaultName(ownerId) uses the authoritative
            // PlayerRegistry counter and is collision-free by construction.
            newEstate.setName(newEstate.defaultName(source.getLevel(), estate.getOwnerId()));
//            newEstate.setName(oldEstate.getName());
            newEstate.setParcelType(oldEstate.getParcelType());

            // mark as relinquished
            newEstate.setRelinquished(true);
            // update parcel with estate
            parcel.setEstate(newEstate);

            // re-register parcel
            ParcelRegistry.register(source.getLevel(), parcel);

            // set the abandon time
            sendSuccess(source, "parcel.relinquish.success");
            save(source.getLevel());

        } catch(Exception e) {
            ClaimMyLand.LOGGER.error("unable to relinquish parcel -> {}", parcel.getId());
            failure(source, "parcel.relinquish.failure");
        }
        return 1;
    }
}
