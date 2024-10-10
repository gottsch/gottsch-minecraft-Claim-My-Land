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
package mod.gottsch.forge.claimmyland.core.persistence;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.registry.PlayerRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

/**
 * 
 * @author Mark Gottschling on Sep 14, 2024
 *
 */
public class PersistedData extends SavedData {

	private static final String PARCEL_REGISTRY = "parcel_registry";
	private static final String PLAYER_REGISTRY = "player_registry";
	
	/**
	 * 
	 * @return
	 */
	public static PersistedData create() {
		return new PersistedData();
	}

	public static PersistedData load(CompoundTag tag) {
		ClaimMyLand.LOGGER.debug("loading world data...");
		if (tag.contains(PARCEL_REGISTRY)) {
			ParcelRegistry.load(tag.getCompound(PARCEL_REGISTRY));
		}
		if (tag.contains(PLAYER_REGISTRY)) {
			PlayerRegistry.load(tag.getCompound(PLAYER_REGISTRY));
		}
		return create();
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ClaimMyLand.LOGGER.debug("saving world data...");
		tag.put(PARCEL_REGISTRY, ParcelRegistry.save(new CompoundTag()));
		tag.put(PLAYER_REGISTRY, PlayerRegistry.save(new CompoundTag()));
		return tag;
	}
	
	/**
	 * @param world
	 * @return
	 */
	public static PersistedData get(Level world) {
		DimensionDataStorage storage = ((ServerLevel)world).getDataStorage();
		PersistedData data = (PersistedData) storage.computeIfAbsent(
				PersistedData::load, PersistedData::create, ClaimMyLand.MOD_ID);
		return data;
	}
}
