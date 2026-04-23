package mod.gottsch.neo.claimmyland.core.command.helper;

import mod.gottsch.neo.claimmyland.core.estate.Estate;
import mod.gottsch.neo.claimmyland.core.parcel.Parcel;
import mod.gottsch.neo.claimmyland.core.parcel.ParcelType;
import mod.gottsch.neo.gottschcore.spatial.ICoords;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.Filterable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds written-book pages for the Claim Atlas. Parcels are grouped by
 * estate; each parcel entry shows its dimension and min-coords, and — when
 * the parcel is in the invoking player's current dimension — a clickable
 * teleport icon running {@code /tp @s <minX> ~ <minZ>}.
 *
 * <p>Estate names and parcel names are color-coded by parcel type to match
 * the CML chat-formatter convention (NATION=blue, ZONE=yellow,
 * CITIZEN=light purple, PLAYER=green). Type labels are intentionally omitted
 * — the color coding carries the same information more compactly.
 *
 * @author Mark Gottschling on Apr 21, 2026
 */
public final class ClaimAtlasFormatter {

    /** Conservative line budget per page. Tune during in-game testing. */
    private static final int LINES_PER_PAGE = 13;

    private ClaimAtlasFormatter() {}

    public static List<Filterable<Component>> buildPages(
            ServerLevel level, List<Parcel> owned, String currentDimension) {

        List<Component> lines = new ArrayList<>();

        // Header
        lines.add(Component.literal("Claim Atlas")
                .withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_AQUA));
        lines.add(Component.literal(""));

        if (owned.isEmpty()) {
            lines.add(Component.literal("No claims yet.")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        } else {
            Map<UUID, List<Parcel>> grouped = groupByEstate(owned);
            for (Map.Entry<UUID, List<Parcel>> entry : grouped.entrySet()) {
                Estate estate = entry.getValue().get(0).getEstate();
                lines.add(buildEstateHeader(estate));
                for (Parcel parcel : entry.getValue()) {
                    lines.addAll(buildParcelLines(parcel, currentDimension));
                }
                lines.add(Component.literal(""));  // separator between estates
            }
        }

        return paginate(lines);
    }

    /* -------------------------------------------------------------------- */

    private static Map<UUID, List<Parcel>> groupByEstate(List<Parcel> parcels) {
        // LinkedHashMap preserves encounter order; v3.0 will introduce explicit sorting.
        Map<UUID, List<Parcel>> grouped = new LinkedHashMap<>();
        for (Parcel parcel : parcels) {
            if (parcel.getEstate() == null) continue;
            grouped.computeIfAbsent(parcel.getEstate().getId(), k -> new ArrayList<>())
                    .add(parcel);
        }
        return grouped;
    }

    private static Component buildEstateHeader(Estate estate) {
        // Estate name colored by the estate's parcel-type hue, bolded.
        MutableComponent line = Component.literal(estate.getName())
                .withStyle(styleFor(estate.getParcelType()).withBold(true));
        line.append(Component.literal(" "));
        line.append(FormatterConstants.hoverableUuid(estate.getId()));
        return line;
    }

    private static List<Component> buildParcelLines(Parcel parcel, String currentDimension) {
        List<Component> out = new ArrayList<>();

        // Line 1: bullet + name (color-coded by type) + [tp] (same-dim only) + uuid hover
        // Bullet sits flush-left; no leading indent.
        MutableComponent line1 = Component.literal("\u2022 ")
                .withStyle(ChatFormatting.DARK_GRAY);
        line1.append(Component.literal(parcel.getName())
                .withStyle(styleFor(parcel.getType())));

        if (currentDimension.equals(parcel.getDimension())) {
            line1.append(Component.literal(" "));
            line1.append(buildTpButton(parcel));
        }

        line1.append(Component.literal(" "));
        line1.append(FormatterConstants.hoverableUuid(parcel.getId()));
        out.add(line1);

        // Line 2: 2-space indent, dimension + min-coords
        ICoords min = parcel.getMinCoords();
        MutableComponent line2 = Component.literal("  ")
                .append(Component.literal(shortenDimension(parcel.getDimension()))
                        .withStyle(ChatFormatting.DARK_GREEN))
                .append(Component.literal(" "))
                .append(Component.literal(min.toShortString())
                        .withStyle(ChatFormatting.DARK_GRAY));
        out.add(line2);

        return out;
    }

    private static Component buildTpButton(Parcel parcel) {
        ICoords min = parcel.getMinCoords();
        String command = "/tp @s " + min.getX() + " ~ " + min.getZ();

        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                Component.literal("Teleport to " + min.toShortString()));
        ClickEvent click = new ClickEvent(ClickEvent.Action.RUN_COMMAND, command);

        return Component.literal(FormatterConstants.ICON_TELEPORT).withStyle(Style.EMPTY
                .withColor(ChatFormatting.DARK_GREEN)
                .withBold(true)
                .withClickEvent(click)
                .withHoverEvent(hover));
    }

    /**
     * Maps a {@link ParcelType} to its chat style hue. Mirrors the JM overlay
     * color convention (NATION=blue, ZONE=yellow, CITIZEN=light purple,
     * PLAYER=green) using the {@link Style} constants from
     * {@link FormatterConstants}.
     *
     * <p>Returned as a non-bold style. Callers bold it if needed (estate
     * headers do; parcel names don't).
     */
    private static Style styleFor(ParcelType type) {
        if (type == null) return Style.EMPTY.withColor(ChatFormatting.WHITE);
        return switch (type) {
            case NATION  -> Style.EMPTY.withColor(ChatFormatting.BLUE);
            case ZONE    -> Style.EMPTY.withColor(ChatFormatting.YELLOW);
            case CITIZEN -> Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE);
            case NONE, PLAYER  -> Style.EMPTY.withColor(ChatFormatting.GREEN);
        };
    }

    private static String shortenDimension(String dim) {
        if (dim == null) return "?";
        if (dim.startsWith("minecraft:")) {
            return switch (dim.substring("minecraft:".length())) {
                case "overworld"  -> "Overworld";
                case "the_nether" -> "Nether";
                case "the_end"    -> "End";
                default           -> dim.substring("minecraft:".length());
            };
        }
        return dim;
    }

    /* -------------------------------------------------------------------- */

    private static List<Filterable<Component>> paginate(List<Component> lines) {
        List<Filterable<Component>> pages = new ArrayList<>();
        int i = 0;
        while (i < lines.size()) {
            int end = Math.min(i + LINES_PER_PAGE, lines.size());
            MutableComponent page = Component.empty();
            for (int j = i; j < end; j++) {
                if (j > i) page.append(Component.literal("\n"));
                page.append(lines.get(j));
            }
            pages.add(Filterable.passThrough(page));
            i = end;
        }
        if (pages.isEmpty()) {
            pages.add(Filterable.passThrough(Component.literal("")));
        }
        return pages;
    }
}