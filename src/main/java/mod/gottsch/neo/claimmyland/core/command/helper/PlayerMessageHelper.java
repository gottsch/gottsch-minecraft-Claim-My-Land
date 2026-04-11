package mod.gottsch.neo.claimmyland.core.command.helper;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * @author Mark Gottschling on 3/5/2026
 *
 * Static helper for sending formatted messages directly to a {@link Player} from
 * item and tool context, where no {@link net.minecraft.commands.CommandSourceStack}
 * is available.
 *
 * <p>This is the player-context companion to {@link CommandHelper}, which serves
 * command context. Both helpers delegate all message construction to
 * {@link CommandResponseFormatter} so formatting is consistent everywhere.
 *
 * <p>Usage pattern:
 * <pre>
 * // Simple failure — title only
 * PlayerMessageHelper.sendFailure(player, "citizen_deed.invalid");
 *
 * // Failure with body detail
 * PlayerMessageHelper.sendFailure(player, "estate.not_found", "estate.not_found.detail", estateName);
 *
 * // Success
 * PlayerMessageHelper.sendSuccess(player, "parcel.claim.success");
 *
 * // Unexpected error in catch block
 * PlayerMessageHelper.unexpectedError(player);
 *
 * // Pre-built formatter output (whitelist responses, display lists, etc.)
 * PlayerMessageHelper.sendLines(player,
 *         CommandResponseFormatter.formatWhitelistAdded(WhitelistType.BLOCK, entryName, estateName, estateId));
 * </pre>
 *
 * <p>Tool classes that previously held private {@code sendError()} / {@code sendSuccess()}
 * helpers (e.g. {@code ZoningTool}, {@code CitizenTool}) should delete those helpers and
 * call this class directly instead.
 *
 * @see CommandHelper
 * @see CommandResponseFormatter
 */
public class PlayerMessageHelper {

    private PlayerMessageHelper() {}

    // -------------------------------------------------------------------------
    // Core send method
    // -------------------------------------------------------------------------

    /**
     * Sends each {@link Component} line to the player via
     * {@link Player#sendSystemMessage(Component)}.
     * All other methods in this class funnel through here.
     *
     * @param player the recipient
     * @param lines  pre-built Component lines from CommandResponseFormatter
     */
    public static void sendLines(Player player, List<Component> lines) {
        lines.forEach(player::sendSystemMessage);
    }

    // -------------------------------------------------------------------------
    // Shorthand — title only (self-explanatory outcomes)
    // -------------------------------------------------------------------------

    /**
     * Sends a green ✔ SUCCESS header using the lang key as the title.
     * Use when the outcome is self-explanatory and no body detail is needed.
     *
     * @param player  the recipient
     * @param langKey lang string key (passed to LangUtil.chat())
     * @param args    optional format args substituted into the lang string
     */
    public static void sendSuccess(Player player, String langKey, Object... args) {
        sendLines(player, CommandResponseFormatter.formatSuccess(langKey, args));
    }

    /**
     * Sends a red ✘ ERROR header using the lang key as the title.
     * Use when the failure is self-explanatory and no body detail is needed.
     *
     * @param player  the recipient
     * @param langKey lang string key (passed to LangUtil.chat())
     * @param args    optional format args substituted into the lang string
     */
    public static void sendFailure(Player player, String langKey, Object... args) {
        sendLines(player, CommandResponseFormatter.formatFailure(langKey, args));
    }

    /**
     * Sends a yellow ⚠ WARNING header using the lang key as the title.
     * Use when the action completed but with caveats the player should know.
     *
     * @param player  the recipient
     * @param langKey lang string key (passed to LangUtil.chat())
     * @param args    optional format args substituted into the lang string
     */
    public static void sendWarning(Player player, String langKey, Object... args) {
        sendLines(player, CommandResponseFormatter.formatWarning(langKey, args));
    }

    /**
     * Sends an aqua ℹ INFO header using the lang key as the title.
     * Use for informational messages that require no action.
     *
     * @param player  the recipient
     * @param langKey lang string key (passed to LangUtil.chat())
     * @param args    optional format args substituted into the lang string
     */
    public static void sendInfo(Player player, String langKey, Object... args) {
        sendLines(player, CommandResponseFormatter.formatInfo(langKey, args));
    }

    // -------------------------------------------------------------------------
    // Full format — title + body detail
    // -------------------------------------------------------------------------

    /**
     * Sends a green ✔ SUCCESS header with a grey body detail line.
     * Use when extra context helps the player understand what succeeded.
     *
     * @param player   the recipient
     * @param titleKey lang key for the bold white title line
     * @param bodyKey  lang key for the grey body line
     * @param bodyArgs optional format args substituted into the body lang string
     */
    public static void sendSuccess(Player player, String titleKey, String bodyKey, Object... bodyArgs) {
//        sendLines(player, CommandResponseFormatter.formatSuccessWithDetail(titleKey, bodyKey, bodyArgs));
        sendLines(player, CommandResponseFormatter.formatSuccessWithDetail(titleKey, bodyKey, (Object[]) bodyArgs));

    }

    /**
     * Sends a red ✘ ERROR header with a grey body detail line.
     * Use when the player needs context to understand or recover from the failure.
     *
     * @param player   the recipient
     * @param titleKey lang key for the bold white title line
     * @param bodyKey  lang key for the grey body line
     * @param bodyArgs optional format args substituted into the body lang string
     */
    public static void sendFailure(Player player, String titleKey, String bodyKey, Object... bodyArgs) {
        sendLines(player, CommandResponseFormatter.formatFailureWithDetail(titleKey, bodyKey, bodyArgs));
    }

    /**
     * Sends a yellow ⚠ WARNING header with a grey body detail line.
     *
     * @param player   the recipient
     * @param titleKey lang key for the bold white title line
     * @param bodyKey  lang key for the grey body line
     * @param bodyArgs optional format args substituted into the body lang string
     */
    public static void sendWarning(Player player, String titleKey, String bodyKey, Object... bodyArgs) {
        sendLines(player, CommandResponseFormatter.formatWarningWithDetail(titleKey, bodyKey, bodyArgs));
    }

    // -------------------------------------------------------------------------
    // Catch-block convenience
    // -------------------------------------------------------------------------

    /**
     * Sends a generic ✘ ERROR message for unexpected exceptions caught in tool
     * or item interaction code. Always pair with a {@code LOGGER.error()} call
     * before invoking this.
     *
     * <pre>
     * } catch (Exception e) {
     *     ClaimMyLand.LOGGER.error("Unexpected error in CitizenDeed", e);
     *     PlayerMessageHelper.unexpectedError(player);
     * }
     * </pre>
     *
     * @param player the recipient
     */
    public static void unexpectedError(Player player) {
        sendLines(player, CommandResponseFormatter.formatFailure("unexpected_error"));
    }
}