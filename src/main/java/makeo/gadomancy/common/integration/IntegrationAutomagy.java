package makeo.gadomancy.common.integration;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import makeo.gadomancy.common.CommonProxy;
import makeo.gadomancy.common.blocks.tiles.TileKnowledgeBook;
import makeo.gadomancy.common.data.config.ModConfig;
import makeo.gadomancy.common.registration.RegisteredBlocks;
import makeo.gadomancy.common.registration.RegisteredItems;
import makeo.gadomancy.common.utils.Injector;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ConfigBlocks;
import tuhljin.automagy.tiles.TileEntityGolemWorkbench;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p>
 * Created by makeo @ 04.10.2015 02:30
 */
public class IntegrationAutomagy {

    // spotless:off
    // TODO sync with Automagy some time...
    private static final AspectList visCostAdvNodeJar = new AspectList()
            .add(Aspect.FIRE, 125)
            .add(Aspect.EARTH, 125)
            .add(Aspect.ORDER, 125)
            .add(Aspect.AIR, 125)
            .add(Aspect.ENTROPY, 125)
            .add(Aspect.WATER, 125);
    // spotless:on

    public static void doInit() {
        Block infinityJar = Block.getBlockFromName("Automagy:blockCreativeJar");
        if (infinityJar != null) {
            RegisteredBlocks.registerStickyJar(infinityJar, 3, false, true);
            RegisteredItems.registerStickyJar(Item.getItemFromBlock(infinityJar), 3);
        }

        if (ModConfig.enableAdditionalNodeTypes) {
            CommonProxy.unregisterWandHandler("Automagy", ConfigBlocks.blockWarded, -1);
        }

        // Better bookshelves -> MOAR knowledge
        Block betterBookshelf = Block.getBlockFromName("Automagy:blockBookshelfEnchanted");
        if (betterBookshelf != null) {
            TileKnowledgeBook.knowledgeIncreaseMap.put(new TileKnowledgeBook.BlockSnapshot(betterBookshelf, 0), 2);
            TileKnowledgeBook.knowledgeIncreaseMap.put(new TileKnowledgeBook.BlockSnapshot(betterBookshelf, 1), 4);
        }
    }

    public static boolean handleNodeJarVisCost(ItemStack wandStack, EntityPlayer player) {
        return ThaumcraftApiHelper
                .consumeVisFromWandCrafting(wandStack, player, IntegrationAutomagy.visCostAdvNodeJar, true);
    }

    public static void tryFillGolemCrafttable(ChunkCoordinates cc, World world) {
        Class<?> clazz = TileEntityGolemWorkbench.class;
        TileEntity te = world.getTileEntity(cc.posX, cc.posY, cc.posZ);
        if (te instanceof TileEntityGolemWorkbench) {
            try {
                final TileEntityGolemWorkbench golemWorkbench = (TileEntityGolemWorkbench) te;
                Injector i = new Injector(te, clazz);
                int heat = golemWorkbench.craftingHeat;
                int impact = i.getField("heatImpactsAt");
                if (heat > impact) {
                    golemWorkbench.craftingHeat = heat - 700;
                }
            } catch (Exception e) {}
        }
    }
}
