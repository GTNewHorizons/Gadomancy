package makeo.gadomancy.common.blocks.tiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import cpw.mods.fml.common.network.NetworkRegistry;
import makeo.gadomancy.common.Gadomancy;
import makeo.gadomancy.common.network.PacketHandler;
import makeo.gadomancy.common.network.packets.PacketStartAnimation;
import makeo.gadomancy.common.network.packets.PacketTCNodeBolt;
import makeo.gadomancy.common.network.packets.PacketTCWispyLine;
import makeo.gadomancy.common.node.NodeManipulatorResult;
import makeo.gadomancy.common.node.NodeManipulatorResultHandler;
import makeo.gadomancy.common.registration.RegisteredBlocks;
import makeo.gadomancy.common.registration.RegisteredMultiblocks;
import makeo.gadomancy.common.registration.RegisteredRecipes;
import makeo.gadomancy.common.utils.MultiblockHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.INode;
import thaumcraft.api.wands.IWandable;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.tiles.TilePedestal;
import thaumcraft.common.tiles.TileWandPedestal;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by HellFirePvP @ 26.10.2015 20:16
 */
public class TileNodeManipulator extends TileWandPedestal implements IWandable {

    private static final int NODE_MANIPULATION_POSSIBLE_WORK_START = 70;
    private static final int NODE_MANIPULATION_WORK_ASPECT_CAP = 120;

    private static final int ELDRITCH_PORTAL_CREATOR_WORK_START = 120;
    private static final int ELDRITCH_PORTAL_CREATOR_ASPECT_CAP = 150;

    // Already set when only multiblock would be present. aka is set, if 'isMultiblockPresent()' returns true.
    private MultiblockType multiblockType;
    private boolean multiblockStructurePresent;
    private boolean isMultiblock;

    private AspectList workAspectList = new AspectList();

    private List<ChunkCoordinates> bufferedCCPedestals = new ArrayList<ChunkCoordinates>();

    private boolean isWorking;
    private int workTick;

    @Override
    public void updateEntity() {

        if (this.worldObj.isRemote) return;

        if (this.isInMultiblock()) {
            this.checkMultiblockTick();
        }
        if (this.isInMultiblock()) {
            this.multiblockTick();
        }
    }

    private void multiblockTick() {
        if (this.multiblockType == null) {
            if (MultiblockHelper.isMultiblockPresent(
                    this.worldObj,
                    this.xCoord,
                    this.yCoord,
                    this.zCoord,
                    RegisteredMultiblocks.completeNodeManipulatorMultiblock)) {
                this.multiblockType = MultiblockType.NODE_MANIPULATOR;
            } else if (MultiblockHelper.isMultiblockPresent(
                    this.worldObj,
                    this.xCoord,
                    this.yCoord,
                    this.zCoord,
                    RegisteredMultiblocks.completeEldritchPortalCreator) && this.checkEldritchEyes(false)) {
                        this.multiblockType = MultiblockType.E_PORTAL_CREATOR;
                    }
        }
        if (this.multiblockType == null) {
            this.breakMultiblock();
            return;
        }
        switch (this.multiblockType) {
            case NODE_MANIPULATOR:
                if (!this.isWorking) {
                    this.doAspectChecks(
                            TileNodeManipulator.NODE_MANIPULATION_WORK_ASPECT_CAP,
                            TileNodeManipulator.NODE_MANIPULATION_POSSIBLE_WORK_START);
                } else {
                    this.manipulationTick();
                }
                break;
            case E_PORTAL_CREATOR:
                if (!this.isWorking) {
                    this.doAspectChecks(
                            TileNodeManipulator.ELDRITCH_PORTAL_CREATOR_ASPECT_CAP,
                            TileNodeManipulator.ELDRITCH_PORTAL_CREATOR_WORK_START);
                } else {
                    if (!this.checkEldritchEyes(true)) {
                        if (this.workTick > 1) {
                            this.workAspectList = new AspectList();
                            this.workTick = 0;
                            this.isWorking = false;
                            this.markDirty();
                            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
                        }
                        return;
                    }
                    this.eldritchPortalCreationTick();
                }
                break;
        }
    }

    private boolean checkEldritchEyes(boolean checkForEyes) {
        this.bufferedCCPedestals.clear();

        int validPedestalsFound = 0;

        labelIt: for (int xDiff = -8; xDiff <= 8; xDiff++) {
            labelZ: for (int zDiff = -8; zDiff <= 8; zDiff++) {
                for (int yDiff = -5; yDiff <= 10; yDiff++) {
                    int itX = this.xCoord + xDiff;
                    int itY = this.yCoord + yDiff;
                    int itZ = this.zCoord + zDiff;

                    Block block = this.worldObj.getBlock(itX, itY, itZ);
                    int meta = this.worldObj.getBlockMetadata(itX, itY, itZ);
                    TileEntity te = this.worldObj.getTileEntity(itX, itY, itZ);
                    if (block != null && block.equals(RegisteredBlocks.blockStoneMachine)
                            && meta == 1
                            && te != null
                            && te instanceof TilePedestal
                            && (!checkForEyes || this.checkTile((TilePedestal) te))) {
                        validPedestalsFound++;
                        this.bufferedCCPedestals.add(new ChunkCoordinates(itX, itY, itZ));
                        if (validPedestalsFound >= 4) {
                            break labelIt;
                        }
                        continue labelZ;
                    }
                }
            }
        }
        return validPedestalsFound >= 4;
    }

    private boolean checkTile(TilePedestal te) {
        ItemStack stack = te.getStackInSlot(0);
        return !(stack == null || stack.getItem() != ConfigItems.itemEldritchObject || stack.getItemDamage() != 0);
    }

    private void eldritchPortalCreationTick() {
        this.workTick++;
        if (this.workTick < 400) {
            if ((this.workTick & 15) == 0) {
                PacketStartAnimation packet = new PacketStartAnimation(
                        PacketStartAnimation.ID_RUNES,
                        this.xCoord,
                        this.yCoord,
                        this.zCoord,
                        (byte) 1);
                PacketHandler.INSTANCE.sendToAllAround(packet, this.getTargetPoint(32));
            }
            if ((this.workTick & 7) == 0) {
                int index = (this.workTick >> 3) & 3;
                try {
                    ChunkCoordinates cc = this.bufferedCCPedestals.get(index);
                    PacketStartAnimation packet = new PacketStartAnimation(
                            PacketStartAnimation.ID_RUNES,
                            cc.posX,
                            cc.posY,
                            cc.posZ,
                            (byte) 1);
                    PacketHandler.INSTANCE.sendToAllAround(packet, this.getTargetPoint(32));
                } catch (Exception exc) {}
            }
            if (this.worldObj.rand.nextBoolean()) {
                Vec3 rel = this.getRelPillarLoc(this.worldObj.rand.nextInt(4));
                PacketTCNodeBolt bolt = new PacketTCNodeBolt(
                        this.xCoord + 0.5F,
                        this.yCoord + 2.5F,
                        this.zCoord + 0.5F,
                        (float) (this.xCoord + 0.5F + rel.xCoord),
                        (float) (this.yCoord + 2.5F + rel.yCoord),
                        (float) (this.zCoord + 0.5F + rel.zCoord),
                        2,
                        false);
                PacketHandler.INSTANCE.sendToAllAround(bolt, this.getTargetPoint(32));
            }
            if (this.worldObj.rand.nextInt(4) == 0) {
                Vec3 relPed = this.getRelPedestalLoc(this.worldObj.rand.nextInt(4));
                PacketTCNodeBolt bolt = new PacketTCNodeBolt(
                        this.xCoord + 0.5F,
                        this.yCoord + 2.5F,
                        this.zCoord + 0.5F,
                        (float) (this.xCoord + 0.5F - relPed.xCoord),
                        (float) (this.yCoord + 1.5 + relPed.yCoord),
                        (float) (this.zCoord + 0.5F - relPed.zCoord),
                        2,
                        false);
                PacketHandler.INSTANCE.sendToAllAround(bolt, this.getTargetPoint(32));
            }
        } else {
            this.schedulePortalCreation();
        }
    }

    private void schedulePortalCreation() {
        this.workTick = 0;
        this.isWorking = false;
        this.workAspectList = new AspectList();

        TileEntity te = this.worldObj.getTileEntity(this.xCoord, this.yCoord + 2, this.zCoord);
        if (te == null || !(te instanceof INode)) return;

        this.consumeEldritchEyes();

        this.worldObj.removeTileEntity(this.xCoord, this.yCoord + 2, this.zCoord);
        this.worldObj.setBlockToAir(this.xCoord, this.yCoord + 2, this.zCoord);
        this.worldObj
                .setBlock(this.xCoord, this.yCoord + 2, this.zCoord, RegisteredBlocks.blockAdditionalEldrichPortal);

        this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
        this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord + 2, this.zCoord);
        this.markDirty();
    }

    private void consumeEldritchEyes() {
        for (ChunkCoordinates cc : this.bufferedCCPedestals) {
            try {
                TilePedestal pedestal = (TilePedestal) this.worldObj.getTileEntity(cc.posX, cc.posY, cc.posZ);
                pedestal.setInventorySlotContents(0, null);
                PacketStartAnimation packet = new PacketStartAnimation(
                        PacketStartAnimation.ID_SPARKLE_SPREAD,
                        cc.posX,
                        cc.posY,
                        cc.posZ);
                PacketHandler.INSTANCE.sendToAllAround(packet, this.getTargetPoint(32));
            } catch (Exception exc) {}
        }
    }

    private void manipulationTick() {
        this.workTick++;
        if (this.workTick < 300) {
            if (this.workTick % 16 == 0) {
                PacketStartAnimation packet = new PacketStartAnimation(
                        PacketStartAnimation.ID_RUNES,
                        this.xCoord,
                        this.yCoord,
                        this.zCoord);
                PacketHandler.INSTANCE.sendToAllAround(packet, this.getTargetPoint(32));
            }
            if (this.worldObj.rand.nextInt(4) == 0) {
                Vec3 rel = this.getRelPillarLoc(this.worldObj.rand.nextInt(4));
                PacketTCNodeBolt bolt = new PacketTCNodeBolt(
                        this.xCoord + 0.5F,
                        this.yCoord + 2.5F,
                        this.zCoord + 0.5F,
                        (float) (this.xCoord + 0.5F + rel.xCoord),
                        (float) (this.yCoord + 2.5F + rel.yCoord),
                        (float) (this.zCoord + 0.5F + rel.zCoord),
                        0,
                        false);
                PacketHandler.INSTANCE.sendToAllAround(bolt, this.getTargetPoint(32));
            }
        } else {
            this.scheduleManipulation();
        }
    }

    private Vec3 getRelPedestalLoc(int pedestalId) {
        try {
            ChunkCoordinates cc = this.bufferedCCPedestals.get(pedestalId);
            return Vec3.createVectorHelper(this.xCoord - cc.posX, this.yCoord - cc.posY, this.zCoord - cc.posZ);
        } catch (Exception exc) {}
        return Vec3.createVectorHelper(0, 0, 0);
    }

    private Vec3 getRelPillarLoc(int pillarId) {
        switch (pillarId) {
            case 0:
                return Vec3.createVectorHelper(0.7, -0.6, 0.7);
            case 1:
                return Vec3.createVectorHelper(-0.7, -0.6, 0.7);
            case 2:
                return Vec3.createVectorHelper(-0.7, -0.6, -0.7);
            case 3:
                return Vec3.createVectorHelper(0.7, -0.6, -0.7);
        }
        return Vec3.createVectorHelper(0, 0, 0);
    }

    private void scheduleManipulation() {
        float overSized = this.calcOversize(TileNodeManipulator.NODE_MANIPULATION_POSSIBLE_WORK_START);

        this.workTick = 0;
        this.isWorking = false;
        this.workAspectList = new AspectList();

        TileEntity te = this.worldObj.getTileEntity(this.xCoord, this.yCoord + 2, this.zCoord);
        if (te == null || !(te instanceof INode)) return;
        INode node = (INode) te;
        int areaRange = TileNodeManipulator.NODE_MANIPULATION_WORK_ASPECT_CAP
                - TileNodeManipulator.NODE_MANIPULATION_POSSIBLE_WORK_START;
        int percChanceForBetter = 0;
        if (areaRange > 0) {
            percChanceForBetter = (int) ((overSized / ((float) areaRange)) * 100);
        }
        NodeManipulatorResult result;
        do {
            result = NodeManipulatorResultHandler.getRandomResult(this.worldObj, node, percChanceForBetter);
        } while (!result.affect(this.worldObj, node));
        PacketStartAnimation packet = new PacketStartAnimation(
                PacketStartAnimation.ID_SPARKLE_SPREAD,
                this.xCoord,
                this.yCoord + 2,
                this.zCoord);
        PacketHandler.INSTANCE.sendToAllAround(packet, this.getTargetPoint(32));
        this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
        this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord + 2, this.zCoord);
        this.markDirty();
        ((TileEntity) node).markDirty();
    }

    private float calcOversize(int neededAspects) {
        int overall = 0;
        for (Aspect a : Aspect.getPrimalAspects()) {
            overall += this.workAspectList.getAmount(a) - neededAspects;
        }
        return ((float) overall) / 6F;
    }

    private void doAspectChecks(int aspectCap, int possibleWorkStart) {
        if (this.canDrainFromWand(aspectCap)) {
            Aspect a = this.drainAspectFromWand(aspectCap);
            if (a != null) {
                this.playAspectDrainFromWand(a);
                this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
                this.markDirty();
            }
        } else {
            this.checkIfEnoughVis(possibleWorkStart);
        }
    }

    private void checkIfEnoughVis(int start) {
        boolean enough = true;
        for (Aspect a : Aspect.getPrimalAspects()) {
            if (this.workAspectList.getAmount(a) < start) {
                enough = false;
                break;
            }
        }
        if (enough) {
            this.isWorking = true;
        }
    }

    private Aspect drainAspectFromWand(int cap) {
        ItemStack stack = this.getStackInSlot(0);
        if (stack == null || !(stack.getItem() instanceof ItemWandCasting)) return null; // Should never happen..
        AspectList aspects = ((ItemWandCasting) stack.getItem()).getAllVis(stack);
        for (Aspect a : this.getRandomlyOrderedPrimalAspectList()) {
            if (aspects.getAmount(a) >= 100 && this.workAspectList.getAmount(a) < cap) {
                int amt = aspects.getAmount(a);
                ((ItemWandCasting) stack.getItem()).storeVis(stack, a, amt - 100);
                this.workAspectList.add(a, 1);
                return a;
            }
        }
        return null;
    }

    private List<Aspect> getRandomlyOrderedPrimalAspectList() {
        ArrayList<Aspect> primals = (ArrayList<Aspect>) Aspect.getPrimalAspects().clone();
        Collections.shuffle(primals);
        return primals;
    }

    private boolean canDrainFromWand(int cap) {
        ItemStack stack = this.getStackInSlot(0);
        if (stack == null || !(stack.getItem() instanceof ItemWandCasting)) return false;
        AspectList aspects = ((ItemWandCasting) stack.getItem()).getAllVis(stack);
        for (Aspect a : Aspect.getPrimalAspects()) {
            if (aspects.getAmount(a) < 100) continue;
            if (this.workAspectList.getAmount(a) < cap) return true;
        }
        return false;
    }

    private void checkMultiblockTick() {
        this.checkMultiblock();
        if (!this.isMultiblockStructurePresent()) {
            this.breakMultiblock();
            this.isMultiblock = false;
            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
            this.markDirty();
        }
    }

    private void playAspectDrainFromWand(Aspect drained) {
        if (drained == null) return;
        NetworkRegistry.TargetPoint point = this.getTargetPoint(32);
        PacketTCWispyLine line = new PacketTCWispyLine(
                this.worldObj.provider.dimensionId,
                this.xCoord + 0.5,
                this.yCoord + 0.8,
                this.zCoord + 0.5,
                this.xCoord + 0.5,
                this.yCoord + 1.4 + (((double) this.worldObj.rand.nextInt(4)) / 10D),
                this.zCoord + 0.5,
                40,
                drained.getColor());
        PacketHandler.INSTANCE.sendToAllAround(line, point);
    }

    private void dropWand() {
        if (this.getStackInSlot(0) != null)
            InventoryUtils.dropItems(this.worldObj, this.xCoord, this.yCoord, this.zCoord);
    }

    public void breakMultiblock() {
        MultiblockHelper.MultiblockPattern compareableCompleteStructure, toRestore;
        if (this.multiblockType == null) {
            this.workAspectList = new AspectList();
            this.dropWand();
            this.workTick = 0;
            this.isWorking = false;
            return;
        }
        switch (this.multiblockType) {
            case NODE_MANIPULATOR:
                compareableCompleteStructure = RegisteredMultiblocks.completeNodeManipulatorMultiblock;
                toRestore = RegisteredMultiblocks.incompleteNodeManipulatorMultiblock;
                break;
            case E_PORTAL_CREATOR:
                compareableCompleteStructure = RegisteredMultiblocks.completeEldritchPortalCreator;
                toRestore = RegisteredMultiblocks.incompleteEldritchPortalCreator;
                break;
            default:
                return;
        }
        for (MultiblockHelper.IntVec3 v : compareableCompleteStructure.keySet()) {
            MultiblockHelper.BlockInfo info = compareableCompleteStructure.get(v);
            MultiblockHelper.BlockInfo restoreInfo = toRestore.get(v);
            if (info.block == RegisteredBlocks.blockNode
                    || (info.block == RegisteredBlocks.blockStoneMachine && (info.meta == 0 || info.meta == 3))
                    || info.block == Blocks.air
                    || info.block == RegisteredBlocks.blockNodeManipulator)
                continue;
            int absX = v.x + this.xCoord;
            int absY = v.y + this.yCoord;
            int absZ = v.z + this.zCoord;
            if (this.worldObj.getBlock(absX, absY, absZ) == info.block
                    && this.worldObj.getBlockMetadata(absX, absY, absZ) == info.meta) {
                this.worldObj.setBlock(absX, absY, absZ, Blocks.air, 0, 0);
                this.worldObj.setBlock(absX, absY, absZ, restoreInfo.block, restoreInfo.meta, 0);
                this.worldObj.markBlockForUpdate(absX, absY, absZ);
            }
        }

        this.workAspectList = new AspectList();
        this.multiblockType = null;
        this.dropWand();
        this.workTick = 0;
        this.isWorking = false;
    }

    public void formMultiblock() {
        MultiblockHelper.MultiblockPattern toBuild;
        if (this.multiblockType == null) return;
        switch (this.multiblockType) {
            case NODE_MANIPULATOR:
                toBuild = RegisteredMultiblocks.completeNodeManipulatorMultiblock;
                break;
            case E_PORTAL_CREATOR:
                toBuild = RegisteredMultiblocks.completeEldritchPortalCreator;
                break;
            default:
                return;
        }
        for (MultiblockHelper.IntVec3 v : toBuild.keySet()) {
            MultiblockHelper.BlockInfo info = toBuild.get(v);
            if (info.block == RegisteredBlocks.blockNode
                    || (info.block == RegisteredBlocks.blockStoneMachine
                            && (info.meta == 0 || info.meta == 1 || info.meta == 3))
                    || info.block == Blocks.air
                    || info.block == RegisteredBlocks.blockNodeManipulator)
                continue;
            int absX = v.x + this.xCoord;
            int absY = v.y + this.yCoord;
            int absZ = v.z + this.zCoord;
            this.worldObj.setBlock(absX, absY, absZ, Blocks.air, 0, 0);
            this.worldObj.setBlock(absX, absY, absZ, info.block, info.meta, 0);
            this.worldObj.markBlockForUpdate(absX, absY, absZ);
        }
        NetworkRegistry.TargetPoint target = this.getTargetPoint(32);
        TileManipulatorPillar pillar = (TileManipulatorPillar) this.worldObj
                .getTileEntity(this.xCoord + 1, this.yCoord, this.zCoord + 1); // wrong
        pillar.setOrientation((byte) 5);
        PacketStartAnimation animation = new PacketStartAnimation(
                PacketStartAnimation.ID_RUNES,
                pillar.xCoord,
                pillar.yCoord,
                pillar.zCoord);
        PacketHandler.INSTANCE.sendToAllAround(animation, target);
        TileManipulatorPillar pillar2 = (TileManipulatorPillar) this.worldObj
                .getTileEntity(this.xCoord - 1, this.yCoord, this.zCoord + 1);
        pillar2.setOrientation((byte) 3);
        animation = new PacketStartAnimation(
                PacketStartAnimation.ID_RUNES,
                pillar2.xCoord,
                pillar2.yCoord,
                pillar2.zCoord);
        PacketHandler.INSTANCE.sendToAllAround(animation, target);
        TileManipulatorPillar pillar3 = (TileManipulatorPillar) this.worldObj
                .getTileEntity(this.xCoord + 1, this.yCoord, this.zCoord - 1); // wrong
        pillar3.setOrientation((byte) 4);
        animation = new PacketStartAnimation(
                PacketStartAnimation.ID_RUNES,
                pillar3.xCoord,
                pillar3.yCoord,
                pillar3.zCoord);
        PacketHandler.INSTANCE.sendToAllAround(animation, target);
        animation = new PacketStartAnimation(
                PacketStartAnimation.ID_RUNES,
                this.xCoord - 1,
                this.yCoord,
                this.zCoord - 1);
        PacketHandler.INSTANCE.sendToAllAround(animation, target);
        this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
        this.markDirty();
        this.isMultiblock = true;
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);

        NBTTagCompound tag = compound.getCompoundTag("Gadomancy");
        this.multiblockStructurePresent = tag.getBoolean("mBlockPresent");
        this.isMultiblock = tag.getBoolean("mBlockState");
        this.isWorking = tag.getBoolean("manipulating");
        this.workTick = tag.getInteger("workTick");
        if (tag.hasKey("multiblockType")) {
            this.multiblockType = MultiblockType.values()[tag.getInteger("multiblockType")];
        }
        this.workAspectList.readFromNBT(tag, "workAspectList");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);

        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("mBlockPresent", this.multiblockStructurePresent);
        tag.setBoolean("mBlockState", this.isMultiblock);
        tag.setBoolean("manipulating", this.isWorking);
        tag.setInteger("workTick", this.workTick);
        if (this.multiblockType != null) {
            tag.setInteger("multiblockType", this.multiblockType.ordinal());
        }
        this.workAspectList.writeToNBT(tag, "workAspectList");
        compound.setTag("Gadomancy", tag);
    }

    public boolean isInMultiblock() {
        return this.isMultiblock;
    }

    public boolean isMultiblockStructurePresent() {
        return this.multiblockStructurePresent;
    }

    public MultiblockType getMultiblockType() {
        return this.multiblockType;
    }

    public boolean checkMultiblock() {
        boolean prevState = this.isMultiblockStructurePresent();
        if (prevState) { // If there is already a multiblock formed...
            if (this.isInMultiblock()) { // If we were actually in multiblock before
                if (this.multiblockType == null) {
                    if (MultiblockHelper.isMultiblockPresent(
                            this.worldObj,
                            this.xCoord,
                            this.yCoord,
                            this.zCoord,
                            RegisteredMultiblocks.completeNodeManipulatorMultiblock)) {
                        this.multiblockType = MultiblockType.NODE_MANIPULATOR;
                    } else if (MultiblockHelper.isMultiblockPresent(
                            this.worldObj,
                            this.xCoord,
                            this.yCoord,
                            this.zCoord,
                            RegisteredMultiblocks.completeEldritchPortalCreator) && this.checkEldritchEyes(false)) {
                                this.multiblockType = MultiblockType.E_PORTAL_CREATOR;
                            }
                }
                if (this.multiblockType == null) {
                    this.breakMultiblock();
                    return false;
                }
                switch (this.multiblockType) {
                    case NODE_MANIPULATOR:
                        this.setMultiblockStructurePresent(
                                MultiblockHelper.isMultiblockPresent(
                                        this.worldObj,
                                        this.xCoord,
                                        this.yCoord,
                                        this.zCoord,
                                        RegisteredMultiblocks.completeNodeManipulatorMultiblock),
                                MultiblockType.NODE_MANIPULATOR);
                        break;
                    case E_PORTAL_CREATOR:
                        this.setMultiblockStructurePresent(
                                MultiblockHelper.isMultiblockPresent(
                                        this.worldObj,
                                        this.xCoord,
                                        this.yCoord,
                                        this.zCoord,
                                        RegisteredMultiblocks.completeEldritchPortalCreator)
                                        && this.checkEldritchEyes(false),
                                MultiblockType.E_PORTAL_CREATOR);
                        break;
                }
            } else { // If we weren't in multiblock eventhough it would be possible.
                this.checkForNonExistingMultiblock();
            }
        } else { // If there was no multiblock formed before..
            this.checkForNonExistingMultiblock();
        }
        return this.isMultiblockStructurePresent();
    }

    private void setMultiblockStructurePresent(boolean present, MultiblockType type) {
        if (present) {
            this.multiblockType = type;
        }
        this.multiblockStructurePresent = present;
    }

    private void checkForNonExistingMultiblock() {
        Map<MultiblockHelper.MultiblockPattern, MultiblockType> patternMap = new HashMap<MultiblockHelper.MultiblockPattern, MultiblockType>();
        patternMap.put(RegisteredMultiblocks.incompleteEldritchPortalCreator, MultiblockType.E_PORTAL_CREATOR);
        patternMap.put(RegisteredMultiblocks.incompleteNodeManipulatorMultiblock, MultiblockType.NODE_MANIPULATOR);

        for (MultiblockHelper.MultiblockPattern pattern : patternMap.keySet()) {
            if (MultiblockHelper.isMultiblockPresent(this.worldObj, this.xCoord, this.yCoord, this.zCoord, pattern)) {
                if (pattern == RegisteredMultiblocks.incompleteEldritchPortalCreator) {
                    if (!this.checkEldritchEyes(false)) return;
                }
                this.setMultiblockStructurePresent(true, patternMap.get(pattern));
                return;
            }
        }
    }

    @Override
    public boolean canInsertItem(int par1, ItemStack par2ItemStack, int par3) {
        return this.isInMultiblock() && super.canInsertItem(par1, par2ItemStack, par3);
    }

    public NetworkRegistry.TargetPoint getTargetPoint(double radius) {
        return new NetworkRegistry.TargetPoint(
                this.worldObj.provider.dimensionId,
                this.xCoord,
                this.yCoord,
                this.zCoord,
                radius);
    }

    @Override
    public AspectList getAspects() {
        return this.workAspectList;
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

    @Override
    public int onWandRightClick(World world, ItemStack stack, EntityPlayer player, int i, int i2, int i3, int i4,
            int i5) {
        return 0;
    }

    @Override
    public ItemStack onWandRightClick(World world, ItemStack stack, EntityPlayer player) {
        return null;
    }

    @Override
    public void onUsingWandTick(ItemStack stack, EntityPlayer player, int i) {}

    @Override
    public void onWandStoppedUsing(ItemStack stack, World world, EntityPlayer player, int i) {}

    public enum MultiblockType {

        NODE_MANIPULATOR(Gadomancy.MODID.toUpperCase() + ".NODE_MANIPULATOR",
                RegisteredRecipes.costsNodeManipulatorMultiblock),
        E_PORTAL_CREATOR(Gadomancy.MODID.toUpperCase() + ".E_PORTAL_CREATOR",
                RegisteredRecipes.costsEldritchPortalCreatorMultiblock);

        private String research;
        private AspectList costs;

        MultiblockType(String research, AspectList costs) {
            this.research = research;
            this.costs = costs;
        }

        public String getResearchNeeded() {
            return this.research;
        }

        public AspectList getMultiblockCosts() {
            return this.costs;
        }
    }
}
