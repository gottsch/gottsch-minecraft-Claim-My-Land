package mod.gottsch.forge.claimmyland.core.estate;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.registry.EstateRegistry;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * a Claim is the ownership token and access provider of a Parcel(s).
 *
 * @author by Mark Gottschling on 1/28/2026
 */
public abstract class AbstractEstate implements Estate {

//    public static final String ID_KEY = "id";
    public static final String NAME_KEY = "name";
    public static final String OWNER_KEY = "owner";

    public static final String PLAYER_WHITELIST_KEY = "whitelist";
    public static final String BLOCK_TAG_WHITELIST_KEY = "blockTagWhitelist";
    public static final String BLOCK_WHITELIST_KEY = "blockWhitelist";
    public static final String ITEM_TAG_WHITELIST_KEY = "itemTagWhitelist";
    public static final String ITEM_WHITELIST_KEY = "itemWhitelist";

    // the unique id of the claim
    private UUID id;
    private String name;

    private UUID ownerId;
    private Set<UUID> playerWhitelist;
    private Set<String> blockWhitelist;
    private Set<String> blockTagWhitelist;
    private Set<String> itemWhitelist;
    private Set<String> itemTagWhitelist;

    public AbstractEstate() {
        setId(UUID.randomUUID());
        setName(defaultName());
    }

    public AbstractEstate(Player player) {
        setId(UUID.randomUUID());
        setOwnerId(player.getUUID());
        setName(defaultName(player));
    }

    public AbstractEstate(UUID ownerId) {
        setId(UUID.randomUUID());
        setOwnerId(ownerId);
        setName(defaultName(ownerId));
    }

    public AbstractEstate(UUID ownerId, String estateName) {
        setId(UUID.randomUUID());
        setOwnerId(ownerId);
        setName(estateName);
    }

    @Override
    public String defaultName() {
        return getId().toString() + "-estate-1";
    }

    @Override
    public String defaultName(Player player) {
        // checks against the ClaimRegistry for any claims by ownerID
        Set<Estate> estates = EstateRegistry.getByOwner(player.getUUID());
        return player.getScoreboardName() + "-estate-" + (estates.size() + 1);
    }

    @Override
    public String defaultName(UUID ownerId) {
        // TODO get the player name by ID from web

        if(ObjectUtils.isNotEmpty(ownerId)) {
            Set<Estate> estates = EstateRegistry.getByOwner(ownerId);
            return ownerId.toString() + "-estate-" + (estates.size() + 1);
        }
        else {
            return defaultName();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        // required field
        if (ObjectUtils.isEmpty(getId())) {
            return tag;
        }
        tag.putUUID(ID_KEY, getId());

        // required field
        if (ObjectUtils.isEmpty(getOwnerId())) {
            return tag;
        }
        tag.putUUID(OWNER_KEY, getOwnerId());

        if (StringUtils.isNotBlank(getName())) {
            tag.putString(NAME_KEY, getName());
        } else {
            // generate and save a default name
            tag.putString(NAME_KEY, defaultName(getOwnerId()));
        }

        if (getPlayerWhitelist() != null) {
            ListTag list = new ListTag();
            getPlayerWhitelist().forEach(data -> {
                CompoundTag uuidTag = new CompoundTag();
                uuidTag.putUUID(ID_KEY, data);
                list.add(uuidTag);
            });
            tag.put(PLAYER_WHITELIST_KEY, list);
        }

        if (getBlockTagWhitelist() != null) {
            ListTag list = new ListTag();
            getBlockTagWhitelist().forEach(data -> {
                StringTag elementTag = StringTag.valueOf(data);
                list.add(elementTag);
            });
            tag.put(BLOCK_TAG_WHITELIST_KEY, list);
        }

        if (getBlockWhitelist() != null) {
            ListTag list = new ListTag();
            getBlockWhitelist().forEach(data -> {
                StringTag elementTag = StringTag.valueOf(data);
                list.add(elementTag);
            });
            tag.put(BLOCK_WHITELIST_KEY, list);
        }

        if (getItemTagWhitelist() != null) {
            ListTag list = new ListTag();
            getItemTagWhitelist().forEach(data -> {
                StringTag elementTag = StringTag.valueOf(data);
                list.add(elementTag);
            });
            tag.put(ITEM_TAG_WHITELIST_KEY, list);
        }

        if (getItemWhitelist() != null) {
            ListTag list = new ListTag();
            getItemWhitelist().forEach(data -> {
                StringTag elementTag = StringTag.valueOf(data);
                list.add(elementTag);
            });
            tag.put(ITEM_WHITELIST_KEY, list);
        }

        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        // clear the white lists
        getPlayerWhitelist().clear();
        getBlockTagWhitelist().clear();
        getBlockWhitelist().clear();
        getItemTagWhitelist().clear();
        getItemWhitelist().clear();

        if (tag.contains(ID_KEY)) {
            setId(tag.getUUID(ID_KEY));
        } else if (this.getId() == null) {
            setId(UUID.randomUUID());
        }

        if (tag.contains(OWNER_KEY)) {
            setOwnerId(tag.getUUID(OWNER_KEY));
        }

        if (tag.contains(NAME_KEY)) {
            setName(tag.getString(NAME_KEY));
        } else {
           setName(defaultName(getOwnerId()));
        }

        // player white list
        if (tag.contains(PLAYER_WHITELIST_KEY)) {
            ListTag list = tag.getList(PLAYER_WHITELIST_KEY, Tag.TAG_COMPOUND);
            list.forEach(element -> {
                CompoundTag uuidTag = ((CompoundTag) element);
                if (uuidTag.contains(ID_KEY)) {
                    ClaimMyLand.LOGGER.debug("loading {} into player whitelist", uuidTag.getUUID(ID_KEY));
                    getPlayerWhitelist().add(uuidTag.getUUID(ID_KEY));
                }
            });
        }

        if (tag.contains(BLOCK_TAG_WHITELIST_KEY)) {
            ListTag list = tag.getList(BLOCK_TAG_WHITELIST_KEY, Tag.TAG_STRING);
            list.forEach(element -> {
                String blockTag = element.getAsString();
                getBlockTagWhitelist().add(blockTag);
            });
        }

        if (tag.contains(BLOCK_WHITELIST_KEY)) {
            ListTag list = tag.getList(BLOCK_WHITELIST_KEY, Tag.TAG_STRING);
            list.forEach(element -> {
                String block = element.getAsString();
                getBlockWhitelist().add(block);
            });
        }

        if (tag.contains(ITEM_TAG_WHITELIST_KEY)) {
            ListTag list = tag.getList(ITEM_TAG_WHITELIST_KEY, Tag.TAG_STRING);
            list.forEach(element -> {
                String itemTag = element.getAsString();
                getItemTagWhitelist().add(itemTag);
            });
        }

        if (tag.contains(ITEM_WHITELIST_KEY)) {
            ListTag list = tag.getList(ITEM_WHITELIST_KEY, Tag.TAG_STRING);
            list.forEach(element -> {
                String item = element.getAsString();
                getItemWhitelist().add(item);
            });
        }
    }

    /*
     * convenience method
     */
    @Override
    public Set<Parcel> getParcels() {
        // fetch all parcels by estate id
        return ParcelRegistry.findAllByEstateId(this.getId());
    }


    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }
    @Override
    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Set<UUID> getPlayerWhitelist() {
        return playerWhitelist == null ? playerWhitelist = new HashSet<>() : playerWhitelist;
    }
    @Override
    public void setPlayerWhitelist(Set<UUID> playerWhitelist) {
        this.playerWhitelist = playerWhitelist;
    }
    @Override
    public Set<String> getBlockWhitelist() {
        return blockWhitelist == null ? blockWhitelist = new HashSet<>() : blockWhitelist;
    }
    @Override
    public void setBlockWhitelist(Set<String> blockWhitelist) {
        this.blockWhitelist = blockWhitelist;
    }
    @Override
    public Set<String> getBlockTagWhitelist() {
        return blockTagWhitelist == null ? blockTagWhitelist = new HashSet<>() : blockTagWhitelist;
    }
    @Override
    public void setBlockTagWhitelist(Set<String> blockTagWhitelist) {
        this.blockTagWhitelist = blockTagWhitelist;
    }
    @Override
    public Set<String> getItemWhitelist() {
        return itemWhitelist == null ? itemWhitelist = new HashSet<>() : itemWhitelist;
    }
    @Override
    public void setItemWhitelist(Set<String> itemWhitelist) {
        this.itemWhitelist = itemWhitelist;
    }
    @Override
    public Set<String> getItemTagWhitelist() {
        return itemTagWhitelist == null ? itemTagWhitelist = new HashSet<>() : itemTagWhitelist;
    }
    @Override
    public void setItemTagWhitelist(Set<String> itemTagWhitelist) {
        this.itemTagWhitelist = itemTagWhitelist;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AbstractEstate that = (AbstractEstate) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "AbstractEstate{" +
                "blockTagWhitelist=" + blockTagWhitelist +
                ", id=" + id +
                ", name='" + name + '\'' +
                ", ownerId=" + ownerId +
                ", playerWhitelist=" + playerWhitelist +
                ", blockWhitelist=" + blockWhitelist +
                ", itemWhitelist=" + itemWhitelist +
                ", itemTagWhitelist=" + itemTagWhitelist +
                '}';
    }


}
