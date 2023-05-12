package makeo.gadomancy.common.blocks.tiles;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import makeo.gadomancy.common.registration.RegisteredBlocks;
import thaumcraft.common.lib.utils.InventoryUtils;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by makeo @ 28.09.2015 20:13
 */
public class TileArcaneDropper extends TileEntity implements ISidedInventory {

    private static final double RADIUS = 1.5;

    private List<EntityItem> items = new ArrayList<EntityItem>();

    private final List<EntityItem> dropQueue = new ArrayList<EntityItem>();

    private int count;

    private int removedCount;
    private EntityItem removedEntity;

    @Override
    public void updateEntity() {
        this.removedCount = 0;
        this.removedEntity = null;

        if (this.dropQueue.size() > 0) {
            for (int i = 0; i < this.dropQueue.size(); i++) {
                EntityItem item = this.dropQueue.get(i);
                if (!item.isDead && !this.worldObj.loadedEntityList.contains(item)) {
                    this.getWorldObj().spawnEntityInWorld(item);
                    this.dropQueue.remove(i);
                    i--;
                }
            }
        }

        if (this.count > 20) {
            this.count = 0;

            World world = this.getWorldObj();
            if (!world.isRemote) {
                int oldSize = this.items.size();
                this.updateInventory();
                if (oldSize != 0 || this.items.size() != 0) {
                    // Comparator...
                    world.notifyBlockChange(this.xCoord, this.yCoord, this.zCoord, this.getBlockType());
                }
            }
        }
        this.count++;
    }

    private void updateInventory() {
        ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() & 7);

        double x = this.xCoord + dir.offsetX * 2 + 0.5;
        double y = this.yCoord + dir.offsetY * 2 + 0.5;
        double z = this.zCoord + dir.offsetZ * 2 + 0.5;

        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x, y, z, x, y, z)
                .expand(TileArcaneDropper.RADIUS, TileArcaneDropper.RADIUS, TileArcaneDropper.RADIUS);
        this.items = this.getWorldObj().getEntitiesWithinAABB(EntityItem.class, box);

        // Add 10 empty slots
        for (int i = 0; i < 10; i++) {
            this.items.add(null);
        }
    }

    private EntityItem dropItem(ItemStack stack) {
        if (this.removedEntity != null && this.removedEntity.isDead) {
            for (EntityItem entityItem : this.items) {
                if (entityItem == this.removedEntity) {
                    if (InventoryUtils.areItemStacksEqualStrict(this.removedEntity.getEntityItem(), stack)
                            && this.removedEntity.getEntityItem().stackSize + this.removedCount == stack.stackSize) {
                        this.removedEntity.getEntityItem().stackSize += this.removedCount;
                        this.removedEntity.isDead = false;

                        this.removedCount = 0;
                        this.removedEntity = null;

                        return entityItem;
                    }
                }
            }
        }

        ForgeDirection side = ForgeDirection.getOrientation(this.getBlockMetadata() & 7);

        double x = this.xCoord + 0.5 + side.offsetX * 0.5;
        double y = this.yCoord + 0.25 + side.offsetY * 0.5;
        double z = this.zCoord + 0.5 + side.offsetZ * 0.5;

        EntityItem entityItem = new EntityItem(this.getWorldObj(), x, y, z, stack.copy()); //

        entityItem.motionX = side.offsetX * 0.1;
        entityItem.motionY = side.offsetY * 0.1;
        entityItem.motionZ = side.offsetZ * 0.1;

        entityItem.delayBeforeCanPickup = 3;

        // prevent stackOverflows
        this.dropQueue.add(entityItem);

        return entityItem;
    }

    @Override
    public int getSizeInventory() {
        this.updateInventory();
        return this.items.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        this.updateInventory();

        if (slot >= this.items.size()) return null; // Get rekt. nothing here on this slot.

        EntityItem entity = this.items.get(slot);
        return entity == null || entity.isDead ? null : entity.getEntityItem();
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        ItemStack stack = this.getStackInSlot(slot);

        if (stack.stackSize - amount <= 0) {
            this.setInventorySlotContents(slot, null);
            return stack.copy();
        }

        ItemStack newStack = stack.copy();
        newStack.stackSize = amount;
        stack.stackSize -= amount;

        this.removedEntity = this.items.get(slot);
        this.removedCount = amount;

        return newStack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        EntityItem oldEntity = this.items.get(slot);

        if (oldEntity != null) {
            oldEntity.setDead();
        }

        if (stack != null && stack.stackSize > 0) {
            if (oldEntity != null && InventoryUtils.areItemStacksEqualStrict(oldEntity.getEntityItem(), stack)) {
                oldEntity.getEntityItem().stackSize = stack.stackSize;
                oldEntity.isDead = false;
            } else {
                this.items.set(slot, this.dropItem(stack));
            }
        }
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int p_70304_1_) {
        return null;
    }

    @Override
    public String getInventoryName() {
        return RegisteredBlocks.blockArcaneDropper.getLocalizedName();
    }

    @Override
    public boolean hasCustomInventoryName() {
        return true;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return player instanceof FakePlayer;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        int[] slots = new int[this.getSizeInventory()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return RegisteredBlocks.blockArcaneDropper.isSideSolid(
                this.getWorldObj(),
                this.xCoord,
                this.yCoord,
                this.zCoord,
                ForgeDirection.getOrientation(side));
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        if (this.canInsertItem(slot, stack, side)) {
            EntityItem entityItem = this.items.get(slot);
            return entityItem == null || entityItem.delayBeforeCanPickup <= 0;
        }
        return false;
    }
}
