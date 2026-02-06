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
package mod.gottsch.forge.claimmyland.core.estate;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * this class is currently just an example class as Estate creation is handled by the containing concrete Parcel.
 * @author by Mark Gottschling on 1/31/2026
 */
public class EstateTypeRegistry {
    @FunctionalInterface
    public interface NoArgFactory {
        Estate create();
    }

    @FunctionalInterface
    public interface PlayerFactory<T extends Player> {
        Estate create(T player);
    }

    private static final Map<ResourceLocation, NoArgFactory> NO_ARG_FACTORY = new HashMap<>();
    private static final Map<ResourceLocation, PlayerFactory<Player>> PLAYER_FACTORY = new HashMap<>();

    public static void register(ResourceLocation type, NoArgFactory factory) {
        NO_ARG_FACTORY.put(type, factory);
    }

    public static <T1> void register(ResourceLocation type, PlayerFactory<Player> factory) {
        PLAYER_FACTORY.put(type, factory);
    }

    public static Estate create(ResourceLocation type) {
        NoArgFactory factory = NO_ARG_FACTORY.get(type);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown type: " + type);
        }
        return factory.create();
    }

    public static <T extends Player> Estate create(ResourceLocation type, Player player) {
        PlayerFactory<Player> factory = (PlayerFactory<Player>) PLAYER_FACTORY.get(type);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown type: " + type);
        }
        return factory.create(player);
    }

    static {
        register(EstateContext.ESTATE_CONTEXT, () -> new EstateContext());
//        register("playerEstate", (player) -> new EstateContext(player));
    }
}
