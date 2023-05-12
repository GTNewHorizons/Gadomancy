package makeo.gadomancy.common.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;

import makeo.gadomancy.common.network.PacketHandler;
import makeo.gadomancy.common.network.packets.PacketSyncData;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by HellFirePvP @ 13.12.2015 15:53
 */
public class SyncDataHolder {

    private static Map<String, AbstractData> serverData = new HashMap<String, AbstractData>();
    private static Map<String, AbstractData> clientData = new HashMap<String, AbstractData>();

    private static List<String> dirtyData = new ArrayList<String>();
    private static byte providerCounter;

    public static void register(AbstractData.AbstractDataProvider<? extends AbstractData> provider) {
        AbstractData.Registry.register(provider);
        AbstractData ad = provider.provideNewInstance();
        ad.setProviderId(provider.getProviderId());
        SyncDataHolder.serverData.put(provider.getKey(), ad);
        ad = provider.provideNewInstance();
        ad.setProviderId(provider.getProviderId());
        SyncDataHolder.clientData.put(provider.getKey(), ad);
    }

    public static byte allocateNewId() {
        byte pId = SyncDataHolder.providerCounter;
        SyncDataHolder.providerCounter++;
        return pId;
    }

    public static <T extends AbstractData> T getDataServer(String key) {
        return (T) SyncDataHolder.serverData.get(key);
    }

    public static <T extends AbstractData> T getDataClient(String key) {
        return (T) SyncDataHolder.clientData.get(key);
    }

    public static void markForUpdate(String key) {
        if (!SyncDataHolder.dirtyData.contains(key)) {
            SyncDataHolder.dirtyData.add(key);
        }
    }

    public static void syncAllDataTo(EntityPlayer player) {
        PacketSyncData dataSync = new PacketSyncData(SyncDataHolder.serverData, true);
        PacketHandler.INSTANCE.sendTo(dataSync, (net.minecraft.entity.player.EntityPlayerMP) player);
    }

    public static void receiveServerPacket(Map<String, AbstractData> data) {
        for (String key : data.keySet()) {
            AbstractData dat = SyncDataHolder.clientData.get(key);
            if (dat != null) {
                dat.handleIncomingData(data.get(key));
            }
        }
    }

    public static void doNecessaryUpdates() {
        if (SyncDataHolder.dirtyData.isEmpty()) return;
        Map<String, AbstractData> pktData = new HashMap<String, AbstractData>();
        for (String s : SyncDataHolder.dirtyData) {
            AbstractData d = SyncDataHolder.getDataServer(s);
            if (d.needsUpdate()) {
                pktData.put(s, d);
            }
        }
        SyncDataHolder.dirtyData.clear();
        PacketSyncData dataSync = new PacketSyncData(pktData, false);
        PacketHandler.INSTANCE.sendToAll(dataSync);
    }

    public static void initialize() {
        SyncDataHolder.register(new DataFamiliar.Provider("FamiliarData"));
        SyncDataHolder.register(new DataAchromatic.Provider("AchromaticData"));
    }
}
