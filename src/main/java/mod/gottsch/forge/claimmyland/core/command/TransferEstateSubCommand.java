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
import mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.estate.EstateContext;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.registry.EstateRegistry;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.UUID;

/**
 * @author by Mark Gottschling on 2/22/2026
 */
public class TransferEstateSubCommand implements SubCommand {
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal(TRANSFER)

                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                        .suggests(OWNER_ESTATE_NAMES)
                        .then(Commands.argument(NEW_OWNER_NAME, StringArgumentType.string())
                                .suggests(PLAYER_NAMES)
                                .executes(source -> {
                                    return transfer(source.getSource(),
                                            StringArgumentType.getString(source, ESTATE_NAME),
                                            StringArgumentType.getString(source, NEW_OWNER_NAME));
                                })
                        )

                );
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {
        return Commands.literal(TRANSFER)
                .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                        .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                        .suggests(OPS_OWNER_ESTATE_NAMES)
                                        .then(Commands.argument(NEW_OWNER_NAME, StringArgumentType.string())
                                                .suggests(PLAYER_NAMES)
                                                .executes(source -> {
                                                    return transfer(source.getSource(),
                                                            StringArgumentType.getString(source, OWNER_NAME),
                                                            StringArgumentType.getString(source, ESTATE_NAME),
                                                            StringArgumentType.getString(source, NEW_OWNER_NAME));
                                                })
                                        )
                                )
                        )

                );
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
        newEstate.findParcels().forEach(parcel -> parcel.setOwnerTime(gameTime));
    }

    // TODO needs to go somewhere common - CommandHelper? or ParcelRegistry?
    private static void transferParcelOwnership(ServerLevel level, Parcel parcel, UUID newOwnerUuid) {
        // unregister parcel
        ParcelRegistry.unregisterParcel(parcel);

        // create new estate and update parcel
        Estate estate = new EstateContext(newOwnerUuid);
        parcel.setEstate(estate);
        parcel.setOwnerTime(level.getGameTime());

        // re-register parcel
        ParcelRegistry.register(parcel);
    }
}
