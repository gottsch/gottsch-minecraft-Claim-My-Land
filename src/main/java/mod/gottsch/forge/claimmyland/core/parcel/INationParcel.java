package mod.gottsch.forge.claimmyland.core.parcel;

import java.util.List;
import java.util.UUID;

public interface INationParcel extends Parcel {
     @Deprecated(forRemoval = true, since = "2.0")
    List<UUID> getBlacklist();
    @Deprecated(forRemoval = true, since = "2.0")
    void setBlacklist(List<UUID> blacklist);
}
