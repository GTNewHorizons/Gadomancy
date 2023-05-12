package makeo.gadomancy.common.blocks.tiles;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import makeo.gadomancy.common.node.ExtendedNodeType;
import makeo.gadomancy.common.utils.Injector;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.TileJarNode;
import thaumcraft.common.tiles.TileNode;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p/>
 * Original created by Azanor@Thaumcraft: thaumcraft.common.tiles.TileJar Modified to create compatibility with
 * ExtendedNodes Created by HellFirePvP @ 25.10.2015 22:50
 */
public class TileExtendedNodeJar extends TileJarNode {

    private static final ResourceLocation tex = new ResourceLocation("thaumcraft", "textures/models/jar.png");
    private AspectList aspects = new AspectList();
    private AspectList aspectsBase = new AspectList();
    private NodeType nodeType = NodeType.NORMAL;
    private NodeModifier nodeModifier;
    private ExtendedNodeType extendedNodeType;
    private NBTTagCompound behaviorSnapshot;
    private String id = "";
    public long animate;

    @Override
    public ResourceLocation getTexture() {
        return TileExtendedNodeJar.tex;
    }

    public boolean canUpdate() {
        return true;
    }

    public void readCustomNBT(NBTTagCompound nbttagcompound) {
        this.aspects.readFromNBT(nbttagcompound);
        this.id = nbttagcompound.getString("nodeId");

        AspectList al = new AspectList();
        NBTTagList tlist = nbttagcompound.getTagList("AspectsBase", 10);
        for (int j = 0; j < tlist.tagCount(); j++) {
            NBTTagCompound rs = tlist.getCompoundTagAt(j);
            if (rs.hasKey("key")) {
                al.add(Aspect.getAspect(rs.getString("key")), rs.getInteger("amount"));
            }
        }
        Short oldBase = nbttagcompound.getShort("nodeVisBase");
        this.aspectsBase = new AspectList();
        if ((oldBase > 0) && (al.size() == 0)) {
            for (Aspect a : this.aspects.getAspects()) {
                this.aspectsBase.merge(a, oldBase);
            }
        } else {
            this.aspectsBase = al.copy();
        }
        this.setNodeType(NodeType.values()[nbttagcompound.getByte("type")]);
        byte mod = nbttagcompound.getByte("modifier");
        if (mod >= 0) this.setNodeModifier(NodeModifier.values()[mod]);
        else this.setNodeModifier(null);
        byte exType = nbttagcompound.getByte("extendedNodeType");
        if (exType >= 0) this.setExtendedNodeType(ExtendedNodeType.values()[exType]);
        else this.setExtendedNodeType(null);
        if (nbttagcompound.hasKey("Behavior")) {
            this.behaviorSnapshot = nbttagcompound.getCompoundTag("Behavior");
        }
    }

    public void writeCustomNBT(NBTTagCompound nbttagcompound) {
        this.aspects.writeToNBT(nbttagcompound);
        nbttagcompound.setString("nodeId", this.id);
        NBTTagList tlist = new NBTTagList();
        nbttagcompound.setTag("AspectsBase", tlist);
        for (Aspect aspect : this.aspectsBase.getAspects()) {
            if (aspect != null) {
                NBTTagCompound f = new NBTTagCompound();
                f.setString("key", aspect.getTag());
                f.setInteger("amount", this.aspectsBase.getAmount(aspect));
                tlist.appendTag(f);
            }
        }
        nbttagcompound.setByte("type", (byte) this.getNodeType().ordinal());
        nbttagcompound
                .setByte("modifier", this.getNodeModifier() == null ? -1 : (byte) this.getNodeModifier().ordinal());
        nbttagcompound.setByte(
                "extendedNodeType",
                this.getExtendedNodeType() == null ? -1 : (byte) this.getExtendedNodeType().ordinal());
        if (this.behaviorSnapshot != null) {
            nbttagcompound.setTag("Behavior", this.behaviorSnapshot);
        }
    }

    public AspectList getAspects() {
        return this.aspects;
    }

    public AspectList getAspectsBase() {
        return this.aspectsBase;
    }

    public void setAspects(AspectList aspects) {
        this.aspects = aspects.copy();
        this.aspectsBase = aspects.copy();
    }

    public int addToContainer(Aspect tt, int am) {
        int out = 0;
        if (this.aspects.getAmount(tt) + am > this.aspectsBase.getAmount(tt)) {
            out = this.aspects.getAmount(tt) + am - this.aspectsBase.getAmount(tt);
        }
        this.aspects.add(tt, am - out);
        this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
        this.markDirty();
        return out;
    }

    public boolean takeFromContainer(Aspect tt, int am) {
        if (this.aspects.getAmount(tt) >= am) {
            this.aspects.remove(tt, am);
            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
            this.markDirty();
            return true;
        }
        return false;
    }

    public boolean takeFromContainer(AspectList ot) {
        return false;
    }

    public boolean doesContainerContainAmount(Aspect tag, int amt) {
        return this.aspects.getAmount(tag) >= amt;
    }

    public boolean doesContainerContain(AspectList ot) {
        for (Aspect tt : ot.getAspects()) {
            if (this.aspects.getAmount(tt) < ot.getAmount(tt)) {
                return false;
            }
        }
        return true;
    }

    public int containerContains(Aspect tag) {
        return this.aspects.getAmount(tag);
    }

    public NodeType getNodeType() {
        return this.nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public void setNodeModifier(NodeModifier nodeModifier) {
        this.nodeModifier = nodeModifier;
    }

    public NodeModifier getNodeModifier() {
        return this.nodeModifier;
    }

    public ExtendedNodeType getExtendedNodeType() {
        return this.extendedNodeType;
    }

    public void setExtendedNodeType(ExtendedNodeType extendedNodeType) {
        this.extendedNodeType = extendedNodeType;
    }

    public int getNodeVisBase(Aspect aspect) {
        return this.aspectsBase.getAmount(aspect);
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setBehaviorSnapshot(NBTTagCompound behaviorSnapshot) {
        this.behaviorSnapshot = behaviorSnapshot;
    }

    public NBTTagCompound getBehaviorSnapshot() {
        return this.behaviorSnapshot;
    }

    public void setNodeVisBase(Aspect aspect, short nodeVisBase) {
        if (this.aspectsBase.getAmount(aspect) < nodeVisBase) {
            this.aspectsBase.merge(aspect, nodeVisBase);
        } else {
            this.aspectsBase.reduce(aspect, this.aspectsBase.getAmount(aspect) - nodeVisBase);
        }
    }

    public boolean receiveClientEvent(int i, int j) {
        if (i == 9) {
            if (this.worldObj.isRemote) {
                for (int yy = -1; yy < 3; yy++) {
                    for (int xx = -1; xx < 2; xx++) {
                        for (int zz = -1; zz < 2; zz++) {
                            Thaumcraft.proxy.blockSparkle(
                                    this.worldObj,
                                    this.xCoord + xx,
                                    this.yCoord + yy,
                                    this.zCoord + zz,
                                    -9999,
                                    5);
                        }
                    }
                }
                this.animate = (System.currentTimeMillis() + 1000L);
            }
            return true;
        }
        return super.receiveClientEvent(i, j);
    }

    public boolean drop = true;

    public int onWandRightClick(World world, ItemStack wandstack, EntityPlayer player, int x, int y, int z, int side,
            int md) {
        if (!world.isRemote) {
            this.drop = false;
            world.setBlock(x, y, z, ConfigBlocks.blockAiry, 0, 3);
            TileExtendedNode tn = (TileExtendedNode) world.getTileEntity(x, y, z);
            if (tn != null) {
                tn.setAspects(this.getAspects());
                tn.setNodeModifier(this.getNodeModifier());
                tn.setNodeType(this.getNodeType());
                tn.setExtendedNodeType(this.getExtendedNodeType());
                new Injector(tn, TileNode.class).setField("id", this.getId());
                tn.readBehaviorSnapshot(this.getBehaviorSnapshot());
                world.markBlockForUpdate(x, y, z);
                tn.markDirty();
            }
        }
        world.playAuxSFX(2001, x, y, z, Block.getIdFromBlock(ConfigBlocks.blockJar) + 61440);
        player.worldObj.playSound(
                x + 0.5D,
                y + 0.5D,
                z + 0.5D,
                "random.glass",
                1.0F,
                0.9F + player.worldObj.rand.nextFloat() * 0.2F,
                false);
        player.swingItem();
        return 0;
    }

    public ItemStack onWandRightClick(World world, ItemStack wandstack, EntityPlayer player) {
        return null;
    }

    public void onUsingWandTick(ItemStack wandstack, EntityPlayer player, int count) {}

    public void onWandStoppedUsing(ItemStack wandstack, World world, EntityPlayer player, int count) {}

    public boolean doesContainerAccept(Aspect tag) {
        return true;
    }
}
