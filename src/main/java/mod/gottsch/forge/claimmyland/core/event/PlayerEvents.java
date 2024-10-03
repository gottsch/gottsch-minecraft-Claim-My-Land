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
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * 
 * @author Mark Gottschling Sep 25, 2024
 *
 */
@EventBusSubscriber(modid = ClaimMyLand.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public class PlayerEvents {

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
