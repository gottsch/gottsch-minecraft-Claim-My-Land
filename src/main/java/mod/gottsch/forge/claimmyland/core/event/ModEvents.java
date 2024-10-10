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
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.event.entity.living.LivingDestroyBlockEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.level.PistonEvent;
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
//            ClaimMyLand.LOGGER.debug("attempt to break block by player -> {} @ {}", event.getPlayer().getDisplayName().getString(), Coords.of(event.getPos()).toShortString());
        }

        // execute if event is enabled
        if (!Config.SERVER.protection.enableBlockBreakEvent.get()) {
            return;
        }

        // check dimension
        if (((Level)event.getLevel()).dimensionTypeId() != BuiltinDimensionTypes.OVERWORLD) {
            return;
        }

        // prevent protected blocks from breaking
        if (!ParcelRegistry.hasAccess(Coords.of(event.getPos()), event.getPlayer().getUUID())) {
            event.setCanceled(true);
            if (ClaimMyLand.LOGGER.isDebugEnabled()) {
//                ClaimMyLand.LOGGER.debug("denied block break -> {} @ {}", event.getPlayer().getDisplayName().getString(), Coords.of(event.getPos()).toShortString());
            }
            if (!event.getLevel().isClientSide()) {
                sendProtectedMessage(event.getLevel(), event.getPlayer());
            }
        }
    }

    @SubscribeEvent
    public void onBlockPlace(final BlockEvent.EntityPlaceEvent event) {
        if (ClaimMyLand.LOGGER.isDebugEnabled()) {
//            ClaimMyLand.LOGGER.debug("attempt to place block by player -> {} @ {}", event.getEntity().getDisplayName().getString(), Coords.of(event.getPos()).toShortString());
        }

        if (!Config.SERVER.protection.enableEntityPlaceEvent.get()
//				|| event.getEntity().hasPermissions(Config.GENERAL.opsPermissionLevel.get())
        ) {
            return;
        }

        // check dimension
        if (((Level)event.getLevel()).dimensionTypeId() != BuiltinDimensionTypes.OVERWORLD) {
            return;
        }

        // prevent protected blocks from placing
        if (event.getEntity() instanceof Player) {
            if (!ParcelRegistry.hasAccess(Coords.of(event.getPos()), event.getEntity().getUUID(), ((Player) event.getEntity()).getItemInHand(InteractionHand.MAIN_HAND))) {
                event.setCanceled(true);
                if (ClaimMyLand.LOGGER.isDebugEnabled()) {
//                    ClaimMyLand.LOGGER.debug("denied block place -> {} @ {}", event.getEntity().getDisplayName().getString(), Coords.of(event.getPos()).toShortString());
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

    @SubscribeEvent
    public void onMutliBlockPlace(final BlockEvent.EntityMultiPlaceEvent event) {
        if (!Config.SERVER.protection.enableEntityMultiPlaceEvent.get()
                || event.getEntity().hasPermissions(Config.SERVER.general.opsPermissionLevel.get()) ) {
            return;
        }

        // check dimension
        if (((Level)event.getLevel()).dimensionTypeId() != BuiltinDimensionTypes.OVERWORLD) {
            return;
        }

        // prevent parcel blocks from breaking
        if (event.getEntity() instanceof Player) {
            if (!ParcelRegistry.hasAccess(Coords.of(event.getPos()), event.getEntity().getUUID())) {
                event.setCanceled(true);
                if (!event.getLevel().isClientSide()) {
                    if (ClaimMyLand.LOGGER.isDebugEnabled()) {
                        ClaimMyLand.LOGGER.debug("denied multi-block place -> {} @ {}", event.getEntity().getDisplayName().getString(), new Coords(event.getPos()).toShortString());
                    }
                    sendProtectedMessage(event.getLevel(), (Player) event.getEntity());
                }
            }
        }
        else if (ParcelRegistry.intersectsParcel(Coords.of(event.getPos()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onToolInteract(final BlockEvent.BlockToolModificationEvent event) {
        if (!Config.SERVER.protection.enableBlockToolInteractEvent.get()
                || event.getPlayer().hasPermissions(Config.SERVER.general.opsPermissionLevel.get())) {
            return;
        }

        // check dimension
        if (((Level)event.getLevel()).dimensionTypeId() != BuiltinDimensionTypes.OVERWORLD) {
            return;
        }

        ItemStack heldItemStack = event.getHeldItemStack();
        if (!ParcelRegistry.hasAccess(Coords.of(event.getPos()), event.getPlayer().getUUID(), heldItemStack)) {
            event.setCanceled(true);
            if (ClaimMyLand.LOGGER.isDebugEnabled()) {
                ClaimMyLand.LOGGER.debug("denied tool interact -> {}", event.getPlayer().getDisplayName().getString(), Coords.of(event.getPos()).toShortString());
            }
            if (!event.getLevel().isClientSide()) {
                sendProtectedMessage(event.getLevel(), event.getPlayer());
            }
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(final PlayerInteractEvent.RightClickBlock event) {
        if (!Config.SERVER.protection.enableRightClickBlockEvent.get()
                || event.getEntity().hasPermissions(Config.SERVER.general.opsPermissionLevel.get())) {
            return;
        }

        // check dimension
        if (((Level)event.getLevel()).dimensionTypeId() != BuiltinDimensionTypes.OVERWORLD) {
            return;
        }

        // ensure to check entity, because mobs like Enderman can pickup/place blocks
        if (event.getEntity() instanceof Player) {

            // get the item in the player's hand
            if (!ParcelRegistry.hasAccess(Coords.of(event.getPos()), event.getEntity().getUUID())) {
                event.setCanceled(true);
                if (ClaimMyLand.LOGGER.isDebugEnabled()) {
                    ClaimMyLand.LOGGER.debug("denied right click -> {} @ {} w/ hand -> {}", event.getEntity().getDisplayName().getString(), Coords.of(event.getPos()).toShortString(), event.getHand().toString());
                }
                if (event.getHand() == InteractionHand.MAIN_HAND) { // reduces to only 1 message per action
                    if (!event.getLevel().isClientSide()) {
                        sendProtectedMessage(event.getLevel(), (Player) event.getEntity());
                    }
                }
            }
        }
        else if (ParcelRegistry.intersectsParcel(Coords.of(event.getPos()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLivingDestroyBlock(final LivingDestroyBlockEvent event) {
        // prevent protected blocks from breaking by mob action
        if (Config.SERVER.protection.enableLivingDestroyBlockEvent.get()
                && ParcelRegistry.intersectsParcel(Coords.of(event.getPos()))) {
            // check dimension
            if (event.getEntity().level().dimensionTypeId() == BuiltinDimensionTypes.OVERWORLD) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onPiston(final PistonEvent.Pre event) {
        if (!Config.SERVER.protection.enablePistionEvent.get()) {
            return;
        }

        // check dimension
        if (((Level)event.getLevel()).dimensionTypeId() != BuiltinDimensionTypes.OVERWORLD) {
            return;
        }

        if (event.getDirection() == Direction.UP || event.getDirection() == Direction.DOWN) {
            return;
        }

        // check if piston itself is inside protected area - if so, exit ie. allow movement
        if (ParcelRegistry.intersectsParcel(Coords.of(event.getPos()))) {
            return;
        }

        if (event.getPistonMoveType() == PistonEvent.PistonMoveType.EXTEND) {
            for (int count = 1; count <=12; count++) {
                int xOffset = 0;
                int zOffset = 0;
                int xPush = 0;
                int zPush = 0;
                switch(event.getDirection()) {
                    default:
                    case NORTH:
                        zOffset = -count;
                        zPush = -1;
                        break;
                    case SOUTH:
                        zOffset = count;
                        zPush = +1;
                        break;
                    case WEST:
                        xOffset = -count;
                        xPush = -1;
                        break;
                    case EAST:
                        xOffset = count;
                        xPush = 1;
                        break;
                }

                if (event.getLevel().getBlockState(event.getPos().offset(xOffset, 0, zOffset)).isSolid()) {
                    // prevent protected blocks from breaking
                    if (ParcelRegistry.intersectsParcel(Coords.of(event.getPos().offset(xOffset, 0, zOffset))) ||
                            ParcelRegistry.intersectsParcel(Coords.of(event.getPos().offset(xOffset + xPush, 0, zOffset + zPush)))) {
                        event.setCanceled(true);
                        return;
                    }
                }
                else {
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public void onExplosion(final ExplosionEvent.Detonate event) {
        // remove any affected blocks that are protected
        event.getAffectedBlocks().removeIf(block -> {
            // prevent protected blocks from breaking
            return Config.SERVER.protection.enableExplosionDetonateEvent.get()
                    && event.getLevel().dimensionTypeId() != BuiltinDimensionTypes.OVERWORLD
                    && ParcelRegistry.intersectsParcel(Coords.of(block.getX(), block.getY(), block.getZ()));
        });
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
