package mod.gottsch.forge.claimmyland.core.block;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 *
 * @author Mark Gottschling on Sep 18, 2024
 *
 */
public enum BorderPosition implements StringRepresentable {
    TOP("top"),
    BOTTOM("bottom"),
    RIGHT("right"),
    LEFT("left"),
    TOP_RIGHT("top_right"),
    TOP_LEFT("top_left"),
    BOTTOM_RIGHT("bottom_right"),
    BOTTOM_LEFT("bottom_left");

    private final String name;

    BorderPosition(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }
};