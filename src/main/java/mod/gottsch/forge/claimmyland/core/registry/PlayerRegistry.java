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
package mod.gottsch.forge.claimmyland.core.registry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import mod.gottsch.forge.claimmyland.ClaimMyLand;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

/**
 * Created by Mark Gottschling on Sep 23, 2024
 */
public class PlayerRegistry {

    public static final BiMap<UUID, String> NAMES = HashBiMap.create();

    private PlayerRegistry() {}

    public static void register(Level level, UUID id) {
        Player player = level.getPlayerByUUID(id);
        if (player != null) {
            register(id, player.getScoreboardName());
        } else {
            register(id, null);
        }
    }

    public static void register(String name) {
        register(null, name);
    }

    public static void register(UUID id, String name) {
        NAMES.put(id, name);
    }

    public static void update(UUID id, String name) {
        String result = NAMES.computeIfPresent(id, (key, val) -> name.toLowerCase());
        if (result == null) {
            UUID idResult = NAMES.inverse().computeIfPresent(name.toLowerCase(), (key, val) -> id);
            if (idResult == null) {
                register(id, name);
            }
        }
    }

    /**
     * get name by id
     * @param
     * @return
     */
    public static Optional<String> get(UUID id) {
        Optional<String> result = Optional.empty();
        if (NAMES.containsKey(id)) {
            result = Optional.of(NAMES.get(id));
        }
        return result;
    }

    /**
     * get id by name
     * @param name
     * @return
     */
    public static Optional<UUID> get(String name) {
        Optional<UUID> result = Optional.empty();
        if (NAMES.inverse().containsKey(name.toLowerCase())) {
            result = Optional.of(NAMES.inverse().get(name.toLowerCase()));
        }
        return result;
    }

    public static void clear() {
        NAMES.clear();
    }

    public static synchronized CompoundTag save(CompoundTag tag) {
        ListTag listTag = new ListTag();
        NAMES.forEach((key, val) -> {
            CompoundTag kv = new CompoundTag();
            kv.putUUID("id", key);
            kv.putString("name", val);
            listTag.add(kv);
        });
        tag.put("playerRegistry", listTag);
        ClaimMyLand.LOGGER.debug("saved player registry");
        return tag;
    }

    public static synchronized void load(CompoundTag tag) {
        if (tag.contains("playerRegistry")) {
            ListTag list = tag.getList("playerRegistry", Tag.TAG_COMPOUND);
            if (list != null) {
                list.forEach(t -> {
                    CompoundTag c = (CompoundTag) t;
                    UUID id = null;
                    String name = null;
                    if (c.contains("id")) {
                        id = c.getUUID("id");
                    }
                    if (c.contains("name")) {
                        name = c.getString("name");
                    }
                    if (id != null || name != null) {
                        register(id, name);
                    }
                });
            }
        }
    }
}
