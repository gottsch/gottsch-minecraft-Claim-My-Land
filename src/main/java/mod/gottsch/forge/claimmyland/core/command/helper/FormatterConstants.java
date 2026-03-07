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

import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
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

    // ===== SHARED STYLES =====

    static final Style BOLD_GOLD         = Style.EMPTY.withBold(true).withColor(ChatFormatting.GOLD);
    static final Style BOLD_LIGHT_PURPLE = Style.EMPTY.withBold(true).withColor(ChatFormatting.LIGHT_PURPLE);
    static final Style BOLD_GREEN        = Style.EMPTY.withBold(true).withColor(ChatFormatting.GREEN);
    static final Style BOLD_AQUA         = Style.EMPTY.withBold(true).withColor(ChatFormatting.AQUA);
    static final Style BOLD_BLUE         = Style.EMPTY.withBold(true).withColor(ChatFormatting.BLUE);
    static final Style BOLD_YELLOW       = Style.EMPTY.withBold(true).withColor(ChatFormatting.YELLOW);
    static final Style BOLD_RED          = Style.EMPTY.withBold(true).withColor(ChatFormatting.RED);

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

    // private constructor — utility class, not instantiable.
    private FormatterConstants() {}
}
