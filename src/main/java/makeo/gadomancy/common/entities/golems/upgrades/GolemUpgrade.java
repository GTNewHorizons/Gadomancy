package makeo.gadomancy.common.entities.golems.upgrades;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;

import makeo.gadomancy.common.Gadomancy;
import makeo.gadomancy.common.utils.NBTHelper;
import thaumcraft.common.entities.golems.EntityGolemBase;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by makeo @ 16.06.2015 14:38
 */
public abstract class GolemUpgrade<T extends NBTBase> {

    private static final String UPGRADE_COMPOUND = "upgrades";

    public abstract String getName();

    public String getTagName() {
        return this.getName();
    }

    public String getText() {
        return Gadomancy.MODID.toLowerCase() + ".upgrades." + this.getName().toLowerCase();
    }

    public ItemStack getUpgradeItem() {
        return null;
    }

    protected T createUpgradeData() {
        return null;
    }

    protected T getUpgradeData(ItemStack stack) {
        return this.getUpgradeData(NBTHelper.getPersistentData(stack));
    }

    protected T getUpgradeData(EntityGolemBase golem) {
        return this.getUpgradeData(NBTHelper.getPersistentData(golem));
    }

    protected T getUpgradeData(NBTTagCompound compound) {
        if (compound.hasKey(GolemUpgrade.UPGRADE_COMPOUND)) {
            NBTTagCompound upgrades = compound.getCompoundTag(GolemUpgrade.UPGRADE_COMPOUND);
            try {
                return (T) upgrades.getTag(this.getTagName());
            } catch (ClassCastException ignored) {}
        }
        return null;
    }

    public boolean hasUpgrade(ItemStack stack) {
        return this.hasUpgrade(NBTHelper.getPersistentData(stack));
    }

    public boolean hasUpgrade(EntityGolemBase golem) {
        return this.hasUpgrade(NBTHelper.getPersistentData(golem));
    }

    protected boolean hasUpgrade(NBTTagCompound compound) {
        return this.getUpgradeData(compound) != null;
    }

    public void addUpgrade(ItemStack stack) {
        this.addUpgrade(NBTHelper.getPersistentData(stack));
    }

    public void addUpgrade(EntityGolemBase golem) {
        this.addUpgrade(NBTHelper.getPersistentData(golem));
    }

    protected void addUpgrade(NBTTagCompound compound) {
        NBTTagCompound upgrades = compound.getCompoundTag(GolemUpgrade.UPGRADE_COMPOUND);
        if (!compound.hasKey(GolemUpgrade.UPGRADE_COMPOUND)) {
            compound.setTag(GolemUpgrade.UPGRADE_COMPOUND, upgrades);
        }

        NBTBase data = this.createUpgradeData();
        upgrades.setTag(this.getTagName(), data == null ? new NBTTagByte((byte) 1) : data);
    }

    public void removeUpgrade(ItemStack stack) {
        this.removeUpgrade(NBTHelper.getPersistentData(stack));
    }

    public void removeUpgrade(EntityGolemBase golem) {
        this.removeUpgrade(NBTHelper.getPersistentData(golem));
    }

    protected void removeUpgrade(NBTTagCompound compound) {
        if (compound.hasKey(GolemUpgrade.UPGRADE_COMPOUND)) {
            compound.removeTag(this.getTagName());
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof GolemUpgrade && ((GolemUpgrade) obj).getName().equals(this.getName());
    }
}
