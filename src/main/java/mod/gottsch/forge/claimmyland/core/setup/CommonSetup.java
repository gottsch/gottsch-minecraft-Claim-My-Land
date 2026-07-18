/*
 * This file is part of  Claim My Land.
 * Copyright (c) 2024 Mark Gottschling (gottsch)
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
package mod.gottsch.forge.claimmyland.core.setup;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.integration.journeymap.JourneyMapIntegration;
import mod.gottsch.forge.claimmyland.core.item.ModItems;
import mod.gottsch.forge.claimmyland.core.network.CMLNetwork;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 *
 * @author Mark Gottschling on Sep 14, 2024
 *
 */
@Mod.EventBusSubscriber(modid = ClaimMyLand.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonSetup {

    /**
     *
     * @param event
     */
    public static void init(final FMLCommonSetupEvent event) {
        // create a claimmyland specific log file
        Config.instance.addRollingFileAppender(ClaimMyLand.MOD_ID);
        ClaimMyLand.LOGGER.debug("file appender created");

        CMLNetwork.register();

        // JourneyMap may be installed on a dedicated server for its own server-side
        // features. JourneyMapOverlayHandler is @OnlyIn(Dist.CLIENT), so this must also
        // gate on physical side or the server crashes loading that class (see CHANGELOG 2.5.3).
        if (FMLEnvironment.dist == Dist.CLIENT && ModList.get().isLoaded("journeymap")) {
            JourneyMapIntegration.init();
        }
    }

    @SubscribeEvent
    public static void registemItemsToTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.BORDER_STONE.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.accept(ModItems.PLAYER_DEED_10.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.accept(ModItems.PLAYER_DEED_16.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.accept(ModItems.PLAYER_DEED_32.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.accept(ModItems.NATION_DEED_100.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.accept(ModItems.CITIZEN_PLACEMENT_TOOL.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.accept(ModItems.ZONING_PLACEMENT_TOOL.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);

        }
    }
}
