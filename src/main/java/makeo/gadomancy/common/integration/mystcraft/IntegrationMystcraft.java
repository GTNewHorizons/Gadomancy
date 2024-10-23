package makeo.gadomancy.common.integration.mystcraft;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p/>
 * Created by HellFirePvP @ 20.12.2015 15:50
 */
public class IntegrationMystcraft {

    public static void doInit() {
        EventHandlerMystcraft eventHandler = new EventHandlerMystcraft();
        MinecraftForge.EVENT_BUS.register(eventHandler);
        FMLCommonHandler.instance().bus().register(eventHandler);
    }
}
