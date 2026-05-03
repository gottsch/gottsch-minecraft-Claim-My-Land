/*
 * This file is part of  Claim My Land.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
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
package mod.gottsch.neo.claimmyland.core.block.entity;

import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.neo.claimmyland.core.command.helper.PlayerMessageHelper;
import mod.gottsch.neo.claimmyland.core.network.CMLNetwork;
import mod.gottsch.neo.claimmyland.core.parcel.Parcel;
import mod.gottsch.neo.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.neo.claimmyland.core.setup.Registration;
import mod.gottsch.neo.gottschcore.spatial.Coords;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Block entity for {@link RenameSignBlock}. Captures the parcel ID and owner
 * player ID when the rename workflow is initiated by {@link BorderStoneBlock#useItemOn},
 * then executes the rename when the player submits the sign editor via
 * {@link #updateText}.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>Block placed programmatically by {@code BorderStoneBlock.useItemOn}.</li>
 *   <li>{@code parcelId} and {@code ownerPlayerId} set immediately after placement.</li>
 *   <li>Sign editor opened for the owner player.</li>
 *   <li>Player submits — vanilla fires {@code ServerboundSignUpdatePacket} →
 *       {@link #updateText} is called on the server.</li>
 *   <li>{@code updateText} renames the parcel, syncs to tracking players, sends
 *       a feedback message, and removes this block from the world.</li>
 * </ol>
 *
 * <p>The two UUIDs are persisted so that a server restart between placement and
 * submission (unlikely but possible) does not silently corrupt world state.
 *
 * @author Mark Gottschling on Apr 16, 2026
 */
public class RenameSignBlockEntity extends SignBlockEntity {

    private static final String TAG_PARCEL_ID = "ParcelId";
    private static final String TAG_OWNER_PLAYER_ID = "OwnerPlayerId";

    /** The parcel whose name will be updated when the sign is submitted. */
    private UUID parcelId;

    /** The UUID of the player who initiated the rename. Used to verify ownership and send feedback. */
    private UUID ownerPlayerId;

    public RenameSignBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RENAME_SIGN_BLOCK_ENTITY.get(), pos, state);
    }

    // -------------------------------------------------------------------------
    // Core rename logic
    // -------------------------------------------------------------------------

    @Override
    public void updateSignText(Player player, boolean isFrontText, List<FilteredText> filteredText) {
        // Let vanilla commit the text first
        super.updateSignText(player, isFrontText, filteredText);

        if (!(level instanceof ServerLevel serverLevel)) return;

        // Build name directly from filteredText parameter — raw player input,
        // no dependency on SignText state readback
        String newName = buildName(filteredText);

        ClaimMyLand.LOGGER.debug("CMLRenameSign: updateSignText called, newName='{}'", newName);

        ServerPlayer owner = ownerPlayerId != null
                ? serverLevel.getServer().getPlayerList().getPlayer(ownerPlayerId)
                : null;

        if (newName.isEmpty()) {
            if (owner != null) PlayerMessageHelper.sendFailure(owner, "parcel.rename.sign.empty");
            serverLevel.removeBlock(getBlockPos(), false);
            return;
        }

        Optional<Parcel> parcelOpt = ParcelRegistry.findByParcelId(parcelId);
        if (parcelOpt.isEmpty()) {
            ClaimMyLand.LOGGER.warn("CMLRenameSign: parcel {} not found", parcelId);
            serverLevel.removeBlock(getBlockPos(), false);
            return;
        }

        Parcel parcel = parcelOpt.get();

        if (ownerPlayerId == null || !ownerPlayerId.equals(parcel.getEstate().getOwnerId())) {
            if (owner != null) PlayerMessageHelper.sendFailure(owner, "parcel.rename.sign.not_owner");
            serverLevel.removeBlock(getBlockPos(), false);
            return;
        }

        parcel.setName(newName);
        CommandHelper.save(serverLevel);
        CMLNetwork.syncParcelToTrackingPlayers(serverLevel, parcel);

        // If the owner is currently standing in this parcel, refresh their HUD cache
        if (owner != null) {
            boolean ownerInParcel = ParcelRegistry.find(
                            Coords.of((int) owner.getX(), (int) owner.getY(), (int) owner.getZ()),
                            serverLevel.dimension().location().toString())
                    .stream()
                    .anyMatch(p -> p.getId().equals(parcel.getId()));
            if (ownerInParcel) {
                CMLNetwork.syncCacheToPlayer(owner, parcel);
            }
        }
        if (owner != null) CMLNetwork.syncParcelToPlayer(serverLevel, owner, parcel);
        if (owner != null) PlayerMessageHelper.sendSuccess(owner, "parcel.rename.sign.success", (Object) newName);

        serverLevel.removeBlock(getBlockPos(), false);
    }



    /**
     * Concatenates all non-empty lines from the sign text with a single space,
     * trimming leading/trailing whitespace from each line and the result.
     *
     * @param filteredText the text submitted by the player
     * @return the concatenated name, or an empty string if all lines were blank
     */
    private static String buildName(List<FilteredText> filteredText) {
        StringBuilder sb = new StringBuilder();
        for (FilteredText ft : filteredText) {
            String line = ft.raw().trim();
            if (!line.isEmpty()) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(line);
            }
        }
        return sb.toString().trim();
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (parcelId != null) tag.putUUID(TAG_PARCEL_ID, parcelId);
        if (ownerPlayerId != null) tag.putUUID(TAG_OWNER_PLAYER_ID, ownerPlayerId);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.hasUUID(TAG_PARCEL_ID)) parcelId = tag.getUUID(TAG_PARCEL_ID);
        if (tag.hasUUID(TAG_OWNER_PLAYER_ID)) ownerPlayerId = tag.getUUID(TAG_OWNER_PLAYER_ID);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public UUID getParcelId() { return parcelId; }
    public void setParcelId(UUID parcelId) { this.parcelId = parcelId; }

    public UUID getOwnerPlayerId() { return ownerPlayerId; }
    public void setOwnerPlayerId(UUID ownerPlayerId) { this.ownerPlayerId = ownerPlayerId; }
}