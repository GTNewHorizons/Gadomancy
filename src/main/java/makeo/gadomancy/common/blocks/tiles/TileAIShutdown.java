package makeo.gadomancy.common.blocks.tiles;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import makeo.gadomancy.client.effect.fx.Orbital;
import makeo.gadomancy.common.registration.AIShutdownWhitelist;
import makeo.gadomancy.common.utils.Injector;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by HellFirePvP @ 08.06.2016 22:56
 */
public class TileAIShutdown extends SynchronizedTileEntity implements IAspectContainer, IEssentiaTransport {

    private static final Random RAND = new Random();

    private static Map<ChunkCoordinates, List<AffectedEntity>> trackedEntities = Maps.newHashMap();
    private static AxisAlignedBB BOX = AxisAlignedBB.getBoundingBox(-3, -1, -3, 4, 2, 4);
    private static Injector injEntityLivingBase = new Injector(EntityLivingBase.class);

    public static final int MAX_AMT = 16;

    public Orbital orbital;

    private int ticksExisted;
    private int storedAmount;

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void updateEntity() {
        if (this.worldObj == null) return;
        this.ticksExisted++;

        if (!this.worldObj.isRemote) {
            ChunkCoordinates cc = this.getCoords();
            if (!TileAIShutdown.trackedEntities.containsKey(cc))
                TileAIShutdown.trackedEntities.put(cc, Lists.newLinkedList());

            if ((this.ticksExisted & 15) == 0) {
                this.killAI();
            }
            if (((this.ticksExisted & 7) == 0)) {
                this.handleIO();
            }
            if (((this.ticksExisted & 31) == 0)) {
                this.drainDefaultEssentia();
            }
        }
    }

    private void drainDefaultEssentia() {
        this.storedAmount = Math.max(0, this.storedAmount - 1);
        this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
        this.markDirty();
    }

    private void handleIO() {
        if (this.storedAmount < TileAIShutdown.MAX_AMT) {
            TileEntity te = ThaumcraftApiHelper
                    .getConnectableTile(this.worldObj, this.xCoord, this.yCoord, this.zCoord, ForgeDirection.UP);
            if (te != null) {
                IEssentiaTransport ic = (IEssentiaTransport) te;
                if (!ic.canOutputTo(ForgeDirection.DOWN)) {
                    return;
                }

                if (ic.getSuctionAmount(ForgeDirection.DOWN) < this.getSuctionAmount(ForgeDirection.UP)) {
                    this.addToContainer(Aspect.ENTROPY, ic.takeEssentia(Aspect.ENTROPY, 1, ForgeDirection.DOWN));
                }
            }
        }
    }

    private void killAI() {
        ChunkCoordinates cc = this.getCoords();
        List objEntityList = this.worldObj.getEntitiesWithinAABB(
                EntityLivingBase.class,
                TileAIShutdown.BOX.copy().offset(this.xCoord, this.yCoord, this.zCoord));
        for (Object o : objEntityList) {
            if (o != null && o instanceof EntityLiving
                    && !((EntityLiving) o).isDead
                    && this.canAffect((EntityLiving) o)) {
                EntityLiving el = (EntityLiving) o;
                if (this.storedAmount <= 0) return;
                AffectedEntity affected = this.removeAI(el);
                TileAIShutdown.trackedEntities.get(cc).add(affected);
            }
        }
    }

    private AffectedEntity removeAI(EntityLiving el) {
        if (TileAIShutdown.RAND.nextInt(4) == 0) {
            this.storedAmount = Math.max(0, this.storedAmount - 1);
            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
            this.markDirty();
        }

        UUID uu = el.getUniqueID();
        List<EntityAITasks.EntityAITaskEntry> tasks = new ArrayList<EntityAITasks.EntityAITaskEntry>(
                el.tasks.taskEntries);
        List<EntityAITasks.EntityAITaskEntry> targetTasks = new ArrayList<EntityAITasks.EntityAITaskEntry>(
                el.targetTasks.taskEntries);

        List<Class<? extends EntityAIBase>> entries = AIShutdownWhitelist.getWhitelistedAIClasses(el);

        Iterator iterator = el.tasks.taskEntries.iterator();
        while (iterator.hasNext()) {
            Object entry = iterator.next();
            if (entry == null || !(entry instanceof EntityAITasks.EntityAITaskEntry)) continue;
            boolean needsRemoval = true;
            for (Class<? extends EntityAIBase> aiClass : entries) {
                if (aiClass.isAssignableFrom(((EntityAITasks.EntityAITaskEntry) entry).action.getClass()))
                    needsRemoval = false;
            }
            if (needsRemoval) {
                iterator.remove();
            }
        }

        iterator = el.targetTasks.taskEntries.iterator();
        while (iterator.hasNext()) {
            Object entry = iterator.next();
            if (entry == null || !(entry instanceof EntityAITasks.EntityAITaskEntry)) continue;
            boolean needsRemoval = true;
            for (Class<? extends EntityAIBase> aiClass : entries) {
                if (aiClass.isAssignableFrom(((EntityAITasks.EntityAITaskEntry) entry).action.getClass()))
                    needsRemoval = false;
            }
            if (needsRemoval) {
                iterator.remove();
            }
        }
        TileAIShutdown.injEntityLivingBase.setObject(el);
        TileAIShutdown.injEntityLivingBase.setField("ignoreCollisions", true);
        TileAIShutdown.injEntityLivingBase.setObject(null);
        return new AffectedEntity(uu, tasks, targetTasks);
    }

    public int getStoredEssentia() {
        return this.storedAmount;
    }

    public boolean canAffect(EntityLiving el) {
        ChunkCoordinates cc = this.getCoords();
        if (!TileAIShutdown.trackedEntities.containsKey(cc)) return false;
        UUID uu = el.getUniqueID();
        for (AffectedEntity ae : TileAIShutdown.trackedEntities.get(cc)) {
            if (ae.eUUID.equals(uu)) return false;
        }
        return true;
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        this.storedAmount = compound.getInteger("amount");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        compound.setInteger("amount", this.storedAmount);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // IAspectContainer & IEssentiaTransport
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public AspectList getAspects() {
        if (this.storedAmount <= 0) return new AspectList();
        return new AspectList().add(Aspect.ENTROPY, this.storedAmount);
    }

    @Override
    public void setAspects(AspectList list) {}

    @Override
    public boolean doesContainerAccept(Aspect aspect) {
        return aspect == Aspect.ENTROPY;
    }

    @Override
    public int addToContainer(Aspect aspect, int amount) {
        if (amount == 0) {
            return amount;
        }

        if (aspect == null) return 0;

        if (this.storedAmount < TileAIShutdown.MAX_AMT) {
            int added = Math.min(amount, TileAIShutdown.MAX_AMT - this.storedAmount);
            this.storedAmount += added;
            amount -= added;
            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
            this.markDirty();
        }
        return amount;
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int i) {
        return false;
    }

    @Override
    @Deprecated
    public boolean takeFromContainer(AspectList list) {
        return false; // NO-OP
    }

    @Override
    public boolean doesContainerContainAmount(Aspect aspect, int i) {
        return false;
    }

    @Override
    @Deprecated
    public boolean doesContainerContain(AspectList list) {
        return false; // NO-OP
    }

    @Override
    public int containerContains(Aspect aspect) {
        return 0;
    }

    @Override
    public boolean isConnectable(ForgeDirection direction) {
        return ForgeDirection.UP.equals(direction);
    }

    @Override
    public boolean canInputFrom(ForgeDirection direction) {
        return this.isConnectable(direction);
    }

    @Override
    public boolean canOutputTo(ForgeDirection direction) {
        return this.isConnectable(direction);
    }

    @Override
    public void setSuction(Aspect aspect, int i) {}

    @Override
    public Aspect getSuctionType(ForgeDirection direction) {
        if (!this.isConnectable(direction)) return null;
        return Aspect.ENTROPY;
    }

    @Override
    public int getSuctionAmount(ForgeDirection direction) {
        if (!this.isConnectable(direction)) return 0;
        return this.getMinimumSuction();
    }

    @Override
    public int takeEssentia(Aspect aspect, int i, ForgeDirection direction) {
        return 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int i, ForgeDirection direction) {
        return this.canInputFrom(direction) ? this.addToContainer(aspect, i) : 0;
    }

    @Override
    public Aspect getEssentiaType(ForgeDirection direction) {
        return Aspect.ENTROPY;
    }

    @Override
    public int getEssentiaAmount(ForgeDirection direction) {
        if (!this.isConnectable(direction)) return 0;
        return this.storedAmount;
    }

    @Override
    public int getMinimumSuction() {
        return 64;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }

    // Tracker methods

    public static void removeTrackedEntity(EntityLiving entityLiving) {
        for (ChunkCoordinates cc : TileAIShutdown.trackedEntities.keySet()) {
            for (AffectedEntity ae : TileAIShutdown.trackedEntities.get(cc)) {
                if (ae.eUUID.equals(entityLiving.getUniqueID())) {
                    TileAIShutdown.trackedEntities.get(cc).remove(ae);
                    entityLiving.tasks.taskEntries = ae.tasks;
                    entityLiving.targetTasks.taskEntries = ae.targetTasks;
                    TileAIShutdown.injEntityLivingBase.setObject(entityLiving);
                    TileAIShutdown.injEntityLivingBase.setField("ignoreCollisions", false);
                    TileAIShutdown.injEntityLivingBase.setObject(null);
                    return;
                }
            }
        }
    }

    public static void removeTrackedEntities(World world, int x, int y, int z) {
        ChunkCoordinates cc = new ChunkCoordinates(x, y, z);
        if (TileAIShutdown.trackedEntities.containsKey(cc)) {
            for (AffectedEntity ae : TileAIShutdown.trackedEntities.get(cc)) {
                for (Object objE : world.getLoadedEntityList()) {
                    if (objE != null && objE instanceof EntityLiving
                            && !((EntityLiving) objE).isDead
                            && ((EntityLiving) objE).getUniqueID().equals(ae.eUUID)) {
                        ((EntityLiving) objE).tasks.taskEntries = ae.tasks;
                        ((EntityLiving) objE).targetTasks.taskEntries = ae.targetTasks;
                        TileAIShutdown.injEntityLivingBase.setObject(objE);
                        TileAIShutdown.injEntityLivingBase.setField("ignoreCollisions", false);
                        TileAIShutdown.injEntityLivingBase.setObject(null);
                    }
                }
            }
            TileAIShutdown.trackedEntities.remove(new ChunkCoordinates(x, y, z));
        }
    }

    public static class AffectedEntity {

        public final UUID eUUID;
        public List<EntityAITasks.EntityAITaskEntry> tasks;
        public List<EntityAITasks.EntityAITaskEntry> targetTasks;

        public AffectedEntity(UUID eUUID, List<EntityAITasks.EntityAITaskEntry> tasks,
                List<EntityAITasks.EntityAITaskEntry> targetTasks) {
            this.eUUID = eUUID;
            this.tasks = tasks;
            this.targetTasks = targetTasks;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || this.getClass() != o.getClass()) return false;
            AffectedEntity that = (AffectedEntity) o;
            return this.eUUID != null ? this.eUUID.equals(that.eUUID) : that.eUUID == null;
        }

        @Override
        public int hashCode() {
            return this.eUUID != null ? this.eUUID.hashCode() : 0;
        }
    }
}
