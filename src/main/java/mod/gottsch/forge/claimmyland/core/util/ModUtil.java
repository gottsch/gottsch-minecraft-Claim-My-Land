package mod.gottsch.forge.claimmyland.core.util;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

public class ModUtil {

    private ModUtil() {}
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
     * convenience method until GottschCore is updated to include this in Box
     * @param box
     * @return
     */
    public static int getArea(Box box) {
      ICoords absoluteSize = ModUtil.getSize(box);
      return absoluteSize.getX() * absoluteSize.getZ() * absoluteSize.getY();
    }

    // TODO add to Box in GottschCore
    public static boolean intersects(Box box1, Box box2) {
        return toAABB(box1).intersects(toAABB(box2));
    }

    // TODO add to Box in GottschCore
    public static boolean contains(Box box1, Box box2) {
        AABB aabb = toAABB(box1);
        return aabb.contains(box2.getMinCoords().toVec3())
                && aabb.contains(box2.getMaxCoords().toVec3());
    }

    // TODO add to Box in GottschCore
    public static AABB toAABB(Box box) {
        return new AABB(box.getMinCoords().toPos(), box.getMaxCoords().toPos());
    }
}
