package hohserg.elegant.networking.impl;

import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.IByteBufSerializable;
import hohserg.elegant.networking.api.ServerToClientPacket;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashMap;
import java.util.Map;

public class ForgeNetworkImpl implements Network<ForgeNetworkImpl.UniversalPacket> {
    @Override
    public void sendToPlayer(ServerToClientPacket serverToClientPacket, EntityPlayerMP player) {
        getChannel(serverToClientPacket).sendTo(preparePacket(serverToClientPacket), player);
    }

    private SimpleNetworkWrapper getChannel(IByteBufSerializable serverToClientPacket) {
        return channels.get(ElegantNetworking.getChannelForPacket(serverToClientPacket.getClass().getName()));
    }

    @Override
    public void sendToClients(ServerToClientPacket serverToClientPacket) {
        getChannel(serverToClientPacket).sendToAll(preparePacket(serverToClientPacket));
    }

    @Override
    public void sendPacketToAllAround(ServerToClientPacket serverToClientPacket, World world, double x, double y, double z, double range) {
        getChannel(serverToClientPacket).sendToAllAround(preparePacket(serverToClientPacket), new NetworkRegistry.TargetPoint(world.provider.getDimension(), x, y, z, range));
    }

    @Override
    public void sendToDimension(ServerToClientPacket serverToClientPacket, World world) {
        getChannel(serverToClientPacket).sendToDimension(preparePacket(serverToClientPacket), world.provider.getDimension());
    }

    @Override
    public void sendToChunk(ServerToClientPacket serverToClientPacket, World world, int chunkX, int chunkZ) {
        SimpleNetworkWrapper channel = getChannel(serverToClientPacket);
        ServerToClientUniversalPacket message = preparePacket(serverToClientPacket);

        PlayerChunkMapEntry playerInstance = ((WorldServer) world).getPlayerChunkMap().getEntry(chunkX, chunkZ);
        if (playerInstance != null)
            for (EntityPlayerMP player : playerInstance.getWatchingPlayers())
                channel.sendTo(message, (EntityPlayerMP) player);
    }

    @Override
    public void sendToServer(ClientToServerPacket packet) {
        getChannel(packet).sendToServer(preparePacket(packet));
    }

    private ServerToClientUniversalPacket preparePacket(ServerToClientPacket packet) {
        String packetClassName = packet.getClass().getName();
        String channel = ElegantNetworking.getChannelForPacket(packetClassName);
        int id = ElegantNetworking.getPacketId(packetClassName);

        return new ServerToClientUniversalPacket(id, channel, packet);
    }

    private ClientToServerUniversalPacket preparePacket(ClientToServerPacket packet) {
        String packetClassName = packet.getClass().getName();
        String channel = ElegantNetworking.getChannelForPacket(packetClassName);
        int id = ElegantNetworking.getPacketId(packetClassName);

        return new ClientToServerUniversalPacket(id, channel, packet);
    }

    @Override
    public void onReceiveClient(UniversalPacket packetRepresent, String channel) {
        this.<ServerToClientPacket>readObjectFromPacket(packetRepresent, channel)
                .onReceive(Minecraft.getMinecraft());
    }

    @Override
    public void onReceiveServer(UniversalPacket packetRepresent, EntityPlayerMP player, String channel) {
        this.<ClientToServerPacket>readObjectFromPacket(packetRepresent, channel)
                .onReceive(player);
    }

    private <A> A readObjectFromPacket(UniversalPacket packetRepresent, String channel) {
        return (A) packetRepresent.getPacket(channel);
    }

    private Map<String, SimpleNetworkWrapper> channels = new HashMap<>();

    @Override
    public void registerChannel(String channel) {
        SimpleNetworkWrapper simpleNetworkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel(channel);
        channels.put(channel, simpleNetworkWrapper);

        simpleNetworkWrapper.registerMessage((message, ctx) -> {
            onReceiveClient(message, channel);
            return null;
        }, ServerToClientUniversalPacket.class, 0, Side.CLIENT);

        simpleNetworkWrapper.registerMessage((message, ctx) -> {
            onReceiveServer(message, ctx.getServerHandler().player, channel);
            return null;
        }, ClientToServerUniversalPacket.class, 0, Side.SERVER);
    }

    @NoArgsConstructor
    public static class ClientToServerUniversalPacket extends UniversalPacket<ClientToServerPacket> {
        public ClientToServerUniversalPacket(int id, String channel, ClientToServerPacket packet) {
            super(id, packet, null);
        }
    }

    @NoArgsConstructor
    public static class ServerToClientUniversalPacket extends UniversalPacket<ServerToClientPacket> {
        public ServerToClientUniversalPacket(int id, String channel, ServerToClientPacket packet) {
            super(id, packet, null);
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    public static class UniversalPacket<A extends IByteBufSerializable> implements IMessage {

        private int id;
        private A packet;
        private ByteBuf buf;

        A getPacket(String channel) {
            id = buf.readByte();
            String packetName = ElegantNetworking.getPacketName(channel, id);
            return (A) ElegantNetworking.getSerializer(packetName).unserialize(buf);
        }


        @Override
        public void fromBytes(ByteBuf buf) {
            this.buf = buf.copy();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeByte(id);
            ElegantNetworking.getSerializer(packet.getClass().getName()).serialize(packet, buf);
        }
    }
}
