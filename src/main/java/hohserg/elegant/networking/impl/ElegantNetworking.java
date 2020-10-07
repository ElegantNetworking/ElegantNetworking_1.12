package hohserg.elegant.networking.impl;

import lombok.Value;

import java.util.HashMap;
import java.util.Map;

public class ElegantNetworking {

    static Map<String, String> channelByPacketClassName = new HashMap<>();
    static Map<String, Integer> packetIdByPacketClassName = new HashMap<>();
    static Map<Integer, String> packetClassNameById = new HashMap<>();
    static Map<String, ISerializer> serializerByPacketClassName = new HashMap<>();

    private static Network defaultImpl = new CCLNetworkImpl();

    public static Network getNetwork() {
        return defaultImpl;
    }

    static void register(PacketInfo p, ISerializer serializer) {
        int id = serializer.packetId();
        channelByPacketClassName.put(p.className, p.channel);
        packetIdByPacketClassName.put(p.className, id);
        packetClassNameById.put(id, p.className);
        serializerByPacketClassName.put(p.className, serializer);
    }

    @Value
    static class PacketInfo {
        public String channel;
        public String className;
    }
}
