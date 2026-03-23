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
package mod.gottsch.forge.claimmyland.core.integration.journeymap;

import mod.gottsch.forge.claimmyland.core.parcel.ClientParcel;
import mod.gottsch.forge.claimmyland.core.parcel.ParcelType;
import mod.gottsch.forge.claimmyland.core.registry.ClientParcelRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a hover tooltip over the JourneyMap fullscreen map when the cursor
 * is positioned over a claimed parcel.
 *
 * Reads the hovered parcel written by JourneyMapOverlayHandler.onMapMouseMoved()
 * via the ClientParcelRegistry bridge field.
 *
 * @author Mark Gottschling on March 22, 2026
 */
@OnlyIn(Dist.CLIENT)
public class ParcelMapTooltipRenderer {

    // JourneyMap's fullscreen map screen class name — used for instanceof guard.
    // Checked by class name to avoid a hard compile-time dependency on JM internals.
    private static final String JM_FULLSCREEN_CLASS =
            "journeymap.client.ui.fullscreen.Fullscreen";

    /**
     * Fires after any Screen finishes rendering. Guards to JM fullscreen only,
     * then draws the parcel tooltip at the current mouse position if a parcel
     * is hovered.
     */
    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!isJmFullscreen(event.getScreen())) return;

        ClientParcel parcel = ClientParcelRegistry.getHoveredParcel();
        if (parcel == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) return;

        List<Component> lines = buildTooltip(parcel);
        if (lines.isEmpty()) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int mouseX = (int) event.getMouseX();
        int mouseY = (int) event.getMouseY();

        graphics.renderComponentTooltip(
                mc.font,
                lines,
                mouseX,
                mouseY);
    }

    /**
     * Builds the tooltip line list for the given parcel.
     *
     * Format:
     *   <estate name>
     *   <ParcelType> • <owner name>
     *   <parcel name>
     *   Nation: <nation name>       ← only for ZONE and CITIZEN
     *   <W> × <D> blocks
     */
    private static List<Component> buildTooltip(ClientParcel parcel) {
        List<Component> lines = new ArrayList<>();

        // Line 1 — estate name (bold white)
        lines.add(Component.literal(parcel.estateName())
                .withStyle(style -> style
                        .withBold(true)
                        .withColor(0xFFFFFF)));

        // Line 2 — type • owner
        String typeName = parcel.parcelType() != null
                ? capitalise(parcel.parcelType().name())
                : "Parcel";
        lines.add(Component.literal(typeName + " \u2022 " + parcel.ownerName())
                .withStyle(style -> style.withColor(0xAAAAAA)));

        // Line 3 — parcel name (only if different from estate name)
        if (parcel.parcelName() != null
                && !parcel.parcelName().equalsIgnoreCase(parcel.estateName())) {
            lines.add(Component.literal(parcel.parcelName())
                    .withStyle(style -> style.withColor(0xDDDDDD)));
        }

        // Line 4 — nation name (ZONE and CITIZEN only)
        if ((parcel.parcelType() == ParcelType.ZONE
                || parcel.parcelType() == ParcelType.CITIZEN)
                && parcel.nationName() != null
                && !parcel.nationName().isBlank()) {
            lines.add(Component.literal("Nation: " + parcel.nationName())
                    .withStyle(style -> style.withColor(0x00AAFF)));
        }

        // Line 5 — size
        int width = parcel.maxX() - parcel.minX() + 1;
        int height = parcel.maxY() - parcel.minY() + 1;
        int depth = parcel.maxZ() - parcel.minZ() + 1;
        lines.add(Component.literal(width + " \u00d7 " + height + " \u00d7 " + depth + " blocks")
                .withStyle(style -> style.withColor(0x888888)));

        return lines;
    }

    private static boolean isJmFullscreen(net.minecraft.client.gui.screens.Screen screen) {
        return screen != null
                && screen.getClass().getName().equals(JM_FULLSCREEN_CLASS);
    }

    private static String capitalise(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}