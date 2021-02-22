package hohserg.elegant.networking.api;

import hohserg.elegant.networking.impl.ISerializerBase;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;

public class NbtSerializer<A extends IByteBufSerializable> {
    private final ISerializerBase<A> serializer;

    public NbtSerializer(ISerializerBase<A> serializer) {
        this.serializer = serializer;
    }

    public NBTTagCompound serialize(A value) {
        NBTTagCompound r = new NBTTagCompound();
        r.setTag("content", serializeToByteArray(value));
        return r;
    }

    public A unserialize(NBTTagCompound nbt) {
        if (nbt.hasKey("content", 7))
            return unserializeFromByteArray((NBTTagByteArray) nbt.getTag("content"));
        else
            throw new IllegalArgumentException("invalid nbt data " + nbt);
    }

    public NBTTagByteArray serializeToByteArray(A value) {
        ByteBuf buffer = Unpooled.buffer();
        serializer.serialize(value, buffer);
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return new NBTTagByteArray(bytes);
    }

    public A unserializeFromByteArray(NBTTagByteArray nbt) {
        ByteBuf buffer = Unpooled.buffer(nbt.getByteArray().length);
        buffer.writeBytes(nbt.getByteArray());
        return serializer.unserialize(buffer);
    }
}
