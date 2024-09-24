package mod.gottsch.forge.claimmyland.core.parcel;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Mark Gottschling on Sep 20, 2024
 */
public enum NationBorderType implements StringRepresentable {
    OPEN,
    CLOSED;

    @Override
    public String toString() {
        return this.name();
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name();
    }
}
