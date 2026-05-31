package makeo.gadomancy.common.crafting;

import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import makeo.gadomancy.common.familiar.FamiliarAugment;
import makeo.gadomancy.common.items.baubles.ItemEtherealFamiliar;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.items.ItemWispEssence;

/**
 * HellFirePvP@Admin Date: 19.04.2016 / 02:02 on Gadomancy EtherealFamiliarUpgradeRecipe
 */
public class EtherealFamiliarUpgradeRecipe extends InfusionRecipe {

    private final int extraEssentia;
    private final FamiliarAugment toAdd;
    private final int requiredPreviousLevel;
    private final Object cachedOutput;

    public EtherealFamiliarUpgradeRecipe(String research, int inst, AspectList aspects, int extraEssentia,
            ItemStack familiarIn, FamiliarAugment toAdd, int reqPrev, ItemStack... surroundings) {
        super(research, null, inst, aspects, familiarIn, surroundings);
        this.extraEssentia = extraEssentia;
        this.toAdd = toAdd;
        this.requiredPreviousLevel = reqPrev;
        this.cachedOutput = super.getRecipeOutput();
    }

    @Override
    public boolean matches(ArrayList<ItemStack> input, ItemStack in, World world, EntityPlayer player) {
        if (in == null || !(in.getItem() instanceof ItemEtherealFamiliar)) return false;
        if (this.getRecipeInput() == null || !(this.getRecipeInput().getItem() instanceof ItemEtherealFamiliar))
            return false;
        Aspect aspect = ItemEtherealFamiliar.getFamiliarAspect(in);
        if (aspect == null) return false;

        if ((!this.research.isEmpty())
                && (!ThaumcraftApiHelper.isResearchComplete(player.getCommandSenderName(), this.research))) {
            return false;
        }

        FamiliarAugment.FamiliarAugmentList list = ItemEtherealFamiliar.getAugments(in);

        int level;
        if (list.contains(this.toAdd)) {
            level = list.getLevel(this.toAdd);
        } else {
            level = 0;
        }
        if (this.requiredPreviousLevel > level) return false;

        if (!this.toAdd.checkConditions(list, level + 1)) {
            return false;
        }

        // Normal infusion recipe stuff...

        ArrayList<ItemStack> inputs = new ArrayList<>();
        for (ItemStack is : input) {
            inputs.add(is.copy());
        }
        for (ItemStack comp : this.getComponents()) {
            boolean matched = false;

            for (int i = 0; i < inputs.size(); i++) {
                ItemStack stack = inputs.get(i);

                if (isValidComponent(stack, comp, aspect)) {
                    inputs.remove(i);
                    matched = true;
                    break;
                }
            }

            if (!matched) return false;
        }
        return true;
    }

    private boolean isValidComponent(ItemStack input, ItemStack expected, Aspect aspect) {
        if (input.getItem() instanceof ItemWispEssence item && input.getItem() == expected.getItem()) {
            AspectList al = item.getAspects(input);
            return al != null && al.getAmount(aspect) == 2;
        }

        return InfusionRecipe.areItemStacksEqual(input, expected, true);
    }

    @Override
    public Object getRecipeOutput(ItemStack input) {
        ItemStack inputCopy = input.copy();
        ItemEtherealFamiliar.incrementAugmentLevel(inputCopy, this.toAdd);
        return inputCopy;
    }

    @Override
    public Object getRecipeOutput() {
        return this.cachedOutput;
    }

    @Override
    public AspectList getAspects(ItemStack input) {
        Aspect aspect = ItemEtherealFamiliar.getFamiliarAspect(input);

        AspectList list = aspects.copy();

        if (aspect != null) {
            list.add(aspect, extraEssentia);
        }

        return list;
    }
}
