package makeo.gadomancy.common.blocks.tiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.network.NetworkRegistry;
import makeo.gadomancy.common.entities.fake.AdvancedFakePlayer;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p/>
 * Created by makeo @ 16.12.2015 11:50
 */
public class TileArcaneHand extends SynchronizedTileEntity implements ISidedInventory {

    private GuiFakePlayer fakePlayer;

    private ItemInWorldManager im;
    private Container container;
    private List<Slot> slots;

    @Override
    public void updateEntity() {
        if (this.im == null) {
            this.updateConnection();
        }
    }

    public void updateConnection() {
        this.container = null;
        this.slots = null;

        if (this.im == null) {
            this.im = new ItemInWorldManager(this.worldObj);
        } else {
            this.im.setWorld((WorldServer) this.worldObj);
        }

        TileEntity tile = this.getWorldObj().getTileEntity(this.xCoord, this.yCoord - 1, this.zCoord);
        if (tile != null && this.worldObj instanceof WorldServer) {
            if (this.fakePlayer == null) {
                this.fakePlayer = new GuiFakePlayer(this);
            }

            this.fakePlayer.posX = this.xCoord;
            this.fakePlayer.posY = this.yCoord;
            this.fakePlayer.posZ = this.zCoord;

            this.im.activateBlockOrUseItem(
                    this.fakePlayer,
                    this.worldObj,
                    null,
                    this.xCoord,
                    this.yCoord - 1,
                    this.zCoord,
                    ForgeDirection.UP.ordinal(),
                    0.5F,
                    0.5F,
                    0.5F);
        }
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        if (this.container == null) {
            return new int[0];
        }

        int[] slots = new int[this.slots.size()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return this.slots.get(slot).isItemValid(stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return this.slots.get(slot).canTakeStack(this.fakePlayer);
    }

    private static class GuiFakePlayer extends AdvancedFakePlayer {

        private TileArcaneHand tile;

        public GuiFakePlayer(TileArcaneHand tile) {
            super((WorldServer) tile.worldObj, UUID.randomUUID(), "ArcaneHand");
            this.tile = tile;
        }

        public void openGui(Object mod, int modGuiId, World world, int x, int y, int z) {
            ModContainer mc = FMLCommonHandler.instance().findContainerFor(mod);
            this.tile.container = NetworkRegistry.INSTANCE.getRemoteGuiContainer(mc, this, modGuiId, world, x, y, z);
            if (this.tile.container != null) {
                this.tile.slots = new ArrayList<Slot>();
                for (Slot slot : (List<Slot>) this.tile.container.inventorySlots) {
                    if (!this.inventory.equals(slot.inventory)) {
                        this.tile.slots.add(slot);
                    }
                }
            }
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
    }

    @Override
    public int getSizeInventory() {
        return this.slots.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.slots.get(slot).getStack();
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        Slot s = this.slots.get(slot);

        if (s.getStack() != null) {
            ItemStack itemstack;
            if (s.getStack().stackSize <= amount) {
                itemstack = s.getStack();
                s.putStack(null);
                this.markDirty();
                return itemstack;
            } else {
                itemstack = s.getStack().splitStack(amount);

                if (s.getStack().stackSize == 0) {
                    s.putStack(null);
                }

                this.markDirty();
                return itemstack;
            }
        }
        return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        this.slots.get(slot).putStack(stack);
    }

    @Override
    public String getInventoryName() {
        return "";
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
        return false;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int p_94041_1_, ItemStack p_94041_2_) {
        return true;
    }
}
