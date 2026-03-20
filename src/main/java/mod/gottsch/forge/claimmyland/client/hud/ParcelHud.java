/*
 * This file is part of  Claim My Land.
 * Copyright (c) 2024 Mark Gottschling (gottsch)
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
 */
package mod.gottsch.forge.claimmyland.client.hud;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.cache.ClientParcelCache;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Renders a small HUD overlay in the bottom-left corner of the screen showing
 * the parcel and estate the local player is currently standing inside.
 *
 * <p>Data source: {@link ClientParcelCache} — the single-entry position cache
 * updated by {@code CacheSyncPacket} on each server BST hit. When the cache
 * reports wilderness ({@code get() == null}) the HUD is hidden entirely.</p>
 *
 * <p>Layout (bottom-left):</p>
 * <pre>
 *   ║ Land Claim                        ← GOLD title
 *   ║ GottschLand  (Nation)             ← estate name WHITE + type suffix in type color
 *   ║ MyPlot | Dev                      ← parcel name + owner, WHITE
 *   ║ Nation: GottschNation             ← only for CITIZEN / ZONE parcels, AQUA
 * </pre>
 *
 * <p>Color convention (matches {@code EstateDisplayFormatter.getParcelColor()}):</p>
 * <ul>
 *   <li>NATION  → BLUE         ({@code 0x5555FF})</li>
 *   <li>CITIZEN → LIGHT_PURPLE ({@code 0xFF55FF})</li>
 *   <li>PLAYER  → GREEN        ({@code 0x55FF55})</li>
 *   <li>ZONE / others → YELLOW ({@code 0xFFFF55})</li>
 * </ul>
 *
 * @author Mark Gottschling on Mar 06, 2026
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = ClaimMyLand.MOD_ID, value = Dist.CLIENT)
public class ParcelHud {

    // -----------------------------------------------------------------------
    // Layout constants
    // -----------------------------------------------------------------------

    /** Horizontal distance from the left edge of the screen to the text. */
    private static final int MARGIN_LEFT = 8;

    /**
     * Vertical distance from the bottom edge of the screen to the bottom of the
     * last rendered line. Sized to sit just above the hotbar (39 px) with a gap.
     */
    private static final int MARGIN_BOTTOM = 44;

    /** Height of one text line including leading. */
    private static final int LINE_HEIGHT = 10;

    /** Width of the colored left border strip in pixels. */
    private static final int BORDER_WIDTH = 2;

    /** Horizontal gap between the left border and the text. */
    private static final int BORDER_GAP = 3;

    // -----------------------------------------------------------------------
    // Color constants — must match EstateDisplayFormatter.getParcelColor()
    // -----------------------------------------------------------------------

    private static final int COLOR_NATION      = 0x5555FF;
    private static final int COLOR_CITIZEN     = 0xFF55FF;
    private static final int COLOR_PLAYER      = 0x55FF55;
    private static final int COLOR_DEFAULT     = 0xFFFF55;
    private static final int COLOR_GOLD        = 0xFFAA00;
    private static final int COLOR_WHITE       = 0xFFFFFF;
    /** Aqua — used for the "Nation: X" line on CITIZEN/ZONE parcels. */
    private static final int COLOR_NATION_LINE = 0x55FFFF;
    private static final int COLOR_BG          = 0x60000000;
    private static final int COLOR_TITLE_BG    = 0x90000000;

    // -----------------------------------------------------------------------
    // Render event handler
    // -----------------------------------------------------------------------

    /**
     * Fires after the hotbar overlay is rendered — exactly once per frame at the
     * correct z-order for a bottom-left HUD element.
     *
     * @author Mark Gottschling on Mar 06, 2026
     */
    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) {
            return;
        }

        // hide HUD while chat (or any screen) is open
        if (mc.screen != null) return;

        ClientParcelCache.Entry entry = ClientParcelCache.get();
        if (entry == null) {
            return; // Wilderness — nothing to show.
        }

        renderHud(event.getGuiGraphics(), mc.font, entry);
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    /**
     * Draws the parcel info panel at the bottom-left of the screen.
     *
     * <p>Renders 2 content lines normally, or 3 for CITIZEN/ZONE parcels
     * (the extra line shows the parent nation name).</p>
     *
     * @author Mark Gottschling on Mar 06, 2026
     */
    private static void renderHud(GuiGraphics graphics, Font font, ClientParcelCache.Entry entry) {
        int screenHeight = graphics.guiHeight();
        int typeColor    = typeColor(entry.getParcelType());

        // --- Build display strings ---
        String titleText  = " Land Claim ";
        String estateName = notEmpty(entry.getEstateName(), "(unnamed estate)");
        String typeSuffix = " (" + typeLabel(entry.getParcelType()) + ")";
        String parcelName = notEmpty(entry.getParcelName(), "(unnamed parcel)");
        String ownerName  = notEmpty(entry.getOwnerName(), "Unknown");
        String parcelLine = parcelName + " | " + ownerName;

        // Nation line — only for CITIZEN and ZONE parcels
        String nationLine = null;
        if (entry.getParcelType() == ParcelType.CITIZEN || entry.getParcelType() == ParcelType.ZONE) {
            String nationName = notEmpty(entry.getNationName(), null);
            if (nationName != null) {
                nationLine = "Nation: " + nationName;
            }
        }

        int lineCount = (nationLine != null) ? 3 : 2;

        // --- Measure widths ---
        int titleWidth   = font.width(titleText);
        int line1Width   = font.width(estateName) + font.width(typeSuffix);
        int line2Width   = font.width(parcelLine);
        int contentWidth = Math.max(line1Width, line2Width);
        if (nationLine != null) {
            contentWidth = Math.max(contentWidth, font.width(nationLine));
        }
        int panelWidth = Math.max(titleWidth, contentWidth);

        // --- Compute panel geometry ---
        int contentTop  = screenHeight - MARGIN_BOTTOM - (lineCount * LINE_HEIGHT);
        int titleTop    = contentTop - LINE_HEIGHT - 2;

        int panelLeft   = MARGIN_LEFT - BORDER_GAP - BORDER_WIDTH - 2;
        int panelRight  = MARGIN_LEFT + panelWidth + 2;
        int panelBottom = contentTop + (lineCount * LINE_HEIGHT) + 1;

        // --- Draw backgrounds ---
        graphics.fill(panelLeft, titleTop - 1, panelRight, titleTop + LINE_HEIGHT + 1, COLOR_TITLE_BG);
        graphics.fill(panelLeft, contentTop - 1, panelRight, panelBottom, COLOR_BG);

        // --- Colored left border (full height: title + content) ---
        graphics.fill(panelLeft, titleTop - 1, panelLeft + BORDER_WIDTH, panelBottom, typeColor | 0xFF000000);

        // --- Title ---
        graphics.drawString(font, titleText, MARGIN_LEFT, titleTop, COLOR_GOLD, false);

        // --- Content lines ---
        int x = MARGIN_LEFT;
        int y = contentTop;

        // Line 1: estate name + type suffix
        graphics.drawString(font, estateName, x, y, COLOR_WHITE, false);
        graphics.drawString(font, typeSuffix, x + font.width(estateName), y, typeColor, false);
        y += LINE_HEIGHT;

        // Line 2: parcel | owner
        graphics.drawString(font, parcelLine, x, y, COLOR_WHITE, false);
        y += LINE_HEIGHT;

        // Line 3 (CITIZEN/ZONE only): Nation: <name>
        if (nationLine != null) {
            graphics.drawString(font, nationLine, x, y, COLOR_NATION_LINE, false);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * @author Mark Gottschling on Mar 06, 2026
     */
    private static String typeLabel(ParcelType type) {
        if (type == null) return "Unknown";
        return switch (type) {
            case NATION  -> "Nation";
            case CITIZEN -> "Citizen";
            case ZONE    -> "Zone";
            case PLAYER  -> "Player";
            default      -> type.name();
        };
    }

    /**
     * @author Mark Gottschling on Mar 06, 2026
     */
    private static int typeColor(ParcelType type) {
        if (type == null) return COLOR_DEFAULT;
        return switch (type) {
            case NATION  -> COLOR_NATION;
            case CITIZEN -> COLOR_CITIZEN;
            case PLAYER  -> COLOR_PLAYER;
            default      -> COLOR_DEFAULT;
        };
    }

    /**
     * @author Mark Gottschling on Mar 06, 2026
     */
    private static String notEmpty(String value, String fallback) {
        return (value != null && !value.isEmpty()) ? value : fallback;
    }
}