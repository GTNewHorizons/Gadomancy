package makeo.gadomancy.common.blocks.tiles;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

import makeo.gadomancy.api.ClickBehavior;
import makeo.gadomancy.common.Gadomancy;
import makeo.gadomancy.common.entities.fake.AdvancedFakePlayer;
import makeo.gadomancy.common.network.PacketHandler;
import makeo.gadomancy.common.network.packets.PacketStartAnimation;
import makeo.gadomancy.common.registration.RegisteredBlocks;
import makeo.gadomancy.common.utils.NBTHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraft.world.storage.SaveHandler;
import net.minecraftforge.common.util.ForgeDirection;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.foci.ItemFocusPrimal;
import thaumcraft.common.lib.research.ResearchManager;
import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by makeo @ 05.10.2015 22:32
 */
public class TileInfusionClaw extends SynchronizedTileEntity implements ISidedInventory {

    private static final UUID FAKE_UUID = UUID.fromString("b23c8c3f-d7bd-49b3-970a-8e86728bab82");

    private final AspectList aspects = new AspectList();


    private static final ItemWandCasting WAND_ITEM = (ItemWandCasting) ConfigItems.itemWandCasting;
    private static final ItemFocusPrimal WAND_FOCUS = (ItemFocusPrimal) ConfigItems.itemFocusPrimal;
    private static final AspectList MAX_WAND_COST = new AspectList().add(Aspect.WATER, 250).add(Aspect.AIR, 250)
            .add(Aspect.EARTH, 250).add(Aspect.FIRE, 250).add(Aspect.ORDER, 250).add(Aspect.ENTROPY, 250);

    private ItemInWorldManager im;
    private Boolean redstoneState;

    private int count;

    private String player;
    private ItemStack wandStack;
    private boolean isLocked;

    private int cooldown;

    @SideOnly(Side.CLIENT)
    public float lastRenderTick;

    /**
     * 0-3: heightMov sides 4-7: widthMov sides 8: sides exp. speed 9: center exp. speed 10: rotation center 11: primal
     * orb offset
     */
    @SideOnly(Side.CLIENT)
    public float[] animationStates;

    public TileInfusionClaw() {
        if (Gadomancy.proxy.getSide() == Side.CLIENT) {
            this.animationStates = new float[12];
            EntityLivingBase entity = Minecraft.getMinecraft().renderViewEntity;
            this.lastRenderTick = entity == null ? 0 : entity.ticksExisted;
        }
    }

    @Override
    public void updateEntity() {
        World world = this.getWorldObj();

        if (!world.isRemote) {
            if (this.redstoneState == null) {
                this.redstoneState = world.isBlockIndirectlyGettingPowered(this.xCoord, this.yCoord, this.zCoord);
            }

            if (this.cooldown > 0) {
                this.cooldown--;
                if (this.cooldown == (int) (7.5f * 20)) {
                    this.performClickBlock();
                }
            }

            if (this.count > 20) {
                this.count = 0;
                if (this.yCoord > 0) {
                    // Comparator...
                    world.notifyBlockChange(this.xCoord, this.yCoord, this.zCoord, this.getBlockType());
                }
                this.consumeVisFromNetwork();
                this.chargeWand();
            }
            this.count++;
        }
    }

    private void consumeVisFromNetwork() {
        for (Aspect aspect : Aspect.getPrimalAspects()) {
            if (aspects.getAmount(aspect) < 1000) {
                int drain = VisNetHandler.drainVis(this.worldObj, this.xCoord, this.yCoord, this.zCoord, aspect, 50);
                if (drain > 0) {
                    this.aspects.add(aspect, drain*20);
                }
            }
        }
    }

    private void chargeWand() {
        if (this.wandStack != null && wandStack.getItem() instanceof ItemWandCasting) {
            int wandMaxVis = TileInfusionClaw.WAND_ITEM.getMaxVis(wandStack);

            for (Aspect aspect : Aspect.getPrimalAspects()) {
                int wandAspectAmount = TileInfusionClaw.WAND_ITEM.getVis(wandStack, aspect);
                int replenishAmount = Math.min(100, wandMaxVis - wandAspectAmount);
                int clawAspectAmount = aspects.getAmount(aspect);

                if (replenishAmount > 0 && clawAspectAmount >= replenishAmount) {
                    TileInfusionClaw.WAND_ITEM.storeVis(this.wandStack, aspect, wandAspectAmount + replenishAmount);
                    aspects.reduce(aspect, replenishAmount);
                }
            }
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        if (compound.hasKey("player")) {
            this.player = compound.getString("player");
        }
        this.wandStack = NBTHelper.getStack(compound, "wandStack");
        this.isLocked = compound.getBoolean("isLocked");
        this.aspects.readFromNBT(compound);
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        if (this.hasOwner()) {
            compound.setString("player", this.player);
        }
        if (this.wandStack != null) {
            NBTHelper.setStack(compound, "wandStack", this.wandStack);
        }
        compound.setBoolean("isLocked", this.isLocked);
        this.aspects.writeToNBT(compound);
    }

    private void markForUpdate() {
        this.markDirty();
        this.getWorldObj().markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
    }

    public void updateRedstone(boolean state) {
        if (this.redstoneState != null && state && !this.redstoneState) {
            this.startClickBlock();
        }
        this.redstoneState = state;
    }

    public boolean isLocked() {
        return this.isLocked;
    }

    public void setIsLocked(boolean isLocked) {
        this.isLocked = isLocked;
        if (!this.getWorldObj().isRemote) {
            this.markForUpdate();
        }
    }

    public boolean setOwner(EntityPlayer player) {
        World world = this.getWorldObj();
        if (!world.isRemote && this.isValidOwner(player)) {
            this.player = player.getCommandSenderName();

            this.markForUpdate();

            return true;
        }
        return false;
    }

    public boolean isValidOwner(EntityPlayer player) {
        return !AdvancedFakePlayer.isFakePlayer(player);
    }

    private ArrayList<String> research;

    private void loadResearch(EntityPlayer fakePlayer) {
        boolean online = false;
        for (String username : MinecraftServer.getServer().getAllUsernames()) {
            if (username.equals(this.player)) {
                online = true;
                break;
            }
        }

        if (online) {
            this.research = ResearchManager.getResearchForPlayer(this.player);
        } else {
            if (this.research == null) {
                Thaumcraft.proxy.getCompletedResearch().put(fakePlayer.getCommandSenderName(), new ArrayList<String>());

                IPlayerFileData playerNBTManagerObj = MinecraftServer.getServer().worldServerForDimension(0)
                        .getSaveHandler().getSaveHandler();
                SaveHandler sh = (SaveHandler) playerNBTManagerObj;
                File dir = ObfuscationReflectionHelper
                        .getPrivateValue(SaveHandler.class, sh, "playersDirectory", "field_75771_c");
                File file1 = new File(dir, this.player + ".thaum");
                File file2 = new File(dir, this.player + ".thaumbak");
                ResearchManager.loadPlayerData(fakePlayer, file1, file2, false);

                this.research = ResearchManager.getResearchForPlayerSafe(fakePlayer.getCommandSenderName());
            }
        }

        Thaumcraft.proxy.getCompletedResearch().put(
                fakePlayer.getCommandSenderName(),
                this.research == null ? new ArrayList<String>() : this.research);
    }

    public String getOwner() {
        return this.player;
    }

    public boolean hasOwner() {
        return this.player != null;
    }

    private void startClickBlock() {
        if (!this.isRunning()) {
            ClickBehavior behavior = this
                    .getClickBehavior(this.getWorldObj(), this.xCoord, this.yCoord - 1, this.zCoord);
            if (behavior != null && (!behavior.hasVisCost() || this.hasSufficientVis())) {
                this.startRunning();
            }
        }
    }

    @SuppressWarnings("unused")
    private void performClickBlock() {
        World world = this.getWorldObj();
        int x = this.xCoord;
        int y = this.yCoord - 1;
        int z = this.zCoord;

        ClickBehavior behavior = this.getClickBehavior(world, x, y, z);
        if (behavior != null) {

            if (world == null) {
                try {
                    world = this.getWorldObj();
                } catch (NullPointerException e) {
                    makeo.gadomancy.common.Gadomancy.log.error("fatal error, world == null! at InfusionClaw");
                    return;
                }
            }

            AdvancedFakePlayer fakePlayer = new AdvancedFakePlayer((WorldServer) world, TileInfusionClaw.FAKE_UUID);
            this.loadResearch(fakePlayer);

            if (behavior.hasVisCost()) {
                if (this.hasSufficientVis()) {
                    this.consumeVis(fakePlayer);
                } else {
                    return;
                }
            }

            if (this.im == null) {
                this.im = new ItemInWorldManager(world);
            } else {
                this.im.setWorld((WorldServer) world);
            }

            if (fakePlayer == null) {
                makeo.gadomancy.common.Gadomancy.log.warn(
                        "Infusion Claw was build inside of a protected area! You need to allow FakePlayers here!");
                return;
            }

            fakePlayer.setHeldItem(this.wandStack);
            this.im.activateBlockOrUseItem(
                    fakePlayer,
                    world,
                    this.wandStack,
                    x,
                    y,
                    z,
                    ForgeDirection.UP.ordinal(),
                    0.5F,
                    0.5F,
                    0.5F);
            this.addInstability(behavior);
        }
    }

    private ClickBehavior getClickBehavior(World world, int x, int y, int z) {
        if (y >= 0 && !world.isRemote
                && world instanceof WorldServer
                && this.hasOwner()
                && !world.isAirBlock(x, y, z)
                && this.wandStack != null
                && this.wandStack.stackSize > 0) {
            return RegisteredBlocks.getClawClickBehavior(world, x, y, z);
        }
        return null;
    }

    public boolean isRunning() {
        if (this.getWorldObj().isRemote) {
            return this.animationStates[8] + this.animationStates[9] + this.animationStates[11] != 0;
        }
        return this.cooldown > 0;
    }

    private void startRunning() {
        PacketHandler.INSTANCE.sendToAllAround(
                new PacketStartAnimation(PacketStartAnimation.ID_INFUSIONCLAW, this.xCoord, this.yCoord, this.zCoord),
                new NetworkRegistry.TargetPoint(
                        this.getWorldObj().provider.dimensionId,
                        this.xCoord + 0.5,
                        this.yCoord + 0.5,
                        this.zCoord + 0.5,
                        48));
        this.cooldown = 22 * 20;
    }

    private void addInstability(ClickBehavior behavior) {
        int instability = 23;
        int maxVis = TileInfusionClaw.WAND_ITEM.getMaxVis(this.wandStack);

        instability += maxVis < 100 ? 3 : -2;

        instability -= Math.floor((maxVis > 300 ? 300 : maxVis) / 300f * 10f);
        instability -= TileInfusionClaw.WAND_ITEM.isStaff(this.wandStack) ? 6 : -1;

        instability += Math.floor((TileInfusionClaw.WAND_ITEM.getCap(this.wandStack).getBaseCostModifier() - 0.4) * 3);

        instability -= TileInfusionClaw.WAND_ITEM.getCap(this.wandStack) == ConfigItems.WAND_CAP_VOID ? 3 : 0;
        instability -= TileInfusionClaw.WAND_ITEM.getRod(this.wandStack) == ConfigItems.STAFF_ROD_PRIMAL ? 6 : 0;

        behavior.addInstability(instability);
    }

    private boolean hasSufficientVis() {
        return this.wandStack != null && this.wandStack.stackSize > 0
                && TileInfusionClaw.WAND_ITEM
                        .consumeAllVis(this.wandStack, null, TileInfusionClaw.MAX_WAND_COST, false, false);
    }

    private void consumeVis(EntityPlayer player) {
        TileInfusionClaw.WAND_ITEM.consumeAllVis(
                this.wandStack,
                player,
                TileInfusionClaw.WAND_FOCUS.getVisCost(this.wandStack),
                true,
                false);
        this.markForUpdate();
    }

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.wandStack;
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (this.isRunning()) {
            return null;
        }

        if (amount > 0) {
            ItemStack result = this.wandStack.copy();
            this.wandStack = null;
            this.markForUpdate();
            return result;
        }
        return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int p_70304_1_) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        this.wandStack = stack;
        this.markForUpdate();
    }

    @Override
    public String getInventoryName() {
        return null;
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (this.isRunning()) {
            return false;
        }

        if (stack.getItem() == TileInfusionClaw.WAND_ITEM) {
            return !TileInfusionClaw.WAND_ITEM.isSceptre(stack) && TileInfusionClaw.WAND_ITEM.getFocus(stack) == null;
        }
        return false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[] { 0 };
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return !this.isRunning() && side > 0 && side < 6;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return (!this.isLocked() || !this.hasSufficientVis()) && this.canInsertItem(slot, stack, side);
    }
}
