package makeo.gadomancy.common.crafting;

import net.minecraft.item.ItemStack;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.items.ItemWispEssence;

final class FamiliarComponents {

    private FamiliarComponents() {}

    /**
     * Returns a copy of the given components where every generic wisp essence is tagged with 2 of the given aspect, so
     * NEI and the thaumonomicon show a concrete aspect (Terra unless a craft just matched) instead of a color-cycling
     * generic essence, and so the infusion matrix binds pedestal consumption to the aspect once matches() resolved it.
     */
    static ItemStack[] resolveWispEssences(ItemStack[] components, Aspect aspect) {
        ItemStack[] resolved = components.clone();
        for (int i = 0; i < resolved.length; i++) {
            ItemStack comp = resolved[i];
            if (comp != null && comp.getItem() instanceof ItemWispEssence) {
                resolved[i] = comp.copy();
                ((ItemWispEssence) comp.getItem()).setAspects(resolved[i], new AspectList().add(aspect, 2));
            }
        }
        return resolved;
    }
}
