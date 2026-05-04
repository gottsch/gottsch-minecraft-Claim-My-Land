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

package mod.gottsch.neo.claimmyland.core.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.command.helper.CommandHelper;
import mod.gottsch.neo.claimmyland.core.estate.Estate;
import mod.gottsch.neo.claimmyland.core.estate.NationEstate;
import mod.gottsch.neo.claimmyland.core.network.CMLNetwork;
import mod.gottsch.neo.claimmyland.core.parcel.NationalizedParcel;
import mod.gottsch.neo.claimmyland.core.parcel.Parcel;
import mod.gottsch.neo.claimmyland.core.parcel.ParcelType;
import mod.gottsch.neo.claimmyland.core.registry.EstateRegistry;
import mod.gottsch.neo.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.neo.claimmyland.core.util.LangUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Ops-only backup management command. Sub-literals:
 *   /cml-ops backup              — immediate on-demand save
 *   /cml-ops backup list         — list available backup files
 *   /cml-ops backup load <file>          — show confirmation prompt
 *   /cml-ops backup load <file> confirm  — restore parcel state from backup
 *
 * @author Mark Gottschling on March 22, 2026
 */
public class BackupSubCommand implements SubCommand {
    public static final String BACKUP = "backup";

    private static final SuggestionProvider<CommandSourceStack> BACKUP_FILENAMES = (ctx, builder) -> {
        if (ClaimMyLand.getParcelSaver() == null) return builder.buildFuture();
        List<String> names = Arrays.stream(ClaimMyLand.getParcelSaver().getAvailableSaves())
                .map(File::getName)
                .collect(Collectors.toList());
        return SharedSuggestionProvider.suggest(names, builder);
    };

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return null;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {
        return Commands.literal(BACKUP)
                .executes(source -> executeBackup(source.getSource()))
                ///// BACKUP LIST /////
                .then(Commands.literal(LIST)
                        .executes(source -> executeList(source.getSource()))
                )
                ///// BACKUP LOAD /////
                .then(Commands.literal(LOAD)
                        .then(Commands.argument(FILENAME, StringArgumentType.string())
                                .suggests(BACKUP_FILENAMES)
                                .executes(source -> executeLoad(
                                        source.getSource(),
                                        StringArgumentType.getString(source, FILENAME)))
                                .then(Commands.literal(CONFIRM)
                                        .executes(source -> executeLoadConfirmed(
                                                source.getSource(),
                                                StringArgumentType.getString(source, FILENAME)))
                                )
                        )
                );
    }

    public static int executeBackup(CommandSourceStack source) {
        if (ClaimMyLand.getParcelSaver() == null) {
            CommandHelper.sendFailure(source, "backup.disabled");
            return -1;
        }

        Optional<File> result = ClaimMyLand.getParcelSaver().save();

        if (result.isEmpty()) {
            CommandHelper.sendFailure(source, "backup.failure");
            return -1;
        }

        CommandHelper.sendSuccess(source, "backup.success");
        source.sendSuccess(
                () -> Component.literal("  " + result.get().getName()),
                false);
        return 1;
    }

    private static int executeList(CommandSourceStack source) {
        if (ClaimMyLand.getParcelSaver() == null) {
            CommandHelper.sendFailure(source, "backup.disabled");
            return -1;
        }

        File[] files = ClaimMyLand.getParcelSaver().getAvailableSaves();

        if (files.length == 0) {
            CommandHelper.sendSuccess(source, "backup.list.empty");
            return 1;
        }

        CommandHelper.sendSuccess(source, "backup.list");
        for (File file : files) {
            source.sendSuccess(() -> Component.literal("  " + file.getName())
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int executeLoad(CommandSourceStack source, String filename) {
        if (ClaimMyLand.getParcelSaver() == null) {
            CommandHelper.sendFailure(source, "backup.disabled");
            return -1;
        }

        File[] files = ClaimMyLand.getParcelSaver().getAvailableSaves();
        Optional<File> target = Arrays.stream(files)
                .filter(f -> f.getName().equals(filename))
                .findFirst();

        if (target.isEmpty()) {
            CommandHelper.sendFailure(source, "backup.load.not_found");
            return -1;
        }

        // Build confirm command with properly quoted filename
        String cmd = "/cml-ops backup load \"" + filename + "\" confirm";
        Component confirmButton = Component.literal(" [✔ Confirm]")
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.GREEN)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Confirm — this will replace ALL parcel data"))));

        // Warning header
        CommandHelper.sendWarning(source, "backup.load.confirm");
        // Body with inline confirm button
        source.sendSuccess(() -> Component.translatable(
                        LangUtil.chat("backup.load.confirm.body"), filename)
                .withStyle(ChatFormatting.GRAY)
                .append(confirmButton), false);
        return 1;
    }

    private static int executeLoadConfirmed(CommandSourceStack source, String filename) {
        if (ClaimMyLand.getParcelSaver() == null) {
            CommandHelper.sendFailure(source, "backup.disabled");
            return -1;
        }

        File[] files = ClaimMyLand.getParcelSaver().getAvailableSaves();
        Optional<File> target = Arrays.stream(files)
                .filter(f -> f.getName().equals(filename))
                .findFirst();

        if (target.isEmpty()) {
            CommandHelper.sendFailure(source, "backup.load.not_found");
            return -1;
        }

        List<Parcel> parcels = ClaimMyLand.getParcelSaver().loadFromFile(target.get());
        if (parcels == null) {
            CommandHelper.sendFailure(source, "backup.load.failure");
            return -1;
        }

        // Clear both registries before repopulating
        EstateRegistry.clear();
        ParcelRegistry.clear();

        // Sort: NATION first so their estates are in EstateRegistry before
        // CitizenParcels and ZoneParcels reference the same estate by ID.
        List<Parcel> sorted = parcels.stream()
                .sorted(Comparator.comparingInt(p -> p.getType() == ParcelType.NATION ? 0 : 1))
                .collect(Collectors.toList());

        for (Parcel parcel : sorted) {
            Estate estate = parcel.getEstate();
            Optional<Estate> existing = EstateRegistry.get(estate.getId());
            if (existing.isPresent()) {
                // Reuse the already-registered estate (shared NationEstate for citizen/zone)
                parcel.setEstate(existing.get());
            } else {
                EstateRegistry.register(estate);
            }
            // Re-link nationEstate to the registered NationEstate so all citizen/zone parcels
            // share the same instance (mirrors the NBT load path via NationalizedParcel.loadNationEstate)
            if (parcel instanceof NationalizedParcel np && np.getNationEstate() != null) {
                EstateRegistry.get(np.getNationEstate().getId())
                        .ifPresent(e -> np.setNationEstate((NationEstate) e));
            }
            ParcelRegistry.register(parcel);
        }

        // Persist restored state so it survives a restart
        CommandHelper.save(source.getLevel());

        // Sync to all online players
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            CMLNetwork.syncAllParcelsToPlayer(player);
        }

        CommandHelper.sendSuccess(source, "backup.load.success", "backup.load.success.body",
                filename, parcels.size());
        return 1;
    }
}
