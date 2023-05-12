package makeo.gadomancy.common.registration;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;
import makeo.gadomancy.api.ClickBehavior;
import makeo.gadomancy.common.blocks.*;
import makeo.gadomancy.common.blocks.tiles.*;
import makeo.gadomancy.common.data.config.ModConfig;
import makeo.gadomancy.common.items.*;
import thaumcraft.common.blocks.BlockAiry;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.tiles.TileInfusionMatrix;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 *
 * Created by makeo @ 13.07.2015 18:38
 */
public class RegisteredBlocks {

    private RegisteredBlocks() {}

    public static int rendererTransparentBlock;
    public static int rendererExtendedNodeJarBlock;
    public static int rendererBlockStoneMachine;

    public static BlockStickyJar blockStickyJar;
    public static BlockArcaneDropper blockArcaneDropper;
    public static BlockInfusionClaw blockInfusionClaw;
    public static BlockRemoteJar blockRemoteJar;
    public static BlockAiry blockNode = (BlockAiry) ConfigBlocks.blockAiry;
    public static BlockExtendedNodeJar blockExtendedNodeJar;
    public static BlockNodeManipulator blockNodeManipulator;
    public static BlockStoneMachine blockStoneMachine;
    public static BlockAdditionalEldritchPortal blockAdditionalEldrichPortal;
    public static BlockAuraPylon blockAuraPylon;
    public static BlockKnowledgeBook blockKnowledgeBook;
    public static BlockEssentiaCompressor blockEssentiaCompressor;

    public static void init() {
        RegisteredBlocks.registerBlocks();

        RegisteredBlocks.registerTileEntities();

        RegisteredBlocks.registerDefaultStickyJars();
        RegisteredBlocks.registerDefaultClawBehaviors();
    }

    // Blocks
    private static void registerBlocks() {
        RegisteredBlocks.blockStickyJar = RegisteredBlocks.registerBlock(new BlockStickyJar());
        RegisteredBlocks.blockArcaneDropper = RegisteredBlocks.registerBlock(new BlockArcaneDropper());
        RegisteredBlocks.blockInfusionClaw = RegisteredBlocks.registerBlock(new BlockInfusionClaw());
        RegisteredBlocks.blockRemoteJar = RegisteredBlocks
                .registerBlock(new BlockRemoteJar(), ItemBlockRemoteJar.class);
        RegisteredBlocks.blockNode = ModConfig.enableAdditionalNodeTypes
                ? (BlockAiry) new BlockNode().setBlockName("blockAiry")
                : (BlockAiry) ConfigBlocks.blockAiry;
        RegisteredBlocks.blockExtendedNodeJar = RegisteredBlocks.registerBlock(new BlockExtendedNodeJar());
        RegisteredBlocks.blockNodeManipulator = RegisteredBlocks
                .registerBlock(new BlockNodeManipulator(), ItemNodeManipulator.class);
        RegisteredBlocks.blockStoneMachine = RegisteredBlocks
                .registerBlock(new BlockStoneMachine(), ItemBlockStoneMachine.class);
        RegisteredBlocks.blockAdditionalEldrichPortal = RegisteredBlocks
                .registerBlock(new BlockAdditionalEldritchPortal(), ItemBlockAdditionalEldritchPortal.class);
        RegisteredBlocks.blockAuraPylon = RegisteredBlocks
                .registerBlock(new BlockAuraPylon(), ItemBlockAuraPylon.class);
        RegisteredBlocks.blockKnowledgeBook = RegisteredBlocks
                .registerBlock(new BlockKnowledgeBook(), ItemBlockKnowledgeBook.class);
        RegisteredBlocks.blockEssentiaCompressor = RegisteredBlocks
                .registerBlock(new BlockEssentiaCompressor(), ItemBlockEssentiaCompressor.class);
    }

    private static <T extends Block> T registerBlock(String name, T block) {
        block.setBlockName(name);
        GameRegistry.registerBlock(block, name);
        return block;
    }

    private static <T extends Block> T registerBlock(String name, T block, Class<? extends ItemBlock> itemClass) {
        block.setBlockName(name);
        GameRegistry.registerBlock(block, itemClass, name);
        return block;
    }

    private static <T extends Block> T registerBlock(T block, Class<? extends ItemBlock> itemClass) {
        RegisteredBlocks.registerBlock(block.getClass().getSimpleName(), block, itemClass);
        return block;
    }

    private static <T extends Block> T registerBlock(T block) {
        RegisteredBlocks.registerBlock(block.getClass().getSimpleName(), block);
        return block;
    }

    // Tiles
    private static void registerTileEntities() {
        RegisteredBlocks.registerTile(TileStickyJar.class);
        RegisteredBlocks.registerTile(TileArcaneDropper.class);
        RegisteredBlocks.registerTile(TileInfusionClaw.class);
        RegisteredBlocks.registerTile(TileRemoteJar.class);
        RegisteredBlocks.registerTile(TileExtendedNode.class);
        RegisteredBlocks.registerTile(TileExtendedNodeJar.class);
        RegisteredBlocks.registerTile(TileNodeManipulator.class);
        RegisteredBlocks.registerTile(TileManipulatorPillar.class);
        RegisteredBlocks.registerTile(TileManipulationFocus.class);
        RegisteredBlocks.registerTile(TileAdditionalEldritchPortal.class);
        RegisteredBlocks.registerTile(TileBlockProtector.class);
        RegisteredBlocks.registerTile(TileAuraPylon.class);
        RegisteredBlocks.registerTile(TileAuraPylonTop.class);
        RegisteredBlocks.registerTile(TileArcanePackager.class);
        RegisteredBlocks.registerTile(TileKnowledgeBook.class);
        RegisteredBlocks.registerTile(TileEssentiaCompressor.class);
        // registerTile(TileAIShutdown.class);
    }

    private static void registerTile(Class<? extends TileEntity> tile, String name) {
        GameRegistry.registerTileEntity(tile, name);
    }

    private static void registerTile(Class<? extends TileEntity> tile) {
        RegisteredBlocks.registerTile(tile, tile.getSimpleName());
    }

    // Sticky Jars
    private static void registerDefaultStickyJars() {
        RegisteredBlocks.registerStickyJar(ConfigBlocks.blockJar, 0, true, true);
        RegisteredBlocks.registerStickyJar(ConfigBlocks.blockJar, 3, true, true);

        RegisteredBlocks.registerStickyJar(RegisteredBlocks.blockRemoteJar, 0, false, false);
    }

    private static List<StickyJarInfo> stickyJars = new ArrayList<StickyJarInfo>();

    public static void registerStickyJar(Block block, int metadata, boolean isLabelable, boolean isPhialable) {
        RegisteredBlocks.stickyJars.add(0, new StickyJarInfo(block, metadata, isLabelable, isPhialable));
    }

    public static StickyJarInfo getStickyJarInfo(Block block, int metadata) {
        for (StickyJarInfo info : RegisteredBlocks.stickyJars) {
            if (info.getBlock() == block && info.getMetadata() == metadata) {
                return info;
            }
        }
        return null;
    }

    // Infusion Claw
    private static void registerDefaultClawBehaviors() {
        RegisteredBlocks.registerClawClickBehavior(new ClickBehavior() {

            @Override
            public boolean isValidForBlock() {
                return (this.block == Blocks.bookshelf && this.metadata == 0)
                        || (this.block == Blocks.cauldron && this.metadata == 0);
            }
        });

        RegisteredBlocks.registerClawClickBehavior(new ClickBehavior(true) {

            private TileInfusionMatrix matrix;

            @Override
            public boolean isValidForBlock() {
                if (this.block == ConfigBlocks.blockStoneDevice && this.metadata == 2) {
                    this.matrix = (TileInfusionMatrix) this.world.getTileEntity(this.x, this.y, this.z);
                    return true;
                }
                return false;
            }

            @Override
            public void addInstability(int instability) {
                this.matrix.instability += instability;
            }

            @Override
            public int getComparatorOutput() {
                return this.matrix.crafting ? 15 : 0;
            }
        });

        /*
         * registerClawClickBehavior(new ClickBehavior(true) {
         * @Override public boolean isValidForBlock() { return block.equals(ConfigBlocks.blockCrystal) && metadata <= 6;
         * }
         * @Override public AspectList getVisCost() { return RegisteredRecipes.costsAuraCoreStart; } });
         */
    }

    private static List<ClickBehavior> clawBehaviors = new ArrayList<ClickBehavior>();

    public static void registerClawClickBehavior(ClickBehavior behavior) {
        RegisteredBlocks.clawBehaviors.add(behavior);
    }

    public static ClickBehavior getClawClickBehavior(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        int metadata = world.getBlockMetadata(x, y, z);

        for (ClickBehavior behavior : RegisteredBlocks.clawBehaviors) {
            behavior.init(world, block, x, y, z, metadata);
            if (behavior.isValidForBlock()) {
                return behavior;
            }
        }
        return null;
    }

    public static List<ClickBehavior> getClawClickBehaviors() {
        return RegisteredBlocks.clawBehaviors;
    }
}
