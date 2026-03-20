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

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.forge.claimmyland.core.command.helper.ParcelDisplayFormatter;
import mod.gottsch.forge.claimmyland.core.estate.NationEstate;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.registry.PlayerRegistry;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper.*;

/**
 * Displays the ownership and parcel details for a given position.
 *
 * @author Mark Gottschling on 3/4/2026
 */
public class ClaimedBySubCommand implements SubCommand {
    public static final String CLAIMED_BY = "claimed_by";

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal(CLAIMED_BY)
                .executes(source -> claimedBy(source.getSource(), null))
                .then(Commands.argument(POS, BlockPosArgument.blockPos())
                        .executes(source -> claimedBy(source.getSource(), BlockPosArgument.getBlockPos(source, POS)))
                );
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {
        return build();
    }

    public static int claimedBy(CommandSourceStack source, BlockPos pos) {
        try {
            ICoords posCoords = pos == null
                    ? Coords.of(source.getPosition())
                    : Coords.of(pos);

            List<Parcel> parcels = new ArrayList<>(ParcelRegistry.find(posCoords).stream()
                    .filter(p -> p.getType() != ParcelType.ZONE).toList());

            if (parcels.isEmpty()) {
                sendSuccess(source, "parcel.claimed_by.not_claimed", posCoords.toShortString());
                return 1;
            }

            // sort by volume — most-significant (largest) first
            parcels.sort(Parcel.volumeComparator);

            List<Component> lines = new ArrayList<>();
            for (int i = 0; i < parcels.size(); i++) {
                Parcel p = parcels.get(i);

                String borderType = p.getType() == ParcelType.NATION
                        ? ((NationEstate) p.getEstate()).getAccessType().getSerializedName().toLowerCase()
                        : null;

                lines.addAll(ParcelDisplayFormatter.formatClaimedBy(
                        p.getEstate().getName(),
                        resolvePlayerName(source, p),
                        p.getType().getSerializedName().toLowerCase(),
                        p.getCoords().toShortString(),
                        p.getAbsoluteBox().getMinCoords().toShortString(),
                        p.getAbsoluteBox().getMaxCoords().toShortString(),
                        ModUtil.getSize(p.getBox()).toShortString(),
                        borderType,
                        p.getDimension()
                ));

                // blank line between parcels, not after the last one
                if (i < parcels.size() - 1) {
                    lines.add(Component.literal(""));
                }
            }

            sendLines(source, lines);

        } catch (Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred retrieving claimed by:", e);
            unexpectedError(source);
        }
        return 1;
    }

    /**
     * Resolves a human-readable player name from the parcel's owner UUID.
     * Falls back to the raw UUID string if the player cannot be found.
     */
    private static String resolvePlayerName(CommandSourceStack source, Parcel parcel) {
        Player onlinePlayer = source.getLevel().getPlayerByUUID(parcel.getOwnerId());
        if (onlinePlayer != null) {
            return onlinePlayer.getScoreboardName();
        }

        Optional<String> registryName = PlayerRegistry.get(parcel.getOwnerId());
        return registryName.orElse(parcel.getOwnerId().toString());
    }
}