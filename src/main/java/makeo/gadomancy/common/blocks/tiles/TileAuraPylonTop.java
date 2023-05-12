package makeo.gadomancy.common.blocks.tiles;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import makeo.gadomancy.client.effect.EffectHandler;
import makeo.gadomancy.client.effect.fx.Orbital;
import makeo.gadomancy.common.registration.RegisteredBlocks;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by HellFirePvP @ 13.11.2015 00:01
 */
public class TileAuraPylonTop extends SynchronizedTileEntity implements IAspectContainer {

    public Orbital orbital;
    private boolean shouldRender;
    private boolean shouldRenderAura;

    @Override
    public void updateEntity() {
        TileEntity te = this.worldObj.getTileEntity(this.xCoord, this.yCoord - 1, this.zCoord);
        if (!this.worldObj.isRemote) {
            if (te == null || !(te instanceof TileAuraPylon)) {
                this.breakTile();
                return;
            }
            if (this.worldObj.getBlock(this.xCoord, this.yCoord - 1, this.zCoord) != RegisteredBlocks.blockAuraPylon
                    || this.worldObj.getBlockMetadata(this.xCoord, this.yCoord - 1, this.zCoord) != 0) {
                this.breakTile();
                return;
            }
            TileAuraPylon pylon = (TileAuraPylon) te;
            if (pylon.isPartOfMultiblock() && !pylon.isMasterTile()) this.breakTile();
        } else {
            this.shouldRender = te != null && te instanceof TileAuraPylon && ((TileAuraPylon) te).isPartOfMultiblock();
            this.shouldRenderAura = te != null && te instanceof TileAuraPylon
                    && ((TileAuraPylon) te).getEssentiaAmount() > 0;
        }
    }

    public Aspect getAspect() {
        TileEntity te = this.worldObj.getTileEntity(this.xCoord, this.yCoord - 1, this.zCoord);
        if (te == null || !(te instanceof TileAuraPylon)) {
            return null;
        }
        return ((TileAuraPylon) te).getAspectType();
    }

    private void breakTile() {
        int meta = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
        Block pylon = this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord);
        if (pylon != null) {
            ArrayList<ItemStack> stacks = pylon.getDrops(this.worldObj, this.xCoord, this.yCoord, this.zCoord, meta, 0);
            for (ItemStack i : stacks) {
                EntityItem item = new EntityItem(this.worldObj, this.xCoord, this.yCoord, this.zCoord, i);
                this.worldObj.spawnEntityInWorld(item);
            }
        }
        this.worldObj.removeTileEntity(this.xCoord, this.yCoord, this.zCoord);
        this.worldObj.setBlockToAir(this.xCoord, this.yCoord, this.zCoord);
        this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
    }

    public boolean shouldRenderEffect() {
        return this.shouldRender;
    }

    public boolean shouldRenderAuraEffect() {
        return this.shouldRenderAura;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (this.orbital != null) {
            this.orbital.clearOrbitals();
            if (this.orbital.registered) EffectHandler.getInstance().unregisterOrbital(this.orbital);
        }
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public AspectList getAspects() {
        TileEntity master = this.worldObj.getTileEntity(this.xCoord, this.yCoord - 1, this.zCoord);
        if (master == null || !(master instanceof TileAuraPylon)) return null;
        return ((TileAuraPylon) master).getAspects();
    }

    @Override
    public void setAspects(AspectList aspectList) {}

    @Override
    public boolean doesContainerAccept(Aspect aspect) {
        return false;
    }

    @Override
    public int addToContainer(Aspect aspect, int i) {
        return 0;
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int i) {
        return false;
    }

    @Override
    public boolean takeFromContainer(AspectList aspectList) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect aspect, int i) {
        return false;
    }

    @Override
    public boolean doesContainerContain(AspectList aspectList) {
        return false;
    }

    @Override
    public int containerContains(Aspect aspect) {
        return 0;
    }
}
