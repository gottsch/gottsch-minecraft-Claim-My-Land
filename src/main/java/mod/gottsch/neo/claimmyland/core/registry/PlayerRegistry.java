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
package mod.gottsch.neo.claimmyland.core.registry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.config.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Mark Gottschling on Sep 23, 2024
 */
public class PlayerRegistry {
    private static final String MOJANG_API_URL = "https://api.mojang.com/user/profile/";
    private static final String MOJANG_API_URL2 = "https://api.mojang.com/users/profiles/minecraft/";
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    private static final String PLAYER_REGISTRY = "playerRegistry";
    private static final String NAME = "name";
    private static final String ID = "id";

    public static final BiMap<UUID, String> NAMES = HashBiMap.create();
    private static final Map<UUID, Integer> ESTATE_NAME_COUNTERS = new HashMap<>();


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
        ESTATE_NAME_COUNTERS.clear();
    }

    public static synchronized CompoundTag save(CompoundTag tag) {
        ListTag listTag = new ListTag();
        NAMES.forEach((key, val) -> {
            CompoundTag kv = new CompoundTag();
            kv.putUUID(ID, key);
            kv.putString(NAME, val);
            kv.putInt("estateNameCounter", ESTATE_NAME_COUNTERS.getOrDefault(key, 0));
            listTag.add(kv);
        });
        tag.put(PLAYER_REGISTRY, listTag);
//        ClaimMyLand.LOGGER.debug("saved player registry");
        return tag;
    }

    public static synchronized void load(CompoundTag tag) {
        if (tag.contains(PLAYER_REGISTRY)) {
            ListTag list = tag.getList(PLAYER_REGISTRY, Tag.TAG_COMPOUND);
            if (list != null) {
                list.forEach(t -> {
                    CompoundTag c = (CompoundTag) t;
                    UUID id = null;
                    String name = null;
                    if (c.contains(ID)) {
                        id = c.getUUID(ID);
                        if (c.contains("estateNameCounter")) {
                            ESTATE_NAME_COUNTERS.put(id, c.getInt("estateNameCounter"));
                        }
                    }
                    if (c.contains(NAME)) {
                        name = c.getString(NAME);
                    }
                    if (id != null || name != null) {
                        register(id, name);
                    }
                });
            }
        }
    }

    @Deprecated
    public static CompletableFuture<String> getOfflinePlayerName(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // mojang api requires uuid without dashes
                URL url = new URL(MOJANG_API_URL + uuid.toString().replace("-", ""));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    Gson gson = new Gson();
                    JsonObject jsonObject = gson.fromJson(response.toString(), JsonObject.class);
                    return jsonObject.get("name").getAsString();

                } else if(connection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT){
                    return null; //UUID not found
                } else {
                    System.err.println("Error fetching name from UUID: " + connection.getResponseCode());
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }, EXECUTOR_SERVICE);
    }

    /*
     * get player using extended search
     * 1. online
     * 2. PlayerRegistry
     * 3. offline
     */
    public static Optional<UUID> getPlayerUuid(ServerLevel level, String playerName) {
        // get the online player
        ServerPlayer player = level.getServer().getPlayerList().getPlayerByName(playerName);
        if (player == null) {
            // get the player from the player registry
            return PlayerRegistry.get(playerName).or(() -> {
                if (Config.SERVER.general.allowMojangNameCalls.get()) {
                    Optional<UUID> playerUuid = PlayerRegistry.getUUIDFromNameSynchronized(playerName);
                    // before returning, update PlayerRegistry with the UUID/name mapping
                    playerUuid.ifPresent(ownerUuid -> PlayerRegistry.update(ownerUuid, playerName));
                    return playerUuid;
                } else {
                    return Optional.empty();
                }
            });
        }
        return Optional.of(player.getUUID());
    }

    public static Optional<String> getPlayerName(ServerLevel level, UUID playerUuid) {
        // get the online player
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerUuid);
        if (player != null) {
            return Optional.of(player.getName().getString());
        }

        // get the player from the player registry
        return PlayerRegistry.get(playerUuid).or(() -> {
            if (Config.SERVER.general.allowMojangNameCalls.get()) {
                Optional<String> playerName = PlayerRegistry.getNameFromUUIDSynchronized(playerUuid);
                // before returning, update PlayerRegistry with the UUID/name mapping
                playerName.ifPresent(name -> PlayerRegistry.update(playerUuid, name));
                return playerName;
            } else {
                return Optional.empty();
            }
        });
    }

    public static Optional<String> getNameFromUUIDSynchronized(UUID uuid) {
        try {
            // mojang api requires uuid without dashes
            URL url = new URL(MOJANG_API_URL + uuid.toString().replace("-", ""));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(response.toString(), JsonObject.class);
                if (jsonObject.has("name")) {
                    String name = jsonObject.get("name").getAsString();
                    return Optional.of(name);
                } else {
                    return Optional.empty();
                }

            } else if(connection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT){
                return Optional.empty(); //UUID not found
            } else {
                System.err.println("Error fetching name from UUID: " + connection.getResponseCode());
                return Optional.empty();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static CompletableFuture<UUID> getUUIDFromName(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(MOJANG_API_URL2 + playerName);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    Gson gson = new Gson();
                    JsonObject jsonObject = gson.fromJson(response.toString(), JsonObject.class);

                    if (jsonObject.has("id")) {
                        String uuidString = jsonObject.get("id").getAsString();
                        return UUID.fromString(uuidString.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"));
                    } else {
                        return null; // Player not found or no ID in response.
                    }

                } else if(connection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT){
                    return null; //Player not found
                } else {
                    System.err.println("Error fetching UUID from name: " + connection.getResponseCode());
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }, EXECUTOR_SERVICE);
    }

    public static Optional<UUID> getUUIDFromNameSynchronized(String playerName) {
        try {
            URL url = new URL(MOJANG_API_URL2 + playerName);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(response.toString(), JsonObject.class);

                if (jsonObject.has("id")) {
                    String uuidString = jsonObject.get("id").getAsString();
                    return Optional.of(UUID.fromString(uuidString.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5")));
                } else {
                    return Optional.empty(); // player not found or no ID in response.
                }

            } else if(connection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT){
                return Optional.empty(); // player not found
            } else {
                System.err.println("Error fetching UUID from name: " + connection.getResponseCode());
                return Optional.empty();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Increments and returns the next estate name index for the given player.
     * This counter only moves forward — demolishing estates never decrements it,
     * guaranteeing unique default names for the lifetime of the world.
     *
     * Call this ONLY at estate creation commit time, not during preview.
     */
    public static synchronized int nextEstateNameIndex(UUID playerId) {
        return ESTATE_NAME_COUNTERS.merge(playerId, 1, Integer::sum);
    }

    /**
     * Returns the NEXT index that would be assigned, without consuming it.
     * Use for preview/display purposes (e.g. Foundation Stone placement feedback).
     */
    @Deprecated(since="2.1")
    public static synchronized int peekEstateNameIndex(UUID playerId) {
        return ESTATE_NAME_COUNTERS.getOrDefault(playerId, 0) + 1;
    }
}
