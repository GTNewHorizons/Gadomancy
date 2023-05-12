package makeo.gadomancy.common.entities.golems.nbt;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

import makeo.gadomancy.api.GadomancyApi;
import makeo.gadomancy.api.golems.AdditionalGolemType;
import makeo.gadomancy.api.golems.cores.AdditionalGolemCore;
import makeo.gadomancy.common.Gadomancy;
import makeo.gadomancy.common.data.config.ModConfig;
import makeo.gadomancy.common.entities.golems.cores.EntityAITasksWrapper;
import makeo.gadomancy.common.utils.NBTHelper;
import thaumcraft.common.entities.golems.EntityGolemBase;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by makeo @ 28.07.2015 17:25
 */
public class ExtendedGolemProperties implements IExtendedEntityProperties {

    private static final String TYPE_TAG = "golemTypeOverride";
    private static final String FORGE_TAG = "ForgeData";

    private EntityGolemBase golem;

    private boolean updateHealth;
    private float health;

    private boolean isSitting;

    private EntityAITasksWrapper taskWrapper;
    private EntityAITasksWrapper targetWrapper;

    public ExtendedGolemProperties(EntityGolemBase golem) {
        this.golem = golem;
    }

    public boolean shouldUpdateHealth() {
        return this.updateHealth;
    }

    public void resetUpdateHealth() {
        this.updateHealth = false;
    }

    public float getHealth() {
        return this.health;
    }

    public boolean isSitting() {
        return this.isSitting;
    }

    public void setSitting(boolean isSitting) {
        this.isSitting = isSitting;
    }

    @Override
    public void saveNBTData(NBTTagCompound compound) {
        AdditionalGolemType type = GadomancyApi.getAdditionalGolemType(this.golem.getGolemType());
        if (type != null) {
            NBTTagCompound entityData;
            if (compound.hasKey(ExtendedGolemProperties.FORGE_TAG)) {
                entityData = compound.getCompoundTag(ExtendedGolemProperties.FORGE_TAG);
            } else {
                entityData = new NBTTagCompound();
                compound.setTag(ExtendedGolemProperties.FORGE_TAG, entityData);
            }

            ExtendedGolemProperties.writeAdvancedGolemType(entityData, type);
            this.golem.ridingEntity = new OverrideRidingEntity(this.golem, compound, this.golem.ridingEntity);
        }

        compound.setBoolean(Gadomancy.MODID + ":sitting", this.isSitting);
    }

    private static void writeAdvancedGolemType(NBTTagCompound base, AdditionalGolemType type) {
        NBTTagCompound compound;
        if (base.hasKey(Gadomancy.MODID)) {
            compound = base.getCompoundTag(Gadomancy.MODID);
        } else {
            compound = new NBTTagCompound();
            base.setTag(Gadomancy.MODID, compound);
        }
        compound.setString(ExtendedGolemProperties.TYPE_TAG, type.getEnumEntry().name());
    }

    @Override
    public void loadNBTData(NBTTagCompound compound) {
        if (compound.hasKey("GolemType")) {
            AdditionalGolemType type = ExtendedGolemProperties.readAdvancedGolemType(this.golem.getEntityData());
            if (type != null) {
                byte lastType = compound.getByte("GolemType");
                compound.setTag("GolemType", new OverrideNBTTagByte(lastType, (byte) type.getEnumEntry().ordinal()));

                this.health = compound.getFloat("HealF") * -1;
                this.updateHealth = true;
            }
        }

        this.isSitting = compound.getBoolean(Gadomancy.MODID + ":sitting");

        this.updateGolemCore();
    }

    public void updateGolemCore() {
        if (!this.golem.worldObj.isRemote) {
            if (NBTHelper.hasPersistentData(this.golem)) {
                NBTTagCompound persistent = NBTHelper.getPersistentData(this.golem);
                if (persistent.hasKey("Core")) {
                    for (AdditionalGolemCore core : GadomancyApi.getAdditionalGolemCores()) {
                        if (core.getName().equals(persistent.getString("Core"))) {
                            this.golem.getDataWatcher().updateObject(ModConfig.golemDatawatcherId, core.getName());
                            break;
                        }
                    }
                }
            }
        }
    }

    public void setWrapperIfNeeded() {
        boolean needsUpdate = false;
        if (this.taskWrapper == null) {
            this.taskWrapper = new EntityAITasksWrapper(this.golem, this.golem.tasks, true);
            this.golem.tasks = this.taskWrapper;

            needsUpdate = true;
        }

        if (this.targetWrapper == null) {
            this.targetWrapper = new EntityAITasksWrapper(this.golem, this.golem.targetTasks, false);
            this.golem.targetTasks = this.targetWrapper;

            needsUpdate = true;
        }

        if (needsUpdate) {
            this.updateGolem();
        }
    }

    public void updateGolem() {
        this.taskWrapper.unlock();
        this.targetWrapper.unlock();

        this.golem.setupGolem();
        this.golem.setupGolemInventory();

        AdditionalGolemCore core = GadomancyApi.getAdditionalGolemCore(this.golem);
        if (core != null) {
            this.taskWrapper.taskEntries.clear();
            this.targetWrapper.taskEntries.clear();

            core.setupGolem(this.golem);

            this.taskWrapper.lock();
            this.targetWrapper.lock();
        }
    }

    private static AdditionalGolemType readAdvancedGolemType(NBTTagCompound base) {
        if (base.hasKey(Gadomancy.MODID)) {
            NBTTagCompound compound = base.getCompoundTag(Gadomancy.MODID);
            if (compound.hasKey(ExtendedGolemProperties.TYPE_TAG)) {
                return GadomancyApi.getAdditionalGolemType(compound.getString(ExtendedGolemProperties.TYPE_TAG));
            }
        }
        return null;
    }

    @Override
    public void init(Entity entity, World world) {}
}
