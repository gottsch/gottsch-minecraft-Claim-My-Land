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
import com.mojang.brigadier.suggestion.SuggestionProvider;
import mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.estate.NationEstate;
import mod.gottsch.forge.claimmyland.core.parcel.NationalizedParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.EstateRegistry;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author by Mark Gottschling on 2/17/2026
 */
public interface SubCommand {
    static final String ACCESS_TYPE ="access_type" ;
    static final String ADD = "add";
    static final String BLOCK = "block";
    static final String BLOCK_NAME = "block_name";
    static final String BY_OWNER = "by_owner";
    static final String CITIZEN_ESTATE_NAME = "citizen_estate_name";
    static final String DEMOLISH = "demolish";
    static final String DETAILS = "details";
    static final String ENTITY = "entity";
    static final String ENTITIES = "entities";
    static final String ESTATE_NAME = "estate_name";
    static final String FRIEND_NAME = "friend_name";
    static final String FRIENDS = "friends";
    static final String ITEM = "item";
    static final String ITEM_NAME = "item_name";
    static final String ITEM_TAG = "item_tag";
    static final String LIST = "list";
    static final String NATION_NAME = "nation_name";
    static final String NEW = "new";
    static final String NEW_NAME = "new_name";
    static final String NEW_OWNER_NAME = "new_owner_name";
    static final String OWNER_NAME = "owner_name";
    static final String OTHER_ESTATE_NAME = "other_estate_name";
    static final String PARCEL_NAME = "parcel_name";
    static final String POS = "pos";
    static final String RELINQUISH = "relinquish";
    static final String REMOVE = "remove";
    static final String RENAME = "rename";
    static final String TAG_NAME = "tag_name";
    static final String TRANSFER = "transfer";
    static final String ZONE_ESTATE_NAME = "zone_estate_name";

    LiteralArgumentBuilder<CommandSourceStack> build();

    LiteralArgumentBuilder<CommandSourceStack> buildOps();

    static final SuggestionProvider<CommandSourceStack> PLAYER_NAMES = (source, builder) -> {
        List<String> names = source.getSource().getLevel().getServer().getPlayerList().getPlayers().stream().map(p -> p.getName().getString()).toList();
        return SharedSuggestionProvider.suggest(names, builder);
    };

    static final SuggestionProvider<CommandSourceStack> OPS_CURRENT_FRIENDS_NAMES = (source, builder) -> {
        String estateName = StringArgumentType.getString(source, ESTATE_NAME);
        String ownerName = StringArgumentType.getString(source, OWNER_NAME);
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource(), ownerName);

        List<String> list = new ArrayList<>();
        if (ownerUuid.isPresent()) {
            Optional<Set<UUID>> friendsUuids = getFriendsWhitelist(source.getSource(), ownerUuid.get(), estateName);
            friendsUuids.ifPresent(uuids -> uuids.forEach(uuid -> {
                Optional<String> name = CommandHelper.getPlayerName(source.getSource(), uuid);
                name.ifPresent(list::add);
            }));
        }
        return SharedSuggestionProvider.suggest(list, builder);
    };

    /*
     * names of owners of estates
     */
    static final SuggestionProvider<CommandSourceStack> OPS_ESTATE_OWNER_NAMES = (source, builder) -> {
        PlayerList playerList = source.getSource().getServer().getPlayerList();
        List<String> names = EstateRegistry.getOwnerIds().stream().map(id -> {
            // TODO get offline player or use registry
            ServerPlayer player = playerList.getPlayer(id);
            return player != null ? player.getName().getString() : "";
        }).toList();

        return SharedSuggestionProvider.suggest(names, builder);
    };

    static final SuggestionProvider<CommandSourceStack> OPS_OWNER_NAMES = (source, builder) -> {
        // TODO get every owner from the ParcelRegistry
        // TODO get every register parcel owner and get the names - merge with online players
        PlayerList playerList = source.getSource().getServer().getPlayerList();
        List<String> names = ParcelRegistry.getOwnerIds().stream().map(id -> {
            ServerPlayer player = playerList.getPlayer(id);
            return player != null ? player.getName().getString() : "";
        }).toList();

        return SharedSuggestionProvider.suggest(names, builder);
    };

    /*
     * names of estates by owner
     */
    static final SuggestionProvider<CommandSourceStack> OPS_OWNER_ESTATE_NAMES = (source, builder) -> {
        String ownerName = StringArgumentType.getString(source, OWNER_NAME);
        List<String> estates = new ArrayList<>();
        Optional<UUID> playerUuid = CommandHelper.getPlayerUuid(source.getSource(), ownerName);
        if (playerUuid.isPresent()) {
            estates = EstateRegistry.findByOwner(playerUuid.get()).stream()
                    .map(Estate::getName)
                    .map(StringArgumentType::escapeIfRequired)
                    .toList();
        }
        return SharedSuggestionProvider.suggest(estates, builder);
    };

    static final SuggestionProvider<CommandSourceStack> OPS_OWNER_ESTATE_NAMES_MINUS_SELF = (source, builder) -> {
        String ownerName = StringArgumentType.getString(source, OWNER_NAME);
        String primaryEstateName = StringArgumentType.getString(source, ESTATE_NAME);
        List<String> estates = new ArrayList<>();
        Optional<UUID> playerUuid = CommandHelper.getPlayerUuid(source.getSource(), ownerName);
        if (playerUuid.isPresent()) {
            estates = EstateRegistry.findByOwner(playerUuid.get()).stream()
                    .map(Estate::getName)
                    .filter(name -> !name.equalsIgnoreCase(primaryEstateName))
                    .map(StringArgumentType::escapeIfRequired)
                    .toList();
        }
        return SharedSuggestionProvider.suggest(estates, builder);
    };

    static final SuggestionProvider<CommandSourceStack>
            OPS_OWNER_NATION_ESTATE_NAMES = (source, builder) -> {
        String ownerName = StringArgumentType.getString(source, OWNER_NAME);

        List<String> names = new ArrayList<>();
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource(), ownerName);
        if (ownerUuid.isPresent()) {
            names = EstateRegistry.findByOwner(ownerUuid.get()).stream()
                    .filter(estate -> estate instanceof NationEstate)
                    .map((Estate::getName))
                    .map(StringArgumentType::escapeIfRequired)
                    .toList();
        }
        return SharedSuggestionProvider.suggest(names, builder);
    };

    static final SuggestionProvider<CommandSourceStack>
            OPS_OWNER_ESTATE_NATION_NAMES = (source, builder) -> {
        String ownerName = StringArgumentType.getString(source, OWNER_NAME);

        Set<String> names = new HashSet<>();
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource(), ownerName);
        if (ownerUuid.isPresent()) {
            names = ParcelRegistry.findByOwner(ownerUuid.get()).stream()
                    .filter(parcel -> parcel instanceof NationalizedParcel)
                    .map(parcel -> (NationalizedParcel) parcel)
                    .map(nationalizedParcel -> nationalizedParcel.getNationEstate().getName())
                    .map(StringArgumentType::escapeIfRequired)
                    .collect(Collectors.toSet());
        }
        return SharedSuggestionProvider.suggest(names, builder);
    };

    public static final SuggestionProvider<CommandSourceStack>
            OPS_OWNER_CITIZEN_ESTATE_NAMES = (source, builder) -> {
        String nationName = StringArgumentType.getString(source, NATION_NAME);
        String ownerName = StringArgumentType.getString(source, OWNER_NAME);

        Set<String> names = new HashSet<>();
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource(), ownerName);
        if (ownerUuid.isPresent()) {
            names = ParcelRegistry.findByOwner(ownerUuid.get()).stream()
                    .filter(parcel -> parcel.getType() == ParcelType.CITIZEN)
                    .map(parcel -> (NationalizedParcel) parcel)
                    .filter(nationalizedParcel -> nationalizedParcel.getNationEstate().getName().equalsIgnoreCase(nationName))
                    .map(nationalizedParcel -> nationalizedParcel.getEstate().getName())
                    .map(StringArgumentType::escapeIfRequired)
                    .collect(Collectors.toSet());
        }
        return SharedSuggestionProvider.suggest(names, builder);
    };

    static final SuggestionProvider<CommandSourceStack>
            OPS_OWNER_CITIZEN_PARCEL_NAMES = (source, builder) -> {
        String ownerName = StringArgumentType.getString(source, OWNER_NAME);
        String estateName = StringArgumentType.getString(source, CITIZEN_ESTATE_NAME);
        Set<String> names = new HashSet<>();

        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource(), ownerName);
        if (ownerUuid.isPresent()) {
            Optional<Estate> estate = CommandHelper.getEstateByOwner(source.getSource(), ownerUuid.get(), estateName);

            if (estate.isEmpty()) {
                return SharedSuggestionProvider.suggest(new ArrayList<>(), builder);
            }

            names = estate.get().findParcels().stream()
                    .map(Parcel::getName)
                    .map(StringArgumentType::escapeIfRequired)
                    .collect(Collectors.toSet());
        }
        return SharedSuggestionProvider.suggest(names, builder);
    };

    static final SuggestionProvider<CommandSourceStack> OPS_OWNER_ESTATE_PARCEL_NAMES = (source, builder) -> {
        String ownerName = StringArgumentType.getString(source, OWNER_NAME);
        String estateName = StringArgumentType.getString(source, ESTATE_NAME);

        List<String> names = new ArrayList<>();
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource(), ownerName);
        if (ownerUuid.isPresent()) {
            Optional<Estate> estate = CommandHelper.getEstateByOwner(source.getSource(), ownerUuid.get(), estateName);
            if (estate.isPresent()) {
                Set<Parcel> parcels = estate.get().findParcels();
                names = parcels.stream()
                        .map((Parcel::getName))
                        .map(StringArgumentType::escapeIfRequired)
                        .toList();
            }
        }
        return SharedSuggestionProvider.suggest(names, builder);
    };

    @Deprecated
    static final SuggestionProvider<CommandSourceStack> OPS_PARCEL_NAMES = (source, builder) -> {
        String ownerName = StringArgumentType.getString(source, OWNER_NAME);
        List<String> parcels = new ArrayList<>();
        Optional<UUID> playerUuid = CommandHelper.getPlayerUuid(source.getSource(), ownerName);
        if (playerUuid.isPresent()) {
            parcels = ParcelRegistry.findByOwner(playerUuid.get()).stream()
                    .map(Parcel::getName)
                    .map(StringArgumentType::escapeIfRequired)
                    .toList();
        }
        return SharedSuggestionProvider.suggest(parcels, builder);
    };

    static final SuggestionProvider<CommandSourceStack>
            OWNER_NATION_ESTATE_NAMES = (source, builder) -> {
        ServerPlayer owner = source.getSource().getPlayerOrException();
        List<String> names = EstateRegistry.findByOwner(owner.getUUID()).stream()
                .filter(estate -> estate instanceof NationEstate)
                .map((Estate::getName))
                .map(StringArgumentType::escapeIfRequired)
                .toList();
        return SharedSuggestionProvider.suggest(names, builder);
    };

    static final SuggestionProvider<CommandSourceStack>
            OWNER_ESTATE_NATION_NAMES = (source, builder) -> {
        ServerPlayer owner = source.getSource().getPlayerOrException();
        Set<String> names = ParcelRegistry.findByOwner(owner.getUUID()).stream()
                .filter(parcel -> parcel instanceof NationalizedParcel)
                .map(parcel -> (NationalizedParcel) parcel)
                .map(nationalizedParcel -> nationalizedParcel.getNationEstate().getName())
                .map(StringArgumentType::escapeIfRequired)
                .collect(Collectors.toSet());
        return SharedSuggestionProvider.suggest(names, builder);
    };

    static final SuggestionProvider<CommandSourceStack>
            OWNER_ESTATE_NAMES = (source, builder) -> {
        ServerPlayer owner = source.getSource().getPlayerOrException();
        List<String> names = EstateRegistry.findByOwner(owner.getUUID()).stream()
                .map(Estate::getName)
                .map(StringArgumentType::escapeIfRequired)
                .toList();
        return SharedSuggestionProvider.suggest(names, builder);
    };

    static final SuggestionProvider<CommandSourceStack> OWNER_ESTATE_NAMES_MINUS_SELF = (source, builder) -> {
        ServerPlayer owner = source.getSource().getPlayerOrException();
        String primaryEstateName = StringArgumentType.getString(source, ESTATE_NAME);
        List<String> estates = new ArrayList<>();

        estates = EstateRegistry.findByOwner(owner.getUUID()).stream()
                .map(Estate::getName)
                .filter(name -> !name.equalsIgnoreCase(primaryEstateName))
                .map(StringArgumentType::escapeIfRequired)
                .toList();

        return SharedSuggestionProvider.suggest(estates, builder);
    };

    public static final SuggestionProvider<CommandSourceStack>
            OWNER_CITIZEN_ESTATE_NAMES = (source, builder) -> {
        String nationName = StringArgumentType.getString(source, NATION_NAME);
        ServerPlayer owner = source.getSource().getPlayerOrException();

        Set<String> names = ParcelRegistry.findByOwner(owner.getUUID()).stream()
                .filter(parcel -> parcel.getType() == ParcelType.CITIZEN)
                .map(parcel -> (NationalizedParcel)parcel)
                .filter(nationalizedParcel -> nationalizedParcel.getNationEstate().getName().equalsIgnoreCase(nationName))
                .map(nationalizedParcel -> nationalizedParcel.getEstate().getName())
                .map(StringArgumentType::escapeIfRequired)
                .collect(Collectors.toSet());

        return SharedSuggestionProvider.suggest(names, builder);
    };

    static final SuggestionProvider<CommandSourceStack>
            OWNER_CITIZEN_PARCEL_NAMES = (source, builder) -> {
        ServerPlayer owner = source.getSource().getPlayerOrException();
        String estateName = StringArgumentType.getString(source, CITIZEN_ESTATE_NAME);
        Optional<Estate> estate = CommandHelper.getEstateByOwner(source.getSource(), owner.getUUID(), estateName);

        if (estate.isEmpty()) {
            return SharedSuggestionProvider.suggest(new ArrayList<>(), builder);
        }

        Set<String> names = estate.get().findParcels().stream()
                .map(parcel -> parcel.getName())
                .map(StringArgumentType::escapeIfRequired)
                .collect(Collectors.toSet());

        return SharedSuggestionProvider.suggest(names, builder);
    };

    public static final SuggestionProvider<CommandSourceStack>
            OWNER_ZONE_ESTATE_NAMES = (source, builder) -> {
        String nationName = StringArgumentType.getString(source, NATION_NAME);
        ServerPlayer owner = source.getSource().getPlayerOrException();

        Set<String> names = ParcelRegistry.findByOwner(owner.getUUID()).stream()
                .filter(parcel -> parcel.getType() == ParcelType.ZONE)
                .map(parcel -> (NationalizedParcel)parcel)
                .filter(nationalizedParcel -> nationalizedParcel.getNationEstate().getName().equalsIgnoreCase(nationName))
                .map(nationalizedParcel -> nationalizedParcel.getEstate().getName())
                .map(StringArgumentType::escapeIfRequired)
                .collect(Collectors.toSet());

        return SharedSuggestionProvider.suggest(names, builder);
    };


    static final SuggestionProvider<CommandSourceStack> OWNER_ESTATE_PARCEL_NAMES = (source, builder) -> {
        ServerPlayer owner = source.getSource().getPlayerOrException();
        String estateName = StringArgumentType.getString(source, ESTATE_NAME);
        Optional<Estate> estate = CommandHelper.getEstateByOwner(source.getSource(), owner.getUUID(), estateName);
        List<String> names = new ArrayList<>();
        if (estate.isPresent()) {
//            Set<Parcel> parcels = ParcelRegistry.findAllByEstateId(estate.get().getId());
            Set<Parcel> parcels = estate.get().findParcels();
            names = parcels.stream()
                    .map((Parcel::getName))
                    .map(StringArgumentType::escapeIfRequired)
                    .toList();
        }
        return SharedSuggestionProvider.suggest(names, builder);
    };

    static final SuggestionProvider<CommandSourceStack> OWNER_ESTATE_PARCEL_NAMES_MORE_THAN_ONE = (source, builder) -> {
        ServerPlayer owner = source.getSource().getPlayerOrException();
        String estateName = StringArgumentType.getString(source, ESTATE_NAME);
        Optional<Estate> estate = CommandHelper.getEstateByOwner(source.getSource(), owner.getUUID(), estateName);

        List<String> names = new ArrayList<>();
        if (estate.isPresent()) {
            Set<Parcel> parcels = ParcelRegistry.findAllByEstateId(estate.get().getId());
            if (parcels.size() == 1) {
                names.add("[cannot split an estate with a single parcel]");
            } else {
                names = parcels.stream()
                        .map((Parcel::getName))
                        .map(StringArgumentType::escapeIfRequired)
                        .toList();
            }
        }
        return SharedSuggestionProvider.suggest(names, builder);
    };

    public static final SuggestionProvider<CommandSourceStack> OWNER_PARCEL_NAMES = (source, builder) -> {
        ServerPlayer owner = source.getSource().getPlayerOrException();
        List<String> names = ParcelRegistry.findByOwner(owner.getUUID()).stream()
                .map((Parcel::getName)).toList();
        return SharedSuggestionProvider.suggest(names, builder);
    };

    static final SuggestionProvider<CommandSourceStack> CURRENT_FRIENDS_NAMES = (source, builder) -> {
        String estateName = StringArgumentType.getString(source, ESTATE_NAME);
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource());
        List<String> list = new ArrayList<>();

        if (ownerUuid.isPresent()) {
            Optional<Set<UUID>> friendsUuids = getFriendsWhitelist(source.getSource(), ownerUuid.get(), estateName);
            friendsUuids.ifPresent(uuids -> uuids.forEach(uuid -> {
                Optional<String> name = CommandHelper.getPlayerName(source.getSource(), uuid);
                name.ifPresent(list::add);
            }));
        }
        return SharedSuggestionProvider.suggest(list, builder);
    };

    static Optional<Set<UUID>> getFriendsWhitelist(CommandSourceStack source, UUID ownerUuid, String estateName) {
        Optional<Estate> estate = CommandHelper.getEstateByOwner(source, ownerUuid, estateName);
        return estate.map(Estate::getPlayerWhitelist);
    }

}
