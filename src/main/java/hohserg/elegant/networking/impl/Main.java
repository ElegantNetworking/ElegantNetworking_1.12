package hohserg.elegant.networking.impl;

import com.google.common.collect.SetMultimap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.realmsclient.gui.ChatFormatting;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.util.stream.Collectors.*;

@Mod(modid = "elegant_networking", name = "ElegantNetworking", version = "1.0")
public class Main {

    private static Set<String> channelsToRegister = new HashSet<>();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) throws ClassNotFoundException {
        for (ModContainer modContainer : Loader.instance().getActiveModList()) {
            SetMultimap<String, ASMDataTable.ASMData> annotationsFor = event.getAsmData().getAnnotationsFor(modContainer);
            if (annotationsFor != null) {
                Set<ASMDataTable.ASMData> asmData = annotationsFor.get("hohserg.elegant.networking.api.ElegantPacket");
                Comparator<ASMDataTable.ASMData> comparing = getAsmDataComparator(modContainer.getSource());

                List<ASMDataTable.ASMData> rawPackets =
                        asmData.stream()
                                .filter(a -> {
                                    try {
                                        return Arrays.stream(Class.forName(a.getClassName()).getInterfaces()).anyMatch(i -> i == ClientToServerPacket.class || i == ServerToClientPacket.class);
                                    } catch (ClassNotFoundException e) {
                                        e.printStackTrace();
                                        return false;
                                    }
                                })
                                .sorted(comparing)
                                .collect(toList());

                if (rawPackets.size() > 0) {

                    List<ElegantNetworking.PacketInfo> packets =
                            rawPackets.stream()
                                    .map(a -> new ElegantNetworking.PacketInfo((String) a.getAnnotationInfo().getOrDefault("channel", modContainer.getModId()), a.getClassName()))
                                    .collect(toList());

                    packets.stream().map(p -> p.channel).forEach(channelsToRegister::add);

                    for (int i = 0; i < packets.size(); i++) {
                        ElegantNetworking.PacketInfo p = packets.get(i);
                        String packetPath = p.className.substring(0, p.className.lastIndexOf('.')).replace('.', '/');
                        Optional<? extends Class<?>> maybeSerializer =
                                listResources(packetPath, f -> f.endsWith(".class")).stream()
                                        .map(f -> f.substring(1, f.lastIndexOf('.')).replace('/', '.'))
                                        .flatMap(className -> {
                                            try {
                                                return Stream.of(Class.forName(className));
                                            } catch (ClassNotFoundException e) {
                                                return Stream.empty();
                                            }
                                        })
                                        .filter(cl -> isSerializer(cl, p.className))
                                        .findAny();
                        if (maybeSerializer.isPresent()) {
                            try {
                                ISerializer o = (ISerializer) maybeSerializer.get().newInstance();
                                System.out.println("Register packet " + ChatFormatting.AQUA + Class.forName(p.className).getSimpleName() + ChatFormatting.RESET + " for channel " + ChatFormatting.AQUA + p.channel + ChatFormatting.RESET + " with id " + o.packetId());
                                ElegantNetworking.register(p, o);
                            } catch (InstantiationException | IllegalAccessException e) {
                                System.out.println("Unable to instantiate serializer " + maybeSerializer.get().getName() + " for packet" + ChatFormatting.AQUA + Class.forName(p.className).getSimpleName() + ChatFormatting.RESET + " for channel " + ChatFormatting.AQUA + p.channel);
                                e.printStackTrace();
                            }
                        } else
                            System.out.println("Not found serializer for packet" + ChatFormatting.AQUA + Class.forName(p.className).getSimpleName() + ChatFormatting.RESET + " for channel " + ChatFormatting.AQUA + p.channel);

                    }
                }
            }
        }
    }

    private boolean isSerializer(Class cl, String className) {
        return Arrays.stream(cl.getGenericInterfaces()).anyMatch(i -> i.getTypeName().equals("hohserg.elegant.networking.impl.ISerializer<" + className + ">"));
    }

    public static List<String> listResources(String folder, Predicate<String> filter) {
        String normalizedFolder = normalize(folder);
        List<String> r = new ArrayList<>();

        CraftingHelper.findFiles(Loader.instance().activeModContainer(), normalizedFolder.substring(1, normalizedFolder.length() - 1), null, (root, resource) -> {
            String filename = root.relativize(resource).toString();
            if (filter.test(filename)) {
                r.add(normalizedFolder + filename);
            }

            return true;
        }, true, true);

        return r;
    }

    private static String normalize(String folder) {
        if (!folder.startsWith("/"))
            folder = "/" + folder;
        if (!folder.endsWith("/"))
            folder = folder + "/";
        return folder.replace("\\\\", "\\");
    }


    private Comparator<ASMDataTable.ASMData> getAsmDataComparator(File source) {
        if (source.isFile()) {
            try {
                ZipFile zipFile = new ZipFile(source);
                ZipEntry entry = zipFile.getEntry("META-INF/fml_cache_annotation.json");


                if (entry != null) {
                    String jsonContent = IOUtils.toString(zipFile.getInputStream(entry), StandardCharsets.UTF_8);

                    JsonParser parser = new JsonParser();
                    JsonObject obj = parser.parse(jsonContent).getAsJsonObject();
                    Set<String> classNames = obj.entrySet().stream().map(Map.Entry::getKey).collect(toSet());

                    List<String> classOrder =
                            Arrays.stream(jsonContent.split("\\r?\\n"))
                                    .filter(l -> l.matches("  \\\".+\\\": \\{"))
                                    .filter(classNames::contains)
                                    .collect(toList());
                    Map<String, Integer> indexByClass = IntStream.range(0, classOrder.size())
                            .boxed()
                            .collect(toMap(classOrder::get, Function.identity()));
                    System.out.println(classOrder);
                    return Comparator.comparing(o -> indexByClass.get(o.getClassName()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Comparator.comparing(ASMDataTable.ASMData::getClassName);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        channelsToRegister.forEach(ElegantNetworking.getNetwork()::registerChannel);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
    }
}
