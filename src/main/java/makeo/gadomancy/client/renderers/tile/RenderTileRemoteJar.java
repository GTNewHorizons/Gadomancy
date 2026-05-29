package makeo.gadomancy.client.renderers.tile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import makeo.gadomancy.client.models.ModelJarPot;
import makeo.gadomancy.common.blocks.BlockRemoteJar;
import makeo.gadomancy.common.utils.SimpleResourceLocation;
import makeo.gadomancy.common.utils.world.fake.FakeWorld;
import thaumcraft.client.renderers.tile.TileJarRenderer;
import thaumcraft.common.tiles.TileMirrorEssentia;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p>
 * Created by makeo @ 14.10.2015 16:01
 */
public class RenderTileRemoteJar extends TileJarRenderer {

    private static final ModelJarPot MODEL_JAR_POT = new ModelJarPot();
    private static final ResourceLocation OVERLAY_TEXTURE = new SimpleResourceLocation("models/jar_pot.png");

    private final TileMirrorEssentia tileMirror;

    public RenderTileRemoteJar() {
        this.tileMirror = new TileMirrorEssentia();
    }

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);

        GL11.glPushMatrix();

        GL11.glTranslatef(0.45f / 2, 0.003f, 0.45f / 2);
        GL11.glScalef(0.55f, 1, 0.55f);

        if (this.field_147501_a.field_147553_e != null) {
            this.setupTile(tile, BlockRemoteJar.getJarTile(tile).networkId != null);
            try {
                this.tileMirror.setWorldObj(tile.getWorldObj());
                TileEntityRendererDispatcher.instance.renderTileEntityAt(this.tileMirror, 0, 0, 0, partialTicks);
            } finally {
                this.tileMirror.setWorldObj(null);
            }
        }

        GL11.glPopMatrix();

        GL11.glDisable(GL11.GL_CULL_FACE);

        GL11.glTranslatef(0.5f, 0.002f, 0.5f);
        GL11.glRotatef(180, 0, 0, 1);

        this.bindTexture(RenderTileRemoteJar.OVERLAY_TEXTURE);

        GL11.glScalef(1.002f, 1f, 1.002f);

        GL11.glTranslatef(0, -1.5f, 0);
        // GL11.glRotatef(180, 1f, 0, 0);

        RenderTileRemoteJar.MODEL_JAR_POT.render(null, 0f, 0f, 0f, 0f, 0f, 0.0625f);

        GL11.glPopMatrix();
        GL11.glPopAttrib();

        super.renderTileEntityAt(tile, x, y, z, partialTicks);
    }

    private void setupTile(TileEntity tile, boolean linked) {
        this.tileMirror.linked = linked;
        this.tileMirror.blockMetadata = 7;
        if (tile.getWorldObj() instanceof FakeWorld) {
            EntityPlayer p = Minecraft.getMinecraft().thePlayer;
            this.tileMirror.xCoord = (int) p.posX;
            this.tileMirror.yCoord = (int) p.posY;
            this.tileMirror.zCoord = (int) p.posZ;
        } else {
            this.tileMirror.xCoord = tile.xCoord;
            this.tileMirror.yCoord = tile.yCoord;
            this.tileMirror.zCoord = tile.zCoord;
        }
    }
}
