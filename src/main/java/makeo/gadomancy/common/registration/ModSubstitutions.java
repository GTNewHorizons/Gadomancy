package makeo.gadomancy.common.registration;

import makeo.gadomancy.common.CommonProxy;
import makeo.gadomancy.common.data.config.ModConfig;
import net.minecraft.init.Blocks;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by HellFirePvP @ 24.10.2015 16:48
 */
public class ModSubstitutions {
    public static void postInit() {
        if (ModConfig.enableAdditionalNodeTypes) {
            CommonProxy.unregisterWandHandler("Thaumcraft", Blocks.glass, -1);
        }
    }
}
