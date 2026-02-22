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
import com.mojang.brigadier.suggestion.SuggestionProvider;
import mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.forge.claimmyland.core.command.helper.WhitelistType;
import mod.gottsch.forge.claimmyland.core.tags.ModTags;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;

import java.util.*;

/**
 * @author by Mark Gottschling on 2/22/2026
 */
public class TagWhitelistSubCommand extends WhitelistSubCommand {

    public static final SuggestionProvider<CommandSourceStack> BLOCK_TAGS = (source, builder) -> {
        List<String> tags = new ArrayList<>();
        ModTags.Blocks.BLOCK_TAG_WHITELISTS.forEach(tagKey -> {
            tags.add(tagKey.location().toString());
        });
        return SharedSuggestionProvider.suggest(tags, builder);
    };

//    private static final SuggestionProvider<CommandSourceStack> CURRENT_BLOCK_TAGS = (source, builder) -> {
//		String estateName = StringArgumentType.getString(source, CommandHelper.ESTATE_NAME);
//		Optional<Set<String>> list = Optional.empty();
//		Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource());
//		if (ownerUuid.isPresent()) {
//			list = getEstateWhitelistByType(source.getSource(), ownerUuid.get(), estateName, WhitelistType.BLOCK_TAG);
//		}
//		return SharedSuggestionProvider.suggest(list.orElseGet(Collections::emptySet), builder);
//	};

    static final SuggestionProvider<CommandSourceStack> CURRENT_ITEM_TAGS = (source, builder) -> {
        String parcelName = StringArgumentType.getString(source, ESTATE_NAME);
        Optional<Set<String>> list = Optional.empty();
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource());
        if (ownerUuid.isPresent()) {
            list = getEstateWhitelistByType(source.getSource(), ownerUuid.get(), parcelName, WhitelistType.ITEM_TAG);
        }
        return SharedSuggestionProvider.suggest(list.orElseGet(Collections::emptySet), builder);
    };

    public static final SuggestionProvider<CommandSourceStack> ITEM_TAGS = (source, builder) -> {
        List<String> tags = new ArrayList<>();
        ModTags.Items.ITEM_TAG_WHITELISTS.forEach(tagKey -> {
            tags.add(tagKey.location().toString());
        });
        return SharedSuggestionProvider.suggest(tags, builder);
    };

    static final SuggestionProvider<CommandSourceStack> CURRENT_ENTITY_SPAWN_TAGS = (source, builder) -> {
        String parcelName = StringArgumentType.getString(source, ESTATE_NAME);
        Optional<Set<String>> list = Optional.empty();
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source.getSource());
        if (ownerUuid.isPresent()) {
            list = getEstateWhitelistByType(source.getSource(), ownerUuid.get(), parcelName, WhitelistType.ENTITY_TAG);
        }
        return SharedSuggestionProvider.suggest(list.orElseGet(Collections::emptySet), builder);
    };

    public static final SuggestionProvider<CommandSourceStack> ENTITY_SPAWN_TAGS = (source, builder) -> {
        List<String> tags = new ArrayList<>();
        ModTags.Entities.ENTITY_SPAWN_TAG_WHITELISTS.forEach(tagKey -> {
            tags.add(tagKey.location().toString());
        });
        return SharedSuggestionProvider.suggest(tags, builder);
    };

    public LiteralArgumentBuilder<CommandSourceStack> build(CommandBuildContext buildContext, WhitelistType type) {
        SuggestionProvider<CommandSourceStack> suggestTags = switch(type) {
            case BLOCK -> null;
            case BLOCK_TAG -> BLOCK_TAGS;
            case ITEM -> null;
            case ITEM_TAG -> ITEM_TAGS;
            case FRIENDS -> null;
            case ENTITY -> null;
            case ENTITY_TAG -> ENTITY_SPAWN_TAGS;
        };

        SuggestionProvider<CommandSourceStack> suggestCurrentTags = switch(type) {
            case BLOCK -> null;
            case BLOCK_TAG -> CURRENT_BLOCK_TAGS;
            case ITEM -> null;
            case ITEM_TAG -> CURRENT_ITEM_TAGS;
            case FRIENDS -> null;
            case ENTITY -> null;
            case ENTITY_TAG -> CURRENT_ENTITY_SPAWN_TAGS;
        };

        return Commands.literal(type.getCommand())
                ///// WHITELIST ADD /////
                .then(Commands.literal(ADD)

                        .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                .suggests(OWNER_ESTATE_NAMES)
                                .then(Commands.argument(TAG_NAME, ResourceLocationArgument.id())
                                        .suggests(suggestTags)
                                        .executes(source -> {
                                            return addToEstate(source.getSource(),
                                                    StringArgumentType.getString(source, ESTATE_NAME),
                                                    ResourceLocationArgument.getId(source, TAG_NAME),
                                                    type);
                                        })
                                )
                        )
                )
                ///// ITEM TAGS WHITELIST LIST /////
                .then(Commands.literal(LIST)

                        .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                .suggests(OWNER_ESTATE_NAMES)
                                .executes(source -> {
                                    return listFromEstate(source.getSource(),
                                            StringArgumentType.getString(source, ESTATE_NAME),
                                            type);
                                })

                        )
                )
                ///// TAGS WHITELIST REMOVE /////
                .then(Commands.literal(REMOVE)

                        .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                .suggests(OWNER_ESTATE_NAMES)
                                .then(Commands.argument(TAG_NAME, ResourceLocationArgument.id())
                                        .suggests(suggestCurrentTags)
                                        .executes(source -> {
                                            return removeFromEstate(source.getSource(),
                                                    StringArgumentType.getString(source, ESTATE_NAME),
                                                    ResourceLocationArgument.getId(source, TAG_NAME),
                                                    type);
                                        })
                                )
                        )

                );

    }

    public LiteralArgumentBuilder<CommandSourceStack> buildOps(CommandBuildContext buildContext, WhitelistType type) {
        SuggestionProvider<CommandSourceStack> suggestTags = switch(type) {
            case BLOCK -> null;
            case BLOCK_TAG -> BLOCK_TAGS;
            case ITEM -> null;
            case ITEM_TAG -> ITEM_TAGS;
            case FRIENDS -> null;
            case ENTITY -> null;
            case ENTITY_TAG -> ENTITY_SPAWN_TAGS;
        };

        SuggestionProvider<CommandSourceStack> suggestCurrentTags = switch(type) {
            case BLOCK -> null;
            case BLOCK_TAG -> CURRENT_BLOCK_TAGS;
            case ITEM -> null;
            case ITEM_TAG -> CURRENT_ITEM_TAGS;
            case FRIENDS -> null;
            case ENTITY -> null;
            case ENTITY_TAG -> CURRENT_ENTITY_SPAWN_TAGS;
        };

        return Commands.literal(type.getCommand())
                ///// TAG WHITELIST ADD /////
                .then(Commands.literal(ADD)
                        .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                                .suggests(OPS_OWNER_NAMES)
                                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                        .suggests(OPS_OWNER_ESTATE_NAMES)
                                        .then(Commands.argument(TAG_NAME, ResourceLocationArgument.id())
                                                .suggests(suggestTags)
                                                .executes(source -> {
                                                    return addToEstate(source.getSource(),
                                                            StringArgumentType.getString(source, OWNER_NAME),
                                                            StringArgumentType.getString(source, ESTATE_NAME),
                                                            ResourceLocationArgument.getId(source, TAG_NAME),
                                                            type);
                                                })
                                        )
                                )
                        )
                )
                ///// BLOCK TAGS WHITELIST LIST /////
                .then(Commands.literal(LIST)
                        .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                                .suggests(OPS_OWNER_NAMES)
                                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                        .suggests(OPS_OWNER_ESTATE_NAMES)
                                        .executes(source -> {
                                            return listFromEstate(source.getSource(),
                                                    StringArgumentType.getString(source, OWNER_NAME),
                                                    StringArgumentType.getString(source, ESTATE_NAME),
                                                    type);
                                        })
                                )
                        )
                )
                ///// BLOCK TAGS WHITELIST REMOVE /////
                .then(Commands.literal(REMOVE)
                        .then(Commands.argument(OWNER_NAME, StringArgumentType.string())
                                .suggests(OPS_OWNER_NAMES)
                                .then(Commands.argument(ESTATE_NAME, StringArgumentType.string())
                                        .suggests(OWNER_ESTATE_NAMES)
                                        .then(Commands.argument(TAG_NAME, ResourceLocationArgument.id())
                                                .suggests(suggestCurrentTags)
                                                .executes(source -> {
                                                    return removeFromEstate(source.getSource(),
                                                            StringArgumentType.getString(source, OWNER_NAME),
                                                            StringArgumentType.getString(source, ESTATE_NAME),
                                                            ResourceLocationArgument.getId(source, TAG_NAME),
                                                            WhitelistType.BLOCK_TAG);
                                                })
                                        )
                                )
                        )
                );
    }
}
