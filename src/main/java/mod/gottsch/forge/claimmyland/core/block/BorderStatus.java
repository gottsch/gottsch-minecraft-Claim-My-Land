package mod.gottsch.forge.claimmyland.core.block;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 *
 * @author Mark Gottschling on Sep 16, 2024
 *
 */
public enum BorderStatus implements StringRepresentable {
    GOOD("good"),
    BAD("bad");

    private final String name;

    BorderStatus(String name) {
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