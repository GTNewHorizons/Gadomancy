package makeo.gadomancy.client.events;

import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.model.IModelCustom;
import net.minecraftforge.client.model.obj.WavefrontObject;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import makeo.gadomancy.api.GadomancyApi;
import makeo.gadomancy.api.golems.cores.AdditionalGolemCore;
import makeo.gadomancy.client.gui.GuiResearchRecipeAuraEffects;
import makeo.gadomancy.client.util.ExtendedTypeDisplayManager;
import makeo.gadomancy.client.util.FamiliarHandlerClient;
import makeo.gadomancy.client.util.MultiTickEffectDispatcher;
import makeo.gadomancy.common.CommonProxy;
import makeo.gadomancy.common.Gadomancy;
import makeo.gadomancy.common.blocks.tiles.TileExtendedNode;
import makeo.gadomancy.common.blocks.tiles.TileExtendedNodeJar;
import makeo.gadomancy.common.data.DataAchromatic;
import makeo.gadomancy.common.data.SyncDataHolder;
import makeo.gadomancy.common.registration.RegisteredBlocks;
import makeo.gadomancy.common.utils.Injector;
import makeo.gadomancy.common.utils.MiscUtils;
import makeo.gadomancy.common.utils.NBTHelper;
import makeo.gadomancy.common.utils.Vector3;
import thaumcraft.api.BlockCoordinates;
import thaumcraft.api.IArchitect;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.client.gui.GuiGolem;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.client.lib.REHWandHandler;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.relics.ItemThaumometer;
import thaumcraft.common.items.wands.ItemWandCasting;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p>
 * Created by makeo @ 13.10.2015 16:11
 */
public final class RenderEventHandler {

    private static final IModelCustom obj;
    private static final ResourceLocation texture = new ResourceLocation("gadomancy:textures/misc/texW.png");

    static {
        ResourceLocation resourceLocation = new ResourceLocation("gadomancy:textures/models/modelAssec.obj");
        IModelCustom buf;
        try {
            buf = new WavefrontObject(
                    "gadomancy:wRender",
                    new GZIPInputStream(
                            Minecraft.getMinecraft().getResourceManager().getResource(resourceLocation)
                                    .getInputStream()));
        } catch (Exception exc) {
            // shush.
            buf = null;
        }
        obj = buf;
    }

    private final REHWandHandler WAND_HANDLER = new REHWandHandler();
    private final FakeArchitectItem ARCHITECT_ITEM = new FakeArchitectItem();
    private Object oldGolemblurb;
    private int blurbId;
    private int dList = -1;

    @SubscribeEvent
    public void on(GuiScreenEvent.DrawScreenEvent.Pre e) {
        if (e.gui instanceof GuiGolem gui) {
            EntityGolemBase golem = new Injector(gui, GuiGolem.class).getField("golem");
            if (golem != null) {
                AdditionalGolemCore core = GadomancyApi.getAdditionalGolemCore(golem);
                if (core != null) {
                    this.blurbId = core.getBaseCore();
                    String key = "golemblurb." + this.blurbId + ".text";
                    this.oldGolemblurb = ResourceReloadListener.languageList.get(key);
                    ResourceReloadListener.languageList
                            .put(key, StatCollector.translateToLocal(core.getUnlocalizedGuiText()));
                }
            }
        }
    }

    @SubscribeEvent
    public void on(GuiScreenEvent.DrawScreenEvent.Post e) {
        if (this.oldGolemblurb != null) {
            String key = "golemblurb." + this.blurbId + ".text";
            ResourceReloadListener.languageList.put(key, this.oldGolemblurb);
            this.oldGolemblurb = null;
        }
    }

    @SubscribeEvent
    public void on(DrawBlockHighlightEvent e) {
        if (e.currentItem == null) return;
        if (e.currentItem.getItem() instanceof ItemWandCasting) {
            ItemFocusBasic focus = ((ItemWandCasting) e.currentItem.getItem()).getFocus(e.currentItem);
            if (!(focus instanceof IArchitect)) {
                Block block = e.player.worldObj.getBlock(e.target.blockX, e.target.blockY, e.target.blockZ);
                if (block != null && block == RegisteredBlocks.blockArcaneDropper) {
                    ForgeDirection dir = ForgeDirection.getOrientation(
                            e.player.worldObj.getBlockMetadata(e.target.blockX, e.target.blockY, e.target.blockZ) & 7);

                    ArrayList<BlockCoordinates> coords = new ArrayList<>();
                    for (int x = -1; x < 2; x++) {
                        for (int y = -1; y < 2; y++) {
                            for (int z = -1; z < 2; z++) {
                                coords.add(
                                        new BlockCoordinates(
                                                e.target.blockX + 2 * dir.offsetX + x,
                                                e.target.blockY + 2 * dir.offsetY + y,
                                                e.target.blockZ + 2 * dir.offsetZ + z));
                            }
                        }
                    }
                    coords.add(
                            new BlockCoordinates(
                                    e.target.blockX + dir.offsetX,
                                    e.target.blockY + dir.offsetY,
                                    e.target.blockZ + dir.offsetZ));

                    this.ARCHITECT_ITEM.setCoords(coords);

                    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
                    this.WAND_HANDLER.handleArchitectOverlay(
                            new ItemStack(this.ARCHITECT_ITEM),
                            e,
                            e.player.ticksExisted,
                            e.target);
                    GL11.glPopAttrib();
                }
            }
        } else if (e.currentItem.getItem() instanceof ItemThaumometer) {
            if (e.target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
            int blockX = e.target.blockX;
            int blockY = e.target.blockY;
            int blockZ = e.target.blockZ;
            if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 0) {
                TileEntity tile = e.player.worldObj.getTileEntity(blockX, blockY, blockZ);
                if (tile instanceof TileExtendedNode node) {
                    if (node.getExtendedNodeType() == null) return;
                    ExtendedTypeDisplayManager
                            .notifyDisplayTick(node.getId(), node.getNodeType(), node.getExtendedNodeType());
                } else if (tile instanceof TileExtendedNodeJar nodeJar) {
                    if (nodeJar.getExtendedNodeType() == null) return;
                    ExtendedTypeDisplayManager
                            .notifyDisplayTick(nodeJar.getId(), nodeJar.getNodeType(), nodeJar.getExtendedNodeType());
                }
            }
        }
    }

    @SubscribeEvent
    public void guiOpen(GuiOpenEvent event) {
        if (event.gui instanceof GuiResearchRecipe gui) {
            ResearchItem research = new Injector(gui, GuiResearchRecipe.class).getField("research");
            if (research.key.equals(Gadomancy.MODID.toUpperCase() + ".AURA_EFFECTS")
                    && !(gui instanceof GuiResearchRecipeAuraEffects)) {
                event.gui = GuiResearchRecipeAuraEffects.create(gui);
            }
        }
    }

    @SubscribeEvent
    public void worldRenderEvent(RenderWorldLastEvent event) {
        ExtendedTypeDisplayManager.notifyRenderTick();
        MultiTickEffectDispatcher.notifyRenderTick(Minecraft.getMinecraft().theWorld, event.partialTicks);
    }

    private EntityPlayer current;
    private ItemStack[] armor;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void renderEntityPre(RenderLivingEvent.Pre event) {
        if (event.entity instanceof EntityPlayer p) {
            if (((DataAchromatic) SyncDataHolder.getDataClient("AchromaticData")).isAchromatic(p)) {
                this.current = p;
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.15F);
                GL11.glDepthMask(false);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glAlphaFunc(GL11.GL_GREATER, 0.003921569F);
            }

            this.armor = p.inventory.armorInventory;
            p.inventory.armorInventory = new ItemStack[this.armor.length];
            System.arraycopy(this.armor, 0, p.inventory.armorInventory, 0, this.armor.length);

            boolean changed = false;
            for (int i = 0; i < this.armor.length; i++) {
                if (this.armor[i] != null && NBTHelper.hasPersistentData(this.armor[i])) {
                    NBTTagCompound compound = NBTHelper.getPersistentData(this.armor[i]);
                    if (compound.hasKey("disguise")) {
                        NBTBase base = compound.getTag("disguise");
                        if (base instanceof NBTTagCompound) {
                            p.inventory.armorInventory[i] = ItemStack.loadItemStackFromNBT((NBTTagCompound) base);
                        } else {
                            p.inventory.armorInventory[i] = null;
                        }
                        changed = true;
                    }
                }
            }

            if (!changed) {
                p.inventory.armorInventory = this.armor;
                this.armor = null;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void renderPost(RenderLivingEvent.Post event) {
        if (event.entity instanceof EntityPlayer p) {
            if (this.armor != null) {
                p.inventory.armorInventory = this.armor;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onSetArmor(RenderPlayerEvent.SetArmorModel event) {
        if (event.entityPlayer == this.current) {
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            GL11.glDepthMask(true);
        }
    }

    @SubscribeEvent
    public void onRender(RenderPlayerEvent.Specials.Post event) {
        if (event.entityPlayer == null) return;
        if (RenderEventHandler.obj == null) return;
        if (!CommonProxy.serverOnlineState) return;
        if (!MiscUtils.isMisunderstood(event.entityPlayer)) return;

        GL11.glColor4f(1f, 1f, 1f, 1f);

        GL11.glPushMatrix();
        Minecraft.getMinecraft().renderEngine.bindTexture(RenderEventHandler.texture);
        boolean f = event.entityPlayer.capabilities.isFlying;
        double ma = f ? 15 : 5;
        double r = (ma * (Math.abs((ClientHandler.ticks % 80) - 40) / 40D)) + ((65 - ma) * Math
                .max(0, Math.min(1, new Vector3(event.entityPlayer.motionX, 0, event.entityPlayer.motionZ).length())));
        GL11.glScaled(0.07, 0.07, 0.07);
        GL11.glRotatef(180, 0, 0, 1);
        GL11.glTranslated(0, -12.7, 0.7 - (((float) (r / ma)) * (f ? 0.5D : 0.2D)));
        if (this.dList == -1) {
            this.dList = GLAllocation.generateDisplayLists(2);
            GL11.glNewList(this.dList, GL11.GL_COMPILE);
            RenderEventHandler.obj.renderOnly("wR");
            GL11.glEndList();
            GL11.glNewList(this.dList + 1, GL11.GL_COMPILE);
            RenderEventHandler.obj.renderOnly("wL");
            GL11.glEndList();
        }
        GL11.glPushMatrix();
        GL11.glRotated(20D + r, 0, -1, 0);
        GL11.glCallList(this.dList);
        GL11.glPopMatrix();
        GL11.glPushMatrix();
        GL11.glRotated(20D + r, 0, 1, 0);
        GL11.glCallList(this.dList + 1);
        GL11.glPopMatrix();
        GL11.glPopMatrix();
    }

    @SubscribeEvent
    public void playerRenderEvent(RenderPlayerEvent.Post renderEvent) {
        FamiliarHandlerClient.playerRenderEvent(renderEvent.entityPlayer, renderEvent.partialRenderTick);
    }
}
