package makeo.gadomancy.common.entities.golems.upgrades;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;

import cpw.mods.fml.common.network.NetworkRegistry;
import makeo.gadomancy.common.registration.RegisteredGolemStuff;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.EnumGolemType;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXShield;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by makeo @ 16.06.2015 12:50
 */
public class GolemUpgradeRunicShield extends GolemUpgrade {

    private static final String LAST_DISCHARGE_TAG = "lastRunicDischarge";

    @Override
    public String getName() {
        return "runicShield";
    }

    public float absorb(EntityGolemBase golem, float amount, DamageSource source) {
        float charge = this.getCharge(golem);

        if (Math.floor(charge) > 0) {
            this.sendShieldEffect(golem, source);

            float rest = (float) (Math.floor(charge) - amount);
            float chargeRest = charge - (float) Math.floor(charge);

            if (rest <= 0) {
                this.setCharge(golem, chargeRest);
                return rest * -1;
            } else {
                this.setCharge(golem, (float) Math.floor(rest) + chargeRest);
                return 0;
            }
        }
        return amount;
    }

    private void sendShieldEffect(EntityGolemBase golem, DamageSource source) {
        int target = -1;
        if (source.getEntity() != null) {
            target = source.getEntity().getEntityId();
        } else if (source == DamageSource.fall) {
            target = -2;
        } else if (source == DamageSource.fallingBlock) {
            target = -3;
        }

        PacketHandler.INSTANCE.sendToAllAround(
                new PacketFXShield(golem.getEntityId(), target),
                new NetworkRegistry.TargetPoint(
                        golem.worldObj.provider.dimensionId,
                        golem.posX,
                        golem.posY,
                        golem.posZ,
                        64.0D));
    }

    public float getCharge(EntityGolemBase golem) {
        int elapsed = golem.ticksExisted - this.getLastDischarge(golem);
        float charge = elapsed * this.getRechargeSpeed(golem);
        float limit = this.getChargeLimit(golem);
        return charge > limit ? limit : charge;
    }

    private void setCharge(EntityGolemBase golem, float charge) {
        int ticksNeeded = Math.round(charge / this.getRechargeSpeed(golem));
        this.setLastDischarge(golem, golem.ticksExisted - ticksNeeded);
    }

    private void setLastDischarge(EntityGolemBase golem, Integer ticks) {
        golem.getEntityData().setInteger(GolemUpgradeRunicShield.LAST_DISCHARGE_TAG, ticks);
    }

    private int getLastDischarge(EntityGolemBase golem) {
        NBTTagCompound compound = golem.getEntityData();
        if (!compound.hasKey(GolemUpgradeRunicShield.LAST_DISCHARGE_TAG)) {
            compound.setInteger(GolemUpgradeRunicShield.LAST_DISCHARGE_TAG, golem.ticksExisted);
            return golem.ticksExisted;
        }
        return compound.getInteger(GolemUpgradeRunicShield.LAST_DISCHARGE_TAG);
    }

    private float getRechargeSpeed(EntityGolemBase golem) {
        return 0.02f;
    }

    public int getChargeLimit(EntityGolemBase golem) {
        return this.getChargeLimit(golem.getGolemType(), golem.getMaxHealth());
    }

    public int getChargeLimit(ItemStack stack) {
        EnumGolemType type = EnumGolemType.getType(stack.getItemDamage());
        float maxHealth = type.health;
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("deco")
                && stack.getTagCompound().getString("deco").contains("H")) {
            maxHealth += 5;
        }
        return this.getChargeLimit(type, maxHealth);
    }

    private int getChargeLimit(EnumGolemType type, float maxHealth) {
        if (type == RegisteredGolemStuff.typeSilverwood.getEnumEntry()) {
            return (int) maxHealth;
        }
        return (int) (maxHealth / 2.6);
    }
}
