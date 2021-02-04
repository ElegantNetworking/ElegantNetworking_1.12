package hohserg.elegant.networking.impl;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

import java.util.stream.Collectors;

@Mod(modid = "elegant_networking", name = "ElegantNetworking")
public class Main {

    public static Logger log;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        log = event.getModLog();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        Init.registerAllPackets(
                Loader.instance().getActiveModList()
                        .stream()
                        .map(mod -> new Init.ModInfo(mod.getModId(), mod.getSource()))
                        .collect(Collectors.toList()),
                log::error,
                Network.getNetwork()::registerChannel
        );
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
    }
}
