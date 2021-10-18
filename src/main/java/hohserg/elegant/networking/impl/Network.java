package hohserg.elegant.networking.impl;

import hohserg.elegant.networking.api.ClientToServerPacket;
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
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
            throw new RuntimeException("Attempt to send ServerToClientPacket from client side: " + packet.getClass().getCanonicalName());
    }

    default void checkSendingSide(ClientToServerPacket packet) {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER)
            throw new RuntimeException("Attempt to send ClientToServerPacket from server side: " + packet.getClass().getCanonicalName());
    }
}
