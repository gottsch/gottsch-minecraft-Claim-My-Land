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
        add(ModBlocks.PLAYER_FOUNDATION_STONE.get(), "Player Foundation Stone");
        add(ModBlocks.CITIZEN_FOUNDATION_STONE.get(), "Citizen Foundation Stone");
        add(ModBlocks.NATION_FOUNDATION_STONE.get(), "Nation Foundation Stone");

        add(ModItems.CITIZEN_PLACEMENT_TOOL.get(), "Citizen Parcel Tool");
        add(ModItems.ZONING_PLACEMENT_TOOL.get(), "Zoning Tool");

        add(ModBlocks.BORDER_STONE.get(), "Border Stone");
        add(ModBlocks.PLAYER_BORDER.get(), "Player Parcel Border");
        add(ModBlocks.CITIZEN_BORDER.get(), "Citizen Parcel Border");
        add(ModBlocks.NATION_BORDER.get(), "Nation Parcel Border");
        add(ModBlocks.ZONE_BORDER.get(), "Zone Parcel Border");
        add(ModBlocks.BUFFER.get(), "Parcel Buffer");

        add(ModBlocks.PLAYER_HORIZONTAL_AREA.get(), "Player Parcel Area");
        add(ModBlocks.CITIZEN_HORIZONTAL_AREA.get(), "Citizen Parcel Area");
        add(ModBlocks.NATION_HORIZONTAL_AREA.get(), "Nation Parcel Area");
        add(ModBlocks.ZONE_HORIZONTAL_AREA.get(), "Zone Parcel Area");

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

        // estates
        add(LangUtil.chat("estate.list"), "%s's Estates");
        add(LangUtil.chat("estate.list.relinquished"), "Relinquished Estates");
        add(LangUtil.chat("estate.list.empty"), "[Empty]");

        add(LangUtil.chat("estate.join.success"), "Estates have been joined.");
        add(LangUtil.chat("estate.join.failure"), "Unable to join estates.");
        add(LangUtil.chat("estate.join.not_like.failure"), "Cannot join estates that are of the different types, nor relinquished estates.");
        add(LangUtil.chat("estate.join.same_estate.failure"), "Cannot join estate to itself.");
        add(LangUtil.chat("estate.join.invalid.failure"), "Cannot join estates:");
        add(LangUtil.chat("estate.join.invalid.reasons"), "Same estate~Not the same type.~One or more estates are relinquished.~Not the same owner.");
        add(LangUtil.chat("estate.join.different.zone.failure"), "Cannot join estates that have different parent zone/nations.");

        add(LangUtil.chat("estate.split.success"), "Estate has been split.");
        add(LangUtil.chat("estate.split.failure"), "Unable to split estate.");
        add(LangUtil.chat("estate.split.single_parcel.failure"), "An estate with only one parcel cannot be split.");

        add(LangUtil.chat("estate.demolish.success"), "The estate has been demolished.");
        add(LangUtil.chat("estate.demolish.failure"), "Unable to demolish the estate.");
        add(LangUtil.chat("estate.demolish.zone_cannot_demolish"), "A Zone parcel (in the estate) cannot be demolished. Use 'remove' instead.");

        add(LangUtil.chat("estate.remove.success"), "The estate has been removed.");
        add(LangUtil.chat("estate.remove.failure"), "Unable to remove the estate.");
        add(LangUtil.chat("estate.remove.not_zone.failure"), "Only zone estates can be removed by players");
        
        add(LangUtil.chat("estate.rename.success"), "The estate has been renamed.");
        add(LangUtil.chat("estate.rename.failure"), "Unable to rename the estate.");
        add(LangUtil.chat("estate.rename.exists.failure"), "Unable to rename. Duplicate parcel name within the estate.");
        add(LangUtil.chat("estate.rename.nation_parcel_exists.failure"), "Unable to rename. Duplicate parcel name within the nation.");

        add(LangUtil.chat("estate.relinquish.success"), "The estate has been relinquished.");
        add(LangUtil.chat("estate.relinquish.failure"), "Unable to relinquish the estate.");
        add(LangUtil.chat("estate.relinquish.disallowed.failure"), "This estate can not be relinquished:");
        add(LangUtil.chat("estate.relinquish.disallowed.reasons"), "Not a citizen parcel(s).~Parcel(s) already relinquished.");

        add(LangUtil.chat("estate.transfer.success"), "The estate has been transferred.");
        add(LangUtil.chat("estate.transfer.failure"), "Unable to transfer the estate.");

        add(LangUtil.chat("estate.whitelist.add.success"), "Friend was added to the whitelist.");
        add(LangUtil.chat("estate.whitelist.add.failure"), "Unable to add friend to the whitelist.");
        add(LangUtil.chat("estate.whitelist.add.same_name.failure"), "Cannot add owner as a friend.");
        add(LangUtil.chat("estate.whitelist.remove.success"), "Friend was removed from the whitelist.");
        add(LangUtil.chat("estate.whitelist.remove.failure"), "Unable to remove friend from the whitelist.");
        add(LangUtil.chat("estate.whitelist.list"), "Friends Whitelist for estate ");

        add(LangUtil.chat("estate.block_tag.add.success"), "A block tag was added to the block tag whitelist.");
        add(LangUtil.chat("estate.block_tag.add.failure"), "Unable to add block tag to the block tag whitelist.");
        add(LangUtil.chat("estate.block_tag.remove.success"), "A block tag was removed from the block tag whitelist.");
        add(LangUtil.chat("estate.block_tag.remove.failure"), "Unable to remove block tag from the block tag whitelist.");

        add(LangUtil.chat("estate.block.add.success"), "A block was added to the block whitelist.");
        add(LangUtil.chat("estate.block.add.failure"), "Unable to add block to the block whitelist.");
        add(LangUtil.chat("estate.block.remove.success"), "A block was removed from the block whitelist.");
        add(LangUtil.chat("estate.block.remove.failure"), "Unable to remove block from the block whitelist.");

        add(LangUtil.chat("estate.item_tag.add.success"), "An item tag was added to the item tag whitelist.");
        add(LangUtil.chat("estate.item_tag.add.failure"), "Unable to add item tag to the item tag whitelist.");
        add(LangUtil.chat("estate.item_tag.remove.success"), "An item tag was removed from the item tag whitelist.");
        add(LangUtil.chat("estate.item_tag.remove.failure"), "Unable to remove item tag from the item tag whitelist.");

        add(LangUtil.chat("estate.item.add.success"), "A item was added to the item whitelist.");
        add(LangUtil.chat("estate.item.add.failure"), "Unable to add item to the item whitelist.");
        add(LangUtil.chat("estate.item.remove.success"), "A item was removed from the item whitelist.");
        add(LangUtil.chat("estate.item.remove.failure"), "Unable to remove item from the item whitelist.");

        add(LangUtil.chat("estate.block_tag.list"), "Block Tag Whitelist for estate ");
        add(LangUtil.chat("estate.block.list"), "Block Whitelist for estate ");
        add(LangUtil.chat("estate.item_tag.list"), "Item Tag Whitelist for estate ");
        add(LangUtil.chat("estate.item.list"), "Item Whitelist for estate ");

        add(LangUtil.chat("estate.nation.unable_to_locate"), "A nation with that name does not exist.");
        add(LangUtil.chat("estate.nation.not_owner"), "You are not the owner of the nation.");

        add(LangUtil.chat("estate.prevent_fire_spread.success"), "The prevent fire spread setting was updated.");

        // parcels
        add(LangUtil.chat("parcel.block_protected"),"Block is protected.");
        add(LangUtil.chat("parcel.outside_world_boundaries"), "The parcel extends beyond the world boundaries.");
        add(LangUtil.chat("parcel.max_reached"), "You have already reached your max. number of parcels.");
        add(LangUtil.chat("parcel.unable_to_locate"), "Unable to find the parcel.");
        add(LangUtil.chat("parcel.place_block.block_claimed"), "You cannot place a block there. It is already claimed.");

        add(LangUtil.chat("parcel.list"), "%s's Parcels");
        add(LangUtil.chat("parcel.list.relinquished"), "Relinquished Parcels");
        add(LangUtil.chat("parcel.list.empty"), "[Empty]");

        add(LangUtil.chat("parcel.relinquish.success"), "The parcel has been relinquished.");
        add(LangUtil.chat("parcel.relinquish.failure"), "Unable to relinquish the parcel.");
        add(LangUtil.chat("parcel.relinquish.disallowed.failure"), "This parcel can not be relinquished:");
        add(LangUtil.chat("parcel.relinquish.disallowed.reasons"), "Not a citizen parcel.~Parcel already relinquished.");


        add(LangUtil.chat("parcel.add.success"), "The parcel has been added.");
        add(LangUtil.chat("parcel.add.failure"), "Unable to add the parcel.");
        add(LangUtil.chat("parcel.add.failure_with_overlaps"), "Unable to add the parcel. It intersects with another parcel.");
        add(LangUtil.chat("parcel.add.failure_too_small"), "Unable to add the parcel. The dimension(s) are too small (< 2).");
        add(LangUtil.chat("parcel.add.structure_warning"), "Claim registered — warning: parcel overlaps a shared vanilla structure.");
        add(LangUtil.chat("parcel.add.structure_denied"), "Claim denied — parcel overlaps a protected vanilla structure.");


        add(LangUtil.chat("parcel.demolish.success"), "The parcel has been demolished.");
        add(LangUtil.chat("parcel.demolish.failure"), "Unable to demolish the parcel.");
        add(LangUtil.chat("parcel.demolish.zone_cannot_demolish"), "A Zone parcel cannot be demolished. Use 'remove' instead.");

        add(LangUtil.chat("parcel.remove.success"), "The parcel has been removed.");
        add(LangUtil.chat("parcel.remove.failure"), "Unable to remove the parcel.");

        add(LangUtil.chat("parcel.clear.success"), "The estates/parcels have been removed.");

        add(LangUtil.chat("parcel.rename.success"), "The parcel has been renamed.");
        add(LangUtil.chat("parcel.rename.failure"), "Unable to rename the parcel.");
        add(LangUtil.chat("parcel.rename.exists.failure"), "Unable to rename. Duplicate name within estate.");

        add(LangUtil.chat("parcel.transfer.success"), "The parcel has been transferred.");
        add(LangUtil.chat("parcel.transfer.failure"), "Unable to transfer the parcel.");

        add(LangUtil.chat("parcel.whitelist.add.success"), "Friend was added to the whitelist.");
        add(LangUtil.chat("parcel.whitelist.add.failure"), "Unable to add friend to the whitelist.");
        add(LangUtil.chat("parcel.whitelist.remove.success"), "Friend was removed from the whitelist.");
        add(LangUtil.chat("parcel.whitelist.remove.failure"), "Unable to remove friend from the whitelist.");
        add(LangUtil.chat("parcel.whitelist.list"), "Friends Whitelist for parcel ");

        add(LangUtil.chat("parcel.block_tag.add.success"), "A block tag was added to the block tag whitelist.");
        add(LangUtil.chat("parcel.block_tag.add.failure"), "Unable to add block tag to the block tag whitelist.");
        add(LangUtil.chat("parcel.block_tag.remove.success"), "A block tag was removed from the block tag whitelist.");
        add(LangUtil.chat("parcel.block_tag.remove.failure"), "Unable to remove block tag from the block tag whitelist.");

        add(LangUtil.chat("parcel.block.add.success"), "A block was added to the block whitelist.");
        add(LangUtil.chat("parcel.block.add.failure"), "Unable to add block to the block whitelist.");
        add(LangUtil.chat("parcel.block.remove.success"), "A block was removed from the block whitelist.");
        add(LangUtil.chat("parcel.block.remove.failure"), "Unable to remove block from the block whitelist.");

        add(LangUtil.chat("parcel.item_tag.add.success"), "An item tag was added to the item tag whitelist.");
        add(LangUtil.chat("parcel.item_tag.add.failure"), "Unable to add item tag to the item tag whitelist.");
        add(LangUtil.chat("parcel.item_tag.remove.success"), "An item tag was removed from the item tag whitelist.");
        add(LangUtil.chat("parcel.item_tag.remove.failure"), "Unable to remove item tag from the item tag whitelist.");

        add(LangUtil.chat("parcel.item.add.success"), "A item was added to the item whitelist.");
        add(LangUtil.chat("parcel.item.add.failure"), "Unable to add item to the item whitelist.");
        add(LangUtil.chat("parcel.item.remove.success"), "A item was removed from the item whitelist.");
        add(LangUtil.chat("parcel.item.remove.failure"), "Unable to remove item from the item whitelist.");

        add(LangUtil.chat("parcel.block_tag.list"), "Block Tag Whitelist for parcel ");
        add(LangUtil.chat("parcel.block.list"), "Block Whitelist for parcel ");
        add(LangUtil.chat("parcel.item_tag.list"), "Item Tag Whitelist for parcel ");
        add(LangUtil.chat("parcel.item.list"), "Item Whitelist for parcel ");

        add(LangUtil.chat("parcel.citizen.nationId_required"), "A nation name is required to add a citizen parcel.");
        add(LangUtil.chat("parcel.nation.nationName_already_exists"), "A nation with that name already exists.");
        add(LangUtil.chat("parcel.nation.unable_to_locate"), "A nation with that name does not exist.");
        add(LangUtil.chat("parcel.nation.not_owner"), "You are not the owner of the nation.");

        add(LangUtil.chat("parcel.unknown_type"), "Unknown parcel type.");

        // deeds
        add(LangUtil.chat("deed.claim.success"), "Parcel claimed!");
        add(LangUtil.chat("deed.claim.success.detail"), "You claimed a parcel at [%s] of size [%s].");
        add(LangUtil.chat("deed.claim.intersects"), "You cannot claimed this parcel as it intersects with another.");
        add(LangUtil.chat("deed.claim.insufficient_size"), "Unable to claim.");
        add(LangUtil.chat("deed.claim.insufficient_size.detail"), "The deed's size [%s] is insufficient to claim parcel of size [%s].");
        add(LangUtil.chat("deed.claim.structure_warning"), "Claim registered — warning: parcel overlaps a shared vanilla structure.");
        add(LangUtil.chat("deed.claim.structure_denied"), "Claim denied — parcel overlaps a protected vanilla structure.");

        add(LangUtil.chat("deed.claim.unable_to_claim"), "Unable to claim.");
        add(LangUtil.chat("deed.claim.unable_to_claim.detail"), "You are unable to claim parcel at [%s] of size [%s].");

        add(LangUtil.chat("deed.generate.failure"), "Unable to generate deed.");
        add(LangUtil.chat("deed.not_owner"), "You are not the owner of this deed.");
        add(LangUtil.chat("deed.too_small"), "One of the Deed's dimensions is too small.");
        add(LangUtil.chat("deed.outside_world_boundaries"), "The parcel would extend beyond the world boundaries.");
        add(LangUtil.chat("deed.invalid_type"), "Invalid deed type. The accepted values are: PLAYER, CITIZEN, or NATION.");

        // foundation stones
        add(LangUtil.chat("foundation_stone.unable_to_location"), "Unable to locate foundation stone block entity.");
        add(LangUtil.chat("foundation_stone.incorrect_deed"), "Only the deed used to place the foundation stone can be used here.");

        // citizen placement
        add(LangUtil.chat("citizen_placement.not_valid_parent"), "That block is not within a valid parent parcel (nation | zone) or it is claimed.");

        // zone placement
        add(LangUtil.chat("zone_placement.not_nation"), "That block is not within a nation parcel or it is claimed.");
        add(LangUtil.chat("zone_placement.not_owner"), "You are not the owner of this nation.");
        add(LangUtil.chat("zone_placement.not_same_nation"), "That block is not within the same nation parcel as the start block.");


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
        add(LangUtil.chat("parcel.claimed_by.not_claimed"), "The block at %s is not claimed.");
        add(LangUtil.chat("parcel.claimed_by.relinquished"), "None (relinquished)");

        add(LangUtil.chat("estate.player.whitelist"), "Player Whitelist: ");
        add(LangUtil.chat("estate.block.whitelist"), "Block Whitelist: ");
        add(LangUtil.chat("estate.block_tag.whitelist"), "Block Tag Whitelist: ");
        add(LangUtil.chat("estate.item.whitelist"), "Item Whitelist: ");
        add(LangUtil.chat("estate.item_tag.whitelist"), "Item Tag Whitelist: ");
        add(LangUtil.chat("estate.entity_spawn.whitelist"), "Entity Spawn Whitelist: ");
        add(LangUtil.chat("estate.entity_spawn_tag.whitelist"), "Entity Spawn Tag Whitelist: ");

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

        // estate
        add(LangUtil.tooltip("estate.details"), "Click to view estate details");

        // parcel
//        add(LangUtil.tooltip("parcel.howto.remove"), "Place cornerstone block inside parcel boundaries.\\nUse cornerstone block to open GUI.\nClick Remove button.");

    }
}
