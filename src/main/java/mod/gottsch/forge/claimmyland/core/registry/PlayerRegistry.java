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
            NAMES.inverse().computeIfPresent(name.toLowerCase(), (key, val) -> id);
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

    public static synchronized void save(CompoundTag tag) {
        ListTag listTag = new ListTag();
        NAMES.forEach((key, val) -> {
            CompoundTag kv = new CompoundTag();
            kv.putUUID("id", key);
            kv.putString("name", val);
            listTag.add(kv);
        });
        tag.put("playerRegistry", listTag);
        ClaimMyLand.LOGGER.debug("saved player registry");
    }

    public static synchronized void load(CompoundTag tag) {
        if (tag.contains("playerRegistry")) {
            ListTag list = tag.getList("playerRegistry", Tag.TAG_COMPOUND);
            list.forEach(t -> {
                CompoundTag c = (CompoundTag) t;
                UUID id = null;
                String name = null;
                if (c.contains("id")) {
                    id = tag.getUUID("id");
                }
                if (c.contains("name")) {
                    name = tag.getString("name");
                }
                if (id != null || name != null) {
                    register(id, name);
                }
            });
        }


    }
}
