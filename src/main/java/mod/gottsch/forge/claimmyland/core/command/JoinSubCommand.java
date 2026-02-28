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
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.registry.EstateRegistry;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * @author by Mark Gottschling on 2/23/2026
 */
public class JoinSubCommand implements SubCommand {
    static final String JOIN = "join";

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal(JOIN)
                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                        .suggests(OWNER_ESTATE_NAMES)
                        .then(Commands.argument(OTHER_ESTATE_NAME, StringArgumentType.string())
                                .suggests(OWNER_ESTATE_NAMES_MINUS_SELF)
                                .executes(source -> {
                                    return join(source.getSource(),
                                            StringArgumentType.getString(source, ESTATE_NAME),
                                            StringArgumentType.getString(source, OTHER_ESTATE_NAME));
                                })
                        )
                );
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {
        return Commands.literal(JOIN)
                .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                        .suggests(OPS_ESTATE_OWNER_NAMES)
                        .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                .suggests(OWNER_ESTATE_NAMES)
                                .then(Commands.argument(OTHER_ESTATE_NAME, StringArgumentType.string())
                                        .suggests(OPS_OWNER_ESTATE_NAMES_MINUS_SELF)
                                        .executes(source -> {
                                            return join(source.getSource(),
                                                    StringArgumentType.getString(source, OWNER_NAME),
                                                    StringArgumentType.getString(source, ESTATE_NAME),
                                                    StringArgumentType.getString(source, OTHER_ESTATE_NAME));
                                        })
                                )
                        )

                );
    }

    public static int join(CommandSourceStack source, String mainEstateName, String otherEstateName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            return join(source, player.getScoreboardName(), mainEstateName, otherEstateName);
        } catch(Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred joining estates:", e);
            CommandHelper.unexceptedError(source);
            return 0;
        }
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
        Optional<UUID> owner = CommandHelper.getPlayerUuid(source, ownerName);
        if (owner.isEmpty()) {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
            return -1;
        }
        // didn't use CommandHelper.getEstateByOwner() because that would search 2x for the estates
        Set<Estate> estates = EstateRegistry.findByOwner(owner.get());
        Optional<Estate> estate = estates.stream().filter(est -> est.getName().equalsIgnoreCase(mainEstateName)).findFirst();
        Optional<Estate> otherEstate = estates.stream().filter(est2 -> est2.getName().equalsIgnoreCase(otherEstateName)).findFirst();

        if (estate.isEmpty() || otherEstate.isEmpty()) {
            // TODO could not locate
            source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.join.failure")).withStyle(ChatFormatting.RED), false);
            return -1;
        }

        // check if attempting to join an estate to itself
//        if (estate.get().getId().equals(otherEstate.get().getId())) {
//            source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.join.same_estate.failure")).withStyle(ChatFormatting.RED), false);
//            return -1;
//        }

        // test is the estates are like-estates ie only player estate can join player estates
        if (!estate.get().canJoin(otherEstate.get())) {
//            source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.join.not_like.failure")).withStyle(ChatFormatting.RED), false);
            source.sendSuccess(
                    () -> Component.translatable(LangUtil.chat("estate.join.invalid.failure")).withStyle(ChatFormatting.RED), false);

            // get and format invalid reasons
            Component reasons = Component.translatable(LangUtil.chat("estate.join.invalid.reasons"));
            for (String s : reasons.getString().split("~")) {
                source.sendSuccess(() -> Component.literal(LangUtil.INDENT2)
                        .append(Component.translatable(s).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC)).append(LangUtil.NEWLINE), false);
            }
            return -1;
        }

        // find all parcels belonging to mainEstate
        Set<Parcel> mainParcels = estate.get().findParcels();

        // find all parcels belonging to otherEstate
        Set<Parcel> parcels = otherEstate.get().findParcels();//ParcelRegistry.findAllByEstateId(otherEstate.get().getId());
        parcels.forEach(parcel -> {
            // unregister the target parcel
            ParcelRegistry.unregisterParcel(parcel);
            // update the estate
            parcel.setEstate(estate.get());

            boolean nameExists = mainParcels.stream()
                    .anyMatch(parcel1 -> parcel1.getName().equalsIgnoreCase(parcel.getName()));

            if (nameExists) {
                parcel.setName(parcel.defaultName(source.getLevel(), owner.get()));
            }

            // re-register the target parcel
            ParcelRegistry.register(parcel);
        });

        // remove other estate
        EstateRegistry.unregister(otherEstate.get());

        source.sendSuccess(() -> Component.translatable(LangUtil.chat("estate.join.success")).withStyle(ChatFormatting.GREEN), false);
        CommandHelper.save(source.getLevel());

        return 1;
    }

}
