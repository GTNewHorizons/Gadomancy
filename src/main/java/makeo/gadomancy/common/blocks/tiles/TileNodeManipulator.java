package makeo.gadomancy.common.blocks.tiles;

import java.util.ArrayList;
import java.util.Collections;
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
 * <p/>
 * Created by HellFirePvP @ 26.10.2015 20:16
 */
public class TileNodeManipulator extends TileWandPedestal implements IWandable {

    // WARNING: the ratio between manipulation cap and start affect the improvement of the node
    private static final int NODE_MANIPULATION_WORK_START = 70;
    private static final int NODE_MANIPULATION_ASPECT_CAP = 120;
    private static final int NODE_MANIPULATION_TICKS = 300;

    // There is no change for having more than the required aspects to form a portal
    private static final int ELDRITCH_PORTAL_ASPECT_REQUIREMENT = 120;
    private static final int ELDRITCH_PORTAL_TICKS = 400;

    private static final int REQUIRED_ELDRITCH_PEDESTALS = 4;

    private static final Vec3[] PILLAR_OFFSETS = { Vec3.createVectorHelper(0.7, -0.6, 0.7),
            Vec3.createVectorHelper(-0.7, -0.6, 0.7), Vec3.createVectorHelper(-0.7, -0.6, -0.7),
            Vec3.createVectorHelper(0.7, -0.6, -0.7), };

    private MultiblockType multiblockType;
    private boolean isMultiblock;

    private AspectList workAspectList = new AspectList();
    private boolean isWorking;
    private int workTick;

    private final List<ChunkCoordinates> bufferedCCPedestals = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Update loop
    // -------------------------------------------------------------------------

    @Override
    public void updateEntity() {
        if (this.worldObj.isRemote) return;

        if (!this.isInMultiblock()) return;

        this.multiblockTick();
    }

    private void multiblockTick() {
        if (!this.isMultiblockPresent()) {
            this.breakMultiblock();
            this.isMultiblock = false;
            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
            this.markDirty();
            return;
        }

        switch (this.multiblockType) {
            case NODE_MANIPULATOR:
                if (!this.isWorking) {
                    this.doAspectChecks(NODE_MANIPULATION_ASPECT_CAP, NODE_MANIPULATION_WORK_START);
                } else if (this.workTick > 0 || this.hasNode()) {
                    this.manipulationTick();
                }
                break;

            case E_PORTAL_CREATOR:
                if (!this.isWorking) {
                    this.doAspectChecks(ELDRITCH_PORTAL_ASPECT_REQUIREMENT, ELDRITCH_PORTAL_ASPECT_REQUIREMENT);
                } else if (this.workTick > 0 || (this.hasNode() && this.checkPedestalsAndEyes())) {
                    this.eldritchPortalCreatorTick();
                }
                break;
        }
    }

    private boolean isMultiblockPresent() {
        if (multiblockType == null) return false;

        switch (this.multiblockType) {
            case NODE_MANIPULATOR:
                if (MultiblockHelper.isMultiblockPresent(
                        this.worldObj,
                        this.xCoord,
                        this.yCoord,
                        this.zCoord,
                        RegisteredMultiblocks.completeNodeManipulatorMultiblock)) {
                    return true;
                }
                break;
            case E_PORTAL_CREATOR:
                if (MultiblockHelper.isMultiblockPresent(
                        this.worldObj,
                        this.xCoord,
                        this.yCoord,
                        this.zCoord,
                        RegisteredMultiblocks.completeEldritchPortalCreator)) {
                    return true;
                }
                break;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Node Randomization
    // -------------------------------------------------------------------------

    private void manipulationTick() {
        if (!this.hasNode()) {
            // node was removed mid-operation; abort
            this.resetWorkState();
            return;
        }

        this.workTick++;
        this.manipulationAnimationTick();
    }

    private void manipulationAnimationTick() {
        if (this.workTick < NODE_MANIPULATION_TICKS) {
            // Periodic rune animation
            if (this.workTick % 16 == 0) {
                this.sendRuneAnimation(this.xCoord, this.yCoord, this.zCoord, (byte) 0);
            }
            // Random bolt FX toward one of the four pillars
            if (this.worldObj.rand.nextInt(4) == 0) {
                Vec3 rel = PILLAR_OFFSETS[this.worldObj.rand.nextInt(4)];
                this.sendNodeBolt(
                        this.xCoord + 0.5F,
                        this.yCoord + 2.5F,
                        this.zCoord + 0.5F,
                        (float) (this.xCoord + 0.5F + rel.xCoord),
                        (float) (this.yCoord + 2.5F + rel.yCoord),
                        (float) (this.zCoord + 0.5F + rel.zCoord),
                        0);
            }
        } else {
            this.finishManipulation();
        }
    }

    private void finishManipulation() {
        float overSized = this.calcOversize();
        this.resetWorkState();

        TileEntity te = this.worldObj.getTileEntity(this.xCoord, this.yCoord + 2, this.zCoord);
        if (!(te instanceof INode node)) {
            return;
        }

        int areaRange = NODE_MANIPULATION_ASPECT_CAP - NODE_MANIPULATION_WORK_START;
        int improvementChance = (int) (overSized / (float) areaRange * 100);

        NodeManipulatorResult result;
        do {
            result = NodeManipulatorResultHandler.getRandomResult(this.worldObj, node, improvementChance);
        } while (!result.affect(this.worldObj, node));

        PacketHandler.INSTANCE.sendToAllAround(
                new PacketStartAnimation(
                        PacketStartAnimation.ID_SPARKLE_SPREAD,
                        this.xCoord,
                        this.yCoord + 2,
                        this.zCoord),
                this.getTargetPoint(32));

        this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
        this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord + 2, this.zCoord);
        this.markDirty();
        ((TileEntity) node).markDirty();
    }

    /**
     * Returns the average amount by which the collected aspects exceed the minimum work threshold, used to scale the
     * probability of getting a better manipulation result.
     */
    private float calcOversize() {
        int total = 0;
        for (Aspect a : Aspect.getPrimalAspects()) {
            total += this.workAspectList.getAmount(a) - TileNodeManipulator.NODE_MANIPULATION_WORK_START;
        }
        return total / 6F;
    }

    // -------------------------------------------------------------------------
    // Node Redirection
    // -------------------------------------------------------------------------

    private void eldritchPortalCreatorTick() {
        if (!this.checkPedestalsAndEyes() || !this.hasNode()) {
            // Eyes or node were removed mid-operation; abort
            this.resetWorkState();
            return;
        }

        this.workTick++;
        this.eldritchPortalAnimationTick();
    }

    private void eldritchPortalAnimationTick() {
        if (this.workTick >= ELDRITCH_PORTAL_TICKS) {
            this.finishEldritchPortal();
            return;
        }

        // Rune animation at the manipulator every 16 ticks
        if ((this.workTick & 15) == 0) {
            this.sendRuneAnimation(this.xCoord, this.yCoord, this.zCoord, (byte) 1);
        }
        // Rune animation cycling through the four pedestals every 8 ticks
        if ((this.workTick & 7) == 0) {
            int index = (this.workTick >> 3) & 3;
            if (index < this.bufferedCCPedestals.size()) {
                ChunkCoordinates cc = this.bufferedCCPedestals.get(index);
                this.sendRuneAnimation(cc.posX, cc.posY, cc.posZ, (byte) 1);
            }
        }
        // Random bolt FX toward a pillar
        if (this.worldObj.rand.nextBoolean()) {
            Vec3 rel = PILLAR_OFFSETS[this.worldObj.rand.nextInt(4)];
            this.sendNodeBolt(
                    this.xCoord + 0.5F,
                    this.yCoord + 2.5F,
                    this.zCoord + 0.5F,
                    (float) (this.xCoord + 0.5F + rel.xCoord),
                    (float) (this.yCoord + 2.5F + rel.yCoord),
                    (float) (this.zCoord + 0.5F + rel.zCoord),
                    2);
        }
        // Random bolt FX toward a pedestal
        if (this.worldObj.rand.nextInt(4) == 0) {
            Vec3 relPed = this.getPedestalOffset(this.worldObj.rand.nextInt(4));
            this.sendNodeBolt(
                    this.xCoord + 0.5F,
                    this.yCoord + 2.5F,
                    this.zCoord + 0.5F,
                    (float) (this.xCoord + 0.5F - relPed.xCoord),
                    (float) (this.yCoord + 1.5 + relPed.yCoord),
                    (float) (this.zCoord + 0.5F - relPed.zCoord),
                    2);
        }
    }

    private void finishEldritchPortal() {
        this.resetWorkState();

        TileEntity te = this.worldObj.getTileEntity(this.xCoord, this.yCoord + 2, this.zCoord);
        if (!(te instanceof INode)) return;

        this.consumeEldritchEyes();

        this.worldObj.removeTileEntity(this.xCoord, this.yCoord + 2, this.zCoord);
        this.worldObj.setBlockToAir(this.xCoord, this.yCoord + 2, this.zCoord);
        this.worldObj
                .setBlock(this.xCoord, this.yCoord + 2, this.zCoord, RegisteredBlocks.blockAdditionalEldrichPortal);

        this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
        this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord + 2, this.zCoord);
        this.markDirty();
    }

    private boolean checkPedestalsAndEyes() {
        if (this.bufferedCCPedestals.size() >= REQUIRED_ELDRITCH_PEDESTALS) {
            for (ChunkCoordinates cc : this.bufferedCCPedestals) {
                TileEntity te = this.worldObj.getTileEntity(cc.posX, cc.posY, cc.posZ);
                if (!(te instanceof TilePedestal) || !pedestalHasEldritchEye((TilePedestal) te)) {
                    this.bufferedCCPedestals.clear();
                    return false;
                }
            }
            return true;
        }

        this.bufferedCCPedestals.clear();

        outer: for (int xDiff = -8; xDiff <= 8; xDiff++) {
            for (int zDiff = -8; zDiff <= 8; zDiff++) {
                for (int yDiff = -5; yDiff <= 10; yDiff++) {
                    int itX = this.xCoord + xDiff;
                    int itY = this.yCoord + yDiff;
                    int itZ = this.zCoord + zDiff;

                    Block block = this.worldObj.getBlock(itX, itY, itZ);
                    int meta = this.worldObj.getBlockMetadata(itX, itY, itZ);
                    TileEntity te = this.worldObj.getTileEntity(itX, itY, itZ);

                    boolean isPedestal = block != null && block.equals(RegisteredBlocks.blockStoneMachine)
                            && meta == 1
                            && te instanceof TilePedestal
                            && this.pedestalHasEldritchEye((TilePedestal) te);

                    if (isPedestal) {
                        this.bufferedCCPedestals.add(new ChunkCoordinates(itX, itY, itZ));
                        if (this.bufferedCCPedestals.size() >= REQUIRED_ELDRITCH_PEDESTALS) {
                            break outer;
                        }
                        break; // Only one valid pedestal per Y column
                    }
                }
            }
        }

        return this.bufferedCCPedestals.size() >= REQUIRED_ELDRITCH_PEDESTALS;
    }

    private boolean pedestalHasEldritchEye(TilePedestal pedestal) {
        ItemStack stack = pedestal.getStackInSlot(0);
        return stack != null && stack.getItem() == ConfigItems.itemEldritchObject && stack.getItemDamage() == 0;
    }

    private void consumeEldritchEyes() {
        NetworkRegistry.TargetPoint target = this.getTargetPoint(32);
        for (ChunkCoordinates cc : this.bufferedCCPedestals) {
            TileEntity te = this.worldObj.getTileEntity(cc.posX, cc.posY, cc.posZ);
            if (!(te instanceof TilePedestal)) continue;
            ((TilePedestal) te).setInventorySlotContents(0, null);
            PacketHandler.INSTANCE.sendToAllAround(
                    new PacketStartAnimation(PacketStartAnimation.ID_SPARKLE_SPREAD, cc.posX, cc.posY, cc.posZ),
                    target);
        }
    }

    // -------------------------------------------------------------------------
    // Wand Handling
    // -------------------------------------------------------------------------

    private void doAspectChecks(int aspectCap, int possibleWorkStart) {
        if (this.canDrainFromWand(aspectCap)) {
            Aspect drained = this.drainAspectFromWand(aspectCap);
            if (drained != null) {
                this.playAspectDrainFX(drained);
                this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
                this.markDirty();
            }
        } else {
            this.checkIfEnoughVis(possibleWorkStart);
        }
    }

    private void checkIfEnoughVis(int requiredPerAspect) {
        for (Aspect a : Aspect.getPrimalAspects()) {
            if (this.workAspectList.getAmount(a) < requiredPerAspect) return;
        }
        this.isWorking = true;
    }

    private boolean canDrainFromWand(int cap) {
        ItemStack stack = this.getStackInSlot(0);
        if (!(stack != null && stack.getItem() instanceof ItemWandCasting)) return false;

        AspectList aspects = ((ItemWandCasting) stack.getItem()).getAllVis(stack);
        for (Aspect a : Aspect.getPrimalAspects()) {
            if (aspects.getAmount(a) >= 100 && this.workAspectList.getAmount(a) < cap) return true;
        }
        return false;
    }

    private Aspect drainAspectFromWand(int cap) {
        ItemStack stack = this.getStackInSlot(0);
        if (!(stack != null && stack.getItem() instanceof ItemWandCasting wand)) return null;

        AspectList aspects = wand.getAllVis(stack);

        List<Aspect> shuffled = new ArrayList<>(Aspect.getPrimalAspects());
        Collections.shuffle(shuffled);

        for (Aspect a : shuffled) {
            if (aspects.getAmount(a) >= 100 && this.workAspectList.getAmount(a) < cap) {
                wand.storeVis(stack, a, aspects.getAmount(a) - 100);
                this.workAspectList.add(a, 1);
                return a;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Breaking and Forming Multiblock
    // -------------------------------------------------------------------------

    public void formMultiblock() {
        if (this.multiblockType == null) return;

        MultiblockHelper.MultiblockPattern toBuild;
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

        for (Map.Entry<MultiblockHelper.IntVec3, MultiblockHelper.BlockInfo> entry : toBuild.entrySet()) {
            MultiblockHelper.IntVec3 v = entry.getKey();
            MultiblockHelper.BlockInfo info = entry.getValue();

            if (this.shouldSkip(info)) continue;

            int absX = v.x + this.xCoord;
            int absY = v.y + this.yCoord;
            int absZ = v.z + this.zCoord;

            this.worldObj.setBlock(absX, absY, absZ, Blocks.air, 0, 0);
            this.worldObj.setBlock(absX, absY, absZ, info.block, info.meta, 0);
            this.worldObj.markBlockForUpdate(absX, absY, absZ);
        }

        NetworkRegistry.TargetPoint target = this.getTargetPoint(32);

        this.orientAndAnimatePillar(this.xCoord + 1, this.yCoord, this.zCoord + 1, (byte) 5, target);
        this.orientAndAnimatePillar(this.xCoord + 1, this.yCoord, this.zCoord - 1, (byte) 4, target);
        this.orientAndAnimatePillar(this.xCoord - 1, this.yCoord, this.zCoord + 1, (byte) 3, target);
        PacketHandler.INSTANCE.sendToAllAround(
                new PacketStartAnimation(PacketStartAnimation.ID_RUNES, this.xCoord - 1, this.yCoord, this.zCoord - 1),
                target);

        this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
        this.markDirty();
        this.isMultiblock = true;
    }

    public void breakMultiblock() {
        if (this.multiblockType == null) {
            this.resetWorkState();
            this.dropWand();
            return;
        }

        MultiblockHelper.MultiblockPattern complete;
        MultiblockHelper.MultiblockPattern incomplete;

        switch (this.multiblockType) {
            case NODE_MANIPULATOR:
                complete = RegisteredMultiblocks.completeNodeManipulatorMultiblock;
                incomplete = RegisteredMultiblocks.incompleteNodeManipulatorMultiblock;
                break;
            case E_PORTAL_CREATOR:
                complete = RegisteredMultiblocks.completeEldritchPortalCreator;
                incomplete = RegisteredMultiblocks.incompleteEldritchPortalCreator;
                break;
            default:
                return;
        }

        for (Map.Entry<MultiblockHelper.IntVec3, MultiblockHelper.BlockInfo> entry : complete.entrySet()) {
            MultiblockHelper.IntVec3 v = entry.getKey();
            MultiblockHelper.BlockInfo info = entry.getValue();
            MultiblockHelper.BlockInfo restoreInfo = incomplete.get(v);

            if (this.shouldSkip(info)) continue;

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

        this.multiblockType = null;
        this.resetWorkState();
        this.dropWand();
    }

    private boolean shouldSkip(MultiblockHelper.BlockInfo info) {
        return info.block == RegisteredBlocks.blockNode || info.block == Blocks.air
                || info.block == RegisteredBlocks.blockNodeManipulator
                || (info.block == RegisteredBlocks.blockStoneMachine
                        && (info.meta == 0 || info.meta == 1 || info.meta == 3));
    }

    private void orientAndAnimatePillar(int x, int y, int z, byte orientation, NetworkRegistry.TargetPoint target) {
        TileManipulatorPillar pillar = (TileManipulatorPillar) this.worldObj.getTileEntity(x, y, z);
        pillar.setOrientation(orientation);
        PacketHandler.INSTANCE.sendToAllAround(
                new PacketStartAnimation(PacketStartAnimation.ID_RUNES, pillar.xCoord, pillar.yCoord, pillar.zCoord),
                target);
    }

    // -------------------------------------------------------------------------
    // Helpers / Other
    // -------------------------------------------------------------------------

    public MultiblockType detectMultiblockType() {
        if (MultiblockHelper.isMultiblockPresent(
                this.worldObj,
                this.xCoord,
                this.yCoord,
                this.zCoord,
                RegisteredMultiblocks.incompleteNodeManipulatorMultiblock)) {
            this.multiblockType = MultiblockType.NODE_MANIPULATOR;
            return MultiblockType.NODE_MANIPULATOR;
        }
        if (MultiblockHelper.isMultiblockPresent(
                this.worldObj,
                this.xCoord,
                this.yCoord,
                this.zCoord,
                RegisteredMultiblocks.incompleteEldritchPortalCreator)) {
            this.multiblockType = MultiblockType.E_PORTAL_CREATOR;
            return MultiblockType.E_PORTAL_CREATOR;
        }
        this.multiblockType = null;
        return null;
    }

    private boolean hasNode() {
        return this.worldObj.getBlock(this.xCoord, this.yCoord + 2, this.zCoord) == RegisteredBlocks.blockNode;
    }

    private void resetWorkState() {
        this.workAspectList = new AspectList();
        this.workTick = 0;
        this.isWorking = false;
        this.markDirty();
        this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
    }

    private void dropWand() {
        if (this.getStackInSlot(0) != null) {
            InventoryUtils.dropItems(this.worldObj, this.xCoord, this.yCoord, this.zCoord);
        }
    }

    private Vec3 getPedestalOffset(int index) {
        if (index < this.bufferedCCPedestals.size()) {
            ChunkCoordinates cc = this.bufferedCCPedestals.get(index);
            return Vec3.createVectorHelper(this.xCoord - cc.posX, this.yCoord - cc.posY, this.zCoord - cc.posZ);
        }
        return Vec3.createVectorHelper(0, 0, 0);
    }

    private void sendRuneAnimation(int x, int y, int z, byte flags) {
        PacketHandler.INSTANCE.sendToAllAround(
                new PacketStartAnimation(PacketStartAnimation.ID_RUNES, x, y, z, flags),
                this.getTargetPoint(32));
    }

    private void sendNodeBolt(float x1, float y1, float z1, float x2, float y2, float z2, int type) {
        PacketHandler.INSTANCE
                .sendToAllAround(new PacketTCNodeBolt(x1, y1, z1, x2, y2, z2, type, false), this.getTargetPoint(32));
    }

    private void playAspectDrainFX(Aspect drained) {
        PacketHandler.INSTANCE.sendToAllAround(
                new PacketTCWispyLine(
                        this.worldObj.provider.dimensionId,
                        this.xCoord + 0.5,
                        this.yCoord + 0.8,
                        this.zCoord + 0.5,
                        this.xCoord + 0.5,
                        this.yCoord + 1.4 + (this.worldObj.rand.nextInt(4) / 10D),
                        this.zCoord + 0.5,
                        40,
                        drained.getColor()),
                this.getTargetPoint(32));
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        NBTTagCompound tag = compound.getCompoundTag("Gadomancy");
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

    public MultiblockType getMultiblockType() {
        return this.multiblockType;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return this.isInMultiblock() && super.canInsertItem(slot, stack, side);
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
    public boolean doesContainerAccept(Aspect aspect) {
        return false;
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

        private final String research;
        private final AspectList costs;

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
