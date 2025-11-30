package makeo.gadomancy.common.crafting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

import makeo.gadomancy.common.research.SimpleResearchItem;
import makeo.gadomancy.common.utils.NBTHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.lib.crafting.InfusionRunicAugmentRecipe;
import thaumcraft.common.lib.research.ResearchManager;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p/>
 * Created by makeo @ 25.12.2015 15:35
 */
public class InfusionDisguiseArmor extends InfusionRunicAugmentRecipe {

    public static final ItemStack[] COMPONENTS = { new ItemStack(Items.slime_ball),
            new ItemStack(ConfigItems.itemResource, 1, 3), new ItemStack(Items.slime_ball),
            new ItemStack(ConfigItems.itemResource, 1, 3) };
    public static final AspectList ASPECTS = new AspectList().add(Aspect.SLIME, 12).add(Aspect.ARMOR, 10)
            .add(Aspect.MAGIC, 8);
    private final Map<ItemStack, List<ItemStack>> cachedItems = new HashMap<>();
    private final Object cachedOutput;

    public InfusionDisguiseArmor() {
        super();
        this.cachedOutput = super.getRecipeOutput();
    }

    @Override
    public boolean matches(ArrayList<ItemStack> input, ItemStack central, World world, EntityPlayer player) {
        if (input.size() != InfusionDisguiseArmor.COMPONENTS.length + 1 || !ResearchManager
                .isResearchComplete(player.getCommandSenderName(), SimpleResearchItem.getFullName("ARMORDISGUISE"))) {
            return false;
        }

        List<ItemStack> copy = (List<ItemStack>) input.clone();
        List<ItemStack> result = new ArrayList<>(copy.size());
        for (ItemStack required : InfusionDisguiseArmor.COMPONENTS) {
            boolean contains = false;
            for (int i = 0; i < copy.size(); i++) {
                if (InfusionRecipe.areItemStacksEqual(required, copy.get(i), true)) {
                    contains = true;
                    result.add(copy.get(i));
                    copy.remove(i);
                    break;
                }
            }
            if (!contains) {
                return false;
            }
        }

        ItemStack disguise = copy.get(0);
        if (!this.isValidDisguise(central, disguise)) {
            return false;
        }

        result.add(0, disguise);
        this.cachedItems.put(central, result);
        return true;
    }

    @Override
    public ItemStack[] getComponents(ItemStack input) {
        List<ItemStack> components = this.cachedItems.get(input);
        if (components != null) {
            return components.toArray(new ItemStack[0]);
        }
        return new ItemStack[0];
    }

    @Override
    public AspectList getAspects(ItemStack input) {
        this.cachedItems.remove(input);
        return InfusionDisguiseArmor.ASPECTS;
    }

    @Override
    public int getInstability(ItemStack input) {
        return -20;
    }

    @Override
    public Object getRecipeOutput(ItemStack input) {
        List<ItemStack> components = this.cachedItems.get(input);
        if (components != null) {
            return InfusionDisguiseArmor.disguiseStack(input, components.get(0));
        }
        return input;
    }

    @Override
    public Object getRecipeOutput() {
        return this.cachedOutput;
    }

    public static ItemStack disguiseStack(ItemStack stack, ItemStack disguise) {
        ItemStack out = stack.copy();
        NBTTagCompound compound = NBTHelper.getPersistentData(out);
        if (disguise.getItem() == Items.potionitem) {
            compound.setBoolean("disguise", true);
        } else {
            compound.setTag("disguise", disguise.writeToNBT(new NBTTagCompound()));
        }
        return out;
    }

    private boolean isValidDisguise(ItemStack armor, ItemStack disguise) {
        if (this.isDisguised(armor) || this.isDisguised(disguise)) {
            return false;
        }

        int armorPos = EntityLiving.getArmorPosition(armor);
        return armorPos != 0 && (this.isInvisItem(disguise) || EntityLiving.getArmorPosition(disguise) == armorPos);
    }

    private boolean isDisguised(ItemStack stack) {
        return NBTHelper.hasPersistentData(stack) && NBTHelper.getPersistentData(stack).hasKey("disguise");
    }

    private boolean isInvisItem(ItemStack disguise) {
        if (disguise.getItem() == Items.potionitem) {
            List effects = Items.potionitem.getEffects(disguise);
            for (Object obj : effects) {
                if (obj instanceof PotionEffect) {
                    if (((PotionEffect) obj).getPotionID() == Potion.invisibility.getId()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
