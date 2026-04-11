package mod.gottsch.neo.claimmyland.core.util;

import it.unimi.dsi.fastutil.ints.IntList;
import mod.gottsch.neo.claimmyland.core.config.Config;
import mod.gottsch.neo.claimmyland.core.parcel.Parcel;
import mod.gottsch.neo.claimmyland.core.parcel.ParcelType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/**
 * @author Mark Gottschling on March 26, 2026
 */
public class CelebrationHelper {

    public static void spawnFireworks(ServerLevel level, Parcel parcel) {
        if (!Config.SERVER.celebration.fireworksEnabled.get()) return;

        int count = switch (parcel.getType()) {
            case NATION -> 3;
            case ZONE   -> 2;
            default     -> 1;  // CITIZEN, PLAYER
        };

        double cx = parcel.getMinCoords().getX()
                + (parcel.getMaxCoords().getX() - parcel.getMinCoords().getX()) / 2.0;
        double cz = parcel.getMinCoords().getZ()
                + (parcel.getMaxCoords().getZ() - parcel.getMinCoords().getZ()) / 2.0;

        BlockPos centerPos = new BlockPos((int) cx, 0, (int) cz);
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, centerPos.getX(), centerPos.getZ());
        double cy = surfaceY + 1;

        for (int i = 0; i < count; i++) {
            double jx = (level.random.nextDouble() - 0.5) * 2;
            double jz = (level.random.nextDouble() - 0.5) * 2;
            ItemStack stack = buildFireworkStack(parcel.getType());
            FireworkRocketEntity rocket = new FireworkRocketEntity(
                    level, cx + jx, cy, cz + jz,
                    stack);
            level.addFreshEntity(rocket);
        }
    }

    public static ItemStack buildFireworkStack(ParcelType type) {
        int color = switch (type) {
            case NATION  -> 0x00AAFF;
            case CITIZEN -> 0xAA55FF;
            case ZONE    -> 0xFFFF55;
            default      -> 0x55FF55;
        };

        FireworkExplosion explosion = new FireworkExplosion(
                FireworkExplosion.Shape.SMALL_BALL,
                IntList.of(color),   // colors
                IntList.of(),        // fade colors
                true,                // trail
                false                // twinkle
        );

        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
        stack.set(DataComponents.FIREWORKS, new Fireworks(1, List.of(explosion)));
        return stack;
    }
}