package makeo.gadomancy.common.utils;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import cpw.mods.fml.common.network.NetworkRegistry;
import makeo.gadomancy.common.blocks.tiles.TileExtendedNode;
import makeo.gadomancy.common.network.PacketHandler;
import makeo.gadomancy.common.network.packets.PacketAnimationAbsorb;
import makeo.gadomancy.common.network.packets.PacketStartAnimation;
import makeo.gadomancy.common.network.packets.PacketTCNodeBolt;
import makeo.gadomancy.common.registration.RegisteredBlocks;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.Config;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.utils.Utils;
import thaumcraft.common.lib.world.ThaumcraftWorldGenerator;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by HellFirePvP @ 25.10.2015 10:17
 */
public class ExplosionHelper {

    public static void taintplosion(World world, int x, int y, int z, boolean taintBiome, int chanceToTaint) {
        ExplosionHelper.taintplosion(world, x, y, z, taintBiome, chanceToTaint, 3.0F, 10, 80);
    }

    public static void taintplosion(World world, int x, int y, int z, boolean taintBiome, int chanceToTaint, float str,
            int size, int blocksAffected) {
        if (chanceToTaint < 1) chanceToTaint = 1;
        world.createExplosion(null, x + 0.5D, y + 0.5D, z + 0.5D, str, false);
        for (int a = 0; a < blocksAffected; a++) {
            int xx = x + world.rand.nextInt(size) - world.rand.nextInt(size);
            int yy = y + world.rand.nextInt(size) - world.rand.nextInt(size);
            int zz = z + world.rand.nextInt(size) - world.rand.nextInt(size);
            if (world.isAirBlock(xx, yy, zz)) {
                if (yy < y) {
                    world.setBlock(xx, yy, zz, ConfigBlocks.blockFluxGoo, 8, 3);
                } else {
                    world.setBlock(xx, yy, zz, ConfigBlocks.blockFluxGas, 8, 3);
                }
            }
            if (!Config.genTaint) continue;

            if (taintBiome && world.rand.nextInt(chanceToTaint) == 0) {
                Utils.setBiomeAt(world, xx, zz, ThaumcraftWorldGenerator.biomeTaint);
            }
        }
    }

    public static class VortexExplosion {

        private TileExtendedNode causingNode;
        private int tick;
        private int phase;
        private World world;
        private int x, y, z;
        private List<Vec3> pastTickBlocks;

        public VortexExplosion(TileExtendedNode exNode) {
            this.causingNode = exNode;
            this.world = exNode.getWorldObj();
            this.x = exNode.xCoord;
            this.y = exNode.yCoord;
            this.z = exNode.zCoord;
            this.phase = 0;
            this.tick = 0;
        }

        public void update() {
            List livingEntities = this.world.getEntitiesWithinAABB(
                    EntityLivingBase.class,
                    AxisAlignedBB.getBoundingBox(this.x, this.y, this.z, this.x + 1, this.y + 1, this.z + 1)
                            .expand(6.0D, 6.0D, 6.0D));
            if ((livingEntities != null) && (livingEntities.size() > 0)) {
                for (Object e : livingEntities) {
                    EntityLivingBase livingEntity = (EntityLivingBase) e;
                    if ((livingEntity.isEntityAlive()) && (!livingEntity.isEntityInvulnerable())) {
                        if (livingEntity instanceof EntityPlayer
                                && ((EntityPlayer) livingEntity).capabilities.isCreativeMode)
                            continue;
                        if (this.world.rand.nextInt(16) != 0) continue;
                        livingEntity.attackEntityFrom(DamageSource.magic, 4F);
                        PacketTCNodeBolt packet = new PacketTCNodeBolt(
                                this.x + 0.5F,
                                this.y + 0.5F,
                                this.z + 0.5F,
                                (float) livingEntity.posX,
                                (float) (livingEntity.posY + livingEntity.height),
                                (float) livingEntity.posZ,
                                0,
                                false);
                        PacketHandler.INSTANCE.sendToAllAround(
                                packet,
                                new NetworkRegistry.TargetPoint(
                                        this.world.provider.dimensionId,
                                        this.x,
                                        this.y,
                                        this.z,
                                        32.0D));
                    }
                }
            }
            if (this.phase < 2 && this.world.rand.nextInt(this.phase == 0 ? 8 : 4) == 0) {
                PacketStartAnimation packet = new PacketStartAnimation(
                        PacketStartAnimation.ID_BURST,
                        this.x,
                        this.y,
                        this.z);
                PacketHandler.INSTANCE.sendToAllAround(
                        packet,
                        new NetworkRegistry.TargetPoint(
                                this.world.provider.dimensionId,
                                this.x,
                                this.y,
                                this.z,
                                32.0D));
                this.world.playSoundEffect(this.x + 0.5, this.y + 0.5, this.z + 0.5, "thaumcraft:ice", 0.8F, 1.0F);
            }
            switch (this.phase) {
                case 0: {
                    this.tick++;
                    if (this.tick > 200) {
                        this.tick = 0;
                        this.phase = 1;
                        ExplosionHelper.taintplosion(this.world, this.x, this.y, this.z, false, 1);
                    }

                    this.sendRandomVortexLightningPacket(this.world, this.x, this.y, this.z);
                    break;
                }
                case 1: {
                    this.tick++;
                    int range = this.tick > 50
                            ? this.tick > 75 ? this.tick > 100 ? this.tick > 200 ? this.tick > 300 ? 9 : 7 : 6 : 5 : 4
                            : 3;
                    if (this.pastTickBlocks != null) {
                        for (Vec3 v : this.pastTickBlocks) {
                            this.sendRandomVortexLightningPacket(this.world, this.x, this.y, this.z);
                            this.world.setBlockToAir((int) v.xCoord, (int) v.yCoord, (int) v.zCoord);
                        }
                    }
                    int ct = this.world.rand.nextInt(4);
                    while (ct > 0) {
                        ct--;
                        this.sendRandomVortexLightningPacket(this.world, this.x, this.y, this.z);
                    }
                    this.pastTickBlocks = new ArrayList<Vec3>();
                    int cnt = 10;
                    do {
                        int xx = this.x + this.world.rand.nextInt(range) - this.world.rand.nextInt(range);
                        int yy = this.y + this.world.rand.nextInt(range) - this.world.rand.nextInt(range);
                        int zz = this.z + this.world.rand.nextInt(range) - this.world.rand.nextInt(range);
                        Block b = this.world.getBlock(xx, yy, zz);
                        float hardness = b.getBlockHardness(this.world, xx, yy, zz);
                        if (b != Blocks.air && hardness > 0 && hardness <= 50 && b != RegisteredBlocks.blockNode) {
                            PacketAnimationAbsorb absorb = new PacketAnimationAbsorb(
                                    this.x,
                                    this.y,
                                    this.z,
                                    xx,
                                    yy,
                                    zz,
                                    7);
                            PacketHandler.INSTANCE.sendToAllAround(
                                    absorb,
                                    new NetworkRegistry.TargetPoint(
                                            this.world.provider.dimensionId,
                                            this.x,
                                            this.y,
                                            this.z,
                                            32D));
                            this.pastTickBlocks.add(Vec3.createVectorHelper(xx, yy, zz));
                        }
                        cnt--;
                    } while (cnt > 0);

                    if (this.tick == 200) {
                        ExplosionHelper.taintplosion(this.world, this.x, this.y, this.z, false, 1);
                    }

                    if (this.tick == 300) {
                        ExplosionHelper.taintplosion(this.world, this.x, this.y, this.z, false, 1);
                    }

                    if (this.tick > 400) {
                        this.phase = 2;
                        this.reduceAspects(this.causingNode);
                        ExplosionHelper.taintplosion(this.world, this.x, this.y, this.z, true, 3);
                    }
                    break;
                }
            }
        }

        private void reduceAspects(TileExtendedNode node) {
            List<Aspect> lostAspects = new ArrayList<Aspect>();
            AspectList list = node.getAspectsBase();
            for (Aspect a : list.getAspects()) {
                if (this.world.rand.nextInt(4) == 0) {
                    lostAspects.add(a);
                } else {
                    list.reduce(a, list.getAmount(a) / 2);
                }
            }
            for (Aspect lost : lostAspects) {
                node.getAspectsBase().remove(lost);
                node.getAspects().remove(lost);
            }
            list = node.getAspects();
            for (Aspect a : list.getAspects()) {
                list.reduce(a, list.getAmount(a) / 2);
            }
            node.getWorldObj().markBlockForUpdate(node.xCoord, node.yCoord, node.zCoord);
            node.markDirty();
        }

        private void sendRandomVortexLightningPacket(World world, int x, int y, int z) {
            PacketStartAnimation animationPacket = new PacketStartAnimation(PacketStartAnimation.ID_EX_VORTEX, x, y, z);
            NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(
                    world.provider.dimensionId,
                    x,
                    y,
                    z,
                    32.0D);
            PacketHandler.INSTANCE.sendToAllAround(animationPacket, point);
        }

        public static void vortexLightning(TileExtendedNode te) {
            World world = te.getWorldObj();
            int x = te.xCoord;
            int y = te.yCoord;
            int z = te.zCoord;
            float xx = x + world.rand.nextInt(3) - world.rand.nextInt(3);
            float yy = y + world.rand.nextInt(3) - world.rand.nextInt(3);
            float zz = z + world.rand.nextInt(3) - world.rand.nextInt(3);
            xx += world.rand.nextFloat() * (world.rand.nextBoolean() ? 1 : -1);
            yy += world.rand.nextFloat() * (world.rand.nextBoolean() ? 1 : -1);
            zz += world.rand.nextFloat() * (world.rand.nextBoolean() ? 1 : -1);
            Thaumcraft.proxy.nodeBolt(world, x + 0.5F, y + 0.5F, z + 0.5F, xx, yy, zz);
        }

        public boolean isFinished() {
            return this.phase > 1;
        }
    }
}
