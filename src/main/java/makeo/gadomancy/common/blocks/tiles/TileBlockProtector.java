package makeo.gadomancy.common.blocks.tiles;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.tiles.TileJarFillable;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p/>
 * Created by makeo @ 08.11.2015 17:36
 */
public class TileBlockProtector extends TileJarFillable {

    private static final int UPDATE_TICKS = 15;
    private static final int MAX_RANGE = 15;
    private static final Aspect ASPECT = Aspect.ORDER;

    private int range;
    private int saturation;
    private int count;

    public TileBlockProtector() {
        this.maxAmount = 8;
        this.aspectFilter = TileBlockProtector.ASPECT;
    }

    public int getCurrentRange() {
        return this.range;
    }

    public int getPowerLevel() {
        return Math.min(
                this.worldObj.getStrongestIndirectPower(this.xCoord, this.yCoord, this.zCoord),
                TileBlockProtector.MAX_RANGE);
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("ProtectSaturation", this.saturation);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.saturation = compound.getInteger("ProtectSaturation");
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        this.aspectFilter = TileBlockProtector.ASPECT;

        int oldRange = this.range;
        this.range = compound.getInteger("ProtectRange");

        if (this.worldObj != null && this.worldObj.isRemote && oldRange != this.range) {
            this.worldObj.updateLightByType(EnumSkyBlock.Block, this.xCoord, this.yCoord, this.zCoord);
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.removeTag("AspectFilter");
        compound.setInteger("ProtectRange", this.range);
    }

    @Override
    public void updateEntity() {
        if (!TileBlockProtector.protectors.contains(this)) {
            TileBlockProtector.protectors.add(this);
        }

        if (!this.worldObj.isRemote) {
            if (++this.count % 5 == 0 && this.amount < this.maxAmount) {
                this.fillJar();
            }

            if (this.count % TileBlockProtector.UPDATE_TICKS == 0) {
                if (this.range == 0) {
                    this.saturation = 0;
                }

                if (this.saturation > 0) {
                    this.saturation--;
                    return;
                }

                int powerLevel = this.getPowerLevel();
                boolean executeDecrease = this.range > powerLevel;

                if (this.range <= powerLevel && powerLevel > 0) {
                    executeDecrease = true;
                    if (this.takeFromContainer(TileBlockProtector.ASPECT, 1)) {

                        if (this.range < powerLevel) {
                            this.range++;
                            this.markDirty();
                            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);

                            this.saturation = 16 - this.range;
                        }
                        executeDecrease = false;
                    }
                }

                if (executeDecrease && this.range > 0) {
                    this.range--;

                    this.markDirty();
                    this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
                }
            }
        } else if (this.range > 0) {
            float sizeMod = 1 - (this.range / 15f);
            if (this.worldObj.rand.nextInt(9 - Thaumcraft.proxy.particleCount(2)) == 0) {
                Thaumcraft.proxy.wispFX3(
                        this.worldObj,
                        this.xCoord + 0.5F,
                        this.yCoord + 0.68F,
                        this.zCoord + 0.5F,
                        this.xCoord + 0.3F + this.worldObj.rand.nextFloat() * 0.4F,
                        this.yCoord + 0.68F,
                        this.zCoord + 0.3F + this.worldObj.rand.nextFloat() * 0.4F,
                        0.3F - (0.15f * sizeMod),
                        6,
                        true,
                        -0.025F);
            }
            if (this.worldObj.rand.nextInt(15 - Thaumcraft.proxy.particleCount(4)) == 0) {
                Thaumcraft.proxy.wispFX3(
                        this.worldObj,
                        this.xCoord + 0.5F,
                        this.yCoord + 0.68F,
                        this.zCoord + 0.5F,
                        this.xCoord + 0.4F + this.worldObj.rand.nextFloat() * 0.2F,
                        this.yCoord + 0.68F,
                        this.zCoord + 0.4F + this.worldObj.rand.nextFloat() * 0.2F,
                        0.2F - (0.15f * sizeMod),
                        6,
                        true,
                        -0.02F);
            }
        }

        if (this.range <= 0) return;
        for (EntityLivingBase entity : worldObj.getEntitiesWithinAABB(EntityLivingBase.class, getProtectedAABB())) {
            if (entity instanceof EntityCreeper creeper) {
                creeper.timeSinceIgnited = 0;
            }

            if (this.worldObj.isRemote && !(entity instanceof EntityPlayer)) {
                this.spawnEntityParticles(entity);
            }
        }
    }

    private void spawnEntityParticles(EntityLivingBase entity) {
        AxisAlignedBB cube = entity.boundingBox;
        if (cube != null && this.worldObj.rand.nextInt(20) == 0) {
            double posX = this.worldObj.rand.nextDouble() * (cube.maxX - cube.minX) + cube.minX;
            double posY = this.worldObj.rand.nextDouble() * (cube.maxX - cube.minX) + cube.minY;
            double posZ = this.worldObj.rand.nextDouble() * (cube.maxX - cube.minX) + cube.minZ;

            switch (this.worldObj.rand.nextInt(5)) {
                case 0:
                    posX = cube.maxX;
                    break;
                case 1:
                    posY = cube.maxY;
                    break;
                case 2:
                    posZ = cube.maxZ;
                    break;
                case 3:
                    posX = cube.minX;
                    break;
                case 4:
                    posZ = cube.minZ;
                    break;
            }

            Thaumcraft.proxy.wispFX3(
                    this.worldObj,
                    posX,
                    posY,
                    posZ,
                    posX + this.worldObj.rand.nextFloat() * 0.2F,
                    posY,
                    posZ + this.worldObj.rand.nextFloat() * 0.2F,
                    0.2F,
                    6,
                    true,
                    -0.02F);
        }
    }

    private void fillJar() {
        TileEntity te = ThaumcraftApiHelper
                .getConnectableTile(this.worldObj, this.xCoord, this.yCoord, this.zCoord, ForgeDirection.DOWN);
        if (te != null) {
            IEssentiaTransport ic = (IEssentiaTransport) te;
            if (!ic.canOutputTo(ForgeDirection.UP)) {
                return;
            }
            Aspect ta = null;
            if (this.aspectFilter != null) {
                ta = this.aspectFilter;
            } else if ((this.aspect != null) && (this.amount > 0)) {
                ta = this.aspect;
            } else if ((ic.getEssentiaAmount(ForgeDirection.UP) > 0)
                    && (ic.getSuctionAmount(ForgeDirection.UP) < this.getSuctionAmount(ForgeDirection.DOWN))
                    && (this.getSuctionAmount(ForgeDirection.DOWN) >= ic.getMinimumSuction())) {
                        ta = ic.getEssentiaType(ForgeDirection.UP);
                    }
            if ((ta != null) && (ic.getSuctionAmount(ForgeDirection.UP) < this.getSuctionAmount(ForgeDirection.DOWN))) {
                this.addToContainer(ta, ic.takeEssentia(ta, 1, ForgeDirection.UP));
            }
        }
    }

    @Override
    public int getMinimumSuction() {
        return super.getMinimumSuction() * 2;
    }

    @Override
    public int getSuctionAmount(ForgeDirection loc) {
        return super.getSuctionAmount(loc) * 2;
    }

    @Override
    public boolean isConnectable(ForgeDirection face) {
        return face == ForgeDirection.DOWN;
    }

    @Override
    public boolean canInputFrom(ForgeDirection face) {
        return face == ForgeDirection.DOWN;
    }

    @Override
    public boolean canOutputTo(ForgeDirection face) {
        return false;
    }

    private static List<TileBlockProtector> protectors = new ArrayList<TileBlockProtector>();

    public static boolean isSpotProtected(World world, final double x, final double y, final double z) {
        return TileBlockProtector.isSpotProtected(world, new ProtectionHelper() {

            @Override
            public boolean checkProtection(TileBlockProtector tile) {
                return TileBlockProtector.isSpotProtected(tile, x, y, z);
            }
        });
    }

    public static boolean isSpotProtected(World world, final Entity entity) {
        return TileBlockProtector.isSpotProtected(world, new ProtectionHelper() {

            @Override
            public boolean checkProtection(TileBlockProtector tile) {
                return TileBlockProtector.isSpotProtected(tile, entity);
            }
        });
    }

    public static boolean isSpotProtected(World world, ProtectionHelper helper) {
        for (int i = 0; i < TileBlockProtector.protectors.size(); i++) {
            TileBlockProtector protector = TileBlockProtector.protectors.get(i);
            if (protector.isInvalid()) {
                TileBlockProtector.protectors.remove(i);
                i--;
            } else if (protector.worldObj.isRemote == world.isRemote && helper.checkProtection(protector)) {
                return true;
            }
        }
        return false;
    }

    private interface ProtectionHelper {

        boolean checkProtection(TileBlockProtector tile);
    }

    private static boolean isSpotProtected(TileBlockProtector tile, Entity entity) {
        AxisAlignedBB entityAABB = entity.boundingBox;
        if (entityAABB != null) {
            return tile.getProtectedAABB().intersectsWith(entityAABB.addCoord(entity.posX, entity.posY, entity.posZ));
        }
        return TileBlockProtector.isSpotProtected(tile, entity.posX, entity.posY, entity.posZ);
    }

    private AxisAlignedBB getProtectedAABB() {
        return AxisAlignedBB.getBoundingBox(
                this.xCoord - this.range,
                this.yCoord - this.range,
                this.zCoord - this.range,
                this.xCoord + this.range,
                this.yCoord + this.range,
                this.zCoord + this.range);
    }

    private static boolean isSpotProtected(TileBlockProtector tile, double x, double y, double z) {
        return tile.getProtectedAABB().isVecInside(Vec3.createVectorHelper(x, y, z));
    }
}
