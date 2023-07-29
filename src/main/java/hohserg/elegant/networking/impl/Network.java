package hohserg.elegant.networking.impl;

import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;
import hohserg.elegant.networking.utils.ReflectionUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

public interface Network<PacketRepresentation> {

    Network defaultImpl = ReflectionUtils.create("hohserg.elegant.networking.impl.ForgeNetworkImpl");
    Platform plantformImpl = ReflectionUtils.create("hohserg.elegant.networking.impl.PlatformImpl");

    static Network getNetwork() {
        return defaultImpl;
    }

    void sendToPlayer(ServerToClientPacket packet, EntityPlayerMP player);

    void sendToClients(ServerToClientPacket packet);

    void sendPacketToAllAround(ServerToClientPacket packet, World world, double x, double y, double z, double range);

    void sendToDimension(ServerToClientPacket packet, World world);

    void sendToChunk(ServerToClientPacket packet, World world, int chunkX, int chunkZ);

    void sendToServer(ClientToServerPacket packet);

    void onReceiveClient(PacketRepresentation packetRepresent, String channel);

    void onReceiveServer(PacketRepresentation packetRepresent, EntityPlayerMP player, String channel);

    void registerChannel(String channel);

    default void checkSendingSide(ServerToClientPacket packet) {
        if (plantformImpl.isClientSide())
            throw new RuntimeException("Attempt to send ServerToClientPacket from client side: " + packet.getClass().getCanonicalName());
    }

    default void checkSendingSide(ClientToServerPacket packet) {
        if (plantformImpl.isServerSide())
            throw new RuntimeException("Attempt to send ClientToServerPacket from server side: " + packet.getClass().getCanonicalName());
    }
}
