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

import mod.gottsch.forge.claimmyland.core.parcel.NationalizedParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.registry.PlayerRegistry;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static mod.gottsch.forge.claimmyland.core.command.helper.FormatterConstants.*;

/**
 * formats parcel lists and individual parcel entries for display in chat.
 * extracted from EstateDisplayFormatter to keep parcel-specific formatting isolated.
 *
 * @author Mark Gottschling on 3/4/2026
 */
public class ParcelDisplayFormatter {

    // ===== PARCEL LIST =====

    /**
     * appends a formatted parcel list — header plus one entry per parcel — to the
     * supplied {@code lines} list.
     *
     * @param level   the server level (used for player name resolution)
     * @param lines   the list to append formatted components to
     * @param parcels the parcels to display
     * @param isOps   whether the viewer has operator permissions (enables clickable owner links)
     * @return the same {@code lines} list, for fluent chaining
     */
    public static List<Component> formatParcelList(ServerLevel level,
                                                   List<Component> lines,
                                                   List<Parcel> parcels,
                                                   boolean isOps) {
        // header
        lines.add(Component.literal(TITLE_BAR).withStyle(BOLD_GOLD));
        lines.add(Component.literal("         PARCEL LIST").withStyle(BOLD_GOLD));
        lines.add(Component.literal(TITLE_BAR).withStyle(BOLD_GOLD));
        lines.add(newline());
        lines.add(Component.literal("Total Parcels: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(parcels.size())).withStyle(ChatFormatting.WHITE)));
        lines.add(newline());

        if (parcels.isEmpty()) {
            lines.add(Component.literal("No parcels found.").withStyle(ChatFormatting.YELLOW));

            lines.add(newline());
            return lines;
        }

        for (int i = 0; i < parcels.size(); i++) {
            Parcel parcel = parcels.get(i);
            boolean isLast = (i == parcels.size() - 1);
            lines.addAll(formatParcelEntry(level, parcel, isLast, "", isOps));

            if (!isLast) {
                lines.add(newline());
            }
        }
        lines.add(newline());
        return lines;
    }

    // ===== PARCEL ENTRY =====

    /**
     * Formats a single parcel as a tree-style entry with owner, position, size, type,
     * and (for nationalized types) the parent nation name.
     *
     * @param level  the server level (used for player name resolution)
     * @param parcel the parcel to format
     * @param isLast whether this is the last item in the list (affects branch character)
     * @param prefix indentation prefix passed down from the parent context
     * @param isOps  whether the viewer has operator permissions
     * @return a list of formatted component lines
     */
    public static List<Component> formatParcelEntry(ServerLevel level,
                                                    Parcel parcel,
                                                    boolean isLast,
                                                    String prefix,
                                                    boolean isOps) {
        List<Component> lines = new ArrayList<>();
        String branch = isLast ? LAST_BRANCH : BRANCH;
        ChatFormatting color = getParcelColor(parcel);

        // parcel header line: branch + name [ID: ...]
//        lines.add(Component.literal(prefix + branch)
//                .append(Component.literal(parcel.getName()).withStyle(color, ChatFormatting.BOLD))
//                .append(Component.literal(" [ID: ").withStyle(ChatFormatting.GRAY))
//                .append(Component.literal(parcel.getId().toString()).withStyle(ChatFormatting.WHITE))
//                .append(Component.literal("]").withStyle(ChatFormatting.GRAY)));
        lines.add(Component.literal(prefix + branch)
                .append(Component.literal(parcel.getName()).withStyle(color, ChatFormatting.BOLD))
                .append(Component.literal(" [ID: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(parcel.getId().toString()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("]").withStyle(ChatFormatting.GRAY))
                .append(parcelDemolishIcon(parcel.getEstate().getName(), parcel.getName()))
                .append(parcelTeleportIcon(parcel.getMinCoords())));

        // indent for detail lines
        String indent = prefix + (isLast ? SPACE : VERTICAL);

        Optional<String> optionalOwnerName = PlayerRegistry.getPlayerName(level, parcel.getEstate().getOwnerId());

        // clickable estate name
        MutableComponent clickableName = Component.literal(parcel.getEstate().getName())
                .withStyle(color, ChatFormatting.BOLD);
        if (isOps && optionalOwnerName.isPresent()) {
            clickableName.withStyle(opsEstateDetailsStyle(optionalOwnerName.get(), parcel.getEstate().getName()));
        } else if (!isOps) {
            clickableName.withStyle(playerEstateDetailsStyle(parcel.getEstate().getName()));
        }

        lines.add(Component.literal(indent)
                .append(Component.literal("Estate: ").withStyle(ChatFormatting.GRAY))
                .append(clickableName)
                .append(Component.literal(" [ID: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(parcel.getEstate().getId().toString()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("]").withStyle(ChatFormatting.GRAY)));

        if (parcel.getEstate().isRelinquished()) {
            lines.add(Component.literal(indent).withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(LangUtil.INDENT4))
                    .append(Component.literal("-RELINQUISHED-").withStyle(ChatFormatting.RED)));
        }

        lines.add(Component.literal(indent)
                .append(Component.literal("Owner: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(
                        optionalOwnerName.orElse(parcel.getEstate().getOwnerId().toString())
                ).withStyle(ChatFormatting.WHITE)));

        // location
        lines.add(Component.literal(indent)
                .append(Component.literal("Dimension: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(parcel.getDimension()).withStyle(ChatFormatting.WHITE)));
//        lines.add(Component.literal(indent)
//                .append(Component.literal("Min Pos: ").withStyle(ChatFormatting.GRAY))
//                .append(Component.literal(formatLocation(parcel.getMinCoords()))
//                        .withStyle(ChatFormatting.GREEN)
//                        .withStyle(tpStyle(parcel.getMinCoords()))));
        lines.add(Component.literal(indent)
                .append(Component.literal("Min Pos: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(formatLocation(parcel.getMinCoords()))
                        .withStyle(ChatFormatting.GREEN)
                        .withStyle(tpStyle(parcel.getMinCoords())))
                .append(parcelTeleportIcon(parcel.getMinCoords())));

        lines.add(Component.literal(indent)
                .append(Component.literal("Max Pos: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(formatLocation(parcel.getMaxCoords()))
                        .withStyle(ChatFormatting.GREEN)
                        .withStyle(tpStyle(parcel.getMaxCoords()))));

        // size
        if (parcel.getSize() != null) {
            ICoords size = ModUtil.getSize(parcel.getSize());
            lines.add(Component.literal(indent)
                    .append(Component.literal("Size: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(size.getX() + "x" + size.getY() + "x" + size.getZ())
                            .withStyle(ChatFormatting.WHITE)));
        }

        // type + optional nation reference
        if (isPlayer(parcel)) {
            lines.add(Component.literal(indent)
                    .append(Component.literal("Type: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("Player").withStyle(color)));
        } else if (isNation(parcel)) {
            lines.add(Component.literal(indent)
                    .append(Component.literal("Type: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("Nation").withStyle(color)));
        } else if (isZone(parcel)) {
            lines.add(Component.literal(indent)
                    .append(Component.literal("Type: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("Zone").withStyle(color)));
            NationalizedParcel nationalizedParcel = (NationalizedParcel) parcel;
            if (nationalizedParcel.getNationEstate().getId() != null) {
                lines.add(Component.literal(indent)
                        .append(Component.literal("Nation: ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(nationalizedParcel.getNationEstate().getName())
                                .withStyle(ChatFormatting.WHITE)));
            }
        } else if (isCitizen(parcel)) {
            lines.add(Component.literal(indent)
                    .append(Component.literal("Type: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("Citizen").withStyle(color)));
            NationalizedParcel nationalizedParcel = (NationalizedParcel) parcel;
            if (nationalizedParcel.getNationEstate().getId() != null) {
                lines.add(Component.literal(indent)
                        .append(Component.literal("Nation: ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(nationalizedParcel.getNationEstate().getName())
                                .withStyle(ChatFormatting.WHITE)));
            }
        }

        return lines;
    }

    /**
     * formats the result of a "claimed by" query for a single parcel.
     * call once per parcel; accumulate results for multi-parcel positions.
     *
     * @param estateName  the estate name (displayed as the bold title)
     * @param ownerName   resolved player name or relinquished label
     * @param parcelType  serialized type name, e.g. "nation", "citizen"
     * @param coords      parcel anchor coords string
     * @param start       absolute min-corner coords string
     * @param end         absolute max-corner coords string
     * @param size        size string (x y z)
     * @param borderType  access-type label for nation parcels, or null for all others
     */
    public static List<Component> formatClaimedBy(
            String estateName,
            String ownerName,
            String parcelType,
            String coords,
            String start,
            String end,
            String size,
            String borderType,
            String dimension
    ) {
        List<Component> lines = new ArrayList<>();

        // section heading
        lines.add(Component.literal("▼ CLAIMED PARCEL").withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA));

        // estate name as the branch title
        lines.add(Component.literal(BRANCH)
                .append(Component.literal(estateName).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)));

        String indent = VERTICAL;

        // properties indented under the branch
        lines.add(Component.literal(indent)
                .append(Component.literal("Owner: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(ownerName).withStyle(ChatFormatting.YELLOW)));

        lines.add(Component.literal(indent)
                .append(Component.literal("Type: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(parcelType).withStyle(ChatFormatting.GOLD)));

        lines.add(Component.literal(indent)
                .append(Component.literal("Dimension: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(dimension).withStyle(ChatFormatting.WHITE)));

        lines.add(Component.literal(indent)
                .append(Component.literal("Coords: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(coords).withStyle(ChatFormatting.GREEN)));

        lines.add(Component.literal(indent)
                .append(Component.literal("Start: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(start).withStyle(ChatFormatting.WHITE)));

        lines.add(Component.literal(indent)
                .append(Component.literal("End: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(end).withStyle(ChatFormatting.WHITE)));

        lines.add(Component.literal(indent)
                .append(Component.literal("Size: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(size).withStyle(ChatFormatting.WHITE)));

        if (borderType != null) {
            lines.add(Component.literal(indent)
                    .append(Component.literal("Border: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(borderType).withStyle(ChatFormatting.GOLD)));
        }
        lines.add(newline());
        return lines;
    }

    // private constructor — static utility class.
    private ParcelDisplayFormatter() {}
}