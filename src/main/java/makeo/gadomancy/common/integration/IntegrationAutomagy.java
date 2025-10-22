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

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by makeo @ 04.10.2015 02:30
 */
public class IntegrationAutomagy extends IntegrationMod {

    // TODO sync with Automagy some time...
    private static final AspectList visCostAdvNodeJar = new AspectList().add(Aspect.FIRE, 125).add(Aspect.EARTH, 125)
            .add(Aspect.ORDER, 125).add(Aspect.AIR, 125).add(Aspect.ENTROPY, 125).add(Aspect.WATER, 125);

    @Override
    public String getModId() {
        return "Automagy";
    }

    @Override
    protected void doInit() {
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
        Block testBookshelf = Block.getBlockFromName("Thaumcraft:blockCosmeticSolid");
        if (betterBookshelf != null) {
            // Tier 1 Bookshelves - Total 4 surrounding attributes
            TileKnowledgeBook.knowledgeIncreaseMap.put(new TileKnowledgeBook.BlockSnapshot(betterBookshelf, 0), new int[]{1, 2, 2, 0});
            TileKnowledgeBook.knowledgeIncreaseMap.put(new TileKnowledgeBook.BlockSnapshot(betterBookshelf, 1), new int[]{1, 1, 3, 0});
            // Tier 2 Bookshelves - Total 8 surrounding attributes
            TileKnowledgeBook.knowledgeIncreaseMap.put(new TileKnowledgeBook.BlockSnapshot(testBookshelf, 4), new int[]{2, 4, 4, 0});
            TileKnowledgeBook.knowledgeIncreaseMap.put(new TileKnowledgeBook.BlockSnapshot(testBookshelf, 5), new int[]{2, 2, 6, 0});
            TileKnowledgeBook.knowledgeIncreaseMap.put(new TileKnowledgeBook.BlockSnapshot(testBookshelf, 6), new int[]{2, 4, 2, 2});
            // Tier 3 Bookshelves - Total 16 surrounding attributes
            TileKnowledgeBook.knowledgeIncreaseMap.put(new TileKnowledgeBook.BlockSnapshot(testBookshelf, 9), new int[]{3, 8, 8, 0});
            TileKnowledgeBook.knowledgeIncreaseMap.put(new TileKnowledgeBook.BlockSnapshot(testBookshelf, 11), new int[]{3, 4, 12, 0});
            TileKnowledgeBook.knowledgeIncreaseMap.put(new TileKnowledgeBook.BlockSnapshot(testBookshelf, 12), new int[]{3, 8, 4, 4});
            // Tier 4 Bookshelves - Total 32 surrounding attributes
            TileKnowledgeBook.knowledgeIncreaseMap.put(new TileKnowledgeBook.BlockSnapshot(testBookshelf, 14), new int[]{4, 12, 12, 8});
        }
    }

    public boolean handleNodeJarVisCost(ItemStack wandStack, EntityPlayer player) {
        return ThaumcraftApiHelper
                .consumeVisFromWandCrafting(wandStack, player, IntegrationAutomagy.visCostAdvNodeJar, true);
    }

    public void tryFillGolemCrafttable(ChunkCoordinates cc, World world) {
        Class<?> workbenchTileClazz;
        try {
            workbenchTileClazz = Class.forName("tuhljin.automagy.tiles.TileEntityGolemWorkbench");
        } catch (ClassNotFoundException e) {
            return;
        }

        TileEntity te = world.getTileEntity(cc.posX, cc.posY, cc.posZ);
        if (te != null && workbenchTileClazz.isAssignableFrom(te.getClass())) { // method instanceof checking..
            try {
                Injector i = new Injector(te, workbenchTileClazz);
                int heat = i.getField("craftingHeat");
                int impact = i.getField("heatImpactsAt");
                if (heat > impact) {
                    i.setField("craftingHeat", heat - 700);
                }
            } catch (Exception e) {}
        }
    }
}
