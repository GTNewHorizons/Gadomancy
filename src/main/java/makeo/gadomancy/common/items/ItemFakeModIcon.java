package makeo.gadomancy.common.items;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;

import makeo.gadomancy.common.Gadomancy;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by makeo @ 20.10.2015 22:49
 */
public class ItemFakeModIcon extends Item {

    public ItemFakeModIcon() {
        this.setTextureName(Gadomancy.MODID + ":category_icon");
    }

    public IIcon[] icons = new IIcon[6];

    @Override
    public void registerIcons(IIconRegister ir) {
        this.icons[0] = ir.registerIcon(Gadomancy.MODID + ":category_icon");
        this.icons[1] = ir.registerIcon(Gadomancy.MODID + ":Attack1");
        this.icons[2] = ir.registerIcon(Gadomancy.MODID + ":Attack2");
        this.icons[3] = ir.registerIcon(Gadomancy.MODID + ":Attack3");
        this.icons[4] = ir.registerIcon(Gadomancy.MODID + ":Range1");
        this.icons[5] = ir.registerIcon(Gadomancy.MODID + ":Cooldown1");
    }

    @Override
    public IIcon getIconFromDamage(int damage) {
        if (damage < 0 || damage >= this.icons.length) return null;
        return this.icons[damage];
    }
}
