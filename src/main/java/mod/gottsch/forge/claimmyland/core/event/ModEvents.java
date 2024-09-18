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
package mod.gottsch.forge.claimmyland.core.event;


import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.persistence.PersistedData;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.world.WorldInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Created by Mark Gottschling on Sep 14, 2024
 */
@Mod.EventBusSubscriber(modid = ClaimMyLand.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onWorldLoad(LevelEvent.Load event) {
        if (!event.getLevel().isClientSide()) {
            Level world = (Level) event.getLevel();
            ClaimMyLand.LOGGER.debug("In world load event for dimension {}", WorldInfo.getDimension(world).toString());
            if (WorldInfo.isSurfaceWorld(world)) {
                ClaimMyLand.LOGGER.debug("loading Claim My Land data...");
                ParcelRegistry.clear();
                PersistedData.get(world);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(final BlockEvent.BreakEvent event) {
        if (ClaimMyLand.LOGGER.isDebugEnabled()) {
            ClaimMyLand.LOGGER.debug("attempt to break block by player -> {} @ {}", event.getPlayer().getDisplayName().getString(), new Coords(event.getPos()).toShortString());
        }

        // execute if event is enabled
        if (!Config.SERVER.protection.enableBlockBreakEvent.get()) {
            return;
        }

        // prevent protected blocks from breaking
        if (!ParcelRegistry.hasAccess(Coords.of(event.getPos()), event.getPlayer().getUUID())) {
            event.setCanceled(true);
            if (ClaimMyLand.LOGGER.isDebugEnabled()) {
                ClaimMyLand.LOGGER.debug("denied block break -> {} @ {}", event.getPlayer().getDisplayName().getString(), Coords.of(event.getPos()).toShortString());
            }
            if (!event.getLevel().isClientSide()) {
                sendProtectedMessage(event.getLevel(), event.getPlayer());
            }
        }
    }

    @SubscribeEvent
    public void onBlockPlace(final BlockEvent.EntityPlaceEvent event) {
        if (ClaimMyLand.LOGGER.isDebugEnabled()) {
            ClaimMyLand.LOGGER.debug("attempt to place block by player -> {} @ {}", event.getEntity().getDisplayName().getString(), new Coords(event.getPos()).toShortString());
        }

        if (!Config.SERVER.protection.enableEntityPlaceEvent.get()
//				|| event.getEntity().hasPermissions(Config.GENERAL.opsPermissionLevel.get())
        ) {
            return;
        }

        // prevent protected blocks from placing
        if (event.getEntity() instanceof Player) {
            if (!ParcelRegistry.hasAccess(Coords.of(event.getPos()), event.getEntity().getUUID(), ((Player) event.getEntity()).getItemInHand(InteractionHand.MAIN_HAND))) {
                event.setCanceled(true);
                if (ClaimMyLand.LOGGER.isDebugEnabled()) {
                    ClaimMyLand.LOGGER.debug("denied block place -> {} @ {}", event.getEntity().getDisplayName().getString(), new Coords(event.getPos()).toShortString());
                }
                if (!event.getLevel().isClientSide()) {
                    sendProtectedMessage(event.getLevel(), (Player) event.getEntity());
                }
            }
        }
        else if (ParcelRegistry.intersectsParcel(Coords.of(event.getPos()))) {
            event.setCanceled(true);
        }

        ClaimMyLand.LOGGER.debug("allowed to place ??");
    }

    /**
     * TODO move to MessageUtil class
     * @param world
     * @param player
     */
    private static void sendProtectedMessage(LevelAccessor world, Player player) {
        if (world.isClientSide() && Config.CLIENT.gui.enableProtectionChatMessages.get()) {
            player.sendSystemMessage((Component.translatable(LangUtil.chat("block_protected")).withStyle(new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.ITALIC})));
        }
    }
}
