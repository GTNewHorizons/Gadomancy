package makeo.gadomancy.common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import makeo.gadomancy.common.aura.AuraEffects;
import makeo.gadomancy.common.containers.ContainerArcanePackager;
import makeo.gadomancy.common.containers.ContainerInfusionClaw;
import makeo.gadomancy.common.data.SyncDataHolder;
import makeo.gadomancy.common.data.config.ModConfig;
import makeo.gadomancy.common.events.EventHandlerEntity;
import makeo.gadomancy.common.events.EventHandlerGolem;
import makeo.gadomancy.common.events.EventHandlerNetwork;
import makeo.gadomancy.common.events.EventHandlerWorld;
import makeo.gadomancy.common.network.PacketHandler;
import makeo.gadomancy.common.network.packets.PacketStartAnimation;
import makeo.gadomancy.common.registration.ModSubstitutions;
import makeo.gadomancy.common.registration.RegisteredBlocks;
import makeo.gadomancy.common.registration.RegisteredEnchantments;
import makeo.gadomancy.common.registration.RegisteredEntities;
import makeo.gadomancy.common.registration.RegisteredGolemStuff;
import makeo.gadomancy.common.registration.RegisteredIntegrations;
import makeo.gadomancy.common.registration.RegisteredItems;
import makeo.gadomancy.common.registration.RegisteredPotions;
import makeo.gadomancy.common.registration.RegisteredRecipes;
import makeo.gadomancy.common.registration.RegisteredResearches;
import makeo.gadomancy.common.utils.Injector;
import makeo.gadomancy.common.utils.world.WorldProviderTCEldrich;
import thaumcraft.api.wands.WandTriggerRegistry;
import thaumcraft.common.entities.golems.ContainerGolem;
import thaumcraft.common.entities.golems.EntityGolemBase;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p>
 * Created by makeo @ 29.11.2014 14:18
 */
public class CommonProxy implements IGuiHandler {

    public static boolean serverOnlineState;

    public void onConstruct() {}

    public void preInitalize() {
        PacketHandler.init();
        RegisteredItems.preInit();
        RegisteredBlocks.init();
        RegisteredItems.init();
        RegisteredGolemStuff.init();
    }

    public void initalize() {
        NetworkRegistry.INSTANCE.registerGuiHandler(Gadomancy.instance, this);
        RegisteredEnchantments.init();
        RegisteredRecipes.init();
        SyncDataHolder.initialize();
        RegisteredEntities.init();
        DimensionManager.registerProviderType(ModConfig.dimOuterId, WorldProviderTCEldrich.class, true);
        DimensionManager.registerDimension(ModConfig.dimOuterId, ModConfig.dimOuterId);
    }

    public void postInitalize() {
        RegisteredPotions.init();
        AuraEffects.AER.getTickInterval(); // initalize AuraEffects
        RegisteredResearches.init();
        RegisteredIntegrations.init();
        RegisteredResearches.postInit();
        RegisteredItems.postInit();
        ModSubstitutions.postInit();
    }

    public static void unregisterWandHandler(String modid, Block block, int metadata) {
        HashMap<String, HashMap<List, List>> triggers = new Injector(WandTriggerRegistry.class).getField("triggers");
        HashMap<List, List> modTriggers = triggers.get(modid);
        if (modTriggers == null) return;
        List arrKey = Arrays.asList(block, metadata);
        modTriggers.remove(arrKey);
        triggers.put(modid, modTriggers);
    }

    public void spawnBubbles(World world, float posX, float posY, float posZ, float rangeAroundItem) {
        PacketStartAnimation pkt = new PacketStartAnimation(
                PacketStartAnimation.ID_BUBBLES,
                Float.floatToIntBits(posX),
                Float.floatToIntBits(posY),
                Float.floatToIntBits(posZ),
                Float.floatToIntBits(rangeAroundItem));
        PacketHandler.INSTANCE.sendToAllAround(
                pkt,
                new NetworkRegistry.TargetPoint(world.provider.dimensionId, posX, posY, posZ, 32));
    }

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return switch (ID) {
            case 0 -> new ContainerGolem(player.inventory, ((EntityGolemBase) world.getEntityByID(x)).inventory);
            case 1 -> new ContainerInfusionClaw(player.inventory, (IInventory) world.getTileEntity(x, y, z));
            case 2 -> new ContainerArcanePackager(player.inventory, (IInventory) world.getTileEntity(x, y, z));
            default -> null;
        };
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    public void runDelayedClientSide(Runnable run) {}

    public Side getSide() {
        return Side.SERVER;
    }

    public EventHandlerGolem EVENT_HANDLER_GOLEM;
    public EventHandlerNetwork EVENT_HANDLER_NETWORK;
    public EventHandlerWorld EVENT_HANDLER_WORLD;
    public EventHandlerEntity EVENT_HANDLER_ENTITY;

    public void onServerAboutToStart(FMLServerAboutToStartEvent event) {
        EVENT_HANDLER_GOLEM = new EventHandlerGolem();
        MinecraftForge.EVENT_BUS.register(EVENT_HANDLER_GOLEM);
        EVENT_HANDLER_NETWORK = new EventHandlerNetwork();
        FMLCommonHandler.instance().bus().register(EVENT_HANDLER_NETWORK);
        EVENT_HANDLER_WORLD = new EventHandlerWorld();
        MinecraftForge.EVENT_BUS.register(EVENT_HANDLER_WORLD);
        FMLCommonHandler.instance().bus().register(EVENT_HANDLER_WORLD);
        EVENT_HANDLER_ENTITY = new EventHandlerEntity();
        MinecraftForge.EVENT_BUS.register(EVENT_HANDLER_ENTITY);
    }

    public void onServerStopped(FMLServerStoppedEvent event) {
        MinecraftForge.EVENT_BUS.unregister(EVENT_HANDLER_GOLEM);
        EVENT_HANDLER_GOLEM = null;
        FMLCommonHandler.instance().bus().unregister(EVENT_HANDLER_NETWORK);
        EVENT_HANDLER_NETWORK = null;
        MinecraftForge.EVENT_BUS.unregister(EVENT_HANDLER_WORLD);
        FMLCommonHandler.instance().bus().unregister(EVENT_HANDLER_WORLD);
        EVENT_HANDLER_WORLD = null;
        MinecraftForge.EVENT_BUS.unregister(EVENT_HANDLER_ENTITY);
        EVENT_HANDLER_ENTITY = null;
    }
}
