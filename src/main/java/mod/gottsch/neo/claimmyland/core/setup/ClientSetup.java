/*
 * This file is part of  Claim My Land.
 * Copyright (c) 2022 Mark Gottschling (gottsch)
 *
 * All rights reserved.
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
package mod.gottsch.neo.claimmyland.core.setup;

import mod.gottsch.neo.claimmyland.client.hud.ParcelHud;
import mod.gottsch.neo.claimmyland.client.renderer.ParcelBorderRenderer;
import mod.gottsch.neo.claimmyland.core.integration.journeymap.ParcelMapTooltipRenderer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;


/**
 *
 * @author Mark Gottschling on Nov 16, 2022
 *
 */
//@EventBusSubscriber(modid = ClaimMyLand.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientSetup {
    /**
     *
     * @param event
     */
    public static void init(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            NeoForge.EVENT_BUS.register(ParcelHud.class);
            NeoForge.EVENT_BUS.register(ParcelBorderRenderer.class);
            NeoForge.EVENT_BUS.register(ParcelMapTooltipRenderer.class);
        });
    }
}
