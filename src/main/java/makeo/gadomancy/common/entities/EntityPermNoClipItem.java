package makeo.gadomancy.common.entities;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import makeo.gadomancy.common.data.config.ModConfig;
import thaumcraft.common.entities.EntityPermanentItem;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by HellFirePvP @ 16.11.2015 19:59
 */
public class EntityPermNoClipItem extends EntityPermanentItem {

    private float fixPosX = -1, fixPosY = -1, fixPosZ = -1; // Can never actually get those values since pylon coords
                                                            // are never .0
    private int masterX, masterY, masterZ; // Coords of its master tile.

    public EntityPermNoClipItem(World world) {
        super(world);
    }

    public EntityPermNoClipItem(World world, float x, float y, float z, ItemStack stack, int masterX, int masterY,
            int masterZ) {
        super(world, x, y, z, stack);
        this.fixPosX = x;
        this.fixPosY = y;
        this.fixPosZ = z;
        this.masterX = masterX;
        this.masterY = masterY;
        this.masterZ = masterZ;

        ChunkCoordinates masterCoords = new ChunkCoordinates(this.masterX, this.masterY, this.masterZ);
        this.getDataWatcher().updateObject(ModConfig.entityNoClipItemDatawatcherMasterId, masterCoords);
        int fixX = Float.floatToIntBits(x);
        int fixY = Float.floatToIntBits(y);
        int fixZ = Float.floatToIntBits(z);
        ChunkCoordinates fixCoords = new ChunkCoordinates(fixX, fixY, fixZ);
        this.getDataWatcher().updateObject(ModConfig.entityNoClipItemDatawatcherFixedId, fixCoords);
    }

    @Override
    protected void entityInit() {
        super.entityInit();

        this.getDataWatcher().addObjectByDataType(ModConfig.entityNoClipItemDatawatcherMasterId, 6);
        this.getDataWatcher().addObjectByDataType(ModConfig.entityNoClipItemDatawatcherFixedId, 6);

        this.getDataWatcher()
                .updateObject(ModConfig.entityNoClipItemDatawatcherMasterId, new ChunkCoordinates(0, 0, 0));
        this.getDataWatcher().updateObject(ModConfig.entityNoClipItemDatawatcherFixedId, new ChunkCoordinates(0, 0, 0));
    }

    @Override
    public void onUpdate() {
        this.noClip = true;
        super.onUpdate();
        this.motionX = 0;
        this.motionY = 0;
        this.motionZ = 0;
        if (this.getDataWatcher().getWatchedObject(ModConfig.entityNoClipItemDatawatcherFixedId).getObject() == null)
            return;

        if ((this.ticksExisted & 1) == 0) {
            ChunkCoordinates cc = (ChunkCoordinates) this.getDataWatcher()
                    .getWatchedObject(ModConfig.entityNoClipItemDatawatcherMasterId).getObject();
            if (cc == null) return;
            TileEntity te = this.worldObj.getTileEntity(cc.posX, cc.posY, cc.posZ);
            if (te instanceof IItemMasterTile) {
                ((IItemMasterTile) te).informMaster();
                ItemChangeTask task = ((IItemMasterTile) te).getAndRemoveScheduledChangeTask();
                if (task != null) {
                    task.changeItem(this);
                }
                ((IItemMasterTile) te).broadcastItemStack(this.getEntityItem());
            }
        }

        ChunkCoordinates fixC = (ChunkCoordinates) this.getDataWatcher()
                .getWatchedObject(ModConfig.entityNoClipItemDatawatcherFixedId).getObject();
        float fX = Float.intBitsToFloat(fixC.posX);
        float fY = Float.intBitsToFloat(fixC.posY);
        float fZ = Float.intBitsToFloat(fixC.posZ);
        this.setPositionAndRotation(fX, fY, fZ, 0, 0);

        if ((this.ticksExisted & 7) == 0 && !this.worldObj.isRemote) {
            ChunkCoordinates masterCoords = (ChunkCoordinates) this.getDataWatcher()
                    .getWatchedObject(ModConfig.entityNoClipItemDatawatcherMasterId).getObject();
            TileEntity te = this.worldObj.getTileEntity(masterCoords.posX, masterCoords.posY, masterCoords.posZ);
            if (!(te instanceof IItemMasterTile) || !((IItemMasterTile) te).canStillHoldItem()) {
                EntityItem item = new EntityItem(this.worldObj, this.posX, this.posY, this.posZ, this.getEntityItem());
                this.worldObj.spawnEntityInWorld(item);
                this.setDead();
            }
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource ds, float p_70097_2_) {
        if (ds.equals(DamageSource.inFire) || ds.equals(DamageSource.onFire)) return false;
        return super.attackEntityFrom(ds, p_70097_2_);
    }

    @Override
    public void setPositionAndRotation2(double p_70056_1_, double p_70056_3_, double p_70056_5_, float p_70056_7_,
            float p_70056_8_, int p_70056_9_) {
        this.setPosition(p_70056_1_, p_70056_3_, p_70056_5_);
        this.setRotation(p_70056_7_, p_70056_8_);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound com) {
        super.readEntityFromNBT(com);

        this.fixPosX = com.getFloat("fX");
        this.fixPosY = com.getFloat("fY");
        this.fixPosZ = com.getFloat("fZ");

        this.masterX = com.getInteger("mX");
        this.masterY = com.getInteger("mY");
        this.masterZ = com.getInteger("mZ");

        this.getDataWatcher().updateObject(
                ModConfig.entityNoClipItemDatawatcherMasterId,
                new ChunkCoordinates(this.masterX, this.masterY, this.masterZ));
        ChunkCoordinates cc = new ChunkCoordinates(
                Float.floatToIntBits(this.fixPosX),
                Float.floatToIntBits(this.fixPosY),
                Float.floatToIntBits(this.fixPosZ));
        this.getDataWatcher().updateObject(ModConfig.entityNoClipItemDatawatcherFixedId, cc);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound com) {
        super.writeEntityToNBT(com);

        com.setFloat("fX", this.fixPosX);
        com.setFloat("fY", this.fixPosY);
        com.setFloat("fZ", this.fixPosZ);

        com.setInteger("mX", this.masterX);
        com.setInteger("mY", this.masterY);
        com.setInteger("mZ", this.masterZ);
    }

    public interface IItemMasterTile {

        boolean canStillHoldItem();

        void informMaster();

        void informItemRemoval();

        ItemChangeTask getAndRemoveScheduledChangeTask();

        void broadcastItemStack(ItemStack itemStack);
    }

    public abstract static class ItemChangeTask {

        public abstract void changeItem(EntityPermNoClipItem item);
    }
}
