package mod.gottsch.neo.claimmyland.core.network;

import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.core.config.ClientServerConfig;
import mod.gottsch.neo.claimmyland.core.config.Config;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;


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
public class ServerConfigSyncPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerConfigSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ClaimMyLand.MOD_ID, "server_config_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerConfigSyncPacket> STREAM_CODEC =
            StreamCodec.of(ServerConfigSyncPacket::encode, ServerConfigSyncPacket::decode);

    private final int parcelBufferRadius;
    private final int nationParcelBufferRadius;
    private final boolean enableParcelEntryTitle;
    private final boolean foundationStoneCentered;

    public ServerConfigSyncPacket(int parcelBufferRadius, int nationParcelBufferRadius,
                                  boolean enableParcelEntryTitle, boolean foundationStoneCentered) {
        this.parcelBufferRadius       = parcelBufferRadius;
        this.nationParcelBufferRadius = nationParcelBufferRadius;
        this.enableParcelEntryTitle   = enableParcelEntryTitle;
        this.foundationStoneCentered  = foundationStoneCentered;
    }

    public static ServerConfigSyncPacket fromServerConfig() {
        return new ServerConfigSyncPacket(
                Config.SERVER.general.parcelBufferRadius.get(),
                Config.SERVER.general.nationParcelBufferRadius.get(),
                Config.SERVER.general.enableParcelEntryTitle.get(),
                Config.SERVER.general.foundationStoneCentered.get()
        );
    }

    private static void encode(RegistryFriendlyByteBuf buf, ServerConfigSyncPacket packet) {
        buf.writeInt(packet.parcelBufferRadius);
        buf.writeInt(packet.nationParcelBufferRadius);
        buf.writeBoolean(packet.enableParcelEntryTitle);
        buf.writeBoolean(packet.foundationStoneCentered);
    }

    private static ServerConfigSyncPacket decode(RegistryFriendlyByteBuf buf) {
        int     parcelBufferRadius       = buf.readInt();
        int     nationParcelBufferRadius = buf.readInt();
        boolean enableParcelEntryTitle   = buf.readBoolean();
        boolean foundationStoneCentered  = buf.readBoolean();
        return new ServerConfigSyncPacket(parcelBufferRadius, nationParcelBufferRadius,
                enableParcelEntryTitle, foundationStoneCentered);
    }

    public static void handle(ServerConfigSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
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
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}