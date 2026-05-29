package makeo.gadomancy.common.crafting;

import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import makeo.gadomancy.common.items.baubles.ItemEtherealFamiliar;
import makeo.gadomancy.common.registration.RegisteredItems;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.ItemWispEssence;

public class EtherealFamiliarCreationRecipe extends InfusionRecipe {

    private Aspect lastMatchedAspect = null;

    public EtherealFamiliarCreationRecipe(String research, int inst, ItemStack central, ItemStack... components) {
        super(research, null, inst, new AspectList(), central, components);
    }

    @Override
    public boolean matches(ArrayList<ItemStack> input, ItemStack central, World world, EntityPlayer player) {
        if (central == null || central.getItem() != ConfigItems.itemAmuletRunic || central.getItemDamage() != 0) {
            return false;
        }
        // Find aspect from ethereal essence
        lastMatchedAspect = null;
        for (ItemStack stack : input) {
            if (stack == null || !(stack.getItem() instanceof ItemWispEssence)) {
                continue;
            }
            AspectList al = ((ItemWispEssence) stack.getItem()).getAspects(stack);
            if (al == null || al.size() != 1) {
                continue;
            }
            Aspect a = al.getAspects()[0];

            if (al.getAmount(a) == 2) {
                if (lastMatchedAspect == null) {
                    lastMatchedAspect = a;
                } else if (lastMatchedAspect != a) {
                    lastMatchedAspect = null;
                    return false; // Aspects must match
                }
            }
        }

        if (lastMatchedAspect == null) return false;

        ArrayList<ItemStack> remaining = new ArrayList<>();
        for (ItemStack is : input) remaining.add(is.copy());

        for (ItemStack comp : this.getComponents()) {
            boolean matched = false;

            for (int i = 0; i < remaining.size(); i++) {
                ItemStack test = remaining.get(i);

                if (isValidComponent(test, comp, lastMatchedAspect)) {
                    remaining.remove(i);
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                lastMatchedAspect = null;
                return false;
            }
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
    public Object getRecipeOutput(ItemStack central) {
        ItemStack result = new ItemStack(RegisteredItems.itemEtherealFamiliar);
        if (lastMatchedAspect == null) {
            ItemEtherealFamiliar.setFamiliarAspect(result, Aspect.EARTH);
        } else {
            ItemEtherealFamiliar.setFamiliarAspect(result, lastMatchedAspect);
        }
        return result;
    }

    @Override
    public AspectList getAspects(ItemStack central) {
        AspectList list = new AspectList();
        list.add(Aspect.AURA, 12);
        list.add(Aspect.MAGIC, 18);

        if (lastMatchedAspect != null) {
            list.add(lastMatchedAspect, 10);
        } else {
            list.add(Aspect.EARTH, 10);
        }

        return list;
    }
}
