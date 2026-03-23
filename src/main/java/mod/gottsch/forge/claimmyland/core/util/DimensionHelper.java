package mod.gottsch.forge.claimmyland.core.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Shared utility for resolving dimension resource keys from string identifiers.
 *
 * <p>Used by {@code ParcelPolygonOverlayFactory}, {@code SyncParcelPacket},
 * and any other client-side system that needs to convert a dimension string
 * (e.g. {@code "minecraft:overworld"}) to a {@code ResourceKey<Level>}.</p>
 *
 * @author Mark Gottschling on March 20, 2026
 */
public class DimensionHelper {

    private DimensionHelper() {}

    /**
     * Converts a dimension string to a {@code ResourceKey<Level>}.
     *
     * <p>Handles the three vanilla dimensions directly for efficiency.
     * Falls back to constructing a key from the resource location for modded dimensions.
     * Returns {@code null} if the string is blank or unparseable — callers should
     * skip overlay creation when null is returned.</p>
     *
     * @param dimension dimension string in resource location format,
     *                  e.g. {@code "minecraft:overworld"}
     * @return the corresponding {@code ResourceKey<Level>}, or {@code null}
     */
    public static ResourceKey<Level> dimensionKey(String dimension) {
        if (dimension == null || dimension.isBlank()) return Level.OVERWORLD;
        return switch (dimension) {
            case "minecraft:overworld"  -> Level.OVERWORLD;
            case "minecraft:the_nether" -> Level.NETHER;
            case "minecraft:the_end"    -> Level.END;
            default -> {
                try {
                    yield ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimension));
                } catch (Exception e) {
                    yield null;
                }
            }
        };
    }
}