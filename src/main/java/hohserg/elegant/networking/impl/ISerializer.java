package hohserg.elegant.networking.impl;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public interface ISerializer<Packet> extends ISerializerBase<Packet> {

    default void serialize_NBTTagCompound_Generic(NBTTagCompound value, ByteBuf acc) {
        ByteBufUtils.writeTag(acc, value);
    }

    default void serialize_ItemStack_Generic(ItemStack value, ByteBuf acc) {
        ByteBufUtils.writeItemStack(acc, value);
    }

    default void serialize_FluidStack_Generic(FluidStack value, ByteBuf acc) {
        serialize_Fluid_Generic(value.getFluid(), acc);
        acc.writeInt(value.amount);
        if (value.tag != null) {
            acc.writeByte(1);
            serialize_NBTTagCompound_Generic(value.tag, acc);
        } else
            acc.writeByte(0);
    }

    default void serialize_Item_Generic(Item value, ByteBuf acc) {
        acc.writeShort(Item.getIdFromItem(value));
    }

    default void serialize_Block_Generic(Block value, ByteBuf acc) {
        acc.writeShort(Block.getIdFromBlock(value));
    }

    default void serialize_Fluid_Generic(Fluid value, ByteBuf acc) {
        serialize_String_Generic(FluidRegistry.getFluidName(value), acc);
    }

    default void serialize_ResourceLocation_Generic(ResourceLocation value, ByteBuf acc) {
        serialize_String_Generic(value.toString(), acc);
    }


    default NBTTagCompound unserialize_NBTTagCompound_Generic(ByteBuf buf) {
        return ByteBufUtils.readTag(buf);
    }

    default ItemStack unserialize_ItemStack_Generic(ByteBuf buf) {
        return ByteBufUtils.readItemStack(buf);
    }

    default FluidStack unserialize_FluidStack_Generic(ByteBuf buf) {
        Fluid fluid = unserialize_Fluid_Generic(buf);
        if (fluid != null) {
            FluidStack stack = new FluidStack(fluid, buf.readInt());
            if (buf.readByte() == 1)
                stack.tag = unserialize_NBTTagCompound_Generic(buf);
            return stack;
        } else
            return null;
    }

    default Item unserialize_Item_Generic(ByteBuf buf) {
        return Item.getItemById(buf.readShort());
    }

    default Block unserialize_Block_Generic(ByteBuf buf) {
        return Block.getBlockById(buf.readShort());
    }

    default Fluid unserialize_Fluid_Generic(ByteBuf buf) {
        return FluidRegistry.getFluid(unserialize_String_Generic(buf));
    }

    default ResourceLocation unserialize_ResourceLocation_Generic(ByteBuf buf) {
        return new ResourceLocation(unserialize_String_Generic(buf));
    }
}
