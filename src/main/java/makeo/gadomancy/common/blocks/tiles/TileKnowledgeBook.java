package makeo.gadomancy.common.blocks.tiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import makeo.gadomancy.client.util.UtilsFX;
import makeo.gadomancy.common.entities.EntityPermNoClipItem;
import makeo.gadomancy.common.network.PacketHandler;
import makeo.gadomancy.common.network.packets.PacketStartAnimation;
import makeo.gadomancy.common.utils.NBTHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.common.entities.EntitySpecialItem;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.lib.events.EssentiaHandler;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.lib.research.ResearchNoteData;

/**
 * HellFirePvP@Admin Date: 19.04.2016 / 14:53 on Gadomancy TileKnowledgeBook
 */
public class TileKnowledgeBook extends SynchronizedTileEntity
        implements EntityPermNoClipItem.IItemMasterTile, IAspectContainer {

    private static final Random rand = new Random();
    private static final int LOWEST_AMOUNT = 10;
    private static final int COGNITIO_TICKS = 150;
    private static final int MAX_NEEDED_KNOWLEDGE = 200;
    private static final int SEARCH_RANGE_XZ = 4;
    private static final int SEARCH_RANGE_Y = 3;
    private static final double MULTIPLIER = 4;

    private static final long ENVIRONMENT_UPDATE_TIMEOUT = 100;
    private static final long RESEARCH_SEARCH_TIMEOUT = 20;
    private static final long ITEM_INFO_TIMEOUT = 16;
    private static final double MAX_PULL_SPEED = 0.5D;

    private FloatingBookAttributes bookAttributes = new FloatingBookAttributes();

    // The itemstack this is connected to.
    private ItemStack storedResearchNote;
    private EntityPermNoClipItem.ItemChangeTask scheduledTask;

    // Ticks
    private long currentWorldTime;
    private long lastProcessedTick;
    // If it's first updateEntity call, based on world time
    private boolean firstTimeThisTick;

    private long lastEnvironmentTick;
    private long lastResearchSearchTick;
    private long lastMovementTick;
    private long lastItemInfoTick;
    private long lastSyncTick;

    private int ticksExisted;
    private int ticksCognitio;

    // Sound effect stuff. I didn't like it when it's like changing pages 4 times in a row...
    private boolean turnedPagesLastTick;

    // General research stuff
    private boolean researching;
    private AspectList workResearchAspects;
    private int surroundingKnowledge;

    private boolean markedForUpdate = false;

    @Override
    public void updateEntity() {
        this.updateWorldTime();
        this.bookAttributes.updateFloatingBook();

        if (!this.worldObj.isRemote) {
            if (this.firstTimeThisTick) {
                if (this.currentWorldTime - this.lastItemInfoTick > ITEM_INFO_TIMEOUT) {
                    this.informItemRemoval();
                }

                if (this.updateResearchStatus()) {
                    this.markForUpdate();
                }
            }

            if (this.researching) {
                this.doResearchCycle();
            }
        } else if (this.researching && this.hasCognitio()) {
            this.doResearchEffects();
        }

        if (this.firstTimeThisTick && this.storedResearchNote == null) {
            this.tryVortexUnfinishedResearchNotes();
        }

        this.sync();
    }

    private void updateWorldTime() {
        this.currentWorldTime = this.worldObj.getTotalWorldTime();
        this.firstTimeThisTick = this.currentWorldTime != this.lastProcessedTick;
        this.lastProcessedTick = this.currentWorldTime;
        this.ticksExisted++;
    }

    private void markForUpdate() {
        this.markedForUpdate = true;
    }

    private void sync() {
        if (!this.markedForUpdate || this.lastSyncTick == currentWorldTime) return;

        this.markedForUpdate = false;
        this.lastSyncTick = currentWorldTime;
        this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
        this.markDirty();
    }

    public boolean hasCognitio() {
        return this.ticksCognitio > 0;
    }

    public boolean isResearching() {
        return this.researching;
    }

    private void doResearchCycle() {
        this.drainCognitio();

        if (!this.hasCognitio()) {
            return;
        }
        this.checkSurroundings();

        int chance = Math.max(0, MAX_NEEDED_KNOWLEDGE - this.surroundingKnowledge) + 100;
        if (TileKnowledgeBook.rand.nextInt(chance) == 0) {
            this.doResearchProgress();
            this.markForUpdate();
        }
    }

    private void doResearchProgress() {
        List<Aspect> aspects = new ArrayList<Aspect>(this.workResearchAspects.aspects.keySet());
        if (aspects.isEmpty()) {
            this.finishResearch();
            return;
        }
        Aspect a = aspects.get(TileKnowledgeBook.rand.nextInt(aspects.size()));
        int value = this.workResearchAspects.aspects.get(a);
        value--;
        if (value <= 0) {
            this.workResearchAspects.aspects.remove(a);
        } else {
            this.workResearchAspects.aspects.put(a, value);
        }
        if (this.workResearchAspects.aspects.isEmpty()) {
            this.finishResearch();
        }
    }

    private void checkSurroundings() {
        if (this.currentWorldTime - lastEnvironmentTick < ENVIRONMENT_UPDATE_TIMEOUT) return;

        this.lastEnvironmentTick = this.currentWorldTime;
        this.surroundingKnowledge = 0;

        for (int xx = -SEARCH_RANGE_XZ; xx <= SEARCH_RANGE_XZ; xx++) {
            for (int zz = -SEARCH_RANGE_XZ; zz <= SEARCH_RANGE_XZ; zz++) {
                for (int yy = -SEARCH_RANGE_Y; yy <= SEARCH_RANGE_Y; yy++) {
                    if (xx == 0 && yy == 0 && zz == 0) {
                        continue;
                    }
                    int absX = xx + this.xCoord;
                    int absY = yy + this.yCoord;
                    int absZ = zz + this.zCoord;
                    Block block = this.worldObj.getBlock(absX, absY, absZ);

                    this.surroundingKnowledge += (int) block.getEnchantPowerBonus(this.worldObj, absX, absY, absZ);
                }
            }
        }
    }

    private void drainCognitio() {
        if (this.ticksCognitio <= 0) {
            this.searchForCognitio();
        } else {
            this.ticksCognitio--;
            if (this.ticksCognitio <= 40) {
                this.searchForCognitio();
            }
            if (this.ticksCognitio <= 0) {
                this.markForUpdate();
            }
        }
    }

    private void searchForCognitio() {
        if ((this.ticksExisted & 31) == 0) {
            int drainRange = 4;
            ForgeDirection[] toTry = ForgeDirection.VALID_DIRECTIONS;
            for (ForgeDirection dir : toTry) {
                if (dir == null) continue; // LUL should not happen...
                if (EssentiaHandler.drainEssentia(this, Aspect.MIND, dir, drainRange)) {
                    this.ticksCognitio += COGNITIO_TICKS;
                    this.markForUpdate();
                    break;
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private void doResearchEffects() {
        if ((this.ticksExisted & 15) == 0) {
            UtilsFX.doRuneEffects(Minecraft.getMinecraft().theWorld, this.xCoord, this.yCoord - 1, this.zCoord, 0);
        }
        if ((this.ticksExisted & 31) == 0) {
            switch (TileKnowledgeBook.rand.nextInt(5)) {
                case 0:
                    if (!this.turnedPagesLastTick) {
                        this.worldObj.playSound(
                                this.xCoord + 0.5,
                                this.yCoord + 0.3,
                                this.zCoord + 0.5,
                                "thaumcraft:page",
                                0.4F,
                                1.0F,
                                false);
                        this.turnedPagesLastTick = true;
                        break;
                    }
                case 1:
                case 2:
                case 3:
                    this.worldObj.playSound(
                            this.xCoord + 0.5,
                            this.yCoord + 0.3,
                            this.zCoord + 0.5,
                            "thaumcraft:write",
                            0.2F,
                            1.0F,
                            false);
                    this.turnedPagesLastTick = false;
                    break;
            }
        }
    }

    private void finishResearch() {
        this.stopResearch();
        this.scheduleFinishItemChange();
        PacketStartAnimation packet = new PacketStartAnimation(
                PacketStartAnimation.ID_SPARKLE_SPREAD,
                this.xCoord,
                this.yCoord,
                this.zCoord);
        PacketHandler.INSTANCE.sendToAllAround(packet, this.getTargetPoint(32));
        this.markForUpdate();
    }

    private NetworkRegistry.TargetPoint getTargetPoint(double radius) {
        return new NetworkRegistry.TargetPoint(
                this.worldObj.provider.dimensionId,
                this.xCoord,
                this.yCoord,
                this.zCoord,
                radius);
    }

    private void scheduleFinishItemChange() {
        this.scheduledTask = new EntityPermNoClipItem.ItemChangeTask() {

            @Override
            public void changeItem(EntityPermNoClipItem item) {
                ItemStack stack = item.getEntityItem();
                stack.stackTagCompound.setBoolean("complete", true);
                stack.setItemDamage(64);
            }
        };
    }

    // Returns true, if updating/sync is required.
    private boolean updateResearchStatus() {
        if (this.researching) {
            if (this.storedResearchNote != null) {
                ResearchNoteData nd = ResearchManager.getData(this.storedResearchNote);
                if (nd == null || nd.isComplete()) {
                    this.stopResearch();
                    return true;
                }
            } else {
                this.stopResearch();
                return true;
            }
        } else {
            if (this.storedResearchNote != null) {
                ResearchNoteData nd = ResearchManager.getData(this.storedResearchNote);
                if (nd != null && !nd.isComplete()) {
                    ResearchItem ri = ResearchCategories.getResearch(nd.key);
                    if (ri != null) {
                        this.beginResearch(ri.tags);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void beginResearch(AspectList researchTags) {
        AspectList workResearchList = new AspectList();
        for (Aspect a : researchTags.aspects.keySet()) {
            int value = researchTags.aspects.get(a);
            int newVal = (int) Math.max(LOWEST_AMOUNT, ((double) value) * MULTIPLIER);
            workResearchList.add(a, newVal);
        }
        this.workResearchAspects = workResearchList;
        this.researching = true;
    }

    private void stopResearch() {
        this.storedResearchNote = null;
        this.workResearchAspects = null;
        this.researching = false;
    }

    @Override
    public boolean canStillHoldItem() {
        return true;
    }

    @Override
    public void informMaster() {
        this.lastItemInfoTick = this.worldObj.getTotalWorldTime();
    }

    @Override
    public void informItemRemoval() {
        this.storedResearchNote = null;
        this.stopResearch();
        this.markForUpdate();
    }

    @Override
    public EntityPermNoClipItem.ItemChangeTask getAndRemoveScheduledChangeTask() {
        if (this.scheduledTask != null) {
            EntityPermNoClipItem.ItemChangeTask buffer = this.scheduledTask;
            this.scheduledTask = null;
            return buffer;
        }
        return null;
    }

    @Override
    public void broadcastItemStack(ItemStack itemStack) {
        this.storedResearchNote = itemStack;
    }

    private void tryVortexUnfinishedResearchNotes() {
        if (this.currentWorldTime - lastResearchSearchTick < RESEARCH_SEARCH_TIMEOUT) return;

        double centerY = this.yCoord + 0.4;

        List<EntityItem> entityItems = this.worldObj.selectEntitiesWithinAABB(
                EntityItem.class,
                AxisAlignedBB.getBoundingBox(
                        this.xCoord - 0.5,
                        centerY - 0.5,
                        this.zCoord - 0.5,
                        this.xCoord + 0.5,
                        centerY + 0.5,
                        this.zCoord + 0.5).expand(8, 8, 8),
                this::isEntityCorrectResearchNote);

        double xx = this.xCoord + 0.5;
        double yy = centerY + 0.5;
        double zz = this.zCoord + 0.5;

        EntityItem closestEntity = null;
        double closestDistance = Double.MAX_VALUE;

        for (EntityItem entity : entityItems) {
            double dist = entity.getDistanceSq(xx, yy, zz);

            if (dist <= closestDistance) {
                closestEntity = entity;
                closestDistance = dist;
            }
        }

        if (closestEntity == null) {
            lastResearchSearchTick = this.currentWorldTime;
            return;
        }

        if (closestDistance < 1 && !this.worldObj.isRemote) {
            ItemStack inter = closestEntity.getEntityItem();
            inter.stackSize--;
            this.storedResearchNote = inter.copy();
            this.storedResearchNote.stackSize = 1;

            EntityPermNoClipItem item = this.createNoClipEntityItem(closestEntity);

            this.lastItemInfoTick = this.currentWorldTime;

            if (inter.stackSize <= 0) {
                closestEntity.setDead();
            }
            closestEntity.noClip = false;
            item.delayBeforeCanPickup = 60;

            this.markForUpdate();
        } else {
            closestEntity.noClip = true;
            this.applyMovementVectors(closestEntity);
        }
    }

    private boolean isEntityCorrectResearchNote(Entity e) {
        return !(e instanceof EntitySpecialItem) && e instanceof EntityItem
                && !e.isDead
                && ((EntityItem) e).getEntityItem() != null
                && ((EntityItem) e).getEntityItem().getItem() instanceof ItemResearchNotes
                && this.shouldVortexResearchNote(((EntityItem) e).getEntityItem());
    }

    private EntityPermNoClipItem createNoClipEntityItem(EntityItem entity) {
        EntityPermNoClipItem item = new EntityPermNoClipItem(
                entity.worldObj,
                this.xCoord + 0.5F,
                this.yCoord + 0.7F,
                this.zCoord + 0.5F,
                this.storedResearchNote,
                this.xCoord,
                this.yCoord,
                this.zCoord);
        entity.worldObj.spawnEntityInWorld(item);
        item.motionX = 0;
        item.motionY = 0;
        item.motionZ = 0;
        item.hoverStart = entity.hoverStart;
        item.age = entity.age;
        item.noClip = true;

        return item;
    }

    // Special to masterTile only!
    private void applyMovementVectors(EntityItem entity) {
        if (currentWorldTime == this.lastMovementTick) return;
        this.lastMovementTick = currentWorldTime;

        double dx = (this.xCoord + 0.5D - entity.posX) / 15.0D;
        double dy = (this.yCoord + 0.5D - entity.posY) / 15.0D;
        double dz = (this.zCoord + 0.5D - entity.posZ) / 15.0D;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double falloff = 1.0D - dist;

        if (dist == 0.0D || falloff <= 0.0D) return;
        falloff *= falloff;

        entity.motionX = clampSpeed(entity.motionX + dx / dist * falloff * 0.15D);
        entity.motionY = clampSpeed(entity.motionY + dy / dist * falloff * 0.25D);
        entity.motionZ = clampSpeed(entity.motionZ + dz / dist * falloff * 0.15D);
    }

    private static double clampSpeed(double v) {
        return Math.max(-MAX_PULL_SPEED, Math.min(MAX_PULL_SPEED, v));
    }

    private boolean shouldVortexResearchNote(ItemStack stack) {
        ResearchNoteData nd = ResearchManager.getData(stack);
        if (nd == null) return false;
        if (nd.isComplete()) return false;
        ResearchItem ri = ResearchCategories.getResearch(nd.key);
        return ri != null;
    }

    public FloatingBookAttributes getBookAttributes() {
        return this.bookAttributes;
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        this.ticksCognitio = compound.getInteger("cognitio");
        this.workResearchAspects = NBTHelper.getAspectList(compound, "workAspects");
        this.researching = compound.getBoolean("researching");
        this.storedResearchNote = NBTHelper.getStack(compound, "crystalStack");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        if (this.workResearchAspects != null) {
            NBTHelper.setAspectList(compound, "workAspects", this.workResearchAspects);
        }
        compound.setInteger("cognitio", this.ticksCognitio);
        compound.setBoolean("researching", this.researching);
        if (this.storedResearchNote != null) NBTHelper.setStack(compound, "crystalStack", this.storedResearchNote);
    }

    @Override
    public AspectList getAspects() {
        return this.workResearchAspects;
    }

    @Override
    public void setAspects(AspectList list) {}

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
    public boolean takeFromContainer(AspectList list) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect aspect, int i) {
        return false;
    }

    @Override
    public boolean doesContainerContain(AspectList list) {
        return false;
    }

    @Override
    public int containerContains(Aspect aspect) {
        return 0;
    }

    public class FloatingBookAttributes {

        // Ugh. EnchantmentTable stuff... I DON'T UNDERSTAND THIS FLOATING BOOK
        // Well not that i tried... copy pasta please.
        public int field_145926_a;
        public float field_145933_i;
        public float field_145931_j;
        public float field_145932_k;
        public float field_145929_l;
        public float field_145930_m;
        public float field_145927_n;
        public float field_145928_o;
        public float field_145925_p;
        public float field_145924_q;

        private void updateFloatingBook() {
            this.field_145927_n = this.field_145930_m;
            this.field_145925_p = this.field_145928_o;
            EntityPlayer entityplayer = TileKnowledgeBook.this.worldObj.getClosestPlayer(
                    (float) TileKnowledgeBook.this.xCoord + 0.5F,
                    (float) TileKnowledgeBook.this.yCoord + 0.5F,
                    (float) TileKnowledgeBook.this.zCoord + 0.5F,
                    3.0D);

            if (entityplayer != null) {
                double d0 = entityplayer.posX - (double) ((float) TileKnowledgeBook.this.xCoord + 0.5F);
                double d1 = entityplayer.posZ - (double) ((float) TileKnowledgeBook.this.zCoord + 0.5F);
                this.field_145924_q = (float) Math.atan2(d1, d0);
                this.field_145930_m += 0.1F;

                if (this.field_145930_m < 0.5F || TileKnowledgeBook.rand.nextInt(40) == 0) {
                    float f1 = this.field_145932_k;

                    do {
                        this.field_145932_k += (float) (TileKnowledgeBook.rand.nextInt(4)
                                - TileKnowledgeBook.rand.nextInt(4));
                    } while (f1 == this.field_145932_k);
                }
            } else {
                this.field_145924_q += 0.02F;
                this.field_145930_m -= 0.1F;
            }

            while (this.field_145928_o >= (float) Math.PI) {
                this.field_145928_o -= ((float) Math.PI * 2F);
            }

            while (this.field_145928_o < -(float) Math.PI) {
                this.field_145928_o += ((float) Math.PI * 2F);
            }

            while (this.field_145924_q >= (float) Math.PI) {
                this.field_145924_q -= ((float) Math.PI * 2F);
            }

            while (this.field_145924_q < -(float) Math.PI) {
                this.field_145924_q += ((float) Math.PI * 2F);
            }

            float f2;

            for (f2 = this.field_145924_q - this.field_145928_o; f2 >= (float) Math.PI; f2 -= ((float) Math.PI * 2F)) {}

            while (f2 < -(float) Math.PI) {
                f2 += ((float) Math.PI * 2F);
            }

            this.field_145928_o += f2 * 0.4F;

            if (this.field_145930_m < 0.0F) {
                this.field_145930_m = 0.0F;
            }

            if (this.field_145930_m > 1.0F) {
                this.field_145930_m = 1.0F;
            }

            ++this.field_145926_a;
            this.field_145931_j = this.field_145933_i;
            float f = (this.field_145932_k - this.field_145933_i) * 0.4F;
            float f3 = 0.2F;

            if (f < -f3) {
                f = -f3;
            }

            if (f > f3) {
                f = f3;
            }

            this.field_145929_l += (f - this.field_145929_l) * 0.9F;
            this.field_145933_i += this.field_145929_l;
        }
    }
}
