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
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * @author by Mark Gottschling on 2/27/2026
 */
public class SplitSubCommand implements SubCommand {
    static final String SPLIT = "split";

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal(SPLIT)
                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                        .suggests(OWNER_ESTATE_NAMES)
                        .then(Commands.argument(PARCEL_NAME, StringArgumentType.string())
                                .suggests(OWNER_ESTATE_PARCEL_NAMES_MORE_THAN_ONE)
                                .executes(source -> {
                                    return split(source.getSource(),
                                            StringArgumentType.getString(source, ESTATE_NAME),
                                            StringArgumentType.getString(source, PARCEL_NAME));
                                })
                        )
                );
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {
        return  Commands.literal(SPLIT)
                .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                        .suggests(OPS_ESTATE_OWNER_NAMES)
                        .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                .suggests(OWNER_ESTATE_NAMES)
                                .then(Commands.argument(PARCEL_NAME, StringArgumentType.string())
                                        .suggests(OPS_OWNER_ESTATE_PARCEL_NAMES)
                                        .executes(source -> {
                                            return split(source.getSource(),
                                                    StringArgumentType.getString(source, OWNER_NAME),
                                                    StringArgumentType.getString(source, ESTATE_NAME),
                                                    StringArgumentType.getString(source, PARCEL_NAME));
                                        })
                                )
                        )
                );
    }

    public static int split(CommandSourceStack source, String estateName, String parcelName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            return split(source, player.getScoreboardName(), estateName, parcelName);
        } catch(Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred joing estates:", e);
            CommandHelper.unexceptedError(source);
            return -1;
        }
    }

    public static int split(CommandSourceStack source, String ownerName, String estateName, String parcelName) {
        Optional<UUID> player = CommandHelper.getPlayerUuid(source, ownerName);
        if (player.isEmpty()) {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
            return -1;
        }

        Set<Estate> estates = EstateRegistry.findByOwner(player.get());
        Optional<Estate> estate = estates.stream().filter(est -> est.getName().equalsIgnoreCase(estateName)).findFirst();

        List<String> names = new ArrayList<>();
        if (estate.isEmpty()) {
            source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.split.failure")).withStyle(ChatFormatting.RED), false);
            return -1;
        }

        Set<Parcel> parcels = ParcelRegistry.findAllByEstateId(estate.get().getId());
        if (parcels.size() <= 1) {
            source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.split.single_parcel.failure")).withStyle(ChatFormatting.RED), false);
            return -1;
        }
        Optional<Parcel> parcel = parcels.stream().filter(p -> p.getName().equalsIgnoreCase(parcelName)).findFirst();
        if (parcel.isEmpty()) {
            source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.split.failure")).withStyle(ChatFormatting.RED), false);
            return -1;
        }

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

        return 1;
    }
}
