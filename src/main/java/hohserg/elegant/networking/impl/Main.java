package hohserg.elegant.networking.impl;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = "elegant_networking", name = "ElegantNetworking")
public class Main {

    private static Logger log;
    public static Config config;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        log = event.getModLog();
        config = Config.init(event.getModConfigurationDirectory());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        Init.initPackets(log::info, log::warn, Network.getNetwork()::registerChannel, config);
    }
}
