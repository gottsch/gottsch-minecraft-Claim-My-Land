package mod.gottsch.forge.claimmyland.core.parcel;

import net.minecraft.util.StringRepresentable;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Mark Gottschling on Feb 9, 2026
 */
public enum NationAccessType implements StringRepresentable {
    OPEN,
    CLOSED;

    public static NationAccessType fromString(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return CLOSED;
        }
    }

    @Override
    public String toString() {
        return StringUtils.capitalize(this.name().toLowerCase());
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name();
    }
}
