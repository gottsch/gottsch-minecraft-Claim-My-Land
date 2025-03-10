package mod.gottsch.forge.claimmyland.core.command;

import mod.gottsch.forge.claimmyland.core.parcel.Parcel;
import mod.gottsch.forge.claimmyland.core.registry.ParcelRegistry;
import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 *
 * @author Mark Gottschling on Mar 9, 2025
 *
 */
public class FriendsWhitelistCommandDelegate {

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

    public static int displayWhitelist(CommandSourceStack source, String ownerName, String parcelName) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        if (player != null) {
            List<Parcel> parcels = ParcelRegistry.findByOwner(player.getUUID());
            Optional<Parcel> parcel = parcels.stream().filter(p -> p.getName().equalsIgnoreCase(parcelName)).findFirst();
            if (parcel.isPresent()) {
                CommandHelper.sendNewLineMessage(source);
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.whitelist.list"))
                        .withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD)
                        .append(Component.translatable(parcelName)
                                .withStyle(ChatFormatting.AQUA)), false);
                CommandHelper.sendNewLineMessage(source);
                parcel.get().getWhitelist().forEach(uuid -> {
                    // get the name for the uuid
                    ServerPlayer whitelistPlayer = source.getServer().getPlayerList().getPlayer(uuid);
                    if (whitelistPlayer != null) {
                        source.sendSuccess(() -> Component.translatable(whitelistPlayer.getName().getString()).withStyle(ChatFormatting.GREEN), false);
                    }
                });

            } else {
                source.sendSuccess(() -> Component.translatable(LangUtil.chat("parcel.whitelist.add.failure")).withStyle(ChatFormatting.RED), false);
            }
        } else {
            CommandHelper.sendUnableToLocatePlayerMessage(source, ownerName);
            return -1;
        }
        return 1;
    }

}
