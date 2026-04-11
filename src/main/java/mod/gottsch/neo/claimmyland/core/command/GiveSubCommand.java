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

package mod.gottsch.neo.claimmyland.core.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.item.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.item.ItemStack;

import java.util.stream.Stream;

import static mod.gottsch.neo.claimmyland.core.command.helper.CommandHelper.unexpectedError;

/**
 * @author by Mark Gottschling on 3/4/2026
 */
public class GiveSubCommand implements SubCommand {
    public static final String GIVE = "give";
    public static final String GIVE_ITEM = "give_item";

    static final SuggestionProvider<CommandSourceStack> GIVABLE_ITEMS = (source, builder) -> {
        return SharedSuggestionProvider.suggest(Stream.of("border_stone", "citizen_tool", "zoning_tool"), builder);
    };

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal(GIVE)
                .then(Commands.argument(GIVE_ITEM, StringArgumentType.greedyString())
                        .suggests(GIVABLE_ITEMS)
                        .executes(source -> {
                            return give(source.getSource(), StringArgumentType.getString(source, GIVE_ITEM));
                        })
                );
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {
        return build();
    }

    public int give(CommandSourceStack source, String giveItem) {
        try {
            ItemStack itemStack = switch(giveItem.toLowerCase()) {
                case "border_stone" -> new ItemStack(ModItems.BORDER_STONE.get());
                case "citizen_tool" -> new ItemStack(ModItems.CITIZEN_PLACEMENT_TOOL.get()); // TODO test if you are a nation owner
                case "zoning_tool" -> new ItemStack(ModItems.ZONING_PLACEMENT_TOOL.get()); // TODO test if you are a nation owner
                default -> ItemStack.EMPTY;
            };

            if (itemStack != ItemStack.EMPTY) {
                // attempt to add the deed item to the player inventory
                source.getPlayerOrException().getInventory().add(itemStack);
            }
            return 1;
        } catch(Exception e) {
            ClaimMyLand.LOGGER.error("an error occurred giving item:", e);
            unexpectedError(source);
            return 0;
        }
    }
}
