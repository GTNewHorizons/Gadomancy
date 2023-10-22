package makeo.gadomancy.common.registration;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.util.EnumHelper;

import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import makeo.gadomancy.common.Gadomancy;
import makeo.gadomancy.common.items.ItemArcanePackage;
import makeo.gadomancy.common.items.ItemAuraCore;
import makeo.gadomancy.common.items.ItemCreativeNode;
import makeo.gadomancy.common.items.ItemElement;
import makeo.gadomancy.common.items.ItemExtendedNodeJar;
import makeo.gadomancy.common.items.ItemFakeGolemPlacer;
import makeo.gadomancy.common.items.ItemFakeGolemShield;
import makeo.gadomancy.common.items.ItemFakeLootbag;
import makeo.gadomancy.common.items.ItemFakeModIcon;
import makeo.gadomancy.common.items.ItemGolemCoreBreak;
import makeo.gadomancy.common.items.ItemTransformationFocus;
import makeo.gadomancy.common.items.baubles.ItemEtherealFamiliar;
import makeo.gadomancy.common.items.baubles.ItemFamiliar_Old;
import makeo.gadomancy.common.research.SimpleResearchItem;
import makeo.gadomancy.common.utils.Injector;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.lib.research.ResearchManager;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by makeo @ 03.06.2015 03:37
 */
public class RegisteredItems {

    private RegisteredItems() {}

    public static EnumRarity raritySacred = EnumHelper.addRarity("Sacred", EnumChatFormatting.GOLD, "Sacred");

    public static CreativeTabs creativeTab;

    public static ItemFakeModIcon itemFakeModIcon;
    public static ItemFakeGolemShield itemFakeGolemShield;
    public static ItemFakeGolemPlacer itemFakeGolemPlacer;

    public static ItemTransformationFocus itemTransformationFocus;
    public static ItemGolemCoreBreak itemGolemCoreBreak;
    public static ItemExtendedNodeJar itemExtendedNodeJar;
    public static ItemFamiliar_Old itemFamiliar_old;
    public static ItemEtherealFamiliar itemEtherealFamiliar;
    public static ItemCreativeNode itemCreativeNode;
    public static ItemArcanePackage itemPackage;
    public static ItemFakeLootbag itemFakeLootbag;
    public static ItemAuraCore itemAuraCore;
    public static ItemElement itemElement;

    public static void preInit() {
        RegisteredItems.creativeTab = new CreativeTabs(Gadomancy.MODID) {

            @Override
            public Item getTabIconItem() {
                return RegisteredItems.itemFakeModIcon;
            }
        };
    }

    public static void init() {
        RegisteredItems.registerItems();

        RegisteredItems.registerDefaultStickyJars();
    }

    public static void postInit() {
        RegisteredItems.registerItemAspects();
    }

    // Items
    private static void registerItems() {
        RegisteredItems.itemTransformationFocus = RegisteredItems.registerItem(new ItemTransformationFocus());
        RegisteredItems.itemGolemCoreBreak = RegisteredItems.registerItem(new ItemGolemCoreBreak());
        RegisteredItems.itemFamiliar_old = RegisteredItems.registerItem(new ItemFamiliar_Old(), "ItemFamiliar");
        RegisteredItems.itemEtherealFamiliar = RegisteredItems.registerItem(new ItemEtherealFamiliar());
        RegisteredItems.itemAuraCore = RegisteredItems.registerItem(new ItemAuraCore());
        RegisteredItems.itemElement = RegisteredItems.registerItem(new ItemElement());

        RegisteredItems.itemCreativeNode = RegisteredItems.registerItem(new ItemCreativeNode());
        RegisteredItems.itemPackage = RegisteredItems.registerItem(new ItemArcanePackage());
        RegisteredItems.itemFakeLootbag = RegisteredItems.registerItem(Thaumcraft.MODID, new ItemFakeLootbag());
        RegisteredItems.itemFakeGolemPlacer = RegisteredItems.registerItem(new ItemFakeGolemPlacer());
        RegisteredItems.itemFakeGolemShield = RegisteredItems.registerItem(new ItemFakeGolemShield());
        RegisteredItems.itemExtendedNodeJar = RegisteredItems.registerItem(new ItemExtendedNodeJar());
        RegisteredItems.itemFakeModIcon = RegisteredItems.registerItem(new ItemFakeModIcon());
    }

    private static <T extends Item> T registerItem(T item, String name) {
        GameRegistry.registerItem(item, name);
        return item;
    }

    private static <T extends Item> T registerItem(T item) {
        return RegisteredItems.registerItem(item, item.getClass().getSimpleName());
    }

    private static <T extends Item> T registerItem(String modId, T item) {
        return RegisteredItems.registerItem(modId, item, item.getClass().getSimpleName());
    }

    private static <T extends Item> T registerItem(String modId, T item, String name) {
        Injector modController = new Injector(
                new Injector(Loader.instance(), Loader.class).getField("modController"),
                LoadController.class);
        Object old = modController.getField("activeContainer");
        modController.setField("activeContainer", Loader.instance().getIndexedModList().get(modId));

        GameRegistry.registerItem(item, name);

        modController.setField("activeContainer", old);
        return item;
    }

    private static void registerItemAspects() {
        AspectList oldAspects = ThaumcraftCraftingManager
                .getObjectTags(new ItemStack(RegisteredBlocks.blockStoneMachine));

        ThaumcraftApi.registerObjectTag(
                new ItemStack(Item.getItemFromBlock(RegisteredBlocks.blockStoneMachine)),
                new int[] { 11, 15 },
                new AspectList());
        ThaumcraftApi.registerObjectTag(
                new ItemStack(Item.getItemFromBlock(RegisteredBlocks.blockStoneMachine)),
                new int[] { 0 },
                oldAspects);

        AspectList pylon = new AspectList().add(Aspect.WATER, 10).add(Aspect.MAGIC, 12).add(Aspect.VOID, 4)
                .add(Aspect.MECHANISM, 4);
        ThaumcraftApi.registerObjectTag(
                new ItemStack(Item.getItemFromBlock(RegisteredBlocks.blockAuraPylon)),
                new int[] { 0 },
                pylon);
        pylon = new AspectList().add(Aspect.FIRE, 10).add(Aspect.AURA, 12).add(Aspect.MAGIC, 8).add(Aspect.LIGHT, 4)
                .add(Aspect.MECHANISM, 4);
        ThaumcraftApi.registerObjectTag(
                new ItemStack(Item.getItemFromBlock(RegisteredBlocks.blockAuraPylon)),
                new int[] { 1 },
                pylon);

        AspectList packager = new AspectList().add(Aspect.TREE, 10).add(Aspect.MECHANISM, 8).add(Aspect.CRAFT, 8)
                .add(Aspect.AURA, 12);
        ThaumcraftApi.registerObjectTag(
                new ItemStack(Item.getItemFromBlock(RegisteredBlocks.blockStoneMachine)),
                new int[] { 4 },
                packager);

        AspectList aspect = new AspectList();
        aspect.add(Aspect.MAGIC, 6).add(Aspect.AURA, 12).add(Aspect.ELDRITCH, 4).add(Aspect.VOID, 10);
        ThaumcraftApi.registerObjectTag(new ItemStack(RegisteredItems.itemAuraCore), new int[] { 0 }, aspect.copy());
        ThaumcraftApi.registerObjectTag(
                new ItemStack(RegisteredItems.itemAuraCore),
                new int[] { 1 },
                aspect.copy().add(Aspect.AIR, 26));
        ThaumcraftApi.registerObjectTag(
                new ItemStack(RegisteredItems.itemAuraCore),
                new int[] { 2 },
                aspect.copy().add(Aspect.FIRE, 26));
        ThaumcraftApi.registerObjectTag(
                new ItemStack(RegisteredItems.itemAuraCore),
                new int[] { 3 },
                aspect.copy().add(Aspect.WATER, 26));
        ThaumcraftApi.registerObjectTag(
                new ItemStack(RegisteredItems.itemAuraCore),
                new int[] { 4 },
                aspect.copy().add(Aspect.EARTH, 26));
        ThaumcraftApi.registerObjectTag(
                new ItemStack(RegisteredItems.itemAuraCore),
                new int[] { 5 },
                aspect.copy().add(Aspect.ORDER, 26));
        ThaumcraftApi.registerObjectTag(
                new ItemStack(RegisteredItems.itemAuraCore),
                new int[] { 6 },
                aspect.copy().add(Aspect.ENTROPY, 26));

        ThaumcraftApi.registerObjectTag(
                new ItemStack(RegisteredItems.itemPackage, 1, 0),
                new AspectList().add(Aspect.CLOTH, 2).add(Aspect.BEAST, 2).add(Aspect.ARMOR, 1));
        ThaumcraftApi.registerObjectTag(
                new ItemStack(RegisteredItems.itemPackage, 1, 1),
                new AspectList().add(Aspect.CLOTH, 4));
        ThaumcraftApi.registerObjectTag(
                new ItemStack(RegisteredItems.itemFakeLootbag, 1, 0),
                new AspectList().add(Aspect.CLOTH, 2).add(Aspect.BEAST, 2).add(Aspect.ARMOR, 1));
        ThaumcraftApi.registerObjectTag(
                new ItemStack(RegisteredItems.itemFakeLootbag, 1, 1),
                new AspectList().add(Aspect.CLOTH, 4));

        ThaumcraftApi.registerObjectTag(
                new ItemStack(RegisteredBlocks.blockKnowledgeBook),
                new AspectList().add(Aspect.MIND, 8).add(Aspect.MECHANISM, 4).add(Aspect.MAGIC, 6)
                        .add(Aspect.ORDER, 4));
    }

    // Sticky jars
    private static void registerDefaultStickyJars() {
        RegisteredItems.registerStickyJar(ConfigItems.itemJarFilled, 0);
        RegisteredItems.registerStickyJar(ConfigItems.itemJarFilled, 3);

        Item itemBlockJar = Item.getItemFromBlock(ConfigBlocks.blockJar);
        RegisteredItems.registerStickyJar(itemBlockJar, 0, new ItemStack(itemBlockJar, 1, 0), "JARLABEL");
        RegisteredItems.registerStickyJar(itemBlockJar, 3, new ItemStack(itemBlockJar, 1, 3), "JARVOID");

        Item itemRemoteJar = Item.getItemFromBlock(RegisteredBlocks.blockRemoteJar);
        RegisteredItems.registerStickyJar(
                itemRemoteJar,
                0,
                new ItemStack(itemRemoteJar),
                SimpleResearchItem.getFullName("REMOTEJAR"));
    }

    private static List<StickyJarItemInfo> stickyJarItems = new ArrayList<StickyJarItemInfo>();

    public static void registerStickyJar(Item item, int damage, ItemStack recipeStack) {
        RegisteredItems.registerStickyJar(item, damage, recipeStack, null);
    }

    public static void registerStickyJar(Item item, int damage, ItemStack recipeStack, String research) {
        StickyJarItemInfo info = new StickyJarItemInfo();
        info.item = item;
        info.damage = damage;
        info.recipeStack = recipeStack;
        info.research = research;
        RegisteredItems.stickyJarItems.add(info);
    }

    public static void registerStickyJar(Item item, int damage) {
        RegisteredItems.registerStickyJar(item, damage, null);
    }

    public static void registerStickyJar(Item item) {
        RegisteredItems.registerStickyJar(item, Short.MAX_VALUE);
    }

    public static boolean isStickyableJar(Item item, int damage) {
        for (StickyJarItemInfo info : RegisteredItems.stickyJarItems) {
            if (info.item == item && (info.damage == damage || info.damage == Short.MAX_VALUE)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isStickyableJar(ItemStack stack) {
        return stack != null && RegisteredItems.isStickyableJar(stack.getItem(), stack.getItemDamage());
    }

    public static List<ItemStack> getStickyJarStacks() {
        return RegisteredItems.getStickyJarStacks(null);
    }

    public static List<ItemStack> getStickyJarStacks(EntityPlayer player) {
        List<ItemStack> stacks = new ArrayList<ItemStack>();
        for (StickyJarItemInfo info : RegisteredItems.stickyJarItems) {
            if (info.recipeStack != null && (player == null || info.research == null
                    || ResearchManager.isResearchComplete(player.getCommandSenderName(), info.research))) {
                stacks.add(info.recipeStack);
            }
        }
        return stacks;
    }

    private static class StickyJarItemInfo {

        public Item item;
        public int damage;
        public ItemStack recipeStack;
        public String research;
    }
}
