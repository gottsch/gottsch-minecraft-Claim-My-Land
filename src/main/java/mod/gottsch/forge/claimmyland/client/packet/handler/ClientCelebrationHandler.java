/*
 * This file is part of Claim My Land.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
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
 *
 */

package mod.gottsch.forge.claimmyland.client.packet.handler;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.network.ClaimCelebrationPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * @author by Mark Gottschling on 3/26/2026
 */
@OnlyIn(Dist.CLIENT)
public class ClientCelebrationHandler {
    /**
     * Perimeter length (in blocks) below which the full perimeter is walked.
     * Above this threshold the capped arc mode is used instead.
     */
    private static final int FULL_PERIMETER_THRESHOLD = 128;

    /**
     * In capped arc mode: number of blocks to walk outward from the Foundation
     * Stone along each of the four edges.
     */
    private static final int ARC_LENGTH = 12;


    public static void handle(ClaimCelebrationPacket packet) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;


        int perimeter = 2 * ((packet.getMaxX() - packet.getMinX()) + (packet.getMaxZ() - packet.getMinZ()));

        if (perimeter <= FULL_PERIMETER_THRESHOLD) {
            emitFullPerimeter(level,
                    packet.getMinX(), packet.getMinZ(),
                    packet.getMaxX(), packet.getMaxZ(),
                    packet.getBorderStoneY());
        } else {
            emitCappedArc(level,
                    packet.getMinX(), packet.getMinZ(),
                    packet.getMaxX(), packet.getMaxZ(),
                    packet.getBorderStoneX(), packet.getBorderStoneY(), packet.getBorderStoneZ());
        }
    }

    // -------------------------------------------------------------------------
    // Private — full perimeter mode
    // -------------------------------------------------------------------------

    /**
     * Emits particles along every block of all four XZ perimeter edges.
     * Used when the parcel perimeter is ≤ {@value #FULL_PERIMETER_THRESHOLD} blocks.
     */
    private static void emitFullPerimeter(Level level,
                                          int minX, int minZ, int maxX, int maxZ,
                                          int stoneY) {
        RandomSource random = level.getRandom();
        double cy = stoneY + 0.5;
        double cx = minX + (maxX - minX) / 2.0;
        double cz = minZ + (maxZ - minZ) / 2.0;

        // North edge: z = minZ, x from minX to maxX
        for (int x = minX; x <= maxX; x++) {
            emitAt(level, random, x + 0.5, cy, minZ + 0.5, cx, cz);
        }
        // South edge: z = maxZ, x from minX to maxX
        for (int x = minX; x <= maxX; x++) {
            emitAt(level, random, x + 0.5, cy, maxZ + 0.5, cx, cz);
        }
        // West edge: x = minX, z from minZ+1 to maxZ-1 (corners already covered)
        for (int z = minZ + 1; z < maxZ; z++) {
            emitAt(level, random, minX + 0.5, cy, z + 0.5, cx, cz);
        }
        // East edge: x = maxX, z from minZ+1 to maxZ-1
        for (int z = minZ + 1; z < maxZ; z++) {
            emitAt(level, random, maxX + 0.5, cy, z + 0.5, cx, cz);
        }
    }

    // -------------------------------------------------------------------------
    // Private — capped arc mode
    // -------------------------------------------------------------------------

    /**
     * Emits particles for up to {@value #ARC_LENGTH} blocks outward from the
     * Foundation Stone position along each of the four parcel edges.
     * Used when the parcel perimeter exceeds {@value #FULL_PERIMETER_THRESHOLD} blocks.
     * <p>
     * The stone position is clamped to the nearest point on each edge before
     * walking — this handles cases where the stone is not exactly on the perimeter.
     * </p>
     */
    private static void emitCappedArc(Level level,
                                      int minX, int minZ, int maxX, int maxZ,
                                      int stoneX, int stoneY, int stoneZ) {
        RandomSource random = level.getRandom();
        double cy = stoneY + 0.5;
        double cx = minX + (maxX - minX) / 2.0;
        double cz = minZ + (maxZ - minZ) / 2.0;

        // Clamp stone XZ to parcel bounds so the arc origin is always valid
        int clampedX = Math.max(minX, Math.min(maxX, stoneX));
        int clampedZ = Math.max(minZ, Math.min(maxZ, stoneZ));

        // North edge (z = minZ): walk ±ARC_LENGTH in X from the stone's X
        int northOriginX = clampedX;
        for (int x = northOriginX - ARC_LENGTH; x <= northOriginX + ARC_LENGTH; x++) {
            if (x < minX || x > maxX) continue;
            emitAt(level, random, x + 0.5, cy, minZ + 0.5, cx, cz);
        }

        // South edge (z = maxZ): walk ±ARC_LENGTH in X from the stone's X
        int southOriginX = clampedX;
        for (int x = southOriginX - ARC_LENGTH; x <= southOriginX + ARC_LENGTH; x++) {
            if (x < minX || x > maxX) continue;
            emitAt(level, random, x + 0.5, cy, maxZ + 0.5, cx, cz);
        }

        // West edge (x = minX): walk ±ARC_LENGTH in Z from the stone's Z
        int westOriginZ = clampedZ;
        for (int z = westOriginZ - ARC_LENGTH; z <= westOriginZ + ARC_LENGTH; z++) {
            if (z < minZ || z > maxZ) continue;
            emitAt(level, random, minX + 0.5, cy, z + 0.5, cx, cz);
        }

        // East edge (x = maxX): walk ±ARC_LENGTH in Z from the stone's Z
        int eastOriginZ = clampedZ;
        for (int z = eastOriginZ - ARC_LENGTH; z <= eastOriginZ + ARC_LENGTH; z++) {
            if (z < minZ || z > maxZ) continue;
            emitAt(level, random, maxX + 0.5, cy, z + 0.5, cx, cz);
        }
    }

    // -------------------------------------------------------------------------
    // Private — per-position emission
    // -------------------------------------------------------------------------

    /**
     * Emits celebration particles at a single perimeter position.
     *
     * @param px  particle X (block centre)
     * @param py  particle Y
     * @param pz  particle Z (block centre)
     * @param cx  parcel XZ centre X — used to compute outward direction for POOF
     * @param cz  parcel XZ centre Z
     */
    private static void emitAt(Level level, RandomSource random,
                               double px, double py, double pz,
                               double cx, double cz) {
        // HAPPY_VILLAGER — rises upward with slight XZ jitter
        level.addParticle(ParticleTypes.HAPPY_VILLAGER,
                px + (random.nextDouble() - 0.5) * 0.3,
                py,
                pz + (random.nextDouble() - 0.5) * 0.3,
                0.0, 0.1 + random.nextDouble() * 0.05, 0.0);

        level.addParticle(ParticleTypes.HAPPY_VILLAGER,
                px + (random.nextDouble() - 0.5) * 0.3,
                py + 0.3,
                pz + (random.nextDouble() - 0.5) * 0.3,
                0.0, 0.08 + random.nextDouble() * 0.04, 0.0);

        // POOF — radiates outward from parcel centre
        double dx = px - cx;
        double dz = pz - cz;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len > 0) {
            dx /= len;
            dz /= len;
        }
        level.addParticle(ParticleTypes.POOF,
                px, py, pz,
                dx * 0.12, 0.04, dz * 0.12);

        level.addParticle(ParticleTypes.POOF,
                px, py + 0.2, pz,
                dx * 0.08, 0.06, dz * 0.08);
    }
}
