package makeo.gadomancy.client.effect.fx;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

import makeo.gadomancy.client.effect.EffectHandler;
import makeo.gadomancy.common.data.config.ModConfig;
import makeo.gadomancy.common.utils.MiscUtils;
import makeo.gadomancy.common.utils.Vector3;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by HellFirePvP @ 17.11.2015 18:41
 */
public final class Orbital {

    private Vector3 center;
    private final World world;
    private int orbitalCounter;
    public boolean registered;
    // INFO: Needs to be updated when its "owner" gets rendered.
    public long lastRenderCall = System.currentTimeMillis();

    private List<OrbitalRenderProperties> orbitals = new ArrayList<OrbitalRenderProperties>();

    public Orbital(Vector3 center, World world) {
        this.center = center;
        this.world = world;
    }

    public void updateCenter(Vector3 center) {
        this.center = center;
    }

    public void addOrbitalPoint(OrbitalRenderProperties properties) {
        if (!this.orbitals.contains(properties)) {
            this.orbitals.add(properties);
        }
    }

    public int orbitalsSize() {
        return this.orbitals.size();
    }

    public void clearOrbitals() {
        this.orbitals.clear();
    }

    public void doRender(float partialTicks) {
        if (MiscUtils.getPositionVector(Minecraft.getMinecraft().renderViewEntity).distance(this.center)
                > ModConfig.renderParticleDistance)
            return;
        if (Minecraft.getMinecraft().isGamePaused()) return;

        for (OrbitalRenderProperties orbitalNode : this.orbitals) {
            Axis axis = orbitalNode.getAxis();
            int counterOffset = orbitalNode.getOffsetTicks() % orbitalNode.getTicksForFullCircle();

            int currentDividedPolicyTick = (this.orbitalCounter + counterOffset) % orbitalNode.getTicksForFullCircle();
            float currentDegree = 360F
                    * (((float) currentDividedPolicyTick) / ((float) orbitalNode.getTicksForFullCircle()));
            double currentRad = Math.toRadians(currentDegree);

            Vector3 point = axis.getAxis().clone().perpendicular().normalize().multiply(orbitalNode.getOffset())
                    .rotate(currentRad, axis.getAxis()).add(this.center);

            if (orbitalNode.getRunnable() != null) {
                orbitalNode.getRunnable().onRender(this.world, point, orbitalNode, this.orbitalCounter, partialTicks);
            }

            if (orbitalNode.getParticleSize() <= 0) continue;

            FXFlow.FXFlowBase flow = new FXFlow.FXFlowBase(
                    this.world,
                    point.getX(),
                    point.getY(),
                    point.getZ(),
                    orbitalNode.getColor(),
                    orbitalNode.getParticleSize(),
                    orbitalNode.getMultiplier(),
                    orbitalNode.getBrightness());

            if (orbitalNode.getSubParticleColor() != null && this.world.rand.nextInt(3) == 0) {
                Vector3 subOffset = this.genSubOffset(this.world.rand, 0.8F);
                Color c = (this.world.rand.nextBoolean()) ? orbitalNode.getSubParticleColor() : orbitalNode.getColor();
                FXFlow.FXFlowBase flow2 = new FXFlow.FXFlowBase(
                        this.world,
                        point.getX() + subOffset.getX(),
                        point.getY() + subOffset.getY(),
                        point.getZ() + subOffset.getZ(),
                        c,
                        orbitalNode.getSubSizeRunnable().getSubParticleSize(this.world.rand, this.orbitalCounter),
                        6,
                        240);

                Minecraft.getMinecraft().effectRenderer.addEffect(flow2);
            }

            Minecraft.getMinecraft().effectRenderer.addEffect(flow);
        }
    }

    private Vector3 genSubOffset(Random rand, float surroundingDistance) {
        float x = ((rand.nextFloat() / 4F) * surroundingDistance) * (rand.nextBoolean() ? 1 : -1);
        float y = ((rand.nextFloat() / 4F) * surroundingDistance) * (rand.nextBoolean() ? 1 : -1);
        float z = ((rand.nextFloat() / 4F) * surroundingDistance) * (rand.nextBoolean() ? 1 : -1);
        return new Vector3(x, y, z);
    }

    public void reduceAllOffsets(float percent) {
        for (OrbitalRenderProperties node : this.orbitals) {
            node.reduceOffset(percent);
        }
    }

    public Vector3[] getOrbitalStartPoints(OrbitalRenderProperties... properties) {
        Vector3[] arr = new Vector3[properties.length];
        for (int i = 0; i < properties.length; i++) {
            OrbitalRenderProperties property = properties[i];
            if (property == null) {
                arr[i] = null;
                continue;
            }

            Axis axis = property.getAxis();
            int counterOffset = property.getOffsetTicks() % property.getTicksForFullCircle();

            int currentDividedPolicyTick = (this.orbitalCounter + counterOffset) % property.getTicksForFullCircle();
            float currentDegree = 360F
                    * (((float) currentDividedPolicyTick) / ((float) property.getTicksForFullCircle()));
            double currentRad = Math.toRadians(currentDegree);

            arr[i] = axis.getAxis().clone().perpendicular().normalize().multiply(property.getOffset())
                    .rotate(currentRad, axis.getAxis()).add(this.center);
        }
        return arr;
    }

    public static void sheduleRenders(Iterable<Orbital> orbitals, float partialTicks) {
        for (Orbital orbital : orbitals) {
            orbital.doRender(partialTicks);
        }
    }

    public static void tickOrbitals(Iterable<Orbital> orbitals) {
        for (Orbital orbital : orbitals) {
            if ((System.currentTimeMillis() - orbital.lastRenderCall) > 1000L) {
                orbital.clearOrbitals();
                EffectHandler.getInstance().unregisterOrbital(orbital);
            } else {
                orbital.orbitalCounter++;
            }
        }
    }

    public static class OrbitalRenderProperties {

        private static final OrbitalSubSizeRunnable subSizeRunnableStatic = new OrbitalSubSizeRunnable() {

            @Override
            public float getSubParticleSize(Random rand, int orbitalExisted) {
                return 0.1F + (rand.nextBoolean() ? 0.0F : 0.1F);
            }
        };

        private Axis axis;
        private double originalOffset, offset;
        private Color color = Color.WHITE;
        private int ticksForFullCircle = 40;
        private OrbitalRenderRunnable runnable;
        private int multiplier = 8;
        private int brightness = 240;
        private float particleSize = 0.2F;
        private int offsetTicks;

        private Color subParticleColor;
        private OrbitalSubSizeRunnable subSizeRunnable;

        public OrbitalRenderProperties(Axis axis, double offsetLength) {
            this.offset = this.originalOffset = offsetLength;
            this.axis = axis;
            this.subSizeRunnable = OrbitalRenderProperties.subSizeRunnableStatic;
        }

        public OrbitalRenderProperties setColor(Color color) {
            this.color = color;
            return this;
        }

        public OrbitalRenderProperties setSubSizeRunnable(OrbitalSubSizeRunnable subSizeRunnable) {
            if (subSizeRunnable == null) return this;
            this.subSizeRunnable = subSizeRunnable;
            return this;
        }

        public OrbitalRenderProperties setTicksForFullCircle(int ticks) {
            this.ticksForFullCircle = ticks;
            return this;
        }

        public OrbitalRenderProperties setRenderRunnable(OrbitalRenderRunnable runnable) {
            this.runnable = runnable;
            return this;
        }

        public OrbitalRenderProperties setBrightness(int brightness) {
            this.brightness = brightness;
            return this;
        }

        public OrbitalRenderProperties setMultiplier(int multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        public OrbitalRenderProperties setOffsetTicks(int offsetTicks) {
            this.offsetTicks = offsetTicks;
            return this;
        }

        public OrbitalRenderProperties setParticleSize(float particleSize) {
            this.particleSize = particleSize;
            return this;
        }

        public OrbitalRenderProperties setSubParticleColor(Color subParticleColor) {
            this.subParticleColor = subParticleColor;
            return this;
        }

        // Percent from 0.0F to 1.0F
        public void reduceOffset(float percent) {
            this.offset = this.originalOffset * percent;
        }

        public float getParticleSize() {
            return this.particleSize;
        }

        public Color getColor() {
            return this.color;
        }

        public Axis getAxis() {
            return this.axis;
        }

        public double getOffset() {
            return this.offset;
        }

        public Color getSubParticleColor() {
            return this.subParticleColor;
        }

        public int getTicksForFullCircle() {
            return this.ticksForFullCircle;
        }

        public OrbitalRenderRunnable getRunnable() {
            return this.runnable;
        }

        public OrbitalSubSizeRunnable getSubSizeRunnable() {
            return this.subSizeRunnable;
        }

        public int getMultiplier() {
            return this.multiplier;
        }

        public int getBrightness() {
            return this.brightness;
        }

        public int getOffsetTicks() {
            return this.offsetTicks;
        }
    }

    public abstract static class OrbitalSubSizeRunnable {

        public abstract float getSubParticleSize(Random rand, int orbitalExisted);
    }

    public abstract static class OrbitalRenderRunnable {

        public abstract void onRender(World world, Vector3 selectedPosition, OrbitalRenderProperties properties,
                int orbitalExisted, float partialTicks);
    }

    public static class Axis {

        public static final Axis X_AXIS = new Axis(new Vector3(1, 0, 0));
        public static final Axis Y_AXIS = new Axis(new Vector3(0, 1, 0));
        public static final Axis Z_AXIS = new Axis(new Vector3(0, 0, 1));

        private Vector3 axis;

        public Axis(Vector3 axis) {
            this.axis = axis;
        }

        public static Axis persisentRandomAxis() {
            // Actually quite important to use only Y-positive here since if we
            // would use negative y, the axis may turn counter-
            // clockwise, what's intended to be set in another variable, which
            // may cause bugs if we want to use only clockwise axis'.
            return new Axis(Vector3.positiveYRandom());
        }

        public Vector3 getAxis() {
            return this.axis.clone();
        }
    }
}
