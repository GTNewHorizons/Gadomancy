package makeo.gadomancy.common.events;

import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import makeo.gadomancy.common.registration.RegisteredEnchantments;
import makeo.gadomancy.common.utils.MiscUtils;
import thaumcraft.common.config.ConfigItems;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p/>
 * Created by HellFirePvP @ 15.12.2015 14:57
 */
public class EventHandlerRedirect {

    private static final ItemStack ITEM_GOGGLES = new ItemStack(ConfigItems.itemGoggles);
    private static boolean hasChanged;
    private static ItemStack oldItem;

    public static void addGoggles(Entity entity) {
        if (entity instanceof EntityPlayer && EventHandlerRedirect.hasGoggles((EntityPlayer) entity)) {
            ItemStack[] armorInv = ((EntityPlayer) entity).inventory.armorInventory;
            EventHandlerRedirect.oldItem = armorInv[3];
            EventHandlerRedirect.hasChanged = true;
            armorInv[3] = EventHandlerRedirect.ITEM_GOGGLES;
        }
    }

    public static void removeGoggles(Entity entity) {
        if (EventHandlerRedirect.hasChanged && entity instanceof EntityPlayer) {
            ((EntityPlayer) entity).inventory.armorInventory[3] = EventHandlerRedirect.oldItem;
            EventHandlerRedirect.oldItem = null;
            EventHandlerRedirect.hasChanged = false;
        }
    }

    private static boolean hasGoggles(EntityPlayer player) {
        ItemStack stack = player.inventory.armorItemInSlot(3);
        if (MiscUtils.isANotApprovedOrMisunderstoodPersonFromMoreDoor(player)) return true;
        return stack != null
                && EnchantmentHelper.getEnchantmentLevel(RegisteredEnchantments.revealer.effectId, stack) > 0;
    }

    @SideOnly(Side.CLIENT)
    public static void preNodeRender() {
        EventHandlerRedirect.addGoggles(Minecraft.getMinecraft().renderViewEntity);
    }

    @SideOnly(Side.CLIENT)
    public static void postNodeRender() {
        EventHandlerRedirect.removeGoggles(Minecraft.getMinecraft().renderViewEntity);
    }
}
