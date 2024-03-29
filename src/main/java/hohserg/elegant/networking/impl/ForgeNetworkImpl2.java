package hohserg.elegant.networking.impl;

import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.IByteBufSerializable;
import hohserg.elegant.networking.api.ServerToClientPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

public class ForgeNetworkImpl2 implements Network<ByteBuf> {
    @Override
    public void sendToPlayer(ServerToClientPacket packet, EntityPlayerMP player) {
        getChannel(packet).sendTo(preparePacket(packet), player);
    }

    @Override
    public void sendToClients(ServerToClientPacket packet) {
        getChannel(packet).sendToAll(preparePacket(packet));
    }

    @Override
    public void sendPacketToAllAround(ServerToClientPacket packet, World world, double x, double y, double z, double range) {
        getChannel(packet).sendToAllAround(preparePacket(packet), new NetworkRegistry.TargetPoint(world.provider.getDimension(), x, y, z, range));
    }

    @Override
    public void sendToDimension(ServerToClientPacket packet, World world) {
        getChannel(packet).sendToDimension(preparePacket(packet), world.provider.getDimension());
    }

    @Override
    public void sendToChunk(ServerToClientPacket packet, World world, int chunkX, int chunkZ) {
        getChannel(packet).sendToAllAround(preparePacket(packet), new NetworkRegistry.TargetPoint(world.provider.getDimension(), chunkX << 4, 0, chunkZ << 4, 256));
    }

    private FMLProxyPacket preparePacket(IByteBufSerializable serverToClientPacket) {
        String name = serverToClientPacket.getClass().getName();
        ByteBuf acc = Unpooled.buffer();
        Registry.getSerializer(name).serialize(serverToClientPacket, acc);
        return new FMLProxyPacket(new PacketBuffer(acc), Registry.getChannelForPacket(name));
    }

    @Override
    public void sendToServer(ClientToServerPacket packet) {
        getChannel(packet).sendToServer(preparePacket(packet));
    }

    private FMLEventChannel getChannel(IByteBufSerializable serverToClientPacket) {
        return channels.get(Registry.getChannelForPacket(serverToClientPacket.getClass().getName()));
    }

    @Override
    public void onReceiveClient(ByteBuf packetRepresent, String channel) {
        int id = packetRepresent.readByte();
        ((ServerToClientPacket) Registry.getSerializer(Registry.getPacketName(channel, id)).unserialize(packetRepresent)).onReceive(Minecraft.getMinecraft());
    }

    @Override
    public void onReceiveServer(ByteBuf packetRepresent, EntityPlayerMP player, String channel) {
        int id = packetRepresent.readByte();
        ((ClientToServerPacket) Registry.getSerializer(Registry.getPacketName(channel, id)).unserialize(packetRepresent)).onReceive(player);
    }

    private Map<String, FMLEventChannel> channels = new HashMap<>();

    @Override
    public void registerChannel(String channel) {
        FMLEventChannel fmlEventChannel = NetworkRegistry.INSTANCE.newEventDrivenChannel(channel);
        channels.put(channel, fmlEventChannel);
        fmlEventChannel.register(new Object() {

            @SubscribeEvent
            @SideOnly(Side.CLIENT)
            public void onClientReceive(FMLNetworkEvent.ClientCustomPacketEvent e) {
                onReceiveClient(e.getPacket().payload(), channel);
            }

            @SubscribeEvent
            @SideOnly(Side.SERVER)
            public void onServerReceive(FMLNetworkEvent.ServerCustomPacketEvent e) {
                onReceiveServer(e.getPacket().payload(), ((NetHandlerPlayServer) e.getHandler()).player, channel);
            }

        });
    }
}
