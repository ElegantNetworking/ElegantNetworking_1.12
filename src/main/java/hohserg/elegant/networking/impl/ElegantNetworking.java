package hohserg.elegant.networking.impl;

import lombok.Value;
import net.minecraftforge.fml.common.Loader;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ElegantNetworking {

    private static Map<String, String> channelByPacketClassName = new HashMap<>();
    private static Map<String, Integer> packetIdByPacketClassName = new HashMap<>();
    private static Map<Pair<String, Integer>, String> packetClassNameByChannelId = new HashMap<>();
    private static Map<String, ISerializer> serializerByPacketClassName = new HashMap<>();

    static String getChannelForPacket(String className) {
        return channelByPacketClassName.get(className);
    }

    static List<String> getPacketsForChannel(String channel) {
        return channelByPacketClassName.entrySet().stream().filter(i -> i.getValue().equals(channel)).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    static int getPacketId(String className) {
        return packetIdByPacketClassName.get(className);
    }

    static String getPacketName(String channel, int id) {
        return packetClassNameByChannelId.get(Pair.of(channel, id));
    }

    static ISerializer getSerializer(String className) {
        return serializerByPacketClassName.get(className);
    }

    private static Network defaultImpl = Loader.isModLoaded("codechickenlib") ? new CCLNetworkImpl() : new ForgeNetworkImpl();

    public static Network getNetwork() {
        return defaultImpl;
    }

    static void register(PacketInfo p, ISerializer serializer) {
        int id = serializer.packetId();
        channelByPacketClassName.put(p.className, p.channel);
        packetIdByPacketClassName.put(p.className, id);
        packetClassNameByChannelId.put(Pair.of(p.channel, id), p.className);
        serializerByPacketClassName.put(p.className, serializer);
    }

    @Value
    static class PacketInfo {
        public String channel;
        public String className;
    }
}
