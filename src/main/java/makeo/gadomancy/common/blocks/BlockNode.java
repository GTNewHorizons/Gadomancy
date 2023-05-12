package makeo.gadomancy.common.blocks;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import makeo.gadomancy.common.blocks.tiles.TileExtendedNode;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by HellFirePvP @ 22.10.2015 19:47
 */
public class BlockNode extends thaumcraft.common.blocks.BlockAiry {

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        if (metadata == 0) {
            return new TileExtendedNode();
        }
        return super.createTileEntity(world, metadata);
    }
}
