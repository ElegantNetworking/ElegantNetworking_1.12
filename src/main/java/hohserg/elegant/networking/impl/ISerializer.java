package hohserg.elegant.networking.impl;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public interface ISerializer<Packet> {
    void serialize(Packet value, ByteBuf acc);

    Packet unserialize(ByteBuf buf);

    int packetId();

    default void serializeString(String value, ByteBuf acc) {
        ByteBufUtils.writeUTF8String(acc, value);
    }

    default String unserializeString(ByteBuf buf) {
        return ByteBufUtils.readUTF8String(buf);
    }
}
