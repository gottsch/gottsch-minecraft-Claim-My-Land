package mod.gottsch.neo.claimmyland.core.parcel;

/**
 * Result of a {@link Parcel#canPlaceAt(net.minecraft.world.level.Level,
 * mod.gottsch.neo.gottschcore.spatial.ICoords)} check. Position-only —
 * overlap and buffer-zone conflicts continue to be reported by claim-time
 * validation in {@code handleClaim()} / {@code handleEmbeddedClaim()}, not here.
 *
 * @author Mark Gottschling on Apr 12, 2026
 */
public enum PlacementResult {
    /** Position is legal for this parcel type. */
    SUCCESS,

    /** Position is outside the world border, build height, or otherwise unbuildable. */
    OUTSIDE_WORLD,

    /** Position is inside a Nation whose access type is CLOSED. */
    NATION_CLOSED,

    /** Player is on the enclosing Nation's blacklist. */
    NATION_BLACKLISTED,

    /**
     * Position is inside a parcel of an allowed ancestor type, but not inside
     * the specific parent type this deed requires. The canonical case: a
     * Citizen deed used inside a Nation but outside any Zone.
     */
    OUTSIDE_VALID_PARENT,

    /**
     * Position is inside a parcel whose type cannot host this deed at all
     * (e.g. a Player deed used inside any Nation, or any deed inside a
     * Citizen or Player parcel).
     */
    INVALID_PARENT_TYPE,

    /**
     * The enclosing parcel denied access for a reason not covered by a more
     * specific value above (e.g. a future access policy added to {@code
     * grantsAccess()} that {@code canPlaceAt} doesn't introspect). Generic
     * fallback only.
     */
    ACCESS_DENIED,

    /** Catch-all for failures that don't match any other reason. */
    UNKNOWN_FAILURE;

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
