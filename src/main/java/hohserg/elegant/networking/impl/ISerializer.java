package hohserg.elegant.networking.impl;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public interface ISerializer<Packet> extends ISerializerBase<Packet>, RegistrableSingletonSerializer {

    default void serialize_BlockPos_Generic(BlockPos value, ByteBuf acc) {
        acc.writeInt(value.getX());
        acc.writeInt(value.getY());
        acc.writeInt(value.getZ());
    }

    default BlockPos unserialize_BlockPos_Generic(ByteBuf buf) {
        return new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
    }

    default void serialize_NBTTagCompound_Generic(NBTTagCompound value, ByteBuf acc) {
        Preconditions.checkNotNull(value);
        ByteBufUtils.writeTag(acc, value);
    }

    default NBTTagCompound unserialize_NBTTagCompound_Generic(ByteBuf buf) {
        NBTTagCompound value = ByteBufUtils.readTag(buf);
        return value != null ? value : new NBTTagCompound();
    }

    default void serialize_ItemStack_Generic(ItemStack value, ByteBuf acc) {
        ByteBufUtils.writeItemStack(acc, value);
    }

    default ItemStack unserialize_ItemStack_Generic(ByteBuf buf) {
        return ByteBufUtils.readItemStack(buf);
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

    default void serialize_Fluid_Generic(Fluid value, ByteBuf acc) {
        serialize_String_Generic(FluidRegistry.getFluidName(value), acc);
    }

    default Fluid unserialize_Fluid_Generic(ByteBuf buf) {
        return FluidRegistry.getFluid(unserialize_String_Generic(buf));
    }

    default void serialize_ResourceLocation_Generic(ResourceLocation value, ByteBuf acc) {
        serialize_String_Generic(value.toString(), acc);
    }

    default ResourceLocation unserialize_ResourceLocation_Generic(ByteBuf buf) {
        return new ResourceLocation(unserialize_String_Generic(buf));
    }
}
