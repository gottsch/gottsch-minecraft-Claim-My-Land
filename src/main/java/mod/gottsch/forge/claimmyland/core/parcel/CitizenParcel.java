package mod.gottsch.forge.claimmyland.core.parcel;

import mod.gottsch.forge.claimmyland.core.block.entity.FoundationStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.config.Config;

import java.util.UUID;

// TODO placeholder
public class CitizenParcel extends AbstractParcel {
    private UUID parentId;
    private UUID nationId;

    @Override
    public boolean grantsAccess(Parcel otherParcel) {

        if (otherParcel.getType() == ParcelType.PLAYER) {
            // TODO check parent Nation is open
            // TODO check if owner id is null/empty
            return true;
        }
        return false;
    }

    @Override
    public boolean hasAccessTo(Parcel otherParcel) {
        return true;
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
