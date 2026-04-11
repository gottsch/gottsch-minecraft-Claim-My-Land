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
package mod.gottsch.neo.claimmyland.core.util;

import mod.gottsch.neo.claimmyland.core.config.Config;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;

/**
 * Builds and caches a {@link StructureIntersectionChecker.StructurePolicy}
 * from the server configuration. The policy is constructed once at world load
 * and invalidated on config reload.
 *
 * @author Mark Gottschling on <date>
 */
public class StructurePolicyFactory {

    private static StructureIntersectionChecker.StructurePolicy cachedPolicy = null;

    // private constructor — static utility class
    private StructurePolicyFactory() {}

    /**
     * Returns the cached policy, building it from config if not yet initialised.
     * Safe to call from any server-thread context after world load.
     */
    public static StructureIntersectionChecker.StructurePolicy getPolicy() {
        if (cachedPolicy == null) {
            cachedPolicy = buildFromConfig();
        }
        return cachedPolicy;
    }

    /**
     * Invalidates the cached policy. The next call to {@link #getPolicy()} will
     * rebuild from the current config values.
     * Call this from the {@code ModConfigEvent.Reloading} handler.
     */
    public static void invalidate() {
        cachedPolicy = null;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static StructureIntersectionChecker.StructurePolicy buildFromConfig() {
        StructureIntersectionChecker.StructurePolicy.Builder builder =
                StructureIntersectionChecker.StructurePolicy.builder();

        List<? extends String> denyList =
                Config.SERVER.structureProtection.denyStructures.get();
        List<? extends String> warnList =
                Config.SERVER.structureProtection.warnStructures.get();

        for (String entry : denyList) {
            addEntry(builder, entry, false);
        }
        for (String entry : warnList) {
            addEntry(builder, entry, true);
        }

        return builder.build();
    }

    /**
     * Parses a single config entry and adds it to the builder as either a
     * deny or warn rule.
     *
     * <p>Entry formats:
     * <ul>
     *   <li>{@code #minecraft:eye_of_ender_located} — structure tag (prefix {@code #})</li>
     *   <li>{@code minecraft:fortress} — specific structure resource key</li>
     * </ul>
     *
     * <p>The displayName is the raw resource-location string (tag prefix stripped).
     * A human-readable lookup table may be added in a future version.
     */
    private static void addEntry(
            StructureIntersectionChecker.StructurePolicy.Builder builder,
            String entry,
            boolean warn) {

        if (entry == null || entry.isBlank()) {
            return;
        }

        if (entry.startsWith("#")) {
            String tagId = entry.substring(1);
            TagKey<Structure> tag = TagKey.create(
                    Registries.STRUCTURE, ResourceLocation.parse(tagId));
            if (warn) {
                builder.warn(tag, tagId);
            } else {
                builder.deny(tag, tagId);
            }
        } else {
            ResourceKey<Structure> key = ResourceKey.create(
                    Registries.STRUCTURE, ResourceLocation.parse(entry));
            if (warn) {
                builder.warn(key, entry);
            } else {
                builder.deny(key, entry);
            }
        }
    }
}