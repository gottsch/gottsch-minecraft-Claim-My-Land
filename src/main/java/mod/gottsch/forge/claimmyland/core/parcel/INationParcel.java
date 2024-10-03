package mod.gottsch.forge.claimmyland.core.parcel;

import java.util.List;
import java.util.UUID;

public interface INationParcel extends Parcel {
    NationBorderType getBorderType();

    void setBorderType(NationBorderType borderType);

    List<UUID> getBlacklist();

    void setBlacklist(List<UUID> blacklist);
}
