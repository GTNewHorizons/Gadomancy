package makeo.gadomancy.common.integration;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

import makeo.gadomancy.common.Gadomancy;

/**
 * HellFirePvP@Admin Date: 20.04.2016 / 00:43 on Gadomancy IntegrationThaumicTinkerer
 */
public class IntegrationThaumicTinkerer extends IntegrationMod {

    private static Class infusedCropBlockClass, infusedCropTile;

    @Override
    public String getModId() {
        return "ThaumicTinkerer";
    }

    @Override
    public void doInit() {
        try {
            IntegrationThaumicTinkerer.infusedCropBlockClass = Class
                    .forName("thaumic.tinkerer.common.block.BlockInfusedGrain");
            IntegrationThaumicTinkerer.infusedCropTile = Class
                    .forName("thaumic.tinkerer.common.block.tile.TileInfusedGrain");
            if (IntegrationThaumicTinkerer.infusedCropBlockClass != null
                    && IntegrationThaumicTinkerer.infusedCropTile != null) {
                Gadomancy.log.info("Hooked TTinkerer magic-crops");
            }
        } catch (Throwable tr) {}
    }

    public static boolean isCropBlock(Block block) {
        if (IntegrationThaumicTinkerer.infusedCropBlockClass == null) return false;
        return block.getClass().equals(IntegrationThaumicTinkerer.infusedCropBlockClass)
                || IntegrationThaumicTinkerer.infusedCropBlockClass.isAssignableFrom(block.getClass());
    }

    public static boolean isCropTile(TileEntity te) {
        if (IntegrationThaumicTinkerer.infusedCropTile == null) return false;
        return te.getClass().equals(IntegrationThaumicTinkerer.infusedCropTile)
                || IntegrationThaumicTinkerer.infusedCropTile.isAssignableFrom(te.getClass());
    }
}
