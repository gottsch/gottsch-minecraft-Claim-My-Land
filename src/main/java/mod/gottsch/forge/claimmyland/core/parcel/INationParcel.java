package mod.gottsch.forge.claimmyland.core.parcel;

import java.util.List;
import java.util.UUID;

/**
 * @author Mark Gottschling on Sep 14, 2024
 */
public interface INationParcel extends Parcel {
    List<UUID> getBlacklist();
    void setBlacklist(List<UUID> blacklist);
}
