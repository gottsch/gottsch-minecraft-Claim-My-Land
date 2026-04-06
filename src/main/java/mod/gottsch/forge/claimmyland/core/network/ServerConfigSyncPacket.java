package mod.gottsch.forge.claimmyland.core.network;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import mod.gottsch.forge.claimmyland.core.config.ClientServerConfig;
import mod.gottsch.forge.claimmyland.core.config.Config;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server → client packet that pushes selected server config values to
 * {@link ClientServerConfig} on the client.
 * <p>
 * Sent to each player inside {@link CMLNetwork#syncAllParcelsToPlayer(net.minecraft.server.level.ServerPlayer)},
 * which is called on login and on dimension change. This ensures the client
 * always has an up-to-date copy of server-controlled settings.
 * </p>
 *
 * @author Mark Gottschling on March 25, 2026
 */
public class ServerConfigSyncPacket {

    private final int parcelBufferRadius;
    private final int nationParcelBufferRadius;
    private final boolean enableParcelEntryTitle;
    private final boolean foundationStoneCentered;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public ServerConfigSyncPacket(int parcelBufferRadius,
                                  int nationParcelBufferRadius,
                                  boolean enableParcelEntryTitle,
                                  boolean foundationStoneCentered) {
        this.parcelBufferRadius       = parcelBufferRadius;
        this.nationParcelBufferRadius = nationParcelBufferRadius;
        this.enableParcelEntryTitle   = enableParcelEntryTitle;
        this.foundationStoneCentered = foundationStoneCentered;
    }

    /**
     * Convenience factory — reads values directly from {@code Config.SERVER}.
     * Use this at all send sites so config values are always current.
     */
    public static ServerConfigSyncPacket fromServerConfig() {
        return new ServerConfigSyncPacket(
                Config.SERVER.general.parcelBufferRadius.get(),
                Config.SERVER.general.nationParcelBufferRadius.get(),
                Config.SERVER.general.enableParcelEntryTitle.get(),
                Config.SERVER.general.foundationStoneCentered.get()
        );
    }

    // -------------------------------------------------------------------------
    // Encode / decode
    // -------------------------------------------------------------------------

    public static void encode(ServerConfigSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.parcelBufferRadius);
        buf.writeInt(packet.nationParcelBufferRadius);
        buf.writeBoolean(packet.enableParcelEntryTitle);
        buf.writeBoolean(Config.SERVER.general.foundationStoneCentered.get());
    }

    public static ServerConfigSyncPacket decode(FriendlyByteBuf buf) {
        int     parcelBufferRadius       = buf.readInt();
        int     nationParcelBufferRadius = buf.readInt();
        boolean enableParcelEntryTitle   = buf.readBoolean();
        boolean foundationStoneCentered = buf.readBoolean();
        return new ServerConfigSyncPacket(parcelBufferRadius, nationParcelBufferRadius,
                enableParcelEntryTitle, foundationStoneCentered);
    }

    // -------------------------------------------------------------------------
    // Handle (client-side)
    // -------------------------------------------------------------------------

    public static void handle(ServerConfigSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientServerConfig.update(
                    packet.parcelBufferRadius,
                    packet.nationParcelBufferRadius,
                    packet.enableParcelEntryTitle,
                    packet.foundationStoneCentered
            );
            ClaimMyLand.LOGGER.debug(
                    "ServerConfigSyncPacket: parcelBufferRadius={}, nationParcelBufferRadius={}, enableParcelEntryTitle={}",
                    packet.parcelBufferRadius, packet.nationParcelBufferRadius,
                    packet.enableParcelEntryTitle);
        });
        ctx.get().setPacketHandled(true);
    }
}