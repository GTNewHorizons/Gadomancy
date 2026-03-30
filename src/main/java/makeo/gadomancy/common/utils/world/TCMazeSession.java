package makeo.gadomancy.common.utils.world;

import java.time.Instant;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import org.apache.logging.log4j.LogManager;

import makeo.gadomancy.common.data.config.ModConfig;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.world.dim.Cell;
import thaumcraft.common.lib.world.dim.CellLoc;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by HellFirePvP @ 05.11.2015 14:12
 */
public class TCMazeSession {

    public final EntityPlayerMP player;
    public final Map<CellLoc, Short> chunksAffected;
    public final int originDimId;
    public final Vec3 originLocation;
    public final Instant sessionStartTime;
    public final ChunkCoordinates spawnPortalCoordinates;
    public ChunkCoordinates bossSpawnCoordinates;

    TCMazeSession(EntityPlayer owner, Map<CellLoc, Short> locations, int dim, Vec3 origin) {
        this.player = (EntityPlayerMP) owner;
        this.chunksAffected = locations;
        this.originDimId = dim;
        this.originLocation = origin;
        this.sessionStartTime = Instant.now();
        this.spawnPortalCoordinates = this.findPortal();
        this.bossSpawnCoordinates = null;
    }

    TCMazeSession() {
        this.player = null;
        this.chunksAffected = null;
        this.originDimId = 0;
        this.originLocation = null;
        this.sessionStartTime = null;
        this.spawnPortalCoordinates = null;
        this.bossSpawnCoordinates = null;
    }

    private ChunkCoordinates findPortal() {
        for (CellLoc chunk : this.chunksAffected.keySet()) {
            Short s = this.chunksAffected.get(chunk);
            Cell cell = new Cell(s);
            if (cell.feature == 1) {
                int x = (chunk.x * 16) + 8;
                int z = (chunk.z * 16) + 8;
                return new ChunkCoordinates(x, TCMazeHandler.TELEPORT_LAYER_Y, z);
            }
        }
        return null;
    }

    final void closeSession(boolean teleport) {
        TCMazeHandler.free(this.chunksAffected);
        if (teleport) {
            WorldUtil.tryTeleportBack(this.player, this.originDimId);
            this.player.setPositionAndUpdate(
                    this.originLocation.xCoord,
                    this.originLocation.yCoord,
                    this.originLocation.zCoord);
        }
    }

    final void startSession() {
        WorldServer ws = MinecraftServer.getServer().worldServerForDimension(ModConfig.dimOuterId);

        for (CellLoc loc : this.chunksAffected.keySet()) {
            long k = ChunkCoordIntPair.chunkXZ2Int(loc.x, loc.z);
            if (ws.theChunkProviderServer.loadedChunkHashMap.containsItem(k)) {
                Chunk c = (Chunk) ws.theChunkProviderServer.loadedChunkHashMap.getValueByKey(k);
                ws.theChunkProviderServer.loadedChunks.remove(c);
                ws.theChunkProviderServer.loadedChunkHashMap.remove(k);
            }
        }

        if (this.spawnPortalCoordinates == null) {
            LogManager.getLogger().error(
                    "Thaumcraft didn't generate a portal! Stopping instance! PLEASE REPORT THIS ERROR!",
                    new IllegalStateException());
            this.closeSession(false);
            this.player.addChatMessage(
                    new ChatComponentText(
                            EnumChatFormatting.RED
                                    + "Thaumcraft didn't generate a portal in the Eldritch dimension. Sorry, we can't teleport you.."));
        } else {
            WorldUtil.teleportToFakeOuter(this.player);
            this.player.setPositionAndUpdate(
                    this.spawnPortalCoordinates.posX + 0.5,
                    TCMazeHandler.TELEPORT_LAYER_Y,
                    this.spawnPortalCoordinates.posZ + 0.5);
            Thaumcraft.proxy.getResearchManager().completeResearch(this.player, "ENTEROUTER"); // badumm tss
        }
    }

    public static TCMazeSession placeholder() {
        return new TCMazeSession();
    }
}
