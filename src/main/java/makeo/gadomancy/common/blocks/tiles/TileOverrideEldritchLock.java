package makeo.gadomancy.common.blocks.tiles;

import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.Entity;
import net.minecraft.util.ChunkCoordinates;

import makeo.gadomancy.common.utils.world.TCMazeHandler;
import makeo.gadomancy.common.utils.world.TCMazeSession;
import thaumcraft.common.lib.world.dim.Cell;
import thaumcraft.common.lib.world.dim.CellLoc;
import thaumcraft.common.lib.world.dim.MazeHandler;
import thaumcraft.common.tiles.TileEldritchLock;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p/>
 * Created by makeo @ 09.11.2015 21:20
 */
public class TileOverrideEldritchLock extends TileEldritchLock {

    @Override
    public void updateEntity() {
        if (!this.worldObj.isRemote) {
            if (super.count != -1) {
                if (super.count + 1 > 100) {
                    ConcurrentHashMap<CellLoc, Short> old = MazeHandler.labyrinth;
                    MazeHandler.labyrinth = TCMazeHandler.labyrinthCopy;

                    int nextEntityId = Entity.nextEntityID;

                    super.updateEntity();

                    int count = Entity.nextEntityID - nextEntityId;
                    Entity[] bosses = new Entity[count];
                    for (int i = 0; i < count; i++) {
                        bosses[i] = this.worldObj.getEntityByID(nextEntityId + i);
                    }

                    MazeHandler.labyrinth = old;
                    for (TCMazeSession session : TCMazeHandler.getSessions().values()) {
                        if (session.chunksAffected != null && session.chunksAffected
                                .containsKey(new CellLoc(this.xCoord >> 4, this.zCoord >> 4))) {
                            TCMazeHandler.putBosses(session, bosses);
                            int cx = this.xCoord >> 4;
                            int cz = this.zCoord >> 4;
                            int centerx = this.xCoord >> 4;
                            int centerz = this.zCoord >> 4;

                            for (int a = -2; a <= 2; ++a) {
                                for (int b = -2; b <= 2; ++b) {
                                    CellLoc here = new CellLoc(cx + a, cz + b);
                                    if (!session.chunksAffected.containsKey(here)) {
                                        continue;
                                    }
                                    Cell c = new Cell(session.chunksAffected.get(here));
                                    if (c.feature == 2) {
                                        centerx = cx + a;
                                        centerz = cz + b;
                                    }
                                }
                            }
                            centerx = centerx * 16 + 16;
                            centerz = centerz * 16 + 16;
                            session.bossSpawnCoordinates = new ChunkCoordinates(
                                    centerx,
                                    TCMazeHandler.TELEPORT_LAYER_Y,
                                    centerz);
                            break;
                        }
                    }
                    return;
                }
            }
        }

        super.updateEntity();
    }
}
