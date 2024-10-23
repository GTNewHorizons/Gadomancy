package makeo.gadomancy.common.integration;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

import thaumic.tinkerer.common.block.BlockInfusedGrain;
import thaumic.tinkerer.common.block.tile.TileInfusedGrain;

/**
 * HellFirePvP@Admin Date: 20.04.2016 / 00:43 on Gadomancy IntegrationThaumicTinkerer
 */
public class IntegrationThaumicTinkerer {

    public static boolean isCropBlock(Block block) {
        return block instanceof BlockInfusedGrain;
    }

    public static boolean isCropTile(TileEntity te) {
        return te instanceof TileInfusedGrain;
    }
}
