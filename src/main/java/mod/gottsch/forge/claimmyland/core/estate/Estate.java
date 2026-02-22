package mod.gottsch.forge.claimmyland.core.estate;

import mod.gottsch.forge.claimmyland.core.parcel.NationalizedParcel;
import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Set;
import java.util.UUID;

/**
 * @author by Mark Gottschling on 1/28/2026
 */
public interface Estate {
    public static final String ID_KEY = "id";

    boolean isRelinquished();

    void setRelinquished(boolean relinquished);

    default boolean canRelinquish() {
        // only citizen parcels can be relinquished
        // NOTE should work as only like parcels can be joined.
        Parcel parcel = findParcels().iterator().next();
        return parcel.getType() == ParcelType.CITIZEN
                && !parcel.getEstate().isRelinquished();
    }

    default boolean canRelinquish(Parcel parcel) {

        return (parcel instanceof NationalizedParcel)
                && parcel.getType() == ParcelType.CITIZEN              // is it a citizen parcel
                && !parcel.getEstate().isRelinquished()                     // its not relinquished already
                && this.getId().equals(parcel.getEstate().getId());    // the estate == parcel's estate
    }

    ResourceLocation getType();
    void setType(ResourceLocation resourceLocation);

    ParcelType getParcelType();
    void setParcelType(ParcelType type);

    UUID getId();

    String defaultName();

    String defaultName(Player player);

    String defaultName(UUID ownerId);

    CompoundTag save(CompoundTag tag);

    void load(CompoundTag tag);

    void setId(UUID id);

    UUID getOwnerId();
    void setOwnerId(UUID ownerId);

    String getName();

    void setName(String name);

    Set<UUID> getPlayerWhitelist();

    void setPlayerWhitelist(Set<UUID> playerWhitelist);

    Set<String> getBlockWhitelist();

    void setBlockWhitelist(Set<String> blockWhitelist);

    Set<String> getBlockTagWhitelist();

    void setBlockTagWhitelist(Set<String> blockTagWhitelist);

    Set<String> getItemWhitelist();

    void setItemWhitelist(Set<String> itemWhitelist);

    Set<String> getItemTagWhitelist();

    void setItemTagWhitelist(Set<String> itemTagWhitelist);

    Set<Parcel> findParcels();

    boolean canJoin(Estate estate);

//    boolean isRelinquished();
//
//    boolean canRelinquish();

    Set<String> getEntitySpawnTagWhitelist();

    void setEntitySpawnTagWhitelist(Set<String> entitySpawnTagWhitelist);

    Set<String> getEntitySpawnWhitelist();

    void setEntitySpawnWhitelist(Set<String> entitySpawnWhitelist);

}
