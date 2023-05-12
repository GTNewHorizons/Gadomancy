package makeo.gadomancy.common.data;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;

import makeo.gadomancy.common.registration.RegisteredPotions;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by HellFirePvP @ 18.12.2015 20:21
 */
public class DataAchromatic extends AbstractData {

    private List<Integer> achromaticEntities = new ArrayList<Integer>();

    private List<Integer> addClientQueue = new ArrayList<Integer>();
    private List<Integer> removeClientQueue = new ArrayList<Integer>();

    public boolean isAchromatic(EntityPlayer player) {
        return this.achromaticEntities.contains(player.getEntityId());
    }

    public void handleApplication(EntityLivingBase entity) {
        if (entity.worldObj.isRemote) return;

        int entityId = entity.getEntityId();

        boolean needsUpdate = false;
        if (!this.addClientQueue.contains(entityId) && !this.achromaticEntities.contains(entityId)) {
            this.addClientQueue.add(entityId);
            this.achromaticEntities.add(entityId);
            needsUpdate = true;
        }
        if (this.removeClientQueue.contains(entityId)) {
            this.removeClientQueue.remove(Integer.valueOf(entityId));
            this.addClientQueue.remove(Integer.valueOf(entityId));
            needsUpdate = false;
        }
        if (needsUpdate) {
            this.markDirty();
        }
    }

    public void handleRemoval(EntityLivingBase entity) {
        if (entity.worldObj.isRemote) return;

        int entityId = entity.getEntityId();

        boolean needsUpdate = false;
        if (!this.removeClientQueue.contains(entityId) && this.achromaticEntities.contains(entityId)) {
            this.removeClientQueue.add(entityId);
            this.achromaticEntities.remove(Integer.valueOf(entityId));
            needsUpdate = true;
        }
        if (this.addClientQueue.contains(entityId)) {
            this.addClientQueue.remove(Integer.valueOf(entityId));
            this.removeClientQueue.remove(Integer.valueOf(entityId));
            needsUpdate = false;
        }
        if (needsUpdate) {
            this.markDirty();
        }
    }

    public void checkPotionEffect(EntityPlayerMP p) {
        if (p.isPotionActive(RegisteredPotions.ACHROMATIC)) {
            this.handleApplication(p);
        }
    }

    @Override
    public boolean needsUpdate() {
        return !this.addClientQueue.isEmpty() || !this.removeClientQueue.isEmpty();
    }

    @Override
    public void writeAllDataToPacket(NBTTagCompound compound) {
        int[] array = new int[this.achromaticEntities.size()];
        for (int i = 0; i < this.achromaticEntities.size(); i++) {
            array[i] = this.achromaticEntities.get(i);
        }
        compound.setTag("additions", new NBTTagIntArray(array));
        compound.setTag("removals", new NBTTagIntArray(new int[0]));
    }

    @Override
    public void writeToPacket(NBTTagCompound compound) {
        int[] array = new int[this.removeClientQueue.size()];
        for (int i = 0; i < this.removeClientQueue.size(); i++) {
            array[i] = this.removeClientQueue.get(i);
        }
        compound.setTag("removals", new NBTTagIntArray(array));
        array = new int[this.addClientQueue.size()];
        for (int i = 0; i < this.addClientQueue.size(); i++) {
            array[i] = this.addClientQueue.get(i);
        }
        compound.setTag("additions", new NBTTagIntArray(array));

        this.removeClientQueue.clear();
        this.addClientQueue.clear();
    }

    @Override
    public void readRawFromPacket(NBTTagCompound compound) {
        int[] array = compound.getIntArray("removals");
        if (array != null && array.length > 0) {
            for (int i : array) {
                this.removeClientQueue.add(i);
            }
        }
        array = compound.getIntArray("additions");
        if (array != null && array.length > 0) {
            for (int i : array) {
                this.addClientQueue.add(i);
            }
        }
    }

    @Override
    public void handleIncomingData(AbstractData serverData) {
        DataAchromatic achromatic = (DataAchromatic) serverData;
        List<Integer> toRemove = achromatic.removeClientQueue;
        this.achromaticEntities.removeAll(toRemove);
        List<Integer> toAdd = achromatic.addClientQueue;
        this.achromaticEntities.addAll(toAdd);
    }

    public static class Provider extends AbstractData.ProviderAutoAllocate<DataAchromatic> {

        public Provider(String key) {
            super(key);
        }

        @Override
        public DataAchromatic provideNewInstance() {
            return new DataAchromatic();
        }
    }
}
