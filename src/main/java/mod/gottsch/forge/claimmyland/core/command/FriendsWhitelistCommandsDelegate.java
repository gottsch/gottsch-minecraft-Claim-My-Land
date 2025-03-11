package mod.gottsch.forge.claimmyland.core.command;

import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 *
 * @author Mark Gottschling on Mar 9, 2025
 *
 */
public class FriendsWhitelistCommandsDelegate {

    public static Optional<List<UUID>> getFriendsWhitelist(CommandSourceStack source, UUID ownerUuid, String parcelName) {
        Optional<Parcel> parcel = CommandHelper.getParcelByOwner(source, ownerUuid, parcelName);
        return parcel.map(Parcel::getWhitelist);
    }

    /*
     * ops version
     */
    public static int add(CommandSourceStack source, String ownerName, String parcelName, String friendsName) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
        Optional<UUID> friendUuid = CommandHelper.getPlayerUuid(source, friendsName);
        if (ownerUuid.isPresent()) {
            if (friendUuid.isPresent()) {
                return add(source, ownerUuid.get(), parcelName, friendUuid.get());
            } else {
                CommandHelper.sendUnableToLocatePlayerMessage(source, friendsName);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return -1;
    }

    /*
     * player version
     */
    public static int add(CommandSourceStack source, String parcelName, String friendsName) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source);
        Optional<UUID> friendUuid = CommandHelper.getPlayerUuid(source, friendsName);
        if (ownerUuid.isPresent()) {
            if (friendUuid.isPresent()) {
                return add(source, ownerUuid.get(), parcelName, friendUuid.get());
            } else {
                CommandHelper.sendUnableToLocatePlayerMessage(source, friendsName);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source);
        }
        return -1;
    }

    /**
     * common version
     */
    private static int add(CommandSourceStack source, UUID ownerUuid, String parcelName, UUID friendUuid) {
        Optional<Parcel> parcel = CommandHelper.getParcelByOwner(source, ownerUuid, parcelName);

        parcel.ifPresentOrElse(action -> {
                    action.getWhitelist().add(friendUuid);
                    CommandHelper.save(source.getLevel());
                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.whitelist.add.success")).withStyle(ChatFormatting.GREEN), false);
                }, () -> source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.whitelist.add.failure")).withStyle(ChatFormatting.RED), false)
        );
        return 1;
    }

    /**
     * player version
     */
    public static int remove(CommandSourceStack source, String parcelName, String friendsName) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source);
        Optional<UUID> friendUuid = CommandHelper.getPlayerUuid(source, friendsName);

        if (ownerUuid.isPresent()) {
            if (friendUuid.isPresent()) {
                return remove(source,ownerUuid.get(), parcelName, friendUuid.get());
            } else {
                CommandHelper.sendUnableToLocatePlayerMessage(source, friendsName);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source);
        }
        return -1;
    }

    /**
     * ops version
     */
    public static int remove(CommandSourceStack source, String ownerName, String parcelName, String friendsName) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
        Optional<UUID> friendsUuid = CommandHelper.getPlayerUuid(source, friendsName);
        if (ownerUuid.isPresent()) {
            if (friendsUuid.isPresent()) {
                return remove(source, ownerUuid.get(), parcelName, friendsUuid.get());
            } else {
                CommandHelper.sendUnableToLocatePlayerMessage(source, friendsName);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return -1;
    }

    /**
     * common version
     */
    public static int remove(CommandSourceStack source, UUID ownerUuid, String parcelName, UUID friendsUuid) {
        Optional<Parcel> parcel = CommandHelper.getParcelByOwner(source, ownerUuid, parcelName);

        parcel.ifPresentOrElse(action -> {
                    if (action.getWhitelist().remove(friendsUuid)) {
                        CommandHelper.save(source.getLevel());
                        source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.whitelist.remove.success")).withStyle(ChatFormatting.GREEN), false);
                    } else {
                        source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.whitelist.remove.failure")).withStyle(ChatFormatting.RED), false);
                    }
                },
                () -> source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.whitelist.remove.failure")).withStyle(ChatFormatting.RED), false)
        );
        return 1;
    }

    /**
     * player version
     */
    public static int list(CommandSourceStack source, String parcelName) {
        Optional<UUID> playerUuid = CommandHelper.getPlayerUuid(source);
        if (playerUuid.isPresent()) {
            return list(source, playerUuid.get(), parcelName);
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source);
        }
        return -1;
    }

    /*
     * ops version
     */
    public static int list(CommandSourceStack source, String ownerName, String parcelName) {
        Optional<UUID> ownerUuid = CommandHelper.getPlayerUuid(source, ownerName);
        if (ownerUuid.isPresent()) {
            return list(source, ownerUuid.get(), parcelName);
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
        }
        return -1;
    }

    /**
     * common version
     */
    public static int list(CommandSourceStack source, UUID ownerUuid, String parcelName) {
        Optional<Parcel> parcel = CommandHelper.getParcelByOwner(source, ownerUuid, parcelName);
        parcel.ifPresentOrElse(action -> {
                    CommandHelper.sendNewLineMessage(source);
                    source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.whitelist.list"))
                            .withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD)
                            .append(Component.translatable(parcelName)
                                    .withStyle(ChatFormatting.AQUA)), false);
                    CommandHelper.sendNewLineMessage(source);
                    action.getWhitelist().forEach(uuid -> {
                        Optional<String> friendsName = CommandHelper.getPlayerName(source, uuid);
                        friendsName.ifPresent(s -> source.sendSuccess(() -> Component.literal(s).withStyle(ChatFormatting.GREEN), false));
                    });
                }, () -> source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.whitelist.list.failure")).withStyle(ChatFormatting.RED), false)
        );
        return 1;
    }
}
