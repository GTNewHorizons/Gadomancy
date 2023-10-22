package makeo.gadomancy.client.effect.fx;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import makeo.gadomancy.client.effect.EffectHandler;
import makeo.gadomancy.common.data.config.ModConfig;
import makeo.gadomancy.common.utils.MiscUtils;
import makeo.gadomancy.common.utils.SimpleResourceLocation;
import makeo.gadomancy.common.utils.Vector3;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p/>
 * Created by HellFirePvP @ 17.11.2015 18:45
 */
public class FXFlow {

    private Color color;
    private Color fadingColor;

    private int policyCounter;
    private EntityFXFlowPolicy policy;

    private Vector3 target;

    private int livingTicks = -1;

    private World origin;
    private double motionBufferX, motionBufferY, motionBufferZ;
    private double lastTickPosX, lastTickPosY, lastTickPosZ;
    private double posX, posY, posZ;
    private float motionMultiplier = 1;
    private float mainParticleSize = 0.2F;
    private float surroundingParticleSize = 0.1F;
    private float surroundingDistance = 0.8F;
    private double unmodMotionBufX, unmodMotionBufY, unmodMotionBufZ;
    public long lastUpdateCall = System.currentTimeMillis();

    public FXFlow(World originWorld) {
        this.origin = originWorld;
        this.policy = EntityFXFlowPolicy.Policies.DEFAULT.getPolicy();
    }

    public FXFlow applyTarget(Vector3 target) {
        this.target = target;
        return this;
    }

    public FXFlow setSurroundingDistance(float surroundingDistance) {
        this.surroundingDistance = (1 / surroundingDistance);
        return this;
    }

    public FXFlow setMainParticleSize(float newSize) {
        this.mainParticleSize = newSize;
        return this;
    }

    public FXFlow setSurroundingParticleSize(float newSize) {
        this.surroundingParticleSize = newSize;
        return this;
    }

    public FXFlow setLivingTicks(int livingTicks) {
        this.livingTicks = livingTicks;
        return this;
    }

    public FXFlow setColor(Color newColor) {
        this.fadingColor = this.color;
        this.color = newColor;
        return this;
    }

    public FXFlow setPosition(double x, double y, double z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        return this;
    }

    public FXFlow setPosition(Vector3 vec) {
        return this.setPosition(vec.getX(), vec.getY(), vec.getZ());
    }

    public void setMotionMultiplier(float motionMultiplier) {
        this.motionMultiplier = motionMultiplier;
    }

    public FXFlow setMotion(double motionX, double motionY, double motionZ) {
        this.unmodMotionBufX = motionX;
        this.unmodMotionBufY = motionY;
        this.unmodMotionBufZ = motionZ;
        motionX *= this.motionMultiplier;
        motionY *= this.motionMultiplier;
        motionZ *= this.motionMultiplier;
        this.motionBufferX = motionX;
        this.motionBufferY = motionY;
        this.motionBufferZ = motionZ;
        return this;
    }

    public void tick() {

        this.livingTicks--;
        if (this.livingTicks <= 0) {
            EffectHandler.getInstance().unregisterFlow(this);
            return;
        }

        this.calculateVelocity();

        this.lastTickPosX = this.posX;
        this.lastTickPosY = this.posY;
        this.lastTickPosZ = this.posZ;
        this.posX += this.motionBufferX;
        this.posY += this.motionBufferY;
        this.posZ += this.motionBufferZ;

        this.policyCounter += 1;
        this.doParticles();
    }

    private void calculateVelocity() {
        if (this.target == null) return;
        if (this.livingTicks <= 0) return; // Never happens..

        Vector3 pos = this.getPositionVector();
        double motDirX = this.target.getX() - pos.getX();
        double motDirY = this.target.getY() - pos.getY();
        double motDirZ = this.target.getZ() - pos.getZ();
        Vector3 mot = new Vector3(motDirX, motDirY, motDirZ).divide(this.livingTicks);
        this.setMotion(mot.getX(), mot.getY(), mot.getZ());
    }

    private Vector3 getPositionVector() {
        return new Vector3(this.posX, this.posY, this.posZ);
    }

    public void setPolicy(EntityFXFlowPolicy policy) {
        this.policy = policy;
    }

    public void setPolicy(EntityFXFlowPolicy.Policies policy) {
        this.policy = policy.getPolicy();
    }

    private void doParticles() {
        if (Minecraft.getMinecraft().theWorld == null) {
            EffectHandler.getInstance().unregisterFlow(this);
            return;
        }
        if (Minecraft.getMinecraft().theWorld.provider.dimensionId != this.origin.provider.dimensionId) {
            EffectHandler.getInstance().unregisterFlow(this);
            return;
        }

        FXFlowBase flow = new FXFlowBase(
                this.origin,
                this.posX,
                this.posY,
                this.posZ,
                this.color,
                this.mainParticleSize,
                9,
                240);
        Minecraft.getMinecraft().effectRenderer.addEffect(flow); // Initial position.
        double lastPosX = this.posX - (this.posX - this.lastTickPosX) / 2.0D;
        double lastPosY = this.posY - (this.posY - this.lastTickPosY) / 2.0D;
        double lastPosZ = this.posZ - (this.posZ - this.lastTickPosZ) / 2.0D;
        FXFlowBase flow2 = new FXFlowBase(
                this.origin,
                lastPosX,
                lastPosY,
                lastPosZ,
                this.color,
                (float) (this.mainParticleSize * 0.8),
                8,
                240);
        Minecraft.getMinecraft().effectRenderer.addEffect(flow2); // Consistency to last position

        if (this.policy != null) this.policy.doSubParticles(
                this,
                this.policyCounter,
                this.posX,
                this.posY,
                this.posZ,
                lastPosX,
                lastPosY,
                lastPosZ);
    }

    public Color getColor() {
        return this.color;
    }

    public Color getFadingColor() {
        return this.fadingColor;
    }

    public float getSurroundingDistance() {
        return this.surroundingDistance;
    }

    public float getSurroundingParticleSize() {
        return this.surroundingParticleSize;
    }

    public float getMainParticleSize() {
        return this.mainParticleSize;
    }

    public float getMotionMultiplier() {
        return this.motionMultiplier;
    }

    public boolean isMovementVectorNullLength() {
        return new Vector3(this.motionBufferX, this.motionBufferY, this.motionBufferZ).lengthSquared() == 0;
    }

    public Vector3 getTarget() {
        return this.target;
    }

    public World getOriginWorld() {
        return this.origin;
    }

    public Vector3 getMovementVector() {
        if (this.isMovementVectorNullLength())
            return new Vector3(this.unmodMotionBufX, this.unmodMotionBufY, this.unmodMotionBufZ);
        return new Vector3(this.motionBufferX, this.motionBufferY, this.motionBufferZ);
    }

    public Random getRand() {
        return this.origin.rand;
    }

    public FXFlow applyProperties(EntityFlowProperties properties) {
        if (properties != null) {
            if (properties.hasTarget) {
                this.applyTarget(properties.target);
            }
            if (properties.color != null) {
                if (properties.fading != null) {
                    this.setColor(properties.fading);
                    this.setColor(properties.color);
                } else {
                    this.setColor(properties.color);
                }
            } else {
                if (properties.fading != null) {
                    this.setColor(properties.fading);
                }
            }
            if (properties.livingTicks != -1) this.setLivingTicks(properties.livingTicks);
            if (properties.mainParticleSize != -1) this.setMainParticleSize(properties.mainParticleSize);
            if (properties.surroundingDistance != -1) this.setSurroundingDistance(properties.surroundingDistance);
            if (properties.surroundingParticleSize != -1)
                this.setSurroundingParticleSize(properties.surroundingParticleSize);
            if (properties.motionMultiplier != Float.MAX_VALUE) {
                this.setMotionMultiplier(properties.motionMultiplier);
            }
            if (properties.policy != null) {
                this.setPolicy(properties.policy);
            }
        }
        return this;
    }

    public static void tickFlows(Iterable<FXFlow> fxFlows) {
        for (FXFlow flow : fxFlows) {
            if ((System.currentTimeMillis() - flow.lastUpdateCall) > 1000L) {
                EffectHandler.getInstance().unregisterFlow(flow);
            }
            flow.tick();
        }
    }

    public static class EntityFlowProperties {

        protected boolean hasTarget;
        protected Vector3 target;
        protected Color color, fading;
        protected EntityFXFlowPolicy policy;
        protected float mainParticleSize = -1, surroundingParticleSize = -1, surroundingDistance = -1;
        protected int livingTicks = -1;
        protected float motionMultiplier = Float.MAX_VALUE;

        public EntityFlowProperties() {}

        public EntityFlowProperties setTarget(Vector3 target) {
            this.hasTarget = target != null;
            this.target = target;
            return this;
        }

        public EntityFlowProperties setPolicy(EntityFXFlowPolicy policy) {
            this.policy = policy;
            return this;
        }

        public EntityFlowProperties setPolicy(EntityFXFlowPolicy.Policies policy) {
            this.policy = policy.getPolicy();
            return this;
        }

        public EntityFlowProperties setColor(Color color) {
            this.color = color;
            return this;
        }

        public EntityFlowProperties setMotionMultiplier(float motionMultiplier) {
            this.motionMultiplier = motionMultiplier;
            return this;
        }

        public EntityFlowProperties setFading(Color fading) {
            this.fading = fading;
            return this;
        }

        public EntityFlowProperties setMainParticleSize(float mainParticleSize) {
            this.mainParticleSize = mainParticleSize;
            return this;
        }

        public EntityFlowProperties setSurroundingParticleSize(float surroundingParticleSize) {
            this.surroundingParticleSize = surroundingParticleSize;
            return this;
        }

        public EntityFlowProperties setSurroundingDistance(float surroundingDistance) {
            this.surroundingDistance = surroundingDistance;
            return this;
        }

        public EntityFlowProperties setLivingTicks(int livingTicks) {
            this.livingTicks = livingTicks;
            return this;
        }
    }

    public static class FXFlowBase extends EntityFX {

        private static Queue<FXFlowBase> fxQueue = new ArrayDeque<FXFlowBase>();

        private float partBlue, partGreen, partRed, partAlpha;

        // Queue variables
        private float partialTicks;
        private float rendArg1, rendArg2, rendArg3, rendArg4, rendArg5;
        private int rendBrightness;

        private ResourceLocation texture = new SimpleResourceLocation("fx/flow_large.png");
        private float buffHalfLife, buffParticleScale;

        public FXFlowBase(World world, double x, double y, double z, Color color, float size, int multiplier,
                int brightness) {
            super(world, x, y, z);
            if (color != null) {
                this.partBlue = color.getBlue() / 255F;
                this.partGreen = color.getGreen() / 255F;
                this.partRed = color.getRed() / 255F;
                this.partAlpha = color.getAlpha() / 255F;
            }
            this.motionX = this.motionY = this.motionZ = 0;
            this.particleScale *= size;
            this.particleMaxAge = 3 * multiplier;
            this.noClip = false;
            this.setSize(0.01F, 0.01F);
            this.particleAge = 0;
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            this.particleTextureIndexX = 0;
            this.particleTextureIndexY = 0;
            this.buffHalfLife = (this.particleMaxAge / 2);
            this.buffParticleScale = this.particleScale;
            this.rendBrightness = brightness;
            this.noClip = true;
        }

        public static void sheduleRender(Tessellator tessellator) {
            boolean isLightingEnabled = GL11.glGetBoolean(GL11.GL_LIGHTING);

            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.003921569F);
            if (isLightingEnabled) GL11.glDisable(GL11.GL_LIGHTING);

            GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.75F);

            for (FXFlowBase fx : FXFlowBase.fxQueue) {
                tessellator.startDrawingQuads();
                fx.renderEssenceBase(tessellator);
                tessellator.draw();
            }

            GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.75F);

            if (isLightingEnabled) GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDepthMask(true);

            FXFlowBase.fxQueue.clear();
        }

        private void renderEssenceBase(Tessellator tessellator) {
            tessellator.setBrightness(this.rendBrightness);
            GL11.glColor4f(this.partRed, this.partGreen, this.partBlue, this.partAlpha);
            Minecraft.getMinecraft().renderEngine.bindTexture(this.texture);
            if (this.particleAge >= this.particleMaxAge) return;
            float agescale = (float) this.particleAge / this.buffHalfLife;
            if (agescale >= 1.0F) agescale = 2 - agescale;
            this.particleScale = this.buffParticleScale * agescale;
            float f10 = 0.5F * this.particleScale;
            float f11 = (float) (this.prevPosX + (this.posX - this.prevPosX) * this.partialTicks - EntityFX.interpPosX);
            float f12 = (float) (this.prevPosY + (this.posY - this.prevPosY) * this.partialTicks - EntityFX.interpPosY);
            float f13 = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * this.partialTicks - EntityFX.interpPosZ);

            tessellator.addVertexWithUV(
                    f11 - this.rendArg1 * f10 - this.rendArg4 * f10,
                    f12 - this.rendArg2 * f10,
                    f13 - this.rendArg3 * f10 - this.rendArg5 * f10,
                    0,
                    1);
            tessellator.addVertexWithUV(
                    f11 - this.rendArg1 * f10 + this.rendArg4 * f10,
                    f12 + this.rendArg2 * f10,
                    f13 - this.rendArg3 * f10 + this.rendArg5 * f10,
                    1,
                    1);
            tessellator.addVertexWithUV(
                    f11 + this.rendArg1 * f10 + this.rendArg4 * f10,
                    f12 + this.rendArg2 * f10,
                    f13 + this.rendArg3 * f10 + this.rendArg5 * f10,
                    1,
                    0);
            tessellator.addVertexWithUV(
                    f11 + this.rendArg1 * f10 - this.rendArg4 * f10,
                    f12 - this.rendArg2 * f10,
                    f13 + this.rendArg3 * f10 - this.rendArg5 * f10,
                    0,
                    0);
        }

        @Override
        public void renderParticle(Tessellator tessellator, float partialTicks, float par3, float par4, float par5,
                float par6, float par7) {
            if (MiscUtils.getPositionVector(Minecraft.getMinecraft().renderViewEntity)
                    .distance(new Vector3(this.posX, this.posY, this.posZ)) > ModConfig.renderParticleDistance)
                return;

            this.partialTicks = partialTicks;
            this.rendArg1 = par3;
            this.rendArg2 = par4;
            this.rendArg3 = par5;
            this.rendArg4 = par6;
            this.rendArg5 = par7;

            FXFlowBase.fxQueue.add(this);
        }
    }
}
