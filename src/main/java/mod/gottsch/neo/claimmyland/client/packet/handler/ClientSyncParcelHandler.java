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

package mod.gottsch.neo.claimmyland.client.packet.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Emits a dust displacement particle effect at the Foundation Stone's position.
 * Called client-side only, on genuinely new preview parcels (first sync after placement).
 * <p>
 * Two phases:
 * <ol>
 *   <li><b>Radial ground burst</b> — 24 {@code POOF} particles radiating outward
 *       at ground level from the stone's XZ centre.</li>
 *   <li><b>Rising column</b> — 10 {@code SMOKE} particles rising from the stone's
 *       centre with random XZ jitter.</li>
 * </ol>
 * </p>
 *
 * @param stoneY  Y coordinate of the Foundation Stone block
 * @param stoneX  X coordinate of the Foundation Stone block
 * @param stoneZ  Z coordinate of the Foundation Stone block
 *
 * @author by Mark Gottschling on 3/26/2026
 */
@OnlyIn(Dist.CLIENT)
public class ClientSyncParcelHandler {
    public static void emitFoundationStoneParticles(int stoneY, int stoneX, int stoneZ) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        RandomSource random = level.getRandom();

        double cx = stoneX + 0.5;
        double cy = stoneY;       // ground level — air expelled from beneath the stone
        double cz = stoneZ + 0.5;

        // Phase 1 — radial ground burst
        int ringCount = 24;
        double ringRadius = 0.6;
        for (int i = 0; i < ringCount; i++) {
            double angle = 2.0 * Math.PI * i / ringCount;
            double vx = Math.cos(angle) * 0.14;
            double vz = Math.sin(angle) * 0.14;
            double px = cx + Math.cos(angle) * ringRadius;
            double pz = cz + Math.sin(angle) * ringRadius;
            level.addParticle(ParticleTypes.POOF, px, cy, pz, vx, 0.025, vz);
        }

        // Phase 2 — rising column
        int columnCount = 10;
        for (int i = 0; i < columnCount; i++) {
            double jitterX = (random.nextDouble() - 0.5) * 0.4;
            double jitterZ = (random.nextDouble() - 0.5) * 0.4;
            double vy = 0.07 + random.nextDouble() * 0.06;
            level.addParticle(ParticleTypes.SMOKE,
                    cx + jitterX, stoneY + 0.5, cz + jitterZ,
                    0.0, vy, 0.0);
        }
    }
}
