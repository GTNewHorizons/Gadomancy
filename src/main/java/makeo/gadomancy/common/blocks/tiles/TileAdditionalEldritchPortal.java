package makeo.gadomancy.common.blocks.tiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;

import makeo.gadomancy.common.data.config.ModConfig;
import makeo.gadomancy.common.utils.world.TCMazeHandler;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.tiles.TileEldritchPortal;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p/>
 * Created by makeo @ 06.11.2015 23:32
 */
public class TileAdditionalEldritchPortal extends TileEldritchPortal {

    private static final Map<EntityPlayer, ExtendedChunkCoordinates> trackedPortalActivity = new HashMap<>();
    private EntityPlayer toTeleport;
    private int count;

    private final Map<EntityPlayerMP, Integer> inPortalFor = new HashMap<>();
    private static final int REQUIRED_TELEPORT_TICKS = 20;

    @Override
    public void updateEntity() {
        this.count += 1;
        if ((this.worldObj.isRemote) && ((this.count % 250 == 0) || (this.count == 1))) {
            this.worldObj.playSound(
                    this.xCoord + 0.5D,
                    this.yCoord + 0.5D,
                    this.zCoord + 0.5D,
                    "thaumcraft:evilportal",
                    1.0F,
                    1.0F,
                    false);
        }

        // necessary for proper rendering of the portals visual fx
        if ((this.worldObj.isRemote) && (this.opencount < 30)) {
            this.opencount += 1;
        }

        if ((!this.worldObj.isRemote) && (this.count % 5 == 0) && this.toTeleport == null) {
            AxisAlignedBB portal = AxisAlignedBB.getBoundingBox(
                    this.xCoord,
                    this.yCoord,
                    this.zCoord,
                    this.xCoord + 1,
                    this.yCoord + 1,
                    this.zCoord + 1).expand(0.5D, 1.0D, 0.5D);
            List<EntityPlayerMP> playersInPortal = this.worldObj.getEntitiesWithinAABB(EntityPlayerMP.class, portal);

            // Remove portal progress for any player who has left the portal this tick.
            this.inPortalFor.keySet()
                    .removeIf(tracked -> playersInPortal == null || !playersInPortal.contains(tracked));

            if (playersInPortal != null && !playersInPortal.isEmpty()) {
                EntityPlayerMP toTeleport = playersInPortal.get(0);

                if (toTeleport.timeUntilPortal > 0) return;

                if (!ResearchManager.isResearchComplete(
                        toTeleport.getCommandSenderName(),
                        TileNodeManipulator.MultiblockType.E_PORTAL_CREATOR.getResearchNeeded())) {
                    return;
                }

                int timeInPortal = this.inPortalFor.getOrDefault(toTeleport, 0);
                timeInPortal += 5;
                this.inPortalFor.put(toTeleport, timeInPortal);

                if (timeInPortal < REQUIRED_TELEPORT_TICKS) {
                    return;
                }

                this.toTeleport = toTeleport;
                toTeleport.timeUntilPortal = 200;

                if (toTeleport.dimension != ModConfig.dimOuterId) {
                    // Teleporting there.
                    if (TCMazeHandler.createSessionWaitForTeleport(toTeleport)) {
                        TileAdditionalEldritchPortal.startTracking(
                                toTeleport,
                                new ExtendedChunkCoordinates(
                                        new ChunkCoordinates(this.xCoord, this.yCoord, this.zCoord),
                                        toTeleport.dimension));
                    }
                } else {
                    // Teleporting back.
                    TCMazeHandler.closeSession(toTeleport, true);
                }
            }
        }
    }

    public static void startTracking(EntityPlayer player, ExtendedChunkCoordinates tileCoords) {
        if (!TileAdditionalEldritchPortal.trackedPortalActivity.containsKey(player)) {
            TileAdditionalEldritchPortal.trackedPortalActivity.put(player, tileCoords);
        }
    }

    public static void informSessionStart(EntityPlayer player) {
        if (TileAdditionalEldritchPortal.trackedPortalActivity.containsKey(player)) {
            ExtendedChunkCoordinates tileCoords = TileAdditionalEldritchPortal.trackedPortalActivity.get(player);
            TileAdditionalEldritchPortal.trackedPortalActivity.remove(player);
            if (tileCoords != null) {
                ChunkCoordinates cc = tileCoords.coordinates;
                WorldServer ws = MinecraftServer.getServer().worldServerForDimension(tileCoords.dimId);
                ws.removeTileEntity(cc.posX, cc.posY, cc.posZ);
                ws.setBlockToAir(cc.posX, cc.posY, cc.posZ);
                ws.markBlockForUpdate(cc.posX, cc.posY, cc.posZ);
            }
        }
    }

    public static class ExtendedChunkCoordinates {

        private final ChunkCoordinates coordinates;
        private final int dimId;

        public ExtendedChunkCoordinates(ChunkCoordinates coordinates, int dimId) {
            this.coordinates = coordinates;
            this.dimId = dimId;
        }
    }
}
