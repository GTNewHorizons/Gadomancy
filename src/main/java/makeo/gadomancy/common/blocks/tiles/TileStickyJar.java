package makeo.gadomancy.common.blocks.tiles;

import java.lang.reflect.Field;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.common.registry.GameData;
import makeo.gadomancy.common.utils.Injector;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.tiles.TileJarFillable;
import thaumcraft.common.tiles.TileJarFillableVoid;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by makeo @ 13.07.2015 15:48
 */
public class TileStickyJar extends TileJarFillable {

    private Block parentBlock;
    private Integer parentMetadata = 0;

    private TileJarFillable parent;

    public ForgeDirection placedOn;

    private final Injector injector;
    private Field fieldCount;

    public TileStickyJar() {
        this.injector = new Injector(null, TileJarFillable.class);
        this.fieldCount = Injector.getField("count", TileJarFillable.class);
    }

    public Block getParentBlock() {
        return this.parentBlock;
    }

    public TileJarFillable getParent() {
        return this.parent;
    }

    public boolean isValid() {
        return this.parentBlock != null && this.parent != null;
    }

    private boolean needsRenderUpdate;

    public void init(TileJarFillable parent, Block parentBlock, int parentMetadata, ForgeDirection placedOn) {
        this.parent = parent;

        this.placedOn = placedOn;

        parent.xCoord = this.xCoord;
        parent.yCoord = this.yCoord;
        parent.zCoord = this.zCoord;

        this.syncFromParent();

        this.parent.setWorldObj(this.getWorldObj());
        this.parentBlock = parentBlock;
        this.parentMetadata = parentMetadata;
        this.injector.setObject(this.parent);

        this.markDirty();
        this.needsRenderUpdate = true;
    }

    private void sync(TileJarFillable from, TileJarFillable to) {
        to.aspect = from.aspect;
        to.aspectFilter = from.aspectFilter;
        to.amount = from.amount;
        to.maxAmount = from.maxAmount;
        to.facing = from.facing;
        to.forgeLiquid = from.forgeLiquid;
        to.lid = from.lid;
    }

    public void syncToParent() {
        this.sync(this, this.parent);
    }

    public void syncFromParent() {
        this.sync(this.parent, this);
    }

    public Integer getParentMetadata() {
        return this.parentMetadata;
    }

    private int count;

    @Override
    public void updateEntity() {
        if (!this.isValid()) {
            if (!this.getWorldObj().isRemote) {
                this.getWorldObj().setBlock(this.xCoord, this.yCoord, this.zCoord, Blocks.air);
            }
            return;
        }

        if (this.getWorldObj().isRemote && this.needsRenderUpdate) {
            this.needsRenderUpdate = false;
            Minecraft.getMinecraft().renderGlobal.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
        }

        this.syncToParent();

        boolean canTakeEssentia = this.amount < this.maxAmount;
        if (this.parent instanceof TileJarFillableVoid) canTakeEssentia = true;

        if ((!this.worldObj.isRemote) && (++this.count % 5 == 0) && canTakeEssentia) {
            this.fillJar();
        }

        this.injector.setField(this.fieldCount, 1);

        this.parent.updateEntity();

        this.syncFromParent();
    }

    @Override
    public void setWorldObj(World world) {
        super.setWorldObj(world);
        if (this.parent != null) this.parent.setWorldObj(this.worldObj);
    }

    private void fillJar() {
        ForgeDirection inputDir = this.placedOn.getOpposite();

        TileEntity te = ThaumcraftApiHelper.getConnectableTile(
                this.parent.getWorldObj(),
                this.parent.xCoord,
                this.parent.yCoord,
                this.parent.zCoord,
                inputDir);
        if (te != null) {
            IEssentiaTransport ic = (IEssentiaTransport) te;
            if (!ic.canOutputTo(ForgeDirection.DOWN)) {
                return;
            }
            Aspect ta = null;
            if (this.parent.aspectFilter != null) {
                ta = this.parent.aspectFilter;
            } else if ((this.parent.aspect != null) && (this.parent.amount > 0)) {
                ta = this.parent.aspect;
            } else if ((ic.getEssentiaAmount(inputDir.getOpposite()) > 0)
                    && (ic.getSuctionAmount(inputDir.getOpposite()) < this.getSuctionAmount(ForgeDirection.UP))
                    && (this.getSuctionAmount(ForgeDirection.UP) >= ic.getMinimumSuction())) {
                        ta = ic.getEssentiaType(inputDir.getOpposite());
                    }
            if ((ta != null)
                    && (ic.getSuctionAmount(inputDir.getOpposite()) < this.getSuctionAmount(ForgeDirection.UP))) {
                this.addToContainer(ta, ic.takeEssentia(ta, 1, inputDir.getOpposite()));
            }
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        String parentType = compound.getString("parentType");
        if (parentType.length() > 0) {
            Block block = GameData.getBlockRegistry().getObject(parentType);
            if (block != null && compound.hasKey("parent") && compound.hasKey("parentMetadata")) {
                NBTTagCompound data = compound.getCompoundTag("parent");
                int metadata = compound.getInteger("parentMetadata");
                TileEntity tile = block.createTileEntity(this.getWorldObj(), metadata);
                if (tile instanceof TileJarFillable) {
                    this.placedOn = ForgeDirection.getOrientation(compound.getInteger("placedOn"));
                    tile.readFromNBT(data);
                    this.init((TileJarFillable) tile, block, metadata, this.placedOn);
                }
            }
        }

        if (!this.isValid() && !this.getWorldObj().isRemote) {
            this.getWorldObj().setBlockToAir(this.xCoord, this.yCoord, this.zCoord);
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        if (this.isValid()) {
            compound.setString("parentType", GameData.getBlockRegistry().getNameForObject(this.parentBlock));
            compound.setInteger("parentMetadata", this.parentMetadata);

            this.syncToParent();

            NBTTagCompound data = new NBTTagCompound();
            this.parent.writeToNBT(data);
            compound.setTag("parent", data);

            compound.setInteger("placedOn", this.placedOn.ordinal());
        }
    }

    @Override
    public AspectList getAspects() {
        if (this.isValid()) {
            this.syncToParent();
            AspectList result = this.parent.getAspects();
            this.syncFromParent();
            return result;
        }
        return new AspectList();
    }

    @Override
    public void setAspects(AspectList paramAspectList) {
        if (this.isValid()) {
            this.syncToParent();
            this.parent.getAspects();
            this.syncFromParent();
        }
    }

    @Override
    public boolean doesContainerAccept(Aspect paramAspect) {
        if (this.isValid()) {
            this.syncToParent();
            boolean result = this.parent.doesContainerAccept(paramAspect);
            this.syncFromParent();
            return result;
        }
        return false;
    }

    @Override
    public int addToContainer(Aspect paramAspect, int paramInt) {
        if (this.isValid()) {
            this.syncToParent();
            int result = this.parent.addToContainer(paramAspect, paramInt);
            this.syncFromParent();
            return result;
        }
        return paramInt;
    }

    @Override
    public boolean takeFromContainer(Aspect paramAspect, int paramInt) {
        if (this.isValid()) {
            this.syncToParent();
            boolean result = this.parent.takeFromContainer(paramAspect, paramInt);
            this.syncFromParent();
            return result;
        }
        return false;
    }

    @Override
    public boolean takeFromContainer(AspectList paramAspectList) {
        if (this.isValid()) {
            this.syncToParent();
            boolean result = this.parent.takeFromContainer(paramAspectList);
            this.syncFromParent();
            return result;
        }
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect paramAspect, int paramInt) {
        if (this.isValid()) {
            this.syncToParent();
            boolean result = this.parent.doesContainerContainAmount(paramAspect, paramInt);
            this.syncFromParent();
            return result;
        }
        return false;
    }

    @Override
    public boolean doesContainerContain(AspectList paramAspectList) {
        if (this.isValid()) {
            this.syncToParent();
            boolean result = this.parent.doesContainerContain(paramAspectList);
            this.syncFromParent();
            return result;
        }
        return false;
    }

    @Override
    public int containerContains(Aspect paramAspect) {
        if (this.isValid()) {
            this.syncToParent();
            int result = this.parent.containerContains(paramAspect);
            this.syncFromParent();
            return result;
        }
        return 0;
    }

    @Override
    public boolean isConnectable(ForgeDirection face) {
        if (this.isValid()) {
            this.syncToParent();
            return this.parent.isConnectable(this.changeDirection(face));
        }
        return false;
    }

    @Override
    public boolean canInputFrom(ForgeDirection face) {
        if (this.isValid()) {
            this.syncToParent();
            boolean result = this.parent.canInputFrom(this.changeDirection(face));
            this.syncFromParent();
            return result;
        }
        return false;
    }

    @Override
    public boolean canOutputTo(ForgeDirection face) {
        if (this.isValid()) {
            this.syncToParent();
            boolean result = this.parent.canOutputTo(this.changeDirection(face));
            this.syncFromParent();
            return result;
        }
        return false;
    }

    public ForgeDirection changeDirection(ForgeDirection face) {
        if (this.placedOn == ForgeDirection.UP) {
            if (face == ForgeDirection.UP || face == ForgeDirection.DOWN) {
                return face.getOpposite();
            }
            return face;
        }

        if (this.placedOn == ForgeDirection.DOWN) {
            return face;
        }

        if (face == ForgeDirection.UP) {
            return ForgeDirection.NORTH;
        }
        if (face == ForgeDirection.DOWN) {
            return ForgeDirection.SOUTH;
        }
        if (face == this.placedOn) {
            return ForgeDirection.DOWN;
        }
        if (face == this.placedOn.getOpposite()) {
            return ForgeDirection.UP;
        }

        switch (this.placedOn) {
            case EAST:
                return face == ForgeDirection.NORTH ? ForgeDirection.WEST : ForgeDirection.EAST;
            case SOUTH:
                return face.getOpposite();
            case WEST:
                return face == ForgeDirection.SOUTH ? ForgeDirection.WEST : ForgeDirection.EAST;
            default:
                break;
        }

        return face;
    }

    @Override
    public void setSuction(Aspect paramAspect, int paramInt) {
        if (this.isValid()) {
            this.syncToParent();
            this.parent.setSuction(paramAspect, paramInt);
            this.syncFromParent();
        }
    }

    @Override
    public Aspect getSuctionType(ForgeDirection paramForgeDirection) {
        if (this.isValid()) {
            this.syncToParent();
            Aspect result = this.parent.getSuctionType(this.changeDirection(paramForgeDirection));
            this.syncFromParent();
            return result;
        }
        return null;
    }

    @Override
    public int getSuctionAmount(ForgeDirection paramForgeDirection) {
        if (this.isValid()) {
            this.syncToParent();
            int result = this.parent.getSuctionAmount(this.changeDirection(paramForgeDirection));
            this.syncFromParent();
            return result;
        }
        return 0;
    }

    @Override
    public int takeEssentia(Aspect paramAspect, int paramInt, ForgeDirection paramForgeDirection) {
        if (this.isValid()) {
            this.syncToParent();
            int result = this.parent.takeEssentia(paramAspect, paramInt, this.changeDirection(paramForgeDirection));
            this.syncFromParent();
            return result;
        }
        return paramInt;
    }

    @Override
    public int addEssentia(Aspect paramAspect, int paramInt, ForgeDirection paramForgeDirection) {
        if (this.isValid()) {
            this.syncToParent();
            int result = this.parent.addEssentia(paramAspect, paramInt, this.changeDirection(paramForgeDirection));
            this.syncFromParent();
            return result;
        }
        return paramInt;
    }

    @Override
    public Aspect getEssentiaType(ForgeDirection paramForgeDirection) {
        if (this.isValid()) {
            this.syncToParent();
            Aspect result = this.parent.getEssentiaType(this.changeDirection(paramForgeDirection));
            this.syncFromParent();
            return result;
        }
        return null;
    }

    @Override
    public int getEssentiaAmount(ForgeDirection paramForgeDirection) {
        if (this.isValid()) {
            this.syncToParent();
            int result = this.parent.getEssentiaAmount(this.changeDirection(paramForgeDirection));
            this.syncFromParent();
            return result;
        }
        return 0;
    }

    @Override
    public int getMinimumSuction() {
        if (this.isValid()) {
            this.syncToParent();
            int result = this.parent.getMinimumSuction();
            this.syncFromParent();
            return result;
        }
        return 0;
    }

    @Override
    public boolean renderExtendedTube() {
        if (this.isValid()) {
            this.syncToParent();
            boolean result = this.parent.renderExtendedTube();
            this.syncFromParent();
            return result;
        }
        return false;
    }
}
