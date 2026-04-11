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
import mod.gottsch.neo.claimmyland.core.parcel.Parcel;
import mod.gottsch.neo.claimmyland.core.parcel.ParcelType;
import mod.gottsch.neo.claimmyland.core.util.LangUtil;
import mod.gottsch.neo.gottschcore.spatial.Coords;
import mod.gottsch.neo.gottschcore.spatial.ICoords;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.world.level.ChunkPos;

import java.util.Optional;
import java.util.Set;

/**
 * shared display constants, styles, and utility methods used across all formatter classes.
 * all members are package-private to keep them internal to the command.helper package.
 *
 * @author Mark Gottschling on 3/4/2026
 */
class FormatterConstants {

    // ===== BOX-DRAWING CHARACTERS =====

    static final String BRANCH      = "├─ ";
    static final String LAST_BRANCH = "└─ ";
    static final String VERTICAL    = "│  ";
    static final String SPACE       = "   ";
    static final String TITLE_BAR   = "═══════════════════════════════";

    // -------------------------------------------------------------------------
    // Interactive icon characters
    // -------------------------------------------------------------------------

    static final String ICON_INFO     = "ℹ";   // U+2139 — estate details
    static final String ICON_RENAME   = "✎";   // U+270E — rename
    static final String ICON_DELETE   = "✘";   // U+2718 — delete / remove
    static final String ICON_TRANSFER = "⇄";   // U+21C4 — transfer ownership
    static final String ICON_TELEPORT = "➤";   // U+27A4 — solid right arrowhead

    // ===== SHARED STYLES =====

    static final Style BOLD_GOLD         = Style.EMPTY.withBold(true).withColor(ChatFormatting.GOLD);
    static final Style BOLD_LIGHT_PURPLE = Style.EMPTY.withBold(true).withColor(ChatFormatting.LIGHT_PURPLE);
    static final Style BOLD_GREEN        = Style.EMPTY.withBold(true).withColor(ChatFormatting.GREEN);
    static final Style BOLD_AQUA         = Style.EMPTY.withBold(true).withColor(ChatFormatting.AQUA);
    static final Style BOLD_BLUE         = Style.EMPTY.withBold(true).withColor(ChatFormatting.BLUE);
    static final Style BOLD_YELLOW       = Style.EMPTY.withBold(true).withColor(ChatFormatting.YELLOW);
    static final Style BOLD_RED          = Style.EMPTY.withBold(true).withColor(ChatFormatting.RED);


    // private constructor — utility class, not instantiable.
    private FormatterConstants() {}

    // ===== NEWLINE =====

    static Component newline() {
        return Component.literal(LangUtil.NEWLINE);
    }

    // ===== LOCATION FORMATTING =====

    static String formatLocation(ICoords coords) {
        return "[" + coords.getX() + ", " + coords.getY() + ", " + coords.getZ() + "]";
    }

    static String formatLocation(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    static String formatLocation(ChunkPos pos) {
        return "Chunk [" + pos.x + ", " + pos.z + "]";
    }

    // ===== CLICK / HOVER STYLES =====

    static Style tpStyle(ICoords coords) {
        return Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/tp @s " + coords.getX() + " ~ " + coords.getZ()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("chat.coordinates.tooltip")));
    }

    static Style playerEstateDetailsStyle(String estateName) {
        return Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/cml estate details " + estateName))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable(LangUtil.tooltip("estate.details"))));
    }

    static Style opsEstateDetailsStyle(String ownerName, String estateName) {
        return Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/cml estate details " + ownerName + " " + estateName))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable(LangUtil.tooltip("estate.details"))));
    }

    // ===== COLOR RESOLUTION =====

    static ChatFormatting getEstateColor(Estate estate) {
        if (isPlayer(estate))  return ChatFormatting.GREEN;
        if (isCitizen(estate)) return ChatFormatting.LIGHT_PURPLE;
        if (isNation(estate))  return ChatFormatting.BLUE;
        return ChatFormatting.YELLOW;
    }

    static ChatFormatting getParcelColor(Parcel parcel) {
        if (isPlayer(parcel))  return ChatFormatting.GREEN;
        if (isCitizen(parcel)) return ChatFormatting.LIGHT_PURPLE;
        if (isNation(parcel))  return ChatFormatting.BLUE;
        return ChatFormatting.YELLOW;
    }

    // ===== ESTATE TYPE PREDICATES =====

    static boolean isNation(Estate estate) {
        return estate.getParcelType() == ParcelType.NATION;
    }

    static boolean isCitizen(Estate estate) {
        return estate.getParcelType() == ParcelType.CITIZEN;
    }

    static boolean isZone(Estate estate) {
        return estate.getParcelType() == ParcelType.ZONE;
    }

    static boolean isPlayer(Estate estate) {
        return estate.getParcelType() == ParcelType.PLAYER;
    }

    // ===== PARCEL TYPE PREDICATES =====

    static boolean isNation(Parcel parcel) {
        return parcel.getType() == ParcelType.NATION;
    }

    static boolean isCitizen(Parcel parcel) {
        return parcel.getType() == ParcelType.CITIZEN;
    }

    static boolean isZone(Parcel parcel) {
        return parcel.getType() == ParcelType.ZONE;
    }

    static boolean isPlayer(Parcel parcel) {
        return parcel.getType() == ParcelType.PLAYER;
    }

    // ===== SPATIAL UTILITIES =====

    /**
     * returns the smallest X, Y, and Z found across all positions in the set.
     * Note: the returned coords are not necessarily from the same ICoords instance.
     *
     * @param coordsSet a non-empty set of ICoords
     * @return an ICoords of [minX, minY, minZ], or empty if the set is empty
     */
    static Optional<ICoords> getMinXZ(Set<ICoords> coordsSet) {
        if (coordsSet == null || coordsSet.isEmpty()) return Optional.empty();

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;

        for (ICoords coords : coordsSet) {
            if (coords.getX() < minX) minX = coords.getX();
            if (coords.getY() < minY) minY = coords.getY();
            if (coords.getZ() < minZ) minZ = coords.getZ();
        }

        return Optional.of(Coords.of(minX, minY, minZ));
    }

    /**
     * ℹ icon — runs estate details command immediately.
     */
    /**
     * ℹ icon — runs estate details command immediately.
     */
    static Component estateInfoIcon(String estateName) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/cml estate details " + estateName))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("View estate details")));
        return Component.literal(" " + ICON_INFO).withStyle(style);
    }

    /**
     * ✎ icon — suggests rename command with trailing space for player to type new name.
     */
    static Component estateRenameIcon(String estateName) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.YELLOW)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/cml estate rename " + estateName + " "))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Rename this estate")));
        return Component.literal(" " + ICON_RENAME).withStyle(style);
    }

    /**
     * ✘ icon — suggests demolish command (returns a deed) for player/citizen/nation estates.
     * Use estaetDemolishIcon() for all non-zone estate types.
     */
    static Component estateDemolishIcon(String estateName) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.RED)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/cml estate demolish " + estateName))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Demolish this estate (returns deed)")));
        return Component.literal(" " + ICON_DELETE).withStyle(style);
    }

    /**
     * ✘ icon — suggests remove command for zone estates only.
     * Requires the nation name as the first argument since remove takes
     * both nationName and zoneName.
     *
     * @param nationName the nation this zone belongs to
     * @param zoneName   the zone estate name to remove
     */
    static Component estateRemoveIcon(String nationName, String zoneName) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.RED)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/cml estate remove " + nationName + " " + zoneName))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Remove this zone estate")));
        return Component.literal(" " + ICON_DELETE).withStyle(style);
    }

    /**
     * ⇄ icon — suggests transfer command with trailing space for player to type new owner.
     */
    static Component estateTransferIcon(String estateName) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/cml estate transfer " + estateName + " "))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Transfer estate ownership")));
        return Component.literal(" " + ICON_TRANSFER).withStyle(style);
    }

    /**
     * ✘ icon — suggests parcel demolish command.
     * Takes both estate name and parcel name since the command requires both.
     *
     * @param estateName the estate the parcel belongs to
     * @param parcelName the parcel name to demolish
     */
    static Component parcelDemolishIcon(String estateName, String parcelName) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.RED)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/cml parcel demolish " + estateName + " " + parcelName))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Demolish this parcel (returns deed)")));
        return Component.literal(" " + ICON_DELETE).withStyle(style);
    }

    /**
     * ↗ icon — suggests teleport command for a parcel location.
     */
    static Component parcelTeleportIcon(ICoords coords) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/tp @s " + coords.getX() + " ~ " + coords.getZ()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Teleport to this parcel")));
        return Component.literal(" " + ICON_TELEPORT).withStyle(style);
    }

    /**
     * ✚ icon — suggests player whitelist add command on the section header.
     * Puts the cursor ready for the player name to be typed.
     *
     * @param estateName the estate name (used in the command)
     */
    static Component playerWhitelistAddIcon(String estateName) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/cml estate whitelist friends add " + estateName + " "))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Add player to whitelist")));
        return Component.literal(" \u271A").withStyle(style);
    }

    static Component playerBlacklistAddIcon(String estateName) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.RED)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/cml estate blacklist add " + estateName + " "))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Add player to blacklist")));
        return Component.literal(" \u271A").withStyle(style);
    }

    /**
     * ✘ icon — suggests player whitelist remove command.
     *
     * @param estateName the estate name (used in the command)
     * @param playerName the player name to remove
     */
    static Component playerWhitelistRemoveIcon(String estateName, String playerName) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.RED)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/cml estate whitelist friends remove " + estateName + " " + playerName))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Remove " + playerName + " from whitelist")));
        return Component.literal(" " + ICON_DELETE).withStyle(style);
    }

    static Component playerBlacklistRemoveIcon(String estateName, String playerName) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.RED)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/cml estate blacklist remove " + estateName + " " + playerName))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Remove " + playerName + " from blacklist")));
        return Component.literal(" " + ICON_DELETE).withStyle(style);
    }

    /**
     * ℹ icon — ops variant that includes owner name.
     */
    static Component estateInfoIconOps(String ownerName, String estateName) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/cml-ops estate details " + ownerName + " " + estateName))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("View estate details")));
        return Component.literal(" " + ICON_INFO).withStyle(style);
    }

    static Component estateRenameIconOps(String ownerName, String estateName) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.YELLOW)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/cml-ops estate rename " + ownerName + " " + estateName + " "))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Rename this estate")));
        return Component.literal(" " + ICON_RENAME).withStyle(style);
    }

    static Component estateDemolishIconOps(String ownerName, String estateName) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.RED)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/cml-ops estate demolish " + ownerName + " " + estateName))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Demolish this estate (returns deed)")));
        return Component.literal(" " + ICON_DELETE).withStyle(style);
    }

    static Component estateRemoveIconOps(String ownerName, String nationName, String zoneName) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.RED)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/cml-ops estate remove " + ownerName + " " + nationName + " " + zoneName))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Remove this zone estate")));
        return Component.literal(" " + ICON_DELETE).withStyle(style);
    }

    static Component estateTransferIconOps(String ownerName, String estateName) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/cml-ops estate transfer " + ownerName + " " + estateName + " "))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Transfer estate ownership")));
        return Component.literal(" " + ICON_TRANSFER).withStyle(style);
    }

    static Component playerWhitelistAddIconOps(String ownerName, String estateName) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/cml-ops estate whitelist friends add " + ownerName + " " + estateName + " "))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Add player to whitelist")));
        return Component.literal(" \u271A").withStyle(style);
    }

    static Component playerWhitelistRemoveIconOps(String ownerName, String estateName, String playerName) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.RED)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/cml-ops estate whitelist friends remove " + ownerName + " " + estateName + " " + playerName))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Remove " + playerName + " from whitelist")));
        return Component.literal(" " + ICON_DELETE).withStyle(style);
    }

    static Component playerBlacklistAddIconOps(String ownerName, String estateName) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.RED)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/cml-ops estate blacklist add " + ownerName + " " + estateName + " "))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Add player to blacklist")));
        return Component.literal(" \u271A").withStyle(style);
    }

    static Component playerBlacklistRemoveIconOps(String ownerName, String estateName, String playerName) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.RED)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/cml-ops estate blacklist remove " + ownerName + " " + estateName + " " + playerName))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Remove " + playerName + " from blacklist")));
        return Component.literal(" " + ICON_DELETE).withStyle(style);
    }
}
