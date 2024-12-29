package makeo.gadomancy.common.blocks.tiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;

import makeo.gadomancy.common.utils.NBTHelper;
import thaumcraft.common.tiles.TileJarFillable;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by makeo @ 14.10.2015 15:06
 */
public class TileRemoteJar extends TileJarFillable {

    public UUID networkId;

    private int count;

    private boolean registered_to_network;

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (this.count % 3 == 0 && !this.getWorldObj().isRemote
                && this.networkId != null
                && (!this.registered_to_network || this.amount < this.maxAmount)) {
            this.count = 0;

            JarNetwork network = TileRemoteJar.getNetwork(this.networkId);

            this.registered_to_network = true;
            if (!network.jars.contains(this)) {
                network.jars.add((TileJarFillable) this.worldObj.getTileEntity(this.xCoord, this.yCoord, this.zCoord));
            }

            network.update();
        }
        this.count++;
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);

        this.networkId = NBTHelper.getUUID(compound, "networkId");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);

        if (this.networkId != null) {
            NBTHelper.setUUID(compound, "networkId", this.networkId);
        }
    }

    private static final Map<UUID, JarNetwork> networks = new HashMap<>();

    private static class JarNetwork {

        private long lastTime;
        private final List<TileJarFillable> jars = new ArrayList<>();

        private void update() {
            long time = MinecraftServer.getServer().getEntityWorld().getTotalWorldTime();
            if (time > this.lastTime) {
                if (this.jars.size() > 1) {

                    TileJarFillable high = null, low = null;
                    Iterator<TileJarFillable> itr = this.jars.iterator();
                    while (itr.hasNext()) {
                        TileJarFillable jar = itr.next();
                        if (isInvalid(jar)) {
                            itr.remove();
                            continue;
                        }

                        if (low == null || low.amount > jar.amount) {
                            low = jar;
                        }
                        if (high == null || high.amount <= jar.amount) {
                            high = jar;
                        }
                    }

                    if (low != high && (low.amount + 1) < high.amount && low.addToContainer(high.aspect, 1) == 0) {
                        high.takeFromContainer(high.aspect, 1);
                    }
                }
                this.lastTime = time + 3;
            }
        }

        private static boolean isInvalid(TileJarFillable jar) {
            return jar == null || jar.getWorldObj() == null
                    || jar.isInvalid()
                    || !jar.getWorldObj().blockExists(jar.xCoord, jar.yCoord, jar.zCoord);
        }
    }

    private static JarNetwork getNetwork(UUID id) {
        JarNetwork network = TileRemoteJar.networks.get(id);

        if (network == null) {
            network = new JarNetwork();
            TileRemoteJar.networks.put(id, network);
        }
        return network;
    }

    public void markForUpdate() {
        this.markDirty();
        this.getWorldObj().markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
    }
}
