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
import mod.gottsch.forge.claimmyland.core.estate.NationEstate;
import mod.gottsch.forge.claimmyland.core.parcel.NationalizedParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.registry.PlayerRegistry;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;

import java.util.*;
import java.util.stream.Collectors;

import static mod.gottsch.forge.claimmyland.core.command.helper.FormatterConstants.*;

/**
 * formats estate lists, estate entries, and estate details for display in chat.
 *
 * <p>parcel-specific formatting is delegated to {@link ParcelDisplayFormatter}.
 * whitelist formatting is delegated to {@link WhitelistFormatter}.
 * shared constants and utilities live in {@link FormatterConstants}.</p>
 *
 * @author Mark Gottschling on 2/18/2026
 */
public class EstateDisplayFormatter {

    // ===== ESTATE LIST =====

    public static void formatEstateList(ServerLevel level,
                                        List<Component> messages,
                                        Set<Estate> estates,
                                        boolean isOps) {
        // header
        messages.add(Component.literal(TITLE_BAR).withStyle(BOLD_GOLD));
        messages.add(Component.literal("         ESTATE LIST").withStyle(BOLD_GOLD));
        messages.add(Component.literal(TITLE_BAR).withStyle(BOLD_GOLD));
        messages.add(newline());
        messages.add(Component.literal("Total Estates: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(estates.size()))).withStyle(ChatFormatting.WHITE));

        if (estates.isEmpty()) {
            messages.add(Component.literal("No estates found.").withStyle(ChatFormatting.YELLOW));
            messages.add(newline());
            return;
        }

        // separate by parcel type
        Set<Estate> nationEstates  = new HashSet<>();
        Set<Estate> citizenEstates = new HashSet<>();
        Set<Estate> regularEstates = new HashSet<>();
        Set<Estate> zoneEstates    = new HashSet<>();

        List<Parcel> parcels = ParcelRegistry.findByOwner(estates.iterator().next().getOwnerId());

        for (Parcel parcel : parcels) {
            switch (parcel.getType()) {
                case NATION  -> nationEstates.add(parcel.getEstate());
                case CITIZEN -> citizenEstates.add(parcel.getEstate());
                case PLAYER  -> regularEstates.add(parcel.getEstate());
                case ZONE    -> zoneEstates.add(parcel.getEstate());
            }
        }

        // nation estates section
        if (!nationEstates.isEmpty()) {
            messages.add(Component.literal("▼ NATION ESTATES").withStyle(BOLD_BLUE));
            Iterator<Estate> iterator = nationEstates.iterator();
            while (iterator.hasNext()) {
                Estate estate = iterator.next();
                boolean isLast = !iterator.hasNext();
                messages.addAll(formatEstateEntry(level, estate, isLast, "", isOps));
                messages.add(isLast ? newline() : Component.literal(VERTICAL));
            }
        }

        // zone estates section
        if (!zoneEstates.isEmpty()) {
            messages.add(Component.literal("▼ ZONE ESTATES").withStyle(BOLD_YELLOW));
            Iterator<Estate> iterator = zoneEstates.iterator();
            while (iterator.hasNext()) {
                Estate estate = iterator.next();
                boolean isLast = !iterator.hasNext();
                messages.addAll(formatEstateEntry(level, estate, isLast, "", isOps));
                messages.add(newline());
            }
        }

        // citizen estates section
        if (!citizenEstates.isEmpty()) {
            messages.add(Component.literal("▼ CITIZEN ESTATES").withStyle(BOLD_LIGHT_PURPLE));
            Iterator<Estate> iterator = citizenEstates.iterator();
            while (iterator.hasNext()) {
                Estate estate = iterator.next();
                boolean isLast = !iterator.hasNext();
                messages.addAll(formatEstateEntry(level, estate, isLast, "", isOps));
                messages.add(newline());
            }
        }

        // regular/player estates section
        if (!regularEstates.isEmpty()) {
            messages.add(Component.literal("▼ PLAYER ESTATES").withStyle(BOLD_GREEN));
            Iterator<Estate> iterator = regularEstates.iterator();
            while (iterator.hasNext()) {
                Estate estate = iterator.next();
                boolean isLast = !iterator.hasNext();
                messages.addAll(formatEstateEntry(level, estate, isLast, "", isOps));
                messages.add(newline());
            }
        }
    }

    // ===== DETAILED SINGLE ESTATE VIEW =====

    public static List<Component> formatEstateDetails(ServerLevel level, Estate estate) {
        List<Component> lines = new ArrayList<>();

        ChatFormatting typeColor = getEstateColor(estate);
        Set<Parcel> parcels = estate.findParcels();

        // header
        lines.add(Component.literal(TITLE_BAR).withStyle(BOLD_GOLD));
        lines.add(Component.literal(LangUtil.INDENT5 + " ESTATE DETAILS").withStyle(BOLD_GOLD));
        lines.add(Component.literal(TITLE_BAR).withStyle(BOLD_GOLD));
        lines.add(newline());

        // basic info
        lines.add(Component.literal(estate.getName()).withStyle(typeColor, ChatFormatting.BOLD));
        lines.add(Component.literal("Estate ID: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(estate.getId().toString()).withStyle(ChatFormatting.WHITE)));

        if (estate.getOwnerId() != null) {
            String ownerName = PlayerRegistry.getPlayerName(level, estate.getOwnerId())
                    .orElseGet(() -> estate.getOwnerId().toString());
            lines.add(Component.literal("Owner: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(ownerName).withStyle(ChatFormatting.WHITE)));
        }

        // type + optional nation reference
        if (isPlayer(estate)) {
            lines.add(Component.literal("Type: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("Player").withStyle(typeColor)));
        } else if (isNation(estate)) {
            lines.add(Component.literal("Type: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("Nation").withStyle(typeColor)));
            lines.add(Component.literal("Access Type: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(((NationEstate) estate).getAccessType().toString())
                            .withStyle(ChatFormatting.WHITE)));
        } else if (isZone(estate)) {
            lines.add(Component.literal("Type: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("Zone").withStyle(typeColor)));
            NationalizedParcel nationalizedParcel = ((NationalizedParcel) parcels.iterator().next());
            if (nationalizedParcel.getNationEstate().getId() != null) {
                lines.add(Component.literal("Nation: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(nationalizedParcel.getNationEstate().getName())
                                .withStyle(ChatFormatting.WHITE)));
            }
        } else if (isCitizen(estate)) {
            lines.add(Component.literal("Type: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("Citizen").withStyle(typeColor)));
            NationalizedParcel nationalizedParcel = ((NationalizedParcel) parcels.iterator().next());
            if (nationalizedParcel.getNationEstate().getId() != null) {
                lines.add(Component.literal("Nation: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(nationalizedParcel.getNationEstate().getName())
                                .withStyle(ChatFormatting.WHITE)));
            }
        }

        // relinquished flag
        lines.add(Component.literal("Relinquished: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(estate.isRelinquished()))
                        .withStyle(estate.isRelinquished() ? ChatFormatting.RED : ChatFormatting.WHITE)));
        lines.add(newline());

        // player whitelist
        Set<UUID> playerWhitelist = estate.getPlayerWhitelist();
        lines.add(Component.literal("")
                .append(Component.literal("▼ ").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD))
                .append(Component.literal("Friends Whitelist ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" (" + playerWhitelist.size() + " friends)").withStyle(ChatFormatting.YELLOW)));
        if (!playerWhitelist.isEmpty()) {
            lines.addAll(WhitelistFormatter.formatEstateDetailsPlayerWhitelist(level, playerWhitelist, LangUtil.INDENT2));
        }
        lines.add(newline());

        // block whitelist
        Set<String> blockWhitelist = estate.getBlockWhitelist();
        lines.add(Component.literal("")
                .append(Component.literal("▼ ").withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY))
                .append(Component.translatable(LangUtil.chat("estate.block.whitelist")).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" (" + blockWhitelist.size() + " blocks)").withStyle(ChatFormatting.AQUA)));
        if (!blockWhitelist.isEmpty()) {
            lines.addAll(WhitelistFormatter.formatGenericList(blockWhitelist, WhitelistType.BLOCK, ""));
        }
        lines.add(newline());

        // block tag whitelist
        Set<String> blockTagWhitelist = estate.getBlockTagWhitelist();
        lines.add(Component.literal("")
                .append(Component.literal("▼ ").withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY))
                .append(Component.translatable(LangUtil.chat("estate.block_tag.whitelist")).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" (" + blockTagWhitelist.size() + " block tags)").withStyle(ChatFormatting.AQUA)));
        if (!blockTagWhitelist.isEmpty()) {
            lines.addAll(WhitelistFormatter.formatGenericList(blockTagWhitelist, WhitelistType.BLOCK_TAG, ""));
        }
        lines.add(newline());

        // item whitelist
        Set<String> itemWhitelist = estate.getItemWhitelist();
        lines.add(Component.literal("")
                .append(Component.literal("▼ ").withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY))
                .append(Component.translatable(LangUtil.chat("estate.item.whitelist")).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" (" + itemWhitelist.size() + " items)").withStyle(ChatFormatting.AQUA)));
        if (!itemWhitelist.isEmpty()) {
            lines.addAll(WhitelistFormatter.formatGenericList(itemWhitelist, WhitelistType.ITEM, ""));
        }
        lines.add(newline());

        // item tag whitelist
        Set<String> itemTagWhitelist = estate.getItemTagWhitelist();
        lines.add(Component.literal("")
                .append(Component.literal("▼ ").withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY))
                .append(Component.translatable(LangUtil.chat("estate.item_tag.whitelist")).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" (" + itemTagWhitelist.size() + " items)").withStyle(ChatFormatting.AQUA)));
        if (!itemTagWhitelist.isEmpty()) {
            lines.addAll(WhitelistFormatter.formatGenericList(itemTagWhitelist, WhitelistType.ITEM_TAG, ""));
        }
        lines.add(newline());

        // entity spawn whitelist
        Set<String> entityWhitelist = estate.getEntitySpawnWhitelist();
        lines.add(Component.literal("")
                .append(Component.literal("▼ ").withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY))
                .append(Component.translatable(LangUtil.chat("estate.entity_spawn.whitelist")).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" (" + entityWhitelist.size() + " entities)").withStyle(ChatFormatting.AQUA)));
        if (!entityWhitelist.isEmpty()) {
            lines.addAll(WhitelistFormatter.formatGenericList(entityWhitelist, WhitelistType.ENTITY, ""));
        }
        lines.add(newline());

        // entity spawn tag whitelist
        Set<String> entityTagWhitelist = estate.getEntitySpawnTagWhitelist();
        lines.add(Component.literal("")
                .append(Component.literal("▼ ").withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY))
                .append(Component.translatable(LangUtil.chat("estate.entity_spawn_tag.whitelist")).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" (" + entityTagWhitelist.size() + " entity tags)").withStyle(ChatFormatting.AQUA)));
        if (!entityTagWhitelist.isEmpty()) {
            lines.addAll(WhitelistFormatter.formatGenericList(entityTagWhitelist, WhitelistType.ENTITY_TAG, ""));
        }
        lines.add(newline());

        // parcels section
        lines.add(Component.literal("▼ Parcels (" + parcels.size() + "):").withStyle(BOLD_AQUA));
        if (parcels.isEmpty()) {
            lines.add(Component.literal(LangUtil.INDENT2)
                    .append(Component.literal("No parcels claimed").withStyle(ChatFormatting.GRAY)));
        } else {
            Iterator<Parcel> iterator = parcels.iterator();
            while (iterator.hasNext()) {
                Parcel parcel = iterator.next();
                boolean isLast = iterator.hasNext();
                String branch = isLast ? LAST_BRANCH : BRANCH;

                lines.add(Component.literal(branch)
                        .append(Component.literal(parcel.getName()).withStyle(typeColor, ChatFormatting.BOLD))
                        .append(Component.literal(" [ID: ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(parcel.getId().toString()).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal("]").withStyle(ChatFormatting.GRAY)));

                String indent = VERTICAL;

                if (estate.isRelinquished()) {
                    lines.add(Component.literal(indent)
                            .append(Component.literal(LangUtil.INDENT4))
                            .append(Component.literal("-RELINQUISHED-").withStyle(ChatFormatting.RED)));
                }

                // location
                lines.add(Component.literal(indent)
                        .append(Component.literal("Dimension: ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(parcel.getDimension()).withStyle(ChatFormatting.WHITE)));
                lines.add(Component.literal(indent)
                        .append(Component.literal("Min Pos: ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(formatLocation(parcel.getMinCoords()))
                                .withStyle(ChatFormatting.GREEN)
                                .withStyle(tpStyle(parcel.getMinCoords()))));
                lines.add(Component.literal(indent)
                        .append(Component.literal("Max Pos: ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(formatLocation(parcel.getMaxCoords()))
                                .withStyle(ChatFormatting.GREEN)
                                .withStyle(tpStyle(parcel.getMaxCoords()))));

                if (parcel.getSize() != null) {
                    ICoords size = ModUtil.getSize(parcel.getSize());
                    lines.add(Component.literal(indent)
                            .append(Component.literal("Size: ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(size.getX() + "x" + size.getY() + "x" + size.getZ())
                                    .withStyle(ChatFormatting.WHITE)));
                }
            }
        }
        lines.add(newline());

        // tenant estates section (nation only)
        if (estate.isNation()) {
            Set<Estate> nationalizedEstates = ParcelRegistry.findAllByNationEstateId(estate.getId()).stream()
                    .map(parcel -> ((NationalizedParcel) parcel).getEstate())
                    .collect(Collectors.toSet());

            lines.add(Component.literal("▼ Tenant Estates (" + nationalizedEstates.size() + "):").withStyle(BOLD_AQUA));

            if (nationalizedEstates.isEmpty()) {
                lines.add(Component.literal(LangUtil.INDENT2)
                        .append(Component.literal("No tenant estates").withStyle(ChatFormatting.GRAY)));
            } else {
                Iterator<Estate> iterator = nationalizedEstates.iterator();
                while (iterator.hasNext()) {
                    Estate tenantEstate = iterator.next();
                    boolean isLast = iterator.hasNext();
                    String branch = isLast ? LAST_BRANCH : BRANCH;
                    String ownerName = PlayerRegistry.getPlayerName(level, estate.getOwnerId())
                            .orElseGet(() -> tenantEstate.getOwnerId().toString());

                    lines.add(Component.literal(branch)
                            .append(Component.literal(tenantEstate.getName())
                                    .withStyle(getEstateColor(tenantEstate), ChatFormatting.BOLD))
                            .append(Component.literal(" [ID: ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(tenantEstate.getId().toString()).withStyle(ChatFormatting.WHITE))
                            .append(Component.literal("]").withStyle(ChatFormatting.GRAY)));

                    String indent = VERTICAL;
                    lines.add(Component.literal(indent)
                            .append(Component.literal("Owner: ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(ownerName).withStyle(ChatFormatting.WHITE)));
                    lines.add(Component.literal(indent)
                            .append(Component.literal("Relinquished: ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(String.valueOf(tenantEstate.isRelinquished()))
                                    .withStyle(tenantEstate.isRelinquished() ? ChatFormatting.RED : ChatFormatting.WHITE)));
                }
            }
            lines.add(newline());
        }

        lines.add(newline());
                
        return lines;
    }

    // ===== PRIVATE: ESTATE ENTRY =====

    private static List<Component> formatEstateEntry(ServerLevel level,
                                                     Estate estate,
                                                     boolean isLast,
                                                     String prefix,
                                                     boolean isOps) {
        List<Component> lines = new ArrayList<>();
        String branch = isLast ? LAST_BRANCH : BRANCH;
        ChatFormatting color = getEstateColor(estate);
        Optional<String> optionalOwnerName = PlayerRegistry.getPlayerName(level, estate.getOwnerId());

        // clickable estate name
        MutableComponent clickableName = Component.literal(estate.getName())
                .withStyle(color, ChatFormatting.BOLD);
        if (isOps && optionalOwnerName.isPresent()) {
            clickableName.withStyle(opsEstateDetailsStyle(optionalOwnerName.get(), estate.getName()));
        } else if (!isOps) {
            clickableName.withStyle(playerEstateDetailsStyle(estate.getName()));
        }

        // estate header line
        lines.add(Component.literal(prefix + branch)
                .append(clickableName)
                .append(Component.literal(" [ID: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(estate.getId().toString()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("]").withStyle(ChatFormatting.GRAY)));

        String indent = prefix + VERTICAL;

        if (estate.isRelinquished()) {
            lines.add(Component.literal(indent).withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(LangUtil.INDENT4))
                    .append(Component.literal("-RELINQUISHED-")).withStyle(ChatFormatting.RED));
        }

        String ownerName = PlayerRegistry.getPlayerName(level, estate.getOwnerId())
                .orElseGet(() -> estate.getOwnerId().toString());
        lines.add(Component.literal(indent)
                .append(Component.literal("Owner: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(ownerName).withStyle(ChatFormatting.WHITE)));

        Set<Parcel> parcels = estate.findParcels();
        lines.add(Component.literal(indent)
                .append(Component.literal("Parcels: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(parcels.size())).withStyle(ChatFormatting.WHITE)));

        boolean isNationFlag = estate.isNation();
        if (isPlayer(estate)) {
            lines.add(Component.literal(indent)
                    .append(Component.literal("Type: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("Player").withStyle(color)));
        } else if (isNationFlag) {
            lines.add(Component.literal(indent)
                    .append(Component.literal("Type: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("Nation").withStyle(color)));
            lines.add(Component.literal(indent)
                    .append(Component.literal("Access Type: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(((NationEstate) estate).getAccessType().toString()))
                    .withStyle(ChatFormatting.WHITE));
        } else if (isZone(estate)) {
            lines.add(Component.literal(indent)
                    .append(Component.literal("Type: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("Zone").withStyle(color)));
            NationalizedParcel nationalizedParcel = ((NationalizedParcel) parcels.iterator().next());
            if (nationalizedParcel.getNationEstate().getId() != null) {
                lines.add(Component.literal(indent)
                        .append(Component.literal("Nation: ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(nationalizedParcel.getNationEstate().getName())
                                .withStyle(ChatFormatting.WHITE)));
            }
        } else if (isCitizen(estate)) {
            lines.add(Component.literal(indent)
                    .append(Component.literal("Type: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("Citizen").withStyle(color)));
            NationalizedParcel nationalizedParcel = ((NationalizedParcel) parcels.iterator().next());
            if (nationalizedParcel.getNationEstate().getId() != null) {
                lines.add(Component.literal(indent)
                        .append(Component.literal("Nation: ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(nationalizedParcel.getNationEstate().getName())
                                .withStyle(ChatFormatting.WHITE)));
            }
        }

        // location (min XZ across all parcels)
        Optional<ICoords> location = getMinXZ(
                parcels.stream().map(Parcel::getMinCoords).collect(Collectors.toSet()));
        if (location.isPresent()) {
            lines.add(Component.literal(indent)
                    .append(Component.literal("Location: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(formatLocation(location.get()))
                            .withStyle(ChatFormatting.GREEN)
                            .withStyle(tpStyle(location.get()))));
        }

        // player whitelist count + names
        Set<UUID> whitelist = estate.getPlayerWhitelist();
        lines.add(Component.literal(indent)
                .append(Component.translatable(LangUtil.chat("estate.player.whitelist")).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.format("(" + whitelist.size() + " friends)")).withStyle(ChatFormatting.YELLOW)));
        if (!whitelist.isEmpty()) {
            lines.addAll(WhitelistFormatter.formatEstateListPlayerWhitelist(level, whitelist, indent + LangUtil.INDENT2));
        }

        // tenant estates (nations only)
        if (isNationFlag) {
            Set<Estate> nationalizedEstates = ParcelRegistry.findAllByNationEstateId(estate.getId()).stream()
                    .map(parcel -> ((NationalizedParcel) parcel).getEstate())
                    .collect(Collectors.toSet());

            if (!nationalizedEstates.isEmpty()) {
                lines.add(Component.literal(indent)
                        .append(Component.translatable(LangUtil.chat("estate.tenants")).withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(String.format("(" + nationalizedEstates.size() + " estates)"))
                                .withStyle(ChatFormatting.AQUA)));
                lines.addAll(formatTenantEstateList(level, nationalizedEstates, indent + LangUtil.INDENT2));
            }
        }
        lines.add(newline());
        return lines;
    }

    // ===== PRIVATE: TENANT ESTATE LIST =====

    private static List<Component> formatTenantEstateList(ServerLevel level,
                                                          Set<Estate> nationalizedEstates,
                                                          String indent) {
        List<Component> lines = new ArrayList<>();

        List<String> sortedEstates = new ArrayList<>(nationalizedEstates.stream().map(Estate::getName).toList());
        sortedEstates.sort(String.CASE_INSENSITIVE_ORDER);

        final int maxPerRow = 5;

        for (int i = 0; i < sortedEstates.size(); i += maxPerRow) {
            MutableComponent component = Component.literal(indent);
            for (int j = 0; j < maxPerRow && (i + j) < sortedEstates.size(); j++) {
                if (j > 0) component.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
                component.append(Component.literal(sortedEstates.get(i + j)).withStyle(ChatFormatting.WHITE));
            }
            lines.add(component);
        }
        lines.add(newline());
        return lines;
    }

    // ===== STATIC ACCESSORS (kept for callers that used getEstateColor / getParcelColor directly) =====

    /**
     * @deprecated Prefer {@link FormatterConstants#getEstateColor(Estate)} directly.
     */
    @Deprecated(since = "2.1")
    public static ChatFormatting getEstateColor(Estate estate) {
        return FormatterConstants.getEstateColor(estate);
    }

    /**
     * @deprecated Prefer {@link FormatterConstants#getParcelColor(Parcel)} directly.
     */
    @Deprecated(since = "2.1")
    public static ChatFormatting getParcelColor(Parcel parcel) {
        return FormatterConstants.getParcelColor(parcel);
    }

    /**
     * @deprecated Prefer {@link FormatterConstants#getMinXZ(Set)} directly.
     */
    @Deprecated(since = "2.1")
    public static Optional<ICoords> getMinXZ(Set<ICoords> coordsSet) {
        return FormatterConstants.getMinXZ(coordsSet);
    }

    /**
     * @deprecated Prefer {@link FormatterConstants#newline()} directly.
     */
    @Deprecated(since = "2.1")
    public static Component newline() {
        return FormatterConstants.newline();
    }

    // private constructor — static utility class.
    private EstateDisplayFormatter() {}
}