package makeo.gadomancy.client.gui;

import java.awt.Color;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import makeo.gadomancy.common.blocks.tiles.TileArcanePackager;
import makeo.gadomancy.common.containers.ContainerArcanePackager;
import makeo.gadomancy.common.utils.ColorHelper;
import makeo.gadomancy.common.utils.SimpleResourceLocation;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p/>
 * Created by makeo @ 14.11.2015 01:17
 */
public class ArcanePackagerGui extends GuiContainer {

    private static final ResourceLocation TEXTURE = new SimpleResourceLocation("gui/gui_packager.png");

    private final TileArcanePackager tile;
    private final InventoryPlayer playerInv;

    public ArcanePackagerGui(InventoryPlayer playerInv, IInventory inventory) {
        super(new ContainerArcanePackager(playerInv, inventory));

        this.tile = (TileArcanePackager) inventory;
        this.playerInv = playerInv;

        this.ySize = 234;
        this.xSize = 190;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int p_146979_1_, int p_146979_2_) {
        if (this.tile.isInvalid()) {
            Minecraft.getMinecraft().displayGuiScreen(null);
            return;
        }

        GL11.glEnable(GL11.GL_BLEND);
        this.mc.renderEngine.bindTexture(ArcanePackagerGui.TEXTURE);

        GL11.glColor3f(1, 1, 1);

        if (this.tile.progress > 0) {
            this.drawTexturedModalRect(89, 67, 210, 0, this.tile.progress, 9);
        }

        if (this.tile.useEssentia) {
            this.drawTexturedModalRect(91, 97, 249, 10, 8, 8);
        }

        if (this.tile.autoStart) {
            this.drawTexturedModalRect(91, 110, 249, 10, 8, 8);
        }

        if (this.tile.disguise) {
            this.drawTexturedModalRect(91, 123, 249, 10, 8, 8);
        }

        GL11.glDisable(GL11.GL_BLEND);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float mouseX, int mouseZ, int par3) {
        this.mc.renderEngine.bindTexture(ArcanePackagerGui.TEXTURE);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_BLEND);
        int var5 = (this.width - this.xSize) / 2;
        int var6 = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(var5, var6, 0, 0, this.xSize, this.ySize);

        this.drawString(
                100,
                98,
                "gadomancy.info.ArcanePackager.useEssentia",
                this.tile.useEssentia ? Color.WHITE : Color.GRAY);

        this.drawString(
                100,
                111,
                "gadomancy.info.ArcanePackager.autoStart",
                this.tile.autoStart ? Color.WHITE : Color.GRAY);

        this.drawString(
                100,
                124,
                "gadomancy.info.ArcanePackager.disguisePackage",
                this.tile.disguise ? Color.WHITE : Color.GRAY);

        GL11.glDisable(GL11.GL_BLEND);
    }

    private void drawString(int x, int y, String unlocalized, Color color) {
        GL11.glPushMatrix();
        GL11.glTranslatef(this.guiLeft + x, this.guiTop + y, 0);
        GL11.glScalef(0.5F, 0.5F, 0.0F);
        this.fontRendererObj.drawString(StatCollector.translateToLocal(unlocalized), 0, 0, ColorHelper.toHex(color));
        GL11.glPopMatrix();
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button);

        int baseX = (this.width - this.xSize) / 2;
        int baseY = (this.height - this.ySize) / 2;

        int checkX = x - baseX - 90;
        int checkY = y - baseY - 97;

        if (checkX >= 0 && checkX < 8 && checkY >= 0 && checkY < 8) {
            this.tile.useEssentia = !this.tile.useEssentia;
            this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, this.tile.useEssentia ? 0 : 1);
        }

        checkY = y - baseY - 110;

        if (checkX >= 0 && checkX < 8 && checkY >= 0 && checkY < 8) {
            this.tile.autoStart = !this.tile.autoStart;
            this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, this.tile.autoStart ? 2 : 3);
        }

        checkY = y - baseY - 123;

        if (checkX >= 0 && checkX < 8 && checkY >= 0 && checkY < 8) {
            this.tile.disguise = !this.tile.disguise;
            this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, this.tile.disguise ? 4 : 5);
        }
    }
}
