package makeo.gadomancy.common.blocks.tiles;

import java.util.ArrayList;
import java.util.HashMap;
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

    private boolean shouldUpdate() {
        return this.count % 3 == 0 && !this.getWorldObj().isRemote
                && this.networkId != null
                && (!this.registered_to_network || this.amount < this.maxAmount);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();

        if (shouldUpdate()) {
            this.count = 0;

            JarNetwork network = TileRemoteJar.getNetwork(this.networkId);

            if (handleNetworkConnections(network)) {
                network.update();
                this.registered_to_network = true;
            }
        }

        this.count++;
    }

    private boolean handleNetworkConnections(JarNetwork network) {
        int networkCapacity = network.jars.size();

        // Network requiring jars for operation, registering jar...
        if (networkCapacity <= 2) {
            if (!network.jars.contains(this)) {
                network.jars.add((TileJarFillable) this.worldObj.getTileEntity(this.xCoord, this.yCoord, this.zCoord));
            }

            return true;
        }

        this.networkId = null;
        this.registered_to_network = false;
        return false;
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

    private static final Map<UUID, JarNetwork> networks = new HashMap<UUID, JarNetwork>();

    private static class JarNetwork {

        private long lastTime;
        private final List<TileJarFillable> jars = new ArrayList<TileJarFillable>(2);

        private void update() {
            long time = MinecraftServer.getServer().getEntityWorld().getTotalWorldTime();
            if (time > this.lastTime) {

                // Not enough jars...
                if (this.jars.size() < 2) return;

                // Just enough jars...
                if (this.jars.size() == 2) {
                    if (hasProcessedJars()) this.lastTime = time + 3;
                    return;
                }

                // Too many jars. Refreshing network...
                this.jars.clear();
            }
        }

        private boolean hasProcessedJars() {

            TileJarFillable jar1 = this.jars.get(0);
            if (!JarNetwork.isValid(jars.get(0))) {
                this.jars.remove(0);
                return false;
            }

            TileJarFillable jar2 = this.jars.get(1);
            if (!JarNetwork.isValid(jars.get(1))) {
                this.jars.remove(1);
                return false;
            }

            // Transfer Essence if necessary
            if (Math.abs(jar1.amount - jar2.amount) > 1) {

                TileJarFillable sourceJar = (jar1.amount > jar2.amount) ? jar1 : jar2;
                TileJarFillable destinationJar = (sourceJar == jar1) ? jar2 : jar1;

                if (destinationJar.addToContainer(sourceJar.aspect, 1) == 0) {
                    sourceJar.takeFromContainer(sourceJar.aspect, 1);
                }
            }
            return true;
        }

        private static boolean isValid(TileJarFillable jar) {
            return jar != null && jar.getWorldObj() != null
                    && !jar.isInvalid()
                    && jar.getWorldObj().blockExists(jar.xCoord, jar.yCoord, jar.zCoord);
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
