package makeo.gadomancy.common.events;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import makeo.gadomancy.api.GadomancyApi;
import makeo.gadomancy.api.golems.cores.AdditionalGolemCore;
import makeo.gadomancy.api.golems.events.GolemDropPlacerEvent;
import makeo.gadomancy.common.Gadomancy;
import makeo.gadomancy.common.entities.golems.ItemAdditionalGolemPlacer;
import makeo.gadomancy.common.utils.NBTHelper;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.ItemGolemPlacer;

public class EventHandlerGolemServer {

    private final Map<EntityGolemBase, EntityPlayer> markedGolems = new HashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void on(PlaySoundAtEntityEvent event) {
        if (!event.entity.worldObj.isRemote && event.entity instanceof EntityGolemBase golem
                && event.name.equals("thaumcraft:zap")
                && event.volume == 0.5F
                && event.pitch == 1.0F) {
            if (this.markedGolems.containsKey(golem)) {
                EntityPlayer player = this.markedGolems.get(golem);
                this.markedGolems.remove(golem);

                AdditionalGolemCore core = GadomancyApi.getAdditionalGolemCore(golem);

                boolean movedPlacer = false;
                boolean movedCore = core == null || !player.isSneaking();

                for (EntityItem entityItem : golem.capturedDrops) {
                    ItemStack item = entityItem.getEntityItem();

                    if (!movedCore && item.getItem() == ConfigItems.itemGolemCore) {
                        entityItem.setEntityItemStack(core.getItem());
                    }

                    if (!movedPlacer && item.getItem() instanceof ItemGolemPlacer
                            || item.getItem() instanceof ItemAdditionalGolemPlacer) {
                        // move persistent data to item
                        NBTTagCompound persistent = (NBTTagCompound) NBTHelper.getPersistentData(golem).copy();
                        if (player.isSneaking()) {
                            persistent.removeTag("Core");
                        }
                        NBTHelper.getData(item).setTag(Gadomancy.MODID, persistent);
                        event.entity.setDead();
                        entityItem.setEntityItemStack(item);

                        MinecraftForge.EVENT_BUS.post(new GolemDropPlacerEvent(player, entityItem, golem));

                        movedPlacer = true;
                    }
                    event.entity.worldObj.spawnEntityInWorld(entityItem);
                }
                golem.capturedDrops.clear();
                golem.captureDrops = false;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void on(AttackEntityEvent event) {
        ItemStack heldItem = event.entityPlayer.getHeldItem();
        if (heldItem != null && heldItem.getItem() == ConfigItems.itemGolemBell
                && event.target instanceof EntityGolemBase
                && !event.target.worldObj.isRemote
                && !event.target.isDead) {
            event.target.captureDrops = true;
            this.markedGolems.put((EntityGolemBase) event.target, event.entityPlayer);
        }
    }
}
