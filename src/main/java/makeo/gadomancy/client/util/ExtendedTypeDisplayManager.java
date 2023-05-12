package makeo.gadomancy.client.util;

import net.minecraft.util.StatCollector;

import makeo.gadomancy.client.events.ResourceReloadListener;
import makeo.gadomancy.common.node.ExtendedNodeType;
import thaumcraft.api.nodes.NodeType;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by HellFirePvP @ 23.10.2015 22:53
 */
public class ExtendedTypeDisplayManager {

    private static int timeout = 7;
    private static int currTime;

    private static boolean changedLanguageFile;
    private static String currentNodeId;
    private static String changedEntry;
    private static String oldName;

    public static void notifyRenderTick() {
        if (ExtendedTypeDisplayManager.currTime > ExtendedTypeDisplayManager.timeout) {
            if (ExtendedTypeDisplayManager.changedLanguageFile) {
                ExtendedTypeDisplayManager.resetLanguageFile();
            }
        } else {
            ExtendedTypeDisplayManager.currTime++;
        }
    }

    private static void resetLanguageFile() {
        ExtendedTypeDisplayManager.currentNodeId = null;
        ResourceReloadListener.languageList
                .put(ExtendedTypeDisplayManager.changedEntry, ExtendedTypeDisplayManager.oldName);
        ExtendedTypeDisplayManager.changedLanguageFile = false;
    }

    public static void notifyDisplayTick(String id, NodeType nodeType, ExtendedNodeType extendedNodeType) {
        ExtendedTypeDisplayManager.currTime = 0;

        if (ExtendedTypeDisplayManager.currentNodeId != null && !ExtendedTypeDisplayManager.currentNodeId.equals(id)) {
            // New node.
            ExtendedTypeDisplayManager.resetLanguageFile();
        }

        if (!ExtendedTypeDisplayManager.changedLanguageFile) {
            String toChance = "nodetype." + nodeType + ".name";
            String name = StatCollector.translateToLocal(toChance);
            String growingStr = StatCollector.translateToLocal("gadomancy.nodes." + extendedNodeType.name());
            String newName = name + ", " + growingStr;
            ResourceReloadListener.languageList.put(toChance, newName);

            ExtendedTypeDisplayManager.oldName = name;
            ExtendedTypeDisplayManager.changedEntry = toChance;
            ExtendedTypeDisplayManager.changedLanguageFile = true;
            ExtendedTypeDisplayManager.currentNodeId = id;
        }
    }
}
