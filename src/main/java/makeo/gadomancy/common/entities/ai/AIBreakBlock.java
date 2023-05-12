package makeo.gadomancy.common.entities.ai;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import makeo.gadomancy.common.entities.fake.AdvancedFakePlayer;
import makeo.gadomancy.common.entities.fake.GolemFakePlayer;
import thaumcraft.common.config.Config;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.Marker;
import thaumcraft.common.lib.utils.InventoryUtils;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by makeo @ 17.09.2015 20:22
 */
public class AIBreakBlock extends EntityAIBase {

    private static final int BLACKLIST_TICKS = 20 * 40;

    private EntityGolemBase golem;
    private AdvancedFakePlayer player;

    private Marker currentMarker;
    private boolean hasValidTool;

    private Map<Marker, Integer> blacklist = new HashMap<>();
    private int blacklistCount;

    public AIBreakBlock(EntityGolemBase golem) {
        this.golem = golem;

        if (golem.worldObj instanceof WorldServer) {
            this.player = new GolemFakePlayer((WorldServer) golem.worldObj, golem);
        }

        this.setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        if (this.golem.ticksExisted % Config.golemDelay > 0) {
            return false;
        }

        this.currentMarker = this.getNextMarker();

        if (!this.hasValidTool()) {
            return this.isInHomeRange();
        }

        return this.currentMarker != null;
    }

    private int count;
    private int clickCount;

    @Override
    public boolean continueExecuting() {
        if (this.hasValidTool) {
            if (!this.hasBlock(this.currentMarker)) {
                return false;
            }

            if (this.distanceSquaredToGolem(this.currentMarker) < 1) {
                if (this.golem.getCarried() == null) {
                    this.golem.startActionTimer();
                } else {
                    this.golem.startRightArmTimer();
                }

                if (this.clickCount % (7 - Math.min(6, this.golem.getGolemStrength())) == 0) {
                    this.doLeftClick();
                }
                this.clickCount++;

                this.golem.getLookHelper().setLookPosition(
                        this.currentMarker.x + 0.5D,
                        this.currentMarker.y + 0.5D,
                        this.currentMarker.z + 0.5D,
                        30.0F,
                        30.0F);

                this.count = 0;
            } else {
                if (this.count == 20) {
                    this.count = 0;

                    ForgeDirection dir = ForgeDirection.getOrientation(this.currentMarker.side);
                    boolean path = this.golem.getNavigator().tryMoveToXYZ(
                            this.currentMarker.x + 0.5D + dir.offsetX,
                            this.currentMarker.y + 0.5D + dir.offsetY,
                            this.currentMarker.z + 0.5D + dir.offsetZ,
                            this.golem.getAIMoveSpeed());
                    if (!path) {
                        if (this.blacklistCount > 10) {
                            this.blacklist.put(this.currentMarker, this.golem.ticksExisted);
                            return false;
                        }
                        this.blacklistCount++;
                    }
                }
                this.count++;
                this.clickCount = 0;
            }
        } else {
            if (this.golem.ticksExisted % Config.golemDelay > 0) {
                if (this.isInHomeRange()) {
                    IInventory homeChest = this.getHomeChest();
                    if (homeChest != null) {
                        this.trySwitchTool(homeChest);
                    }
                } else {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void resetTask() {
        this.currentMarker = null;
        this.blacklistCount = 0;
        this.cancelLeftClick();
    }

    private void doLeftClick() {
        ItemStack tool = this.golem.getCarried();
        this.player.setHeldItem(tool);

        this.player.theItemInWorldManager.updateBlockRemoving();

        if (this.player.theItemInWorldManager.durabilityRemainingOnBlock == -1
                || !this.player.theItemInWorldManager.isDestroyingBlock) {
            this.player.theItemInWorldManager.onBlockClicked(
                    this.currentMarker.x,
                    this.currentMarker.y,
                    this.currentMarker.z,
                    this.currentMarker.side);
        } else if (this.player.theItemInWorldManager.durabilityRemainingOnBlock >= 9) {
            this.player.theItemInWorldManager
                    .uncheckedTryHarvestBlock(this.currentMarker.x, this.currentMarker.y, this.currentMarker.z);
            this.player.theItemInWorldManager.durabilityRemainingOnBlock = -1;

            if (tool != null) {
                Block block = this.golem.worldObj
                        .getBlock(this.currentMarker.x, this.currentMarker.y, this.currentMarker.z);
                tool.getItem().onBlockDestroyed(
                        tool,
                        this.golem.worldObj,
                        block,
                        this.currentMarker.x,
                        this.currentMarker.y,
                        this.currentMarker.z,
                        this.player);
            }
        }
        this.hasValidTool();
        this.golem.updateCarried();
    }

    private void cancelLeftClick() {
        ItemInWorldManager manager = this.player.theItemInWorldManager;
        if (manager.isDestroyingBlock) this.player.theItemInWorldManager.cancelDestroyingBlock(
                manager.partiallyDestroyedBlockX,
                manager.partiallyDestroyedBlockY,
                manager.partiallyDestroyedBlockZ);
    }

    private Marker getNextMarker() {
        List<Marker> markers = this.golem.getMarkers();

        markers.sort(Comparator.comparingDouble(this::distanceSquaredToGolem));

        for (Marker marker : markers) {
            if (this.isValid(marker)) {
                return marker;
            }
        }
        return null;
    }

    private double distanceSquaredToGolem(Marker marker) {
        return this.distanceSquaredToGolem(marker.x, marker.y, marker.z, marker.side);
    }

    private double distanceSquaredToGolem(double x, double y, double z, int facing) {
        ForgeDirection dir = ForgeDirection.getOrientation(facing);
        return this.golem.getDistanceSq(
                x + 0.5 + (0.5 * dir.offsetX),
                y + 0.5 + (0.5 * dir.offsetY),
                z + 0.5 + (0.5 * dir.offsetZ));
    }

    private boolean isInHomeRange() {
        ChunkCoordinates home = this.golem.getHomePosition();
        return this.golem.getDistanceSq(home.posX + 0.5, home.posY + 0.5, home.posZ + 0.5) < 3;
    }

    private boolean hasBlock(Marker marker) {
        return marker != null && !this.golem.worldObj.isAirBlock(marker.x, marker.y, marker.z);
    }

    private boolean isValid(Marker marker) {
        if (marker == null) return false;

        if (this.blacklist.containsKey(marker)) {
            if (this.blacklist.get(marker) + AIBreakBlock.BLACKLIST_TICKS >= this.golem.ticksExisted) {
                return false;
            } else {
                this.blacklist.remove(marker);
            }
        }

        float range = this.golem.getRange();
        if (this.golem.getHomePosition().getDistanceSquared(marker.x, marker.y, marker.z) > range * range) {
            return false;
        }

        Block block = this.golem.worldObj.getBlock(marker.x, marker.y, marker.z);

        if (!block.isAir(this.golem.worldObj, marker.x, marker.y, marker.z)) {
            ItemStack blockStack = new ItemStack(Item.getItemFromBlock(block));
            boolean empty = true;
            for (int slot = 0; slot < this.golem.inventory.slotCount; slot++) {
                ItemStack stack = this.golem.inventory.inventory[slot];

                if (stack != null && Block.getBlockFromItem(stack.getItem()) != Blocks.air) {
                    empty = false;
                    if ((marker.color == -1 || marker.color == this.golem.colors[slot])
                            && InventoryUtils.areItemStacksEqual(
                                    blockStack,
                                    stack,
                                    this.golem.checkOreDict(),
                                    this.golem.ignoreDamage(),
                                    this.golem.ignoreNBT())) {
                        return true;
                    }
                }
            }
            return empty;
        }
        return false;
    }

    private IInventory getHomeChest() {
        ChunkCoordinates coords = this.golem.getHomePosition();
        ForgeDirection facing = ForgeDirection.getOrientation(this.golem.homeFacing);
        TileEntity tile = this.golem.worldObj.getTileEntity(
                coords.posX - facing.offsetX,
                coords.posY - facing.offsetY,
                coords.posZ - facing.offsetZ);
        if (tile instanceof IInventory) {
            return (IInventory) tile;
        }
        return null;
    }

    private void trySwitchTool(IInventory inv) {
        for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
            ItemStack stack = inv.getStackInSlot(slot);

            ItemStack current = this.golem.getCarried();
            if (current != null) {
                current = InventoryUtils.insertStack(inv, current, this.golem.homeFacing, true);
                if (current != null) {
                    return;
                }
                this.golem.setCarried(null);

                if (this.hasValidTool()) {
                    this.hasValidTool = true;
                    return;
                }
            }

            if (stack != null && this.isValidTool(stack)) {
                stack = stack.copy();

                stack.stackSize = 1;
                stack = InventoryUtils.extractStack(inv, stack, this.golem.homeFacing, false, false, false, true);

                if (stack != null) {
                    this.golem.setCarried(stack);
                    this.golem.updateCarried();
                    this.player.setHeldItem(stack);

                    this.hasValidTool = true;
                    break;
                }
            }
        }
    }

    private boolean hasValidTool() {
        this.hasValidTool = this.isValidTool(this.golem.getCarried());
        return this.hasValidTool;
    }

    private boolean isValidTool(ItemStack tool) {
        if (tool == null || Block.getBlockFromItem(tool.getItem()) == Blocks.air) {
            boolean empty = true;
            for (int slot = 0; slot < this.golem.inventory.slotCount; slot++) {
                ItemStack stack = this.golem.inventory.inventory[slot];
                if (stack != null && Block.getBlockFromItem(stack.getItem()) == Blocks.air
                        && (this.currentMarker == null || this.golem.colors[slot] == -1
                                || this.currentMarker.color == -1
                                || this.golem.colors[slot] == this.currentMarker.color)) {
                    empty = false;

                    if (tool == null) {
                        return false;
                    }

                    if (InventoryUtils.areItemStacksEqual(
                            tool,
                            stack,
                            this.golem.checkOreDict(),
                            this.golem.ignoreDamage(),
                            this.golem.ignoreNBT())) {
                        return true;
                    }
                }
            }
            return empty && tool == null;
        }
        return false;
    }
}
