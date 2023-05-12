package makeo.gadomancy.common.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import makeo.gadomancy.common.Gadomancy;
import makeo.gadomancy.common.registration.RegisteredBlocks;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by makeo @ 29.09.2015 22:07
 */
public abstract class BlockTransparent extends Block implements IBlockTransparent {

    protected BlockTransparent(Material material) {
        super(material);
    }

    private IIcon iconTransparent;

    public IIcon getTransparentIcon() {
        return this.iconTransparent;
    }

    @Override
    public void registerBlockIcons(IIconRegister ir) {
        this.iconTransparent = ir.registerIcon(Gadomancy.MODID + ":transparent");
        super.registerBlockIcons(ir);
    }

    @Override
    public int getRenderType() {
        return RegisteredBlocks.rendererTransparentBlock;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }
}
