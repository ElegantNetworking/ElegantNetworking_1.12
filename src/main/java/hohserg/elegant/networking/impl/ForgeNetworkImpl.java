package hohserg.elegant.networking.impl;

import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.IByteBufSerializable;
import hohserg.elegant.networking.api.ServerToClientPacket;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashMap;
import java.util.Map;

public class ForgeNetworkImpl implements Network<ForgeNetworkImpl.UniversalPacket> {
    @Override
    public void sendToPlayer(ServerToClientPacket packet, EntityPlayerMP player) {
        checkSendingSide(packet);
        getChannel(packet).sendTo(preparePacket(packet), player);
    }

    @Override
    public void sendToClients(ServerToClientPacket packet) {
        checkSendingSide(packet);
        getChannel(packet).sendToAll(preparePacket(packet));
    }

    @Override
    public void sendPacketToAllAround(ServerToClientPacket packet, World world, double x, double y, double z, double range) {
        checkSendingSide(packet);
        getChannel(packet).sendToAllAround(preparePacket(packet), new NetworkRegistry.TargetPoint(world.provider.getDimension(), x, y, z, range));
    }

    @Override
    public void sendToDimension(ServerToClientPacket packet, World world) {
        checkSendingSide(packet);
        getChannel(packet).sendToDimension(preparePacket(packet), world.provider.getDimension());
    }

    @Override
    public void sendToChunk(ServerToClientPacket packet, World world, int chunkX, int chunkZ) {
        checkSendingSide(packet);
        SimpleNetworkWrapper channel = getChannel(packet);
        ServerToClientUniversalPacket message = preparePacket(packet);

        PlayerChunkMapEntry playerInstance = ((WorldServer) world).getPlayerChunkMap().getEntry(chunkX, chunkZ);
        if (playerInstance != null)
            for (EntityPlayerMP player : playerInstance.getWatchingPlayers())
                channel.sendTo(message, player);
    }

    @Override
    public void sendToServer(ClientToServerPacket packet) {
        checkSendingSide(packet);
        getChannel(packet).sendToServer(preparePacket(packet));
    }

    private SimpleNetworkWrapper getChannel(IByteBufSerializable packet) {
        return channels.get(Registry.getChannelForPacket(packet.getClass().getName()));
    }

    private ServerToClientUniversalPacket preparePacket(ServerToClientPacket packet) {
        return new ServerToClientUniversalPacket(Registry.getPacketId(packet.getClass()), packet);
    }

    private ClientToServerUniversalPacket preparePacket(ClientToServerPacket packet) {
        return new ClientToServerUniversalPacket(Registry.getPacketId(packet.getClass()), packet);
    }

    @Override
    public void onReceiveClient(UniversalPacket packetRepresent, String channel) {
        this.<ServerToClientPacket>readObjectFromPacket(packetRepresent, channel)
                .onReceive(mc());
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
            if (!mc().isCallingFromMinecraftThread())
                mc().addScheduledTask(() -> onReceiveClient(message, channel));
            else
                onReceiveClient(message, channel);

            return null;
        }, ServerToClientUniversalPacket.class, 0, Side.CLIENT);

        simpleNetworkWrapper.registerMessage((message, ctx) -> {
            MinecraftServer mc = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (!mc.isCallingFromMinecraftThread())
                mc.addScheduledTask(() -> onReceiveServer(message, ctx.getServerHandler().player, channel));
            else
                onReceiveServer(message, ctx.getServerHandler().player, channel);

            return null;
        }, ClientToServerUniversalPacket.class, 1, Side.SERVER);
    }

    @NoArgsConstructor
    public static class ClientToServerUniversalPacket extends UniversalPacket<ClientToServerPacket> {
        public ClientToServerUniversalPacket(int id, ClientToServerPacket packet) {
            super(id, packet, null);
        }
    }

    @NoArgsConstructor
    public static class ServerToClientUniversalPacket extends UniversalPacket<ServerToClientPacket> {
        public ServerToClientUniversalPacket(int id, ServerToClientPacket packet) {
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
            String packetName = Registry.getPacketName(channel, id);
            return (A) Registry.getSerializer(packetName).unserialize(buf);
        }


        @Override
        public void fromBytes(ByteBuf buf) {
            this.buf = buf.copy();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeByte(id);
            Registry.getSerializer(packet.getClass().getName()).serialize(packet, buf);
        }
    }

    public static Minecraft mc() {
        return Minecraft.getMinecraft();
    }
}
