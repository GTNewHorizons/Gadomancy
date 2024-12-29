package makeo.gadomancy.common.integration;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import makeo.gadomancy.common.registration.RegisteredBlocks;
import makeo.gadomancy.common.registration.RegisteredItems;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by makeo @ 04.10.2015 01:52
 */
public class IntegrationThaumicExploration {

    public static void doInit() {
        Block trashJar = Block.getBlockFromName("ThaumicExploration:trashJar");
        if (trashJar != null) {
            RegisteredBlocks.registerStickyJar(trashJar, 0, false, true);
            RegisteredItems.registerStickyJar(Item.getItemFromBlock(trashJar), 0, new ItemStack(trashJar), "TRASHJAR");
        }
    }
}
