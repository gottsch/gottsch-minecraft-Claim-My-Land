package mod.gottsch.forge.claimmyland.core.estate;

import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
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

    ResourceLocation getType();

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

    Set<Parcel> getParcels();
}
