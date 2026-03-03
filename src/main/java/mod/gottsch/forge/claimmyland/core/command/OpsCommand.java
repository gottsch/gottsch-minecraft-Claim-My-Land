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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.forge.claimmyland.core.command.helper.WhitelistType;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.util.List;

/**
 *
 * @author Mark Gottschling on Mar 19, 2024
 *
 */
public class OpsCommand {
    private static final String CML_OPS = "cml-ops";


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

    /*
     * cml-ops
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher
                .register(Commands.literal(CML_OPS).requires(source -> {
                                    return source.hasPermission(Config.SERVER.general.opsPermissionLevel.get()); // only ops can use command
                                })
                                ///// DEED TOP-LEVEL OPTION /////
                                                ///// NEW DEED /////
                                        .then(new GenerateDeedSubCommand().buildOps())

                                ///// PARCEL TOP-LEVEL OPTION /////
                                .then(Commands.literal(CommandHelper.PARCEL).requires(source -> {
                                                    return source.hasPermission(Config.SERVER.general.opsPermissionLevel.get());
                                                })
                                                ///// LIST OPTION /////
                                                .then(new ListParcelsSubCommand().buildOps())

                                                ///// RELINQUISH OPTION /////
                                                .then(new RelinquishParcelSubCommand().buildOps())

                                                ///// DEMOLISH /////
                                                .then(new DemolishParcelSubCommand().buildOps())

                                                ///// RENAME PARCEL /////
                                                .then(new RenameParcelSubCommand().buildOps())

                                                ///// TRANSFER /////
                                                .then(new TransferParcelSubCommand().buildOps())

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
                                )
                                ///// ESTATE TOP-LEVEL OPTION /////
                                .then(Commands.literal(CommandHelper.ESTATE).requires(source -> {
                                                    return source.hasPermission(Config.SERVER.general.opsPermissionLevel.get());
                                                })
                                                ///// ACCESS TYPE /////
                                                .then(new AccessTypeSubCommand().buildOps())

                                                /// // LIST ESTATES /////
                                                .then(new ListEstateSubCommand().buildOps())
                                                ///// DETAILS OPTION /////
                                                .then(new EstateDetailsSubCommand().buildOps())
                                                ///// RENAME ESTATE /////
                                                .then(new RenameEstateSubCommand().buildOps())

                                                ///// REMOVE ESTATE /////
                                                // removes Estate from the world without returning Deeds
                                                .then(new RemoveEstateSubCommand().buildOps())

                                                ///// RELINQUISH /////
                                                .then(new RelinquishEstateSubCommand().buildOps())

                                                ///// TRANSFER /////
                                                .then(new TransferEstateSubCommand().buildOps())

                                                ///// JOIN (ANNEX) OPTION /////
                                                .then(new JoinSubCommand().buildOps())

                                                ///// SPLIT (CEDE) OPTION /////
                                                .then(new SplitSubCommand().buildOps())
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

//    private static int generateDeed(CommandSourceStack source, String deedType, int xSize, int ySizeUp, int ySizeDown, int zSize) {
//        return generateDeed(source, deedType, xSize, ySizeUp, ySizeDown, zSize, "");
//    }

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
//    private static int generateDeed(CommandSourceStack source, String deedType, int xSize, int ySizeUp, int ySizeDown, int zSize, String nationName) {
//        // get the type
//        ParcelType type;
//
//        type = ParcelType.fromString(deedType);
//        if (type == ParcelType.NONE) {
//            source.sendFailure(Component.translatable(LangUtil.chat("deed.invalid_type")).withStyle(ChatFormatting.RED));
//            return -1;
//        }
//
//        Optional<Estate> optionalEstate = EstateRegistry.findByName(nationName);
//
//        // validations
//        if ((type == ParcelType.CITIZEN) && optionalEstate.isEmpty()) {
//            source.sendFailure(Component.translatable(LangUtil.chat("deed.citizen.nationId_required")).withStyle(ChatFormatting.RED));
//            return -1;
//        }
//
//        if (xSize < 2 || (ySizeUp  + ySizeDown) < 2 || zSize < 2) {
//            CommandHelper.failure(source, "deed.too_small");
//            return -1;
//        }
//
//        if (source.getLevel().isOutsideBuildHeight(ySizeUp + ySizeDown)) {
//            source.sendFailure(Component.translatable(LangUtil.chat("deed.outside_world_boundaries")).withStyle(ChatFormatting.RED));
//            return -1;
//        }
//
//        // create a relative sized Box
//        Box size = new Box(new Coords(0, -ySizeDown, 0), new Coords(xSize-1, ySizeUp-1, zSize-1));
//
//        // attempt to add the deed item to the player inventory
//        try {
//            // create a deed item
//            ItemStack deed = switch (type) {
//                case PLAYER -> DeedFactory.createPlayerDeed(size);
//                // NOTE nation DEED does NOT take in a nationId nor nationName as
//                // a deed is a net new parcel to be used by anyone. the name would not be known
//                // and also this avoids duplicate names floating around in the deeds.
//                case NATION -> DeedFactory.createNationDeed(source.getLevel(), size);
//                case CITIZEN -> {
//                    ItemStack d = DeedFactory.createCitizenDeed(size, optionalEstate.get().getId());
////                    d.getOrCreateTag().putString(Deed.NATION_NAME, nationName);
//                    yield d;
//                }
//                case ZONE -> ItemStack.EMPTY;
//                default -> ItemStack.EMPTY;
//            };
//
//            if (deed != ItemStack.EMPTY) {
//                source.getPlayerOrException().getInventory().add(deed);
//            }
//        } catch (Exception e) {
//            ClaimMyLand.LOGGER.error("error while generating deed:", e);
//            source.sendSuccess(() -> Component.translatable(LangUtil.chat(" deed.generate.failure")).withStyle(ChatFormatting.RED), false);
//        }
//
//        return 1;
//    }
}