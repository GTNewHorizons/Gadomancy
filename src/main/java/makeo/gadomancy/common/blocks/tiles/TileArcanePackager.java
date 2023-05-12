package makeo.gadomancy.common.blocks.tiles;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.ForgeDirection;

import makeo.gadomancy.common.registration.RegisteredItems;
import makeo.gadomancy.common.utils.ItemUtils;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.tiles.TileJarFillable;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p/>
 * Created by makeo @ 14.11.2015 12:22
 */
public class TileArcanePackager extends TileJarFillable implements ISidedInventory {

    private static final Aspect ASPECT = Aspect.CLOTH;

    private ItemStack[] contents = new ItemStack[12];

    // 0 - 46
    public byte progress = -1;
    public boolean autoStart;
    public boolean useEssentia;
    public boolean disguise;
    private Boolean redstoneState;

    private int count;

    public TileArcanePackager() {
        this.aspectFilter = TileArcanePackager.ASPECT;
        this.maxAmount = 8;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        this.contents = new ItemStack[this.getSizeInventory()];
        NBTTagList list = compound.getTagList("Items", 10);

        for (int i = 0; i < list.tagCount(); ++i) {
            NBTTagCompound slot = list.getCompoundTagAt(i);
            int j = slot.getByte("Slot") & 255;

            if (j >= 0 && j < this.contents.length) {
                this.contents[j] = ItemStack.loadItemStackFromNBT(slot);
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        NBTTagList list = new NBTTagList();
        for (int i = 0; i < this.contents.length; ++i) {
            if (this.contents[i] != null) {
                NBTTagCompound slot = new NBTTagCompound();
                slot.setByte("Slot", (byte) i);
                this.contents[i].writeToNBT(slot);
                list.appendTag(slot);
            }
        }
        compound.setTag("Items", list);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (this.worldObj.isRemote) {
            if (this.progress >= 0 && this.progress < 46) {
                this.progress++;
            }
        } else {
            if (this.redstoneState == null) {
                this.redstoneState = this.worldObj
                        .isBlockIndirectlyGettingPowered(this.xCoord, this.yCoord, this.zCoord);
            }

            if (this.progress >= 47) {
                this.doPack();
                this.progress = -1;
                this.markForUpdate();
                this.count = 1;
            }

            if (this.progress >= 0) {
                this.progress++;
            } else if (this.count++ % 5 == 0) {
                if (this.autoStart && this.canPack()) {
                    this.progress = 0;
                    this.markForUpdate();
                }
            }
        }
    }

    public void updateRedstone(boolean state) {
        if (this.redstoneState != null && state && !this.redstoneState) {
            if (this.canPack()) {
                this.progress = 0;
                this.markForUpdate();
            }
        }
        this.redstoneState = state;
    }

    private boolean canPack() {
        if (this.getStackInSlot(11) != null) {
            return false;
        }

        boolean check = false;
        for (int i = 0; i < 9; i++) {
            if (this.getStackInSlot(i) != null) {
                check = true;
                break;
            }
        }

        if (!check) {
            return false;
        }

        if (this.useEssentia) {
            return this.amount >= 4;
        } else {
            ItemStack leather = this.getStackInSlot(9);
            if (leather == null || leather.stackSize < 1 || leather.getItem() != Items.leather) {
                return false;
            }

            ItemStack string = this.getStackInSlot(10);
            return string != null && string.stackSize >= 1 && string.getItem() == Items.string;
        }
    }

    private void doPack() {
        if (this.canPack()) {
            List<ItemStack> contents = new ArrayList<ItemStack>();
            for (int i = 0; i < 9; i++) {
                ItemStack stack = this.getStackInSlot(i);
                if (stack != null) {
                    contents.add(stack);
                }
            }

            ItemStack pack = new ItemStack(
                    this.disguise ? RegisteredItems.itemFakeLootbag : RegisteredItems.itemPackage,
                    1,
                    this.useEssentia ? 1 : 0);
            boolean success = RegisteredItems.itemPackage.setContents(pack, contents);

            if (success) {
                if (this.useEssentia) {
                    this.amount -= 4;
                } else {
                    this.decrStackSize(9, 1);
                    this.decrStackSize(10, 1);
                }
                this.setInventorySlotContents(11, pack);

                for (int i = 0; i < 9; i++) {
                    this.setInventorySlotContents(i, null);
                }
            } else {
                this.worldObj
                        .newExplosion(null, this.xCoord + 0.5, this.yCoord + 0.5, this.zCoord + 0.5, 1, false, false);

                for (int i = 0; i < 9; i++) {
                    ItemStack stack = this.contents[i];
                    if (stack != null) {
                        EntityItem entityItem = new EntityItem(
                                this.worldObj,
                                this.xCoord + 0.5,
                                this.yCoord + (13 / 16f),
                                this.zCoord + 0.5,
                                stack);
                        ItemUtils.applyRandomDropOffset(entityItem, this.worldObj.rand);
                        this.worldObj.spawnEntityInWorld(entityItem);
                        this.contents[i] = null;
                    }
                }

                this.progress = -1;
                this.markForUpdate();
            }
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        this.aspectFilter = TileArcanePackager.ASPECT;

        this.progress = compound.getByte("progress");

        byte settings = compound.getByte("settings");
        this.autoStart = (settings & 1) == 1;
        this.useEssentia = (settings & 2) == 2;
        this.disguise = (settings & 4) == 4;
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.removeTag("AspectFilter");

        compound.setByte("progress", this.progress);

        byte settings = (byte) (this.autoStart ? 1 : 0);
        settings |= this.useEssentia ? 2 : 0;
        settings |= this.disguise ? 4 : 0;
        compound.setByte("settings", settings);
    }

    public void markForUpdate() {
        this.markDirty();
        this.getWorldObj().markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
    }

    @Override
    public int getSizeInventory() {
        return 12;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.contents[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (this.contents[slot] != null) {
            ItemStack itemstack;

            if (this.contents[slot].stackSize <= amount) {
                itemstack = this.contents[slot];
                this.contents[slot] = null;
                this.markDirty();
                return itemstack;
            } else {
                itemstack = this.contents[slot].splitStack(amount);
                if (this.contents[slot].stackSize == 0) {
                    this.contents[slot] = null;
                }
                this.markDirty();
                return itemstack;
            }
        } else {
            return null;
        }
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        this.contents[slot] = stack;
        if (stack != null && stack.stackSize > this.getInventoryStackLimit()) {
            stack.stackSize = this.getInventoryStackLimit();
        }
        this.markDirty();
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
    public String getInventoryName() {
        return "blub";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
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
        if (slot == 9) {
            return stack.getItem() == Items.leather;
        } else if (slot == 10) {
            return stack.getItem() == Items.string;
        } else return slot != 11;
    }

    private static final int[] ORIENTATION_MAPPING = { -1, -1, 0, 2, 1, 3 };

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        if (side == 0) {
            return new int[] { 11 };
        }

        if (TileArcanePackager.ORIENTATION_MAPPING[side] == super.facing) {
            return new int[] { 9, 10, 0, 1, 2, 3, 4, 5, 6, 7, 8 };
        }

        return new int[0];
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return !(this.useEssentia && (slot == 9 || slot == 10));
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return true;
    }
}
