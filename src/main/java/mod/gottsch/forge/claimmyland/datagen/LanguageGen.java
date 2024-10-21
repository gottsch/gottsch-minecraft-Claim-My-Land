/*
 * This file is part of  Claim My Land.
 * Copyright (c) 2024 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.claimmyland.datagen;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.block.ModBlocks;
import mod.gottsch.forge.claimmyland.core.item.ModItems;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

/**
 * 
 * @author Mark Gottschling on Sep 18, 2024
 *
 */
public class LanguageGen extends LanguageProvider {

    public LanguageGen(PackOutput gen, String locale) {
        super(gen, ClaimMyLand.MOD_ID, locale);
    }
    
    @Override
    protected void addTranslations() {
        // deeds
        add(ModItems.PLAYER_DEED.get(), "Player Deed");
        add(ModItems.NATION_DEED.get(), "Nation Deed");
        add(ModItems.CITIZEN_DEED.get(), "Citizen Deed");

        add(ModItems.PLAYER_DEED_10.get(), "Player Deed 10x20x10");
        add(ModItems.PLAYER_DEED_16.get(), "Player Deed 16x32x16");
        add(ModItems.PLAYER_DEED_32.get(), "Player Deed 32x64x32");
        add(ModItems.NATION_DEED_100.get(), "Nation Deed 100x100");

        // blocks
        add(ModBlocks.PLAYER_FOUNDATION_STONE.get(), "Foundation Stone");
//        add(ModBlocks.ZONE_PLACEMENT_BLOCK.get(), "Zoning Tool");
        add(ModItems.CITIZEN_PLACEMENT_TOOL.get(), "Citizen Parcel Tool");
        add(ModItems.ZONING_PLACEMENT_TOOL.get(), "Zoning Tool");

//        add(ProtectItItems.FOUNDATION_STONE_ITEM.get(), "F")
//
//                "message.protectit.block_region.successfully_protected":"Region %s -> %s is now owned and protected.",
//                "message.protectit.block_region.protected":"A block(s) in that region are already owned and protected.",
//                "message.protectit.block_region.not_protected":"That region is not protected.",
//                "message.protectit.block_region.not_owner":"You are not the owner of that property.",
//                "message.protectit.block_region.not_protected_or_owner":"That region is not owned or you are not the owner.",
//                "message.protectit.invalid_coords_format": "Block pos B must be >= than block pos A.",
//
//                "message.protectit.option_unavailable": "That option is not available yet.",
//                "message.protectit.non_givable_item": "That is not a valid item to give.",
//
//                "message.protectit.claim_successfully_removed": "The claim has been removed.",
//                "message.protectit.unable_locate_player": "Unable to locate the player.",
//                "message.protectit.property.list": "%s's Protected Properties",
//                "message.protectit.property.list.empty": "[Empty]",
//                "message.protectit.property.rename.success": "The property was successfully renamed to ",
//                "message.protectit.property.name.unknown": "Player does not own a property named ",
//                "message.protectit.whitelist.property.list": "Whitelist for property ",
//                "message.protectit.whitelist.add.success": "The player was successfully added to ",
//                "message.protectit.whitelist.remove.success": "The player was successfully removed from ",
//                "message.protectit.whitelist.clear.success": "The whitelist was successfully cleared from ",
//

        /*
         * Util.chats
         */
        // exceptions / errors
        add(LangUtil.chat("unable_locate_player"), "Unable to locate the player -> %s");
        add(LangUtil.chat("unexpected_error"), "An unexpected error occurred.");

        // parcels
        add(LangUtil.chat("parcel.block_protected"),"Block is protected.");
        add(LangUtil.chat("parcel.outside_world_boundaries"), "The parcel extends beyond the world boundaries.");
        add(LangUtil.chat("parcel.max_reached"), "You have already reached your max. number of parcels.");
        add(LangUtil.chat("parcel.unable_to_locate"), "Unable to find the parcel.");


        add(LangUtil.chat("parcel.list"), "%s's Parcels");
        add(LangUtil.chat("parcel.list.abandoned"), "Abandoned Parcels");
        add(LangUtil.chat("parcel.list.empty"), "[Empty]");

        add(LangUtil.chat("parcel.abandon.success"), "The parcel has been abandoned.");
        add(LangUtil.chat("parcel.abandon.failure"), "Unable to abandon the parcel.");

        add(LangUtil.chat("parcel.add.success"), "The parcel has been added.");
        add(LangUtil.chat("parcel.add.failure"), "Unable to add the parcel.");
        add(LangUtil.chat("parcel.add.failure_with_overlaps"), "Unable to add the parcel. It intersects with another parcel.");
        add(LangUtil.chat("parcel.add.failure_too_small"), "Unable to add the parcel. The dimension(s) are too small (< 2).");

        add(LangUtil.chat("parcel.remove.success"), "The parcel has been removed.");
        add(LangUtil.chat("parcel.remove.failure"), "Unable to remove the parcel.");

        add(LangUtil.chat("parcel.rename.success"), "The parcel has been renamed.");
        add(LangUtil.chat("parcel.rename.failure"), "Unable to rename the parcel.");

        add(LangUtil.chat("parcel.transfer.success"), "The parcel has been transferred.");
        add(LangUtil.chat("parcel.transfer.failure"), "Unable to transfer the parcel.");

        add(LangUtil.chat("parcel.whitelist.add.success"), "A player was added to the whitelist.");
        add(LangUtil.chat("parcel.whitelist.add.failure"), "Unable to player to the whitelist.");
        add(LangUtil.chat("parcel.whitelist.list"), "Whitelist for property ");

        add(LangUtil.chat("parcel.citizen.nationId_required"), "A nation name is required to add a citizen parcel.");
        add(LangUtil.chat("parcel.nation.nationName_already_exists"), "A nation with that name already exists.");
        add(LangUtil.chat("parcel.nation.unable_to_locate"), "A nation with that name does not exist.");
        add(LangUtil.chat("parcel.nation.not_owner"), "You are not the owner of the nation.");

        // deeds
        add(LangUtil.chat("deed.claim.success"), "You claimed a parcel at [%s] of size [%s].");
        add(LangUtil.chat("deed.claim.intersects"), "You cannot claimed this parcel as it intersects with another.");
        add(LangUtil.chat("deed.claim.insufficient_size"), "The deed's size [%s] is insufficient to claim parcel of size [%s].");
        add(LangUtil.chat("deed.claim.unable_to_claim"), "You unable to claim parcel at [%s] of size [%s].");

        add(LangUtil.chat("deed.generate.failure"), "Unable to generate deed.");
        add(LangUtil.chat("deed.not_owner"), "You are not the owner of this deed.");

        // foundation stones
        add(LangUtil.chat("foundation_stone.unable_to_location"), "Unable to locate foundation stone block entity.");
        add(LangUtil.chat("foundation_stone.incorrect_deed"), "Only the deed used to place the foundation stone can be used here.");

        // citizen placement
        add(LangUtil.chat("citizen_placement.not_valid_parent"), "That block is not within a valid parent parcel (nation | zone) or it is claimed.");

        // zone placement
        add(LangUtil.chat("zone_placement.not_nation"), "That block is not within a nation parcel or it is claimed.");
        add(LangUtil.chat("zone_placement.not_owner"), "You are not the owner of this nation.");


        // info
        add(LangUtil.chat("parcel.claimed_by"), "The block at %s is claimed by %s:");
        add(LangUtil.chat("parcel.name"), "Name: %s");
        add(LangUtil.chat("parcel.type"), "Type: %s");
        add(LangUtil.chat("parcel.coords"), "Pos: %s");
        add(LangUtil.chat("parcel.start"), "Start: %s");
        add(LangUtil.chat("parcel.end"), "End: %s");
        add(LangUtil.chat("parcel.size"), "Size: %s");
        add(LangUtil.chat("parcel.area"), "Area: %s m^2");
        add(LangUtil.chat("parcel.volume"), "Volume: %s m^3");
        add(LangUtil.chat("parcel.border"), "Border: %s");

        /*
         *  Util.tooltips
         */
        // general
        add(LangUtil.tooltip("hold_shift"), "Hold [SHIFT] to expand");

        // deed
        add(LangUtil.tooltip("deed.howto"), "Use the deed to place a foundation stone at desired location. Then, use the deed again on the foundation stone to accept location.");
        add(LangUtil.tooltip("deed.type"), "Type: %s");
        add(LangUtil.tooltip("deed.size"), "Size: (%s)");
        add(LangUtil.tooltip("deed.id"), "ID: %s");
        add(LangUtil.tooltip("deed.nation_id"), "Nation ID: %s");

        add(LangUtil.tooltip("player_deed.usage"), "Can be used on any unclaimed land and within nations with OPEN borders.");
        add(LangUtil.tooltip("citizen_deed.usage"), "Can be used on any unclaimed land within parent nation and within nations with OPEN borders.");
        add(LangUtil.tooltip("nation_deed.usage"), "Can be used on any unclaimed land.");

        // parcel
        // TODO this is for an admin stone or info stone - need a good name

        add(LangUtil.tooltip("parcel.howto.remove"), "Place cornerstone block inside parcel boundaries.\\nUse cornerstone block to open GUI.\nClick Remove button.");


    }
}
