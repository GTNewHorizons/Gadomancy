package makeo.gadomancy.common.events;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.Explosion;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import makeo.gadomancy.common.Gadomancy;
import makeo.gadomancy.common.blocks.tiles.TileAIShutdown;
import makeo.gadomancy.common.blocks.tiles.TileBlockProtector;
import makeo.gadomancy.common.blocks.tiles.TileNodeManipulator;
import makeo.gadomancy.common.blocks.tiles.TileStickyJar;
import makeo.gadomancy.common.data.SyncDataHolder;
import makeo.gadomancy.common.data.config.ModConfig;
import makeo.gadomancy.common.entities.EntityItemElement;
import makeo.gadomancy.common.items.ItemElement;
import makeo.gadomancy.common.registration.RegisteredBlocks;
import makeo.gadomancy.common.registration.RegisteredItems;
import makeo.gadomancy.common.utils.GolemEnumHelper;
import makeo.gadomancy.common.utils.ItemUtils;
import makeo.gadomancy.common.utils.MiscUtils;
import makeo.gadomancy.common.utils.NBTHelper;
import makeo.gadomancy.common.utils.WandHandler;
import makeo.gadomancy.common.utils.world.TCMazeHandler;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.tiles.TileJarFillable;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p/>
 * Created by makeo @ 05.07.2015 13:20
 */
public class EventHandlerWorld {

    public Map<EntityItem, Long> trackedItems = new HashMap<EntityItem, Long>();

    @SubscribeEvent(priority = EventPriority.LOW)
    public void on(EntityJoinWorldEvent event) {
        if (!event.world.isRemote && event.entity instanceof EntityItem) {
            ItemStack stack = ((EntityItem) event.entity).getEntityItem();
            if (this.isDisguised(stack)) {
                long time = event.world.getTotalWorldTime() + event.world.rand.nextInt(60) + 40;
                this.trackedItems.put((EntityItem) event.entity, time);
            }
            if (stack.getItem() instanceof ItemElement && !(event.entity instanceof EntityItemElement)) {
                event.setCanceled(true);
                EntityItem newItem = new EntityItemElement(
                        event.world,
                        event.entity.posX,
                        event.entity.posY,
                        event.entity.posZ,
                        ((EntityItem) event.entity).getEntityItem());
                newItem.delayBeforeCanPickup = ((EntityItem) event.entity).delayBeforeCanPickup;
                newItem.motionX = event.entity.motionX;
                newItem.motionY = event.entity.motionY;
                newItem.motionZ = event.entity.motionZ;
                event.world.spawnEntityInWorld(newItem);
            }
        }
    }

    @SubscribeEvent
    public void on(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.START && !event.world.isRemote
                && event.world.getTotalWorldTime() % 10 == 0) {

            Iterator<Map.Entry<EntityItem, Long>> iterator = this.trackedItems.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<EntityItem, Long> entry = iterator.next();
                EntityItem entity = entry.getKey();

                if (event.world == entity.worldObj) {
                    if (entity.isDead || !this.isDisguised(entity.getEntityItem())) {
                        iterator.remove();
                        continue;
                    }

                    int x = MathHelper.floor_double(entity.posX);
                    int y = MathHelper.floor_double(entity.posY);
                    int z = MathHelper.floor_double(entity.posZ);

                    if (entity.delayBeforeCanPickup <= 0
                            && entity.worldObj.getTotalWorldTime() - entry.getValue() > 0) {
                        if (ConfigBlocks.blockFluidPure == event.world.getBlock(x, y, z)
                                && event.world.getBlockMetadata(x, y, z) == 0) {
                            NBTTagCompound compound = NBTHelper.getPersistentData(entity.getEntityItem());
                            NBTBase base = compound.getTag("disguise");
                            if (base instanceof NBTTagCompound) {
                                ItemStack stack = ItemStack.loadItemStackFromNBT((NBTTagCompound) base);
                                EntityItem newEntity = new EntityItem(
                                        event.world,
                                        entity.posX,
                                        entity.posY,
                                        entity.posZ,
                                        stack);
                                ItemUtils.applyRandomDropOffset(newEntity, event.world.rand);
                                event.world.spawnEntityInWorld(newEntity);
                            }
                            compound.removeTag("disguise");
                            if (compound.hasNoTags()) {
                                NBTHelper.removePersistentData(entity.getEntityItem());
                                if (entity.getEntityItem().getTagCompound().hasNoTags()) {
                                    entity.getEntityItem().setTagCompound(null);
                                }
                            }
                            event.world.setBlockToAir(x, y, z);
                        }
                    } else {
                        Gadomancy.proxy.spawnBubbles(
                                event.world,
                                (float) entity.posX,
                                (float) entity.posY,
                                (float) entity.posZ,
                                0.2f);
                    }
                }
            }
        }
    }

    private boolean isDisguised(ItemStack stack) {
        return NBTHelper.hasPersistentData(stack) && NBTHelper.getPersistentData(stack).hasKey("disguise");
    }

    private int serverTick;
    private Entity lastUpdated;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void on(LivingEvent.LivingUpdateEvent e) {
        if (!e.entityLiving.worldObj.isRemote) {
            this.lastUpdated = e.entityLiving;
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void on(WorldEvent.Load e) {
        if (!e.world.isRemote && e.world.provider.dimensionId == 0) {
            Gadomancy.loadModData();

            GolemEnumHelper.validateSavedMapping();
            GolemEnumHelper.reorderEnum();

            TCMazeHandler.init();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void on(ExplosionEvent.Start e) {
        Explosion expl = e.explosion;
        if (expl.isSmoking && (expl.exploder != null ? TileBlockProtector.isSpotProtected(e.world, expl.exploder)
                : TileBlockProtector.isSpotProtected(e.world, expl.explosionX, expl.explosionY, expl.explosionZ))) {
            // why?
            // expl.isSmoking = false;
            e.setCanceled(true);
            e.world.newExplosion(
                    expl.exploder,
                    expl.explosionX,
                    expl.explosionY,
                    expl.explosionZ,
                    expl.explosionSize,
                    expl.isFlaming,
                    false);
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void on(WorldEvent.Unload e) {
        if (!e.world.isRemote && e.world.provider.dimensionId == 0) {
            Gadomancy.unloadModData();

            TCMazeHandler.closeAllSessionsAndCleanup();
        }
    }

    @SubscribeEvent
    public void on(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        TCMazeHandler.scheduleTick();
        SyncDataHolder.doNecessaryUpdates();
        this.serverTick++;
        if ((this.serverTick & 15) == 0) {
            EventHandlerEntity.registeredLuxPylons.clear();
        }
    }

    private Map<EntityPlayer, Integer> interacts;

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void on(BlockEvent.PlaceEvent e) {
        if (e.isCanceled()) {
            if (this.interacts != null) this.interacts.remove(e.player);
        } else {
            if (!e.world.isRemote && this.isStickyJar(e.itemInHand)) {
                TileEntity parent = e.world.getTileEntity(e.x, e.y, e.z);
                if (parent instanceof TileJarFillable) {
                    int metadata = e.world.getBlockMetadata(e.x, e.y, e.z);
                    e.world.setBlock(e.x, e.y, e.z, RegisteredBlocks.blockStickyJar, metadata, 2);

                    TileEntity tile = e.world.getTileEntity(e.x, e.y, e.z);
                    if (tile instanceof TileStickyJar) {
                        Integer sideHit = this.interacts.get(e.player);
                        ((TileStickyJar) tile).init(
                                (TileJarFillable) parent,
                                e.placedBlock,
                                metadata,
                                ForgeDirection.getOrientation(sideHit == null ? 1 : sideHit).getOpposite());
                        RegisteredBlocks.blockStickyJar.onBlockPlacedBy(e.world, e.x, e.y, e.z, e.player, e.itemInHand);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent event) {
        if (!event.world.isRemote) {
            if (event.block == RegisteredBlocks.blockNodeManipulator) {
                TileEntity te = event.world.getTileEntity(event.x, event.y, event.z);
                if (te != null && te instanceof TileNodeManipulator) {
                    if (((TileNodeManipulator) te).isInMultiblock()) ((TileNodeManipulator) te).breakMultiblock();
                }
            }
            if (event.block == RegisteredBlocks.blockStoneMachine && event.blockMetadata == 5) {
                TileAIShutdown.removeTrackedEntities(event.world, event.x, event.y, event.z);
            }
            if (event.world.provider.dimensionId == ModConfig.dimOuterId) {
                if (event.block == ConfigBlocks.blockEldritchNothing) {
                    if (event.getPlayer().capabilities.isCreativeMode
                            && MiscUtils.isANotApprovedOrMisunderstoodPersonFromMoreDoor(event.getPlayer()))
                        return;
                    event.setCanceled(true);
                    event.getPlayer().addChatMessage(
                            new ChatComponentText(
                                    EnumChatFormatting.ITALIC + ""
                                            + EnumChatFormatting.GRAY
                                            + StatCollector
                                                    .translateToLocal("gadomancy.eldritch.nobreakPortalNothing")));
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void on(PlayerInteractEvent e) {
        if (!e.world.isRemote && e.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK
                && this.isStickyJar(e.entityPlayer.getHeldItem())) {
            if (this.interacts == null) {
                this.interacts = new HashMap<EntityPlayer, Integer>();
            }
            this.interacts.put(e.entityPlayer, e.face);
        }

        if (e.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            ItemStack i = e.entityPlayer.getHeldItem();
            if (i != null && (i.getItem() instanceof ItemWandCasting)) {
                WandHandler.handleWandInteract(e.world, e.x, e.y, e.z, e.entityPlayer, i);
            }
        }
    }

    private boolean isStickyJar(ItemStack stack) {
        return stack != null && RegisteredItems.isStickyableJar(stack)
                && stack.hasTagCompound()
                && stack.stackTagCompound.getBoolean("isStickyJar");
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void on(ItemTooltipEvent e) {
        if (e.toolTip.size() > 0 && e.itemStack.hasTagCompound()) {
            if (e.itemStack.stackTagCompound.getBoolean("isStickyJar")) {
                e.toolTip.add(1, "\u00a7a" + StatCollector.translateToLocal("gadomancy.lore.stickyjar"));
            }
        }

        if (e.toolTip.size() > 0 && NBTHelper.hasPersistentData(e.itemStack)) {
            NBTTagCompound compound = NBTHelper.getPersistentData(e.itemStack);
            if (compound.hasKey("disguise")) {
                NBTBase base = compound.getTag("disguise");
                String lore;
                if (base instanceof NBTTagCompound) {
                    ItemStack stack = ItemStack.loadItemStackFromNBT((NBTTagCompound) base);
                    lore = String.format(
                            StatCollector.translateToLocal("gadomancy.lore.disguise.item"),
                            EnumChatFormatting.getTextWithoutFormattingCodes(stack.getDisplayName()));
                } else {
                    lore = StatCollector.translateToLocal("gadomancy.lore.disguise.none");
                }
                e.toolTip.add("\u00a7a" + lore);
            }
        }
    }
}
