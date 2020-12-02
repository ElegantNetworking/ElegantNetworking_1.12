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

    default void serializeBooleanGeneric(boolean value, ByteBuf acc) {
        acc.writeBoolean(value);
    }

    default void serializeByteGeneric(byte value, ByteBuf acc) {
        acc.writeByte(value);
    }

    default void serializeShortGeneric(short value, ByteBuf acc) {
        acc.writeShort(value);
    }

    default void serializeIntGeneric(int value, ByteBuf acc) {
        acc.writeInt(value);
    }

    default void serializeLongGeneric(long value, ByteBuf acc) {
        acc.writeLong(value);
    }

    default void serializeCharGeneric(char value, ByteBuf acc) {
        acc.writeChar(value);
    }

    default void serializeFloatGeneric(float value, ByteBuf acc) {
        acc.writeFloat(value);
    }

    default void serializeDoubleGeneric(double value, ByteBuf acc) {
        acc.writeDouble(value);
    }

    default void serializeStringGeneric(String value, ByteBuf acc) {
        ByteBufUtils.writeUTF8String(acc, value);
    }

    default void serializeNBTTagCompoundGeneric(NBTTagCompound value, ByteBuf acc) {
        ByteBufUtils.writeTag(acc, value);
    }

    default void serializeItemStackGeneric(ItemStack value, ByteBuf acc) {
        ByteBufUtils.writeItemStack(acc, value);
    }

    default void serializeFluidStackGeneric(FluidStack value, ByteBuf acc) {
        serializeFluidGeneric(value.getFluid(), acc);
        acc.writeInt(value.amount);
        if (value.tag != null) {
            acc.writeByte(1);
            serializeNBTTagCompoundGeneric(value.tag, acc);
        } else
            acc.writeByte(0);
    }

    default void serializeItemGeneric(Item value, ByteBuf acc) {
        acc.writeShort(Item.getIdFromItem(value));
    }

    default void serializeBlockGeneric(Block value, ByteBuf acc) {
        acc.writeShort(Block.getIdFromBlock(value));
    }

    default void serializeFluidGeneric(Fluid value, ByteBuf acc) {
        serializeStringGeneric(FluidRegistry.getFluidName(value), acc);
    }

    default void serializeResourceLocationGeneric(ResourceLocation value, ByteBuf acc) {
        serializeStringGeneric(value.toString(), acc);
    }

    default void serializeUUIDGeneric(UUID value, ByteBuf acc) {
        serializeStringGeneric(value.toString(), acc);
    }


    default boolean unserializeBooleanGeneric(ByteBuf buf) {
        return buf.readBoolean();
    }

    default byte unserializeByteGeneric(ByteBuf buf) {
        return buf.readByte();
    }

    default short unserializeShortGeneric(ByteBuf buf) {
        return buf.readShort();
    }

    default int unserializeIntGeneric(ByteBuf buf) {
        return buf.readInt();
    }

    default long unserializeLongGeneric(ByteBuf buf) {
        return buf.readLong();
    }

    default char unserializeCharGeneric(ByteBuf buf) {
        return buf.readChar();
    }

    default float unserializeFloatGeneric(ByteBuf buf) {
        return buf.readFloat();
    }

    default double unserializeDoubleGeneric(ByteBuf buf) {
        return buf.readDouble();
    }

    default String unserializeStringGeneric(ByteBuf buf) {
        return ByteBufUtils.readUTF8String(buf);
    }

    default NBTTagCompound unserializeNBTTagCompoundGeneric(ByteBuf buf) {
        return ByteBufUtils.readTag(buf);
    }

    default ItemStack unserializeItemStackGeneric(ByteBuf buf) {
        return ByteBufUtils.readItemStack(buf);
    }

    default FluidStack unserializeFluidStackGeneric(ByteBuf buf) {
        Fluid fluid = unserializeFluidGeneric(buf);
        if (fluid != null) {
            FluidStack stack = new FluidStack(fluid, buf.readInt());
            if (buf.readByte() == 1)
                stack.tag = unserializeNBTTagCompoundGeneric(buf);
            return stack;
        } else
            return null;
    }

    default Item unserializeItemGeneric(ByteBuf buf) {
        return Item.getItemById(buf.readShort());
    }

    default Block unserializeBlockGeneric(ByteBuf buf) {
        return Block.getBlockById(buf.readShort());
    }

    default Fluid unserializeFluidGeneric(ByteBuf buf) {
        return FluidRegistry.getFluid(unserializeStringGeneric(buf));
    }

    default ResourceLocation unserializeResourceLocationGeneric(ByteBuf buf) {
        return new ResourceLocation(unserializeStringGeneric(buf));
    }

    default UUID unserializeUUIDGeneric(ByteBuf buf) {
        return UUID.fromString(unserializeStringGeneric(buf));
    }
}
