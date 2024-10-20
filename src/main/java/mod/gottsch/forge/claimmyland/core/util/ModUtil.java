package mod.gottsch.forge.claimmyland.core.util;

import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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

    // TODO add to Box in GottschCore
    public static boolean contains(Box box1, Box box2) {
//        AABB aabb = toAABB(box1);
        return contains(box1, box2.getMinCoords())
                && contains(box1, box2.getMaxCoords());
//        return aabb.contains(box2.getMinCoords().toVec3())
//                && aabb.contains(box2.getMaxCoords().toVec3());
    }

    // TODO move to GottschCore
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

    // TODO add to Box in GottschCore
    public static AABB toAABB(Box box) {
        return new AABB(box.getMinCoords().toPos(), box.getMaxCoords().toPos());
    }
}
