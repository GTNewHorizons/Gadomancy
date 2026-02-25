package makeo.gadomancy.common.events;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import makeo.gadomancy.api.GadomancyApi;
import makeo.gadomancy.api.golems.cores.AdditionalGolemCore;
import makeo.gadomancy.common.entities.golems.ItemAdditionalGolemPlacer;
import makeo.gadomancy.common.registration.RegisteredGolemStuff;
import makeo.gadomancy.common.utils.NBTHelper;
import thaumcraft.common.entities.golems.ItemGolemPlacer;

public class EventHandlerTooltips {

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void on1(ItemTooltipEvent e) {
        if (!e.toolTip.isEmpty() && e.itemStack.hasTagCompound()) {
            if (e.itemStack.stackTagCompound.getBoolean("isStickyJar")) {
                e.toolTip.add(1, EnumChatFormatting.GREEN + StatCollector.translateToLocal("gadomancy.lore.stickyjar"));
            }
        }

        if (!e.toolTip.isEmpty() && NBTHelper.hasPersistentData(e.itemStack)) {
            NBTTagCompound compound = NBTHelper.getPersistentData(e.itemStack);
            if (compound.hasKey("disguise")) {
                NBTBase base = compound.getTag("disguise");
                String lore = null;
                if (base instanceof NBTTagCompound) {
                    ItemStack stack = ItemStack.loadItemStackFromNBT((NBTTagCompound) base);
                    if (stack != null) {
                        lore = String.format(
                                StatCollector.translateToLocal("gadomancy.lore.disguise.item"),
                                EnumChatFormatting.getTextWithoutFormattingCodes(stack.getDisplayName()));
                    }
                } else {
                    lore = StatCollector.translateToLocal("gadomancy.lore.disguise.none");
                }
                if (lore != null) {
                    e.toolTip.add(EnumChatFormatting.GREEN + lore);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void on2(ItemTooltipEvent event) {
        if (event.itemStack != null) {
            if (event.itemStack.getItem() instanceof ItemGolemPlacer
                    || event.itemStack.getItem() instanceof ItemAdditionalGolemPlacer) {
                if (RegisteredGolemStuff.upgradeRunicShield.hasUpgrade(event.itemStack)) {
                    event.toolTip.add(
                            "\u00a76" + StatCollector.translateToLocal("item.runic.charge")
                                    + " +"
                                    + RegisteredGolemStuff.upgradeRunicShield.getChargeLimit(event.itemStack));
                }

                AdditionalGolemCore core = GadomancyApi.getAdditionalGolemCore(event.itemStack);
                if (core != null) {
                    String searchStr = StatCollector.translateToLocal("item.ItemGolemCore.name");
                    for (int i = 0; i < event.toolTip.size(); i++) {
                        String line = event.toolTip.get(i);
                        if (line.contains(searchStr)) {
                            int index = line.indexOf('\u00a7', searchStr.length()) + 2;
                            event.toolTip.remove(i);
                            event.toolTip.add(
                                    i,
                                    line.substring(0, index)
                                            + StatCollector.translateToLocal(core.getUnlocalizedName()));
                            break;
                        }
                    }
                }
            }
        }
    }
}
