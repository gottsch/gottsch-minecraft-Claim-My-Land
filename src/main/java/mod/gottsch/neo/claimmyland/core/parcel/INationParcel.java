package mod.gottsch.neo.claimmyland.core.parcel;

import mod.gottsch.neo.claimmyland.core.estate.Estate;
import mod.gottsch.neo.claimmyland.core.estate.NationEstate;

import java.util.List;
import java.util.UUID;

/**
 * @author Mark Gottschling on Sep 14, 2024
 */
public interface INationParcel extends Parcel {
    List<UUID> getBlacklist();
    void setBlacklist(List<UUID> blacklist);

    /**
     * Convenience accessor mirroring {@link NationalizedParcel#getAccessType()}.
     * A NationParcel's own estate is a NationEstate, so this delegates directly.
     *
     * @author Mark Gottschling on Apr 12, 2026
     */
    default NationAccessType getAccessType() {
        Estate estate = getEstate();
        if (estate instanceof NationEstate nationEstate) {
            return nationEstate.getAccessType();
        }
        return NationAccessType.CLOSED;
    }
}
