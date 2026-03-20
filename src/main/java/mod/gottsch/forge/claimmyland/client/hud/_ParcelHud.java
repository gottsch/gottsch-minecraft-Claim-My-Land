/*
 * This file is part of Claim My Land.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
 *
 * Claim My Land is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Claim My Land is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Claim My Land.  If not, see <http://www.gnu.org/licenses/lgpl>.
 *
 */

package mod.gottsch.forge.claimmyland.client.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.gottsch.forge.claimmyland.core.cache.ClientParcelCache;
import mod.gottsch.forge.claimmyland.core.font.ModFonts;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Renders a compact HUD overlay in the bottom-left corner of the screen showing
 * the parcel and estate the local player is currently standing inside.
 *
 * <p>Data source: {@link ClientParcelCache} — the single-entry position cache
 * updated by {@code CacheSyncPacket} on each server BST hit. When the cache
 * reports wilderness the HUD is hidden entirely.</p>
 *
 * <p>Layout (bottom-left). The title bar renders at full font size; the four
 * detail lines render at {@link #TEXT_SCALE} (75%) to keep the panel compact:</p>
 * <pre>
 *   [ Land Claim ]          ← full-size title, GOLD, darker background
 *   [Estate Name]           ← 75% scale, white
 *   [Parcel Name]           ← 75% scale, white
 *   Owner: [ownerName]      ← 75% scale, white
 *   Type: [label]           ← 75% scale, type color
 * </pre>
 *
 * <p>Color convention (type label):</p>
 * <ul>
 *   <li>Nation  → {@code 0x55FFFF} AQUA</li>
 *   <li>Citizen → {@code 0x55FF55} GREEN</li>
 *   <li>Others  → {@code 0xFFFF55} YELLOW</li>
 * </ul>
 *
 * <p><b>Registration:</b> register explicitly on the Forge event bus from
 * {@code ClientSetup} — do NOT rely on {@code @Mod.EventBusSubscriber}
 * auto-discovery for an isolated package:</p>
 * <pre>
 *   MinecraftForge.EVENT_BUS.register(ParcelHud.class);
 * </pre>
 *
 * @author Mark Gottschling on Mar 06, 2026
 */
@Deprecated(forRemoval = true, since = "2.2")
@OnlyIn(Dist.CLIENT)
public class _ParcelHud {

    // -----------------------------------------------------------------------
    // Layout constants
    // -----------------------------------------------------------------------

    /** Horizontal distance from the left edge of the screen (unscaled pixels). */
    private static final int MARGIN_LEFT = 4;

    /**
     * Vertical distance from the bottom of the screen to the bottom of the last
     * detail line (unscaled pixels). Sized to clear the vanilla hotbar (39 px)
     * with a small gap.
     */
    private static final int MARGIN_BOTTOM = 44;

    /** Scale factor applied to the four detail lines. 0.75 = 75% of normal size. */
    private static final float TEXT_SCALE = 1f;

    /** Full-size line height (unscaled). Used for the title bar. */
    private static final int TITLE_LINE_HEIGHT = 10;

    /**
     * Effective line height for detail lines after scaling.
     * ceil(10 * 0.75) = 8 px — keeps lines from overlapping.
     */
    private static final int DETAIL_LINE_HEIGHT = (int) Math.ceil(TITLE_LINE_HEIGHT * TEXT_SCALE);

    /** Number of detail lines (estate, parcel, owner, type). */
    private static final int DETAIL_LINE_COUNT = 4;

    // -----------------------------------------------------------------------
    // Color constants
    // -----------------------------------------------------------------------

    /** Title bar text — GOLD. */
    private static final int COLOR_TITLE   = 0xFFAA00;

    /** Nation type label — AQUA. */
    private static final int COLOR_NATION  = 0x55FFFF;

    /** Citizen type label — GREEN. */
    private static final int COLOR_CITIZEN = 0x55FF55;

    /** Fallback type label — YELLOW. */
    private static final int COLOR_DEFAULT = 0xFFFF55;

    /** Detail line text — WHITE. */
    private static final int COLOR_WHITE   = 0xFFFFFF;

    /** Title bar background — slightly more opaque than the detail panel. */
    private static final int COLOR_TITLE_BG  = 0x90000000;

    /** Detail panel background — semi-transparent black. */
    private static final int COLOR_DETAIL_BG = 0x60000000;

    /** Title string — shown at all times when a parcel is active. */
    private static final String TITLE = " Land Claim ";

    // -----------------------------------------------------------------------
    // Event handler
    // -----------------------------------------------------------------------

    /**
     * Fires once per frame after the vanilla hotbar has been drawn.
     * Exits immediately if the player is in wilderness or has hidden the GUI (F1).
     */
    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }

        ClientParcelCache.Entry entry = ClientParcelCache.get();
        if (entry == null) {
            return; // wilderness — nothing to show
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) {
            return;
        }

        Style customStyle = Style.EMPTY.withFont(ModFonts.PROGGY_FONT);

        renderHud(event.getGuiGraphics(), mc.font, customStyle, entry);
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    /**
     * Draws the title bar and the scaled detail panel at the bottom-left of
     * the screen.
     *
     * <p>Coordinate strategy: all positions are calculated in unscaled screen
     * pixels. For the detail lines we push a matrix scale, then divide the
     * target position by {@link #TEXT_SCALE} so the text lands at the right
     * unscaled pixel after the scale is applied.</p>
     *
     * @param graphics current frame graphics context
     * @param font     active Minecraft font renderer
     * @param entry    current (non-null) cache entry
     */
    private static void renderHud(GuiGraphics graphics, Font font, Style style, ClientParcelCache.Entry entry) {
        int screenHeight = graphics.guiHeight();

        // ---- Build detail line strings ----
        String estateLine = notEmpty(entry.getEstateName(), "(unnamed estate)");
        String parcelLine = notEmpty(entry.getParcelName(), "(unnamed parcel)");
        String ownerLine  = "Owner: " + notEmpty(entry.getOwnerName(), "Unknown");
        String typeLine   = formatTypeLine(entry.getParcelType());

        // ---- Measure widths (unscaled font units, scaled down) ----
        // We need the panel width in unscaled pixels. Since detail text is drawn
        // at TEXT_SCALE, its visual width = font.width(s) * TEXT_SCALE.
        float detailPanelWidth = 0f;
        for (String line : new String[]{ estateLine, parcelLine, ownerLine, typeLine }) {
            float w = font.width(line) * TEXT_SCALE;
            if (w > detailPanelWidth) detailPanelWidth = w;
        }
        int titleWidth  = font.width(TITLE);
        // Panel is at least as wide as the title
        int panelWidth = (int) Math.max(titleWidth, detailPanelWidth);

        // ---- Calculate vertical positions (bottom-up) ----
        int detailBlockHeight = DETAIL_LINE_COUNT * DETAIL_LINE_HEIGHT;
        int detailTop  = screenHeight - MARGIN_BOTTOM - detailBlockHeight;
        int titleTop   = detailTop - TITLE_LINE_HEIGHT - 1; // 1 px gap between title and detail

        // ---- Draw title bar background ----
        graphics.fill(
                MARGIN_LEFT - 2,
                titleTop - 2,
                MARGIN_LEFT + panelWidth + 2,
                titleTop + TITLE_LINE_HEIGHT + 1,
                COLOR_TITLE_BG
        );

        // ---- Draw detail panel background ----
        graphics.fill(
                MARGIN_LEFT - 2,
                detailTop - 1,
                MARGIN_LEFT + panelWidth + 2,
                detailTop + detailBlockHeight + 1,
                COLOR_DETAIL_BG
        );

        // ---- Draw title (full size) ----
        graphics.drawString(font, Component.literal(TITLE).withStyle(style), MARGIN_LEFT, titleTop, COLOR_TITLE, false);

        // ---- Draw detail lines (scaled) ----
        // Push a scale matrix. Text positions must be divided by TEXT_SCALE so
        // they land at the correct unscaled pixel after the matrix is applied.
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.scale(TEXT_SCALE, TEXT_SCALE, 1.0f);

        float inv   = 1.0f / TEXT_SCALE;
        int   sx    = (int) (MARGIN_LEFT * inv);
        int   sy    = (int) (detailTop   * inv);

//        Component component = Component.literal(estateLine).withStyle(style);
        graphics.drawString(font, estateLine, sx, sy, COLOR_WHITE, false);

        sy += (int) (DETAIL_LINE_HEIGHT * inv);
        graphics.drawString(font, parcelLine, sx, sy, COLOR_WHITE, false);

        sy += (int) (DETAIL_LINE_HEIGHT * inv);
        graphics.drawString(font, ownerLine,  sx, sy, COLOR_WHITE, false);

        sy += (int) (DETAIL_LINE_HEIGHT * inv);
        graphics.drawString(font, typeLine,   sx, sy, typeColor(entry.getParcelType()), false);

        pose.popPose();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Returns {@code value} if it is non-null and non-empty, otherwise
     * returns {@code fallback}.
     */
    private static String notEmpty(String value, String fallback) {
        return (value != null && !value.isEmpty()) ? value : fallback;
    }

    /**
     * Returns the display string for the parcel type label line.
     *
     * @param type the parcel type, may be null
     * @return a human-readable type label
     */
    private static String formatTypeLine(ParcelType type) {
        if (type == null) return "Type: Unknown";
        return switch (type) {
            case NATION  -> "Type: Nation";
            case CITIZEN -> "Type: Citizen";
            case ZONE    -> "Type: Zone";
            default      -> "Type: " + type.name();
        };
    }

    /**
     * Maps a {@link ParcelType} to the ARGB color integer for its type label.
     *
     * @param type the parcel type, may be null
     * @return color integer
     */
    private static int typeColor(ParcelType type) {
        if (type == null) return COLOR_DEFAULT;
        return switch (type) {
            case NATION  -> COLOR_NATION;
            case CITIZEN -> COLOR_CITIZEN;
            default      -> COLOR_DEFAULT;
        };
    }
}