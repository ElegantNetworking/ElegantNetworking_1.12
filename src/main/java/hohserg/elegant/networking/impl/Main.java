package hohserg.elegant.networking.impl;

import com.google.common.collect.SetMultimap;
import com.mojang.realmsclient.gui.ChatFormatting;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

@Mod(modid = "elegant_networking", name = "ElegantNetworking")
public class Main {

    private static Set<String> channelsToRegister = new HashSet<>();
    public static Logger log;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) throws ClassNotFoundException {
        log = event.getModLog();

        for (ModContainer modContainer : Loader.instance().getActiveModList()) {
            SetMultimap<String, ASMDataTable.ASMData> annotationsFor = event.getAsmData().getAnnotationsFor(modContainer);
            if (annotationsFor != null) {

                List<ASMDataTable.ASMData> rawPackets =
                        annotationsFor.get("hohserg.elegant.networking.api.ElegantPacket").stream()
                                .filter(a -> {
                                    try {
                                        return Arrays.stream(Class.forName(a.getClassName()).getInterfaces()).anyMatch(i -> i == ClientToServerPacket.class || i == ServerToClientPacket.class);
                                    } catch (ClassNotFoundException e) {
                                        e.printStackTrace();
                                        return false;
                                    }
                                })
                                .collect(toList());

                Map<String, Class> rawSerializers = annotationsFor.get("hohserg.elegant.networking.impl.SerializerMark").stream()
                        .flatMap(a -> {
                            try {
                                return Stream.of(Class.forName(a.getClassName()));
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                                return Stream.empty();
                            }
                        })
                        .filter(aClass -> Arrays.stream(aClass.getInterfaces()).anyMatch(i -> i == ISerializer.class))
                        .collect(toMap(
                                cl -> cl.getAnnotation(SerializerMark.class).packetClass().getCanonicalName(),
                                cl -> cl));

                log.debug("rawSerializers " + rawSerializers);

                if (rawPackets.size() > 0) {

                    List<ElegantNetworking.PacketInfo> packets =
                            rawPackets.stream()
                                    .map(a -> new ElegantNetworking.PacketInfo((String) a.getAnnotationInfo().getOrDefault("channel", modContainer.getModId()), a.getClassName()))
                                    .collect(toList());

                    packets.stream().map(p -> p.channel).forEach(channelsToRegister::add);

                    for (ElegantNetworking.PacketInfo p : packets) {
                        Class maybeSerializer = rawSerializers.get(p.className);
                        if (maybeSerializer != null) {
                            try {
                                ISerializer o = (ISerializer) maybeSerializer.newInstance();
                                log.info("Register packet " + ChatFormatting.AQUA + Class.forName(p.className).getSimpleName() + ChatFormatting.RESET + " for channel " + ChatFormatting.AQUA + p.channel + ChatFormatting.RESET + " with id " + o.packetId());
                                ElegantNetworking.register(p, o);
                            } catch (InstantiationException | IllegalAccessException e) {
                                log.error("Unable to instantiate serializer " + maybeSerializer.getName() + " for packet" + ChatFormatting.AQUA + Class.forName(p.className).getSimpleName() + ChatFormatting.RESET + " for channel " + ChatFormatting.AQUA + p.channel);
                                e.printStackTrace();
                            }
                        } else
                            log.error("Not found serializer for packet " + ChatFormatting.AQUA + Class.forName(p.className).getSimpleName() + ChatFormatting.RESET + " for channel " + ChatFormatting.AQUA + p.channel);

                    }
                }
            }
        }
    }


    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        channelsToRegister.forEach(ElegantNetworking.getNetwork()::registerChannel);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
    }
}
