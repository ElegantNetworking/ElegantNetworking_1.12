package hohserg.elegant.networking.impl;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionType;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

/*
pattern:

ABC_([A-z]+)

replacement:

default void serialize_$1_Generic($1 value, ByteBuf acc) {
    RegistryHandler.instance.serializeSingleton($1.class, value, acc);
}

default $1 unserialize_$1_Generic(ByteBuf buf) {
    return RegistryHandler.instance.unserializeSingleton($1.class, buf);
}
*/
public interface RegistrableSingletonSerializer {

    default void serialize_Item_Generic(Item value, ByteBuf acc) {
        RegistryHandler.instance.serializeSingleton(Item.class, value, acc);
    }

    default Item unserialize_Item_Generic(ByteBuf buf) {
        return RegistryHandler.instance.unserializeSingleton(Item.class, buf);
    }

    default void serialize_Block_Generic(Block value, ByteBuf acc) {
        RegistryHandler.instance.serializeSingleton(Block.class, value, acc);
    }

    default Block unserialize_Block_Generic(ByteBuf buf) {
        return RegistryHandler.instance.unserializeSingleton(Block.class, buf);
    }

    default void serialize_Potion_Generic(Potion value, ByteBuf acc) {
        RegistryHandler.instance.serializeSingleton(Potion.class, value, acc);
    }

    default Potion unserialize_Potion_Generic(ByteBuf buf) {
        return RegistryHandler.instance.unserializeSingleton(Potion.class, buf);
    }

    default void serialize_Biome_Generic(Biome value, ByteBuf acc) {
        RegistryHandler.instance.serializeSingleton(Biome.class, value, acc);
    }

    default Biome unserialize_Biome_Generic(ByteBuf buf) {
        return RegistryHandler.instance.unserializeSingleton(Biome.class, buf);
    }

    default void serialize_SoundEvent_Generic(SoundEvent value, ByteBuf acc) {
        RegistryHandler.instance.serializeSingleton(SoundEvent.class, value, acc);
    }

    default SoundEvent unserialize_SoundEvent_Generic(ByteBuf buf) {
        return RegistryHandler.instance.unserializeSingleton(SoundEvent.class, buf);
    }

    default void serialize_PotionType_Generic(PotionType value, ByteBuf acc) {
        RegistryHandler.instance.serializeSingleton(PotionType.class, value, acc);
    }

    default PotionType unserialize_PotionType_Generic(ByteBuf buf) {
        return RegistryHandler.instance.unserializeSingleton(PotionType.class, buf);
    }

    default void serialize_Enchantment_Generic(Enchantment value, ByteBuf acc) {
        RegistryHandler.instance.serializeSingleton(Enchantment.class, value, acc);
    }

    default Enchantment unserialize_Enchantment_Generic(ByteBuf buf) {
        return RegistryHandler.instance.unserializeSingleton(Enchantment.class, buf);
    }

    enum RegistryHandler implements IRegistryHandlerBase {
        instance;

        RegistryHandler() {
            register(Item.class, ForgeRegistries.ITEMS);
            register(Block.class, ForgeRegistries.BLOCKS);
            register(Potion.class, ForgeRegistries.POTIONS);
            register(Biome.class, ForgeRegistries.BIOMES);
            register(SoundEvent.class, ForgeRegistries.SOUND_EVENTS);
            register(PotionType.class, ForgeRegistries.POTION_TYPES);
            register(Enchantment.class, ForgeRegistries.ENCHANTMENTS);
        }

        private <A extends IForgeRegistryEntry<A>> void register(Class<A> type, IForgeRegistry<A> forgeRegistry) {
            register(type, ((ForgeRegistry<A>) forgeRegistry)::getID, ((ForgeRegistry<A>) forgeRegistry)::getValue);
        }
    }
}
