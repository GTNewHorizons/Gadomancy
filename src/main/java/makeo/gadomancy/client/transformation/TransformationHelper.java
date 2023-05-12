package makeo.gadomancy.client.transformation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import makeo.gadomancy.client.renderers.entity.PlayerCameraRenderer;
import makeo.gadomancy.common.network.PacketHandler;
import makeo.gadomancy.common.network.packets.PacketAbortTransform;
import makeo.gadomancy.common.registration.RegisteredIntegrations;
import thaumcraft.common.entities.golems.EntityGolemBase;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p/>
 * Created by makeo @ 04.07.2015 02:38
 */
@SideOnly(Side.CLIENT)
public class TransformationHelper {

    private static final FakeEntityGolemBase DEFAULT_GOLEM;

    static {
        EntityGolemBase golem = new EntityGolemBase(null) {

            @Override
            public byte getCore() {
                return (byte) 4;
            }
        };
        DEFAULT_GOLEM = new FakeEntityGolemBase(golem, null);
    }

    private static PlayerCameraRenderer renderer;

    private static Map<Integer, Integer> transformedPlayers = new HashMap<Integer, Integer>();

    private static Map<Integer, FakeEntityGolemBase> entityCache = new HashMap<Integer, FakeEntityGolemBase>();

    private TransformationHelper() {}

    public static boolean isTransformable() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.thePlayer.equals(mc.renderViewEntity) && !RegisteredIntegrations.morph.isMorphed()
                && mc.entityRenderer.getClass().equals(EntityRenderer.class)
                && (TransformationHelper.renderer == null || TransformationHelper.renderer.isRemoved());
    }

    public static boolean isForeignTransformed() {
        Minecraft mc = Minecraft.getMinecraft();
        return !mc.renderViewEntity.equals(mc.thePlayer) || RegisteredIntegrations.morph.isMorphed()
                || (!mc.entityRenderer.getClass().equals(EntityRenderer.class)
                        && !(mc.entityRenderer instanceof PlayerCameraRenderer));
    }

    public static boolean isTransformed() {
        return TransformationHelper.renderer != null && !TransformationHelper.renderer.isMarkedForRemoval();
    }

    public static boolean isTransformed(EntityPlayer player) {
        if (player.equals(Minecraft.getMinecraft().thePlayer)) return TransformationHelper.isTransformed();
        return TransformationHelper.getTransformedEntityId(player) != -1;
    }

    public static FakeEntityGolemBase getTransformedEntity(EntityPlayer player) {
        Integer entityId = TransformationHelper.transformedPlayers.get(player.getEntityId());
        if (entityId != null) {
            FakeEntityGolemBase golem = TransformationHelper.entityCache.get(entityId);
            if (golem == null) {
                EntityGolemBase rawGolem = TransformationHelper.findGolem(entityId);
                if (rawGolem != null) {
                    golem = new FakeEntityGolemBase(rawGolem, player);
                    TransformationHelper.entityCache.put(entityId, golem);
                }
            }

            if (golem != null) {
                return golem;
            }
        }
        return TransformationHelper.DEFAULT_GOLEM;
    }

    private static EntityGolemBase findGolem(int entityId) {
        List<Entity> loaded = Minecraft.getMinecraft().thePlayer.worldObj.getLoadedEntityList();
        for (int i = 0; i < loaded.size(); i++) {
            Entity entity = loaded.get(i);
            if (entity instanceof EntityGolemBase && entity.getEntityId() == entityId) {
                return (EntityGolemBase) entity;
            }
        }
        return null;
    }

    public static FakeEntityGolemBase getTransformedEntity() {
        return TransformationHelper.getTransformedEntity(Minecraft.getMinecraft().thePlayer);
    }

    private static int getTransformedEntityId(EntityPlayer player) {
        Integer entityId = TransformationHelper.transformedPlayers.get(player.getEntityId());
        return entityId == null ? -1 : entityId;
    }

    public static int getTransformedEntityId() {
        return TransformationHelper.getTransformedEntityId(Minecraft.getMinecraft().thePlayer);
    }

    public static void cancelTransformation(PacketAbortTransform.AbortReason reason) {
        if (TransformationHelper.isTransformed()) {
            PacketHandler.INSTANCE.sendToServer(new PacketAbortTransform(reason));
            TransformationHelper.onAbortTransformation(Minecraft.getMinecraft().thePlayer.getEntityId());
        }
    }

    public static void onStartTransformation(int targetId, int appearanceId) {
        if (Minecraft.getMinecraft().thePlayer.getEntityId() == targetId) {
            if (TransformationHelper.isTransformable()) {
                TransformationHelper.transformedPlayers.put(targetId, appearanceId);

                Minecraft mc = Minecraft.getMinecraft();
                TransformationHelper.renderer = new PlayerCameraRenderer(mc, mc.entityRenderer);
                mc.entityRenderer = TransformationHelper.renderer;
            } else {
                TransformationHelper.cancelTransformation(PacketAbortTransform.AbortReason.TRANSFORMATION_FAILED);
            }
        } else {
            TransformationHelper.transformedPlayers.put(targetId, appearanceId);
        }
    }

    public static void onAbortTransformation(int targetId) {
        TransformationHelper.transformedPlayers.remove(targetId);
        TransformationHelper.entityCache.remove(targetId);

        if (Minecraft.getMinecraft().thePlayer.getEntityId() == targetId && TransformationHelper.isTransformed()) {
            TransformationHelper.renderer.markForRemoval();
        }
    }
}
