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

package mod.gottsch.forge.claimmyland.core.parcel;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.estate.NationEstate;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * @author by Mark Gottschling on 1/31/2026
 */
public class ParcelTypeRegistry {

//    @FunctionalInterface
//    public interface ParcelFactory {
//        Parcel create();
//    }

    @FunctionalInterface
    public interface ParcelFactory {
        Parcel create(Object... args);
    }

    private static final Map<ParcelType, ParcelFactory> PARCEL_FACTORY = new HashMap<>();

    public static void register(ParcelType type, ParcelFactory factory) {
        PARCEL_FACTORY.put(type, factory);
    }

    // solution 1
//    static {
//        register(ParcelType.PLAYER, PlayerParcel::new);
//    }

    public static Optional<Parcel> create(ParcelType type, Object... args) {
        ParcelFactory factory = PARCEL_FACTORY.get(type);
        if (factory == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(factory.create(args));
        } catch (Exception e) {
            ClaimMyLand.LOGGER.error("failed to create instance of type: " + type, e);
            return Optional.empty();
        }
    }

    // solution 2

    static {
        // Register with lambda that handles different arg counts
        register(ParcelType.PLAYER, args -> {
            if (args.length == 0) {
                return PlayerParcel.create();
            } else if (args.length == 1 && args[0] instanceof Parcel) {
                return PlayerParcel.create((Parcel) args[0]);
            }
            throw new IllegalArgumentException("invalid arguments for PlayerParcel.");
        });
        register(ParcelType.CITIZEN, args -> {
            if(args.length == 0) {
                return CitizenParcel.create();
            }
//            else if (args.length == 1 && args[0] instanceof UUID) {
//                return CitizenParcel.create((UUID) args[0]);
//            }
            throw new IllegalArgumentException("invalid arguments for CitizenParcel.");
        });
        register(ParcelType.NATION, args -> {
            if (args.length ==0) {
                return NationParcel.create();
            }
            throw new IllegalArgumentException("invalid arguments for NationParcel.");
        });
        register(ParcelType.ZONE, args -> {
            if (args.length == 0) {
                return ZoneParcel.create();
            }
//            else if (args.length == 1 && args[0] instanceof UUID) {
//                return ZoneParcel.create((UUID) args[0]);
//            }
            else if (args.length == 1 && args[0] instanceof NationParcel) { // deprecated
                return ZoneParcel.create((NationParcel) args[0]);
            }
            else if (args.length == 1 && args[0] instanceof NationEstate) {
                return ZoneParcel.create((NationEstate) args[0]);
            }
            throw new IllegalArgumentException("invalid arguments for ZoneParcel.");
        });
    }
}