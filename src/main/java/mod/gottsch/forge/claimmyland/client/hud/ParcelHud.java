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
 * <p>Layout (bottom-left, 3 lines, 10 px per line):</p>
 * <pre>
 *   ║ Land Claim                        ← GOLD title
 *   ║ GottschLand  (Nation)             ← estate name WHITE + type suffix in type color
 *   ║ MyPlot | Dev                      ← parcel name + owner, WHITE
 * </pre>
 *
 * <p>A solid 2 px colored left border runs the full panel height in the type color,
 * providing visual identification without a redundant type label line.</p>
 *
 * <p>Color convention (matches {@code EstateDisplayFormatter.getParcelColor()}):</p>
 * <ul>
 *   <li>NATION  → BLUE         ({@code 0x5555FF})</li>
 *   <li>CITIZEN → LIGHT_PURPLE ({@code 0xFF55FF})</li>
 *   <li>PLAYER  → GREEN        ({@code 0x55FF55})</li>
 *   <li>ZONE / others → YELLOW ({@code 0xFFFF55})</li>
 * </ul>
 *
 * <p>Registration: explicit registration on the Forge event bus is required from
 * {@code ClientSetup} — do NOT rely on {@code @Mod.EventBusSubscriber} auto-discovery
 * for this isolated package:</p>
 * <pre>
 *   MinecraftForge.EVENT_BUS.register(ParcelHud.class);
 * </pre>
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

    /**
     * Number of content lines rendered below the title bar
     * (estate+type, parcel+owner).
     */
    private static final int LINE_COUNT = 2;

    /** Width of the colored left border strip in pixels. */
    private static final int BORDER_WIDTH = 2;

    /** Horizontal gap between the left border and the text. */
    private static final int BORDER_GAP = 3;

    // -----------------------------------------------------------------------
    // Color constants — must match EstateDisplayFormatter.getParcelColor()
    // -----------------------------------------------------------------------

    /** Nation type color — BLUE ({@code ChatFormatting.BLUE}). */
    private static final int COLOR_NATION  = 0x5555FF;

    /** Citizen type color — LIGHT_PURPLE ({@code ChatFormatting.LIGHT_PURPLE}). */
    private static final int COLOR_CITIZEN = 0xFF55FF;

    /** Player type color — GREEN ({@code ChatFormatting.GREEN}). */
    private static final int COLOR_PLAYER  = 0x55FF55;

    /** Zone / default type color — YELLOW ({@code ChatFormatting.YELLOW}). */
    private static final int COLOR_DEFAULT = 0xFFFF55;

    /** Title text color — GOLD ({@code ChatFormatting.GOLD}). */
    private static final int COLOR_GOLD    = 0xFFAA00;

    /** Standard white used for estate name and parcel+owner line. */
    private static final int COLOR_WHITE   = 0xFFFFFF;

    /** Semi-transparent black for the main panel background. */
    private static final int COLOR_BG      = 0x60000000;

    /** Slightly more opaque black for the title bar background. */
    private static final int COLOR_TITLE_BG = 0x90000000;

    // -----------------------------------------------------------------------
    // Render event handler
    // -----------------------------------------------------------------------

    /**
     * Fires after the hotbar overlay is rendered — exactly once per frame at the
     * correct z-order for a bottom-left HUD element.
     *
     * @param event the post-render overlay event
     */
    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }

        ClientParcelCache.Entry entry = ClientParcelCache.get();
        if (entry == null) {
            return; // Wilderness — nothing to show.
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) {
            return;
        }

        renderHud(event.getGuiGraphics(), mc.font, entry);
    }

    // -----------------------------------------------------------------------
    // Rendering helpers
    // -----------------------------------------------------------------------

    /**
     * Draws the three-line parcel info panel (title + 2 content lines) at the
     * bottom-left of the screen, with a solid colored left border in the type color.
     *
     * @param graphics the current frame graphics context
     * @param font     the active Minecraft font renderer
     * @param entry    the current (non-null) cache entry
     */
    private static void renderHud(GuiGraphics graphics, Font font, ClientParcelCache.Entry entry) {
        int screenHeight = graphics.guiHeight();
        int typeColor    = typeColor(entry.getParcelType());

        // --- Build display strings ---

        // Title bar
        String titleText = " Land Claim ";

        // Line 1: estate name (white) + " (Type)" suffix (type color)
        String estateName = notEmpty(entry.getEstateName(), "(unnamed estate)");
        String typeSuffix = " (" + typeLabel(entry.getParcelType()) + ")";

        // Line 2: parcel name + " | " + owner (all white)
        String parcelName = notEmpty(entry.getParcelName(), "(unnamed parcel)");
        String ownerName  = notEmpty(entry.getOwnerName(),  "Unknown");
        String parcelLine = parcelName + " | " + ownerName;

        // --- Measure widths ---
        int titleWidth   = font.width(titleText);
        int line1Width   = font.width(estateName) + font.width(typeSuffix);
        int line2Width   = font.width(parcelLine);
        int contentWidth = Math.max(line1Width, line2Width);
        int panelWidth   = Math.max(titleWidth, contentWidth);

        // --- Compute panel geometry ---
        // Content block sits above the hotbar; title bar sits above the content block.
        int contentTop  = screenHeight - MARGIN_BOTTOM - (LINE_COUNT * LINE_HEIGHT);
        int titleTop    = contentTop - LINE_HEIGHT - 2; // 2 px gap between title and content

        int panelLeft   = MARGIN_LEFT - BORDER_GAP - BORDER_WIDTH - 2;
        int panelRight  = MARGIN_LEFT + panelWidth + 2;
        int panelBottom = contentTop + (LINE_COUNT * LINE_HEIGHT) + 1;

        // --- Draw title bar background ---
        graphics.fill(panelLeft, titleTop - 1, panelRight, titleTop + LINE_HEIGHT + 1, COLOR_TITLE_BG);

        // --- Draw content panel background ---
        graphics.fill(panelLeft, contentTop - 1, panelRight, panelBottom, COLOR_BG);

        // --- Draw colored left border (full height: title + content) ---
        graphics.fill(panelLeft, titleTop - 1, panelLeft + BORDER_WIDTH, panelBottom, typeColor | 0xFF000000);

        // --- Draw title text ---
        graphics.drawString(font, titleText, MARGIN_LEFT, titleTop, COLOR_GOLD, false);

        // --- Draw content lines ---
        int x = MARGIN_LEFT;
        int y = contentTop;

        // Line 1: estate name in white, type suffix in type color
        graphics.drawString(font, estateName, x, y, COLOR_WHITE, false);
        graphics.drawString(font, typeSuffix, x + font.width(estateName), y, typeColor, false);
        y += LINE_HEIGHT;

        // Line 2: parcel | owner in white
        graphics.drawString(font, parcelLine, x, y, COLOR_WHITE, false);
    }

    /**
     * Returns the short human-readable type label used in the {@code (Type)} suffix.
     *
     * @param type the parcel type, may be null
     * @return a capitalized type name
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
     * Maps a {@link ParcelType} to the RGB color integer used for the type suffix
     * text and the left border. Matches {@code EstateDisplayFormatter.getParcelColor()}.
     *
     * <ul>
     *   <li>NATION  → BLUE         ({@code 0x5555FF})</li>
     *   <li>CITIZEN → LIGHT_PURPLE ({@code 0xFF55FF})</li>
     *   <li>PLAYER  → GREEN        ({@code 0x55FF55})</li>
     *   <li>ZONE / others → YELLOW ({@code 0xFFFF55})</li>
     * </ul>
     *
     * @param type the parcel type, may be null
     * @return the RGB color integer (no alpha — border caller ORs in 0xFF000000)
     */
    private static int typeColor(ParcelType type) {
        if (type == null) return COLOR_DEFAULT;
        return switch (type) {
            case NATION  -> COLOR_NATION;
            case CITIZEN -> COLOR_CITIZEN;
            case PLAYER  -> COLOR_PLAYER;
            default      -> COLOR_DEFAULT; // ZONE and any future types
        };
    }

    /**
     * Returns {@code value} if non-null and non-empty, otherwise returns
     * {@code fallback}.
     *
     * @param value    the string to test
     * @param fallback the fallback string
     * @return a non-null, non-empty string
     */
    private static String notEmpty(String value, String fallback) {
        return (value != null && !value.isEmpty()) ? value : fallback;
    }
}