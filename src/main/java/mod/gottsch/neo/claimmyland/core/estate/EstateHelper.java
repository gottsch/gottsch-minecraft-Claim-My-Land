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

package mod.gottsch.neo.claimmyland.core.estate;

import mod.gottsch.neo.claimmyland.core.registry.PlayerRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * @author by Mark Gottschling on 3/5/2026
 */
public class EstateHelper {

    public static String buildName(ServerLevel level, UUID ownerId) {
        String playerName = PlayerRegistry.getPlayerName(level, ownerId)
                .orElse(ownerId.toString().substring(0, 8));
        int index = PlayerRegistry.nextEstateNameIndex(ownerId);
        return playerName + "-estate-" + index;
    }

    public static String buildName(ServerPlayer player) {
        int index = PlayerRegistry.nextEstateNameIndex(player.getUUID());
        return player.getScoreboardName() + "-estate-" + index;
    }

    private EstateHelper() {}
}
