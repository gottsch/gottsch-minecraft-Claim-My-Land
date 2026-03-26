package mod.gottsch.forge.claimmyland.core.command.helper;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static mod.gottsch.forge.claimmyland.core.command.helper.FormatterConstants.newline;

/**
 * @author Mark Gottschling on 3/4/2026
 *
 * Builds formatted {@link Component} lists for all command and tool responses.
 * Every method returns {@code List<Component>} — never void — so that both
 * {@link CommandHelper} (command context) and {@link PlayerMessageHelper}
 * (player/tool context) can consume the output via their respective
 * {@code sendLines()} methods.
 *
 * <h3>Two message shapes</h3>
 *
 * <b>Shorthand</b> — title only, used when the outcome is self-explanatory:
 * <pre>
 * ✔ PARCEL DEMOLISHED
 * ══════════════════════════════
 * </pre>
 *
 * <b>Full</b> — title + grey body, used when context helps the player:
 * <pre>
 * ✘ ERROR
 * ══════════════════════════════
 *
 * Estate Not Found
 *
 * No estate named 'MyEstate' exists.
 * </pre>
 *
 * <h3>Routing</h3>
 * <ul>
 *   <li>Command callers use {@link CommandHelper#sendSuccess}, {@link CommandHelper#sendFailure}, etc.
 *   <li>Tool/item callers use {@link PlayerMessageHelper#sendSuccess}, {@link PlayerMessageHelper#sendFailure}, etc.
 *   <li>Both call {@code CommandResponseFormatter.format*()} and pass the result to their
 *       respective {@code sendLines()} method.
 *   <li>Whitelist and display-list responses are always sent via {@code sendLines()} directly,
 *       since their colour is embedded in the Component styles.
 * </ul>
 *
 * <h3>Return-type change from v2.0</h3>
 * All methods previously returning {@code List<String>} now return {@code List<Component>}.
 * This preserves bold, colour, and translatable formatting when rendered by both
 * {@code source.sendSuccess()} and {@code player.sendSystemMessage()}.
 */
public class CommandResponseFormatter {

    private static final String SEPARATOR = "══════════════════════════════";

    private static final String BRANCH      = "├─ ";
    private static final String LAST_BRANCH = "└─ ";

    private CommandResponseFormatter() {}

    // =========================================================================
    // Core format builders — shorthand (title only)
    // =========================================================================

    /**
     * Green ✔ SUCCESS — shorthand.
     *
     * @param langKey lang key for the title (passed through LangUtil.chat())
     * @param args    optional substitution args for the lang string
     */
    public static List<Component> formatSuccess(String langKey, Object... args) {
        return buildShorthand(ChatFormatting.GREEN, "✔", langKey, args);
    }

    /**
     * Red ✘ ERROR — shorthand.
     *
     * @param langKey lang key for the title
     * @param args    optional substitution args
     */
    public static List<Component> formatFailure(String langKey, Object... args) {
        return buildShorthand(ChatFormatting.RED, "✘", langKey, args);
    }

    /**
     * Yellow ⚠ WARNING — shorthand.
     *
     * @param langKey lang key for the title
     * @param args    optional substitution args
     */
    public static List<Component> formatWarning(String langKey, Object... args) {
        return buildShorthand(ChatFormatting.YELLOW, "⚠", langKey, args);
    }

    /**
     * Aqua ℹ INFO — shorthand.
     *
     * @param langKey lang key for the title
     * @param args    optional substitution args
     */
    public static List<Component> formatInfo(String langKey, Object... args) {
        return buildShorthand(ChatFormatting.AQUA, "ℹ", langKey, args);
    }

    public static List<Component> formatFailureWithDetail(String titleKey, String bodyKey, Object... bodyArgs) {
        return buildFull(ChatFormatting.RED, "✘", titleKey, bodyKey, bodyArgs);
    }

    public static List<Component> formatSuccessWithDetail(String titleKey, String bodyKey, Object... bodyArgs) {
//        return buildFull(ChatFormatting.GREEN, "✔", titleKey, bodyKey, bodyArgs);
        return buildFull(ChatFormatting.GREEN, "✔", titleKey, bodyKey, (Object[]) bodyArgs);

    }

    public static List<Component> formatWarningWithDetail(String titleKey, String bodyKey, Object... bodyArgs) {
        return buildFull(ChatFormatting.YELLOW, "⚠", titleKey, bodyKey, bodyArgs);
    }

    // =========================================================================
    // Core format builders — full (title + body)
    // =========================================================================

    /**
     * Green ✔ SUCCESS — full format with a grey body detail line.
     *
     * @param titleKey lang key for the bold white title
     * @param bodyKey  lang key for the grey body line
     * @param bodyArgs optional substitution args for the body lang string
     */
    public static List<Component> formatSuccess(String titleKey, String bodyKey, Object... bodyArgs) {
        return buildFull(ChatFormatting.GREEN, "✔", titleKey, bodyKey, bodyArgs);
    }

    /**
     * Red ✘ ERROR — full format with a grey body detail line.
     *
     * @param titleKey lang key for the bold white title
     * @param bodyKey  lang key for the grey body line
     * @param bodyArgs optional substitution args for the body lang string
     */
    public static List<Component> formatFailure(String titleKey, String bodyKey, Object... bodyArgs) {
        return buildFull(ChatFormatting.RED, "✘", titleKey, bodyKey, bodyArgs);
    }

    /**
     * Red ✘ ERROR — full format with a bulleted list of reason lines.
     *
     * @param titleKey  lang key for the bold white title
     * @param reasonsKey lang key whose translated value is a ~-delimited list of reason lang keys
     */
    public static List<Component> formatFailureWithReasons(String titleKey, String reasonsKey) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("✘ ERROR").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        lines.add(Component.literal(SEPARATOR).withStyle(ChatFormatting.RED));
        lines.add(newline());
        lines.add(Component.translatable(LangUtil.chat(titleKey))
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        lines.add(newline());

        String[] reasons = Component.translatable(LangUtil.chat(reasonsKey))
                .getString().split("~");

        for (int i = 0; i < reasons.length; i++) {
            String branch = (i == reasons.length - 1) ? LAST_BRANCH : BRANCH;
            lines.add(Component.literal(branch)
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(reasons[i].trim())
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC)));
        }
        lines.add(newline());
        return lines;
    }

    /**
     * Yellow ⚠ WARNING — full format with a grey body detail line.
     *
     * @param titleKey lang key for the bold white title
     * @param bodyKey  lang key for the grey body line
     * @param bodyArgs optional substitution args for the body lang string
     */
    public static List<Component> formatWarning(String titleKey, String bodyKey, Object... bodyArgs) {
        return buildFull(ChatFormatting.YELLOW, "⚠", titleKey, bodyKey, bodyArgs);
    }

    // =========================================================================
    // Generic whitelist responses
    // =========================================================================

    /**
     * Green ✔ [TYPE] WHITELISTED — with entry name and estate detail.
     */
    public static List<Component> formatWhitelistAdded(
            WhitelistType type, String entryName, String estateName, int estateId) {

        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("✔ " + type.getTitle().toUpperCase() + " WHITELISTED")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        lines.add(Component.literal(SEPARATOR).withStyle(ChatFormatting.GREEN));
        lines.add(newline());
        lines.add(Component.literal(entryName)
                .withStyle(type.getColor(), ChatFormatting.BOLD));
        lines.add(newline());
        lines.add(Component.literal("Added to:").withStyle(ChatFormatting.GRAY));
        lines.add(buildEstateRef(estateName, estateId));

        lines.add(newline());
        return lines;
    }

    /**
     * Yellow ⚠ [TYPE] REMOVED — with entry name and estate detail.
     */
    public static List<Component> formatWhitelistRemoved(
            WhitelistType type, String entryName, String estateName, int estateId) {

        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("⚠ " + type.getTitle().toUpperCase() + " REMOVED")
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        lines.add(Component.literal(SEPARATOR).withStyle(ChatFormatting.YELLOW));
        lines.add(newline());
        lines.add(Component.literal(entryName)
                .withStyle(type.getColor(), ChatFormatting.BOLD));
        lines.add(newline());
        lines.add(Component.literal("Removed from:").withStyle(ChatFormatting.GRAY));
        lines.add(buildEstateRef(estateName, estateId));

        lines.add(newline());
        return lines;
    }

    /**
     * Red ✘ Already Whitelisted error.
     */
    public static List<Component> formatWhitelistAlreadyExists(
            WhitelistType type, String entryName, String estateName) {

        return formatFailureWithDetail(
                "whitelist.already_exists",
                "whitelist.already_exists.detail",
                entryName, type.getTitle().toLowerCase(), estateName);
    }

    /**
     * Red ✘ Not Whitelisted error.
     */
    public static List<Component> formatWhitelistNotFound(
            WhitelistType type, String entryName, String estateName) {

        return formatFailureWithDetail(
                "whitelist.not_found",
                "whitelist.not_found.detail",
                entryName, type.getTitle().toLowerCase(), estateName);
    }

    // =========================================================================
    // Estate-specific responses
    // =========================================================================

    /** Green ✔ ESTATE RENAMED — shows old and new name. */
    public static List<Component> formatEstateRenamed(String oldName, String newName, UUID estateId) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("✔ ESTATE RENAMED")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        lines.add(Component.literal(SEPARATOR).withStyle(ChatFormatting.GREEN));
        lines.add(newline());
        lines.add(Component.literal("Estate ID: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(estateId.toString()).withStyle(ChatFormatting.WHITE)));
        lines.add(newline());
        lines.add(Component.literal("Old Name:").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("  " + oldName)
                .withStyle(ChatFormatting.RED, ChatFormatting.STRIKETHROUGH));
        lines.add(newline());
        lines.add(Component.literal("New Name:").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("  " + newName)
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

        lines.add(newline());
        return lines;
    }

    /** Green ✔ ESTATE CREATED — shows name, ID, type, status. */
    public static List<Component> formatEstateCreated(String estateName, int estateId, String estateType) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("✔ ESTATE CREATED")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        lines.add(Component.literal(SEPARATOR).withStyle(ChatFormatting.GREEN));
        lines.add(newline());
        lines.add(Component.literal(estateName)
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        lines.add(newline());
        lines.add(Component.literal(BRANCH + "Estate ID: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(estateId)).withStyle(ChatFormatting.WHITE)));
        lines.add(Component.literal(BRANCH + "Type: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(estateType).withStyle(ChatFormatting.YELLOW)));
        lines.add(Component.literal(LAST_BRANCH + "Status: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("Active").withStyle(ChatFormatting.GREEN)));

        lines.add(newline());
        return lines;
    }

    /** Red ✘ ESTATE DELETED — shows name, ID, parcel count removed. */
    public static List<Component> formatEstateDeleted(String estateName, UUID estateId, int parcelCount) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("✘ ESTATE DELETED")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        lines.add(Component.literal(SEPARATOR).withStyle(ChatFormatting.RED));
        lines.add(newline());
        lines.add(Component.literal(estateName)
                .withStyle(ChatFormatting.RED, ChatFormatting.STRIKETHROUGH));
        lines.add(newline());
        lines.add(Component.literal(BRANCH + "Estate ID: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(estateId.toString()).withStyle(ChatFormatting.WHITE)));
        lines.add(Component.literal(LAST_BRANCH + "Parcels Removed: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(parcelCount)).withStyle(ChatFormatting.YELLOW)));

        lines.add(newline());
        return lines;
    }

    /** Yellow ⚠ OWNERSHIP TRANSFERRED — shows estate, old and new owner. */
    public static List<Component> formatOwnershipTransferred(
            String estateName, UUID estateId, String oldOwner, String newOwner) {

        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("⚠ OWNERSHIP TRANSFERRED")
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        lines.add(Component.literal(SEPARATOR).withStyle(ChatFormatting.YELLOW));
        lines.add(newline());
        lines.add(Component.literal(estateName)
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        lines.add(Component.literal("Estate ID: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(estateId.toString()).withStyle(ChatFormatting.WHITE)));
        lines.add(newline());
        lines.add(Component.literal("Previous Owner:").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("  " + oldOwner).withStyle(ChatFormatting.RED));
        lines.add(newline());
        lines.add(Component.literal("New Owner:").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("  " + newOwner)
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

        lines.add(newline());
        return lines;
    }

    // =========================================================================
    // Parcel-specific responses
    // =========================================================================

    /** Green ✔ PARCEL CLAIMED — shows parcel ID, estate, and location. */
    public static List<Component> formatParcelClaimed(int parcelId, String estateName, String location) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("✔ PARCEL CLAIMED")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        lines.add(Component.literal(SEPARATOR).withStyle(ChatFormatting.GREEN));
        lines.add(newline());
        lines.add(Component.literal("Parcel #" + parcelId)
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        lines.add(newline());
        lines.add(Component.literal(BRANCH + "Estate: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(estateName).withStyle(ChatFormatting.YELLOW)));
        lines.add(Component.literal(LAST_BRANCH + "Location: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(location).withStyle(ChatFormatting.WHITE)));

        lines.add(newline());
        return lines;
    }

    /** Yellow ⚠ PARCEL UNCLAIMED — shows parcel ID and estate it was removed from. */
    public static List<Component> formatParcelUnclaimed(int parcelId, String estateName) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("⚠ PARCEL UNCLAIMED")
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        lines.add(Component.literal(SEPARATOR).withStyle(ChatFormatting.YELLOW));
        lines.add(newline());
        lines.add(Component.literal("Parcel #" + parcelId)
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        lines.add(newline());
        lines.add(Component.literal(LAST_BRANCH + "Removed from: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(estateName).withStyle(ChatFormatting.YELLOW)));

        lines.add(newline());
        return lines;
    }

    /** Yellow ⚠ OWNERSHIP TRANSFERRED — shows estate, old and new owner. */
    public static List<Component> formatParcelOwnershipTransferred(
            String estateName, String parcelName, UUID parcelId, String oldOwner, String newOwner) {

        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("⚠ OWNERSHIP TRANSFERRED")
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        lines.add(Component.literal(SEPARATOR).withStyle(ChatFormatting.YELLOW));
        lines.add(newline());
        lines.add(Component.literal(parcelName)
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        lines.add(Component.literal("Parcel ID: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(parcelId.toString()).withStyle(ChatFormatting.WHITE)));
        lines.add(Component.literal("Estate: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(estateName).withStyle(ChatFormatting.WHITE)));
        lines.add(newline());
        lines.add(Component.literal("Previous Owner:").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("  " + oldOwner).withStyle(ChatFormatting.RED));
        lines.add(newline());
        lines.add(Component.literal("New Owner:").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("  " + newOwner)
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

        lines.add(newline());
        return lines;
    }

    // =========================================================================
    // Private builder helpers
    // =========================================================================

    /**
     * Builds the shorthand shape:
     * <pre>
     * [icon] TITLE
     * ══════════════════════════════
     * </pre>
     */
    private static List<Component> buildShorthand(
            ChatFormatting color, String icon, String langKey, Object[] args) {

        List<Component> lines = new ArrayList<>();
        MutableComponent title = Component.translatable(LangUtil.chat(langKey), args)
                .withStyle(color, ChatFormatting.BOLD);
        lines.add(Component.literal(icon + " ").withStyle(color, ChatFormatting.BOLD).append(title));
        lines.add(Component.literal(SEPARATOR).withStyle(color));

        lines.add(newline());
        return lines;
    }

    /**
     * Builds the full shape:
     * <pre>
     * [icon] TITLE_KEY (translated, bold coloured)
     * ══════════════════════════════
     *
     * BODY_KEY (translated, grey)
     * </pre>
     */
    private static List<Component> buildFull(
            ChatFormatting color, String icon, String titleKey, String bodyKey, Object[] bodyArgs) {

//        ClaimMyLand.LOGGER.info("buildFull bodyArgs.length=" + bodyArgs.length);
//        for (int i = 0; i < bodyArgs.length; i++) {
//            ClaimMyLand.LOGGER.info("  bodyArgs[" + i + "] = " + bodyArgs[i]);
//        }
        List<Component> lines = new ArrayList<>();
        // header line uses the generic icon label (ERROR / SUCCESS / WARNING)
        lines.add(Component.literal(icon + " " + icon2Label(icon))
                .withStyle(color, ChatFormatting.BOLD));
        lines.add(Component.literal(SEPARATOR).withStyle(color));
        lines.add(newline());
        // title = translatable, bold white
        lines.add(Component.translatable(LangUtil.chat(titleKey))
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        lines.add(newline());
        // body = translatable, grey
        lines.add(
                Component.translatable(LangUtil.chat(bodyKey), (Object[]) bodyArgs)
//                Component.translatable(LangUtil.chat(bodyKey), bodyArgs)
                .withStyle(ChatFormatting.GRAY));

        lines.add(newline());
        return lines;
    }

    /** Maps icon symbol to the generic header label shown in full-format messages. */
    private static String icon2Label(String icon) {
        return switch (icon) {
            case "✔" -> "SUCCESS";
            case "✘" -> "ERROR";
            case "⚠" -> "WARNING";
            case "ℹ" -> "INFO";
            default  -> "";
        };
    }

    /** Builds the indented "  EstateName [ID: N]" estate reference line. */
    private static Component buildEstateRef(String estateName, int estateId) {
        return Component.literal("  ")
                .append(Component.literal(estateName).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" [ID: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(estateId)).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("]").withStyle(ChatFormatting.GRAY));
    }
}