/*
 * This file is part of  Claim My Land.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
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

import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.block.entity.BorderStoneBlockEntity;
import net.minecraft.world.level.ChunkPos;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry that tracks all live BorderStoneBlockEntity instances.
 * Provides both a flat set for full iteration and a chunk-keyed index
 * for efficient chunk-based lookup (used by ChunkWatchEvent resync).
 *
 * @author Mark Gottschling on March 18, 2026
 */
public class ActiveBorderStoneRegistry {

    private static final Set<BorderStoneBlockEntity> STONES = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<ChunkPos, Set<BorderStoneBlockEntity>> BY_CHUNK = new ConcurrentHashMap<>();

    private ActiveBorderStoneRegistry() {}

    public static void add(BorderStoneBlockEntity stone) {
//        ClaimMyLand.LOGGER.debug("ActiveBorderStoneRegistry.add: stone.pos={}", stone.getBlockPos().toShortString());
        STONES.add(stone);
        BY_CHUNK.computeIfAbsent(new ChunkPos(stone.getBlockPos()), k -> ConcurrentHashMap.newKeySet()).add(stone);
    }

    public static void remove(BorderStoneBlockEntity stone) {
        STONES.remove(stone);
        ChunkPos chunk = new ChunkPos(stone.getBlockPos());
        Set<BorderStoneBlockEntity> chunkSet = BY_CHUNK.get(chunk);
        if (chunkSet != null) {
            chunkSet.remove(stone);
            if (chunkSet.isEmpty()) {
                BY_CHUNK.remove(chunk);
            }
        }
    }

    public static Set<BorderStoneBlockEntity> getAll() {
        return Collections.unmodifiableSet(STONES);
    }

    public static Set<BorderStoneBlockEntity> getInChunk(ChunkPos chunk) {
        return BY_CHUNK.getOrDefault(chunk, Collections.emptySet());
    }

    /**
     * Provided for completeness and testability. Not called in production —
     * static state is cleared naturally on server stop, and stones self-register
     * via onLoad() on server start.
     */
    public static void clear() {
        STONES.clear();
        BY_CHUNK.clear();
    }
}