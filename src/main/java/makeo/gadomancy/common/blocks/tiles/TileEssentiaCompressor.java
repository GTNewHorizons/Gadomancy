package makeo.gadomancy.common.blocks.tiles;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import makeo.gadomancy.client.events.ClientHandler;
import makeo.gadomancy.common.network.packets.PacketAnimationAbsorb;
import makeo.gadomancy.common.registration.RegisteredBlocks;
import makeo.gadomancy.common.registration.RegisteredItems;
import makeo.gadomancy.common.utils.ExplosionHelper;
import makeo.gadomancy.common.utils.Injector;
import makeo.gadomancy.common.utils.Vector3;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.WorldCoordinates;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.client.fx.bolt.FXLightningBolt;
import thaumcraft.client.fx.particles.FXEssentiaTrail;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.events.EssentiaHandler;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXEssentiaSource;
import thaumcraft.common.tiles.TilePedestal;
import tuhljin.automagy.api.essentia.IAspectContainerWithMax;
import tuhljin.automagy.api.essentia.IEssentiaLocusReadable;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by HellFirePvP @ 22.04.2016 21:42
 */
@Optional.InterfaceList({
        @Optional.Interface(iface = "tuhljin.automagy.api.essentia.IEssentiaLocusReadable", modid = "Automagy"),
        @Optional.Interface(iface = "tuhljin.automagy.api.essentia.IAspectContainerWithMax", modid = "Automagy") })
public class TileEssentiaCompressor extends SynchronizedTileEntity
        implements IEssentiaTransport, IEssentiaLocusReadable, IAspectContainerWithMax {

    public static final int MAX_SIZE = 8;
    public static final int MAX_ASPECT_STORAGE = 3000, STD_ASPECT_STORAGE = 200;

    private static Injector injEssentiaHandler = new Injector(EssentiaHandler.class);
    private static int multiblockIDCounter;

    // Standard multiblock stuff
    private boolean isMasterTile; // For debugging
    private int multiblockYIndex = -1, multiblockId = -1;

    private boolean isMultiblockPresent;

    private int incSize;
    private Vector3 coordPedestal;
    private int consumeTick;

    private AspectList al = new AspectList();
    private int ticksExisted;
    private boolean prevFound;

    @Override
    public void updateEntity() {
        super.updateEntity();
        this.ticksExisted++;

        if (!this.worldObj.isRemote) {
            if (this.isMultiblockFormed()) {
                this.checkMultiblock();
            }

            if (!this.isMultiblockFormed()) return;

            if (this.isMasterTile() && (this.prevFound || (this.ticksExisted % 100) == 0)) {
                List<WorldCoordinates> coords = this.searchAndGetSources();
                if (coords == null || coords.isEmpty()) {
                    this.prevFound = false;
                } else {
                    this.prevFound = this.searchForEssentia(coords);
                }
            }

            if (this.isMasterTile()
                    && ((this.incSize < TileEssentiaCompressor.MAX_SIZE && (this.ticksExisted % 40) == 0)
                            || (this.coordPedestal != null))) {
                this.consumeElements();
            }
        } else {
            if (this.isMasterTile() && this.isMultiblockFormed()) {
                this.playLightningEffects();
                this.playVortexEffects();
                if (this.al.visSize() > 0) {
                    this.playEssentiaEffects();
                }
            }
        }

        if (this.isMasterTile() && this.isMultiblockFormed()) {
            this.vortexEntities();
        }
    }

    @Optional.Method(modid = "Automagy")
    public AspectList getAspectsBase() {
        return this.getAspects();
    }

    private void consumeElements() {
        if (this.coordPedestal != null) {
            if (!this.checkPedestal(this.coordPedestal)) {
                this.consumeTick = 0;
                this.coordPedestal = null;
                return;
            }
            this.consumeTick++;
            if (this.consumeTick <= 400) {
                PacketAnimationAbsorb absorb = new PacketAnimationAbsorb(
                        this.xCoord,
                        this.yCoord + 1,
                        this.zCoord,
                        this.coordPedestal.getBlockX(),
                        this.coordPedestal.getBlockY() + 1,
                        this.coordPedestal.getBlockZ(),
                        5,
                        Block.getIdFromBlock(ConfigBlocks.blockCosmeticSolid),
                        1);
                makeo.gadomancy.common.network.PacketHandler.INSTANCE.sendToAllAround(
                        absorb,
                        new NetworkRegistry.TargetPoint(
                                this.getWorldObj().provider.dimensionId,
                                this.xCoord,
                                this.yCoord,
                                this.zCoord,
                                16));
            } else {
                TilePedestal te = (TilePedestal) this.worldObj.getTileEntity(
                        this.coordPedestal.getBlockX(),
                        this.coordPedestal.getBlockY(),
                        this.coordPedestal.getBlockZ());
                te.setInventorySlotContents(0, null);
                te.markDirty();
                this.worldObj.markBlockForUpdate(
                        this.coordPedestal.getBlockX(),
                        this.coordPedestal.getBlockY(),
                        this.coordPedestal.getBlockZ());
                this.consumeTick = 0;
                this.coordPedestal = null;
                this.incSize += 1;
                this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
                this.markDirty();
            }
        } else {
            for (int xx = -3; xx <= 3; xx++) {
                for (int zz = -3; zz <= 3; zz++) {
                    Vector3 offset = new Vector3(this.xCoord + xx, this.yCoord, this.zCoord + zz);
                    if (this.checkPedestal(offset)) {
                        this.coordPedestal = offset;
                        this.consumeTick = 0;
                        return;
                    }
                }
            }
            this.coordPedestal = null;
            this.consumeTick = 0;
        }
    }

    private boolean checkPedestal(Vector3 coordPedestal) {
        Block at = this.worldObj
                .getBlock(coordPedestal.getBlockX(), coordPedestal.getBlockY(), coordPedestal.getBlockZ());
        int md = this.worldObj
                .getBlockMetadata(coordPedestal.getBlockX(), coordPedestal.getBlockY(), coordPedestal.getBlockZ());
        TileEntity te = this.worldObj
                .getTileEntity(coordPedestal.getBlockX(), coordPedestal.getBlockY(), coordPedestal.getBlockZ());
        if (at == null || te == null || md != 1) return false;
        if (!at.equals(RegisteredBlocks.blockStoneMachine) || !(te instanceof TilePedestal)) return false;
        ItemStack st = ((TilePedestal) te).getStackInSlot(0);
        if (st == null || st.getItem() == null) return false;
        return st.getItem().equals(RegisteredItems.itemElement) && st.getItemDamage() == 0;
    }

    // only call from master tile.
    private boolean searchForEssentia(List<WorldCoordinates> coordinates) {
        if (this.ticksExisted % 10 != 0) return this.prevFound;
        for (Aspect a : Aspect.aspects.values()) {
            if (this.doDrain(a, coordinates)) {
                this.al.add(a, 1);
                this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
                this.markDirty();
                return true;
            }
        }
        return false;
    }

    // only call from master tile.
    private boolean doDrain(Aspect a, List<WorldCoordinates> coordinates) {
        for (WorldCoordinates coordinate : coordinates) {
            TileEntity sourceTile = this.worldObj.getTileEntity(coordinate.x, coordinate.y, coordinate.z);
            if (!(sourceTile instanceof IAspectSource)) {
                continue;
            }
            if (sourceTile instanceof TileEssentiaCompressor) continue;
            IAspectSource as = (IAspectSource) sourceTile;
            AspectList contains = as.getAspects();
            if (contains == null || contains.visSize() > this.al.visSize()) continue;
            if (!this.canAccept(a)) continue;
            if (as.takeFromContainer(a, 1)) {
                PacketHandler.INSTANCE.sendToAllAround(
                        new PacketFXEssentiaSource(
                                this.xCoord,
                                this.yCoord + 1,
                                this.zCoord,
                                (byte) (this.xCoord - coordinate.x),
                                (byte) (this.yCoord - coordinate.y + 1),
                                (byte) (this.zCoord - coordinate.z),
                                a.getColor()),
                        new NetworkRegistry.TargetPoint(
                                this.worldObj.provider.dimensionId,
                                this.xCoord,
                                this.yCoord + 1,
                                this.zCoord,
                                32.0D));
                return true;
            }
        }
        return false;
    }

    private int currentStorageSize() {
        return TileEssentiaCompressor.STD_ASPECT_STORAGE + this.incSize
                * ((TileEssentiaCompressor.MAX_ASPECT_STORAGE - TileEssentiaCompressor.STD_ASPECT_STORAGE)
                        / TileEssentiaCompressor.MAX_SIZE);
    }

    private boolean canAccept(Aspect a) {
        int current = this.al.getAmount(a);
        int max = this.currentStorageSize();
        return current < max;
    }

    public int getSizeStage() {
        return this.incSize;
    }

    float tr = 1.0F;
    float tri;
    float tg = 1.0F;
    float tgi;
    float tb = 1.0F;
    float tbi;
    public float cr = 1.0F;
    public float cg = 1.0F;
    public float cb = 1.0F;
    public Aspect displayAspect;

    // Thanks @TileEssentiaReservoir :P
    @SideOnly(Side.CLIENT)
    private void playEssentiaEffects() {
        if ((ClientHandler.ticks % 20 == 0) && (this.al.size() > 0)) {
            this.displayAspect = this.al.getAspects()[(ClientHandler.ticks / 20 % this.al.size())];
            Color c = new Color(this.displayAspect.getColor());
            this.tr = (c.getRed() / 255.0F);
            this.tg = (c.getGreen() / 255.0F);
            this.tb = (c.getBlue() / 255.0F);
            this.tri = ((this.cr - this.tr) / 20.0F);
            this.tgi = ((this.cg - this.tg) / 20.0F);
            this.tbi = ((this.cb - this.tb) / 20.0F);
        }
        if (this.displayAspect == null) {
            this.tr = (this.tg = this.tb = 1.0F);
            this.tri = (this.tgi = this.tbi = 0.0F);
        } else {
            this.cr -= this.tri;
            this.cg -= this.tgi;
            this.cb -= this.tbi;
        }
        int count = 1;
        this.cr = Math.min(1.0F, Math.max(0.0F, this.cr));
        this.cg = Math.min(1.0F, Math.max(0.0F, this.cg));
        this.cb = Math.min(1.0F, Math.max(0.0F, this.cb));
        FXEssentiaTrail essentiaTrail = new FXEssentiaTrail(
                this.worldObj,
                this.xCoord + 0.5,
                this.yCoord + 0.4,
                this.zCoord + 0.5,
                this.xCoord + 0.5,
                this.yCoord + 1.5,
                this.zCoord + 0.5,
                count,
                new Color(this.cr, this.cg, this.cb).getRGB(),
                0.8F);
        essentiaTrail.noClip = true;
        essentiaTrail.motionY = (0.01F + MathHelper.sin(count / 3.0F) * 0.001F);
        essentiaTrail.motionX = (MathHelper.sin(count / 10.0F) * 0.01F + this.worldObj.rand.nextGaussian() * 0.01D);
        essentiaTrail.motionZ = (MathHelper.sin(count / 10.0F) * 0.01F + this.worldObj.rand.nextGaussian() * 0.01D);
        ParticleEngine.instance.addEffect(this.worldObj, essentiaTrail);

        essentiaTrail = new FXEssentiaTrail(
                this.worldObj,
                this.xCoord + 0.5,
                this.yCoord + 2.6,
                this.zCoord + 0.5,
                this.xCoord + 0.5,
                this.yCoord + 1.5,
                this.zCoord + 0.5,
                count,
                new Color(this.cr, this.cg, this.cb).getRGB(),
                0.8F);
        essentiaTrail.noClip = true;
        essentiaTrail.motionY = -(0.01F + MathHelper.sin(count / 3.0F) * 0.001F);
        essentiaTrail.motionX = (MathHelper.sin(count / 10.0F) * 0.01F + this.worldObj.rand.nextGaussian() * 0.01D);
        essentiaTrail.motionZ = (MathHelper.sin(count / 10.0F) * 0.01F + this.worldObj.rand.nextGaussian() * 0.01D);
        ParticleEngine.instance.addEffect(this.worldObj, essentiaTrail);
    }

    private List<WorldCoordinates> searchAndGetSources() {
        WorldCoordinates thisCoordinates = new WorldCoordinates(this);
        Map<WorldCoordinates, List<WorldCoordinates>> teSources = new HashMap<>();
        this.getSourcesField(teSources);
        if (!teSources.containsKey(thisCoordinates)) {
            this.searchSources();
            this.getSourcesField(teSources);
            if (teSources.containsKey(thisCoordinates)) {
                return this.searchAndGetSources();
            }
            return new ArrayList<>();
        }
        List<WorldCoordinates> result = teSources.get(thisCoordinates);
        ((Map<WorldCoordinates, List<WorldCoordinates>>) TileEssentiaCompressor.injEssentiaHandler.getField("sources"))
                .remove(thisCoordinates);
        return result;
    }

    private void searchSources() {
        WorldCoordinates thisCoord = new WorldCoordinates(
                this.xCoord,
                this.yCoord,
                this.zCoord,
                this.worldObj.provider.dimensionId);
        List<WorldCoordinates> coords = new LinkedList<>();
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            this.unsafe_search(thisCoord, direction, coords);
        }

        ((Map<WorldCoordinates, List<WorldCoordinates>>) TileEssentiaCompressor.injEssentiaHandler.getField("sources"))
                .put(thisCoord, coords);
    }

    private void unsafe_search(WorldCoordinates coord, ForgeDirection direction, List<WorldCoordinates> out) {
        ((HashMap<WorldCoordinates, Long>) TileEssentiaCompressor.injEssentiaHandler.getField("sourcesDelay"))
                .remove(coord);
        TileEssentiaCompressor.injEssentiaHandler.invokeMethod(
                "getSources",
                new Class[] { World.class, WorldCoordinates.class, ForgeDirection.class, int.class },
                this.worldObj,
                coord,
                direction,
                5);
        List<WorldCoordinates> coords = ((Map<WorldCoordinates, List<WorldCoordinates>>) TileEssentiaCompressor.injEssentiaHandler
                .getField("sources")).get(coord);
        if (coords != null) {
            out.addAll(
                    ((Map<WorldCoordinates, List<WorldCoordinates>>) TileEssentiaCompressor.injEssentiaHandler
                            .getField("sources")).get(coord));
            ((Map<WorldCoordinates, List<WorldCoordinates>>) TileEssentiaCompressor.injEssentiaHandler
                    .getField("sources")).remove(coord);
        }
    }

    private void getSourcesField(Map<WorldCoordinates, List<WorldCoordinates>> out) {
        out.clear();
        out.putAll(TileEssentiaCompressor.injEssentiaHandler.getField("sources"));
    }

    private void playVortexEffects() {
        for (int a = 0; a < Thaumcraft.proxy.particleCount(1); a++) {
            int tx = this.xCoord + this.worldObj.rand.nextInt(4) - this.worldObj.rand.nextInt(4);
            int ty = this.yCoord + 1 + this.worldObj.rand.nextInt(4) - this.worldObj.rand.nextInt(4);
            int tz = this.zCoord + this.worldObj.rand.nextInt(4) - this.worldObj.rand.nextInt(4);
            if (ty > this.worldObj.getHeightValue(tx, tz)) {
                ty = this.worldObj.getHeightValue(tx, tz);
            }
            Vec3 v1 = Vec3.createVectorHelper(this.xCoord + 0.5D, this.yCoord + 1.5D, this.zCoord + 0.5D);
            Vec3 v2 = Vec3.createVectorHelper(tx + 0.5D, ty + 0.5D, tz + 0.5D);

            MovingObjectPosition mop = ThaumcraftApiHelper
                    .rayTraceIgnoringSource(this.worldObj, v1, v2, true, false, false);
            if ((mop != null) && (this.getDistanceFrom(mop.blockX, mop.blockY, mop.blockZ) < 16.0D)) {
                tx = mop.blockX;
                ty = mop.blockY;
                tz = mop.blockZ;
                Block bi = this.worldObj.getBlock(tx, ty, tz);
                int md = this.worldObj.getBlockMetadata(tx, ty, tz);
                if (!bi.isAir(this.worldObj, tx, ty, tz)) {
                    Thaumcraft.proxy
                            .hungryNodeFX(this.worldObj, tx, ty, tz, this.xCoord, this.yCoord + 1, this.zCoord, bi, md);
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private void playLightningEffects() {
        if (this.incSize < 6) {
            if (this.incSize < 2) {
                if (this.worldObj.rand.nextInt(6) != 0) return;
            } else {
                if (this.worldObj.rand.nextBoolean()) return;
            }
        }
        double originX = this.xCoord + 0.5;
        double originY = this.yCoord + 1.5;
        double originZ = this.zCoord + 0.5;
        double targetX = this.xCoord + 0.4 + this.worldObj.rand.nextFloat() * 0.2;
        double targetY = this.yCoord + 0.4 + (this.worldObj.rand.nextBoolean() ? 2.2 : 0);
        double targetZ = this.zCoord + 0.4 + this.worldObj.rand.nextFloat() * 0.2;
        FXLightningBolt bolt = new FXLightningBolt(
                Minecraft.getMinecraft().theWorld,
                originX,
                originY,
                originZ,
                targetX,
                targetY,
                targetZ,
                Minecraft.getMinecraft().theWorld.rand.nextLong(),
                10,
                4.0F,
                5);
        bolt.defaultFractal();
        bolt.setType(5);
        bolt.finalizeBolt();
    }

    private void vortexEntities() {
        if (this.incSize <= 0) return;
        List entities = this.worldObj.getEntitiesWithinAABB(
                Entity.class,
                AxisAlignedBB.getBoundingBox(
                        this.xCoord - 0.5,
                        this.yCoord - 0.5,
                        this.zCoord - 0.5,
                        this.xCoord + 0.5,
                        this.yCoord + 0.5,
                        this.zCoord + 0.5).expand(4, 4, 4));
        for (Object o : entities) {
            if (!(o instanceof Entity) || ((Entity) o).isDead) continue;
            this.applyMovementVectors((Entity) o);
        }
    }

    private void applyMovementVectors(Entity entity) {
        double mult = this.incSize * (1D / ((double) TileEssentiaCompressor.MAX_SIZE));

        double var3 = (this.xCoord + 0.5D - entity.posX) / 8.0D;
        double var5 = (this.yCoord + 1.5D - entity.posY) / 8.0D;
        double var7 = (this.zCoord + 0.5D - entity.posZ) / 8.0D;
        double var9 = Math.sqrt(var3 * var3 + var5 * var5 + var7 * var7);
        double var11 = 1.0D - var9;
        if (var11 > 0.0D) {
            var11 *= var11;
            entity.motionX += var3 / var9 * var11 * (0.08D * mult);
            entity.motionY += var5 / var9 * var11 * (0.11D * mult);
            entity.motionZ += var7 / var9 * var11 * (0.08D * mult);
        }
    }

    public void checkMultiblock() {
        if (this.isMasterTile) {
            boolean canSustain = this.isMultiblockableBlock(1);
            if (!this.isMultiblockableBlock(2)) canSustain = false;
            if (!canSustain) {
                this.breakMultiblock();
                this.isMultiblockPresent = false;
            }
        } else {
            TileEssentiaCompressor master = this.tryFindMasterTile();
            if (master != null) {
                if (!master.isMultiblockPresent) {
                    this.breakMultiblock();
                }
            } else {
                this.breakMultiblock();
            }
        }
    }

    public TileEssentiaCompressor tryFindMasterTile() {
        if (!this.isMultiblockFormed()) return null;
        if (this.isMasterTile()) return this; // lul.. check before plz.
        Block down = this.worldObj.getBlock(this.xCoord, this.yCoord - this.multiblockYIndex, this.zCoord);
        if (!down.equals(RegisteredBlocks.blockEssentiaCompressor)) return null;
        TileEntity te = this.worldObj.getTileEntity(this.xCoord, this.yCoord - this.multiblockYIndex, this.zCoord);
        if (!(te instanceof TileEssentiaCompressor)) return null;
        TileEssentiaCompressor compressor = (TileEssentiaCompressor) te;
        if (compressor.multiblockId != this.multiblockId || !compressor.isMasterTile()) return null;
        return compressor;
    }

    private boolean isMultiblockableBlock(int yOffset) {
        Block block = this.worldObj.getBlock(this.xCoord, this.yCoord + yOffset, this.zCoord);
        TileEntity te = this.worldObj.getTileEntity(this.xCoord, this.yCoord + yOffset, this.zCoord);
        if (!block.equals(RegisteredBlocks.blockEssentiaCompressor)) return false;
        if (!(te instanceof TileEssentiaCompressor)) return false;
        TileEssentiaCompressor compressor = (TileEssentiaCompressor) te;
        return compressor.multiblockId == this.multiblockId;
    }

    public void breakMultiblock() {
        this.doExplosion();
        this.multiblockId = -1;
        this.isMasterTile = false;
        this.multiblockYIndex = -1;
        this.al = new AspectList();
        this.markDirty();
        this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
    }

    private void doExplosion() {
        AspectList al = this.getAspects();
        if (al.visSize() > 0) {
            ExplosionHelper.taintplosion(this.worldObj, this.xCoord, this.yCoord, this.zCoord, true, 2, 2.0F, 4, 20);
            this.worldObj.setBlockToAir(this.xCoord, this.yCoord, this.zCoord);
        } else {
            ExplosionHelper.taintplosion(this.worldObj, this.xCoord, this.yCoord, this.zCoord, false, 0, 2.0F, 4, 20);
            this.worldObj.setBlockToAir(this.xCoord, this.yCoord, this.zCoord);
        }
    }

    public static int getAndIncrementNewMultiblockId() {
        TileEssentiaCompressor.multiblockIDCounter++;
        return TileEssentiaCompressor.multiblockIDCounter;
    }

    public void setInMultiblock(boolean isMaster, int yIndex, int multiblockId) {
        this.multiblockId = multiblockId;
        this.isMasterTile = isMaster;
        this.multiblockYIndex = yIndex;
        if (isMaster) {
            this.isMultiblockPresent = true; // Initial state.
        }
        this.al = new AspectList();
        this.markDirty();
        this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
    }

    public boolean isMultiblockFormed() {
        return this.multiblockId != -1;
    }

    public boolean isMasterTile() {
        return this.isMasterTile;
    }

    public boolean isAccessPoint() {
        return this.multiblockYIndex == 1;
    }

    public int getMultiblockYIndex() {
        return this.multiblockYIndex;
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);

        this.multiblockYIndex = compound.getInteger("multiblockYIndex");
        this.isMasterTile = compound.getBoolean("isMasterTile");
        this.multiblockId = compound.getInteger("multiblockId");
        this.isMultiblockPresent = compound.getBoolean("multiblockPresent");
        this.incSize = compound.getInteger("sizeInc");
        AspectList al = new AspectList();
        NBTTagCompound cmp = compound.getCompoundTag("aspects");
        for (Object tag : cmp.func_150296_c()) {
            String strTag = (String) tag;
            int amt = cmp.getInteger(strTag);
            al.add(Aspect.getAspect(strTag), amt);
        }
        this.al = al;
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);

        compound.setBoolean("isMasterTile", this.isMasterTile);
        compound.setInteger("multiblockId", this.multiblockId);
        compound.setInteger("multiblockYIndex", this.multiblockYIndex);
        compound.setBoolean("multiblockPresent", this.isMasterTile);
        compound.setInteger("sizeInc", this.incSize);
        NBTTagCompound aspects = new NBTTagCompound();
        for (Aspect a : this.al.aspects.keySet()) {
            aspects.setInteger(a.getTag(), this.al.aspects.get(a));
        }
        compound.setTag("aspects", aspects);
    }

    @Override
    public AspectList getAspects() {
        if (this.isMultiblockFormed() && this.isAccessPoint()) {
            TileEssentiaCompressor master = this.tryFindMasterTile();
            if (master != null) {
                return master.al;
            }
        }
        return new AspectList();
    }

    @Override
    public void setAspects(AspectList list) {}

    @Override
    public boolean doesContainerAccept(Aspect aspect) {
        return this.isMultiblockFormed() && this.isAccessPoint() && this.canAccept(aspect);
    }

    @Override
    public int addToContainer(Aspect aspect, int amount) {
        if (this.doesContainerAccept(aspect) && this.canAccept(aspect)) {
            TileEssentiaCompressor master = this.tryFindMasterTile();
            if (master == null) return 0;
            int available = master.currentStorageSize() - master.al.getAmount(aspect);
            if (available <= 0) return amount;
            int amountToAdd = Math.min(amount, available);
            master.al.add(aspect, amountToAdd);
            this.worldObj.markBlockForUpdate(master.xCoord, master.yCoord, master.zCoord);
            this.markDirty();
            return amount - amountToAdd;
        }
        return amount;
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int i) {
        if (this.isMultiblockFormed() && this.isAccessPoint()) {
            TileEssentiaCompressor master = this.tryFindMasterTile();
            if (master == null) return false;
            boolean couldTake = master.al.reduce(aspect, i);
            master.al.remove(aspect, 0);
            this.worldObj.markBlockForUpdate(master.xCoord, master.yCoord, master.zCoord);
            this.markDirty();
            return couldTake;
        }
        return false;
    }

    @Override
    @Deprecated
    public boolean takeFromContainer(AspectList list) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect aspect, int i) {
        if (this.isMultiblockFormed() && this.isAccessPoint()) {
            TileEssentiaCompressor master = this.tryFindMasterTile();
            if (master == null) return false;
            return master.al.getAmount(aspect) >= i;
        }
        return false;
    }

    @Override
    @Deprecated
    public boolean doesContainerContain(AspectList list) {
        return false;
    }

    @Override
    public int containerContains(Aspect aspect) {
        if (this.isMultiblockFormed() && this.isAccessPoint()) {
            TileEssentiaCompressor master = this.tryFindMasterTile();
            if (master == null) return 0;
            return master.al.getAmount(aspect);
        }
        return 0;
    }

    @Override
    public boolean isConnectable(ForgeDirection direction) {
        return false;
    }

    @Override
    public boolean canInputFrom(ForgeDirection direction) {
        if (this.isMultiblockFormed() && this.isAccessPoint()) { // The middle one
            return direction == ForgeDirection.SOUTH || direction == ForgeDirection.NORTH
                    || direction == ForgeDirection.EAST
                    || direction == ForgeDirection.WEST;
        }
        return false;
    }

    @Override
    public boolean canOutputTo(ForgeDirection direction) {
        if (this.isMultiblockFormed() && this.isAccessPoint()) { // The middle one
            return direction == ForgeDirection.SOUTH || direction == ForgeDirection.NORTH
                    || direction == ForgeDirection.EAST
                    || direction == ForgeDirection.WEST;
        }
        return false;
    }

    @Override
    public void setSuction(Aspect aspect, int i) {}

    @Override
    public Aspect getSuctionType(ForgeDirection direction) {
        return null;
    }

    @Override
    public int getSuctionAmount(ForgeDirection direction) {
        if (this.canInputFrom(direction)) {
            return 16;
        }
        return 0;
    }

    @Override
    public int takeEssentia(Aspect aspect, int i, ForgeDirection direction) {
        if (this.isMultiblockFormed() && this.isAccessPoint()) {
            if (!this.canOutputTo(direction)) return 0;
            TileEssentiaCompressor master = this.tryFindMasterTile();
            if (master == null) return 0;
            int amt = master.al.getAmount(aspect);
            int taken = amt - master.al.remove(aspect, i).getAmount(aspect);
            this.worldObj.markBlockForUpdate(master.xCoord, master.yCoord, master.zCoord);
            this.markDirty();
            return taken;
        }
        return 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, ForgeDirection direction) {
        if (this.canInputFrom(direction)) {
            return amount - this.addToContainer(aspect, amount);
        }
        return 0;
    }

    @Override
    public Aspect getEssentiaType(ForgeDirection direction) {
        if (this.isMultiblockFormed() && this.isAccessPoint()) {
            TileEssentiaCompressor master = this.tryFindMasterTile();
            if (master == null) return null;
            return new ArrayList<>(master.al.aspects.keySet()).get(this.worldObj.rand.nextInt(master.al.size()));
        }
        return null;
    }

    @Override
    public int getEssentiaAmount(ForgeDirection direction) {
        if (this.isMultiblockFormed() && this.isAccessPoint()) {
            TileEssentiaCompressor master = this.tryFindMasterTile();
            if (master == null) return 0;
            return master.al.visSize();
        }
        return 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }
}
