package mod.gottsch.forge.claimmyland.core.parcel;

public enum ClaimResult {
    SUCCESS,
    INTERSECTS,
    INSUFFICIENT_SIZE,
    FAILURE;

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
