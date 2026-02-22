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

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * @author by Mark Gottschling on 2/17/2026
 */
@Mod.EventBusSubscriber(modid = ClaimMyLand.MOD_ID, value = Dist.CLIENT)
public class KeyInputHandler {

    private static boolean wasTogglePressed = false;
    private static boolean wasScrollUpPressed = false;
    private static boolean wasScrollDownPressed = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        // Toggle HUD
        boolean togglePressed = KeyBindings.TOGGLE_HUD.isDown();
        if (togglePressed && !wasTogglePressed) {
            ScrollableCustomHUD.toggle();

            if (ScrollableCustomHUD.isVisible()) {
                mc.player.displayClientMessage(
                        Component.literal(ChatFormatting.GREEN + "[+] " +
                                ChatFormatting.GRAY + "Land Claim HUD shown"), true);
            } else {
                mc.player.displayClientMessage(
                        Component.literal(ChatFormatting.RED + "[-] " +
                                ChatFormatting.GRAY + "Land Claim HUD hidden"), true);
            }
        }
        wasTogglePressed = togglePressed;

        // Scroll Up
        boolean scrollUpPressed = KeyBindings.SCROLL_HUD_UP.isDown();
        if (scrollUpPressed && !wasScrollUpPressed) {
            if (ScrollableCustomHUD.isVisible()) {
                ScrollableCustomHUD.scrollUp(3);
            }
        }
        wasScrollUpPressed = scrollUpPressed;

        // Scroll Down
        boolean scrollDownPressed = KeyBindings.SCROLL_HUD_DOWN.isDown();
        if (scrollDownPressed && !wasScrollDownPressed) {
            if (ScrollableCustomHUD.isVisible()) {
                ScrollableCustomHUD.scrollDown(3);
            }
        }
        wasScrollDownPressed = scrollDownPressed;
    }
}