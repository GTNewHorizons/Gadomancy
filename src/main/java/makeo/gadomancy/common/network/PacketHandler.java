package makeo.gadomancy.common.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import makeo.gadomancy.common.Gadomancy;
import makeo.gadomancy.common.network.packets.PacketAnimationAbsorb;
import makeo.gadomancy.common.network.packets.PacketFamiliarBolt;
import makeo.gadomancy.common.network.packets.PacketStartAnimation;
import makeo.gadomancy.common.network.packets.PacketSyncConfigs;
import makeo.gadomancy.common.network.packets.PacketSyncData;
import makeo.gadomancy.common.network.packets.PacketTCNodeBolt;
import makeo.gadomancy.common.network.packets.PacketTCNotificationText;
import makeo.gadomancy.common.network.packets.PacketTCWispyLine;
import makeo.gadomancy.common.network.packets.PacketUpdateGolemTypeOrder;
import makeo.gadomancy.common.network.packets.PacketUpdateOnlineState;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by makeo @ 03.07.2015 19:13
 */
public class PacketHandler {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE
            .newSimpleChannel(Gadomancy.MODID.toLowerCase());

    public static void init() {
        int id = 0;

        // Random stuff
        PacketHandler.INSTANCE
                .registerMessage(PacketUpdateGolemTypeOrder.class, PacketUpdateGolemTypeOrder.class, id++, Side.CLIENT);
        PacketHandler.INSTANCE
                .registerMessage(PacketStartAnimation.class, PacketStartAnimation.class, id++, Side.CLIENT);
        PacketHandler.INSTANCE
                .registerMessage(PacketAnimationAbsorb.class, PacketAnimationAbsorb.class, id++, Side.CLIENT);

        // Using TC Clientside content
        PacketHandler.INSTANCE.registerMessage(PacketTCNodeBolt.class, PacketTCNodeBolt.class, id++, Side.CLIENT);
        PacketHandler.INSTANCE.registerMessage(PacketTCWispyLine.class, PacketTCWispyLine.class, id++, Side.CLIENT);
        PacketHandler.INSTANCE
                .registerMessage(PacketTCNotificationText.class, PacketTCNotificationText.class, id++, Side.CLIENT);

        // Familiar
        PacketHandler.INSTANCE.registerMessage(PacketFamiliarBolt.class, PacketFamiliarBolt.class, id++, Side.CLIENT);

        // Sync data
        PacketHandler.INSTANCE.registerMessage(PacketSyncConfigs.class, PacketSyncConfigs.class, id++, Side.CLIENT);
        PacketHandler.INSTANCE.registerMessage(PacketSyncData.class, PacketSyncData.class, id++, Side.CLIENT);
        PacketHandler.INSTANCE
                .registerMessage(PacketUpdateOnlineState.class, PacketUpdateOnlineState.class, id++, Side.CLIENT);
    }
}
