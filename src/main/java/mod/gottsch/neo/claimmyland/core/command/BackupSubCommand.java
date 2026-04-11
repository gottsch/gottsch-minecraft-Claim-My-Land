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

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.command.helper.CommandHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.util.Optional;

/**
 * Ops-only command that triggers an immediate on-demand backup of parcel data.
 * Wired to the /cml-ops command tree via buildOps().
 *
 * Usage: /cml-ops backup
 *
 * @author Mark Gottschling on March 22, 2026
 */
public class BackupSubCommand implements SubCommand {
    public static final String BACKUP = "backup";

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return null;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> buildOps() {
        return Commands.literal(BACKUP)
                .executes(source -> executeBackup(source.getSource()));
    }

    /**
     * Triggers an immediate parcel data backup and reports the result to the issuing operator.
     */
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

        // Send success with filename as a literal component appended after the key-based line
        CommandHelper.sendSuccess(source, "backup.success");
        source.sendSuccess(
                () -> Component.literal("  " + result.get().getName()),
                false);
        return 1;
    }
}