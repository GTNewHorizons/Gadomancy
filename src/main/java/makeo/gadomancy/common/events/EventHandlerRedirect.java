package makeo.gadomancy.common.events;

import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import makeo.gadomancy.common.registration.RegisteredEnchantments;
import makeo.gadomancy.common.registration.RegisteredPotions;
import makeo.gadomancy.common.utils.MiscUtils;
import thaumcraft.api.aspects.Aspect;
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
    public static void preNodeRender(TileEntity tile) {
        EventHandlerRedirect.addGoggles(Minecraft.getMinecraft().renderViewEntity);
    }

    @SideOnly(Side.CLIENT)
    public static void postNodeRender(TileEntity tile) {
        EventHandlerRedirect.removeGoggles(Minecraft.getMinecraft().renderViewEntity);
    }

    public static void preBlockHighlight(DrawBlockHighlightEvent event) {
        EventHandlerRedirect.addGoggles(event.player);
    }

    public static void postBlockHighlight(DrawBlockHighlightEvent event) {
        EventHandlerRedirect.removeGoggles(event.player);
    }

    public static int getAdditionalVisDiscount(EntityPlayer player, Aspect aspect, int currentTotalDiscount) {
        if (player.isPotionActive(RegisteredPotions.VIS_DISCOUNT)) {
            currentTotalDiscount += (player.getActivePotionEffect(RegisteredPotions.VIS_DISCOUNT).getAmplifier() + 1)
                    * 8;
        }
        return currentTotalDiscount;
    }

    public static int getFortuneLevel(EntityLivingBase entity) {
        int fortuneLevel = EventHandlerRedirect
                .getRealEnchantmentLevel(Enchantment.fortune.effectId, entity.getHeldItem());
        if (entity.isPotionActive(RegisteredPotions.POTION_LUCK)) {
            int lvl = entity.getActivePotionEffect(RegisteredPotions.POTION_LUCK).getAmplifier() + 1; // Amplifier
                                                                                                      // 0-indexed
            fortuneLevel += lvl;
        }
        return fortuneLevel;
    }

    public static int getLootingLevel(EntityLivingBase entity) {
        int lootingLevel = EventHandlerRedirect
                .getRealEnchantmentLevel(Enchantment.looting.effectId, entity.getHeldItem());
        if (entity.isPotionActive(RegisteredPotions.POTION_LUCK)) {
            int lvl = entity.getActivePotionEffect(RegisteredPotions.POTION_LUCK).getAmplifier() + 1; // Amplifier
                                                                                                      // 0-indexed
            lootingLevel += lvl;
        }
        return lootingLevel;
    }

    public static int onGetEnchantmentLevel(int enchantmentId, ItemStack stack) {
        EntityPlayer possiblePlayer = null;
        if (stack != null) {
            MinecraftServer server = MinecraftServer.getServer();
            if (server != null && server.getConfigurationManager() != null) {
                for (EntityPlayer player : server.getConfigurationManager().playerEntityList) {
                    if (player != null && player.getHeldItem() == stack) {
                        possiblePlayer = player;
                        break;
                    }
                }
            }
        }

        if (possiblePlayer != null) {
            if (enchantmentId == Enchantment.fortune.effectId) {
                return EventHandlerRedirect.getFortuneLevel(possiblePlayer);
            } else if (enchantmentId == Enchantment.looting.effectId) {
                return EventHandlerRedirect.getLootingLevel(possiblePlayer);
            }
        }

        return EventHandlerRedirect.getRealEnchantmentLevel(enchantmentId, stack);
    }

    private static int getRealEnchantmentLevel(int enchantmentId, ItemStack stack) {
        if (stack == null) {
            return 0;
        } else {
            NBTTagList nbttaglist = stack.getEnchantmentTagList();
            if (nbttaglist == null) {
                return 0;
            } else {
                for (int j = 0; j < nbttaglist.tagCount(); ++j) {
                    short id = nbttaglist.getCompoundTagAt(j).getShort("id");
                    if (id == enchantmentId) {
                        return nbttaglist.getCompoundTagAt(j).getShort("lvl");
                    }
                }
                return 0;
            }
        }
    }
}
