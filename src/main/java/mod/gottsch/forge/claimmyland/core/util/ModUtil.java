package mod.gottsch.forge.claimmyland.core.util;

import com.google.gson.Gson;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

public class ModUtil {

    private ModUtil() {}

    public static ResourceLocation getName(Block block) {
        // don't bother checking optional - if it is empty, then the block isn't registered and this shouldn't run anyway.
        ResourceLocation name = ForgeRegistries.BLOCKS.getResourceKey(block).get().location();
        return name;
    }

    public static ResourceLocation getName(Item item) {
        // don't bother checking optional - if it is empty, then the block isn't registered and this shouldn't run anyway.
        ResourceLocation name = ForgeRegistries.ITEMS.getResourceKey(item).get().location();
        return name;
    }

    public static ResourceLocation getName(EntityType<?> item) {
        // don't bother checking optional - if it is empty, then the block isn't registered and this shouldn't run anyway.
        ResourceLocation name = ForgeRegistries.ENTITY_TYPES.getResourceKey(item).get().location();
        return name;
    }

    public static String getPlayerNameByUUID(UUID uuid) {
//        MinecraftServer server = MinecraftServer.getServer();
//        if (server != null) {
//            PlayerList playerList = server.getPlayerList();
//            if (playerList != null) {
//                OfflinePlayer player = playerList.getPlayerByUUID(uuid);
//                if (player != null) {
//                    return player.getName();
//                }
//            }
//        }
        return null;
    }

    /**
     * convenience method until GottschCore is updated to include this in Box
     * @param box
     * @param size
     * @return
     */
    public static Box inflate(Box box, int size) {
        return new Box(box.getMinCoords().add(-size, -size, -size), box.getMaxCoords().add(size, size, size));
    }

    /**
     * GottschCore's version of this is wrong.
     * need to add (1, 1, 1) because you must include the pos at min.
     * ie. min = 1, max = 5, delta = 4, but the actual size is 5.
     * @return
     */
    public static ICoords getSize(Box box) {
        return box.getMaxCoords().delta(box.getMinCoords()).add(1, 1, 1);
    }

    /**
     * area default is xz plane
     * convenience method until GottschCore is updated to include this in Box
     * @param box
     * @return
     */
    public static int getArea(Box box) {
        ICoords absoluteSize = ModUtil.getSize(box);
        return absoluteSize.getX() * absoluteSize.getZ();
    }

    public static int getVolume(Box box) {
      ICoords absoluteSize = ModUtil.getSize(box);
      return absoluteSize.getX() * absoluteSize.getZ() * absoluteSize.getY();
    }

    public static boolean intersects(Box box1, Box box2) {
        return toAABB(box1).intersects(toAABB(box2));
    }

    /**
     * a variant of intersects where result is true is the borders are touching
     * @param box1
     * @param box2
     * @return
     */
    public static boolean touching(Box box1, Box box2) {
        return box1.getMinCoords().getX() <= box2.getMaxCoords().getX()
                && box1.getMaxCoords().getX() >= box2.getMinCoords().getX()
                && box1.getMinCoords().getY() <= box2.getMaxCoords().getY()
                && box1.getMaxCoords().getY() >= box2.getMinCoords().getY()
                && box1.getMinCoords().getZ() <= box2.getMaxCoords().getZ()
                && box1.getMaxCoords().getZ() >= box2.getMinCoords().getZ();
    }

    public static boolean contains(Box box1, Box box2) {
        return contains(box1, box2.getMinCoords())
                && contains(box1, box2.getMaxCoords());
    }

    public static boolean contains(Box box, ICoords coords) {
        return coords.getX() >= box.getMinCoords().getX() && coords.getX() <= box.getMaxCoords().getX()
                && coords.getY() >= box.getMinCoords().getY() && coords.getY() <= box.getMaxCoords().getY()
                && coords.getZ() >= box.getMinCoords().getZ() && coords.getZ() <= box.getMaxCoords().getZ();
    }
    ///////////// from AABB - why >= min, BUT only < max ???
//    public boolean contains(Vec3 p_82391_) {
//        return this.contains(p_82391_.x, p_82391_.y, p_82391_.z);
//    }
//
//    public boolean contains(double p_82394_, double p_82395_, double p_82396_) {
//        return p_82394_ >= this.minX && p_82394_ < this.maxX && p_82395_ >= this.minY && p_82395_ < this.maxY && p_82396_ >= this.minZ && p_82396_ < this.maxZ;
//    }
    /////////////////////

    public static AABB toAABB(Box box) {
        return new AABB(box.getMinCoords().toPos(), box.getMaxCoords().toPos());
    }

    // TODO move to PlayerRegistry
    private static final String API_URL = "https://api.mojang.com/users/profiles/minecraft/";

    public static Optional<UUID> getUUIDByPlayerName(String playerName) {
//        String uuid = null;
        MinecraftID minecraftID = null;
        try {
            // Construct the URL to the Mojang API
            String urlString = API_URL + playerName;
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Read the response
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // parse the JSON response
            String jsonResponse = response.toString();
            System.out.println(jsonResponse.toString());
            if (!jsonResponse.isEmpty()) {
                Gson gson = new Gson();
                minecraftID = gson.fromJson(jsonResponse, MinecraftID.class);
//                JSONObject jsonObject = new JSONObject(jsonResponse);
//                uuid = jsonObject.getString("id"); // This returns the UUID
            }
//            System.out.println("uuid ->" + uuid);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return minecraftID != null ? Optional.of(UUID.fromString(minecraftID.getId())) : Optional.empty();
    }

    public static class MinecraftID {
        String id;
        String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
