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

import com.mojang.blaze3d.systems.RenderSystem;
import mod.gottsch.forge.claimmyland.ClaimMyLand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * @author by Mark Gottschling on 2/17/2026
 */
@Mod.EventBusSubscriber(modid = ClaimMyLand.MOD_ID, value = Dist.CLIENT)
public class ScrollableCustomHUD {

    private static final List<String> allLines = new ArrayList<>();
    private static boolean isVisible = false;
    private static long hideTime = 0;

    // Line limiting
    private static int maxTotalLines = 500; // Maximum total lines to keep in history

    // Scrolling
    private static int scrollOffset = 0;
    private static int maxVisibleLines = 30; // Lines visible at once

    // Font scaling
    private static float fontScale = 1.0f; // 0.75 = 75% size for more compact display
    private static int scaledLineHeight = 9; // Adjusted based on scale

    // Position and size
    private static int hudX = 10;
    private static int hudY = 10;
    private static int hudWidth = 800;
    private static int padding = 4;

    private static Font customFont = null;
    private static final ResourceLocation CUSTOM_FONT_LOCATION = new ResourceLocation(ClaimMyLand.MOD_ID, "hud_font");

//    private static Font getCustomFont() {
//        if (customFont == null) {
//            Minecraft mc = Minecraft.getInstance();
//            try {
//                // Load the custom font
//                customFont = new Font(id -> mc.fontManager.fontSets.get(CUSTOM_FONT_LOCATION));
//            } catch (Exception e) {
//                System.err.println("Failed to load custom font, using default: " + e.getMessage());
//                customFont = mc.font;
//            }
//        }
//        return customFont;
//    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay().id().toString().equals("minecraft:hotbar")) {
            if (isVisible && !allLines.isEmpty()) {
                renderScrollableHUD(event.getGuiGraphics());
            }

            // Auto-hide after timeout
            if (hideTime > 0 && System.currentTimeMillis() > hideTime) {
                isVisible = false;
                hideTime = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!isVisible || allLines.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        // Only scroll HUD when no screen is open (not chat, not inventory, etc.)
        if (mc.screen == null) {
            double scroll = event.getScrollDelta();

            if (scroll > 0) {
                scrollUp(1);
            } else if (scroll < 0) {
                scrollDown(1);
            }

            event.setCanceled(true); // Prevent other scroll actions
        }
    }

    private static void renderScrollableHUD(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();

        // Clamp scroll offset
        int maxScroll = Math.max(0, allLines.size() - maxVisibleLines);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        // Calculate actual visible lines
        int visibleLineCount = Math.min(maxVisibleLines, allLines.size());
        int hudHeight = (visibleLineCount * scaledLineHeight) + (padding * 2);

        // Calculate actual width based on content (accounting for scale)
//        int maxWidth = 0;
//        for (int i = scrollOffset; i < Math.min(scrollOffset + maxVisibleLines, allLines.size()); i++) {
//            int width = (int)(mc.font.width(allLines.get(i)) * fontScale);
//            if (width > maxWidth) {
//                maxWidth = width;
//            }
//        }
//        hudWidth = Math.max(200, maxWidth + padding * 2 + 20); // +20 for scrollbar

        // Draw background
        graphics.fill(hudX, hudY, hudX + hudWidth, hudY + hudHeight, 0xD0000000);

        // Draw border
        graphics.fill(hudX, hudY, hudX + hudWidth, hudY + 1, 0xFF555555); // Top
        graphics.fill(hudX, hudY + hudHeight - 1, hudX + hudWidth, hudY + hudHeight, 0xFF555555); // Bottom
        graphics.fill(hudX, hudY, hudX + 1, hudY + hudHeight, 0xFF555555); // Left
        graphics.fill(hudX + hudWidth - 1, hudY, hudX + hudWidth, hudY + hudHeight, 0xFF555555); // Right

        // Draw visible lines with scaling
        int textX = hudX + padding;
        int textY = hudY + padding;

        for (int i = 0; i < visibleLineCount; i++) {
            int lineIndex = scrollOffset + i;
            if (lineIndex < allLines.size()) {
                String line = allLines.get(lineIndex);

                // Calculate position for this line
                float drawX = textX;
                float drawY = textY + (i * scaledLineHeight);

                // Apply scaling for this line
                graphics.pose().pushPose();
                graphics.pose().translate(drawX, drawY, 0);
                graphics.pose().scale(fontScale, fontScale, 1.0f);

                // Draw with shadow for much better readability at small sizes
                graphics.drawString(mc.font, line, 0, 0, 0xFFFFFF, true);

                graphics.pose().popPose();
            }
        }

        // Draw scrollbar if needed
        if (allLines.size() > maxVisibleLines) {
            renderScrollbar(graphics, hudHeight);
        }

        // Draw scroll indicators
        renderScrollIndicators(graphics, hudHeight);

        // Draw line counter with shadow
        String counter = "§7" + (scrollOffset + 1) + "-" +
                Math.min(scrollOffset + maxVisibleLines, allLines.size()) +
                " / " + allLines.size();

        // Scale the counter too
        graphics.pose().pushPose();
        int counterWidth = (int)(mc.font.width(counter) * fontScale);
        float counterX = hudX + hudWidth - counterWidth - padding - 15;
        float counterY = hudY + hudHeight - (scaledLineHeight + 2);
        graphics.pose().translate(counterX, counterY, 0);
        graphics.pose().scale(fontScale, fontScale, 1.0f);
        graphics.drawString(mc.font, counter, 0, 0, 0xAAAAAA, true);
        graphics.pose().popPose();
    }

    private static void renderScrollbar(GuiGraphics graphics, int hudHeight) {
        int scrollbarX = hudX + hudWidth - 10;
        int scrollbarY = hudY + padding;
        int scrollbarHeight = hudHeight - (padding * 2);

        // Scrollbar background
        graphics.fill(scrollbarX, scrollbarY, scrollbarX + 6, scrollbarY + scrollbarHeight, 0x80333333);

        // Scrollbar thumb
        float scrollPercentage = (float) scrollOffset / (allLines.size() - maxVisibleLines);
        int thumbHeight = Math.max(10, (int) ((float) maxVisibleLines / allLines.size() * scrollbarHeight));
        int thumbY = scrollbarY + (int) (scrollPercentage * (scrollbarHeight - thumbHeight));

        graphics.fill(scrollbarX, thumbY, scrollbarX + 6, thumbY + thumbHeight, 0xFF888888);
    }

    private static void renderScrollIndicators(GuiGraphics graphics, int hudHeight) {
        Minecraft mc = Minecraft.getInstance();

        // Up arrow if not at top
        if (scrollOffset > 0) {
            String upArrow = "§f▲";
            int arrowX = hudX + hudWidth - 10;
            graphics.drawString(mc.font, upArrow, arrowX, hudY + 2, 0xFFFFFF);
        }

        // Down arrow if not at bottom
        int maxScroll = Math.max(0, allLines.size() - maxVisibleLines);
        if (scrollOffset < maxScroll) {
            String downArrow = "§f▼";
            int arrowX = hudX + hudWidth - 10;
            graphics.drawString(mc.font, downArrow, arrowX, hudY + hudHeight - 10, 0xFFFFFF, false);
        }
    }

    // Scroll up by a certain number of lines
    public static void scrollUp(int lines) {
        scrollOffset = Math.max(0, scrollOffset - lines);
    }

    // Scroll down by a certain number of lines
    public static void scrollDown(int lines) {
        int maxScroll = Math.max(0, allLines.size() - maxVisibleLines);
        scrollOffset = Math.min(maxScroll, scrollOffset + lines);
    }

    // Scroll to a specific line
    public static void scrollToLine(int lineNumber) {
        int maxScroll = Math.max(0, allLines.size() - maxVisibleLines);
        scrollOffset = Math.max(0, Math.min(maxScroll, lineNumber));
    }

    // ===== LINE MANAGEMENT =====

    // Add a single line
    public static void addLine(String text) {
        allLines.add(text);

        // Remove oldest lines if we exceed the limit (rolling log behavior)
        while (allLines.size() > maxTotalLines) {
            allLines.remove(0);
            // Adjust scroll offset so we don't scroll past the beginning
            if (scrollOffset > 0) {
                scrollOffset--;
            }
        }

        isVisible = true;

        // Auto-scroll to bottom when adding new lines
        int maxScroll = Math.max(0, allLines.size() - maxVisibleLines);
        scrollOffset = maxScroll;
    }

    // Add multiple lines at once
    public static void addLines(List<String> lines) {
        for (String line : lines) {
            allLines.add(line);
        }

        // Remove oldest lines if we exceed the limit
        while (allLines.size() > maxTotalLines) {
            allLines.remove(0);
            if (scrollOffset > 0) {
                scrollOffset--;
            }
        }

        isVisible = true;

        // Auto-scroll to bottom
        int maxScroll = Math.max(0, allLines.size() - maxVisibleLines);
        scrollOffset = maxScroll;
    }

    // Clear all lines
    public static void clear() {
        allLines.clear();
        scrollOffset = 0;
        isVisible = false;
        hideTime = 0;
    }

    // ===== SCROLLING CONTROLS =====

    // Reset scroll to top
    public static void scrollToTop() {
        scrollOffset = 0;
    }

    // Scroll to bottom
    public static void scrollToBottom() {
        int maxScroll = Math.max(0, allLines.size() - maxVisibleLines);
        scrollOffset = maxScroll;
    }

    // ===== VISIBILITY CONTROLS =====

    // Show the HUD
    public static void show() {
        isVisible = true;
    }

    // Hide the HUD
    public static void hide() {
        isVisible = false;
    }

    // Set auto-hide timer (milliseconds)
    public static void setAutoHide(long milliseconds) {
        hideTime = System.currentTimeMillis() + milliseconds;
    }

    // Toggle visibility
    public static void toggle() {
        isVisible = !isVisible;
    }

    // Check if visible
    public static boolean isVisible() {
        return isVisible;
    }

    // ===== CONFIGURATION =====

    // Set maximum visible lines (how many lines shown at once)
    public static void setMaxVisibleLines(int lines) {
        maxVisibleLines = Math.max(5, lines);
    }

    // Set maximum total lines to keep in history
    public static void setMaxTotalLines(int max) {
        maxTotalLines = Math.max(10, max);
    }

    // Get maximum total lines
    public static int getMaxTotalLines() {
        return maxTotalLines;
    }

    // Set HUD position
    public static void setPosition(int x, int y) {
        hudX = x;
        hudY = y;
    }

    // Set font scale (0.5 = half size, 1.0 = normal, 0.75 = recommended)
    public static void setFontScale(float scale) {
        fontScale = Math.max(0.3f, Math.min(2.0f, scale)); // Clamp between 0.3 and 2.0
        scaledLineHeight = (int)(10 * fontScale); // Adjust line height accordingly
    }

    // Preset font sizes
    public static void setFontSizeTiny() {
        setFontScale(0.5f);
    }

    public static void setFontSizeSmall() {
        setFontScale(0.75f);
    }

    public static void setFontSizeNormal() {
        setFontScale(1.0f);
    }

    public static void setFontSizeLarge() {
        setFontScale(1.25f);
    }

    // Get current scale
    public static float getFontScale() {
        return fontScale;
    }

    // ===== INFO GETTERS =====

    // Get current line count
    public static int getLineCount() {
        return allLines.size();
    }

    // Get current scroll position
    public static int getScrollOffset() {
        return scrollOffset;
    }
}
