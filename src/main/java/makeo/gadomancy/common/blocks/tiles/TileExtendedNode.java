package makeo.gadomancy.common.blocks.tiles;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.EnumDifficulty;

import cpw.mods.fml.common.network.NetworkRegistry;
import makeo.gadomancy.common.Gadomancy;
import makeo.gadomancy.common.network.PacketHandler;
import makeo.gadomancy.common.network.packets.PacketTCNodeBolt;
import makeo.gadomancy.common.node.ExtendedNodeType;
import makeo.gadomancy.common.node.GrowingNodeBehavior;
import makeo.gadomancy.common.utils.ExplosionHelper;
import makeo.gadomancy.common.utils.ResearchHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.common.entities.EntityAspectOrb;
import thaumcraft.common.entities.monster.EntityWisp;
import thaumcraft.common.items.ItemCrystalEssence;
import thaumcraft.common.items.ItemManaBean;
import thaumcraft.common.items.ItemWispEssence;
import thaumcraft.common.tiles.TileNode;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by HellFirePvP @ 21.10.2015 23:39
 */
public class TileExtendedNode extends TileNode {

    public int ticksExisted;
    private ExtendedNodeType extendedNodeType;
    private GrowingNodeBehavior behavior;

    private boolean doingVortexExplosion;
    private ExplosionHelper.VortexExplosion vortexExplosion;

    public TileExtendedNode() {
        this.behavior = new GrowingNodeBehavior(this);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();

        this.ticksExisted++;

        if (this.doingVortexExplosion) {
            if (this.vortexExplosion == null) {
                this.vortexExplosion = new ExplosionHelper.VortexExplosion(this);
            }
            if (this.vortexExplosion.isFinished()) {
                this.vortexExplosion = null;
                this.doingVortexExplosion = false;
            } else {
                this.vortexExplosion.update();
            }
        }

        boolean needUpdate;

        needUpdate = this.handleGrowingNodeFirst(false);
        needUpdate = this.handleGrowingNodeSecond(needUpdate);

        if (!this.worldObj.isRemote && needUpdate) {
            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
            this.markDirty();
        }
    }

    private boolean handleGrowingNodeSecond(boolean needUpdate) {
        if (this.extendedNodeType == null || this.extendedNodeType != ExtendedNodeType.GROWING) return needUpdate;

        if (this.worldObj.difficultySetting == EnumDifficulty.PEACEFUL) return needUpdate;
        if (this.worldObj.isRemote) return needUpdate;
        if (this.ticksExisted % 8 != 0) return needUpdate;

        List livingEntities = this.worldObj.getEntitiesWithinAABB(
                EntityLivingBase.class,
                AxisAlignedBB.getBoundingBox(
                        this.xCoord,
                        this.yCoord,
                        this.zCoord,
                        this.xCoord + 1,
                        this.yCoord + 1,
                        this.zCoord + 1).expand(6.0D, 6.0D, 6.0D));
        if ((livingEntities != null) && (livingEntities.size() > 0)) {
            for (Object e : livingEntities) {
                EntityLivingBase livingEntity = (EntityLivingBase) e;
                if ((livingEntity.isEntityAlive()) && (!livingEntity.isEntityInvulnerable())) {
                    if (livingEntity instanceof EntityPlayer
                            && ((EntityPlayer) livingEntity).capabilities.isCreativeMode)
                        continue;
                    if (!this.behavior.mayZapNow()) continue;

                    ResearchHelper.distributeResearch(
                            Gadomancy.MODID.toUpperCase() + ".GROWING_AGGRESSION",
                            this.worldObj,
                            this.xCoord,
                            this.yCoord,
                            this.zCoord,
                            6);

                    livingEntity.attackEntityFrom(DamageSource.magic, this.behavior.getZapDamage());

                    this.worldObj.playSoundEffect(
                            this.xCoord + 0.5,
                            this.yCoord + 0.5,
                            this.zCoord + 0.5,
                            "thaumcraft:zap",
                            0.8F,
                            1.0F);

                    PacketTCNodeBolt packet = new PacketTCNodeBolt(
                            this.xCoord + 0.5F,
                            this.yCoord + 0.5F,
                            this.zCoord + 0.5F,
                            (float) livingEntity.posX,
                            (float) (livingEntity.posY + livingEntity.height),
                            (float) livingEntity.posZ,
                            0,
                            false);
                    PacketHandler.INSTANCE.sendToAllAround(
                            packet,
                            new NetworkRegistry.TargetPoint(
                                    this.worldObj.provider.dimensionId,
                                    this.xCoord,
                                    this.yCoord,
                                    this.zCoord,
                                    32.0D));
                }
            }
        }

        return needUpdate;
    }

    private boolean handleGrowingNodeFirst(boolean needUpdate) {
        if (this.extendedNodeType == null || this.extendedNodeType != ExtendedNodeType.GROWING) return needUpdate;

        needUpdate = this.doVortex(EntityAspectOrb.class, needUpdate, new GrowingNodeVortexRunnable<EntityAspectOrb>() {

            @Override
            public Aspect getAspect(EntityAspectOrb entity) {
                return entity.getAspect();
            }

            @Override
            public GrowingNodeBehavior.AspectType getAspectType(EntityAspectOrb entity) {
                return GrowingNodeBehavior.AspectType.ASPECT_ORB;
            }
        });
        needUpdate = this.doVortex(EntityItem.class, needUpdate, new GrowingNodeVortexRunnable<EntityItem>() {

            @Override
            public boolean doesVortex(EntityItem entity) {
                return entity.getEntityItem().getItem() instanceof ItemWispEssence
                        || entity.getEntityItem().getItem() instanceof ItemManaBean
                        || entity.getEntityItem().getItem() instanceof ItemCrystalEssence;
            }

            @Override
            public Aspect getAspect(EntityItem entity) {
                IEssentiaContainerItem container = (IEssentiaContainerItem) entity.getEntityItem().getItem();
                return container.getAspects(entity.getEntityItem()).getAspects().length > 0
                        ? container.getAspects(entity.getEntityItem()).getAspects()[0]
                        : null;
            }

            @Override
            public GrowingNodeBehavior.AspectType getAspectType(EntityItem entity) {
                return entity.getEntityItem().getItem() instanceof ItemWispEssence
                        ? GrowingNodeBehavior.AspectType.WISP_ESSENCE
                        : entity.getEntityItem().getItem() instanceof ItemCrystalEssence
                                ? GrowingNodeBehavior.AspectType.CRYSTAL_ESSENCE
                                : GrowingNodeBehavior.AspectType.MANA_BEAN;
            }
        });
        needUpdate = this.doVortex(EntityWisp.class, needUpdate, new GrowingNodeVortexRunnable<EntityWisp>() {

            @Override
            public Aspect getAspect(EntityWisp entity) {
                return Aspect.getAspect(entity.getType());
            }

            @Override
            public GrowingNodeBehavior.AspectType getAspectType(EntityWisp entity) {
                return GrowingNodeBehavior.AspectType.WISP;
            }
        });
        if (this.worldObj.isRemote) return needUpdate;

        if (this.behavior.lookingForNode()) {
            int xx = this.worldObj.rand.nextInt(8) - this.worldObj.rand.nextInt(8);
            int yy = this.worldObj.rand.nextInt(8) - this.worldObj.rand.nextInt(8);
            int zz = this.worldObj.rand.nextInt(8) - this.worldObj.rand.nextInt(8);
            TileEntity t = this.worldObj.getTileEntity(this.xCoord + xx, this.yCoord + yy, this.zCoord + zz);
            if (t != null && t instanceof TileNode && !(xx == 0 && yy == 0 && zz == 0)) {
                TileNode node = (TileNode) t;
                int thisSize = this.getAspectsBase().visSize();
                int othersSize = node.getAspectsBase().visSize();
                if (thisSize >= othersSize) {
                    this.behavior.lockOnTo(node);
                    this.worldObj.markBlockForUpdate(this.xCoord + xx, this.yCoord + yy, this.zCoord + zz);
                    node.markDirty();
                    needUpdate = true;
                }
            }
        } else {
            needUpdate = this.behavior.updateBehavior(needUpdate);
        }
        return needUpdate;
    }

    private void applyMovementVectors(Entity entity) {
        double var3 = (this.xCoord + 0.5D - entity.posX) / 15.0D;
        double var5 = (this.yCoord + 0.5D - entity.posY) / 15.0D;
        double var7 = (this.zCoord + 0.5D - entity.posZ) / 15.0D;
        double var9 = Math.sqrt(var3 * var3 + var5 * var5 + var7 * var7);
        double var11 = 1.0D - var9;
        if (var11 > 0.0D) {
            var11 *= var11;
            entity.motionX += var3 / var9 * var11 * 0.15D;
            entity.motionY += var5 / var9 * var11 * 0.25D;
            entity.motionZ += var7 / var9 * var11 * 0.15D;
        }
    }

    private <E extends Entity> boolean doVortex(Class<E> entityClass, boolean needUpdate,
            GrowingNodeVortexRunnable<E> runnable) {
        List entities = this.worldObj.getEntitiesWithinAABB(
                entityClass,
                AxisAlignedBB.getBoundingBox(
                        this.xCoord,
                        this.yCoord,
                        this.zCoord,
                        this.xCoord + 1,
                        this.yCoord + 1,
                        this.zCoord + 1).expand(15.0D, 15.0D, 15.0D));
        if ((entities != null) && (entities.size() > 0)) {
            for (Object eObj : entities) {
                E entity = (E) eObj;
                if (!runnable.doesVortex(entity)) continue;
                if ((entity.isEntityAlive()) && (!entity.isEntityInvulnerable())) {
                    double d = this.getDistanceTo(entity.posX, entity.posY, entity.posZ);
                    if (d < 0.5D) { // prev: 2.0D
                        Aspect a = runnable.getAspect(entity);
                        if (a != null) {
                            if (this.getAspects().getAmount(a) < this.getNodeVisBase(a)) {
                                this.addToContainer(a, 1);
                            } else {
                                // Adding it permanently
                                if (!this.worldObj.isRemote && this.behavior.doesAccept(a)) {
                                    this.behavior.addAspect(runnable.getAspectType(entity), a, 1);
                                }
                            }
                        }
                        needUpdate = true;
                        entity.setDead();
                    }
                }
                this.applyMovementVectors(entity);
            }
        }
        return needUpdate;
    }

    public ExtendedNodeType getExtendedNodeType() {
        return this.extendedNodeType;
    }

    public void setExtendedNodeType(ExtendedNodeType extendedNodeType) {
        this.extendedNodeType = extendedNodeType;
    }

    public NBTTagCompound getBehaviorSnapshot() {
        if (this.extendedNodeType != null && this.extendedNodeType == ExtendedNodeType.GROWING) {
            NBTTagCompound behaviorCompound = new NBTTagCompound();
            this.behavior.writeToNBT(behaviorCompound);
            return behaviorCompound;
        }
        return null;
    }

    public void readBehaviorSnapshot(NBTTagCompound tagCompound) {
        if (tagCompound != null && this.extendedNodeType != null && this.extendedNodeType == ExtendedNodeType.GROWING) {
            this.behavior.readFromNBT(tagCompound);
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbttagcompound) {
        super.readCustomNBT(nbttagcompound);

        NBTTagCompound compound = nbttagcompound.getCompoundTag("Gadomancy");

        String exName = compound.getString("exNodeType");
        if (exName != null && !exName.isEmpty()) {
            this.extendedNodeType = ExtendedNodeType.valueOf(exName);
        } else {
            this.extendedNodeType = null;
        }

        if (this.extendedNodeType != null && this.extendedNodeType == ExtendedNodeType.GROWING) {
            NBTTagCompound growingNodeBehavior = compound.getCompoundTag("NodeBehavior");
            this.behavior.readFromNBT(growingNodeBehavior);
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbttagcompound) {
        super.writeCustomNBT(nbttagcompound);

        NBTTagCompound compound = new NBTTagCompound();

        if (this.extendedNodeType != null) {
            compound.setString("exNodeType", this.extendedNodeType.name());

            if (this.extendedNodeType == ExtendedNodeType.GROWING) {
                NBTTagCompound behaviorCompound = new NBTTagCompound();
                this.behavior.writeToNBT(behaviorCompound);
                compound.setTag("NodeBehavior", behaviorCompound);
            }
        }

        nbttagcompound.setTag("Gadomancy", compound);
    }

    public void triggerVortexExplosion() {
        this.doingVortexExplosion = true;
    }

    private abstract static class GrowingNodeVortexRunnable<E> {

        public boolean doesVortex(E entity) {
            return true;
        }

        public abstract Aspect getAspect(E entity);

        public abstract GrowingNodeBehavior.AspectType getAspectType(E entity);
    }
}
