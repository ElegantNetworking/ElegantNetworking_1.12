package hohserg.elegant.networking.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import hohserg.elegant.networking.api.IByteBufSerializable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static hohserg.elegant.networking.impl.DataUtils2.CollectionFlags.ElementsCongeneric;
import static hohserg.elegant.networking.impl.DataUtils2.CollectionFlags.ElementsHeterogeneric;

public class DataUtils2 {


    public static ByteBuf serialize(Object value) {
        ByteBuf r = Unpooled.buffer();
        serialize(value, r);
        return r;
    }

    /**
     * Generic unserialization
     *
     * @param buf ByteBuf representation of packet
     * @return Packet object
     */
    static <A> A unserialize(ByteBuf buf, int packetId) {
        try {
            return (A) unserialize(buf, Class.forName(ElegantNetworking.packetClassNameById.get(packetId)));
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException("Unable to unserialize packet: class not found", e);
        }
    }

    enum CollectionFlags {
        ElementsCongeneric, ElementsHeterogeneric
    }

    /**
     * Generic serialization
     *
     * @param value Some object
     * @param acc
     * @return ByteBuf representation of object
     */
    public static void serialize(Object value, ByteBuf acc) {
        acc.writeBoolean(value == null);
        if (value != null) {
            Class<?> valueClass = value.getClass();
            if (valueClass == Integer.class) acc.writeInt((Integer) value);
            else if (valueClass == Double.class) acc.writeDouble((Double) value);
            else if (valueClass == Float.class) acc.writeFloat((Float) value);
            else if (valueClass == Long.class) acc.writeLong((Long) value);
            else if (valueClass == Short.class) acc.writeShort((Short) value);
            else if (valueClass == Byte.class) acc.writeByte((Byte) value);
            else if (valueClass == Character.class) acc.writeChar((Character) value);
            else if (valueClass == Boolean.class) acc.writeBoolean((Boolean) value);
            else if (valueClass == String.class) ByteBufUtils.writeUTF8String(acc, (String) value);
            else if (value instanceof Enum) ByteBufUtils.writeUTF8String(acc, ((Enum) value).name());
            else if (value instanceof Map) {
                Map<Object, Object> mapValue = (Map<Object, Object>) value;
                acc.writeShort(mapValue.size());
                if (!mapValue.isEmpty()) {
                    CollectionFlags keyFlag = writeCollectionMeta(acc, mapValue.keySet());
                    CollectionFlags valueFlag = writeCollectionMeta(acc, mapValue.values());
                    mapValue.forEach((k, v) -> {
                        if (keyFlag == ElementsHeterogeneric)
                            ByteBufUtils.writeUTF8String(acc, k.getClass().getName());
                        serialize(k, acc);

                        if (valueFlag == ElementsHeterogeneric)
                            ByteBufUtils.writeUTF8String(acc, v.getClass().getName());
                        serialize(v, acc);
                    });
                }
            } else if (value instanceof Collection) {
                Collection<Object> collectionValue = (Collection<Object>) value;
                acc.writeShort(collectionValue.size());
                if (!collectionValue.isEmpty()) {
                    CollectionFlags flag = writeCollectionMeta(acc, collectionValue);
                    if (flag == ElementsHeterogeneric) {
                        collectionValue.forEach(e -> {
                            ByteBufUtils.writeUTF8String(acc, e.getClass().getName());
                            serialize(e, acc);
                        });
                    } else {
                        collectionValue.forEach(e -> serialize(e, acc));
                    }
                }
            } else if (value instanceof IByteBufSerializable && haveUnserializeConstructor(valueClass)) {

                ByteBuf buffer = Unpooled.buffer();
                ((IByteBufSerializable) value).serialize(buffer);
                acc.writeShort(buffer.readableBytes());
                acc.writeBytes(buffer);

            } else try {
                for (Field field : value.getClass().getDeclaredFields())
                    if (!Modifier.isTransient(field.getModifiers())) {
                        field.setAccessible(true);
                        if (field.getType() == int.class) acc.writeInt(field.getInt(value));
                        else if (field.getType() == double.class) acc.writeDouble(field.getDouble(value));
                        else if (field.getType() == float.class) acc.writeFloat(field.getFloat(value));
                        else if (field.getType() == long.class) acc.writeLong(field.getLong(value));
                        else if (field.getType() == short.class) acc.writeShort(field.getShort(value));
                        else if (field.getType() == byte.class) acc.writeByte(field.getByte(value));
                        else if (field.getType() == char.class) acc.writeChar(field.getChar(value));
                        else if (field.getType() == boolean.class) acc.writeBoolean(field.getBoolean(value));
                        else {
                            if (!Modifier.isFinal(field.getType().getModifiers()))
                                ByteBufUtils.writeUTF8String(acc, field.getType().getName());
                            serialize(field.get(value), acc);
                        }
                    }
            } catch (IllegalAccessException e) {
                throw new UnsupportedOperationException("Unable to serialize object " + value, e);
            }
        }
    }

    public static <A> Object unserialize(ByteBuf buf, Class<A> valueClass) throws ClassNotFoundException {
        boolean isNull = buf.readBoolean();
        if (isNull)
            return null;
        else {
            if (valueClass == Integer.class) return buf.readInt();
            else if (valueClass == Double.class) return buf.readDouble();
            else if (valueClass == Float.class) return buf.readFloat();
            else if (valueClass == Long.class) return buf.readLong();
            else if (valueClass == Short.class) return buf.readShort();
            else if (valueClass == Byte.class) return buf.readByte();
            else if (valueClass == Character.class) return buf.readChar();
            else if (valueClass == Boolean.class) return buf.readBoolean();
            else if (valueClass == String.class) return ByteBufUtils.readUTF8String(buf);
            else if (Enum.class.isAssignableFrom(valueClass)) return Enum.valueOf((Class) valueClass, ByteBufUtils.readUTF8String(buf));
            else if (Map.class.isAssignableFrom(valueClass)) {
                CollectionBuilder collectionBuilder = supportedCollections.getOrDefault(valueClass, hashMapBuilder).get();

                int size = buf.readShort();
                if (size == 0)
                    return collectionBuilder.build();

                CollectionFlags keyFlag = CollectionFlags.values()[buf.readByte()];

                Class kClass = keyFlag == ElementsCongeneric ? Class.forName(ByteBufUtils.readUTF8String(buf)) : Object.class;

                CollectionFlags valueFlag = CollectionFlags.values()[buf.readByte()];

                Class vClass = valueFlag == ElementsCongeneric ? Class.forName(ByteBufUtils.readUTF8String(buf)) : Object.class;


                for (int i = 0; i < size; i++) {
                    collectionBuilder.add(Pair.of(
                            unserialize(buf, keyFlag == ElementsCongeneric ? kClass : Class.forName(ByteBufUtils.readUTF8String(buf))),
                            unserialize(buf, valueFlag == ElementsCongeneric ? vClass : Class.forName(ByteBufUtils.readUTF8String(buf)))
                    ));
                }
                return collectionBuilder.build();
            } else if (Collection.class.isAssignableFrom(valueClass)) {
                CollectionBuilder collectionBuilder = supportedCollections.getOrDefault(valueClass,
                        List.class.isAssignableFrom(valueClass) ? arrayListBuilder :
                                Set.class.isAssignableFrom(valueClass) ? hashSetBuilder :
                                        unsopportedCollection(valueClass)
                ).get();

                int size = buf.readShort();
                if (size == 0)
                    return collectionBuilder.build();

                CollectionFlags flag = CollectionFlags.values()[buf.readByte()];

                if (flag == ElementsHeterogeneric) {
                    for (int i = 0; i < size; i++) {
                        Class<?> elementClass = Class.forName(ByteBufUtils.readUTF8String(buf));
                        collectionBuilder.add(unserialize(buf, elementClass));
                    }
                } else {
                    Class<?> elementClass = Class.forName(ByteBufUtils.readUTF8String(buf));
                    for (int i = 0; i < size; i++)
                        collectionBuilder.add(unserialize(buf, elementClass));

                }
                return collectionBuilder.build();
            } else if (IByteBufSerializable.class.isAssignableFrom(valueClass) && haveUnserializeConstructor(valueClass)) {
                try {
                    Constructor<A> unserializeConstructor = valueClass.getDeclaredConstructor(ByteBuf.class);
                    short chunkSize = buf.readShort();
                    return unserializeConstructor.newInstance(buf.readBytes(chunkSize));
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new UnsupportedOperationException("Unable to unserialize packet with unserialization constructor " + valueClass, e);
                }
            } else
                try {
                    try {
                        Constructor<A> defaultConstructor = valueClass.getDeclaredConstructor();
                        defaultConstructor.setAccessible(true);
                        A value = defaultConstructor.newInstance();

                        for (Field field : valueClass.getDeclaredFields())
                            if (!Modifier.isTransient(field.getModifiers())) {
                                field.setAccessible(true);
                                if (field.getType() == int.class)
                                    field.setInt(value, buf.readInt());
                                else if (field.getType() == double.class) field.setDouble(value, buf.readDouble());
                                else if (field.getType() == float.class) field.setFloat(value, buf.readFloat());
                                else if (field.getType() == long.class) field.setLong(value, buf.readLong());
                                else if (field.getType() == short.class) field.setShort(value, buf.readShort());
                                else if (field.getType() == byte.class) field.setByte(value, buf.readByte());
                                else if (field.getType() == char.class) field.setChar(value, buf.readChar());
                                else if (field.getType() == boolean.class) field.setBoolean(value, buf.readBoolean());
                                else {
                                    Class<?> fieldConcreteClass = Modifier.isFinal(field.getType().getModifiers()) ? field.getType() : Class.forName(ByteBufUtils.readUTF8String(buf));
                                    field.set(value, unserialize(buf, fieldConcreteClass));
                                }
                            }
                        return value;

                    } catch (NoSuchMethodException e) {
                        Constructor<?> dataConstructor = valueClass.getDeclaredConstructors()[0];
                        dataConstructor.setAccessible(true);
                        List<Object> args = new ArrayList<>(dataConstructor.getParameterCount());
                        for (Field field : valueClass.getDeclaredFields())
                            if (!Modifier.isTransient(field.getModifiers())) {
                                field.setAccessible(true);
                                if (field.getType() == int.class) args.add(buf.readInt());
                                else if (field.getType() == double.class) args.add(buf.readDouble());
                                else if (field.getType() == float.class) args.add(buf.readFloat());
                                else if (field.getType() == long.class) args.add(buf.readLong());
                                else if (field.getType() == short.class) args.add(buf.readShort());
                                else if (field.getType() == byte.class) args.add(buf.readByte());
                                else if (field.getType() == char.class) args.add(buf.readChar());
                                else if (field.getType() == boolean.class) args.add(buf.readBoolean());
                                else {
                                    Class<?> fieldConcreteClass = Modifier.isFinal(field.getType().getModifiers()) ? field.getType() : Class.forName(ByteBufUtils.readUTF8String(buf));
                                    args.add(unserialize(buf, fieldConcreteClass));
                                }
                            }
                        return dataConstructor.newInstance(args.toArray(new Object[0]));
                    }
                } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                    throw new UnsupportedOperationException("Unable to unserialize object " + valueClass, e);
                }
        }
    }

    private static <A> boolean haveUnserializeConstructor(Class<A> valueClass) {
        try {
            return valueClass.getDeclaredConstructor(ByteBuf.class) != null;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static CollectionFlags writeCollectionMeta(ByteBuf acc, Collection<Object> collection) {
        Set<Class> classSet = collection.stream().map(Object::getClass).collect(Collectors.toSet());
        if (classSet.size() == 1) {
            acc.writeByte(ElementsCongeneric.ordinal());
            ByteBufUtils.writeUTF8String(acc, classSet.iterator().next().getName());
            return ElementsCongeneric;
        } else {
            acc.writeByte(ElementsHeterogeneric.ordinal());
            return ElementsHeterogeneric;
        }
    }

    private interface CollectionBuilder<A, C> {
        void add(A v);

        C build();
    }

    private static final Supplier<CollectionBuilder> hashMapBuilder = () -> new CollectionBuilder<Map.Entry, HashMap>() {
        private HashMap<Object, Object> builder = new HashMap<>();

        public void add(Map.Entry v) {
            builder.put(v.getKey(), v.getValue());
        }

        public HashMap build() {
            return builder;
        }
    };
    private static final Supplier<CollectionBuilder> arrayListBuilder = () -> new CollectionBuilder<Object, ArrayList>() {
        private ArrayList<Object> builder = new ArrayList<>();

        public void add(Object v) {
            builder.add(v);
        }

        public ArrayList build() {
            return builder;
        }
    };
    private static final Supplier<CollectionBuilder> hashSetBuilder = () -> new CollectionBuilder<Object, HashSet>() {
        private HashSet<Object> builder = new HashSet<>();

        public void add(Object v) {
            builder.add(v);
        }

        public HashSet build() {
            return builder;
        }
    };

    private static Supplier<CollectionBuilder> unsopportedCollection(Class valueClass) {
        throw new UnsupportedOperationException("Unsopported collection class " + valueClass);
    }

    private static Map<Class, Supplier<CollectionBuilder>> supportedCollections =
            ImmutableMap.<Class, Supplier<CollectionBuilder>>builder()
                    .put(ImmutableMap.class, () -> new CollectionBuilder<Map.Entry, ImmutableMap>() {
                        private ImmutableMap.Builder builder = ImmutableMap.builder();

                        public void add(Map.Entry v) {
                            builder.put(v);
                        }

                        public ImmutableMap build() {
                            return builder.build();
                        }
                    })
                    .put(ImmutableSet.class, () -> new CollectionBuilder<Object, ImmutableSet>() {
                        private ImmutableSet.Builder builder = ImmutableSet.builder();

                        public void add(Object v) {
                            builder.add(v);
                        }

                        public ImmutableSet build() {
                            return builder.build();
                        }
                    })
                    .put(ImmutableList.class, () -> new CollectionBuilder<Object, ImmutableList>() {
                        private ImmutableList.Builder builder = ImmutableList.builder();

                        public void add(Object v) {
                            builder.add(v);
                        }

                        public ImmutableList build() {
                            return builder.build();
                        }
                    })
                    .put(ArrayList.class, arrayListBuilder)
                    .put(HashMap.class, hashMapBuilder)
                    .put(HashSet.class, hashSetBuilder)
                    .build();
}
