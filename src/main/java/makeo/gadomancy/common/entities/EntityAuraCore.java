package makeo.gadomancy.common.entities;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.WeightedRandom;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import io.netty.buffer.ByteBuf;
import makeo.gadomancy.client.effect.EffectHandler;
import makeo.gadomancy.client.effect.fx.EntityFXFlowPolicy;
import makeo.gadomancy.client.effect.fx.FXFlow;
import makeo.gadomancy.client.effect.fx.Orbital;
import makeo.gadomancy.common.data.config.ModConfig;
import makeo.gadomancy.common.items.ItemAuraCore;
import makeo.gadomancy.common.network.PacketHandler;
import makeo.gadomancy.common.network.packets.PacketStartAnimation;
import makeo.gadomancy.common.registration.RegisteredItems;
import makeo.gadomancy.common.utils.MiscUtils;
import makeo.gadomancy.common.utils.PrimalAspectList;
import makeo.gadomancy.common.utils.Vector3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ScanResult;
import thaumcraft.common.lib.research.ScanManager;
import thaumcraft.common.lib.utils.BlockUtils;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by HellFirePvP @ 16.11.2015 14:42
 */
public class EntityAuraCore extends EntityItem implements IEntityAdditionalSpawnData {

    private static final String SPLIT = ";";

    private static final int PRE_GATHER_EFFECT_LENGTH = 50;
    private static final int GATHER_EFFECT_LENGTH = 500; // 20 + 430
    private static final int GATHER_RANGE = 4;
    private static final int CLUSTER_WEIGHT = 10; // Counts as 10 'blocks' 'scanned' with the given aspect.
    public static final int CLUSTER_RANGE = 10; // Defines how close the clicked cluster has to be.
    private static final int REQUIRED_BLOCKS = (int) Math
            .round(Math.pow(EntityAuraCore.GATHER_RANGE * 2 + 1, 3) * 0.15);

    public PrimalAspectList internalAuraList = new PrimalAspectList();
    public Orbital auraOrbital;

    // Effect stuff ResidentSleeper
    private Aspect[] effectAspects = new Aspect[6];
    private Orbital.OrbitalRenderProperties[] effectProperties = new Orbital.OrbitalRenderProperties[6];
    private FXFlow[] flows = new FXFlow[6];

    private String oldAspectDataSent;
    public ChunkCoordinates activationLocation;

    private int blockCount;

    public EntityAuraCore(World world) {
        super(world);
    }

    public EntityAuraCore(World world, double x, double y, double z, ItemStack stack, ChunkCoordinates startingCoords,
            Aspect[] aspects) {
        super(world, x, y, z, stack);
        this.activationLocation = startingCoords;
        if (aspects.length == 1) {
            this.internalAuraList.add(aspects[0], EntityAuraCore.CLUSTER_WEIGHT * 6);
        } else {
            for (Aspect a : aspects) {
                if (a == null) continue;
                this.internalAuraList.add(a, EntityAuraCore.CLUSTER_WEIGHT);
            }
        }
        this.sendAspectData(this.electParliament());

        this.initGathering();
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.getDataWatcher().addObjectByDataType(ModConfig.entityAuraCoreDatawatcherAspectsId, 4);

        this.getDataWatcher().updateObject(ModConfig.entityAuraCoreDatawatcherAspectsId, "");
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (this.auraOrbital == null && !MiscUtils.getPositionVector(this).equals(Vector3.ZERO)
                && this.worldObj.isRemote) {
            this.auraOrbital = new Orbital(MiscUtils.getPositionVector(this), this.worldObj);
        }
        if (this.auraOrbital != null && this.worldObj.isRemote) {
            if (!this.auraOrbital.registered) {
                EffectHandler.getInstance().registerOrbital(this.auraOrbital);
            }
            this.auraOrbital.updateCenter(MiscUtils.getPositionVector(this));
            if (this.ticksExisted > EntityAuraCore.PRE_GATHER_EFFECT_LENGTH) {
                int part = this.ticksExisted - EntityAuraCore.PRE_GATHER_EFFECT_LENGTH;
                float perc = ((float) EntityAuraCore.GATHER_EFFECT_LENGTH - part)
                        / ((float) EntityAuraCore.GATHER_EFFECT_LENGTH);
                this.auraOrbital.reduceAllOffsets(perc);
            }
        }

        if (this.age + 5 >= this.lifespan) {
            this.age = 0;
        }

        if (!this.worldObj.isRemote) {
            if (this.ticksExisted > EntityAuraCore.GATHER_EFFECT_LENGTH) {
                this.finishCore();
            } else if (this.ticksExisted > EntityAuraCore.PRE_GATHER_EFFECT_LENGTH) {
                this.auraGatherCycle();
            }
        } else {
            boolean changed = this.recieveAspectData();
            for (int i = 0; i < this.effectProperties.length; i++) {
                Orbital.OrbitalRenderProperties node = this.effectProperties[i];
                if (node == null) {
                    if (this.effectAspects[i] == null) {
                        continue;
                    }

                    node = new Orbital.OrbitalRenderProperties(Orbital.Axis.persisentRandomAxis(), 1D); // rand.nextDouble()
                    node.setColor(new Color(this.effectAspects[i].getColor()))
                            .setTicksForFullCircle(120 + this.rand.nextInt(40));
                    node.setOffsetTicks(this.rand.nextInt(80));
                    Color c = this.getSubParticleColor(this.effectAspects[i]);
                    node.setSubParticleColor(c);
                    node.setParticleSize(0.1f);
                    node.setSubSizeRunnable(new Orbital.OrbitalSubSizeRunnable() {

                        @Override
                        public float getSubParticleSize(Random rand, int orbitalExisted) {
                            return 0.05F + (rand.nextBoolean() ? 0.0F : 0.025F);
                        }
                    });
                    // node.setMultiplier()
                    this.effectProperties[i] = node;
                }

                if (this.flows[i] == null && this.ticksExisted < EntityAuraCore.PRE_GATHER_EFFECT_LENGTH) {
                    Vector3 v = new Vector3(
                            this.activationLocation.posX + 0.5D,
                            this.activationLocation.posY + 0.5D,
                            this.activationLocation.posZ + 0.5D);
                    this.flows[i] = EffectHandler.getInstance().effectFlow(
                            this.worldObj,
                            v,
                            new FXFlow.EntityFlowProperties().setPolicy(EntityFXFlowPolicy.Policies.DEFAULT)
                                    .setTarget(this.auraOrbital.getOrbitalStartPoints(node)[0])
                                    .setColor(new Color(this.effectAspects[i].getColor()))
                                    .setFading(this.getSubParticleColor(this.effectAspects[i])));
                    this.flows[i].setLivingTicks(EntityAuraCore.PRE_GATHER_EFFECT_LENGTH - this.ticksExisted);
                }

                if (this.flows[i] != null) this.flows[i].applyTarget(this.auraOrbital.getOrbitalStartPoints(node)[0]);

                if (changed) {
                    if (this.effectProperties[i] != null) {
                        this.effectProperties[i].setColor(new Color(this.effectAspects[i].getColor()));
                        Color c = this.getSubParticleColor(this.effectAspects[i]);
                        node.setSubParticleColor(c);
                    }

                    if (this.flows[i] != null) {
                        this.flows[i].setColor(this.getSubParticleColor(this.effectAspects[i]));
                        this.flows[i].setColor(new Color(this.effectAspects[i].getColor()));
                    }
                }
            }
            if (this.ticksExisted >= (EntityAuraCore.PRE_GATHER_EFFECT_LENGTH - 1)) {
                if (this.auraOrbital.orbitalsSize() == 0) {
                    for (int i = 0; i < 6; i++) {
                        Orbital.OrbitalRenderProperties node = this.effectProperties[i];
                        this.auraOrbital.addOrbitalPoint(node);
                    }
                }
            } else {
                for (int i = 0; i < 6; i++) {
                    if (this.flows[i] != null) {
                        this.flows[i].lastUpdateCall = System.currentTimeMillis();
                    }
                }
            }
        }
    }

    private void finishCore() {
        ItemStack auraCore = new ItemStack(RegisteredItems.itemAuraCore, 1, 0);
        boolean success = false;
        if (this.blockCount >= EntityAuraCore.REQUIRED_BLOCKS) {
            double avg = ((double) this.internalAuraList.visSize()) / ((double) this.internalAuraList.size());
            Aspect[] sortedHtL = this.internalAuraList.getAspectsSortedAmount();
            AspectList al = new AspectList();
            for (Aspect a : sortedHtL) {
                if (a == null) return;
                int am = this.internalAuraList.getAmount(a);
                if (am >= avg) {
                    al.add(a, am);
                }
            }

            List<AspectWRItem> rand = new ArrayList<AspectWRItem>();
            for (Aspect a : al.getAspects()) {
                if (a == null) continue;
                rand.add(new AspectWRItem(al.getAmount(a), a));
            }

            Aspect aura = ((AspectWRItem) WeightedRandom.getRandomItem(this.worldObj.rand, rand)).getAspect();
            for (ItemAuraCore.AuraCoreType type : ItemAuraCore.AuraCoreType.values()) {
                if (type.isAspect() && type.getAspect().equals(aura)) {
                    RegisteredItems.itemAuraCore.setCoreType(auraCore, type);
                    success = true;
                }
            }
        }

        PacketStartAnimation animationPacket;
        if (!success) {
            animationPacket = new PacketStartAnimation(
                    PacketStartAnimation.ID_SMOKE_SPREAD,
                    (int) this.posX,
                    (int) this.posY,
                    (int) this.posZ,
                    Float.floatToIntBits(2F));
        } else {
            animationPacket = new PacketStartAnimation(
                    PacketStartAnimation.ID_SPARKLE_SPREAD,
                    (int) this.posX,
                    (int) this.posY,
                    (int) this.posZ,
                    (byte) 0);
        }
        PacketHandler.INSTANCE.sendToAllAround(animationPacket, MiscUtils.getTargetPoint(this.worldObj, this, 32));

        EntityItem ei = new EntityItem(this.worldObj, this.posX, this.posY, this.posZ, auraCore);
        ei.motionX = 0;
        ei.motionY = 0;
        ei.motionZ = 0;
        this.worldObj.spawnEntityInWorld(ei);
        this.setDead();
    }

    private List<ChunkCoordinates> markedLocations;

    private void initGathering() {
        this.markedLocations = new ArrayList<ChunkCoordinates>((int) Math.pow(EntityAuraCore.GATHER_RANGE * 2 + 1, 3));
        for (int x = -EntityAuraCore.GATHER_RANGE; x <= EntityAuraCore.GATHER_RANGE; x++) {
            for (int y = -EntityAuraCore.GATHER_RANGE; y <= EntityAuraCore.GATHER_RANGE; y++) {
                for (int z = -EntityAuraCore.GATHER_RANGE; z <= EntityAuraCore.GATHER_RANGE; z++) {
                    this.markedLocations.add(new ChunkCoordinates(x, y, z));
                }
            }
        }
        Collections.shuffle(this.markedLocations, new Random(this.activationLocation.hashCode()));
    }

    private void auraGatherCycle() {
        if (this.ticksExisted >= EntityAuraCore.PRE_GATHER_EFFECT_LENGTH) {
            int elapsed = this.ticksExisted - EntityAuraCore.PRE_GATHER_EFFECT_LENGTH - 1;
            float dist = (EntityAuraCore.GATHER_EFFECT_LENGTH - EntityAuraCore.PRE_GATHER_EFFECT_LENGTH)
                    / (float) this.markedLocations.size();

            int index = (int) (elapsed / dist);
            int lastIndex = (int) ((elapsed - 1) / dist);
            if (index < this.markedLocations.size() && index > lastIndex) {
                int diff = index - lastIndex;
                for (int i = 0; i < diff; i++) {
                    ChunkCoordinates coord = this.markedLocations.get(index + i);

                    int x = (int) (coord.posX + this.posX);
                    int y = (int) (coord.posY + this.posY);
                    int z = (int) (coord.posZ + this.posZ);

                    Block block = this.worldObj.getBlock(x, y, z);
                    if (block != Blocks.air) {
                        int meta = this.worldObj.getBlockMetadata(x, y, z);
                        ScanResult result = null;
                        MovingObjectPosition pos = new MovingObjectPosition(
                                x,
                                y,
                                z,
                                ForgeDirection.UP.ordinal(),
                                Vec3.createVectorHelper(0, 0, 0),
                                true);
                        ItemStack is = null;
                        try {
                            is = block.getPickBlock(pos, this.worldObj, x, y, z);
                        } catch (Throwable ignored) {}
                        try {
                            if (is == null) {
                                is = BlockUtils.createStackedBlock(block, meta);
                            }
                        } catch (Exception ignored) {}
                        try {
                            if (is == null) {
                                result = new ScanResult((byte) 1, Block.getIdFromBlock(block), meta, null, "");
                            } else {
                                result = new ScanResult(
                                        (byte) 1,
                                        Item.getIdFromItem(is.getItem()),
                                        is.getItemDamage(),
                                        null,
                                        "");
                            }
                        } catch (Exception ignored) {}

                        if (result == null) continue; // We can't scan it BibleThump

                        AspectList aspects = ScanManager.getScanAspects(result, this.worldObj);
                        if (aspects.size() > 0) {
                            this.internalAuraList.add(aspects);
                            this.blockCount++;
                        }
                    }
                }
            }
            this.sendAspectData(this.electParliament());
        }
    }

    private Aspect[] electParliament() {
        Aspect[] colors = new Aspect[6];

        Aspect[] aspects = this.internalAuraList.getAspectsSortedAmount();

        int totalSize = this.internalAuraList.visSize();

        int availableSeats = 6;
        for (Aspect aspect : aspects) {
            float percent = this.internalAuraList.getAmount(aspect) / (float) totalSize;

            int seats = (int) Math.ceil(availableSeats * percent);
            seats = Math.min(seats, availableSeats);
            availableSeats -= seats;

            for (int i = 0; i < colors.length && seats > 0; i++) {
                if (colors[i] == null) {
                    colors[i] = aspect;
                    seats--;
                }
            }

            if (availableSeats <= 0) {
                break;
            }
        }

        for (int i = 0; i < colors.length; i++) {
            if (colors[i] == null) {
                colors[i] = aspects[0];
            }
        }
        return colors;
    }

    private Color getSubParticleColor(Aspect a) {
        Color c = null;
        if (a.equals(Aspect.AIR)) {
            c = new Color(0xFEFFC9);
        } else if (a.equals(Aspect.FIRE)) {
            c = new Color(0xFFAA3C);
        } else if (a.equals(Aspect.EARTH)) {
            c = new Color(0x7EE35F);
        } else if (a.equals(Aspect.WATER)) {
            c = new Color(0x78CFCC);
        } else if (a.equals(Aspect.ORDER)) {
            c = new Color(0xFFFFFF);
        } else if (a.equals(Aspect.ENTROPY)) {
            c = new Color(0x000000);
        }
        return c;
    }

    // Client side only.
    private boolean recieveAspectData() {
        String rec = this.getDataWatcher().getWatchableObjectString(ModConfig.entityAuraCoreDatawatcherAspectsId);
        if (this.oldAspectDataSent == null || !this.oldAspectDataSent.equals(rec)) {
            this.oldAspectDataSent = rec;
        } else {
            return false;
        }

        if (rec.equals("")) return false;

        String[] arr = rec.split(EntityAuraCore.SPLIT);
        if (arr.length != 6) throw new IllegalStateException(
                "Server sent wrong Aura Data! '" + rec + "' Please report this error to the mod authors!");
        for (int i = 0; i < arr.length; i++) {
            String s = arr[i];
            Aspect a = Aspect.getAspect(s);
            this.effectAspects[i] = a;
        }
        return true;
    }

    private void sendAspectData(Aspect[] aspects) {
        StringBuilder sb = new StringBuilder();
        for (Aspect aspect : aspects) {
            if (sb.length() > 0) {
                sb.append(EntityAuraCore.SPLIT);
            }
            sb.append(aspect.getTag());
        }
        String toSend = sb.toString();

        if (!toSend.equals(this.oldAspectDataSent)) {
            this.oldAspectDataSent = toSend;
            this.getDataWatcher().updateObject(ModConfig.entityAuraCoreDatawatcherAspectsId, toSend);
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);

        this.internalAuraList = new PrimalAspectList();

        this.ticksExisted = compound.getInteger("ticksExisted");
        NBTTagList list = compound.getTagList("auraList", compound.getId());
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound cmp = list.getCompoundTagAt(i);
            if (cmp.hasKey("tag") && cmp.hasKey("amt")) {
                this.internalAuraList.add(Aspect.getAspect(cmp.getString("tag")), cmp.getInteger("amt"));
            }
        }
        if (compound.hasKey("activationVecX") && compound.hasKey("activationVecY")
                && compound.hasKey("activationVecZ")) {
            int x = compound.getInteger("activationVecX");
            int y = compound.getInteger("activationVecY");
            int z = compound.getInteger("activationVecZ");
            this.activationLocation = new ChunkCoordinates(x, y, z);
        }
        this.sendAspectData(this.electParliament());

        this.initGathering();

        this.blockCount = compound.getInteger("blockCount");
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);

        compound.setInteger("ticksExisted", this.ticksExisted);
        NBTTagList list = new NBTTagList();
        for (Aspect a : this.internalAuraList.getAspects()) {
            if (a == null) continue;
            NBTTagCompound aspectCompound = new NBTTagCompound();
            aspectCompound.setString("tag", a.getTag());
            aspectCompound.setInteger("amt", this.internalAuraList.getAmount(a));
            list.appendTag(aspectCompound);
        }
        compound.setTag("auraList", list);
        if (this.activationLocation != null) {
            compound.setInteger("activationVecX", this.activationLocation.posX);
            compound.setInteger("activationVecY", this.activationLocation.posY);
            compound.setInteger("activationVecZ", this.activationLocation.posZ);
        }

        compound.setInteger("blockCount", this.blockCount);
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeInt(this.ticksExisted);

        buffer.writeInt(this.activationLocation.posX);
        buffer.writeInt(this.activationLocation.posY);
        buffer.writeInt(this.activationLocation.posZ);
    }

    @Override
    public void readSpawnData(ByteBuf buffer) {
        this.ticksExisted = buffer.readInt();

        this.activationLocation = new ChunkCoordinates();
        this.activationLocation.posX = buffer.readInt();
        this.activationLocation.posY = buffer.readInt();
        this.activationLocation.posZ = buffer.readInt();
    }

    public static final class AspectWRItem extends WeightedRandom.Item {

        private final Aspect aspect;

        public AspectWRItem(int weight, Aspect aspect) {
            super(weight);
            this.aspect = aspect;
        }

        public Aspect getAspect() {
            return this.aspect;
        }
    }
}
