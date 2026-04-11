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
package mod.gottsch.neo.claimmyland.core.event;

import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.network.CMLNetwork;
import mod.gottsch.neo.claimmyland.core.parcel.Parcel;
import mod.gottsch.neo.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.neo.claimmyland.core.registry.PlayerRegistry;
import mod.gottsch.neo.claimmyland.core.util.ModUtil;
import mod.gottsch.neo.claimmyland.core.util.TagHelper;
import mod.gottsch.neo.gottschcore.spatial.Coords;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Optional;

/**
 * @author Mark Gottschling Sep 25, 2024
 */
@EventBusSubscriber(modid = ClaimMyLand.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class PlayerEvents {

//    @SubscribeEvent
//    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
//        if (event.getEntity() instanceof ServerPlayer player) {
//            CMLNetwork.syncAllParcelsToPlayer(player);
//        }
//    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // ClientEvents already clears ClientParcelCache on the client side.
            // Here we just resync the full registry for the new dimension.
            CMLNetwork.syncAllParcelsToPlayer(player);
        }
    }

	@SubscribeEvent
	public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
		if (!event.getLevel().isClientSide()) {
			if (event.getEntity() instanceof Player) {
				PlayerRegistry.update(event.getEntity().getUUID(), event.getEntity().getScoreboardName());
			}
		}
	}

    @SubscribeEvent
    public static void onPlayerHurt(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (ServerPlayer) event.getEntity();
            String dimension = player.level().dimension().location().toString();

            // mob on player hurt
            if (event.getSource().getEntity() instanceof Mob) {
                if (ParcelRegistry.intersectsParcel(Coords.of(player.blockPosition()), dimension)) {
                    event.setCanceled(true);
                }
            }
            // player on player hurt
            else if (event.getSource().getEntity() instanceof Player) {
                if (ParcelRegistry.intersectsParcel(Coords.of(player.blockPosition()), dimension)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    /*
     * NOTE for now ALL parcels are protected against mob spawns, unless
     * the source of spawn is from an egg. the usage of the egg has it's own event
     * to check protections against.
     * Ex. if a player attempts to use an egg in a parcel that is not theirs, the egg item
     * usage would be denied and this spawn event wouldn't be called.
     */
    @SubscribeEvent
    public static void onSpawnEntity(MobSpawnEvent.PositionCheck event) {
        if (!(event.getLevel() instanceof ServerLevel world)) {
            return;
        }
        // get the parcel
        String dimension = world.dimension().location().toString();
        Optional<Parcel> parcel = ParcelRegistry.findLeastSignificant(Coords.of(event.getEntity().blockPosition()), dimension);
                if (parcel.isEmpty()) {
            return;
        }

        // process the entity spawn tag whitelist
        boolean isInWhitelist = false;
        for (String tagName : parcel.get().getEstate().getEntitySpawnTagWhitelist()) {
            ResourceLocation location = ResourceLocation.parse(tagName);
//            ClaimMyLand.LOGGER.debug("creating tag for parcel item tag -> {}", location.toString());
            // get the tag from the resource key
            if (TagHelper.doesEntityBelongToTag(event.getEntity().getType(), location)) {
                isInWhitelist = true;
                break;
            }
        }
        // check the spawn whitelist
        if (!isInWhitelist) {
            for (String entityName : parcel.get().getEstate().getEntitySpawnWhitelist()) {
                ResourceLocation location = ResourceLocation.parse(entityName);
//                ClaimMyLand.LOGGER.debug("comparing item locations for held item -> {}", entityName);
                if (ModUtil.getName(event.getEntity().getType()).equals(location)) {
                    isInWhitelist = true;
                    break;
                }
            }
        }

        if (ParcelRegistry.intersectsParcel(Coords.of(event.getEntity().blockPosition()), dimension)
         && !(event.getSpawnType().equals(MobSpawnType.SPAWN_EGG)
//                || event.getSpawnType().equals(MobSpawnType.BUCKET))
                || isInWhitelist)) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
//			ProtectIt.LOGGER.debug("denied mob spawn -> {} @ {}", event.getEntity().getDisplayName().getString(), new Coords(event.getEntity().blockPosition()).toShortString());
        }

    }
}
