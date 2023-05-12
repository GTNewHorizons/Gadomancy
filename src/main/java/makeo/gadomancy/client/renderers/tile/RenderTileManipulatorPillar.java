package makeo.gadomancy.client.renderers.tile;

import java.util.Map;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import makeo.gadomancy.common.utils.Injector;
import makeo.gadomancy.common.utils.SimpleResourceLocation;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.client.renderers.tile.TileInfusionPillarRenderer;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p/>
 * Created by makeo @ 27.10.2015 13:23
 */
public class RenderTileManipulatorPillar extends TileInfusionPillarRenderer {

    private static final String TC_TEXTURE = "textures/models/pillar.png";
    private static final ResourceLocation TEXTURE = new SimpleResourceLocation("models/manipulator_pillar.png");
    private static final Map<String, ResourceLocation> BOUND_TEXTURES = new Injector(UtilsFX.class)
            .getField("boundTextures");

    @Override
    public void renderTileEntityAt(TileEntity par1TileEntity, double par2, double par4, double par6, float par8) {
        ResourceLocation originalTexture = RenderTileManipulatorPillar.BOUND_TEXTURES
                .get(RenderTileManipulatorPillar.TC_TEXTURE);
        RenderTileManipulatorPillar.BOUND_TEXTURES
                .put(RenderTileManipulatorPillar.TC_TEXTURE, RenderTileManipulatorPillar.TEXTURE);
        super.renderTileEntityAt(par1TileEntity, par2, par4, par6, par8);

        if (originalTexture != null) {
            RenderTileManipulatorPillar.BOUND_TEXTURES.put(RenderTileManipulatorPillar.TC_TEXTURE, originalTexture);
        } else {
            RenderTileManipulatorPillar.BOUND_TEXTURES.remove(RenderTileManipulatorPillar.TC_TEXTURE);
        }
    }
}
