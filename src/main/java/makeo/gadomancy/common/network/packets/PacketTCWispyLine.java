package makeo.gadomancy.common.network.packets;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import makeo.gadomancy.client.util.MultiTickEffectDispatcher;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p/>
 * Created by HellFirePvP @ 28.10.2015 23:28
 */
public class PacketTCWispyLine implements IMessage, IMessageHandler<PacketTCWispyLine, IMessage> {

    private int dimId;
    private double pedestalX, pedestalY, pedestalZ;
    private double originX, originY, originZ;
    private int tickCap;
    private int colorAsInt;

    public PacketTCWispyLine() {}

    public PacketTCWispyLine(int dimId, double pedestalX, double pedestalY, double pedestalZ, double originX,
            double originY, double originZ, int tickCap, int colorAsInt) {
        this.dimId = dimId;
        this.pedestalX = pedestalX;
        this.pedestalY = pedestalY;
        this.pedestalZ = pedestalZ;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.tickCap = tickCap;
        this.colorAsInt = colorAsInt;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.dimId = buf.readInt();
        this.pedestalX = buf.readDouble();
        this.pedestalY = buf.readDouble();
        this.pedestalZ = buf.readDouble();
        this.originX = buf.readDouble();
        this.originY = buf.readDouble();
        this.originZ = buf.readDouble();
        this.tickCap = buf.readInt();
        this.colorAsInt = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.dimId);
        buf.writeDouble(this.pedestalX);
        buf.writeDouble(this.pedestalY);
        buf.writeDouble(this.pedestalZ);
        buf.writeDouble(this.originX);
        buf.writeDouble(this.originY);
        buf.writeDouble(this.originZ);
        buf.writeInt(this.tickCap);
        buf.writeInt(this.colorAsInt);
    }

    @Override
    public IMessage onMessage(PacketTCWispyLine p, MessageContext ctx) {
        MultiTickEffectDispatcher.FloatyLineInfo info = new MultiTickEffectDispatcher.FloatyLineInfo(
                p.dimId,
                p.pedestalX,
                p.pedestalY,
                p.pedestalZ,
                p.originX,
                p.originY,
                p.originZ,
                p.tickCap,
                p.colorAsInt);
        MultiTickEffectDispatcher.registerFloatyLine(info);
        return null;
    }
}
