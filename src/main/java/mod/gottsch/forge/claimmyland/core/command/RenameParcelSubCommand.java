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
import mod.gottsch.forge.claimmyland.core.parcel.NationalizedParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * @author by Mark Gottschling on 2/19/2026
 */
public class RenameParcelSubCommand implements SubCommand {
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal(RENAME)
                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                        .suggests(OWNER_ESTATE_NAMES)
                        .then(Commands.argument(PARCEL_NAME, StringArgumentType.string())
                                .suggests(OWNER_ESTATE_PARCEL_NAMES)
                                .then(Commands.argument(NEW_NAME, StringArgumentType.string())
                                        .executes(source -> {
                                            return rename(source.getSource(),
                                                    StringArgumentType.getString(source, ESTATE_NAME),
                                                    StringArgumentType.getString(source, PARCEL_NAME),
                                                    StringArgumentType.getString(source, NEW_NAME));
                                        })
                                )
                        )
                );
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {
        return Commands.literal(RENAME)
                .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                        .suggests(OPS_OWNER_NAMES)
                        .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                .suggests(OWNER_ESTATE_PARCEL_NAMES)
                                .then(Commands.argument(PARCEL_NAME, StringArgumentType.string())
                                        .suggests(OPS_PARCEL_NAMES)
                                        .then(Commands.argument(NEW_NAME, StringArgumentType.string())
                                                .executes(source -> {
                                                    return rename(source.getSource(),
                                                            StringArgumentType.getString(source, OWNER_NAME),
                                                            StringArgumentType.getString(source, ESTATE_NAME),
                                                            StringArgumentType.getString(source, PARCEL_NAME),
                                                            StringArgumentType.getString(source, NEW_NAME));
                                                })
                                        )
                                )
                        )
                );
    }

    public static int rename(CommandSourceStack source, String estateName, String parcelName, String newName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            return rename(source, player.getScoreboardName(), estateName, parcelName, newName);
        } catch(Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred renaming parcel:", e);
            CommandHelper.unexpectedError(source);
            return 0;
        }
    }

    public static int rename(CommandSourceStack source, String ownerName, String estateName, String parcelName, String newName){
        Optional<UUID> player = CommandHelper.getPlayerUuid(source, ownerName);

        if (player.isEmpty()) {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
            return -1;
        }


        List<Parcel> parcels = ParcelRegistry.findByOwner(player.get());
        Optional<Parcel> optionalParcel = parcels.stream().filter(p -> p.getEstate().getName().equalsIgnoreCase(estateName)
                && p.getName().equalsIgnoreCase(parcelName)).findFirst();
        if (optionalParcel.isEmpty()) {
            source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.rename.failure")).withStyle(ChatFormatting.RED), false);
            return -1;
        }
        // ensure parcels have unique names within the same estate
        Parcel parcel = optionalParcel.get();
        if (ParcelRegistry.hasName(parcel, newName)) {
            source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.rename.exists.failure")).withStyle(ChatFormatting.RED), false);
            return -1;
        }

        // if nationalized parcel, ensure parcel name is unique within the nation
        if (parcel instanceof NationalizedParcel nationalizedParcel) {
            Set<Parcel> nationParcels = ParcelRegistry.findAllByNationEstateId(nationalizedParcel.getNationEstate().getId());
            if (ParcelRegistry.hasName(nationParcels, parcel, newName)) {
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.rename.nation_parcel_exists.failure")).withStyle(ChatFormatting.RED), false);
                return -1;
            }
        }

        // update name
        parcel.setName(newName.replace(" ", "_"));
        source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.rename.success")).withStyle(ChatFormatting.GREEN), false);
        CommandHelper.save(source.getLevel());

        return 1;
    }

}
