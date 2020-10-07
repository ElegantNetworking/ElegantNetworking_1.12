package hohserg.elegant.networking.test;

import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Value;
import net.minecraft.entity.player.EntityPlayerMP;

//@ElegantPacket
@Data
public class Test implements ClientToServerPacket {
    protected int a;
    int b;
    private int c;
    boolean d;
    private boolean e;
    String f;
    InnerTest g;
    final double h;

    @Override
    public void onReceive(EntityPlayerMP player) {

    }

    @Value
    public static class InnerTest {
        String a;
    }
}
