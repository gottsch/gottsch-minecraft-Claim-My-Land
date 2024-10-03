package mod.gottsch.forge.claimmyland.core.gui.chat;

import mod.gottsch.forge.claimmyland.core.util.LangUtil;
import mod.gottsch.forge.claimmyland.core.util.ModUtil;
import mod.gottsch.forge.gottschcore.spatial.ICoords;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class ChatMessages {
    public static void unableToClaim(Player player, ICoords coords, ICoords size) {
        player.sendSystemMessage(Component.translatable(LangUtil.chat("deed.claim.unable_to_claim"),
                coords.toShortString(), size.toShortString()).withStyle(ChatFormatting.RED));
    }
}
