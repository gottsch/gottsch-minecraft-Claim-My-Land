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
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.forge.claimmyland.core.command.helper.CommandResponseFormatter;
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.estate.EstateTypeRegistry;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

import static mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper.*;


/**
 * relinquish a Citizen estate.
 * citizen parcels are contained within Nation estates.
 * @author by Mark Gottschling on 2/17/2026
 */
public class RelinquishEstateSubCommand implements SubCommand {

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal(RELINQUISH)
                .then(Commands.argument(NATION_NAME, StringArgumentType.string())
                        .suggests(OWNER_ESTATE_NATION_NAMES)
                        .then(Commands.argument(CITIZEN_ESTATE_NAME, StringArgumentType.string())
                                .suggests(OWNER_CITIZEN_ESTATE_NAMES)
                                .executes(source -> {
                                    return relinquish(source.getSource(),
                                            StringArgumentType.getString(source, CITIZEN_ESTATE_NAME));
                                })
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
                                        .executes(source -> {
                                            return relinquish(source.getSource(),
                                                    StringArgumentType.getString(source, OWNER_NAME),
                                                    StringArgumentType.getString(source, CITIZEN_ESTATE_NAME));
                                        })
                                )
                        )
                );
    }

    // player version
    public static int relinquish(CommandSourceStack source, String estateName) {
        ServerPlayer player = source.getPlayer();
        return relinquish(source, player.getScoreboardName(), estateName);
    }

    // common version
    public static int relinquish(CommandSourceStack source, String ownerName, String estateName) {
        Optional<UUID> player = getPlayerUuid(source, ownerName);
        if (player.isEmpty()) {
            sendUnableToLocatePlayerMessage(source, ownerName);
            return -1;
        }

        Optional<Estate> optionalEstate = CommandHelper.getEstateByOwner(source, player.get(), estateName);
        if (optionalEstate.isEmpty()) {
            failure(source, "estate.relinquish.failure");
            return -1;
        }

        Estate estate = optionalEstate.get();
        if (!estate.canRelinquish()) {
            sendLines(source,
                    CommandResponseFormatter.formatFailureWithReasons(
                            "estate.relinquish.disallowed.failure",
                            "estate.relinquish.disallowed.reasons"));
            return -1;
        }

        estate.findParcels().forEach(parcel -> {
            try {
                // unregister parcel
                ParcelRegistry.unregisterParcel(source.getLevel(), parcel);

                // save old estate
                Estate oldEstate = parcel.getEstate();

                // create new estate (clears all whitelists)
                Estate newEstate = EstateTypeRegistry.create(estate.getType());
                newEstate.setOwnerId(estate.getOwnerId());
                newEstate.setName(oldEstate.getName());
                newEstate.setParcelType(oldEstate.getParcelType());

                // mark as relinquished
                newEstate.setRelinquished(true);
                // update parcel with estate
                parcel.setEstate(newEstate);

                // re-register
                ParcelRegistry.register(source.getLevel(), parcel);

            } catch (Exception e) {
                ClaimMyLand.LOGGER.error("unable to relinquish estate -> {}", parcel.getId());
                // TODO should this message be sent as multiple parcels are being abandoned ??
                failure(source, "estate.relinquish.failure");
            }
        });

        sendSuccess(source, "estate.relinquish.success");
        save(source.getLevel());

        return 1;
    }
}
