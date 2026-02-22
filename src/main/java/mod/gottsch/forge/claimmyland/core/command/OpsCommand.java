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
import mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.forge.claimmyland.core.command.helper.WhitelistType;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.item.Deed;
import mod.gottsch.forge.claimmyland.core.item.DeedFactory;
import mod.gottsch.forge.claimmyland.core.parcel.NationBorderType;
import mod.gottsch.forge.claimmyland.core.parcel.NationParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.EstateRegistry;
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
import net.minecraft.world.level.block.Block;
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

    @Deprecated
    public static final SuggestionProvider<CommandSourceStack> PARCEL_NAMES = (source, builder) -> {
        String ownerName = StringArgumentType.getString(source, CommandHelper.OWNER_NAME);
        List<String> parcels = new ArrayList<>();
        Optional<UUID> playerUuid = CommandHelper.getPlayerUuid(source.getSource(), ownerName);
        if (playerUuid.isPresent()) {
            parcels = ParcelRegistry.findByOwner(playerUuid.get()).stream().map(Parcel::getName).toList();
        }
        return SharedSuggestionProvider.suggest(parcels, builder);
    };

    /*
     * names of estates by owner
     */
    @Deprecated
    public static final SuggestionProvider<CommandSourceStack> OWNER_ESTATE_NAMES = (source, builder) -> {
        String ownerName = StringArgumentType.getString(source, CommandHelper.OWNER_NAME);
        List<String> estates = new ArrayList<>();
        Optional<UUID> playerUuid = CommandHelper.getPlayerUuid(source.getSource(), ownerName);
        if (playerUuid.isPresent()) {
            estates = EstateRegistry.getByOwner(playerUuid.get()).stream().map(Estate::getName).toList();
        }
        return SharedSuggestionProvider.suggest(estates, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> OWNER_ESTATE_NAMES_MINUS_SELF = (source, builder) -> {
        String ownerName = StringArgumentType.getString(source, CommandHelper.OWNER_NAME);
        String primaryEstateName = StringArgumentType.getString(source, CommandHelper.ESTATE_NAME);
        List<String> estates = new ArrayList<>();
        Optional<UUID> playerUuid = CommandHelper.getPlayerUuid(source.getSource(), ownerName);
        if (playerUuid.isPresent()) {
            estates = EstateRegistry.getByOwner(playerUuid.get()).stream()
                    .map(Estate::getName)
                    .filter(name -> !name.equalsIgnoreCase(primaryEstateName)).toList();
        }
        return SharedSuggestionProvider.suggest(estates, builder);
    };

    /*
     * names of owners of estates
     */
    @Deprecated
    public static final SuggestionProvider<CommandSourceStack> ESTATE_OWNER_NAMES = (source, builder) -> {
        PlayerList playerList = source.getSource().getServer().getPlayerList();
        List<String> names = EstateRegistry.getOwnerIds().stream().map(id -> {
            // TODO get offline player or use registry
            ServerPlayer player = playerList.getPlayer(id);
            return player != null ? player.getName().getString() : "";
        }).toList();

        return SharedSuggestionProvider.suggest(names, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> OWNER_ESTATE_PARCEL_NAMES = (source, builder) -> {
        String ownerName = StringArgumentType.getString(source, CommandHelper.OWNER_NAME);
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource(), ownerName);

        List<String> names = new ArrayList<>();
        if (ownerUuid.isPresent()) {
            String estateName = StringArgumentType.getString(source, CommandHelper.ESTATE_NAME);

            Set<Estate> estates = EstateRegistry.getByOwner(ownerUuid.get());
            Optional<Estate> estate = estates.stream().filter(est -> est.getName().equalsIgnoreCase(estateName)).findFirst();

            if (estate.isPresent()) {
                Set<Parcel> parcels = ParcelRegistry.findAllByEstateId(estate.get().getId());
                names = parcels.stream().map((Parcel::getName)).toList();
            }
        }
        return SharedSuggestionProvider.suggest(names, builder);
    };

    @Deprecated
    public static final SuggestionProvider<CommandSourceStack> OWNER_NAMES = (source, builder) -> {
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


//    private static final SuggestionProvider<CommandSourceStack> CURRENT_BLOCK_TAGS = (source, builder) -> {
//        String ownerName = StringArgumentType.getString(source, CommandHelper.OWNER_NAME);
//        String parcelName = StringArgumentType.getString(source, CommandHelper.PARCEL_NAME);
//        Optional<Set<String>> whitelist = Optional.empty();
//
//        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource(), ownerName);
//        if (ownerUuid.isPresent()) {
//            whitelist = InteractWhitelistCommandsDelegate.getWhitelistByType(source.getSource(), ownerUuid.get(), parcelName, InteractWhitelistCommandsDelegate.WhitelistType.BLOCK_TAG);
//        }
//        return SharedSuggestionProvider.suggest(whitelist.orElseGet(Collections::emptySet), builder);
//    };

//    @Deprecated
//    private static final SuggestionProvider<CommandSourceStack> CURRENT_BLOCKS = (source, builder) -> {
//        String ownerName = StringArgumentType.getString(source, CommandHelper.OWNER_NAME);
//        String parcelName = StringArgumentType.getString(source, CommandHelper.PARCEL_NAME);
//        Optional<Set<String>> whitelist = Optional.empty();
//
//        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource(), ownerName);
//        if (ownerUuid.isPresent()) {
//            whitelist = InteractWhitelistCommandsDelegate.getWhitelistByType(source.getSource(), ownerUuid.get(), parcelName, InteractWhitelistCommandsDelegate.WhitelistType.BLOCK);
//        }
//        return SharedSuggestionProvider.suggest(whitelist.orElseGet(Collections::emptySet), builder);
//    };
//
//    private static final SuggestionProvider<CommandSourceStack> CURRENT_ITEM_TAGS = (source, builder) -> {
//        String ownerName = StringArgumentType.getString(source, CommandHelper.OWNER_NAME);
//        String parcelName = StringArgumentType.getString(source, CommandHelper.PARCEL_NAME);
//        Optional<Set<String>> whitelist = Optional.empty();
//        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource(), ownerName);
//        if (ownerUuid.isPresent()) {
//            whitelist = InteractWhitelistCommandsDelegate.getWhitelistByType(source.getSource(), ownerUuid.get(), parcelName, InteractWhitelistCommandsDelegate.WhitelistType.ITEM_TAG);
//        }
//        return SharedSuggestionProvider.suggest(whitelist.orElseGet(Collections::emptySet), builder);
//    };
//
//    private static final SuggestionProvider<CommandSourceStack> CURRENT_ITEMS = (source, builder) -> {
//        String ownerName = StringArgumentType.getString(source, CommandHelper.OWNER_NAME);
//        String parcelName = StringArgumentType.getString(source, CommandHelper.PARCEL_NAME);
//        Optional<Set<String>> whitelist = Optional.empty();
//        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource(), ownerName);
//        if (ownerUuid.isPresent()) {
//            whitelist = InteractWhitelistCommandsDelegate.getWhitelistByType(source.getSource(), ownerUuid.get(), parcelName, InteractWhitelistCommandsDelegate.WhitelistType.ITEM);
//        }
//        return SharedSuggestionProvider.suggest(whitelist.orElseGet(Collections::emptySet), builder);
//    };

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
//                                                ///// LIST OPTION /////
                                                .then(new ListParcelsSubCommand().buildOps())

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
                                                ///// RELINQUISH OPTION /////
                                                .then(new RelinquishParcelSubCommand().buildOps())

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
                                                // removes Parcel from the world without returning a Deed
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
                                                .then(new RenameParcelSubCommand().buildOps())

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

                                                .then(Commands.literal(CommandHelper.BACKUP)
                                                        .executes(source -> {
                                                            return ParcelCommandDelegate.backupParcels(source.getSource());
                                                        })
                                                )
                                )
                                ///// ESTATE TOP-LEVEL OPTION /////
                                .then(Commands.literal(CommandHelper.ESTATE).requires(source -> {
                                                    return source.hasPermission(Config.SERVER.general.opsPermissionLevel.get());
                                                })
                                                ///// ACCESS TYPE /////
                                                .then(Commands.literal("access_type")
                                                        .then(Commands.argument(CommandHelper.NATION_NAME, StringArgumentType.string())
                                                                .suggests(NATION_NAMES)
                                                                .then(Commands.argument("access_type", StringArgumentType.string())
                                                                        .suggests(BORDER_TYPES)
                                                                        .executes(source -> {
                                                                            return ParcelCommandDelegate.borderType(source.getSource(), StringArgumentType.getString(source, CommandHelper.NATION_NAME), StringArgumentType.getString(source, "access_type"));
                                                                        })
                                                                )
                                                        )
                                                )

                                                /// // LIST ESTATES /////
                                                .then(new ListEstateSubCommand().buildOps())
                                                ///// DETAILS OPTION /////
                                                .then(new EstateDetailsSubCommand().buildOps())
                                                ///// RENAME ESTATE /////
                                                .then(new RenameEstateSubCommand().buildOps())

                                                ///// REMOVE ESTATE /////
                                                // removes Estate from the world without returning Deeds
                                                .then(Commands.literal(CommandHelper.REMOVE)
                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                .suggests(ESTATE_OWNER_NAMES)
                                                                .then(Commands.argument(CommandHelper.ESTATE_NAME, StringArgumentType.string())
                                                                        .suggests(OWNER_ESTATE_NAMES)
                                                                        .executes(source -> {
                                                                            return EstateCommandDelegate.removeEstate(source.getSource(),
                                                                                    StringArgumentType.getString(source, CommandHelper.OWNER_NAME),
                                                                                    StringArgumentType.getString(source, CommandHelper.ESTATE_NAME));
                                                                        })

                                                                )
                                                        )
                                                )
                                                ///// RELINQUISH /////
                                                .then(new RelinquishEstateSubCommand().buildOps())

                                                ///// TRANSFER /////
                                                .then(Commands.literal(CommandHelper.TRANSFER)
                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                .suggests(OWNER_NAMES)
                                                                .then(Commands.argument(CommandHelper.ESTATE_NAME, StringArgumentType.string())
                                                                        .suggests(OWNER_ESTATE_NAMES)
                                                                        .then(Commands.argument(CommandHelper.NEW_OWNER_NAME, StringArgumentType.string())
                                                                                .suggests(OWNER_NAMES)
                                                                                .executes(source -> {
                                                                                    return EstateCommandDelegate.transfer(source.getSource(),
                                                                                            StringArgumentType.getString(source, CommandHelper.OWNER_NAME),
                                                                                            StringArgumentType.getString(source, CommandHelper.ESTATE_NAME),
                                                                                            StringArgumentType.getString(source, CommandHelper.NEW_OWNER_NAME));
                                                                                })
                                                                        )
                                                                )
                                                        )
                                                )
                                                ///// JOIN (ANNEX) OPTION /////
                                                .then(Commands.literal(CommandHelper.JOIN)
                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                .suggests(ESTATE_OWNER_NAMES)
                                                                .then(Commands.argument(CommandHelper.ESTATE_NAME, StringArgumentType.string())
                                                                        .suggests(OWNER_ESTATE_NAMES)
                                                                        .then(Commands.argument(CommandHelper.OTHER_ESTATE_NAME, StringArgumentType.string())
                                                                                .suggests(OWNER_ESTATE_NAMES_MINUS_SELF)
                                                                                .executes(source -> {
                                                                                    return EstateCommandDelegate.join(source.getSource(),
                                                                                            StringArgumentType.getString(source, CommandHelper.OWNER_NAME),
                                                                                            StringArgumentType.getString(source, CommandHelper.ESTATE_NAME),
                                                                                            StringArgumentType.getString(source, CommandHelper.OTHER_ESTATE_NAME));
                                                                                })
                                                                        )
                                                                )
                                                        )
                                                )
                                                ///// SPLIT (CEDE) OPTION /////
                                                .then(Commands.literal(CommandHelper.SPLIT)
                                                        .then(Commands.argument(CommandHelper.OWNER_NAME, StringArgumentType.string())
                                                                .suggests(ESTATE_OWNER_NAMES)
                                                                .then(Commands.argument(CommandHelper.ESTATE_NAME, StringArgumentType.string())
                                                                        .suggests(OWNER_ESTATE_NAMES)
                                                                        .then(Commands.argument(CommandHelper.PARCEL_NAME, StringArgumentType.string())
                                                                                .suggests(OWNER_ESTATE_PARCEL_NAMES)
                                                                                .executes(source -> {
                                                                                    return EstateCommandDelegate.split(source.getSource(),
                                                                                            StringArgumentType.getString(source, CommandHelper.OWNER_NAME),
                                                                                            StringArgumentType.getString(source, CommandHelper.ESTATE_NAME),
                                                                                            StringArgumentType.getString(source, CommandHelper.PARCEL_NAME));
                                                                                })
                                                                        )
                                                                )
                                                        )
                                                )
                                                ///// WHITELIST OPTION /////
                                                .then(Commands.literal(CommandHelper.WHITELIST)
                                                                ///// BLOCK TAG WHITELIST OPTION /////
                                                                .then(new TagWhitelistSubCommand().buildOps(buildContext, WhitelistType.BLOCK_TAG))
                                                                ///// BLOCK WHITELIST OPTION /////
                                                                .then(new BlockWhitelistSubCommand().buildOps(buildContext, WhitelistType.BLOCK))
                                                                ///// ITEM TAG WHITELIST OPTION /////
                                                                .then(new TagWhitelistSubCommand().buildOps(buildContext, WhitelistType.ITEM_TAG))
                                                                ///// ITEM WHITELIST OPTION /////
                                                                .then(new ItemWhitelistSubCommand().buildOps(buildContext, WhitelistType.ITEM))
                                                                .then(new TagWhitelistSubCommand().build(buildContext, WhitelistType.ENTITY_TAG))
                                                                .then(new EntitySpawnWhitelistSubCommand().build(buildContext, WhitelistType.ENTITY))
                                                                .then(new FriendsWhitelistSubCommand().build())
                                                )
                                                ///// CLEAR /////
                                                // performs same action as parcel > clear
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

        // TODO get rid of nationId
        // find the nation by name
        UUID nationId = ParcelRegistry.getNations().stream()
                .filter(n -> nationName.equalsIgnoreCase(((NationParcel) n).getName()))
                .findFirst()
                .map(n -> ((NationParcel)n).getNationId()).orElse(null);

        // validations
        if ((type == ParcelType.CITIZEN) && nationId == null) {
            source.sendFailure(Component.translatable(LangUtil.chat("deed.citizen.nationId_required")).withStyle(ChatFormatting.RED));
            return 0;
        }

        if (xSize < 2 || (ySizeUp  + ySizeDown) < 2 || zSize < 2) {
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
//                    d.getOrCreateTag().putString(Deed.NATION_NAME, nationName);
                    yield d;
                }
                case ZONE -> ItemStack.EMPTY;
                default -> ItemStack.EMPTY;
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