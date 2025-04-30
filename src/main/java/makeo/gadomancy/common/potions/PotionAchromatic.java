package makeo.gadomancy.common.potions;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.ai.attributes.BaseAttributeMap;
import net.minecraft.util.AxisAlignedBB;

import makeo.gadomancy.common.data.DataAchromatic;
import makeo.gadomancy.common.data.SyncDataHolder;
import makeo.gadomancy.common.utils.Vector3;
import thaumcraft.api.aspects.Aspect;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p/>
 * Created by makeo @ 11.12.2015 17:32
 */
public class PotionAchromatic extends PotionCustomTexture {

    public PotionAchromatic(int id) {
        super(id, false, Aspect.CRYSTAL.getColor(), Aspect.CRYSTAL.getImage());
        this.setPotionName("potion.achromatic");
    }

    @Override
    public void applyAttributesModifiersToEntity(EntityLivingBase entity, BaseAttributeMap p_111185_2_,
            int p_111185_3_) {
        super.applyAttributesModifiersToEntity(entity, p_111185_2_, p_111185_3_);

        ((DataAchromatic) SyncDataHolder.getDataServer("AchromaticData")).handleApplication(entity);
    }

    @Override
    public void removeAttributesModifiersFromEntity(EntityLivingBase entity, BaseAttributeMap p_111187_2_,
            int p_111187_3_) {
        super.removeAttributesModifiersFromEntity(entity, p_111187_2_, p_111187_3_);

        ((DataAchromatic) SyncDataHolder.getDataServer("AchromaticData")).handleRemoval(entity);
    }

    @Override
    public boolean isReady(int p_76397_1_, int p_76397_2_) {
        return true;
    }

    @Override
    public void performEffect(EntityLivingBase entity, int strength) {

        List<IProjectile> projectiles = entity.worldObj.getEntitiesWithinAABB(
                IProjectile.class,
                AxisAlignedBB.getBoundingBox(
                        entity.posX - 4.0D,
                        entity.posY - 4.0D,
                        entity.posZ - 4.0D,
                        entity.posX + 3.0D,
                        entity.posY + 3.0D,
                        entity.posZ + 3.0D));
        for (IProjectile p : projectiles) {
            final Entity e = (Entity) p;
            Vector3 motionVec = new Vector3(e.motionX, e.motionY, e.motionZ).normalize().multiply(
                    Math.sqrt(
                            (e.posX - entity.posX) * (e.posX - entity.posX)
                                    + (e.posY - entity.posY) * (e.posY - entity.posY)
                                    + (e.posZ - entity.posZ) * (e.posZ - entity.posZ))
                            * 2.0D);
            e.posX += motionVec.getX();
            e.posY += motionVec.getY();
            e.posZ += motionVec.getZ();
        }
    }
}
