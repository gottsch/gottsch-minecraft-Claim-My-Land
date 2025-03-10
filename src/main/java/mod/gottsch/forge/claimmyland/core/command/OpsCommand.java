/*
 * This file is part of  Claim My Land.
 * Copyright (c) 2024 Mark Gottschling (gottsch)
 *
 * All rights reserved.
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
 */
package mod.gottsch.forge.claimmyland.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.item.CitizenDeed;
import mod.gottsch.forge.claimmyland.core.item.Deed;
import mod.gottsch.forge.claimmyland.core.item.DeedFactory;
import mod.gottsch.forge.claimmyland.core.parcel.NationBorderType;
import mod.gottsch.forge.claimmyland.core.parcel.NationParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 *
 * @author Mark Gottschling on Mar 19, 2024
 *
 */
public class OpsCommand {


    private static final SuggestionProvider<CommandSourceStack> DEED_TYPES = (source, builder) -> {
        return SharedSuggestionProvider.suggest(Arrays.stream(ParcelType.values()).filter(p -> p != ParcelType.ZONE).map(ParcelType::getSerializedName), builder);
    };

    private static final SuggestionProvider<CommandSourceStack> PARCEL_TYPES = (source, builder) -> {
        return SharedSuggestionProvider.suggest(Arrays.stream(ParcelType.values()).map(ParcelType::getSerializedName), builder);
    };

    private static final SuggestionProvider<CommandSourceStack> BORDER_TYPES = (source, builder) -> {
        return SharedSuggestionProvider.suggest(Arrays.stream(NationBorderType.values()).map(NationBorderType::getSerializedName), builder);
    };


    private static final SuggestionProvider<CommandSourceStack> PARCEL_NAMES = (source, builder) -> {
        String ownerName = StringArgumentType.getString(source, CommandHelper.OWNER_NAME);
        // get the UUID for the name
//        ServerPlayer player = source.getSource().getServer().getPlayerList().getPlayerByName(ownerName);

        List<String> parcels = new ArrayList<>();
        Optional<UUID> playerUuid = CommandHelper.getPlayerUuid(source.getSource(), ownerName);
        if (playerUuid.isPresent()) {
//        if (player != null) {
            parcels = ParcelRegistry.findByOwner(playerUuid.get()).stream().map(Parcel::getName).toList();
        }
        return SharedSuggestionProvider.suggest(parcels, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> OWNER_NAMES = (source, builder) -> {
        // TODO get every owner from the ParcelRegistry
        PlayerList playerList = source.getSource().getServer().getPlayerList();
        List<String> names = ParcelRegistry.getOwnerIds().stream().map(id -> {
            ServerPlayer player = playerList.getPlayer(id);
            return player != null ? player.getName().getString() : "";
        }).toList();

        return SharedSuggestionProvider.suggest(names, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> NATION_NAMES = (source, builder) -> {
        List<String> names = ParcelRegistry.getNations().stream().map((Parcel::getName)).toList();
        return SharedSuggestionProvider.suggest(names, builder);
    };

//    private static final SuggestionProvider<CommandSourceStack> BLOCKS = (source, builder) -> {
//        List<String> tags = List.of(
//                ModUtil.getName(Blocks.CHEST).toString(),
//                ModUtil.getName(Blocks.BARREL).toString()
//        );
//
//        return SharedSuggestionProvider.suggest(tags, builder);
//    };


    private static final SuggestionProvider<CommandSourceStack> CURRENT_BLOCK_TAGS = (source, builder) -> {
        String ownerName = StringArgumentType.getString(source, CommandHelper.OWNER_NAME);
        String parcelName = StringArgumentType.getString(source, CommandHelper.PARCEL_NAME);
        Optional<List<String>> list = Optional.empty();

        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource(), ownerName);
        if (ownerUuid.isPresent()) {
            list = InteractWhitelistCommandsDelegate.getWhitelistByType(source.getSource(), ownerUuid.get(), parcelName, InteractWhitelistCommandsDelegate.WhitelistType.BLOCK_TAG);
        }
        return SharedSuggestionProvider.suggest(list.orElse(new ArrayList<>()), builder);
    };

    private static final SuggestionProvider<CommandSourceStack> CURRENT_BLOCKS = (source, builder) -> {
        String ownerName = StringArgumentType.getString(source, CommandHelper.OWNER_NAME);
        String parcelName = StringArgumentType.getString(source, CommandHelper.PARCEL_NAME);
        Optional<List<String>> list = Optional.empty();

        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource(), ownerName);
        if (ownerUuid.isPresent()) {
            list = InteractWhitelistCommandsDelegate.getWhitelistByType(source.getSource(), ownerUuid.get(), parcelName, InteractWhitelistCommandsDelegate.WhitelistType.BLOCK);
        }
        return SharedSuggestionProvider.suggest(list.orElse(new ArrayList<>()), builder);
    };

    private static final SuggestionProvider<CommandSourceStack> CURRENT_ITEM_TAGS = (source, builder) -> {
        String ownerName = StringArgumentType.getString(source, CommandHelper.OWNER_NAME);
        String parcelName = StringArgumentType.getString(source, CommandHelper.PARCEL_NAME);
        Optional<List<String>> list = Optional.empty();
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource(), ownerName);
        if (ownerUuid.isPresent()) {
            list = InteractWhitelistCommandsDelegate.getWhitelistByType(source.getSource(), ownerUuid.get(), parcelName, InteractWhitelistCommandsDelegate.WhitelistType.ITEM_TAG);
        }
        return SharedSuggestionProvider.suggest(list.orElse(new ArrayList<>()), builder);
    };

    private static final SuggestionProvider<CommandSourceStack> CURRENT_ITEMS = (source, builder) -> {
        String ownerName = StringArgumentType.getString(source, CommandHelper.OWNER_NAME);
        String parcelName = StringArgumentType.getString(source, CommandHelper.PARCEL_NAME);
        Optional<List<String>> list = Optional.empty();
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource(), ownerName);
        if (ownerUuid.isPresent()) {
            list = InteractWhitelistCommandsDelegate.getWhitelistByType(source.getSource(), ownerUuid.get(), parcelName, InteractWhitelistCommandsDelegate.WhitelistType.ITEM);
        }
        return SharedSuggestionProvider.suggest(list.orElse(new ArrayList<>()), builder);
    };

    /*
     * cml-ops
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher
                .register(Commands.literal(CommandHelper.CML_OPS).requires(source -> {
                                    return source.hasPermission(Config.SERVER.general.opsPermissionLevel.get()); // only ops can use command
                                })
                                ///// DEED TOP-LEVEL OPTION /////
                                .then(Commands.literal(CommandHelper.DEED).requires(source -> {
                                                    return source.hasPermission(Config.SERVER.general.opsPermissionLevel.get());
                                                })
                                                ///// GENERATE OPTION /////
//                                                .then(Commands.literal(CommandHelper.GENERATE)
                                                ///// NEW DEED /////
                                                .then(Commands.literal(CommandHelper.NEW)
                                                        .then(Commands.argument(CommandHelper.DEED_TYPE, StringArgumentType.string())
                                                                .suggests(DEED_TYPES)
                                                                .then(Commands.argument(CommandHelper.X_SIZE, IntegerArgumentType.integer())
                                                                        .then(Commands.argument(CommandHelper.Y_SIZE_UP, IntegerArgumentType.integer())
                                                                                .then(Commands.argument(CommandHelper.Y_SIZE_DOWN, IntegerArgumentType.integer())
                                                                                        .then(Commands.argument(CommandHelper.Z_SIZE, IntegerArgumentType.integer())
                                                                                                .executes(source -> {
                                                                                                    return generateDeed(source.getSource(),
                                                                                                            StringArgumentType.getString(source, CommandHelper.DEED_TYPE),
                                                                                                            IntegerArgumentType.getInteger(source, CommandHelper.X_SIZE),
                                                                                                            IntegerArgumentType.getInteger(source, CommandHelper.Y_SIZE_UP),
                                                                                                            IntegerArgumentType.getInteger(source, CommandHelper.Y_SIZE_DOWN),
                                                                                                            IntegerArgumentType.getInteger(source, CommandHelper.Z_SIZE)
                                                                                                    );
                                                                                                })
                                                                                                .then(Commands.argument(CommandHelper.NATION_NAME, StringArgumentType.string())
                                                                                                        .suggests(NATION_NAMES)
                                                                                                        .executes(source -> {
                                                                                                            return generateDeed(source.getSource(),
                                                                                                                    StringArgumentType.getString(source, CommandHelper.DEED_TYPE),
                                                                                                                    IntegerArgumentType.getInteger(source, CommandHelper.X_SIZE),
                                                                                                                    IntegerArgumentType.getInteger(source, CommandHelper.Y_SIZE_UP),
                                                                                                                    IntegerArgumentType.getInteger(source, CommandHelper.Y_SIZE_DOWN),
                                                                                                                    IntegerArgumentType.getInteger(source, CommandHelper.Z_SIZE),
                                                                                                                    StringArgumentType.getString(source, CommandHelper.NATION_NAME)
                                                                                                            );
                                                                                                            // TODO need to supply the owner name
                                                                                                        })
                                                                                                )
                                                                                        )

                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                                ///// TRANSFER /////
                                                .then(Commands.literal(CommandHelper.TRANSFER)
//                                                                .then(Commands.literal(CommandHelper.BY_OWNER)
                                                                .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                        .suggests(OWNER_NAMES)
                                                                        .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                                .suggests(PARCEL_NAMES)
                                                                                .executes(source -> {
                                                                                    // TODO make new method
                                                                                    return generateDeedFromParcel(source.getSource(),
                                                                                            StringArgumentType.getString(source, CommandHelper.OWNER_NAME),
                                                                                            StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
                                                                                            "");
                                                                                })
                                                                                .then(Commands.argument(CommandHelper.NEW_OWNER_NAME, StringArgumentType.string())
                                                                                        .suggests(OWNER_NAMES)
                                                                                        .executes(source -> {
                                                                                            return generateDeedFromParcel(source.getSource(),
                                                                                                    StringArgumentType.getString(source, CommandHelper.OWNER_NAME),
                                                                                                    StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
                                                                                                    StringArgumentType.getString(source, CommandHelper.NEW_OWNER_NAME));
                                                                                        })
                                                                                )
                                                                        )
                                                                )
                                                )

                                )
                                ///// PARCEL TOP-LEVEL OPTION /////
                                .then(Commands.literal(CommandHelper.PARCEL).requires(source -> {
                                                    return source.hasPermission(Config.SERVER.general.opsPermissionLevel.get());
                                                })
                                                ///// LIST OPTION /////
                                                .then(Commands.literal(CommandHelper.LIST)
                                                        .then(Commands.literal(CommandHelper.BY_NATION)
                                                                .then(Commands.argument(CommandHelper.NATION_NAME, StringArgumentType.string())
                                                                        .suggests(NATION_NAMES)
                                                                        .executes(source -> {
                                                                            return ParcelCommandDelegate.listParcelsByNation(source.getSource(), StringArgumentType.getString(source, CommandHelper.NATION_NAME));
                                                                        })
                                                                )
                                                        )
                                                        .then(Commands.literal(CommandHelper.BY_OWNER)
                                                                .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                        .suggests(OWNER_NAMES)
                                                                        .executes(source -> {
                                                                            return ParcelCommandDelegate.listParcelsByOwner(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME));
                                                                        })
                                                                )
                                                        )
                                                        .then(Commands.literal(CommandHelper.BY_ABANDONED)
                                                                .executes(source -> {
                                                                    return ParcelCommandDelegate.listParcelsByAbandoned(source.getSource());
                                                                })
                                                        )
                                                )

                                                ///// ADD OPTION /////
                                                .then(Commands.literal(CommandHelper.ADD)
                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                .suggests(OWNER_NAMES)
                                                                .then(Commands.argument(CommandHelper.POS, BlockPosArgument.blockPos())
                                                                        .then(Commands.argument(CommandHelper.X_SIZE, IntegerArgumentType.integer())
                                                                                .then(Commands.argument(CommandHelper.Y_SIZE_UP, IntegerArgumentType.integer())
                                                                                        .then(Commands.argument(CommandHelper.Y_SIZE_DOWN, IntegerArgumentType.integer())
                                                                                                .then(Commands.argument(CommandHelper.Z_SIZE, IntegerArgumentType.integer())
                                                                                                        .then(Commands.argument(CommandHelper.DEED_TYPE, StringArgumentType.string())
                                                                                                                .suggests(PARCEL_TYPES)
                                                                                                                .executes(source -> {
                                                                                                                    return ParcelCommandDelegate.addParcel(source.getSource(),
                                                                                                                            StringArgumentType.getString(source, CommandHelper.OWNER_NAME),
                                                                                                                            BlockPosArgument.getLoadedBlockPos(source, CommandHelper.POS),
                                                                                                                            IntegerArgumentType.getInteger(source, CommandHelper.X_SIZE),
                                                                                                                            IntegerArgumentType.getInteger(source, CommandHelper.Y_SIZE_UP),
                                                                                                                            IntegerArgumentType.getInteger(source, CommandHelper.Y_SIZE_DOWN),
                                                                                                                            IntegerArgumentType.getInteger(source, CommandHelper.Z_SIZE),
                                                                                                                            StringArgumentType.getString(source, CommandHelper.DEED_TYPE)

                                                                                                                    );
                                                                                                                })
                                                                                                                .then(Commands.argument("nationName", StringArgumentType.string())
                                                                                                                        .suggests(NATION_NAMES)
                                                                                                                        .executes(source -> {
                                                                                                                            return ParcelCommandDelegate.addParcel(source.getSource(),
                                                                                                                                    StringArgumentType.getString(source, CommandHelper.OWNER_NAME),
                                                                                                                                    BlockPosArgument.getLoadedBlockPos(source, CommandHelper.POS),
                                                                                                                                    IntegerArgumentType.getInteger(source, CommandHelper.X_SIZE),
                                                                                                                                    IntegerArgumentType.getInteger(source, CommandHelper.Y_SIZE_UP),
                                                                                                                                    IntegerArgumentType.getInteger(source, CommandHelper.Y_SIZE_DOWN),
                                                                                                                                    IntegerArgumentType.getInteger(source, CommandHelper.Z_SIZE),
                                                                                                                                    StringArgumentType.getString(source, CommandHelper.DEED_TYPE),
                                                                                                                                    StringArgumentType.getString(source, "nationName")
                                                                                                                            );
                                                                                                                        })
                                                                                                                )
                                                                                                        )
                                                                                                )
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                                ///// ABANDON OPTION /////
                                                .then(Commands.literal(CommandHelper.ABANDON)
                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                .suggests(OWNER_NAMES)
                                                                .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                        .suggests(PARCEL_NAMES)
                                                                        .executes(source -> {
                                                                            return ParcelCommandDelegate.abandonParcel(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME), StringArgumentType.getString(source, CommandHelper.PARCEL_NAME));
                                                                        })
                                                                )
                                                        )
                                                )

                                                ///// BORDER TYPE /////
                                                .then(Commands.literal(CommandHelper.BORDER_TYPE)
                                                        .then(Commands.argument(CommandHelper.NATION_NAME, StringArgumentType.string())
                                                                .suggests(NATION_NAMES)
                                                                .then(Commands.argument(CommandHelper.BORDER_TYPE, StringArgumentType.string())
                                                                        .suggests(BORDER_TYPES)
                                                                        .executes(source -> {
                                                                            return ParcelCommandDelegate.borderType(source.getSource(), StringArgumentType.getString(source, CommandHelper.NATION_NAME), StringArgumentType.getString(source, CommandHelper.BORDER_TYPE));
                                                                        })
                                                                )

                                                        )
                                                )
                                                ///// DEMOLISH /////
                                                .then(Commands.literal(CommandHelper.DEMOLISH)
                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                .suggests(OWNER_NAMES)
                                                                .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                        .suggests(PARCEL_NAMES)
                                                                        .executes(source -> {
                                                                            return ParcelCommandDelegate.demolishParcel(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME), StringArgumentType.getString(source, CommandHelper.PARCEL_NAME));
                                                                        })
                                                                )
                                                        )
                                                )
                                                ///// REMOVE PARCEL /////
                                                .then(Commands.literal(CommandHelper.REMOVE)
                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                .suggests(OWNER_NAMES)
                                                                .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                        .suggests(PARCEL_NAMES)
                                                                        .executes(source -> {
                                                                            return ParcelCommandDelegate.removeParcel(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME), StringArgumentType.getString(source, CommandHelper.PARCEL_NAME));
                                                                        })

                                                                )
                                                        )
                                                )
                                                ///// RENAME PARCEL /////
                                                .then(Commands.literal(CommandHelper.RENAME)
                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                .suggests(OWNER_NAMES)
                                                                .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                        .suggests(PARCEL_NAMES)
                                                                        .then(Commands.argument(CommandHelper.NEW_NAME, StringArgumentType.string())
                                                                                .executes(source -> {
                                                                                    return ParcelCommandDelegate.renameParcel(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME),
                                                                                            StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
                                                                                            StringArgumentType.getString(source, CommandHelper.NEW_NAME));
                                                                                })
                                                                        )

                                                                )
                                                        )
                                                )
                                                ///// TRANSFER /////
                                                .then(Commands.literal(CommandHelper.TRANSFER)
                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                .suggests(OWNER_NAMES)
                                                                .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                        .suggests(PARCEL_NAMES)
                                                                        .then(Commands.argument(CommandHelper.NEW_OWNER_NAME, StringArgumentType.string())
                                                                                .suggests(OWNER_NAMES)
                                                                                .executes(source -> {
                                                                                    return ParcelCommandDelegate.transferParcel(source.getSource(),
                                                                                            StringArgumentType.getString(source, CommandHelper.OWNER_NAME),
                                                                                            StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
                                                                                            StringArgumentType.getString(source, CommandHelper.NEW_OWNER_NAME));
                                                                                })
                                                                        )
                                                                )
                                                        )
                                                )
                                                ///// CLEAR /////
                                                .then(Commands.literal(CommandHelper.CLEAR)
                                                        .executes(source -> {
                                                            return ParcelCommandDelegate.clearAllParcels(source.getSource());
                                                        })
                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                .suggests(OWNER_NAMES)
                                                                .executes(source -> {
                                                                    return ParcelCommandDelegate.clearOwnerParcels(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME));
                                                                })
                                                        )
                                                )
                                                ///// WHITELIST OPTION /////
                                                .then(Commands.literal(CommandHelper.WHITELIST)
                                                                // TODO change to subcommand [PLAYER | BLOCK_TAG | BLOCK | etc]
                                                                ///// BLOCK TAG WHITELIST OPTION /////
                                                                .then(Commands.literal(CommandHelper.BLOCK_TAG)
                                                                        ///// BLOCK TAG WHITELIST ADD /////
                                                                        .then(Commands.literal(CommandHelper.ADD)
                                                                                .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                                        .suggests(OWNER_NAMES)
                                                                                        .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                                                .suggests(PARCEL_NAMES)
                                                                                                .then(Commands.argument(CommandHelper.TAG_NAME, ResourceLocationArgument.id())
                                                                                                        .suggests(CommandHelper.BLOCK_TAGS)
                                                                                                        .executes(source -> {
                                                                                                            return InteractWhitelistCommandsDelegate.add(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME), StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
                                                                                                                    ResourceLocationArgument.getId(source, CommandHelper.TAG_NAME), InteractWhitelistCommandsDelegate.WhitelistType.BLOCK_TAG);
                                                                                                        })
                                                                                                )
                                                                                        )
                                                                                )
                                                                        )
                                                                        ///// BLOCK TAGS WHITELIST LIST /////
                                                                        .then(Commands.literal(CommandHelper.LIST)
                                                                                .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                                        .suggests(OWNER_NAMES)
                                                                                        .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                                                .suggests(PARCEL_NAMES)
                                                                                                .executes(source -> {
                                                                                                    return InteractWhitelistCommandsDelegate.list(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME), StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
                                                                                                            InteractWhitelistCommandsDelegate.WhitelistType.BLOCK_TAG);
                                                                                                })
                                                                                        )
                                                                                )
                                                                        )
                                                                        ///// BLOCK TAGS WHITELIST REMOVE /////
                                                                        .then(Commands.literal(CommandHelper.REMOVE)
                                                                                .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                                        .suggests(OWNER_NAMES)
                                                                                        .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                                                .suggests(PARCEL_NAMES)
                                                                                                .then(Commands.argument(CommandHelper.TAG_NAME, ResourceLocationArgument.id())
                                                                                                        .suggests(CURRENT_BLOCK_TAGS)
                                                                                                        .executes(source -> {
                                                                                                            return InteractWhitelistCommandsDelegate.remove(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME), StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
                                                                                                                    ResourceLocationArgument.getId(source, CommandHelper.TAG_NAME), InteractWhitelistCommandsDelegate.WhitelistType.BLOCK_TAG);
                                                                                                        })
                                                                                                )
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                                /// ///
                                                                ///// BLOCK WHITELIST OPTION /////
                                                                .then(Commands.literal(CommandHelper.BLOCKS)
                                                                                ///// BLOCK WHITELIST ADD /////
                                                                                .then(Commands.literal(CommandHelper.ADD)
                                                                                                .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                                                                .suggests(OWNER_NAMES)
                                                                                                                .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                                                                                .suggests(PARCEL_NAMES)
//                                                                                        .then(Commands.argument(CommandHelper.TAG_NAME, ResourceLocationArgument.id())
                                                                                                                                .then(Commands.argument(CommandHelper.ITEM, ItemArgument.item(buildContext))
                                                                                                                                                .executes(source -> {
                                                                                                                                                    return InteractWhitelistCommandsDelegate.add(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME), StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
//                                                                                                            ResourceLocationArgument.getId(source, CommandHelper.TAG_NAME),
                                                                                                                                                            ItemArgument.getItem(source, CommandHelper.ITEM),
                                                                                                                                                            InteractWhitelistCommandsDelegate.WhitelistType.BLOCK);
                                                                                                                                                })
                                                                                                                                )
                                                                                                                )
                                                                                                )
                                                                                )
                                                                                ///// BLOCK WHITELIST REMOVE /////
                                                                                .then(Commands.literal(CommandHelper.REMOVE)
                                                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                                                .suggests(OWNER_NAMES)
                                                                                                .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                                                        .suggests(PARCEL_NAMES)
                                                                                                        .then(Commands.argument(CommandHelper.TAG_NAME, ResourceLocationArgument.id())
                                                                                                                .suggests(CURRENT_BLOCKS)
                                                                                                                .executes(source -> {
                                                                                                                    return InteractWhitelistCommandsDelegate.remove(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME), StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
                                                                                                                            ResourceLocationArgument.getId(source, CommandHelper.TAG_NAME), InteractWhitelistCommandsDelegate.WhitelistType.BLOCK);
                                                                                                                })
                                                                                                        )
                                                                                                )
                                                                                        )
                                                                                )
                                                                                ///// BLOCK WHITELIST LIST /////
                                                                                .then(Commands.literal(CommandHelper.LIST)
                                                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                                                .suggests(OWNER_NAMES)
                                                                                                .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                                                        .suggests(PARCEL_NAMES)
                                                                                                        .executes(source -> {
                                                                                                            return InteractWhitelistCommandsDelegate.list(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME), StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
                                                                                                                    InteractWhitelistCommandsDelegate.WhitelistType.BLOCK);
                                                                                                        })
                                                                                                )
                                                                                        )
                                                                                )
                                                                )
                                                                ///// ITEM TAG WHITELIST OPTION /////
                                                                .then(Commands.literal(CommandHelper.ITEM_TAG)
                                                                        ///// WHITELIST ADD /////
                                                                        .then(Commands.literal(CommandHelper.ADD)
                                                                                .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                                        .suggests(OWNER_NAMES)
                                                                                        .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                                                .suggests(PARCEL_NAMES)
                                                                                                .then(Commands.argument(CommandHelper.TAG_NAME, ResourceLocationArgument.id())
                                                                                                        .suggests(CommandHelper.ITEM_TAGS)
                                                                                                        .executes(source -> {
                                                                                                            return InteractWhitelistCommandsDelegate.add(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME), StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
                                                                                                                    ResourceLocationArgument.getId(source, CommandHelper.TAG_NAME), InteractWhitelistCommandsDelegate.WhitelistType.ITEM_TAG);
                                                                                                        })
                                                                                                )
                                                                                        )
                                                                                )
                                                                        )
                                                                        ///// ITEM TAGS WHITELIST LIST /////
                                                                        .then(Commands.literal(CommandHelper.LIST)
                                                                                .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                                        .suggests(OWNER_NAMES)
                                                                                        .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                                                .suggests(PARCEL_NAMES)
                                                                                                .executes(source -> {
                                                                                                    return InteractWhitelistCommandsDelegate.list(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME), StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
                                                                                                            InteractWhitelistCommandsDelegate.WhitelistType.ITEM_TAG);
                                                                                                })
                                                                                        )
                                                                                )
                                                                        )
                                                                        ///// ITEM TAGS WHITELIST REMOVE /////
                                                                        .then(Commands.literal(CommandHelper.REMOVE)
                                                                                .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                                        .suggests(OWNER_NAMES)
                                                                                        .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                                                .suggests(PARCEL_NAMES)
                                                                                                .then(Commands.argument(CommandHelper.TAG_NAME, ResourceLocationArgument.id())
                                                                                                        .suggests(CURRENT_ITEM_TAGS)
                                                                                                        .executes(source -> {
                                                                                                            return InteractWhitelistCommandsDelegate.remove(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME), StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
                                                                                                                    ResourceLocationArgument.getId(source, CommandHelper.TAG_NAME), InteractWhitelistCommandsDelegate.WhitelistType.ITEM_TAG);
                                                                                                        })
                                                                                                )
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                                ///// ITEM WHITELIST OPTION /////
                                                                .then(Commands.literal(CommandHelper.ITEMS)
                                                                                ///// ITEM WHITELIST ADD /////
                                                                                .then(Commands.literal(CommandHelper.ADD)
                                                                                                .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                                                                .suggests(OWNER_NAMES)
                                                                                                                .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                                                                                .suggests(PARCEL_NAMES)
                                                                                                                                          .then(Commands.argument(CommandHelper.ITEM, ItemArgument.item(buildContext))
                                                                                                                                                .executes(source -> {
                                                                                                                                                    return InteractWhitelistCommandsDelegate.add(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME), StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
                                                                                                                                                            ItemArgument.getItem(source, CommandHelper.ITEM),
                                                                                                                                                            InteractWhitelistCommandsDelegate.WhitelistType.ITEM);
                                                                                                                                                })
                                                                                                                                )
                                                                                                                )
                                                                                                )
                                                                                )
                                                                                ///// ITEM WHITELIST REMOVE /////
                                                                                .then(Commands.literal(CommandHelper.REMOVE)
                                                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                                                .suggests(OWNER_NAMES)
                                                                                                .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                                                        .suggests(PARCEL_NAMES)
                                                                                                        .then(Commands.argument(CommandHelper.TAG_NAME, ResourceLocationArgument.id())
                                                                                                                .suggests(CURRENT_ITEMS)
                                                                                                                .executes(source -> {
                                                                                                                    return InteractWhitelistCommandsDelegate.remove(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME), StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
                                                                                                                            ResourceLocationArgument.getId(source, CommandHelper.TAG_NAME), InteractWhitelistCommandsDelegate.WhitelistType.ITEM);
                                                                                                                })
                                                                                                        )
                                                                                                )
                                                                                        )
                                                                                )
                                                                                ///// ITEM WHITELIST LIST /////
                                                                                .then(Commands.literal(CommandHelper.LIST)
                                                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                                                .suggests(OWNER_NAMES)
                                                                                                .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                                                        .suggests(PARCEL_NAMES)
                                                                                                        .executes(source -> {
                                                                                                            return InteractWhitelistCommandsDelegate.list(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME), StringArgumentType.getString(source, CommandHelper.PARCEL_NAME),
                                                                                                                    InteractWhitelistCommandsDelegate.WhitelistType.ITEM);
                                                                                                        })
                                                                                                )
                                                                                        )
                                                                                )
                                                                )
                                                                .then(Commands.literal(CommandHelper.FRIENDS)
                                                                                ///// WHITELIST ADD /////
//                                                                                .then(Commands.literal(CommandHelper.ADD)
//                                                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
//                                                                                                .suggests(OWNER_NAMES)
//                                                                                                .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
//                                                                                                        .suggests(PARCEL_NAMES)
//                                                                                                        .executes(source -> {
//                                                                                                            return ParcelWhitelistCommandDelegate.addToWhitelist(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME), StringArgumentType.getString(source, CommandHelper.PARCEL_NAME));
//                                                                                                        })
//                                                                                                )
//                                                                                        )
//                                                                                )
//                                                                                ///// WHITELIST LIST /////
//                                                                                .then(Commands.literal(CommandHelper.LIST)
//                                                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
//                                                                                                .suggests(OWNER_NAMES)
//                                                                                                .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
//                                                                                                        .suggests(PARCEL_NAMES)
//                                                                                                        .executes(source -> {
//                                                                                                            return ParcelWhitelistCommandDelegate.displayWhitelist(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME), StringArgumentType.getString(source, CommandHelper.PARCEL_NAME));
//                                                                                                        })
//                                                                                                )
//                                                                                        )
//                                                                                )
                                                                        ///// TODO WHITELIST REMOVE /////
                                                                        ///// PLAYER WHITELIST REMOVE /////
//                                                                        .then(Commands.literal(CommandHelper.LIST)
//                                                                                .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
//                                                                                        .suggests(OWNER_NAMES)
//                                                                                        .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
//                                                                                                .suggests(PARCEL_NAMES)
//                                                                                                .executes(source -> {
//                                                                                                    return ParcelWhitelistCommandDelegate.removeFromWhitelist(source.getSource(), StringArgumentType.getString(source, CommandHelper.OWNER_NAME), StringArgumentType.getString(source, CommandHelper.PARCEL_NAME));
//                                                                                                })
//                                                                                        )
//                                                                                )
//                                                                        )
                                                                )
                                                )
                                                .then(Commands.literal(CommandHelper.BACKUP)
                                                        .executes(source -> {
                                                            return ParcelCommandDelegate.backupParcels(source.getSource());
                                                        })
                                                )
                                )
                );


    } // end of method

    private static int generateDeed(CommandSourceStack source, String deedType, int xSize, int ySizeUp, int ySizeDown, int zSize) {
        return generateDeed(source, deedType, xSize, ySizeUp, ySizeDown, zSize, "");
    }

    /**
     *
     * @param source
     * @param deedType
     * @param xSize
     * @param ySizeUp
     * @param ySizeDown
     * @param zSize
     * @return
     */
    private static int generateDeed(CommandSourceStack source, String deedType, int xSize, int ySizeUp, int ySizeDown, int zSize, String nationName) {
        // get the type
        ParcelType type;
        try {
            type = ParcelType.valueOf(deedType);
        } catch (Exception e) {
            source.sendFailure(Component.translatable(LangUtil.chat("deed.invalid_type")).withStyle(ChatFormatting.RED));
            return 0;
        }

        // find the nation by name
        UUID nationId = ParcelRegistry.getNations().stream()
                .filter(n -> nationName.equalsIgnoreCase(((NationParcel) n).getName()))
                .findFirst()
                .map(n -> ((NationParcel)n).getNationId()).orElse(null);

        // validations
        if ((type == ParcelType.CITIZEN || type == ParcelType.ZONE) && nationId == null) {
            source.sendFailure(Component.translatable(LangUtil.chat("deed.citizen.nationId_required")).withStyle(ChatFormatting.RED));
            return 0;
        }

        if (xSize < 2 || ySizeUp < 2 || ySizeDown < 2 || zSize < 2) {
            CommandHelper.failure(source, "deed.too_small");
            return 0;
        }

        if (source.getLevel().isOutsideBuildHeight(ySizeUp + ySizeDown)) {
            source.sendFailure(Component.translatable(LangUtil.chat("deed.outside_world_boundaries")).withStyle(ChatFormatting.RED));
            return 0;
        }

        // create a relative sized Box
        Box size = new Box(new Coords(0, -ySizeDown, 0), new Coords(xSize-1, ySizeUp-1, zSize-1));

        // attempt to add the deed item to the player inventory
        try {
            // create a deed item
            ItemStack deed = switch (type) {
                case PLAYER -> DeedFactory.createPlayerDeed(size);
                // NOTE nation DEED does NOT take in a nationId nor nationName as
                // a deed is a net new parcel to be used by anyone. the name would not be known
                // and also this avoids duplicate names floating around in the deeds.
                case NATION -> DeedFactory.createNationDeed(source.getLevel(), size);
                case CITIZEN -> {
                    ItemStack d = DeedFactory.createCitizenDeed(size, nationId);
                    d.getOrCreateTag().putString(CitizenDeed.NATION_NAME, nationName);
                    yield d;
                }
                case ZONE -> ItemStack.EMPTY;
            };

            if (deed != ItemStack.EMPTY) {
                source.getPlayerOrException().getInventory().add(deed);
            }
        } catch (Exception e) {
            ClaimMyLand.LOGGER.error("error while generating deed:", e);
            source.sendSuccess(() -> Component.translatable(LangUtil.chat(" deed.generate.failure")).withStyle(ChatFormatting.RED), false);
        }

        return 1;
    }


    /**
     * @param source
     * @param ownerName
     * @param parcelName
     * @return
     */
    @Deprecated
    private static int generateDeedFromParcel(CommandSourceStack source, String ownerName, String parcelName, String newOwnerName) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        if (player != null) {
            List<Parcel> parcels = ParcelRegistry.findByOwner(player.getUUID());
            Optional<Parcel> parcel = parcels.stream().filter(p -> p.getName().equalsIgnoreCase(parcelName)).findFirst();
            if (parcel.isPresent()) {
                ItemStack deed = DeedFactory.createDeed(parcel.get().getClass(), parcel.get().getSize());
                CompoundTag tag = deed.getOrCreateTag();
                tag.putUUID(Deed.PARCEL_ID, parcel.get().getId());
                // set owner id if present
                if(StringUtils.isNotBlank(newOwnerName)) {
                    ServerPlayer newOwner = source.getServer().getPlayerList().getPlayerByName(newOwnerName);
                    if (newOwner != null) {
                        tag.putUUID(Deed.OWNER_ID, newOwner.getUUID());
                    } else {
                        CommandHelper.sendUnableToLocatePlayerMessage(source, newOwnerName);
                        return -1;
                    }
                }
                // attempt to add the deed item to the player inventory
                try {
                    if (deed != null && deed != ItemStack.EMPTY) {
                        source.getPlayerOrException().getInventory().add(deed);
                    }
                } catch (Exception e) {
                    ClaimMyLand.LOGGER.error("error on generateFromParcel -> ", e);
                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("unexpected_error")).withStyle(ChatFormatting.RED), false);

                }
            } else {
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.generate.failure")).withStyle(ChatFormatting.RED), false);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }

        return 1;
    }

    private static int generateDeedFromNation(CommandSourceStack source, String nationName, int xSize, int ySizeUp, int ySizeDown, int zSize) {
        // create a relative sized Box
        Box size = new Box(new Coords(0, -ySizeDown, 0), new Coords(xSize-1, ySizeUp-1, zSize-1));

        Optional<Parcel> parcel = ParcelRegistry.getNations().stream().filter(p -> p.getName().equalsIgnoreCase(nationName)).findFirst();
        if (parcel.isPresent()) {
            ItemStack deed = DeedFactory.createCitizenDeed(size, ((NationParcel)parcel.get()).getNationId());
            CompoundTag tag = deed.getOrCreateTag();

            // attempt to add the deed item to the player inventory
            try {
                if (deed != ItemStack.EMPTY) {
                    source.getPlayerOrException().getInventory().add(deed);
                }
            } catch (Exception e) {
                ClaimMyLand.LOGGER.error("error on generateDeedFromNation -> ", e);
                CommandHelper.sendUnableToGenerateDeedMessage(source, nationName);
            }
        } else {
            // TODO can't find nation
        }
        return 1;
    }
}