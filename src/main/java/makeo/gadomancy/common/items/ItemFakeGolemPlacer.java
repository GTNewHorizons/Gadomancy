package makeo.gadomancy.common.items;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import makeo.gadomancy.api.GadomancyApi;
import makeo.gadomancy.api.golems.AdditionalGolemType;
import thaumcraft.common.entities.golems.EnumGolemType;
import thaumcraft.common.entities.golems.ItemGolemPlacer;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by makeo @ 03.08.2015 14:43
 */
public class ItemFakeGolemPlacer extends Item {

    private static final ItemGolemPlacer PLACER = new ItemGolemPlacer();

    @SideOnly(Side.CLIENT)
    public int getRenderPasses(int metadata) {
        return ItemFakeGolemPlacer.PLACER.getRenderPasses(metadata);
    }

    @SideOnly(Side.CLIENT)
    public boolean requiresMultipleRenderPasses() {
        return true;
    }

    @Override
    public boolean getHasSubtypes() {
        return true;
    }

    @Override
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        for (EnumGolemType type : EnumGolemType.values()) {
            if (type.health > 0) {
                list.add(new ItemStack(this, 1, type.ordinal()));
            }
        }
    }

    @Override
    public void registerIcons(IIconRegister ir) {
        ItemFakeGolemPlacer.PLACER.registerIcons(ir);
        for (AdditionalGolemType type : GadomancyApi.getAdditionalGolemTypes()) {
            type.registerIcons(ir);
        }
    }

    @Override
    public IIcon getIcon(ItemStack stack, int pass) {
        AdditionalGolemType type = this.getAdditonalType(stack);
        if (type != null) {
            return type.getPlacerItem().getIcon(stack, pass);
        }
        return ItemFakeGolemPlacer.PLACER.getIcon(stack, pass);
    }

    private AdditionalGolemType getAdditonalType(ItemStack stack) {
        EnumGolemType type = EnumGolemType.getType(stack.getItemDamage());
        if (type != null) {
            return GadomancyApi.getAdditionalGolemType(type);
        }
        return null;
    }
}
