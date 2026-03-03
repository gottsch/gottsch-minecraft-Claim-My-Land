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

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.parcel.NationAccessType;
import mod.gottsch.forge.claimmyland.core.parcel.NationParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * @author by Mark Gottschling on 2/9/2026
 */
public class NationEstateContext extends AbstractEstate implements NationEstate {

    private NationAccessType accessType;
    private Set<UUID> playerBlacklist;

    public NationEstateContext() {
        super();
        this.accessType = NationAccessType.CLOSED;
    }

    public NationEstateContext(UUID ownerUuid) {
        super(ownerUuid);
        this.accessType = NationAccessType.CLOSED;
    }

    @Override
    public ResourceLocation getType() {
        return EstateTypeRegistry.ESTATE_TYPE;
    }

    @Override
    public boolean canJoin(Estate estate) {
        if (!super.canJoin(estate)) {
            return false;
        }

        if (getId().equals(estate.getId()) || isRelinquished() || estate.isRelinquished()) {
            return false;
        }

        if (!getOwnerId().equals(estate.getOwnerId())) {
            return false;
        }

        Optional<Parcel> p1 = findParcels().stream().findFirst();
        if (p1.isEmpty()) {
            return false;
        }

        return estate.findParcels().stream()
                .findFirst()
                .map(p2 -> p1.get().getType() == p2.getType())
                .orElse(false);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        super.save(tag);

        tag.putString(ACCESS_TYPE_KEY, getAccessType().name());

        if (getPlayerBlacklist() != null) {
            ListTag list = new ListTag();
            getPlayerBlacklist().forEach(data -> {
                CompoundTag uuidTag = new CompoundTag();
                uuidTag.putUUID(ID_KEY, data);
                list.add(uuidTag);
            });
            tag.put(PLAYER_BLACKLIST_KEY, list);
        }
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        getPlayerBlacklist().clear();

        if (tag.contains(ACCESS_TYPE_KEY)) {
            setAccessType(NationAccessType.fromString(tag.getString(ACCESS_TYPE_KEY)));
        }

        // player black list
        if (tag.contains(PLAYER_BLACKLIST_KEY)) {
            ListTag list = tag.getList(PLAYER_BLACKLIST_KEY, Tag.TAG_COMPOUND);
            list.forEach(element -> {
                CompoundTag uuidTag = ((CompoundTag) element);
                if (uuidTag.contains(ID_KEY)) {
                    ClaimMyLand.LOGGER.debug("loading {} into player blacklist", uuidTag.getUUID(ID_KEY));
                    getPlayerBlacklist().add(uuidTag.getUUID(ID_KEY));
                }
            });
        }

        super.load(tag);
    }

    /**
     * finds all the citizen & zone parcels from all the nations parcels in the estate.
     */
    @Override
    public Set<Parcel> findTenantParcels() {
        // fetch all parcels by estate id
        return ParcelRegistry.findAllByNationEstateId(this.getId());
    }

    @Override
    public boolean isRelinquished() {
        return false;
    }

    @Override
    public boolean canRelinquish() {
        return false;
    }

    @Override
    public NationAccessType getAccessType() {
        return accessType;
    }

    @Override
    public void setAccessType(NationAccessType accessType) {
        this.accessType = accessType;
    }

    @Override
    public Set<UUID> getPlayerBlacklist() {
        return playerBlacklist == null ? playerBlacklist = new HashSet<>() : playerBlacklist;
    }

    public void setPlayerBlacklist(Set<UUID> playerBlacklist) {
        this.playerBlacklist = playerBlacklist;
    }
}
