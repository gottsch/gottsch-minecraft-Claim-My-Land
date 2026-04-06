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

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.estate.Estate;
import mod.gottsch.forge.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.forge.claimmyland.core.estate.EstateHelper;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.claimmyland.core.util.StructureIntersectionChecker;
import mod.gottsch.forge.claimmyland.core.util.StructurePolicyFactory;
import mod.gottsch.forge.gottschcore.spatial.Box;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 *
 * @author Mark Gottschling on Mar 16, 2024
 *
 */
public interface Parcel {
    public static final String PARCEL_ID = "parcel_id";
    public static final String DEED_ID = "deed_id";
    public static final String OWNER_ID = "owner_id";
    public static final String PARCEL_TYPE = "parcel_type";
    public static final String SIZE = "size";

    public static final String TYPE = "type";

    public static Comparator<Parcel> volumeComparator = new Comparator<Parcel>() {
        @Override
        public int compare(Parcel p1, Parcel p2) {
                // use p1 < p2 because the sort should be ascending
                if (ModUtil.getVolume(p1.getBox()) < ModUtil.getVolume(p2.getBox())) {
                    // greater than
                    return 1;
                } else {
                    // less than
                    return -1;
                }
            }
    };

    ParcelType getType();
    void setType(ParcelType type);
    default boolean isPlayer() { return getType() == ParcelType.PLAYER; }
    default boolean isCitizen() { return getType() == ParcelType.CITIZEN; }
    default boolean isZone() { return getType() == ParcelType.ZONE; }
    default boolean isNation() { return getType() == ParcelType.NATION; }

    void save(CompoundTag parcelTag);
    Parcel load(CompoundTag tag);

    /**
     *
     * @return
     */
    default public String randomName() {
        String name;
        int size = ParcelRegistry.size() + 1;

        int iterations = 0;
        boolean nameNotFound = false;
        do {
            name = "Parcel" + String.valueOf(size);
            // check the registry
            if (!ParcelRegistry.hasName(this, name)) nameNotFound = true;
        } while (iterations++ < 3 && !nameNotFound);

        if (!nameNotFound) {
            name = StringUtils.capitalize(RandomStringUtils.random(8, true, false));
        }
        return name;
    }

    String defaultName(Player player);

    String defaultName(UUID ownerId);

    String defaultName(ServerLevel level, UUID ownerId);

    /**
     * determine if this parcel grants access to the given parcel
     * this is called when attempting to place a foundation stone,
     * ie using a deed on a non-foundation stone block.
     * @param parcel
     * @return
     */
    boolean grantsAccess(Parcel parcel);

    /**
     * determine if the this parcel has access to the given parcel.
     * this is called when attempting to place a foundation stone,
     * @param parcel
     * @return
     */
    boolean hasAccessTo(Parcel parcel);

    /**
     * this is called when attempting to claim a parcel represented by the foundation stone.
     * ie. using a deed on a foundation stone block.
     * @param blockEntity
     * @return
     */
    boolean hasAccessTo(FoundationStoneBlockEntity blockEntity);

    boolean isOwner(UUID id);

    /**
     * does the entity have access to the parcel
     * @param entityId
     * @return
     */
    boolean grantsAccess(UUID entityId);

    /**
     * does the entity and itemStack have access to the parcel
     * @param entityId
     * @param stack
     * @return
     */
    boolean grantsAccess(UUID entityId, ItemStack stack);

     /**
     * determines whether this deed can place a Foundation stone in the world.
     * @param level
     * @param coords
     * @return
     */
    default boolean canPlaceAt(Level level, ICoords coords) {
        /*
         * check if parcel is within another existing parcel
         */
        String dimension = level.dimension().location().toString();
        Optional<Parcel> registryParcel = ParcelRegistry.findLeastSignificant(coords, dimension);
        return registryParcel.map(this::handleEmbeddedPlacementRules).orElseGet(this::handlePlacementRules);
    }

    /**
     * handles situations where this deed DOES NOT overlap with any other parcels
     * @return
     */
    default public boolean handlePlacementRules() {
        return true;
    }

    /**
     * handles situations where this does DOES overlap with other parcels.
     * ie check whitelist, nation permissions etc
     * @return
     */
    default public boolean handleEmbeddedPlacementRules(Parcel registryParcel) {
        return hasAccessTo(registryParcel) && registryParcel.grantsAccess(this);
    }

    // TODO have 2 variants of this, one that takes the parcelBox (static) and one that doesn't
    public ClaimResult handleEmbeddedClaim(Level level, Parcel parentParcel); //, Box parcelBox);

    /**
     * Default behaviour to claim a parcel in the open world (no enclosing parent).
     *
     * Rule 1  — Direct box overlap:
     *   1a. If the overlapping parcel is a permitted ancestor/descendant, verify
     *       full containment. Failing containment → NOT_IN_PARENT.
     *   1b. Same-owner same-type siblings may touch but not overlap → INTERSECTS.
     *   1c. All other overlaps (foreign owner, same-owner cross-type) → INTERSECTS.
     *
     * Rule 2  — Bidirectional buffer conflict (same-owner same-type siblings skip this):
     *   2a. Existing parcel's buffer zone reaches into this parcel's box.
     *   2b. This parcel's own buffer zone reaches into an existing parcel's box.
     */
    default public ClaimResult handleClaim(Level level) {
        String dimension = ((ServerLevel) level).dimension().location().toString();

        // ── Rule 1: direct box-to-box overlap ────────────────────────────────
        List<Parcel> directOverlaps = ParcelRegistry.find(getBox(), dimension);
        for (Parcel existing : directOverlaps) {

            // a parcel must never match itself (e.g. during a re-registration)
            if (getId().equals(existing.getId())) {
                return ClaimResult.FAILURE;
            }

            boolean hierarchical = ParcelType.isAllowedAncestor(existing.getType(), getType())
                    || ParcelType.isAllowedDescendant(existing.getType(), getType());

//            if (hierarchical) {
//                // Rule 1a: hierarchical overlap is only valid when fully contained
//                if (!ModUtil.contains(existing.getBox(), getBox())) {
//                    return ClaimResult.NOT_IN_PARENT;
//                }
//                // fully contained within a permitted parent/child — not a conflict
//                continue;
//            }
            if (hierarchical) {
                boolean ancestorContainsPlacing =
                        ParcelType.isAllowedAncestor(existing.getType(), getType())
                                && ModUtil.contains(existing.getBox(), getBox());
                boolean placingContainsDescendant =
                        ParcelType.isAllowedDescendant(existing.getType(), getType())
                                && ModUtil.contains(getBox(), existing.getBox());
                if (!ancestorContainsPlacing && !placingContainsDescendant) {
                    return ClaimResult.NOT_IN_PARENT;
                }
                continue;
            }

            // Rule 1b/1c: delegate ownership/type decision to the resolver
            if (ParcelConflictResolver.isConflict(getType(), existing.getType(),
                    getOwnerId(), existing.getOwnerId())) {
                return ClaimResult.INTERSECTS;
            }

            // Rule 1b: same-owner same-type sibling — touching is fine, overlap is not
            // (isConflict returned false, so we know it is a sibling at this point)
            if (ModUtil.overlaps(getBox(), existing.getBox())) {
                return ClaimResult.INTERSECTS;
            }
        }

        // ── Rule 2: bidirectional buffer conflict ─────────────────────────────
        // Rule 2a: existing parcels whose buffer zones reach into this parcel's box
        List<Parcel> bufferOverlaps = ParcelRegistry.findBuffer(getBox(), dimension).stream()
                .filter(p -> !getId().equals(p.getId()))
                .toList();

        // Rule 2b: this parcel's own buffer zone reaches into existing parcel boxes
        int bufferSize = getBufferSize();
        List<Parcel> inflatedOverlaps = bufferSize > 0
                ? ParcelRegistry.find(ModUtil.inflate(getBox(), bufferSize), dimension).stream()
                  .filter(p -> !getId().equals(p.getId()))
                  .toList()
                : List.of();

        // Same-owner same-type siblings are exempt from buffer checks (tiling is allowed).
        // All other combinations use the resolver.
        boolean bufferConflict =
                bufferOverlaps.stream().anyMatch(p ->
                        ParcelConflictResolver.isConflict(getType(), p.getType(),
                                getOwnerId(), p.getOwnerId()))
                        || inflatedOverlaps.stream().anyMatch(p ->
                        ParcelConflictResolver.isConflict(getType(), p.getType(),
                                getOwnerId(), p.getOwnerId()));

        if (bufferConflict) {
            return ClaimResult.INTERSECTS;
        }

        return nameAndRegister(level);
    }


    /**
     * Validates the proposed parcel against structure intersection rules.
     *
     * @param level the ServerLevel the parcel will live in
     * @return SUCCESS, SUCCESS_WITH_WARNINGS, or STRUCTURE_DENIED
     */
    default ClaimResult validateClaim(ServerLevel level) {
        if (!Config.SERVER.structureProtection.enabled.get()) {
            return ClaimResult.SUCCESS;
        }
        StructureIntersectionChecker.StructurePolicy policy =
                StructurePolicyFactory.getPolicy();
        StructureIntersectionChecker.CheckResult result =
                StructureIntersectionChecker.check(
                        level,
                        getBox().getMinCoords(),
                        getBox().getMaxCoords(),
                        policy);
        if (result.isDenied()) {
            return ClaimResult.STRUCTURE_DENIED;
        }
        if (result.hasWarnings()) {
            return ClaimResult.SUCCESS_WITH_WARNINGS;
        }
        return ClaimResult.SUCCESS;
    }

    default ClaimResult nameAndRegister(Level level) {
        ClaimResult validation = validateClaim((ServerLevel) level);
        if (validation == ClaimResult.STRUCTURE_DENIED) {
            return validation;
        }
        // set dimension before registration so registerChunk() can read it
        setDimension(((ServerLevel) level).dimension().location().toString());
        getEstate().setName(EstateHelper.buildName((ServerLevel) level, getOwnerId()));
        setName(ParcelHelper.buildName((ServerLevel) level, getEstate()));
        ParcelRegistry.register((ServerLevel) level, this);
        CommandHelper.save(level);
        return validation;  // SUCCESS or SUCCESS_WITH_WARNINGS
    }

    default ClaimResult nameAndRegister(Level level, String playerName) {
        ClaimResult validation = validateClaim((ServerLevel) level);
        if (validation == ClaimResult.STRUCTURE_DENIED) {
            return validation;
        }
        // set dimension before registration so registerChunk() can read it
        setDimension(((ServerLevel) level).dimension().location().toString());
        getEstate().setName(EstateHelper.buildName((ServerLevel) level, getOwnerId()));
        setName(ParcelHelper.buildName((ServerLevel) level, getEstate()));
        ParcelRegistry.register((ServerLevel) level, this, playerName);
        CommandHelper.save(level);
        return validation;  // SUCCESS or SUCCESS_WITH_WARNINGS
    }

    boolean isValidClaim(Estate estate);

    boolean isValid();

    Box getAbsoluteBox();

    Box getBox();

    ICoords getMinCoords();
    ICoords getMaxCoords();

    UUID getId();

    void setId(UUID id);

    Estate getEstate();
    void setEstate(Estate estate);

    UUID getOwnerId();

    void setOwnerId(UUID ownerId);

    UUID getDeedId();

    void setDeedId(UUID deedId);

    String getName();

    void setName(String name);

    ICoords getCoords();

    void setCoords(ICoords coords);

    Box getSize();

    void setSize(Box size);

    int getArea();

    int getBufferSize();

    /*
     * convenience method
     */
    Set<UUID> getPlayerWhitelist();
    void setPlayerWhitelist(Set<UUID> whitelist);

    String getDimension();
    void setDimension(String dimension);
}
