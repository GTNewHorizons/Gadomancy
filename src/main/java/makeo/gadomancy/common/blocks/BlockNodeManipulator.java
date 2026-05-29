package makeo.gadomancy.common.blocks;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import makeo.gadomancy.common.Gadomancy;
import makeo.gadomancy.common.blocks.tiles.TileNodeManipulator;
import makeo.gadomancy.common.blocks.tiles.TileNodeManipulator.MultiblockType;
import makeo.gadomancy.common.registration.RegisteredItems;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.common.blocks.BlockStoneDevice;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.research.ResearchManager;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p/>
 * Created by HellFirePvP @ 26.10.2015 19:23
 */
public class BlockNodeManipulator extends BlockStoneDevice {

    private static final int METADATA = 5;

    public BlockNodeManipulator() {
        this.setBlockName("blockNodeManipulator");
        this.setCreativeTab(RegisteredItems.creativeTab);
    }

    @Override
    public void registerBlockIcons(IIconRegister ir) {
        super.registerBlockIcons(ir);

        this.iconPedestal[1] = ir.registerIcon(Gadomancy.MODID + ":manipulator_bot");
        this.iconWandPedestal[0] = ir.registerIcon(Gadomancy.MODID + ":manipulator_side");
        this.iconWandPedestal[1] = ir.registerIcon(Gadomancy.MODID + ":manipulator_top");

        this.iconWandPedestalFocus[0] = ir.registerIcon(Gadomancy.MODID + ":manipulator_focus_side");
        this.iconWandPedestalFocus[1] = ir.registerIcon(Gadomancy.MODID + ":manipulator_focus_top");
        this.iconWandPedestalFocus[2] = ir.registerIcon(Gadomancy.MODID + ":manipulator_focus_bot");
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return (metadata == METADATA) ? new TileNodeManipulator() : null;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int rs) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileNodeManipulator) return 0;
        return super.getComparatorInputOverride(world, x, y, z, rs);
    }

    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(item, 1, METADATA));
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float subX,
            float subY, float subZ) {
        if (world.isRemote) {
            return false;
        }

        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof TileNodeManipulator nodeManipulatorTE)) {
            return false;
        }

        if (nodeManipulatorTE.isInMultiblock()) {
            return super.onBlockActivated(world, x, y, z, player, side, subX, subY, subZ);
        }

        if (nodeManipulatorTE.detectMultiblockType() == null) {
            return false;
        }

        ItemStack heldItem = player.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemWandCasting)) {
            return false;
        }

        MultiblockType type = nodeManipulatorTE.getMultiblockType();
        if (!ResearchManager.isResearchComplete(player.getCommandSenderName(), type.getResearchNeeded())) {
            return false;
        }

        if (!ThaumcraftApiHelper
                .consumeVisFromWandCrafting(player.getCurrentEquippedItem(), player, type.getMultiblockCosts(), true)) {
            return false;
        }

        nodeManipulatorTE.formMultiblock();
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileNodeManipulator nodeManipulatorTE) {
                if (nodeManipulatorTE.isInMultiblock()) nodeManipulatorTE.breakMultiblock();
            }
        }
        super.breakBlock(world, x, y, z, block, meta);
    }
}
