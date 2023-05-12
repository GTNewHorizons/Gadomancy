package makeo.gadomancy.common.crafting;

import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import makeo.gadomancy.common.familiar.FamiliarAugment;
import makeo.gadomancy.common.items.baubles.ItemEtherealFamiliar;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;

/**
 * HellFirePvP@Admin Date: 19.04.2016 / 02:02 on Gadomancy EtherealFamiliarUpgradeRecipe
 */
public class EtherealFamiliarUpgradeRecipe extends InfusionRecipe {

    private FamiliarAugment toAdd;
    private int requiredPreviousLevel;

    public EtherealFamiliarUpgradeRecipe(String research, int inst, AspectList aspects2, ItemStack familiarIn,
            FamiliarAugment toAdd, int reqPrev, ItemStack... surroundings) {
        super(research, null, inst, aspects2, familiarIn, surroundings);
        this.toAdd = toAdd;
        this.requiredPreviousLevel = reqPrev;
    }

    @Override
    public boolean matches(ArrayList<ItemStack> input, ItemStack in, World world, EntityPlayer player) {
        if (in == null || !(in.getItem() instanceof ItemEtherealFamiliar)) return false; // We call it "FamiliarAugment"
                                                                                         // Recipe for a reason..
        if (this.getRecipeInput() == null || !(this.getRecipeInput().getItem() instanceof ItemEtherealFamiliar))
            return false; // A bit late but still working..

        if ((this.research.length() > 0)
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
        if (this.requiredPreviousLevel > level) return false; // Requires higher level to do this infusion.

        if (!this.toAdd.checkConditions(list, level + 1)) {
            return false; // Preconditions not met.
        }

        // Normal infusionrecipe stuff...

        ItemStack inCopy;
        ArrayList<ItemStack> ii = new ArrayList<ItemStack>();
        for (ItemStack is : input) {
            ii.add(is.copy());
        }
        for (ItemStack comp : this.getComponents()) {
            boolean b = false;
            for (int a = 0; a < ii.size(); a++) {
                inCopy = ii.get(a).copy();
                if (comp.getItemDamage() == 32767) {
                    inCopy.setItemDamage(32767);
                }
                if (InfusionRecipe.areItemStacksEqual(inCopy, comp, true)) {
                    ii.remove(a);
                    b = true;
                    break;
                }
            }
            if (!b) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Object getRecipeOutput(ItemStack in) {
        ItemStack inputCopy = in.copy();
        ItemEtherealFamiliar.incrementAugmentLevel(inputCopy, this.toAdd);
        return inputCopy;
    }
}
