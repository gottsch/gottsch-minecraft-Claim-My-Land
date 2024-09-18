/*
 * This file is part of  Claim My Land.
 * Copyright (c) 2024 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.claimmyland.core.parcel;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Create by Mark Gottschling on Sep 14, 2024
 */
public enum ParcelType implements StringRepresentable {
    PERSONAL,
    NATION,
    CITIZEN,
    CITIZEN_CLAIM_ZONE;

    public static List<String> getNames() {
        return EnumSet.allOf(ParcelType.class).stream().map(Enum::name).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return this.name();
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name();
    }
}
