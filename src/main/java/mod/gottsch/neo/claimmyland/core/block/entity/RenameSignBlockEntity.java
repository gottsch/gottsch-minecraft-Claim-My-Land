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
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;

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

    /**
     * Called by vanilla when the player submits the sign editor
     * ({@code ServerboundSignUpdatePacket}). Calls {@code super.updateText()} first
     * to commit the player's input into the {@link SignText} state, then reads the
     * populated text to build the new parcel name.
     *
     * <p>Always removes the block on return — whether the rename succeeded or failed —
     * so the temporary sign never remains in the world.
     */
    @Override
    public boolean updateText(UnaryOperator<SignText> updater, boolean isFrontText) {
        ClaimMyLand.LOGGER.debug("CMLRenameSign: updateText called on thread: {}",
                Thread.currentThread().getName());
        super.updateText()
        // Call super first — this commits the player's typed lines into the SignText
        // state so that getText(pIsFrontText) below returns the actual input.
        // Without this, getText() returns the empty default text.
//        super.updateText(updater, isFrontText);
        // Manually commit the player's input without calling super, which throws
        // internally and swallows everything after it in the packet handler.
        SignText newSignText = updater.apply(getText(isFrontText));
        setText(newSignText, isFrontText);
        setChanged();
//        if (!(level instanceof ServerLevel serverLevel)) return false;
        if (!(level instanceof ServerLevel serverLevel)) {
            ClaimMyLand.LOGGER.debug("CMLRenameSign: level is not ServerLevel, type={}", level.getClass().getName());
            return false;
        }
        ClaimMyLand.LOGGER.debug("CMLRenameSign: level is ServerLevel");

        String newName = buildName(getText(isFrontText));
        ClaimMyLand.LOGGER.debug("CMLRenameSign: newName='{}'", newName);

        ServerPlayer owner = ownerPlayerId != null
                ? serverLevel.getServer().getPlayerList().getPlayer(ownerPlayerId)
                : null;
        ClaimMyLand.LOGGER.debug("CMLRenameSign: ownerPlayerId={}, owner={}", ownerPlayerId, owner);

        // Validate: name must not be blank
        if (newName.isEmpty()) {
            if (owner != null) {
                PlayerMessageHelper.sendFailure(owner, "parcel.rename.sign.empty");
            }
            serverLevel.removeBlock(getBlockPos(), false);
            return false;
        }

        // Resolve the parcel
        Optional<Parcel> parcelOpt = ParcelRegistry.findByParcelId(parcelId);
        ClaimMyLand.LOGGER.debug("CMLRenameSign: parcelId={}, found={}", parcelId, parcelOpt.isPresent());

        if (parcelOpt.isEmpty()) {
            ClaimMyLand.LOGGER.warn("CMLRenameSignBlockEntity: parcel {} not found during rename — sign removed", parcelId);
            serverLevel.removeBlock(getBlockPos(), false);
            return false;
        }

        Parcel parcel = parcelOpt.get();
        ClaimMyLand.LOGGER.debug("CMLRenameSign: owner check — parcel owner={}, ourOwner={}",
                parcel.getEstate().getOwnerId(), ownerPlayerId);
        // Defensive owner check
        if (ownerPlayerId == null || !ownerPlayerId.equals(parcel.getEstate().getOwnerId())) {
            if (owner != null) {
                PlayerMessageHelper.sendFailure(owner, "parcel.rename.sign.not_owner");
            }
            serverLevel.removeBlock(getBlockPos(), false);
            return false;
        }

        // Perform the rename
        parcel.setName(newName);
        ClaimMyLand.LOGGER.debug("CMLRenameSign: set name to '{}' on object {}", parcel.getName(), System.identityHashCode(parcel));

        ParcelRegistry.findByParcelId(parcelId).ifPresent(p ->
                ClaimMyLand.LOGGER.debug("CMLRenameSign: registry object has name '{}' on object {}", p.getName(), System.identityHashCode(p))
        );

        CommandHelper.save(serverLevel);
        CMLNetwork.syncParcelToTrackingPlayers(serverLevel, parcel);
        if (owner != null) {
            CMLNetwork.syncParcelToPlayer(serverLevel, owner, parcel);
            PlayerMessageHelper.sendSuccess(owner, "parcel.rename.sign.success", (Object)newName);
        }

        serverLevel.removeBlock(getBlockPos(), false);
        return true;
    }

    /**
     * Concatenates all non-empty lines from the sign text with a single space,
     * trimming leading/trailing whitespace from each line and the result.
     *
     * @param signText the text submitted by the player
     * @return the concatenated name, or an empty string if all lines were blank
     */
    private static String buildName(SignText signText) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            String line = signText.getMessage(i, false).getString().trim();
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