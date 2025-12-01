package makeo.gadomancy.common.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import makeo.gadomancy.common.Gadomancy;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.internal.DummyInternalMethodHandler;
import thaumcraft.api.internal.IInternalMethodHandler;
import thaumcraft.common.lib.research.ResearchManager;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p/>
 * Created by makeo @ 26.12.2015 18:10
 */
public class InfusionVisualDisguiseArmor extends InfusionRecipe {

    private static final IInternalMethodHandler FAKE_HANDLER = new FakeMethodHandler();
    private static final int[] POTION_METAS = { 8206, 16398, 8270, 16462 };

    private static ItemStack[] armorItems;

    private final ItemStack[] components;
    private final boolean transparent;

    public InfusionVisualDisguiseArmor(boolean transparent) {
        super(
                "",
                new ItemStack(Blocks.cobblestone),
                0,
                InfusionDisguiseArmor.ASPECTS,
                new ItemStack(Blocks.dirt),
                new ItemStack[0]);
        this.transparent = transparent;

        this.components = new ItemStack[InfusionDisguiseArmor.COMPONENTS.length + 1];
        System.arraycopy(
                InfusionDisguiseArmor.COMPONENTS,
                0,
                this.components,
                1,
                InfusionDisguiseArmor.COMPONENTS.length);

        if (InfusionVisualDisguiseArmor.armorItems == null && Gadomancy.proxy.getSide() == Side.CLIENT) {
            this.findArmorItems();
        }
    }

    private void findArmorItems() {
        List<ItemStack> items = new ArrayList<>();
        for (Object o : Item.itemRegistry) {
            if (o instanceof Item item && item.getCreativeTab() != null) {
                final ItemStack stack = new ItemStack(item);
                if (EntityLiving.getArmorPosition(stack) != 0) {
                    try {
                        item.getSubItems(item, item.getCreativeTab(), items);
                    } catch (Throwable ignored) {}
                }
            }
        }
        items.removeIf(stack -> stack == null || EntityLiving.getArmorPosition(stack) == 0);
        InfusionVisualDisguiseArmor.armorItems = items.toArray(new ItemStack[0]);
    }

    private boolean canSee(ItemStack stack) {
        String research = InfusionVisualDisguiseArmor.getResearchKey(stack);
        return research == null || ResearchManager
                .isResearchComplete(Minecraft.getMinecraft().thePlayer.getCommandSenderName(), research);
    }

    public ItemStack getCurrentDisguise() {
        if (this.transparent) {
            return new ItemStack(
                    Items.potionitem,
                    1,
                    InfusionVisualDisguiseArmor.POTION_METAS[(int) ((System.currentTimeMillis() / 1000)
                            % InfusionVisualDisguiseArmor.POTION_METAS.length)]);
        }

        Random random = new Random(System.currentTimeMillis() / 1000);
        int first = this.getRecipeInput(random);
        int firstPos = EntityLiving.getArmorPosition(InfusionVisualDisguiseArmor.armorItems[first]);

        int next;
        do {
            next = random.nextInt(InfusionVisualDisguiseArmor.armorItems.length);
        } while (next == first
                || EntityLiving.getArmorPosition(InfusionVisualDisguiseArmor.armorItems[next]) != firstPos
                || !this.canSee(InfusionVisualDisguiseArmor.armorItems[next]));
        return InfusionVisualDisguiseArmor.armorItems[next];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ItemStack getRecipeInput() {
        if (Minecraft.getMinecraft().thePlayer == null) {
            return new ItemStack(Items.cookie);
        }
        return InfusionVisualDisguiseArmor.armorItems[this
                .getRecipeInput(new Random(System.currentTimeMillis() / 1000))];
    }

    private int getRecipeInput(Random random) {
        int index;
        do {
            index = random.nextInt(InfusionVisualDisguiseArmor.armorItems.length);
        } while (!this.canSee(InfusionVisualDisguiseArmor.armorItems[index]));
        return index;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Object getRecipeOutput(ItemStack input) {
        if (Minecraft.getMinecraft().thePlayer == null) {
            return new ItemStack(Items.cookie);
        }
        return InfusionDisguiseArmor.disguiseStack(input, this.getCurrentDisguise());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ItemStack[] getComponents() {
        this.components[0] = this.getCurrentDisguise();
        return this.components;
    }

    @SideOnly(Side.CLIENT)
    private static String getResearchKey(ItemStack stack) {
        IInternalMethodHandler old = ThaumcraftApi.internalMethods;
        ThaumcraftApi.internalMethods = InfusionVisualDisguiseArmor.FAKE_HANDLER;
        Object[] result = ThaumcraftApi.getCraftingRecipeKey(Minecraft.getMinecraft().thePlayer, stack);
        ThaumcraftApi.internalMethods = old;
        return result == null ? null : (String) result[0];
    }

    private static class FakeMethodHandler extends DummyInternalMethodHandler {

        @Override
        public boolean isResearchComplete(String username, String researchkey) {
            return true;
        }
    }
}
