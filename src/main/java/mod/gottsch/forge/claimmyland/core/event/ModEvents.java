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
import mod.gottsch.forge.claimmyland.core.block.entity.BorderStoneBlockEntity;
import mod.gottsch.forge.claimmyland.core.command.helper.PlayerMessageHelper;
import mod.gottsch.forge.claimmyland.core.config.Config;
import mod.gottsch.forge.claimmyland.core.network.CMLNetwork;
import mod.gottsch.forge.claimmyland.core.persistence.PersistedData;
import mod.gottsch.forge.claimmyland.core.registry.ActiveBorderStoneRegistry;
import mod.gottsch.forge.claimmyland.core.registry.ParcelChunkIndex;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.gottschcore.spatial.Coords;
import mod.gottsch.forge.gottschcore.world.WorldInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDestroyBlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.*;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mark Gottschling on Sep 14, 2024
 */
@Mod.EventBusSubscriber(modid = ClaimMyLand.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {

    private static final int RESYNC_INTERVAL = 6000; // 5 minutes @ 20 ticks/sec
    private static final Map<ResourceKey<Level>, Integer> resyncTickCounters = new ConcurrentHashMap<>();

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
    public static void onPlayerLogout(final PlayerEvent.PlayerLoggedOutEvent event) {
        ParcelRegistry.REGION_CACHE.invalidatePlayer(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerLogin(final PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ParcelRegistry.REGION_CACHE.invalidatePlayer(player.getUUID()); // ← ADD: force tick handler to BST on first tick
        // Bulk sync all parcels to the joining player so their ClientParcelRegistry
        // is populated immediately. CacheSyncPackets will keep it updated from there.
        CMLNetwork.syncAllParcelsToPlayer(player);
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(final PlayerEvent.PlayerChangedDimensionEvent event) {
        ParcelRegistry.REGION_CACHE.invalidatePlayer(event.getEntity().getUUID());
    }

    /**
     * Tracks which parcel each player is currently inside and sends a
     * CacheSyncPacket to their client whenever that changes.
     *
     * This is what drives ClientParcelCache during normal movement —
     * without it the HUD has no data. Protection checks also call
     * resolveParcelCached(), but only when the player actually interacts
     * with a block. This tick handler ensures the cache stays current
     * even when the player is just standing or walking.
     *
     * Performance notes:
     *   - END phase only  → fires once per tick, not twice
     *   - Chunk pre-filter → O(1) HashMap lookup before touching the BST
     *   - isCached() guard → wilderness packet sent only on transition,
     *     not every tick when the player is already in open land
     *   - resolveParcelCached() cache hit → bounds check only, no BST,
     *     no packet; packet only sent on boundary crossing (cache miss)
     *
     * @author by Mark Gottschling on Mar 06, 2026
     */
    @SubscribeEvent
    public static void onPlayerTick(final TickEvent.PlayerTickEvent event) {
        // Once per tick (END phase), server side only
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        BlockPos pos = player.blockPosition();

        // Dimension guard — unprotected dimension, clear client cache on transition
        if (!isInProtectedDimension(player.level())) {
            if (ParcelRegistry.REGION_CACHE.isCached(player.getUUID())) {
                ParcelRegistry.REGION_CACHE.invalidatePlayer(player.getUUID());
                CMLNetwork.syncWildernessToPlayer(player);
            }
            return;
        }

        String dimension = getDimensionString(player.level());

        // Chunk pre-filter — if this chunk has no parcels, handle wilderness
        // transition without touching the BST at all
        if (!ParcelChunkIndex.isChunkClaimed(pos.getX(), pos.getZ(), dimension)) {
            if (ParcelRegistry.REGION_CACHE.isCached(player.getUUID())) {
                ParcelRegistry.REGION_CACHE.invalidatePlayer(player.getUUID());
                CMLNetwork.syncWildernessToPlayer(player);
            }
            return;
        }

        // resolveParcelCached() does everything from here:
        //   cache hit  → bounds check only, no BST, no packet (client already knows)
        //   cache miss → BST query, updates server cache, sends CacheSyncPacket
        //   no parcel  → invalidates server cache, sends wilderness packet
//        ParcelRegistry.resolveParcelCached(player, Coords.of(pos), dimension);
        ParcelRegistry.syncParcelToClient(player, Coords.of(pos), dimension);
    }

    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        ChunkPos chunk = event.getPos();
        ServerPlayer player = event.getPlayer();
        Set<BorderStoneBlockEntity> stones = ActiveBorderStoneRegistry.getInChunk(chunk);
        if (stones.isEmpty()) return;

        for (BorderStoneBlockEntity stone : stones) {
            if (stone.getParcelId() == null || stone.getLevel() == null) continue;
            if (!(stone.getLevel() instanceof ServerLevel serverLevel)) continue;
            ParcelRegistry.findByParcelId(stone.getParcelId()).ifPresent(parcel -> {
                // resync full parcel state (name, ownership, etc.) to this player
                CMLNetwork.syncParcelToPlayer(serverLevel, player, parcel);
                // resync border visibility
                int conflictState = ParcelRegistry.resolveConflictState(
                        stone.getAbsoluteBox(), parcel.getEstate().getOwnerId(),
                        parcel.getId(), parcel.getType());
                CMLNetwork.syncBorderVisibilityToPlayer(player,
                        parcel.getId(), true, conflictState, stone.getBlockPos().getY());
            });
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        event.getServer().getAllLevels().forEach(level -> {
            if (level.players().isEmpty()) return;
            int count = resyncTickCounters.merge(level.dimension(), 1, Integer::sum);
            if (count >= RESYNC_INTERVAL) {
                resyncTickCounters.put(level.dimension(), 0);
                CMLNetwork.periodicResync(level);
            }
        });
    }

    @SubscribeEvent
    public static void onBlockBreak(final BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        // chunk pre-filter
        BlockPos pos = event.getPos();
        String dimension = getDimensionString(event.getLevel());
        if (!ParcelChunkIndex.isChunkClaimed(pos.getX(), pos.getZ(), dimension)) {
            return; // O(1) — this chunk has no parcels, skip BST entirely
        }

        // execute if event is enabled
        if (!Config.SERVER.protection.enableBlockBreakEvent.get() || hasOpsPermission(event.getPlayer())) {
            return;
        }

        // check dimension
        if (!isInProtectedDimension(event.getLevel())) {
            return;
        }

        // prevent protected blocks from breaking
//        if (!ParcelRegistry.hasAccess(Coords.of(event.getPos()), event.getPlayer().getUUID())) {
        if (!ParcelRegistry.hasAccess(
                (ServerPlayer) event.getPlayer(),
                Coords.of(event.getPos()),
                getDimensionString(event.getLevel()))) {

            event.setCanceled(true);
//            if (ClaimMyLand.LOGGER.isDebugEnabled()) {
//                ClaimMyLand.LOGGER.debug("denied block break -> {} @ {}", event.getPlayer().getDisplayName().getString(), Coords.of(event.getPos()).toShortString());
//            }
//            if (!event.getLevel().isClientSide()) {
//                sendProtectedMessage(event.getLevel(), event.getPlayer());
//            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(final BlockEvent.EntityPlaceEvent event) {

        if (event.getLevel().isClientSide()) {
            return;
        }

        ClaimMyLand.LOGGER.debug("player is attempting to place block");
        ClaimMyLand.LOGGER.debug("onBlockPlace — entity={}, block={}, pos={}",
                event.getEntity(), event.getPlacedBlock().getBlock(), event.getPos());

        // chunk pre-filter
        BlockPos pos = event.getPos();
        String dimension = getDimensionString(event.getLevel());
        if (!ParcelChunkIndex.isChunkClaimed(pos.getX(), pos.getZ(), dimension)) {
            return; // O(1) — this chunk has no parcels, skip BST entirely
        }

        if (!Config.SERVER.protection.enableEntityPlaceEvent.get()
                || (event.getEntity() instanceof Player && hasOpsPermission((Player) event.getEntity()))) {
            return;
        }

        // check dimension
        if (!isInProtectedDimension(event.getLevel())) {
            return;
        }

        // prevent protected blocks from placing
        if (event.getEntity() instanceof Player player) {
            if (!ParcelRegistry.hasAccess(
                    (ServerPlayer) event.getEntity(),
                    Coords.of(event.getPos()),
                    getDimensionString(event.getLevel()),
                    ((Player) event.getEntity()).getItemInHand(InteractionHand.MAIN_HAND))) {

                event.setCanceled(true);
                /// //
                if (ClaimMyLand.LOGGER.isDebugEnabled()) {
                    ClaimMyLand.LOGGER.debug("denied block place -> {} @ {}", event.getEntity().getDisplayName().getString(), Coords.of(event.getPos()).toShortString());
                }
//                if (!event.getLevel().isClientSide()) {
//                    sendProtectedMessage(event.getLevel(), (Player) event.getEntity());
//                }
                /// //
                PlayerMessageHelper.sendFailure(player, "parcel.place_block.block_claimed");

            }
        } else {
            // non-player entity placement (includes natural fire spread)
            if (ParcelRegistry.isFireSpreadPrevented(Coords.of(event.getPos()), event.getState(), dimension)) {
                event.setCanceled(true);
            } else if (ParcelRegistry.intersectsParcel(Coords.of(event.getPos()), dimension)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onMutliBlockPlace(final BlockEvent.EntityMultiPlaceEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        // chunk pre-filter
        BlockPos pos = event.getPos();
        String dimension = getDimensionString(event.getLevel());
        if (!ParcelChunkIndex.isChunkClaimed(pos.getX(), pos.getZ(), dimension)) {
            return; // O(1) — this chunk has no parcels, skip BST entirely
        }

//        if (ClaimMyLand.LOGGER.isDebugEnabled()) {
//            ClaimMyLand.LOGGER.debug("attempt to place multi-block by player -> {} @ {}", event.getEntity().getDisplayName().getString(), Coords.of(event.getPos()).toShortString());
//        }

        if (!Config.SERVER.protection.enableEntityMultiPlaceEvent.get()
                || hasOpsPermission((Player) event.getEntity())) {
            return;
        }

        // check dimension
        if (!isInProtectedDimension(event.getLevel())) {
            return;
        }

        // prevent parcel blocks from breaking
        if (event.getEntity() instanceof Player) {

//            ItemStack heldItem = ((Player)event.getEntity()).getItemInHand(InteractionHand.MAIN_HAND);
//                ClaimMyLand.LOGGER.debug("player -> {} is hold item in main hand -> {}", event.getEntity().getDisplayName().getString(), ((Player)event.getEntity()).getItemInHand(InteractionHand.MAIN_HAND));
//             // TODO check other hand
//            if (heldItem == ItemStack.EMPTY) {
//                // TODO
//            }
//            BlockState state = event.getLevel().getBlockState(event.getPos());
//
//            if (!ParcelRegistry.hasAccess(Coords.of(event.getPos()), event.getEntity().getUUID(), state, heldItem)) {
//            if (!ParcelRegistry.hasAccess(Coords.of(event.getPos()), event.getEntity().getUUID(), ((Player) event.getEntity()).getItemInHand(InteractionHand.MAIN_HAND))) {
            if (!ParcelRegistry.hasAccess(
                    (ServerPlayer) event.getEntity(),
                    Coords.of(event.getPos()),
                    getDimensionString(event.getLevel()),
                    ((Player) event.getEntity()).getItemInHand(InteractionHand.MAIN_HAND))) {

                event.setCanceled(true);
                if (!event.getLevel().isClientSide()) {
                    if (ClaimMyLand.LOGGER.isDebugEnabled()) {
                        ClaimMyLand.LOGGER.debug("denied multi-block place -> {} @ {}", event.getEntity().getDisplayName().getString(), new Coords(event.getPos()).toShortString());
                    }
//                    sendProtectedMessage(event.getLevel(), (Player) event.getEntity());
                }
            }
        } else if (ParcelRegistry.intersectsParcel(Coords.of(event.getPos()), dimension)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onToolInteract(final BlockEvent.BlockToolModificationEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        // chunk pre-filter
        BlockPos pos = event.getPos();
        String dimension = getDimensionString(event.getLevel());
        if (!ParcelChunkIndex.isChunkClaimed(pos.getX(), pos.getZ(), dimension)) {
            return; // O(1) — this chunk has no parcels, skip BST entirely
        }

        if (!Config.SERVER.protection.enableBlockToolInteractEvent.get()
                || (event.getPlayer() != null && hasOpsPermission(event.getPlayer()))) {
            return;
        }

        // check dimension
        if (!isInProtectedDimension(event.getLevel())) {
            return;
        }

        ItemStack heldItemStack = event.getHeldItemStack();
//        ClaimMyLand.LOGGER.debug("player -> {} is hold item in main hand -> {}", event.getPlayer().getDisplayName().getString(), ((Player)event.getPlayer()).getItemInHand(InteractionHand.MAIN_HAND));

//        if (!ParcelRegistry.hasAccess(Coords.of(event.getPos()), event.getPlayer().getUUID(), heldItemStack)) {
        if (!ParcelRegistry.hasAccess(
                (ServerPlayer) event.getPlayer(),
                Coords.of(event.getPos()),
                getDimensionString(event.getLevel()),
                heldItemStack)) {
            event.setCanceled(true);
            if (ClaimMyLand.LOGGER.isDebugEnabled()) {
                ClaimMyLand.LOGGER.debug("denied tool interact -> {}", event.getPlayer().getDisplayName().getString(), Coords.of(event.getPos()).toShortString());
            }
//            if (!event.getLevel().isClientSide()) {
//                sendProtectedMessage(event.getLevel(), event.getPlayer());
//            }
        }
    }

    // TODO this needs  a special case because it checks the whitelists, whereas other event do not
    @SubscribeEvent
    public static void onPlayerInteractBlock(final PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) {
            return;
        }

        // chunk pre-filter
        BlockPos pos = event.getPos();
        String dimension = getDimensionString(event.getLevel());
        if (!ParcelChunkIndex.isChunkClaimed(pos.getX(), pos.getZ(), dimension)) {
            return; // O(1) — this chunk has no parcels, skip BST entirely
        }

        if (!Config.SERVER.protection.enableRightClickBlockEvent.get()
                || hasOpsPermission(event.getEntity())) {
            return;
        }

        // check dimension
        if (!isInProtectedDimension(event.getLevel())) {
            return;
        }

        // ensure to check entity, because mobs like Enderman can pickup/place blocks
        if (event.getEntity() instanceof Player) {

            ItemStack heldItem = ItemStack.EMPTY;
            if (event.getHand() == InteractionHand.MAIN_HAND) {
                heldItem = ((Player) event.getEntity()).getItemInHand(InteractionHand.MAIN_HAND);
                ClaimMyLand.LOGGER.debug("player -> {} is hold item in main hand -> {}", event.getEntity().getDisplayName().getString(), ((Player) event.getEntity()).getItemInHand(InteractionHand.MAIN_HAND));
            } // TODO check other hand

//            ClaimMyLand.LOGGER.info("event.pos -> {}", event.getPos());
//            ClaimMyLand.LOGGER.info("event.face -> {}", event.getFace());
//            ClaimMyLand.LOGGER.info("event.placement pos -> {}", event.getPos().relative(event.getFace()));

            BlockState state = event.getLevel().getBlockState(event.getPos());
//            if (!ParcelRegistry.hasInteractAccess(Coords.of(event.getPos()), event.getEntity().getUUID(), state, heldItem)) {
            if (!ParcelRegistry.hasInteractAccess(
                    (ServerPlayer) event.getEntity(),
                    Coords.of(event.getPos().relative(event.getFace())),
                    getDimensionString(event.getLevel()),
                    state,
                    heldItem)) {

                event.setCanceled(true);
                if (ClaimMyLand.LOGGER.isDebugEnabled()) {
                    ClaimMyLand.LOGGER.debug("denied right click -> {} @ {} w/ hand -> {}", event.getEntity().getDisplayName().getString(), Coords.of(event.getPos()).toShortString(), event.getHand().toString());
                }
//                if (event.getHand() == InteractionHand.MAIN_HAND) { // reduces to only 1 message per action
//                    if (!event.getLevel().isClientSide()) {
//                        sendProtectedMessage(event.getLevel(), (Player) event.getEntity());
//                    }
//                }
            }
        } else if (ParcelRegistry.intersectsParcel(Coords.of(event.getPos()), dimension)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerInteractItem(final PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide) {
            return;
        }

        // chunk pre-filter
        BlockPos pos = event.getPos();
        String dimension = getDimensionString(event.getLevel());
        if (!ParcelChunkIndex.isChunkClaimed(pos.getX(), pos.getZ(), dimension)) {
            return; // O(1) — this chunk has no parcels, skip BST entirely
        }

        // TEMP log
        ClaimMyLand.LOGGER.debug("player -> {} attempting to use item -> {}", event.getEntity().getDisplayName().getString(), ((Player) event.getEntity()).getItemInHand(InteractionHand.MAIN_HAND));
        if (!Config.SERVER.protection.enableRightClickItemEvent.get() || hasOpsPermission(event.getEntity())) {
            return;
        }

        // check dimension
        if (!isInProtectedDimension(event.getLevel())) {
            return;
        }

//        if (event.getEntity() instanceof Player) {

        ItemStack heldItem = ItemStack.EMPTY;
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            heldItem = ((Player) event.getEntity()).getItemInHand(InteractionHand.MAIN_HAND);
            ClaimMyLand.LOGGER.debug("player -> {} is hold item in main hand -> {}", event.getEntity().getDisplayName().getString(), ((Player) event.getEntity()).getItemInHand(InteractionHand.MAIN_HAND));
        } // TODO check other hand

//        if (!ParcelRegistry.hasInteractAccess(Coords.of(event.getPos()), event.getEntity().getUUID(), heldItem)) {
        if (!ParcelRegistry.hasInteractAccess(
                (ServerPlayer) event.getEntity(),
                Coords.of(event.getPos()),
                getDimensionString(event.getLevel()),
                heldItem)) {
            event.setCanceled(true);
            if (ClaimMyLand.LOGGER.isDebugEnabled()) {
                ClaimMyLand.LOGGER.debug("denied right click -> {} @ {} w/ hand -> {}", event.getEntity().getDisplayName().getString(), Coords.of(event.getPos()).toShortString(), event.getHand().toString());
            }
//                if (event.getHand() == InteractionHand.MAIN_HAND) { // reduces to only 1 message per action
//                    if (!event.getLevel().isClientSide()) {
//                        sendProtectedMessage(event.getLevel(), (Player) event.getEntity());
//                    }
//                }
//            }
        }
//        else if (ParcelRegistry.intersectsParcel(Coords.of(event.getPos()))) {
//            event.setCanceled(true);
//        }
    }

    @SubscribeEvent
    public static void onLivingDestroyBlock(final LivingDestroyBlockEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        // chunk pre-filter
        BlockPos pos = event.getPos();
        String dimension = getDimensionString(event.getEntity().level());
        if (!ParcelChunkIndex.isChunkClaimed(pos.getX(), pos.getZ(), dimension)) {
            return; // O(1) — this chunk has no parcels, skip BST entirely
        }

        // prevent protected blocks from breaking by mob action
        if (Config.SERVER.protection.enableLivingDestroyBlockEvent.get()
                && ParcelRegistry.intersectsParcel(Coords.of(event.getPos()), dimension)) {
            // check dimension
            if (!isInProtectedDimension(event.getEntity().level())) {
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onPiston(final PistonEvent.Pre event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        // chunk pre-filter
        BlockPos pos = event.getPos();
        String dimension = getDimensionString(event.getLevel());
        if (!ParcelChunkIndex.isChunkClaimed(pos.getX(), pos.getZ(), dimension)) {
            return; // O(1) — this chunk has no parcels, skip BST entirely
        }

        if (!Config.SERVER.protection.enablePistionEvent.get()) {
            return;
        }

        // check dimension
        if (!isInProtectedDimension(event.getLevel())) {
            return;
        }

        if (event.getDirection() == Direction.UP || event.getDirection() == Direction.DOWN) {
            return;
        }

        // check if piston itself is inside protected area - if so, exit ie. allow movement
        if (ParcelRegistry.intersectsParcel(Coords.of(event.getPos()), dimension)) {
            return;
        }

        if (event.getPistonMoveType() == PistonEvent.PistonMoveType.EXTEND) {
            for (int count = 1; count <= 12; count++) {
                int xOffset = 0;
                int zOffset = 0;
                int xPush = 0;
                int zPush = 0;
                switch (event.getDirection()) {
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
                    if (ParcelRegistry.intersectsParcel(Coords.of(event.getPos().offset(xOffset, 0, zOffset)), dimension) ||
                            ParcelRegistry.intersectsParcel(Coords.of(event.getPos().offset(xOffset + xPush, 0, zOffset + zPush)), dimension)) {
                        event.setCanceled(true);
                        return;
                    }
                } else {
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onExplosion(final ExplosionEvent.Detonate event) {
        String dimension = getDimensionString(event.getLevel());
        List<BlockPos> affectedBlocks = event.getAffectedBlocks();
        affectedBlocks.removeIf(pos ->
                !ParcelChunkIndex.isChunkClaimed(pos.getX(), pos.getZ(), dimension)
        );
        // remove any affected blocks that are protected
        affectedBlocks.removeIf(block -> {
            // prevent protected blocks from breaking
            return Config.SERVER.protection.enableExplosionDetonateEvent.get()
//                    && event.getLevel().dimensionTypeId() != BuiltinDimensionTypes.OVERWORLD
                    && !isInProtectedDimension(event.getLevel())
                    && ParcelRegistry.intersectsParcel(Coords.of(block.getX(), block.getY(), block.getZ()), dimension);
        });
    }


//    @SubscribeEvent
//    public static void onPlayerUseItem(final LivingEntityUseItemEvent event) {
////        if (event.getEntity().level().isClientSide()) {
////            return;
////        }
//
//        // TODO finish
//    }

    /**
     * Ops permission.
     *
     * @param player
     * @return
     */
    private static boolean hasOpsPermission(Player player) {
        return player.hasPermissions(Config.SERVER.general.opsPermissionLevel.get());
        // TODO are part of the ops/admin config list
    }

//    /**
//     * Most powerful permission. Can do anything, anywhere, regardless of parcel protections.
//     * @param player
//     * @return
//     */
//    private static boolean hasDeityPermission(Player player) {
//        // has Ops AND is Creative mod
//        return hasOpsPermission(player) && player.getAbilities().instabuild;
//    }
//
//    /**
//     * TODO move to MessageUtil class
//     * @param world
//     * @param player
//     */
//    @Deprecated
//    private static void sendProtectedMessage(LevelAccessor world, Player player) {
//        if (world.isClientSide() && Config.CLIENT.gui.enableProtectionChatMessages.get()) {
//            player.sendSystemMessage((Component.translatable(LangUtil.chat("block_protected")).withStyle(new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.ITALIC})));
//        }
//    }

    private static boolean isInProtectedDimension(LevelAccessor level) {
        ResourceLocation dimId = ((Level) level).dimension().location();
        List<? extends String> excluded = Config.SERVER.dimensions.excludedDimensions.get();
        return excluded.stream().noneMatch(e -> e.equals(dimId.toString()));
    }

    private static String getDimensionString(LevelAccessor level) {
        return ((Level) level).dimension().location().toString();
        // Returns e.g. "minecraft:overworld", "minecraft:the_nether", etc.
    }
}
