package makeo.gadomancy.common.familiar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;

import baubles.api.BaublesApi;
import cpw.mods.fml.common.network.NetworkRegistry;
import makeo.gadomancy.common.items.baubles.ItemEtherealFamiliar;
import makeo.gadomancy.common.network.PacketHandler;
import makeo.gadomancy.common.network.packets.PacketFamiliarBolt;
import makeo.gadomancy.common.utils.MiscUtils;
import makeo.gadomancy.common.utils.Vector3;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.Config;
import thaumcraft.common.items.baubles.ItemAmuletVis;
import thaumcraft.common.items.wands.ItemWandCasting;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by HellFirePvP @ 29.12.2015 18:12
 */
public class FamiliarController {

    public static final double RANGE_DEF = 6;
    public static final int AS_DELAY_DEF = 32;
    public static final double DMG_DEF = 1;

    public static final double RANGE_INC = 1.8;
    public static final int AS_INC = 6;
    public static final double DMG_INC = 1.1;

    public static final double SHOCK_DMG_INC = 0.8;
    public static final double FIRE_DMG_INC = 0.5;
    public static final double POISON_RANGE_INC = 0.6;
    public static final double WEAKNESS_DMG_DEC = 0.3;

    private static final Random RAND = new Random();
    private int tickLastEffect;
    private final EntityPlayer owningPlayer;

    public FamiliarController(EntityPlayer owner) {
        this.owningPlayer = owner;
    }

    public void tick() {
        IInventory baubles = BaublesApi.getBaubles(this.owningPlayer);
        ItemStack stack = baubles.getStackInSlot(0);
        if (stack == null || !(stack.getItem() instanceof ItemEtherealFamiliar)) return;

        if (this.tickLastEffect > 0) {
            this.tickLastEffect--;
            return;
        }

        FamiliarAugment.FamiliarAugmentList augments = ItemEtherealFamiliar.getAugments(stack);
        if (augments == null) return;

        if (augments.isEmpty()) {
            this.doDefaultAttack();
            this.tickLastEffect = FamiliarController.AS_DELAY_DEF;
        } else {
            FamiliarAugment.FamiliarAugmentPair effectElement = null;
            for (FamiliarAugment.FamiliarAugmentPair pair : augments) {
                FamiliarAugment augment = pair.augment;
                if (augment.equals(FamiliarAugment.SHOCK) || augment.equals(FamiliarAugment.FIRE)
                        || augment.equals(FamiliarAugment.POISON)
                        || augment.equals(FamiliarAugment.WEAKNESS)) {
                    effectElement = pair;
                    break;
                }
            }

            AspectList costs = new AspectList();

            int dmgLevel = augments.getLevel(FamiliarAugment.DAMAGE_INCREASE);
            if (dmgLevel > 0) {
                for (int i = 0; i < dmgLevel; i++) {
                    costs.add(FamiliarAugment.DAMAGE_INCREASE.getCostsPerLevel());
                }
            }

            int attackSpeedLevel = augments.getLevel(FamiliarAugment.ATTACK_SPEED);
            if (attackSpeedLevel > 0) {
                for (int i = 0; i < attackSpeedLevel; i++) {
                    costs.add(FamiliarAugment.ATTACK_SPEED.getCostsPerLevel());
                }
            }

            int rangeLevel = augments.getLevel(FamiliarAugment.RANGE_INCREASE);
            if (rangeLevel > 0) {
                for (int i = 0; i < rangeLevel; i++) {
                    costs.add(FamiliarAugment.RANGE_INCREASE.getCostsPerLevel());
                }
            }

            double damage = FamiliarController.DMG_DEF + (dmgLevel != -1 ? FamiliarController.DMG_INC * dmgLevel : 0);
            int ticksUntilNextAttack = FamiliarController.AS_DELAY_DEF
                    - (attackSpeedLevel != -1 ? FamiliarController.AS_INC * attackSpeedLevel : 0);
            double range = FamiliarController.RANGE_DEF
                    + (rangeLevel != -1 ? FamiliarController.RANGE_INC * rangeLevel : 0);

            if (effectElement != null) {
                for (int i = 0; i < effectElement.level; i++) {
                    costs.add(effectElement.augment.getCostsPerLevel());
                }
            }

            if (!this.owningPlayer.capabilities.isCreativeMode
                    && !FamiliarController.consumeVisFromInventory(this.owningPlayer, costs, false)) {
                return;
            }

            if (effectElement == null) {
                if (this.doEnhancedDefaultAttack(damage, range)) {
                    this.tickLastEffect = ticksUntilNextAttack;
                    if (!this.owningPlayer.capabilities.isCreativeMode && FamiliarController.RAND.nextInt(3) == 0) {
                        FamiliarController.consumeVisFromInventory(this.owningPlayer, costs, true);
                    }
                }
            } else {
                if (this.doAttack(effectElement, damage, range)) {
                    this.tickLastEffect = ticksUntilNextAttack;
                    if (!this.owningPlayer.capabilities.isCreativeMode && FamiliarController.RAND.nextInt(3) == 0) {
                        FamiliarController.consumeVisFromInventory(this.owningPlayer, costs, true);
                    }
                }
            }
        }
    }

    private boolean doAttack(FamiliarAugment.FamiliarAugmentPair element, double damage, double range) {
        int toSelect = 1;
        int effectLevel = element.level;
        FamiliarAugment augment = element.augment;
        int boltType = 6;
        if (augment.equals(FamiliarAugment.SHOCK)) {
            boltType = 1;
            damage += effectLevel * FamiliarController.SHOCK_DMG_INC;
        } else if (augment.equals(FamiliarAugment.FIRE)) {
            boltType = 4;
            damage += effectLevel * FamiliarController.FIRE_DMG_INC;
        } else if (augment.equals(FamiliarAugment.POISON)) {
            boltType = 3;
            range += effectLevel * FamiliarController.POISON_RANGE_INC;
        } else if (augment.equals(FamiliarAugment.WEAKNESS)) {
            boltType = 5;
            damage -= effectLevel * FamiliarController.WEAKNESS_DMG_DEC;
            if (effectLevel == 3) toSelect = 3;
        }

        List<EntityLivingBase> toAttack = this.selectEntityToAttack(range, toSelect);
        if (toAttack == null || toAttack.isEmpty()) return false;
        for (EntityLivingBase entity : toAttack) {
            this.attack(entity, damage, boltType);
        }
        if (augment.equals(FamiliarAugment.SHOCK)) {
            for (EntityLivingBase entity : toAttack) {
                if (FamiliarController.RAND.nextBoolean()) {
                    Vector3 vel = MiscUtils.getPositionVector(entity)
                            .subtract(MiscUtils.getPositionVector(this.owningPlayer)).normalize().divide(2)
                            .multiply(0.8 * effectLevel);
                    if (vel.getY() < 0) vel.setY(-vel.getY());
                    entity.motionX += vel.getX();
                    entity.motionY += vel.getY();
                    entity.motionZ += vel.getZ();
                }
            }
        } else if (augment.equals(FamiliarAugment.FIRE)) {
            int fireDur = 14;
            for (int i = 0; i < effectLevel; i++) {
                fireDur *= 2;
            }
            for (EntityLivingBase entity : toAttack) {
                entity.setFire(fireDur);
                if (effectLevel == 3) {
                    entity.addPotionEffect(
                            new PotionEffect(Config.potionSunScornedID, MiscUtils.ticksForMinutes(1), 2));
                }
            }
        } else if (augment.equals(FamiliarAugment.POISON)) {
            int potionLvl = 0;
            if (effectLevel > 1) potionLvl += 1;
            for (EntityLivingBase entity : toAttack) {
                entity.addPotionEffect(
                        new PotionEffect(Potion.poison.getId(), MiscUtils.ticksForSeconds(30), potionLvl));
                if (effectLevel == 3) {
                    entity.addPotionEffect(new PotionEffect(Potion.hunger.getId(), MiscUtils.ticksForMinutes(2), 3));
                }
            }
        } else if (augment.equals(FamiliarAugment.WEAKNESS)) {
            int potionLvl = 0;
            if (effectLevel == 3) potionLvl++;
            int dur = MiscUtils.ticksForSeconds(30);
            for (EntityLivingBase entity : toAttack) {
                entity.addPotionEffect(new PotionEffect(Potion.weakness.getId(), dur, potionLvl));
                entity.addPotionEffect(new PotionEffect(Potion.moveSlowdown.getId(), dur, potionLvl));
            }
        }
        return true;
    }

    public static boolean consumeVisFromInventory(EntityPlayer player, AspectList cost, boolean doit) {
        IInventory baubles = BaublesApi.getBaubles(player);
        for (int a = 0; a < 4; a++) {
            if ((baubles.getStackInSlot(a) != null)
                    && ((baubles.getStackInSlot(a).getItem() instanceof ItemAmuletVis))) {
                boolean done = ((ItemAmuletVis) baubles.getStackInSlot(a).getItem())
                        .consumeAllVis(baubles.getStackInSlot(a), player, cost, doit, true);
                if (done) {
                    return true;
                }
            }
        }
        for (int a = player.inventory.mainInventory.length - 1; a >= 0; a--) {
            ItemStack item = player.inventory.mainInventory[a];
            if ((item != null) && ((item.getItem() instanceof ItemWandCasting))) {
                boolean done = ((ItemWandCasting) item.getItem()).consumeAllVis(item, player, cost, doit, true);
                if (done) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean doEnhancedDefaultAttack(double damage, double range) {
        List<EntityLivingBase> toAttack = this.selectEntityToAttack(range, 1);
        if (toAttack == null || toAttack.isEmpty()) return false;

        this.attack(toAttack.get(0), damage, 6);
        return true;
    }

    private boolean doDefaultAttack() {
        List<EntityLivingBase> toAttack = this.selectEntityToAttack(FamiliarController.RANGE_DEF, 1);
        if (toAttack == null || toAttack.isEmpty()) return false;

        this.attack(toAttack.get(0), FamiliarController.DMG_DEF, 6);
        return true;
    }

    private void attack(EntityLivingBase toAttack, double damage, int boltType) {
        toAttack.attackEntityFrom(DamageSource.magic, (float) damage);
        toAttack.worldObj.playSoundEffect(
                toAttack.posX + 0.5,
                toAttack.posY + 0.5,
                toAttack.posZ + 0.5,
                "thaumcraft:zap",
                0.8F,
                1.0F);
        PacketFamiliarBolt bolt = new PacketFamiliarBolt(
                this.owningPlayer.getCommandSenderName(),
                (float) toAttack.posX,
                (float) toAttack.posY,
                (float) toAttack.posZ,
                boltType,
                true);
        PacketHandler.INSTANCE.sendToAllAround(
                bolt,
                new NetworkRegistry.TargetPoint(
                        toAttack.worldObj.provider.dimensionId,
                        this.owningPlayer.posX,
                        this.owningPlayer.posY,
                        this.owningPlayer.posZ,
                        16));
    }

    private List<EntityLivingBase> selectEntityToAttack(double rad, int amountToSelectMax) {
        List<EntityLivingBase> attackable = this.getAttackableEntities(rad);
        if (attackable.size() <= 0) return null;
        List<EntityLivingBase> entities = new ArrayList<EntityLivingBase>();
        if (attackable.size() <= amountToSelectMax) {
            entities.addAll(attackable);
        } else {
            for (int i = 0; i < amountToSelectMax; i++) {
                entities.add(attackable.remove(FamiliarController.RAND.nextInt(attackable.size())));
            }
        }
        return entities;
    }

    private List<EntityLivingBase> getAttackableEntities(double rad) {
        double x = this.owningPlayer.posX;
        double y = this.owningPlayer.posY;
        double z = this.owningPlayer.posZ;
        List<EntityLivingBase> entities = this.owningPlayer.worldObj.getEntitiesWithinAABB(
                EntityLivingBase.class,
                AxisAlignedBB.getBoundingBox(x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5)
                        .expand(rad, rad, rad));

        Iterator<EntityLivingBase> it = entities.iterator();
        while (it.hasNext()) {
            EntityLivingBase base = it.next();
            if (base == null || base.isDead || base instanceof EntityPlayer || !(base instanceof IMob)) it.remove();
            // TODO remove entities we don't want to attack...
        }

        return entities;
    }
}
