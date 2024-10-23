package makeo.gadomancy.common.integration.waila;

import makeo.gadomancy.common.blocks.BlockInfusionClaw;
import makeo.gadomancy.common.blocks.BlockRemoteJar;
import makeo.gadomancy.common.blocks.BlockStickyJar;
import mcp.mobius.waila.api.impl.ModuleRegistrar;
import thaumcraft.common.entities.golems.EntityGolemBase;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p>
 * Created by makeo @ 24.07.2015 14:48
 */
public class IntegrationWaila {

    public static void doInit() {
        final ModuleRegistrar reg = ModuleRegistrar.instance();
        StickyJarProvider stickyJarProvider = new StickyJarProvider();
        reg.registerTailProvider(stickyJarProvider, BlockStickyJar.class);
        reg.registerStackProvider(stickyJarProvider, BlockStickyJar.class);
        reg.registerHeadProvider(stickyJarProvider, BlockStickyJar.class);
        reg.registerBodyProvider(stickyJarProvider, BlockStickyJar.class);
        reg.registerTailProvider(new AdvancedGolemProvider(), EntityGolemBase.class);
        reg.registerBodyProvider(new InfusionClawProvider(), BlockInfusionClaw.class);
        reg.registerBodyProvider(new RemoteJarProvider(), BlockRemoteJar.class);
    }
}
