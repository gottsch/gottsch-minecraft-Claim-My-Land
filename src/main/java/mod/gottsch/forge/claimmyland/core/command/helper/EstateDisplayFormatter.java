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
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.EstateRegistry;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.registry.PlayerRegistry;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author by Mark Gottschling on 2/18/2026
 */
public class EstateDisplayFormatter {
    // box drawing characters for tree structure
    private static final String BRANCH = "├─ ";
    private static final String LAST_BRANCH = "└─ ";
    private static final String VERTICAL = "│  ";
    private static final String SPACE = "   ";
    private static final String TITLE_BAR = "═══════════════════════════════";

    // styles
    private static final Style BOLD_GOLD = Style.EMPTY.withBold(true).withColor(ChatFormatting.GOLD);
    private static final Style BOLD_LIGHT_PURPLE = Style.EMPTY.withBold(true).withColor(ChatFormatting.LIGHT_PURPLE);
    private static final Style BOLD_GREEN = Style.EMPTY.withBold(true).withColor(ChatFormatting.GREEN);
    private static final Style BOLD_AQUA = Style.EMPTY.withBold(true).withColor(ChatFormatting.AQUA);
    private static final Style BOLD_BLUE = Style.EMPTY.withBold(true).withColor(ChatFormatting.BLUE);
    private static final Style BOLD_YELLOW = Style.EMPTY.withBold(true).withColor(ChatFormatting.YELLOW);

    public static void formatEstateList(ServerLevel level, List<Component> messages, Set<Estate> estates, boolean isOps) {

        // header
        messages.add(Component.literal(TITLE_BAR).withStyle(BOLD_GOLD));
        messages.add(Component.literal("         ESTATE LIST").withStyle(BOLD_GOLD));
        messages.add(Component.literal(TITLE_BAR).withStyle(BOLD_GOLD));
        messages.add(newline());
        messages.add(Component.literal("Total Estates: ").withStyle(ChatFormatting.GRAY )
                .append(Component.literal(String.valueOf(estates.size()))).withStyle(ChatFormatting.WHITE));


        // TODO this isn't right
        if (estates.isEmpty()) {
            messages.add(Component.literal("No estates found.").withStyle(ChatFormatting.YELLOW));
            messages.add(newline());
            return;
        }

        // Separate by type
        Set<Estate> nationEstates = new HashSet<>();
        Set<Estate> citizenEstates = new HashSet<>();
        Set<Estate> regularEstates = new HashSet<>();
        Set<Estate> zoneEstates = new HashSet<>();

        // TODO 1. add isNation() etc to Estate or to EstateRegistry
        // TODO 2. since estates can only have 1 type of parcel, add a Type property

        List<Parcel> parcels = ParcelRegistry.findByOwner(estates.iterator().next().getOwnerId());
        // TODO create a cache (Map) of Estate -> List<Parcel>, or Map<UUID, Map<Estate, List<Parcel>>> from the above this of parcels

        for (Parcel parcel : parcels) {
//            if (parcel.getType() == ParcelType.NATION) {
//                nationEstates.add(parcel.getEstate());
//            }
            switch(parcel.getType()) {
                case NATION -> nationEstates.add(parcel.getEstate());
                case CITIZEN -> citizenEstates.add(parcel.getEstate());
                case PLAYER -> regularEstates.add(parcel.getEstate());
                case ZONE -> zoneEstates.add(parcel.getEstate());
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
//            messages.add(newline());
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
//            messages.add(newline());
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
//            messages.add(newline());
        }

        // regular estates section
        if (!regularEstates.isEmpty()) {
            messages.add(Component.literal("▼ PLAYER ESTATES").withStyle(BOLD_GREEN));
            Iterator<Estate> iterator = regularEstates.iterator();
            while (iterator.hasNext()) {
                Estate estate = iterator.next();
                boolean isLast = !iterator.hasNext();
                messages.addAll(formatEstateEntry(level, estate, isLast, "", isOps));
                messages.add(newline());
            }
//            messages.add(newline());
        }
    }



    private static List<Component> formatEstateEntry(ServerLevel level, Estate estate, boolean isLast, String prefix, boolean isOps) {
        List<Component> lines = new ArrayList<>();
        String branch = isLast ? LAST_BRANCH : BRANCH;
        ChatFormatting color = getEstateColor(estate);
        Optional<String> optionalOwnerName = PlayerRegistry.getPlayerName(level, estate.getOwnerId());

        // build clickable name component
        MutableComponent clickableName = Component.literal(estate.getName()).withStyle(color, ChatFormatting.BOLD);
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

        // estate details
        String indent = prefix + VERTICAL; //(isLast ? SPACE : VERTICAL);

        if (estate.isRelinquished()) {
            lines.add(Component.literal(indent).withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(LangUtil.INDENT4))
                    .append(Component.literal("-RELINQUISHED-")).withStyle(ChatFormatting.RED));
        }

        // attempt to the get owner name
        String ownerName = PlayerRegistry.getPlayerName(level, estate.getOwnerId()).orElseGet(() -> estate.getOwnerId().toString());
        lines.add(
                Component.literal(indent)
                        .append(Component.literal("Owner: ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(ownerName).withStyle(ChatFormatting.WHITE)));


        // TODO cache this in the parent method and pass into here
        Set<Parcel> parcels = estate.findParcels();

        lines.add(
                Component.literal(indent)
                        .append(Component.literal("Parcels: ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(String.valueOf(parcels.size())).withStyle(ChatFormatting.WHITE)));

        boolean isNationFlag = estate.isNation();
        if (isPlayer(estate)) {
            lines.add(Component.literal(indent)
                    .append(Component.literal("Type: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("Player").withStyle(color)));
        }
        else if (isNationFlag) {
            lines.add(Component.literal(indent)
                    .append(Component.literal("Type: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("Nation").withStyle(color)));
            lines.add(Component.literal(indent)
                    .append(Component.literal("Access Type: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(((NationEstate)estate).getAccessType().toString())).withStyle(ChatFormatting.WHITE));
//            if (estate.getCitizenCount() > 0) {
//                lines.add(indent + ChatFormatting.GRAY + "Citizens: " + ChatFormatting.WHITE + estate.getCitizenCount());
//            }
        } else if (isZone(estate)) {
            lines.add(
                    Component.literal(indent)
                            .append(Component.literal("Type: ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal("Zone").withStyle(color)));
            NationalizedParcel nationalizedParcel = ((NationalizedParcel)parcels.iterator().next());
            if (nationalizedParcel.getNationEstate().getId() != null) {
                lines.add(
                        Component.literal(indent)
                                .append(Component.literal("Nation: ").withStyle(ChatFormatting.GRAY ))
                                .append(Component.literal(nationalizedParcel.getNationEstate().getName()).withStyle(ChatFormatting.WHITE)));
            }
        } else if (isCitizen(estate)) {
            lines.add(
                    Component.literal(indent)
                            .append(Component.literal("Type: ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal("Citizen").withStyle(color)));
            NationalizedParcel nationalizedParcel = ((NationalizedParcel)parcels.iterator().next());
            if (nationalizedParcel.getNationEstate().getId() != null) {
                lines.add(
                        Component.literal(indent)
                                .append(Component.literal("Nation: ").withStyle(ChatFormatting.GRAY ))
                                .append(Component.literal(nationalizedParcel.getNationEstate().getName()).withStyle(ChatFormatting.WHITE)));
            }
        }

        // location
        Optional<ICoords> location = getMinXZ(parcels.stream().map(parcel -> parcel.getMinCoords()).collect(Collectors.toSet()));
        if (location.isPresent()) {
            lines.add(Component.literal(indent)
                    .append(Component.literal("Location: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(formatLocation(location.get())).withStyle(ChatFormatting.GREEN).withStyle(tpStyle(location.get()))));
        }

        // player whitelist
        Set<UUID> whitelist = estate.getPlayerWhitelist();
        lines.add(Component.literal(indent)
                .append(Component.translatable(LangUtil.chat("estate.player.whitelist")).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.format("(" + whitelist.size() + " friends)")).withStyle(ChatFormatting.YELLOW)));

        if (!whitelist.isEmpty()) {
//            lines.add(Component.literal(LangUtil.INDENT4)
//                    .append(Component.literal("No whitelisted players found.").withStyle(ChatFormatting.WHITE)));
            lines.addAll(formatEstateListPlayerWhitelist(level, whitelist, indent + LangUtil.INDENT2));
        }

        if (isNationFlag) {
            // TENANT ESTATES
            Set<Estate> nationalizedEstates = ParcelRegistry.findAllByNationEstateId(estate.getId()).stream()
                    .map(parcel -> ((NationalizedParcel)parcel).getEstate())
                    .collect(Collectors.toSet());

            if (!nationalizedEstates.isEmpty()) {
                lines.add(Component.literal(indent)
                        .append(Component.translatable(LangUtil.chat("estate.tenants")).withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(String.format("(" + nationalizedEstates.size() + " estates)")).withStyle(ChatFormatting.AQUA)));
                lines.addAll(formatTenantEstateList(level, nationalizedEstates, indent + LangUtil.INDENT2));
            }
        }

        return lines;
    }

    private static List<Component> formatTenantEstateList(ServerLevel level, Set<Estate> nationalizedEstates, String indent) {
        List<Component> lines = new ArrayList<>();

        // group players alphabetically for easier reading
        List<String> sortedEstates = new ArrayList<>(nationalizedEstates.stream().map(Estate::getName).toList());
        sortedEstates.sort(String.CASE_INSENSITIVE_ORDER);

        // TODO this could be a config value
        final int maxPerRow = 5;

        for (int i = 0; i < sortedEstates.size(); i += maxPerRow) {
            MutableComponent component = Component.literal(indent);

            // add up to maxPerRow names
            for (int j = 0; j < maxPerRow && (i + j) < sortedEstates.size(); j++) {
                if (j > 0) {
                    component.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
                }
                component.append(Component.literal(String.valueOf(sortedEstates.get(i + j))).withStyle(ChatFormatting.WHITE));
            }
            lines.add(component);
        }
        return lines;
    }

    // ===== DETAILED SINGLE ESTATE VIEW =====

    public static List<Component> formatEstateDetails(ServerLevel level, Estate estate) {
        List<Component> lines = new ArrayList<>();

        ChatFormatting typeColor = getEstateColor(estate);
        String typeName = estate.getParcelType().name();
        Set<Parcel> parcels = estate.findParcels();

        // header
        lines.add(Component.literal(TITLE_BAR).withStyle(BOLD_GOLD));
        lines.add(Component.literal(LangUtil.INDENT5 + " ESTATE DETAILS").withStyle(BOLD_GOLD));
        lines.add(Component.literal(TITLE_BAR).withStyle(BOLD_GOLD));;
        lines.add(newline());

        // basic Info
        lines.add(Component.literal(estate.getName()).withStyle(typeColor, ChatFormatting.BOLD));
        lines.add(Component.literal("Estate ID: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(estate.getId().toString()).withStyle(ChatFormatting.WHITE)));

        if (estate.getOwnerId() != null) {
            // attempt to the get owner name
            String ownerName = PlayerRegistry.getPlayerName(level, estate.getOwnerId()).orElseGet(() -> estate.getOwnerId().toString());
            lines.add(
                    (Component.literal("Owner: ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(ownerName).withStyle(ChatFormatting.WHITE)));
        }

        // type
        if (isPlayer(estate)) {
            lines.add(Component.literal("Type: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("Player").withStyle(typeColor)));
        }
        else if (isNation(estate)) {
            lines.add(Component.literal("Type: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("Nation").withStyle(typeColor)));
            lines.add(Component.literal("Access Type: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(((NationEstate)estate).getAccessType().toString()).withStyle(ChatFormatting.WHITE)));

        } else if (isZone(estate)) {
            lines.add(
                    Component.literal("Type: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal("Zone").withStyle(typeColor)));
            NationalizedParcel nationalizedParcel = ((NationalizedParcel)parcels.iterator().next());
            if (nationalizedParcel.getNationEstate().getId() != null) {
                lines.add(
                        Component.literal("Nation: ").withStyle(ChatFormatting.GRAY )
                                .append(Component.literal(nationalizedParcel.getNationEstate().getName()).withStyle(ChatFormatting.WHITE)));
            }
        } else if (isCitizen(estate)) {
            lines.add(
                    Component.literal("Type: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal("Citizen").withStyle(typeColor)));
            NationalizedParcel nationalizedParcel = ((NationalizedParcel)parcels.iterator().next());
            if (nationalizedParcel.getNationEstate().getId() != null) {
                lines.add(
                        Component.literal("Nation: ").withStyle(ChatFormatting.GRAY )
                                .append(Component.literal(nationalizedParcel.getNationEstate().getName()).withStyle(ChatFormatting.WHITE)));
            }
        }

        // is relinquished
        lines.add(Component.literal("Relinquished: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(estate.isRelinquished())).withStyle(estate.isRelinquished() ? ChatFormatting.RED : ChatFormatting.WHITE)));

        lines.add(newline());

        // player whitelist
        Set<UUID> playerWhitelist = estate.getPlayerWhitelist();
        lines.add(Component.literal("")
                .append(Component.literal("▼ ").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD))
                .append(Component.literal("Friends Whitelist ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" (" + playerWhitelist.size() + " friends)").withStyle(ChatFormatting.YELLOW)));
        if (!playerWhitelist.isEmpty()) {
            lines.addAll(formatEstateDetailsPlayerWhitelist(level, playerWhitelist, LangUtil.INDENT2));
        }
        lines.add(newline());

        // block and block tag whitelist
        Set<String> blockWhitelist = estate.getBlockWhitelist();
        lines.add(Component.literal("")
                .append(Component.literal("▼ ").withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY))
                .append(Component.translatable(LangUtil.chat("estate.block.whitelist")).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" (" + blockWhitelist.size() + " blocks)").withStyle(ChatFormatting.AQUA)));
        if (!blockWhitelist.isEmpty()) {
            lines.addAll(formatGenericList(blockWhitelist, WhitelistType.BLOCK, ""));
        }
        lines.add(newline());

        Set<String> blockTagWhitelist = estate.getBlockTagWhitelist();
        lines.add(Component.literal("")
                .append(Component.literal("▼ ").withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY))
                .append(Component.translatable(LangUtil.chat("estate.block_tag.whitelist")).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" (" + blockTagWhitelist.size() + " block tags)").withStyle(ChatFormatting.AQUA)));

        if (!blockTagWhitelist.isEmpty()) {
            lines.addAll(formatGenericList(blockTagWhitelist, WhitelistType.BLOCK_TAG, ""));
        }
        lines.add(newline());

        // item and item tag whitelists
        Set<String> itemWhitelist = estate.getItemWhitelist();
        lines.add(Component.literal("")
                .append(Component.literal("▼ ").withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY))
                .append(Component.translatable(LangUtil.chat("estate.item.whitelist")).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" (" + itemWhitelist.size() + " items)").withStyle(ChatFormatting.AQUA)));

        if (!itemWhitelist.isEmpty()) {
            lines.addAll(formatGenericList(itemWhitelist, WhitelistType.ITEM, ""));
        }
        lines.add(newline());

        Set<String> itemTagWhitelist = estate.getItemTagWhitelist();
        lines.add(Component.literal("")
                .append(Component.literal("▼ ").withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY))
                .append(Component.translatable(LangUtil.chat("estate.item_tag.whitelist")).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" (" + itemTagWhitelist.size() + " items)").withStyle(ChatFormatting.AQUA)));
        if (!itemTagWhitelist.isEmpty()) {
            lines.addAll(formatGenericList(itemTagWhitelist, WhitelistType.ITEM_TAG, ""));
        }
        lines.add(newline());

        // entity and entity tag whitelists
        Set<String> entityWhitelist = estate.getEntitySpawnWhitelist();
        lines.add(Component.literal("")
                .append(Component.literal("▼ ").withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY))
                .append(Component.translatable(LangUtil.chat("estate.entity_spawn.whitelist")).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" (" + entityWhitelist.size() + " entities)").withStyle(ChatFormatting.AQUA)));
        if (!entityWhitelist.isEmpty()) {
            lines.addAll(formatGenericList(entityWhitelist, WhitelistType.ENTITY, ""));
        }
        lines.add(newline());

        Set<String> entityTagWhitelist = estate.getEntitySpawnTagWhitelist();
        lines.add(Component.literal("")
                .append(Component.literal("▼ ").withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY))
                .append(Component.translatable(LangUtil.chat("estate.entity_spawn_tag.whitelist")).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" (" + entityTagWhitelist.size() + " entity tags)").withStyle(ChatFormatting.AQUA)));
        if (!entityTagWhitelist.isEmpty()) {
            lines.addAll(formatGenericList(entityTagWhitelist, WhitelistType.ENTITY_TAG, ""));
        }

        lines.add(newline());

        // ===========
        // parcels info
        // ===========
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

//                lines.add(Component.literal(branch).append(Component.literal("Parcel #" + parcel.getId()).withStyle(ChatFormatting.WHITE)));
                // id line
                lines.add(Component.literal(branch)
                        .append(Component.literal(parcel.getName()).withStyle(typeColor, ChatFormatting.BOLD))
                        .append(Component.literal(" [ID: ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(parcel.getId().toString()).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal("]").withStyle(ChatFormatting.GRAY)));

                String indent = VERTICAL; //isLast ? SPACE : VERTICAL;

                if (estate.isRelinquished()) {
                    lines.add(Component.literal(indent)
                            .append(Component.literal(LangUtil.INDENT4))
                            .append(Component.literal("-RELINQUISHED-").withStyle(ChatFormatting.RED)));
                }

                lines.add(Component.literal(indent)
                        .append(Component.literal( "Min Pos: ").withStyle(ChatFormatting.GRAY))
                        .append(
                                Component.literal(
                                                formatLocation(parcel.getMinCoords()))
                                        .withStyle(ChatFormatting.GREEN)
                                        .withStyle(tpStyle(parcel.getMinCoords()))));
                lines.add(Component.literal(indent)
                        .append(Component.literal( "Max Pos: ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(
                                        formatLocation(parcel.getMaxCoords()))
                                .withStyle(ChatFormatting.GREEN)
                                .withStyle(tpStyle(parcel.getMaxCoords()))));

                if (parcel.getSize() != null) {
                    ICoords size = ModUtil.getSize(parcel.getSize());
                    lines.add(Component.literal(indent)
                            .append(Component.literal("Size: ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(size.getX() + "x" + size.getY() + "x" + size.getZ()).withStyle(ChatFormatting.WHITE)));
                }
            }
        }
        lines.add(newline());

        // ===========
        // tenant estates info
        // ===========
        if (estate.isNation()) {
            // TENANT ESTATES
            Set<Estate> nationalizedEstates = ParcelRegistry.findAllByNationEstateId(estate.getId()).stream()
                    .map(parcel -> ((NationalizedParcel)parcel).getEstate())
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
                    // attempt to the get owner name
                    String ownerName = PlayerRegistry.getPlayerName(level, estate.getOwnerId()).orElseGet(() -> tenantEstate.getOwnerId().toString());

                    lines.add(Component.literal(branch)
                            .append(Component.literal(tenantEstate.getName()).withStyle(getEstateColor(tenantEstate), ChatFormatting.BOLD))
                            .append(Component.literal(" [ID: ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(tenantEstate.getId().toString()).withStyle(ChatFormatting.WHITE))
                            .append(Component.literal("]").withStyle(ChatFormatting.GRAY)));

                    String indent = VERTICAL; //isLast ? SPACE : VERTICAL;
                    lines.add(Component.literal(indent)
                            .append(Component.literal("Owner: ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(ownerName).withStyle(ChatFormatting.WHITE)));
                    lines.add(Component.literal(indent)
                            // is relinquished
                            .append(Component.literal("Relinquished: ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(String.valueOf(tenantEstate.isRelinquished())).withStyle(tenantEstate.isRelinquished() ? ChatFormatting.RED : ChatFormatting.WHITE)));
                }
            }
            lines.add(newline());
        }


//         nation-specific info
//        if (estate.isNation()) {
//            lines.add("");
//            lines.add(ChatFormatting.AQUA + "" + ChatFormatting.BOLD + "Nation Info:");
//            lines.add(ChatFormatting.GRAY + "Citizens: " + ChatFormatting.WHITE + estate.getCitizenCount());
//        }

//        nationalized-specific info
//        if (isCitizen(estate) || isZone(estate)) {
//            NationalizedParcel nationalizedParcel = ((NationalizedParcel) parcels.iterator().next());
//            if (nationalizedParcel.getNationEstate().getId() != null) {
//                lines.add(newline());
//                lines.add(
//                        Component.literal("Nation: ").withStyle(ChatFormatting.GRAY)
//                                .append(Component.literal(nationalizedParcel.getNationEstate().getName()).withStyle(ChatFormatting.WHITE)));
//            }
//        }
        return lines;
    }

    // format player whitelist
    private static List<Component> formatEstateListPlayerWhitelist(ServerLevel level, Set<UUID> whitelist, String indent) {
        List<Component> lines = new ArrayList<>();

        // get player names
        List<String> playerNames = whitelist.stream()
                .map(uuid -> PlayerRegistry.getPlayerName(level, uuid))
                .flatMap(Optional::stream)
                .toList();

        // group players alphabetically for easier reading
        List<String> sortedPlayers = new ArrayList<>(playerNames);
        sortedPlayers.sort(String.CASE_INSENSITIVE_ORDER);

        // TODO this could be a config value
        final int maxPerRow = 5;

//        List<UUID> list = new ArrayList<>(whitelist);
        for (int i = 0; i < sortedPlayers.size(); i += maxPerRow) {
            MutableComponent component = Component.literal(indent);

            // add up to 3 names
            for (int j = 0; j < maxPerRow && (i + j) < sortedPlayers.size(); j++) {
                if (j > 0) {
                    component.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
                }
//                UUID uuid = list.get(i + j);
//                String playerName = PlayerRegistry.getNameFromUUIDSynchronized(uuid).orElseGet(uuid::toString);
//                component.append(Component.literal(playerName).withStyle(ChatFormatting.WHITE));
                component.append(Component.literal(String.valueOf(sortedPlayers.get(i + j))).withStyle(ChatFormatting.WHITE));

            }
            lines.add(component);
        }
        return lines;
    }

    // part of estate details or stand-alone player whitelist
    private static List<Component> formatEstateDetailsPlayerWhitelist(ServerLevel level, Set<UUID> players, String indent) {
        List<Component> lines = new ArrayList<>();

        // get player names
        List<String> playerNames = players.stream()
                .map(uuid -> PlayerRegistry.getPlayerName(level, uuid))
                .flatMap(Optional::stream)
                .toList();

//        defaults to Unknown Player - could change to display the UUID
//        List<String> playerNames = players.stream()
//                .map(uuid -> PlayerRegistry.getPlayerName(level, uuid))
//                .map(opt -> opt.orElse("Unknown Player"))
//                .toList();

        // group players alphabetically for easier reading
        List<String> sortedPlayers = new ArrayList<>(playerNames);
        sortedPlayers.sort(String.CASE_INSENSITIVE_ORDER);

        // display 3 players per line
        for (int i = 0; i < sortedPlayers.size(); i += 3) {
            MutableComponent component = Component.literal(indent);

            boolean isLastGroup = i + 3 >= sortedPlayers.size();
            String branch = isLastGroup ? LAST_BRANCH : BRANCH;
            component.append(Component.literal(branch));

            for (int j = 0; j < 3 && (i + j) < sortedPlayers.size(); j++) {
                if (j > 0) {
                    component.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
                }
                component.append(Component.literal(String.valueOf(sortedPlayers.get(i + j))).withStyle(ChatFormatting.WHITE));
            }
            lines.add(component);
        }
        return lines;
    }

    // format whitelist
    private static List<Component> formatWhitelist(Set<?> whitelist, String indent) {
        List<Component> lines = new ArrayList<>();
        // TODO this could be a config value
        final int maxPerRow = 5;

        List<?> list = new ArrayList<>(whitelist);
        for (int i = 0; i < list.size(); i += maxPerRow) {
            MutableComponent component = Component.literal(indent);

            // add up to 3 names
            for (int j = 0; j < maxPerRow && (i + j) < list.size(); j++) {
                if (j > 0) {
                    component.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
                }
                component.append(Component.literal(list.get(i + j).toString()).withStyle(ChatFormatting.WHITE));
            }
            lines.add(component);
        }
        return lines;
    }

    private static List<Component> formatGenericList(Set<String> data, WhitelistType type, String indent) {
        List<Component> lines = new ArrayList<>();

        // sort the list
        List<String> blockNames = data.stream()
                .sorted()
                .toList();

        for (int i = 0; i < blockNames.size(); i++) {
            boolean isLast = i == blockNames.size() - 1;
            String branch = isLast ? LAST_BRANCH : BRANCH;
            lines.add(Component.literal(indent + branch)
                    .append(Component.literal(blockNames.get(i)).withStyle(type.getColor())));
        }
        return lines;
    }

    // ===== PARCEL LIST FORMATTING =====

    /**
     *
     */
    public static List<Component> formatParcelList(ServerLevel level, List<Component> lines, List<Parcel> parcels, boolean isOps) {

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
            return lines;
        }

        for (int i = 0; i < parcels.size(); i++) {
            Parcel parcel = parcels.get(i);
            boolean isLast = (i == parcels.size() - 1);
            lines.addAll(formatParcelEntry(level, parcel, isLast, "", isOps));

            if (!isLast) {
                lines.add(newline()); // spacing between parcels
            }
        }

        return lines;
    }

    private static List<Component> formatParcelEntry(ServerLevel level, Parcel parcel, boolean isLast, String prefix, boolean isOps) {
        List<Component> lines = new ArrayList<>();
        String branch = isLast ? LAST_BRANCH : BRANCH;
        ChatFormatting color = getParcelColor(parcel);

        // parcel header
        lines.add(Component.literal(prefix + branch)
                .append(Component.literal(parcel.getName()).withStyle(color, ChatFormatting.BOLD))
                .append(Component.literal(" [ID: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(parcel.getId().toString()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("]").withStyle(ChatFormatting.GRAY))
        );

        // parcel details
        String indent = prefix + (isLast ? SPACE : VERTICAL);

        Optional<String> optionalOwnerName = PlayerRegistry.getPlayerName(level, parcel.getEstate().getOwnerId());

        // build clickable name component
        MutableComponent clickableName = Component.literal(parcel.getEstate().getName()).withStyle(color, ChatFormatting.BOLD);
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

        // attempt to the get owner name
//            String ownerName = PlayerRegistry.getPlayerName(level, parcel.getOwnerId()).orElseGet(() -> parcel.getOwnerId().toString());
        lines.add(Component.literal(indent)
                .append(Component.literal("Owner: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(optionalOwnerName.orElse(parcel.getEstate().getOwnerId().toString())).withStyle(ChatFormatting.WHITE)));


        lines.add(Component.literal(indent)
                .append(Component.literal( "Min Pos: ").withStyle(ChatFormatting.GRAY))
                .append(
                        Component.literal(
                                        formatLocation(parcel.getMinCoords()))
                                .withStyle(ChatFormatting.GREEN)
                                .withStyle(tpStyle(parcel.getMinCoords()))));
        lines.add(Component.literal(indent)
                .append(Component.literal( "Max Pos: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(
                                formatLocation(parcel.getMaxCoords()))
                        .withStyle(ChatFormatting.GREEN)
                        .withStyle(tpStyle(parcel.getMaxCoords()))));

        if (parcel.getSize() != null) {
            ICoords size = ModUtil.getSize(parcel.getSize());
            lines.add(Component.literal(indent)
                    .append(Component.literal("Size: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(size.getX() + "x" + size.getY() + "x" + size.getZ()).withStyle(ChatFormatting.WHITE)));
        }

        if (isPlayer(parcel)) {
            lines.add(Component.literal(indent)
                    .append(Component.literal("Type: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("Player").withStyle(color)));
        }
        else if (isNation(parcel)) {
            lines.add(Component.literal(indent)
                    .append(Component.literal("Type: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("Nation").withStyle(color)));
//            if (estate.getCitizenCount() > 0) {
//                lines.add(indent + ChatFormatting.GRAY + "Citizens: " + ChatFormatting.WHITE + estate.getCitizenCount());
//            }
        } else if (isZone(parcel)) {
            lines.add(
                    Component.literal(indent)
                            .append(Component.literal("Type: ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal("Zone").withStyle(color)));
            NationalizedParcel nationalizedParcel = (NationalizedParcel) parcel;
            if (nationalizedParcel.getNationEstate().getId() != null) {
                lines.add(
                        Component.literal(indent)
                                .append(Component.literal("Nation: ").withStyle(ChatFormatting.GRAY ))
                                .append(Component.literal(nationalizedParcel.getNationEstate().getName()).withStyle(ChatFormatting.WHITE)));
            }
        } else if (isCitizen(parcel)) {
            lines.add(
                    Component.literal(indent)
                            .append(Component.literal("Type: ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal("Citizen").withStyle(color)));
            NationalizedParcel nationalizedParcel = (NationalizedParcel) parcel;
            if (nationalizedParcel.getNationEstate().getId() != null) {
                lines.add(
                        Component.literal(indent)
                                .append(Component.literal("Nation: ").withStyle(ChatFormatting.GRAY ))
                                .append(Component.literal(nationalizedParcel.getNationEstate().getName()).withStyle(ChatFormatting.WHITE)));
            }
        }

        return lines;
    }

    // ===== WHITELIST METHODS =====
    public static List<Component> formatEstateWhitelists(ServerLevel level, Estate estate) {
        List<Component> lines = new ArrayList<>();

        ChatFormatting typeColor = getEstateColor(estate);

        // Player Whitelist Section
        lines.add(Component.literal("▼ FRIENDS WHITELIST").withStyle(BOLD_AQUA ));
        Set<UUID> players = estate.getPlayerWhitelist();
        if (!players.isEmpty()) {
            lines.add(Component.literal(LangUtil.INDENT2 + "Total: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.valueOf(players.size())).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal( "friends").withStyle(ChatFormatting.GRAY)));
            lines.addAll(formatEstateDetailsPlayerWhitelist(level, players, LangUtil.INDENT2));
        }
        lines.add(newline());


        return lines;
    }



    // ===== PLAYER WHITELIST FORMATTING =====

    public static List<Component> formatStandAlonePlayerWhitelist(ServerLevel level, Set<UUID> players, String title, UUID estateId) {
        List<Component> lines = new ArrayList<>();

        // header
        lines.add(Component.literal(TITLE_BAR).withStyle(BOLD_YELLOW));
        lines.add(Component.literal(LangUtil.INDENT4 + title).withStyle(BOLD_YELLOW));
        lines.add(Component.literal(TITLE_BAR).withStyle(BOLD_YELLOW));
        lines.add(newline());

        if (players == null || players.isEmpty()) {
            lines.add(Component.literal("No friends whitelisted").withStyle(ChatFormatting.GRAY));
            return lines;
        }

        lines.add(Component.literal("Total Friends: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(players.size())).withStyle(ChatFormatting.WHITE)));
        lines.add(newline());

        lines.addAll(formatPlayerList(level, players, "", estateId));

        return lines;
    }

    private static List<Component> formatPlayerList(ServerLevel level, Set<UUID> players, String indent, UUID estateId) {
        List<Component> lines = new ArrayList<>();

//      defaults to Unknown Player - could change to display the UUID
        List<String> playerNames = players.stream()
                .map(uuid -> PlayerRegistry.getPlayerName(level, uuid)
                        .orElse("Unknown Player [" + uuid + "]"))
                .toList();

        // group players alphabetically for easier reading
        List<String> sortedPlayers = new ArrayList<>(playerNames);
        sortedPlayers.sort(String.CASE_INSENSITIVE_ORDER);

        // one player per line
        for (int i = 0; i < sortedPlayers.size(); i++) {
            boolean isLast = i == sortedPlayers.size() - 1;
            String branch = isLast ? LAST_BRANCH : BRANCH;

            String playerName = sortedPlayers.get(i);

            // Format: ├─ [icon space] PlayerName
            // The icon space is where you'll add clickable icons later
            lines.add(Component.literal(indent + branch)
                    .append(playerName).withStyle(ChatFormatting.WHITE));
        }

        return lines;
    }

    // ===== GENERIC WHITELIST FORMATTING =====

    public static List<Component> formatStandAloneGenericWhitelist(Set<String> data, WhitelistType type, String title, UUID estateId) {
        List<Component> lines = new ArrayList<>();

        // header
        lines.add(Component.literal(TITLE_BAR).withStyle(ChatFormatting.BOLD, type.getColor()));
        lines.add(Component.literal(LangUtil.INDENT4 + title).withStyle(ChatFormatting.BOLD, type.getColor()));
        lines.add(Component.literal(TITLE_BAR).withStyle(ChatFormatting.BOLD, type.getColor()));
        lines.add(newline());

        if (data == null || data.isEmpty()) {
            lines.add(Component.literal("No entries in whitelist").withStyle(ChatFormatting.GRAY));
            return lines;
        }

        lines.add(Component.literal("Total: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(data.size())).withStyle(ChatFormatting.WHITE)));
        lines.add(newline());

        lines.addAll(formatGenericList(data, type, ""));

        return lines;
    }

    // ===== HELPER METHODS =====

    /**
     * returns the smallest X and smallest Z found across all positions in the set,
     * not necessarily from the same ICoords.
     *
     * @param coordsSet A non-empty set of BlockPos
     * @return a ICoords of [minX, 0, minZ], or null if the set is empty
     */
    public static Optional<ICoords> getMinXZ(Set<ICoords> coordsSet) {
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

    public static ChatFormatting getEstateColor(Estate estate) {
        ParcelType type = estate.getParcelType();
        if (isPlayer(estate)) {
            return ChatFormatting.GREEN;
        }
        else if (isCitizen(estate)) {
            return ChatFormatting.LIGHT_PURPLE;
        } else if (isNation(estate)) {
            return ChatFormatting.BLUE;
        } else {
            return ChatFormatting.YELLOW;
        }
    }

    public static ChatFormatting getParcelColor(Parcel parcel) {
        ParcelType type = parcel.getType();
        if (isPlayer(parcel)) {
            return ChatFormatting.GREEN;
        }
        else if (isCitizen(parcel)) {
            return ChatFormatting.LIGHT_PURPLE;
        } else if (isNation(parcel)) {
            return ChatFormatting.BLUE;
        } else {
            return ChatFormatting.YELLOW;
        }
    }

    private static String formatLocation(ICoords coords) {
        return "[" +coords.getX() + ", " + coords.getY() + ", " + coords.getZ() +"]";
    }

    private static String formatLocation(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    // overload for different location types
    private static String formatLocation(ChunkPos pos) {
        return "Chunk [" + pos.x + ", " + pos.z + "]";
    }

    private static boolean isNation(Estate estate) {
        return estate.getParcelType() == ParcelType.NATION;
    }

    private static boolean isCitizen(Estate estate) {
        return estate.getParcelType() == ParcelType.CITIZEN;
    }

    private static boolean isZone(Estate estate) {
        return estate.getParcelType() == ParcelType.ZONE;
    }

    private static boolean isPlayer(Estate estate) {
        return estate.getParcelType() == ParcelType.PLAYER;
    }

    private static boolean isNation(Parcel parcel) {
        return parcel.getType() == ParcelType.NATION;
    }

    private static boolean isCitizen(Parcel parcel) {
        return parcel.getType() == ParcelType.CITIZEN;
    }

    private static boolean isZone(Parcel parcel) {
        return parcel.getType() == ParcelType.ZONE;
    }

    private static boolean isPlayer(Parcel parcel) {
        return parcel.getType() == ParcelType.PLAYER;
    }

    // TODO add to LangUtil or parent formatter class
    public static Component newline() {
        return Component.literal(LangUtil.NEWLINE);
    }

    private static Style tpStyle(ICoords coords) {
        return Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + coords.getX() + " ~ " + coords.getZ()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip")));
    }

    private static Style playerEstateDetailsStyle(String estateName) {
        return Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cml estate details " + estateName))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(LangUtil.tooltip("estate.details"))));
    }

    private static Style opsEstateDetailsStyle(String ownerName, String estateName) {
        return Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cml estate details " + ownerName + " " + estateName))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(LangUtil.tooltip("estate.details"))));
    }
}
