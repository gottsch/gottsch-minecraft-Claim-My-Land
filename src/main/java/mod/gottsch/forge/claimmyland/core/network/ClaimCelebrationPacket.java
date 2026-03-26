package mod.gottsch.forge.claimmyland.core.network;

import mod.gottsch.forge.claimmyland.ClaimMyLand;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

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
public class ClaimCelebrationPacket {

    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;
    private final int borderStoneX;
    private final int borderStoneY;
    private final int borderStoneZ;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public ClaimCelebrationPacket(int minX, int minZ, int maxX, int maxZ,
                                  int borderStoneX, int borderStoneY, int borderStoneZ) {
        this.minX        = minX;
        this.minZ        = minZ;
        this.maxX        = maxX;
        this.maxZ        = maxZ;
        this.borderStoneX = borderStoneX;
        this.borderStoneY = borderStoneY;
        this.borderStoneZ = borderStoneZ;
    }

    // -------------------------------------------------------------------------
    // Encode / decode
    // -------------------------------------------------------------------------

    public static void encode(ClaimCelebrationPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.minX);
        buf.writeInt(packet.minZ);
        buf.writeInt(packet.maxX);
        buf.writeInt(packet.maxZ);
        buf.writeInt(packet.borderStoneX);
        buf.writeInt(packet.borderStoneY);
        buf.writeInt(packet.borderStoneZ);
    }

    public static ClaimCelebrationPacket decode(FriendlyByteBuf buf) {
        int minX         = buf.readInt();
        int minZ         = buf.readInt();
        int maxX         = buf.readInt();
        int maxZ         = buf.readInt();
        int borderStoneX = buf.readInt();
        int borderStoneY = buf.readInt();
        int borderStoneZ = buf.readInt();
        return new ClaimCelebrationPacket(minX, minZ, maxX, maxZ,
                borderStoneX, borderStoneY, borderStoneZ);
    }

    public int getMinX() { return minX; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxZ() { return maxZ; }
    public int getBorderStoneX() { return borderStoneX; }
    public int getBorderStoneY() { return borderStoneY; }
    public int getBorderStoneZ() { return borderStoneZ; }
}