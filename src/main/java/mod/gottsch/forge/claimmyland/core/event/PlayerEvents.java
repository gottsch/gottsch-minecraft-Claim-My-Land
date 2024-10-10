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
package mod.gottsch.forge.claimmyland.core.event;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.item.Deed;
import mod.gottsch.forge.claimmyland.core.item.DeedFactory;
import mod.gottsch.forge.claimmyland.core.item.ModItems;
import mod.gottsch.forge.claimmyland.core.item.PlayerDeed;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.registry.PlayerRegistry;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.UUID;

/**
 * 
 * @author Mark Gottschling Sep 25, 2024
 *
 */
@EventBusSubscriber(modid = ClaimMyLand.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public class PlayerEvents {

//	@SubscribeEvent
//	public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
//		if (!event.getLevel().isClientSide()) {
//			if (event.getEntity() instanceof Player) {
//				PlayerRegistry.update(event.getEntity().getUUID(), event.getEntity().getScoreboardName());
//			}
//			else if (event.getEntity() instanceof ItemEntity itemEntity) {
//			 	if (itemEntity.getItem().getItem() instanceof Deed) {
//					if (itemEntity.getItem().is(ModItems.PLAYER_DEED_10.get())) {
//						DeedFactory.createPlayerDeed(itemEntity.getItem(), new Box(Coords.of(0, -10, 0), Coords.of(9, 9, 9)));
//					} else if (itemEntity.getItem().is(ModItems.PLAYER_DEED_16.get())) {
//						DeedFactory.createPlayerDeed(itemEntity.getItem(), new Box(Coords.of(0, -16, 0), Coords.of(15, 15, 15)));
//					} else if (itemEntity.getItem().is(ModItems.PLAYER_DEED_32.get())) {
//						DeedFactory.createPlayerDeed(itemEntity.getItem(), new Box(Coords.of(0, -32, 0), Coords.of(32, 32, 32)));
//					} else if (itemEntity.getItem().is(ModItems.NATION_DEED_100.get())) {
//						DeedFactory.createNationDeed(event.getLevel(), itemEntity.getItem(), new Box(Coords.of(0, 0, 0), Coords.of(99, 0, 99)));
//					}
//				}
//			}
//		}
//	}

//	private static void createPlayerParcel(ItemStack stack, Box size) {
//		CompoundTag tag = stack.getOrCreateTag();
//		// create a relative sized Box
//		tag.putUUID(Deed.PARCEL_ID, UUID.randomUUID());
//		tag.putUUID(Deed.DEED_ID, UUID.randomUUID());
//		tag.putString(Deed.PARCEL_TYPE, ParcelType.PLAYER.name());
//		CompoundTag sizeTag = new CompoundTag();
//		size.save(sizeTag);
//		tag.put(Deed.SIZE, sizeTag);
//	}

	@SubscribeEvent
	public static void onPlayerHurt(LivingHurtEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (ServerPlayer) event.getEntity();
			
			// mob on player hurt
			if (event.getSource().getEntity() instanceof Mob) {
				// prevent mob from hurting player
				// NOTE for now ALL parcels are protected against hurt events
				if (ParcelRegistry.intersectsParcel(Coords.of(player.blockPosition()))) {
					event.setCanceled(true);
//					ProtectIt.LOGGER.debug("denied mob attack -> {} @ {}", event.getEntity().getDisplayName().getString(), new Coords(player.blockPosition()).toShortString());
				}
			}
			// player on player hurt
			else if (event.getSource().getEntity() instanceof Player) {
				// prevent player from hurting player
				// NOTE for now ALL parcels are protected against hurt events
				if (ParcelRegistry.intersectsParcel(Coords.of(player.blockPosition()))) {
					event.setCanceled(true);
//					ProtectIt.LOGGER.debug("denied player attack -> {} @ {}", event.getEntity().getDisplayName().getString(), new Coords(player.blockPosition()).toShortString());
				}
			}
		}
	}

	// NOTE for now ALL parcels are protected against mob spawns
	@SubscribeEvent
	public static void onSpawnEntity(MobSpawnEvent.FinalizeSpawn event) {
		if (ParcelRegistry.intersectsParcel(Coords.of(event.getEntity().blockPosition()))) {
			event.setResult(Result.DENY);
			event.setSpawnCancelled(true);
//			ProtectIt.LOGGER.debug("denied mob spawn -> {} @ {}", event.getEntity().getDisplayName().getString(), new Coords(event.getEntity().blockPosition()).toShortString());
		}
	}
}
