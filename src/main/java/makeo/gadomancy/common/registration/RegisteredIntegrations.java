package makeo.gadomancy.common.registration;

import makeo.gadomancy.common.integration.IntegrationAutomagy;
import makeo.gadomancy.common.integration.IntegrationMorph;
import makeo.gadomancy.common.integration.IntegrationNEI;
import makeo.gadomancy.common.integration.IntegrationThaumicExploration;
import makeo.gadomancy.common.integration.LoadedMods;
import makeo.gadomancy.common.integration.mystcraft.IntegrationMystcraft;
import makeo.gadomancy.common.integration.thaumichorizions.IntegrationThaumicHorizions;
import makeo.gadomancy.common.integration.waila.IntegrationWaila;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p>
 * Created by makeo @ 09.07.2015 16:00
 */
public class RegisteredIntegrations {

    public static void init() {
        if (LoadedMods.MORPH) IntegrationMorph.doInit();
        if (LoadedMods.THAUMICEXPLORATION) IntegrationThaumicExploration.doInit();
        if (LoadedMods.AUTOMAGY) IntegrationAutomagy.doInit();
        if (LoadedMods.NOTENOUGHITEMS) IntegrationNEI.doInit();
        if (LoadedMods.MYSTCRAFT) IntegrationMystcraft.doInit();
        if (LoadedMods.THAUMICHORIZONS) IntegrationThaumicHorizions.doInit();
        if (LoadedMods.WAILA) IntegrationWaila.doInit();
    }

}
