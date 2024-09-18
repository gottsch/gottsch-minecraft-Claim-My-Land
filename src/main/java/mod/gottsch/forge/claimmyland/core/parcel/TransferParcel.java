package mod.gottsch.forge.claimmyland.core.parcel;

import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.config.Config;

import java.util.UUID;

// TODO placeholder
public class TransferParcel extends AbstractParcel {
    public static final String NATION_ID_KEY = "nation_id";
    private static final String NATION_NAME_KEY = "nation_name";

    private UUID nationId;
    private String nationName;

    @Override
    public boolean grantsAccess(Parcel parcel) {
        return false;
    }

    @Override
    public boolean hasAccessTo(Parcel parcel) {
        return false;
    }

    @Override
    public boolean hasAccessTo(FoundationStoneBlockEntity blockEntity) {
        return false;
    }

    @Override
    public int getBufferSize() {
        return Config.SERVER.general.nationParcelBufferRadius.get();
    }

    public UUID getNationId() {
        return nationId;
    }

    public void setNationId(UUID nationId) {
        this.nationId = nationId;
    }

    public String getNationName() {
        return nationName;
    }

    public void setNationName(String nationName) {
        this.nationName = nationName;
    }
}
