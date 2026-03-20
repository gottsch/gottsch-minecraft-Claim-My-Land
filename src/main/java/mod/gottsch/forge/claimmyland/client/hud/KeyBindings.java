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

import com.mojang.blaze3d.platform.InputConstants;
import mod.gottsch.forge.claimmyland.ClaimMyLand;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * @author by Mark Gottschling on 2/17/2026
 */
@Deprecated(forRemoval = true, since = "2.2")
//@Mod.EventBusSubscriber(modid = ClaimMyLand.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KeyBindings {

    public static final KeyMapping TOGGLE_HUD = new KeyMapping(
            "key.claimmyland.toggle_hud",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.claimmyland"
    );

    public static final KeyMapping SCROLL_HUD_UP = new KeyMapping(
            "key.claimmyland.scroll_hud_up",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UP,              // Page Up key
            "key.categories.claimmyland"
    );

    public static final KeyMapping SCROLL_HUD_DOWN = new KeyMapping(
            "key.claimmyland.scroll_hud_down",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_DOWN,            // Page Down key
            "key.categories.claimmyland"
    );

//    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_HUD);
        event.register(SCROLL_HUD_UP);
        event.register(SCROLL_HUD_DOWN);
    }
}
