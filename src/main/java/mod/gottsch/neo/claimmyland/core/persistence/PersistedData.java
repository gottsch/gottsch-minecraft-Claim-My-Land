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
package mod.gottsch.neo.claimmyland.core.persistence;

import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.neo.claimmyland.core.registry.PlayerRegistry;
import net.minecraft.core.HolderLookup;
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
	private static final String PARCEL_REGISTRY_V2 = "parcel_registry_v2";
	private static final String PLAYER_REGISTRY = "player_registry";

	/**
	 *
	 * @return
	 */
	public static PersistedData create() {
		return new PersistedData();
	}

	// NeoForge 1.21.1: load() receives a HolderLookup.Provider as second parameter
	public static PersistedData load(CompoundTag tag, HolderLookup.Provider provider) {
		ClaimMyLand.LOGGER.debug("loading world data...");
		if (tag.contains(PARCEL_REGISTRY_V2)) {
			ParcelRegistry.load(tag.getCompound(PARCEL_REGISTRY_V2));
		}
		PlayerRegistry.load(tag);
		return create();
	}

	// NeoForge 1.21.1: save() receives a HolderLookup.Provider as second parameter
	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
		ClaimMyLand.LOGGER.debug("saving world data...");
		tag.put(PARCEL_REGISTRY_V2, ParcelRegistry.save(new CompoundTag()));
		PlayerRegistry.save(tag);
		return tag;
	}

	/**
	 * @param world
	 * @return
	 */
	public static PersistedData get(Level world) {
		// always use the Overworld level for global storage
		ServerLevel overworld = ((ServerLevel) world).getServer().getLevel(Level.OVERWORLD);
		DimensionDataStorage storage = overworld.getDataStorage();

		return storage.computeIfAbsent(
				new SavedData.Factory<>(PersistedData::create, PersistedData::load),
				ClaimMyLand.MOD_ID);
	}
}
