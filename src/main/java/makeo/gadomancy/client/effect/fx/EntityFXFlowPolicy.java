package makeo.gadomancy.client.effect.fx;

import java.awt.*;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

import makeo.gadomancy.common.utils.Vector3;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p/>
 * Created by HellFirePvP @ 17.11.2015 18:42
 */
public abstract class EntityFXFlowPolicy {

    public abstract void doSubParticles(FXFlow fxFlow, int policyCounter, double posX, double posY, double posZ,
            double lastPosX, double lastPosY, double lastPosZ);

    public enum Policies {

        NO_OP(null),
        CIRCLE_MIX(new CircularMixPolicy()),
        CIRCLE(new CircularPolicy()),
        DEFAULT(new DefaultPolicy());

        private EntityFXFlowPolicy policy;

        Policies(EntityFXFlowPolicy policy) {
            this.policy = policy;
        }

        public EntityFXFlowPolicy getPolicy() {
            return this.policy;
        }
    }

    static class DefaultPolicy extends EntityFXFlowPolicy {

        @Override
        public void doSubParticles(FXFlow fxFlow, int policyCounter, double posX, double posY, double posZ,
                double lastPosX, double lastPosY, double lastPosZ) {
            this.doParticles(fxFlow, policyCounter, posX, posY, posZ, fxFlow.getRand().nextInt(3) + 1);
        }

        public void doParticles(FXFlow fxFlow, int policyCounter, double posX, double posY, double posZ, int count) {
            Random rand = fxFlow.getOriginWorld().rand;
            for (int i = 0; i < count; i++) {
                Vector3 subOffset = this.genSubOffset(rand, fxFlow.getSurroundingDistance());
                Color c = (fxFlow.getFadingColor() != null && rand.nextBoolean()) ? fxFlow.getFadingColor()
                        : fxFlow.getColor();
                FXFlow.FXFlowBase flow = new FXFlow.FXFlowBase(
                        fxFlow.getOriginWorld(),
                        posX + subOffset.getX(),
                        posY + subOffset.getY(),
                        posZ + subOffset.getZ(),
                        c,
                        rand.nextInt(1) + fxFlow.getSurroundingParticleSize(),
                        6,
                        240);
                Minecraft.getMinecraft().effectRenderer.addEffect(flow);
            }
        }

        private Vector3 genSubOffset(Random rand, float surroundingDistance) {
            float x = ((rand.nextFloat() / 4F) * surroundingDistance) * (rand.nextBoolean() ? 1 : -1);
            float y = ((rand.nextFloat() / 4F) * surroundingDistance) * (rand.nextBoolean() ? 1 : -1);
            float z = ((rand.nextFloat() / 4F) * surroundingDistance) * (rand.nextBoolean() ? 1 : -1);
            return new Vector3(x, y, z);
        }
    }

    static class CircularPolicy extends EntityFXFlowPolicy {

        public static final int TICKS_PER_FULL_TURN = 40;

        @Override
        public void doSubParticles(FXFlow fxFlow, int policyCounter, double posX, double posY, double posZ,
                double lastPosX, double lastPosY, double lastPosZ) {
            Vector3 rotationAxis = fxFlow.getMovementVector();

            Vector3 perpendicular = rotationAxis.clone().perpendicular().normalize()
                    .multiply(fxFlow.getSurroundingDistance());
            Vector3 counterSide = perpendicular.clone().rotate(Math.toRadians(180), rotationAxis);

            int currentDividedPolicyTick = policyCounter % CircularPolicy.TICKS_PER_FULL_TURN;
            float currentDegree = 360F
                    * (((float) currentDividedPolicyTick) / ((float) CircularPolicy.TICKS_PER_FULL_TURN));
            double currentRad = Math.toRadians(currentDegree);

            perpendicular = perpendicular.rotate(currentRad, rotationAxis);
            counterSide = counterSide.rotate(currentRad, rotationAxis);

            World w = fxFlow.getOriginWorld();
            Color c = (fxFlow.getFadingColor() != null) ? fxFlow.getFadingColor() : fxFlow.getColor();
            FXFlow.FXFlowBase flow = new FXFlow.FXFlowBase(
                    w,
                    posX + perpendicular.getX(),
                    posY + perpendicular.getY(),
                    posZ + perpendicular.getZ(),
                    c,
                    fxFlow.getSurroundingParticleSize(),
                    6,
                    240);
            Minecraft.getMinecraft().effectRenderer.addEffect(flow);
            FXFlow.FXFlowBase flow2 = new FXFlow.FXFlowBase(
                    w,
                    posX + counterSide.getX(),
                    posY + counterSide.getY(),
                    posZ + counterSide.getZ(),
                    c,
                    fxFlow.getSurroundingParticleSize(),
                    6,
                    240);
            Minecraft.getMinecraft().effectRenderer.addEffect(flow2);
        }
    }

    static class CircularMixPolicy extends CircularPolicy {

        @Override
        public void doSubParticles(FXFlow fxFlow, int policyCounter, double posX, double posY, double posZ,
                double lastPosX, double lastPosY, double lastPosZ) {
            super.doSubParticles(fxFlow, policyCounter, posX, posY, posZ, lastPosX, lastPosY, lastPosZ);

            ((DefaultPolicy) Policies.DEFAULT.getPolicy())
                    .doParticles(fxFlow, policyCounter, posX, posY, posZ, fxFlow.getRand().nextInt(2));
        }
    }
}
