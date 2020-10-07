package hohserg.elegant.networking.api;

import hohserg.elegant.networking.impl.ElegantNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

/**
 * Base interface for packet, which can be send from server to client
 */
public interface ServerToClientPacket extends IByteBufSerializable {
    /**
     * Called when the packet is received
     */
    void onReceive(Minecraft mc);

    default void sendToPlayer(EntityPlayerMP player) {
        ElegantNetworking.getNetwork().sendToPlayer(this, player);
    }

    default void sendToClients() {
        ElegantNetworking.getNetwork().sendToClients(this);
    }

    default void sendPacketToAllAround(World world, double x, double y, double z, double range) {
        ElegantNetworking.getNetwork().sendPacketToAllAround(this, world, x, y, z, range);
    }

    default void sendToDimension(World world) {
        ElegantNetworking.getNetwork().sendToDimension(this, world);
    }

    default void sendToChunk(World world, int chunkX, int chunkZ) {
        ElegantNetworking.getNetwork().sendToChunk(this, world, chunkX, chunkZ);
    }
}
