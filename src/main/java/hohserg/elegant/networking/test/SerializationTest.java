package hohserg.elegant.networking.test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import hohserg.elegant.networking.api.IByteBufSerializable;
import hohserg.elegant.networking.impl.DataUtils2;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.List;
import java.util.Map;

public class SerializationTest {
    public static void main(String[] args) throws ClassNotFoundException {
        Test value = new Test(1, 1.5, "test", new Test2(1, true), null, ImmutableMap.of("test", 1, "lol", 2), Lists.newArrayList(1f, 2.5f), ImmutableList.of("test", 1, 4d));
        System.out.println(DataUtils2.unserialize(DataUtils2.serialize(value), Test.class).equals(value));
        PacketPotionHotkey packetPotionHotkey = new PacketPotionHotkey(1);
        System.out.println(DataUtils2.unserialize(DataUtils2.serialize(packetPotionHotkey), PacketPotionHotkey.class).equals(packetPotionHotkey));
    }

    @Value
    public static class Test {
        int a;
        double b;
        String c;
        Test2 d;
        Test2 e;
        Map<String, Integer> f;
        List<Float> g;
        List<Object> h;
    }

    @AllArgsConstructor
    @Value
    public static class Test2 implements IByteBufSerializable {
        int a;
        boolean b;

        public Test2(ByteBuf buf) {
            a = buf.readInt();
            b = buf.readBoolean();
        }

        @Override
        public void serialize(ByteBuf acc) {
            acc.writeInt(a);
            acc.writeBoolean(b);
        }
    }
}
