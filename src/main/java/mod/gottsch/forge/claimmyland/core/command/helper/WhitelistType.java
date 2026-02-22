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

package mod.gottsch.forge.claimmyland.core.command.helper;

import net.minecraft.ChatFormatting;

/**
 * @author by Mark Gottschling on 2/20/2026
 */
public enum WhitelistType {
    BLOCK("block", "blocks","BLOCK WHITELIST", ChatFormatting.GREEN),
    BLOCK_TAG("block tag", "block_tags", "BLOCK TAG WHITELIST", ChatFormatting.DARK_GREEN),
    ITEM("item", "items", "ITEM WHITELIST", ChatFormatting.AQUA),
    ITEM_TAG("item tag", "item_tags", "ITEM TAG WHITELIST", ChatFormatting.DARK_AQUA),
    FRIENDS("friends", "friends", "FRIENDS WHITELIST", ChatFormatting.YELLOW),
    ENTITY("entity", "entities", "ENTITY WHITELIST", ChatFormatting.LIGHT_PURPLE),
    ENTITY_TAG("entity tag", "entity_tags", "ENTITY TAG WHITELIST", ChatFormatting.DARK_PURPLE);

    private final String value;
    private final String command;
    private final String title;
    private final ChatFormatting color;

    WhitelistType(String value, String command, String title, ChatFormatting color) {
        this.value = value;
        this.command = command;
        this.title = title;
        this.color = color;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public String getCommand() {
        return command;
    }

    public String getTitle() {
        return title;
    }

    public String getValue() {
        return value;
    }
}
