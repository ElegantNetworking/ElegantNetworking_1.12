package hohserg.elegant.networking.impl;

import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.IByteBufSerializable;
import hohserg.elegant.networking.api.ServerToClientPacket;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;

public interface Network<PacketRepresentation> {

    Network defaultImpl =
            Main.config.getBackgroundPacketSystem() == Config.BackgroundPacketSystem.CCLImpl && Loader.isModLoaded("codechickenlib") ?
                    new CCLNetworkImpl() :
                    new ForgeNetworkImpl();

    static Network getNetwork() {
        return defaultImpl;
    }

    void sendToPlayer(ServerToClientPacket serverToClientPacket, EntityPlayerMP player);

    void sendToClients(ServerToClientPacket serverToClientPacket);

    void sendPacketToAllAround(ServerToClientPacket serverToClientPacket, World world, double x, double y, double z, double range);

    void sendToDimension(ServerToClientPacket serverToClientPacket, World world);

    void sendToChunk(ServerToClientPacket serverToClientPacket, World world, int chunkX, int chunkZ);

    void sendToServer(ClientToServerPacket clientToServerPacket);

    void onReceiveClient(PacketRepresentation packetRepresent, String channel);

    void onReceiveServer(PacketRepresentation packetRepresent, EntityPlayerMP player, String channel);

    void registerChannel(String channel);

    default void checkSendingSide(IByteBufSerializable packet) {
        Side side = FMLCommonHandler.instance().getEffectiveSide();

        if (side == Side.CLIENT && packet instanceof ServerToClientPacket)
            throw new RuntimeException("Attempt to send ServerToClientPacket from client side: " + packet.getClass().getCanonicalName());
        else if (side == Side.SERVER && packet instanceof ClientToServerPacket)
            throw new RuntimeException("Attempt to send ClientToServerPacket from server side: " + packet.getClass().getCanonicalName());
    }

}
