package mod.gottsch.forge.claimmyland.core.parcel;

import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.config.Config;

import java.util.UUID;

// TODO placeholder
public class CitizenZoneParcel extends AbstractParcel {
    private UUID nationId;

    @Override
    public boolean grantsAccess(Parcel parcel) {
        return false;
    }

    @Override
    public boolean hasAccessTo(Parcel otherParcel) {

        return true;
    }

    public boolean grantAccess(Parcel otherParcel) {

        // a personal deed cannot be used on an existing parcel
        if (otherParcel.getType() == ParcelType.PERSONAL) {
            // TODO check parent Nation is open
            // TODO check if owner id is null/empty
            return true;
        }

        return false;
    }

    @Override
    public boolean hasAccessTo(FoundationStoneBlockEntity blockEntity) {
        return false;
    }

    @Override
    public int getBufferSize() {
        return Config.SERVER.general.parcelBufferRadius.get();
    }

    public UUID getNationId() {
        return nationId;
    }

    public void setNationId(UUID nationId) {
        this.nationId = nationId;
    }
}
