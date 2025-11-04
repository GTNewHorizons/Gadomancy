package makeo.gadomancy.common.integration;

import net.minecraft.item.ItemStack;

import codechicken.nei.api.API;
import makeo.gadomancy.common.registration.RegisteredBlocks;
import makeo.gadomancy.common.registration.RegisteredItems;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p/>
 * Created by makeo @ 02.12.2015 13:46
 */
public class IntegrationNEI extends IntegrationMod {

    @Override
    public String getModId() {
        return "NotEnoughItems";
    }

    @Override
    protected void doInit() {
        API.hideItem(new ItemStack(RegisteredItems.itemFakeModIcon));
        API.hideItem(new ItemStack(RegisteredBlocks.blockStickyJar, 1, Short.MAX_VALUE));
        API.hideItem(new ItemStack(RegisteredItems.itemPackage, 1, Short.MAX_VALUE));
        API.hideItem(new ItemStack(RegisteredItems.itemFakeLootbag, 1, Short.MAX_VALUE));
        API.hideItem(new ItemStack(RegisteredBlocks.blockExtendedNodeJar, 1, Short.MAX_VALUE));
        API.hideItem(new ItemStack(RegisteredItems.itemExtendedNodeJar, 1, Short.MAX_VALUE));
        API.hideItem(new ItemStack(RegisteredItems.itemFakeGolemPlacer, 1, Short.MAX_VALUE));
        API.hideItem(new ItemStack(RegisteredItems.itemTransformationFocus, 1, Short.MAX_VALUE));
        API.hideItem(new ItemStack(RegisteredItems.itemFamiliar_old, 1, Short.MAX_VALUE));
    }
}
