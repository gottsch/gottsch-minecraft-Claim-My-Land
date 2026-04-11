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

package mod.gottsch.neo.claimmyland.core.command.helper;

import mod.gottsch.neo.claimmyland.core.estate.Estate;
import mod.gottsch.neo.claimmyland.core.registry.PlayerRegistry;
import mod.gottsch.neo.claimmyland.core.util.LangUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import java.util.*;

import static mod.gottsch.neo.claimmyland.core.command.helper.FormatterConstants.*;

/**
 * formats all whitelist types — player, block, block-tag, item, item-tag, entity,
 * entity-tag — for display in chat. Extracted from EstateDisplayFormatter.
 *
 * <p>all public methods return {@code List<Component>} (one entry per logical line)
 * so callers can append to an existing message list or forward to Chat Plus.</p>
 *
 * @author Mark Gottschling on 3/4/2026
 */
public class WhitelistFormatter {

    // ===== STAND-ALONE PLAYER WHITELIST =====

    /**
     * formats the player (friends) whitelist for a stand-alone display with header.
     * used by the {@code /cml whitelist player list} command family.
     *
     * @param level    the server level (used for UUID → name resolution)
     * @param players  the set of player UUIDs on the whitelist
     * @param title    the header title string (e.g. "Friends Whitelist — MyEstate")
     * @param estateId the estate's UUID (reserved for future clickable icon support)
     * @return formatted component lines
     */
    public static List<Component> formatStandAlonePlayerWhitelist(ServerLevel level,
                                                                  Set<UUID> players,
                                                                  String title,
                                                                  UUID estateId,
                                                                  String estateName) {
        List<Component> lines = new ArrayList<>();

        // header
        lines.add(Component.literal(TITLE_BAR).withStyle(BOLD_YELLOW));
        lines.add(Component.literal(LangUtil.INDENT4 + title).withStyle(BOLD_YELLOW) .append(playerWhitelistAddIcon(estateName)));
        lines.add(Component.literal(TITLE_BAR).withStyle(BOLD_YELLOW));
        lines.add(newline());

        if (players == null || players.isEmpty()) {
            lines.add(Component.literal("No friends whitelisted").withStyle(ChatFormatting.GRAY));
            lines.add(newline());
            return lines;
        }

        lines.add(Component.literal("Total Friends: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(players.size())).withStyle(ChatFormatting.WHITE)));
        lines.add(newline());

        lines.addAll(formatPlayerList(level, players, "", estateId, estateName));
        lines.add(newline());
        return lines;
    }

    // ===== STAND-ALONE GENERIC WHITELIST =====

    /**
     * formats a generic string-based whitelist (block, item, entity, or their tag variants)
     * for stand-alone display with header.
     *
     * @param data     the set of registry-name strings on the whitelist
     * @param type     the whitelist type (drives colour and label)
     * @param title    the header title string
     * @param estateId the estate's UUID (reserved for future clickable icon support)
     * @return formatted component lines
     */
    public static List<Component> formatStandAloneGenericWhitelist(Set<String> data,
                                                                   WhitelistType type,
                                                                   String title,
                                                                   UUID estateId) {
        List<Component> lines = new ArrayList<>();

        // header
        lines.add(Component.literal(TITLE_BAR).withStyle(ChatFormatting.BOLD, type.getColor()));
        lines.add(Component.literal(LangUtil.INDENT4 + title).withStyle(ChatFormatting.BOLD, type.getColor()));
        lines.add(Component.literal(TITLE_BAR).withStyle(ChatFormatting.BOLD, type.getColor()));
        lines.add(newline());

        if (data == null || data.isEmpty()) {
            lines.add(Component.literal("No entries in whitelist").withStyle(ChatFormatting.GRAY));
            lines.add(newline());
            return lines;
        }

        lines.add(Component.literal("Total: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(data.size())).withStyle(ChatFormatting.WHITE)));
        lines.add(newline());

        lines.addAll(formatGenericList(data, type, ""));
        lines.add(newline());
        return lines;
    }

    // ===== ALL WHITELISTS FOR AN ESTATE (summary view) =====

    /**
     * formats the player (friends) whitelist section as it appears inside the estate
     * list view — a compact inline summary.
     *
     * @param level   the server level
     * @param estate  the estate whose player whitelist to format
     * @return formatted component lines
     */
    public static List<Component> formatEstateWhitelists(ServerLevel level, Estate estate) {
        List<Component> lines = new ArrayList<>();

        Set<UUID> players = estate.getPlayerWhitelist();
        lines.add(Component.literal("")
                .append(Component.literal("▼ FRIENDS WHITELIST").withStyle(BOLD_AQUA))
                .append(playerWhitelistAddIcon(estate.getName())));
        if (!players.isEmpty()) {
            lines.add(Component.literal(LangUtil.INDENT2 + "Total: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.valueOf(players.size())).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" friends").withStyle(ChatFormatting.GRAY)));
            lines.addAll(formatEstateDetailsPlayerWhitelist(level, players, LangUtil.INDENT2, estate.getName()));
        }
        lines.add(newline());

        return lines;
    }

    // ===== PACKAGE-PRIVATE: PLAYER WHITELIST VARIANTS =====
    // Used internally by EstateDisplayFormatter for inline estate-list and estate-details views.

    /**
     * formats the player whitelist as it appears inline inside the estate <em>list</em>
     * view — grouped up to {@code maxPerRow} names per line for compact display.
     *
     * @param level    the server level
     * @param whitelist the set of player UUIDs
     * @param indent   indentation prefix
     * @return formatted component lines
     */
//    static List<Component> formatEstateListPlayerWhitelist(ServerLevel level,
//                                                           Set<UUID> whitelist, String indent, String estateName) {
//        return formatPlayerList(level, whitelist, indent, null, estateName);
//    }

    public static List<Component> formatEstateListPlayerWhitelist(ServerLevel level,
                                                                  Set<UUID> blacklist,
                                                                  String indent,
                                                                  String estateName,
                                                                  @Nullable String ownerName) {
        return formatPlayerList(level, blacklist, indent, estateName, ownerName, true);
    }

    public static List<Component> formatEstateListPlayerBlacklist(ServerLevel level,
                                                                  Set<UUID> blacklist,
                                                                  String indent,
                                                                  String estateName,
                                                                  @Nullable String ownerName) {
        return formatPlayerList(level, blacklist, indent, estateName, ownerName, false);
    }

    // Update the existing private formatPlayerList() to handle blacklist remove icons
    // by adding a boolean isWhitelist parameter, or add a separate private helper.
    // Simplest approach — add a new private helper formatBlacklistEntry():

    private static List<Component> formatPlayerList(ServerLevel level,
                                                    Set<UUID> playerIds,
                                                    String indent,
                                                    String estateName,
                                                    @Nullable String ownerName,
                                                    boolean isWhitelist) {
        List<Component> lines = new ArrayList<>();
        for (UUID id : playerIds) {
            String name = PlayerRegistry.getPlayerName(level, id).orElse(id.toString().substring(0, 8));
            MutableComponent line = Component.literal(indent + LangUtil.INDENT2 + name)
                    .withStyle(ChatFormatting.WHITE);
            if (isWhitelist) {
                line.append(ownerName != null
                        ? playerWhitelistRemoveIconOps(ownerName, estateName, name)
                        : playerWhitelistRemoveIcon(estateName, name));
            } else {
                line.append(ownerName != null
                        ? playerBlacklistRemoveIconOps(ownerName, estateName, name)
                        : playerBlacklistRemoveIcon(estateName, name));
            }
            lines.add(line);
        }
        return lines;
    }

    /**
     * formats the player whitelist as it appears inline inside the estate <em>details</em>
     * view — three names per line with tree-branch prefixes.
     *
     * @param level   the server level
     * @param players the set of player UUIDs
     * @param indent  indentation prefix
     * @return formatted component lines
     */
    static List<Component> formatEstateDetailsPlayerWhitelist(ServerLevel level,
                                                              Set<UUID> players, String indent, String estateName) {
        return formatPlayerList(level, players, indent, null, estateName);
    }
//    static List<Component> formatEstateDetailsPlayerWhitelist(ServerLevel level,
//                                                              Set<UUID> players,
//                                                              String indent) {
//        List<Component> lines = new ArrayList<>();
//
//        List<String> playerNames = players.stream()
//                .map(uuid -> PlayerRegistry.getPlayerName(level, uuid))
//                .flatMap(Optional::stream)
//                .toList();
//
//        List<String> sortedPlayers = new ArrayList<>(playerNames);
//        sortedPlayers.sort(String.CASE_INSENSITIVE_ORDER);
//
//        // three players per line with tree-branch prefix
//        for (int i = 0; i < sortedPlayers.size(); i += 3) {
//            MutableComponent component = Component.literal(indent);
//            boolean isLastGroup = i + 3 >= sortedPlayers.size();
//            component.append(Component.literal(isLastGroup ? LAST_BRANCH : BRANCH));
//            for (int j = 0; j < 3 && (i + j) < sortedPlayers.size(); j++) {
//                if (j > 0) component.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
//                component.append(Component.literal(sortedPlayers.get(i + j)).withStyle(ChatFormatting.WHITE));
//            }
//            lines.add(component);
//        }
//        lines.add(newline());
//        return lines;
//    }

    // ===== PACKAGE-PRIVATE: GENERIC LIST =====

    /**
     * formats a sorted set of registry-name strings as a tree list, one entry per line,
     * coloured by whitelist type.
     *
     * @param data   the set of strings to display
     * @param type   the whitelist type (drives colour)
     * @param indent indentation prefix
     * @return formatted component lines
     */
    static List<Component> formatGenericList(Set<String> data, WhitelistType type, String indent) {
        List<Component> lines = new ArrayList<>();

        List<String> sorted = data.stream().sorted().toList();

        for (int i = 0; i < sorted.size(); i++) {
            boolean isLast = i == sorted.size() - 1;
            String branch = isLast ? LAST_BRANCH : BRANCH;
            lines.add(Component.literal(indent + branch)
                    .append(Component.literal(sorted.get(i)).withStyle(type.getColor())));
        }
        lines.add(newline());
        return lines;
    }

    // ===== PRIVATE: STAND-ALONE PLAYER LIST =====

    /**
     * formats a player whitelist as an alphabetically sorted, one-per-line list.
     * Used only by {@link #formatStandAlonePlayerWhitelist}.
     */
    private static List<Component> formatPlayerList(ServerLevel level,
                                                    Set<UUID> players, String indent, UUID estateId, String estateName) {

        List<String> sortedPlayers = players.stream()
                .map(uuid -> PlayerRegistry.getPlayerName(level, uuid)
                        .orElse("Unknown Player [" + uuid + "]"))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        List<Component> lines = new ArrayList<>();
        for (int i = 0; i < sortedPlayers.size(); i++) {
            boolean isLast = i == sortedPlayers.size() - 1;
            String playerName = sortedPlayers.get(i);
            MutableComponent line = Component.literal(indent + (isLast ? LAST_BRANCH : BRANCH))
                    .append(Component.literal(playerName).withStyle(ChatFormatting.WHITE))
                    .append(playerWhitelistRemoveIcon(estateName, playerName));
            lines.add(line);
        }
        return lines;
    }

    // private constructor — static utility class.
    private WhitelistFormatter() {}
}