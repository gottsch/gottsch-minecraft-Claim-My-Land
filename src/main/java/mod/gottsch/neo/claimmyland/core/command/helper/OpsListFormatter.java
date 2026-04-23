/*
 * This file is part of Claim My Land.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
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
package mod.gottsch.neo.claimmyland.core.command.helper;

import mod.gottsch.neo.claimmyland.core.registry.PlayerRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static mod.gottsch.neo.claimmyland.core.command.helper.FormatterConstants.*;

/**
 * formats the CML ops list for display in chat with interactive add/remove icons.
 *
 * <p>Matches the visual style of {@link EstateDisplayFormatter#formatEstateList}:
 * gold title bar, total count, tree-branch entries. Entries display the player's
 * name when resolvable; otherwise the trailing twelve characters of the UUID
 * with the full UUID on hover (via {@link FormatterConstants#hoverableUuid}).</p>
 *
 * @author Mark Gottschling on Apr 19, 2026
 */
public class OpsListFormatter {

    // ===== OPS LIST =====

    public static List<Component> formatOpsList(ServerLevel level, List<UUID> ops) {
        List<Component> messages = new ArrayList<>();

        // header
        messages.add(Component.literal(TITLE_BAR).withStyle(BOLD_GOLD));
        messages.add(Component.literal("         CML OPS LIST").withStyle(BOLD_GOLD));
        messages.add(Component.literal(TITLE_BAR).withStyle(BOLD_GOLD));
        messages.add(newline());

        // count + add button
        messages.add(Component.literal("Total Ops: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(ops.size())).withStyle(ChatFormatting.WHITE))
                .append(opsAddIcon()));

        if (ops.isEmpty()) {
            messages.add(Component.literal("No CML ops configured.").withStyle(ChatFormatting.YELLOW));
            messages.add(newline());
            return messages;
        }

        messages.add(newline());

        // tree-branch entries
        Iterator<UUID> iterator = ops.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            boolean isLast = !iterator.hasNext();
            String prefix = isLast ? LAST_BRANCH : BRANCH;

            Optional<String> nameOpt = PlayerRegistry.getPlayerName(level, uuid);
            Component nameComponent = nameOpt
                    .<Component>map(name -> Component.literal(name).withStyle(ChatFormatting.WHITE))
                    .orElseGet(() -> hoverableUuid(uuid));

            // Remove icon target: prefer the resolved name; if we only have a
            // UUID, pass the UUID string so the remove command still resolves.
            String removeArg = nameOpt.orElse(uuid.toString());

            messages.add(Component.literal(prefix).withStyle(ChatFormatting.GRAY)
                    .append(nameComponent)
                    .append(opsRemoveIcon(removeArg)));
        }

        messages.add(newline());
        return messages;
    }

    // ===== ICONS =====

    /**
     * ✚ icon — suggests the ops add command with trailing space for player
     * name entry.
     */
    private static Component opsAddIcon() {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/cml-ops ops_list add "))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Add op")));
        return Component.literal(" " + ICON_ADD).withStyle(style);
    }

    /**
     * ✘ icon — suggests the ops remove command with the target already filled in.
     */
    private static Component opsRemoveIcon(String target) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.RED)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/cml-ops ops_list remove " + target))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Remove " + target + " from ops")));
        return Component.literal(" " + ICON_DELETE).withStyle(style);
    }

    // private constructor — static utility class.
    private OpsListFormatter() {}
}