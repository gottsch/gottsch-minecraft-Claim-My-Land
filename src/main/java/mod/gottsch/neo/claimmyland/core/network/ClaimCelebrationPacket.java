package mod.gottsch.neo.claimmyland.core.network;

import mod.gottsch.neo.claimmyland.ClaimMyLand;
import mod.gottsch.neo.claimmyland.client.packet.handler.ClientCelebrationHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client packet that triggers the claim celebration particle wave on
 * the claiming player's client.
 * <p>
 * Two emission modes depending on parcel perimeter length:
 * <ul>
 *   <li><b>Full perimeter</b> — if perimeter ≤ {@value #FULL_PERIMETER_THRESHOLD}
 *       blocks, particles are emitted along every block of all four XZ edges.</li>
 *   <li><b>Capped arc</b> — if perimeter &gt; {@value #FULL_PERIMETER_THRESHOLD},
 *       particles are emitted for up to {@value #ARC_LENGTH} blocks outward from
 *       the Foundation Stone position along each of the four edges.</li>
 * </ul>
 * Only sent to the claiming player ({@link net.minecraftforge.network.PacketDistributor#PLAYER}).
 * </p>
 *
 * @author Mark Gottschling on March 25, 2026
 */
public class ClaimCelebrationPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClaimCelebrationPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ClaimMyLand.MOD_ID, "claim_celebration"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClaimCelebrationPacket> STREAM_CODEC =
            StreamCodec.of(ClaimCelebrationPacket::encode, ClaimCelebrationPacket::decode);

    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;
    private final int borderStoneX;
    private final int borderStoneY;
    private final int borderStoneZ;

    public ClaimCelebrationPacket(int minX, int minZ, int maxX, int maxZ,
                                  int borderStoneX, int borderStoneY, int borderStoneZ) {
        this.minX         = minX;
        this.minZ         = minZ;
        this.maxX         = maxX;
        this.maxZ         = maxZ;
        this.borderStoneX = borderStoneX;
        this.borderStoneY = borderStoneY;
        this.borderStoneZ = borderStoneZ;
    }

    private static void encode(RegistryFriendlyByteBuf buf, ClaimCelebrationPacket packet) {
        buf.writeInt(packet.minX);
        buf.writeInt(packet.minZ);
        buf.writeInt(packet.maxX);
        buf.writeInt(packet.maxZ);
        buf.writeInt(packet.borderStoneX);
        buf.writeInt(packet.borderStoneY);
        buf.writeInt(packet.borderStoneZ);
    }

    private static ClaimCelebrationPacket decode(RegistryFriendlyByteBuf buf) {
        int minX         = buf.readInt();
        int minZ         = buf.readInt();
        int maxX         = buf.readInt();
        int maxZ         = buf.readInt();
        int borderStoneX = buf.readInt();
        int borderStoneY = buf.readInt();
        int borderStoneZ = buf.readInt();
        return new ClaimCelebrationPacket(minX, minZ, maxX, maxZ, borderStoneX, borderStoneY, borderStoneZ);
    }

    public static void handle(ClaimCelebrationPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientCelebrationHandler.handle(packet);
            }
        });
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public int getMinX()         { return minX; }
    public int getMinZ()         { return minZ; }
    public int getMaxX()         { return maxX; }
    public int getMaxZ()         { return maxZ; }
    public int getBorderStoneX() { return borderStoneX; }
    public int getBorderStoneY() { return borderStoneY; }
    public int getBorderStoneZ() { return borderStoneZ; }
}