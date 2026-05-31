package makeo.gadomancy.client.util;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;

import org.lwjgl.opengl.GL11;

import com.github.bsideup.jabel.Desugar;

import makeo.gadomancy.client.renderers.tile.RenderTileNodeBasic;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.client.lib.UtilsFX;

public class NodeRenderQueue {

    public static final List<QueuedNode> nodeQueue = new ArrayList<>();
    public static final List<QueuedDrainBeam> drainQueue = new ArrayList<>();

    @Desugar
    public record QueuedNode(double x, double y, double z, double viewDistance, boolean visible, boolean depthIgnore,
            float size, AspectList aspects, NodeType type, NodeModifier mod) {}

    @Desugar
    public record QueuedDrainBeam(double fromX, double fromY, double fromZ, double toX, double toY, double toZ,
            int color, float alpha, float partialTicks) {}

    public static void flush(float partialTicks) {
        EntityLivingBase viewer = Minecraft.getMinecraft().renderViewEntity;

        if (!nodeQueue.isEmpty()) {
            GL11.glPushMatrix();
            for (QueuedNode qn : nodeQueue) {
                RenderTileNodeBasic.renderNode(
                        viewer,
                        qn.viewDistance(),
                        qn.visible(),
                        qn.depthIgnore(),
                        qn.size(),
                        qn.x(),
                        qn.y(),
                        qn.z(),
                        partialTicks,
                        qn.aspects(),
                        qn.type(),
                        qn.mod());
            }
            GL11.glPopMatrix();
            nodeQueue.clear();
        }

        if (!drainQueue.isEmpty()) {
            GL11.glPushMatrix();
            for (QueuedDrainBeam qdb : drainQueue) {
                UtilsFX.drawFloatyLine(
                        qdb.fromX(),
                        qdb.fromY(),
                        qdb.fromZ(),
                        qdb.toX(),
                        qdb.toY(),
                        qdb.toZ(),
                        qdb.partialTicks(),
                        qdb.color(),
                        "textures/misc/wispy.png",
                        -0.02F,
                        qdb.alpha());
            }
            GL11.glPopMatrix();
            drainQueue.clear();
        }
    }
}
