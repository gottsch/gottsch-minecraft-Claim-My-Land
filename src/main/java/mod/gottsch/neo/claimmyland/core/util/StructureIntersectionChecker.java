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

import it.unimi.dsi.fastutil.longs.LongSet;
import mod.gottsch.neo.gottschcore.spatial.ICoords;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Checks whether a proposed parcel overlaps any Minecraft structure
 * that other players need to complete the game (or that should remain shared).
 *
 * ── HOW MINECRAFT STRUCTURE DATA WORKS ────────────────────────────────────
 *
 * Structures in 1.20 are stored per-chunk once a chunk reaches STRUCTURE_STARTS
 * generation status. The StructureManager (accessed via ServerLevel) can answer:
 *
 *   hasAnyStructureAt(pos)                  – any structure at all here?
 *   getStructureWithPieceAt(pos, TagKey)    – any piece of a tagged structure category?
 *   getStructureAt(pos, Structure)          – is a specific structure's start here?
 *   getAllStructuresAt(pos)                 – map of all structure starts at pos
 *
 * "Piece" matters: a Stronghold start chunk radius can be ~20 blocks wide, but
 * the actual rooms and portal chamber are tracked as individual bounding-box
 * pieces. getStructureWithPieceAt checks pieces, giving precise footprints.
 *
 * ── HOW WE WALK THE REGION ────────────────────────────────────────────────
 *
 * We sample the NW corner of every chunk the proposed parcel overlaps.
 * Because structures are indexed at chunk granularity, one sample per chunk is
 * sufficient. A 100x100 parcel is at most 7x7 = 49 map lookups — negligible
 * for a one-time claim-creation check.
 *
 * ── IMPORTANT LIMITATION ──────────────────────────────────────────────────
 *
 * StructureManager only knows about chunks that have been generated to at least
 * STRUCTURE_STARTS status. Completely unvisited chunks will return nothing. In
 * practice this is fine — players explore an area before claiming it — but
 * document it so the behaviour isn't surprising.
 *
 * ── TAG vs RESOURCEKEY ────────────────────────────────────────────────────
 *
 * Some structures have StructureTags constants (Stronghold, Village, Mansion,
 * Monument). Others (Nether Fortress, End City, Bastion) do not, so we look
 * them up via ResourceKey and resolve against the level's registry.
 *
 * @author Mark Gottschling on <date>
 */
public class StructureIntersectionChecker {

    // =========================================================================
    // ResourceKeys for structures that have no StructureTags constant
    // These IDs are stable across all 1.20.x releases
    // =========================================================================

    public static final ResourceKey<Structure> FORTRESS =
            resourceKey("fortress");
    public static final ResourceKey<Structure> END_CITY =
            resourceKey("end_city");
    public static final ResourceKey<Structure> BASTION_REMNANT =
            resourceKey("bastion_remnant");
    public static final ResourceKey<Structure> MONUMENT =
            resourceKey("monument");

    private static ResourceKey<Structure> resourceKey(String path) {
        return ResourceKey.create(Registries.STRUCTURE,
                ResourceLocation.withDefaultNamespace(path));
    }

    // =========================================================================
    // Built-in reference policies
    // These are retained as reference constants. The active policy used at
    // claim time is built from config via StructurePolicyFactory.
    // =========================================================================

    /**
     * HARD DENY — structures critical to game completion or globally unique.
     */
    public static final StructurePolicy HARD_DENY_POLICY = StructurePolicy.builder()
            .deny(StructureTags.EYE_OF_ENDER_LOCATED,  "Stronghold (End Portal)")
            .deny(FORTRESS,                              "Nether Fortress")
            .deny(BASTION_REMNANT,                       "Bastion Remnant")
            .deny(END_CITY,                              "End City")
            .build();

    /**
     * WARN — important shared structures. Claiming over them is allowed but
     * the player is informed and the event is logged for admins.
     */
    public static final StructurePolicy WARN_POLICY = StructurePolicy.builder()
            .warn(StructureTags.VILLAGE,                   "Village")
            .warn(StructureTags.ON_WOODLAND_EXPLORER_MAPS, "Woodland Mansion")
            .warn(StructureTags.ON_OCEAN_EXPLORER_MAPS,    "Ocean Monument")
            .build();

    /**
     * Opinionated default combining both. Use this if you don't want to
     * configure your own.
     */
    public static final StructurePolicy DEFAULT_POLICY = StructurePolicy.builder()
            .addAll(HARD_DENY_POLICY)
            .addAll(WARN_POLICY)
            .build();

    // private constructor — static utility class
    private StructureIntersectionChecker() {}

    // =========================================================================
    // Main API
    // =========================================================================

    /**
     * Checks whether a proposed parcel overlaps any structures defined by
     * the policy.
     *
     * Call this only at claim-creation time — never on every block event.
     * It performs O(chunks_in_parcel) StructureManager hash lookups, which is
     * fast for a one-time check but not suitable as a hot-path guard.
     *
     * @param level     the ServerLevel the parcel will live in
     * @param minCoords the minimum corner of the proposed parcel bounds
     * @param maxCoords the maximum corner of the proposed parcel bounds
     * @param policy    which structures to check at which severity level
     * @return a CheckResult describing any structures found
     */
    public static CheckResult check(ServerLevel level,
                                    ICoords minCoords, ICoords maxCoords,
                                    StructurePolicy policy) {
        StructureManager mgr = level.structureManager();

        List<String> denied = new ArrayList<>();
        List<String> warned = new ArrayList<>();

        int minCX = minCoords.getX() >> 4;
        int maxCX = maxCoords.getX() >> 4;
        int minCZ = minCoords.getZ() >> 4;
        int maxCZ = maxCoords.getZ() >> 4;

        // Walk every chunk the parcel overlaps, sampling the NW corner of each.
        // Y=64 is arbitrary — structure lookups key on XZ chunk position, not Y.
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
//                BlockPos sample = new BlockPos(cx << 4, 64, cz << 4);
                int sampleX = Math.max(minCoords.getX(), cx << 4) + 8;
                int sampleZ = Math.max(minCoords.getZ(), cz << 4) + 8;
                BlockPos sample = new BlockPos(sampleX, 64, sampleZ);

                for (StructurePolicy.Entry entry : policy.denyEntries) {
                    if (!denied.contains(entry.displayName)
                            && matchesAt(mgr, level, sample, entry)) {
                        denied.add(entry.displayName);
                    }
                }

                for (StructurePolicy.Entry entry : policy.warnEntries) {
                    if (!warned.contains(entry.displayName)
                            && matchesAt(mgr, level, sample, entry)) {
                        warned.add(entry.displayName);
                    }
                }
            }
        }

        return new CheckResult(denied, warned);
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /**
     * Returns true if the given entry's structure has a piece at this block
     * position.
     *
     * For TagKey entries we use getStructureWithPieceAt, which checks actual
     * piece bounding boxes (rooms, corridors, etc.) for a precise footprint.
     *
     * For ResourceKey entries we resolve the Structure object from the level's
     * registry then call getStructureAt. This checks structure starts at chunk
     * resolution, which is slightly coarser but is the correct API when we
     * have no TagKey.
     */
    private static boolean matchesAt(StructureManager mgr, ServerLevel level,
                                     BlockPos pos, StructurePolicy.Entry entry) {

        Map<Structure, LongSet> all = mgr.getAllStructuresAt(pos);
        if (all.isEmpty()) return false;

        if (entry.tagKey != null) {
            // check if any found structure is a member of the tag
            return all.keySet().stream().anyMatch(structure ->
                    level.registryAccess()
                            .registryOrThrow(Registries.STRUCTURE)
                            .getHolderOrThrow(
                                    level.registryAccess()
                                            .registryOrThrow(Registries.STRUCTURE)
                                            .getResourceKey(structure).orElseThrow()
                            )
                            .is(entry.tagKey)
            );
        } else {
            // check if any found structure matches the resource key
            return all.keySet().stream().anyMatch(structure ->
                    level.registryAccess()
                            .registryOrThrow(Registries.STRUCTURE)
                            .getResourceKey(structure)
                            .map(key -> key.equals(entry.resourceKey))
                            .orElse(false)
            );
        }
    }

    // =========================================================================
    // CheckResult
    // =========================================================================

    /**
     * The outcome of a structure intersection check.
     */
    public static class CheckResult {

        /** Names of deny-level structures found. Non-empty → claim must be rejected. */
        public final List<String> deniedBy;

        /** Names of warn-level structures found. Non-empty → inform player and log. */
        public final List<String> warnedBy;

        CheckResult(List<String> deniedBy, List<String> warnedBy) {
            this.deniedBy = List.copyOf(deniedBy);
            this.warnedBy = List.copyOf(warnedBy);
        }

        public boolean isDenied()    { return !deniedBy.isEmpty(); }
        public boolean hasWarnings() { return !warnedBy.isEmpty(); }
        public boolean isClean()     { return deniedBy.isEmpty() && warnedBy.isEmpty(); }
    }

    // =========================================================================
    // StructurePolicy
    // =========================================================================

    /**
     * Defines which structures to check at which severity level.
     * Construct with {@link StructurePolicy#builder()}.
     */
    public static class StructurePolicy {

        final List<Entry> denyEntries;
        final List<Entry> warnEntries;

        private StructurePolicy(List<Entry> deny, List<Entry> warn) {
            this.denyEntries = List.copyOf(deny);
            this.warnEntries = List.copyOf(warn);
        }

        public static Builder builder() { return new Builder(); }

        static class Entry {
            final TagKey<Structure>      tagKey;
            final ResourceKey<Structure> resourceKey;
            final String                 displayName;

            Entry(TagKey<Structure> tagKey, String displayName) {
                this.tagKey      = tagKey;
                this.resourceKey = null;
                this.displayName = displayName;
            }

            Entry(ResourceKey<Structure> resourceKey, String displayName) {
                this.tagKey      = null;
                this.resourceKey = resourceKey;
                this.displayName = displayName;
            }
        }

        public static class Builder {
            private final List<Entry> denyEntries = new ArrayList<>();
            private final List<Entry> warnEntries = new ArrayList<>();

            /** Deny claims overlapping any structure matched by this tag. */
            public Builder deny(TagKey<Structure> tag, String displayName) {
                denyEntries.add(new Entry(tag, displayName));
                return this;
            }

            /** Deny claims overlapping this specific structure (by registry key). */
            public Builder deny(ResourceKey<Structure> key, String displayName) {
                denyEntries.add(new Entry(key, displayName));
                return this;
            }

            /** Warn (but allow) claims overlapping any structure matched by this tag. */
            public Builder warn(TagKey<Structure> tag, String displayName) {
                warnEntries.add(new Entry(tag, displayName));
                return this;
            }

            /** Warn (but allow) claims overlapping this specific structure. */
            public Builder warn(ResourceKey<Structure> key, String displayName) {
                warnEntries.add(new Entry(key, displayName));
                return this;
            }

            /** Copy all entries from another policy (for composing policies). */
            public Builder addAll(StructurePolicy other) {
                denyEntries.addAll(other.denyEntries);
                warnEntries.addAll(other.warnEntries);
                return this;
            }

            public StructurePolicy build() {
                return new StructurePolicy(denyEntries, warnEntries);
            }
        }
    }
}