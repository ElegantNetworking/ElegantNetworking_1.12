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

import java.util.UUID;

public interface ISerializer<Packet> {
    void serialize(Packet value, ByteBuf acc);

    Packet unserialize(ByteBuf buf);

    int packetId();

    default void serialize_Boolean_Generic(boolean value, ByteBuf acc) {
        acc.writeBoolean(value);
    }

    default void serialize_Byte_Generic(byte value, ByteBuf acc) {
        acc.writeByte(value);
    }

    default void serialize_Short_Generic(short value, ByteBuf acc) {
        acc.writeShort(value);
    }

    default void serialize_Int_Generic(int value, ByteBuf acc) {
        acc.writeInt(value);
    }

    default void serialize_Long_Generic(long value, ByteBuf acc) {
        acc.writeLong(value);
    }

    default void serialize_Char_Generic(char value, ByteBuf acc) {
        acc.writeChar(value);
    }

    default void serialize_Float_Generic(float value, ByteBuf acc) {
        acc.writeFloat(value);
    }

    default void serialize_Double_Generic(double value, ByteBuf acc) {
        acc.writeDouble(value);
    }

    default void serialize_String_Generic(String value, ByteBuf acc) {
        ByteBufUtils.writeUTF8String(acc, value);
    }

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

    default void serialize_UUID_Generic(UUID value, ByteBuf acc) {
        serialize_String_Generic(value.toString(), acc);
    }


    default boolean unserialize_Boolean_Generic(ByteBuf buf) {
        return buf.readBoolean();
    }

    default byte unserialize_Byte_Generic(ByteBuf buf) {
        return buf.readByte();
    }

    default short unserialize_Short_Generic(ByteBuf buf) {
        return buf.readShort();
    }

    default int unserialize_Int_Generic(ByteBuf buf) {
        return buf.readInt();
    }

    default long unserialize_Long_Generic(ByteBuf buf) {
        return buf.readLong();
    }

    default char unserialize_Char_Generic(ByteBuf buf) {
        return buf.readChar();
    }

    default float unserialize_Float_Generic(ByteBuf buf) {
        return buf.readFloat();
    }

    default double unserialize_Double_Generic(ByteBuf buf) {
        return buf.readDouble();
    }

    default String unserialize_String_Generic(ByteBuf buf) {
        return ByteBufUtils.readUTF8String(buf);
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

    default UUID unserialize_UUID_Generic(ByteBuf buf) {
        return UUID.fromString(unserialize_String_Generic(buf));
    }
}
