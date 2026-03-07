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
import mod.gottsch.forge.claimmyland.core.command.helper.CommandResponseFormatter;
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.parcel.NationalizedParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.registry.EstateRegistry;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper.*;

/**
 * @author by Mark Gottschling on 2/22/2026
 */
public class RemoveEstateSubCommand implements SubCommand {
    /*
     * player version presents nation selection first, then estate.  only displays zone estates
     */
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal(REMOVE)
                .then(Commands.argument(NATION_NAME, StringArgumentType.string())
                        .suggests(OWNER_NATION_ESTATE_NAMES)
                        .then(Commands.argument(ZONE_ESTATE_NAME, StringArgumentType.string())
                                .suggests(OWNER_ZONE_ESTATE_NAMES)

                                .executes(source -> {
                                    return remove(source.getSource(),
                                            StringArgumentType.getString(source, ZONE_ESTATE_NAME));
                                })
                        )
                );
    }

    /**
     * ops versions allows you to select any estate for removal
     */
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {
        return Commands.literal(REMOVE)
                .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                        .suggests(OPS_OWNER_NAMES)
                        .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                .suggests(OPS_OWNER_ESTATE_NAMES)
                                .executes(source -> {
                                    return remove(source.getSource(),
                                            StringArgumentType.getString(source, OWNER_NAME),
                                            StringArgumentType.getString(source, ESTATE_NAME));
                                })
                        )
                );
    }


    /*
     * player version - for zones only
     */
    public int remove(CommandSourceStack source, String estateName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            UUID ownerUuid = player.getUUID();

            Optional<Estate> estate = CommandHelper.getEstateByOwner(source, ownerUuid, estateName);
            if (estate.isEmpty()) {
                failure(source, "estate.remove.failure");
                return -1;
            }

            // ensure that the estate is a zone type
            if (!estate.get().isZone()) {
                failure(source, "estate.remove.not_zone.failure");
                return -1;
            }

            // capture parcel size
            int size = estate.get().findParcels().size();

            removeEstate(source, estate.get());

            sendLines(source,
                    CommandResponseFormatter.formatEstateDeleted(estate.get().getName(), estate.get().getId(), size));
            save(source.getLevel());
            return 1;

        } catch (Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred renaming estate:", e);
            unexpectedError(source);
            return -1;
        }
    }

    /*
     * ops version
     */
    public int remove(CommandSourceStack source, String ownerName, String estateName){
        Optional<UUID> owner = CommandHelper.getPlayerUuid(source, ownerName);
        if (owner.isEmpty()) {
           sendUnableToLocatePlayerMessage(source, ownerName);
            return -1;
        }

        Optional<Estate> estate = CommandHelper.getEstateByOwner(source, owner.get(), estateName);
        if (estate.isEmpty()) {
            sendFailure(source, "estate.remove.failure");
            return -1;
        }
        // capture parcel size
        int size = estate.get().findParcels().size();

        removeEstate(source, estate.get());

        sendLines(source,
                CommandResponseFormatter.formatEstateDeleted(estate.get().getName(), estate.get().getId(), size));
        save(source.getLevel());
        return 1;
    }

    private void removeEstate(CommandSourceStack source, Estate estate) {
        // remove all parcels
        estate.findParcels().forEach(parcel -> {
            // remove the border
            removeBorder(source.getLevel(), parcel);
            // unregister the parcel
            ParcelRegistry.unregisterParcel(source.getLevel(), parcel);
        });

        // if a Nation Estate then remove all Zone tenant estates and conver all Citizen tenant estates to Player
        if (estate.isNation()) {
            Set<Parcel> tenantParcels = ParcelRegistry.findAllByNationEstateId(estate.getId());
            Set<Estate> removeEstates = new HashSet<>();
            tenantParcels.stream()
                    .map(parcel -> (NationalizedParcel) parcel)
                    .forEach(nationalizedParcel -> {
                        removeBorder(source.getLevel(), nationalizedParcel);
                        ParcelRegistry.unregisterParcel(source.getLevel(), nationalizedParcel);
                    });
        }

        // unregister the estate (redundant)
        EstateRegistry.unregister(estate);
    }

    private void removeBorder(ServerLevel level, Parcel parcel) {
        BlockEntity blockEntity = level.getBlockEntity(parcel.getCoords().toPos());
        if (blockEntity instanceof FoundationStoneBlockEntity) {
            ((FoundationStoneBlockEntity) blockEntity).removeParcelBorder(level, parcel.getCoords());
        }
    }
}
