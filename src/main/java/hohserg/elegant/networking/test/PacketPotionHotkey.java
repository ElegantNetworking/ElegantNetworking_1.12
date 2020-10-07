package hohserg.elegant.networking.test;

import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;






@ElegantPacket
//@AllArgsConstructor
@Value
public class PacketPotionHotkey implements ClientToServerPacket {
    int potionHotSlot;

    @Override
    public void onReceive(EntityPlayerMP player) {
        ItemStack stack = player.inventory.getStackInSlot(9 + potionHotSlot);
        if (stack.getItem() == Items.POTIONITEM)//если в слоте зелька, то Стив выпьет ее одним глотком :D
        {
            ItemStack stack1 = Items.POTIONITEM.onItemUseFinish(stack, player.world, player);
            System.out.println(stack1.getCount());
            player.inventory.setInventorySlotContents(9 + potionHotSlot, stack1);
        }
    }
/*
    public PacketPotionHotkey(ByteBuf buf) {
        potionHotSlot = buf.readByte();
    }

    @Override
    public ByteBuf serialize() {
        return Unpooled.buffer().writeByte(potionHotSlot);
    }*/
}
