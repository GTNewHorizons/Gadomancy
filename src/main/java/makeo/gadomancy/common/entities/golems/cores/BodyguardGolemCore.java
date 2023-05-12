package makeo.gadomancy.common.entities.golems.cores;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathNavigate;

import makeo.gadomancy.api.golems.cores.AdditionalGolemCore;
import makeo.gadomancy.common.Gadomancy;
import makeo.gadomancy.common.entities.ai.AIFollowOwner;
import makeo.gadomancy.common.entities.ai.AISit;
import makeo.gadomancy.common.entities.ai.AIUncheckedAttackOnCollide;
import makeo.gadomancy.common.entities.ai.AIUncheckedDartAttack;
import makeo.gadomancy.common.entities.golems.nbt.ExtendedGolemProperties;
import makeo.gadomancy.common.registration.RegisteredItems;
import thaumcraft.common.entities.ai.misc.AIOpenDoor;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.Marker;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p/>
 * Created by makeo @ 31.10.2015 15:35
 */
public class BodyguardGolemCore extends AdditionalGolemCore {

    @Override
    public void setupGolem(EntityGolemBase golem) {
        WrappedGolem wrapped = new WrappedGolem(golem);

        golem.setHomeArea(0, 300000000, 0, 300000000);
        golem.setMarkers(new ArrayList<Marker>());

        golem.targetTasks.addTask(1, new EntityAIOwnerHurtByTarget(wrapped));
        golem.targetTasks.addTask(2, new EntityAIOwnerHurtTarget(wrapped));
        golem.targetTasks.addTask(3, new EntityAIHurtByTarget(golem, true));

        golem.tasks.addTask(1, new AISit(golem));

        if (golem.decoration.contains("R")) {
            golem.tasks.addTask(2, new AIUncheckedDartAttack(golem));
        }
        golem.tasks.addTask(3, new AIUncheckedAttackOnCollide(golem));
        golem.tasks.addTask(4, new AIFollowOwner(wrapped, 5, 2));

        golem.tasks.addTask(5, new AIOpenDoor(golem, true));
        golem.tasks.addTask(7, new EntityAIWatchClosest(golem, EntityPlayer.class, 6.0F));
        golem.tasks.addTask(8, new EntityAILookIdle(golem));
    }

    @Override
    public boolean hasMarkers() {
        return false;
    }

    private static class WrappedGolem extends EntityTameable {

        private EntityGolemBase golem;

        public WrappedGolem(EntityGolemBase golem) {
            super(golem.worldObj);

            this.golem = golem;
        }

        @Override
        public boolean isWithinHomeDistance(int p_110176_1_, int p_110176_2_, int p_110176_3_) {
            return true;
        }

        @Override
        public float getAIMoveSpeed() {
            return this.golem.getAIMoveSpeed();
        }

        @Override
        public Random getRNG() {
            return this.golem.getRNG();
        }

        @Override
        public EntityLivingBase getAttackTarget() {
            return this.golem.getAttackTarget();
        }

        @Override
        public boolean canAttackClass(Class p_70686_1_) {
            return this.golem.canAttackClass(p_70686_1_);
        }

        @Override
        public EntitySenses getEntitySenses() {
            return this.golem.getEntitySenses();
        }

        @Override
        public IAttributeInstance getEntityAttribute(IAttribute p_110148_1_) {
            if (this.golem != null) {
                return this.golem.getEntityAttribute(p_110148_1_);
            }
            return super.getEntityAttribute(p_110148_1_);
        }

        @Override
        public void setAttackTarget(EntityLivingBase p_70624_1_) {
            this.golem.setAttackTarget(p_70624_1_);
        }

        @Override
        public boolean isTamed() {
            return true;
        }

        @Override
        public PathNavigate getNavigator() {
            return this.golem.getNavigator();
        }

        @Override
        public EntityAgeable createChild(EntityAgeable entity) {
            return null;
        }

        @Override
        public EntityLivingBase getOwner() {
            return this.golem.getOwner();
        }

        @Override
        public EntityLookHelper getLookHelper() {
            return this.golem.getLookHelper();
        }

        @Override
        public double getDistanceSqToEntity(Entity entity) {
            return this.golem.getDistanceSqToEntity(entity);
        }

        @Override
        public boolean getLeashed() {
            return this.golem.getLeashed();
        }

        @Override
        public int getVerticalFaceSpeed() {
            return this.golem.getVerticalFaceSpeed();
        }

        @Override
        public void setLocationAndAngles(double x, double y, double z, float rotationYaw, float rotationPitch) {
            rotationYaw = this.golem.rotationYaw;
            rotationPitch = this.golem.rotationPitch;
            this.golem.setLocationAndAngles(x, y, z, rotationYaw, rotationPitch);
            this.worldObj.playSoundEffect(x, y, z, "mob.endermen.portal", 0.5F, 1.0F);
        }
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public boolean openGui(EntityPlayer player, EntityGolemBase golem) {
        ExtendedGolemProperties props = (ExtendedGolemProperties) golem.getExtendedProperties(Gadomancy.MODID);
        props.setSitting(!props.isSitting());

        return true;
    }

    @Override
    public byte getBaseCore() {
        return 4;
    }

    @Override
    public String getUnlocalizedName() {
        return "gadomancy.golem.bodyguardcore";
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(RegisteredItems.itemGolemCoreBreak, 1, 1);
    }
}
