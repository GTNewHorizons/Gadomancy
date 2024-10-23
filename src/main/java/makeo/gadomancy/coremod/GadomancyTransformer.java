package makeo.gadomancy.coremod;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import cpw.mods.fml.common.FMLLog;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p/>
 * Created by makeo @ 07.12.2015 21:48
 */
public class GadomancyTransformer implements IClassTransformer {

    private static final String NAME_ENCHANTMENT_HELPER = "net.minecraft.enchantment.EnchantmentHelper";
    private static final String NAME_WANDMANAGER = "thaumcraft.common.items.wands.WandManager";
    private static final String NAME_NODE_RENDERER = "thaumcraft.client.renderers.tile.TileNodeRenderer";
    private static final String NAME_RENDER_EVENT_HANDLER = "thaumcraft.client.lib.RenderEventHandler";
    private static final String NAME_NEI_ITEMPANEL = "codechicken.nei.ItemPanel";
    private static final String NAME_GOLEM_ENUM = "thaumcraft.common.entities.golems.EnumGolemType";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;
        boolean needsTransform = transformedName.equals(GadomancyTransformer.NAME_ENCHANTMENT_HELPER)
                || transformedName.equals(GadomancyTransformer.NAME_WANDMANAGER)
                || transformedName.equals(GadomancyTransformer.NAME_NODE_RENDERER)
                || transformedName.equals(GadomancyTransformer.NAME_RENDER_EVENT_HANDLER)
                || transformedName.equals(GadomancyTransformer.NAME_NEI_ITEMPANEL)
                || transformedName.equals(GadomancyTransformer.NAME_GOLEM_ENUM);
        if (!needsTransform) {
            return basicClass;
        }

        FMLLog.fine("[GadomancyTransformer] Transforming " + name + ": " + transformedName);

        ClassNode node = new ClassNode();
        ClassReader reader = new ClassReader(basicClass);
        reader.accept(node, 0);

        switch (transformedName) {
            case GadomancyTransformer.NAME_ENCHANTMENT_HELPER:
                for (MethodNode mn : node.methods) {
                    // TODO fix mappings to obf since we run at index 0 now
                    if (mn.name.equals("getFortuneModifier") || mn.name.equals("func_77517_e")) {
                        mn.instructions = new InsnList();

                        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        mn.instructions.add(
                                new MethodInsnNode(
                                        Opcodes.INVOKESTATIC,
                                        "makeo/gadomancy/common/events/EventHandlerRedirect",
                                        "getFortuneLevel",
                                        "(Lnet/minecraft/entity/EntityLivingBase;)I",
                                        false));
                        mn.instructions.add(new InsnNode(Opcodes.IRETURN));

                    } else if (mn.name.equals("getEnchantmentLevel") || mn.name.equals("func_77506_a")) {
                        mn.instructions = new InsnList();

                        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 0));
                        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        mn.instructions.add(
                                new MethodInsnNode(
                                        Opcodes.INVOKESTATIC,
                                        "makeo/gadomancy/common/events/EventHandlerRedirect",
                                        "onGetEnchantmentLevel",
                                        "(ILnet/minecraft/item/ItemStack;)I",
                                        false));
                        mn.instructions.add(new InsnNode(Opcodes.IRETURN));
                    }
                }
                break;
            case GadomancyTransformer.NAME_WANDMANAGER:
                for (MethodNode mn : node.methods) {
                    if (mn.name.equals("getTotalVisDiscount")) {
                        InsnList updateTotal = new InsnList();

                        updateTotal.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        updateTotal.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        updateTotal.add(new VarInsnNode(Opcodes.ILOAD, 2));
                        updateTotal.add(
                                new MethodInsnNode(
                                        Opcodes.INVOKESTATIC,
                                        "makeo/gadomancy/common/events/EventHandlerRedirect",
                                        "getAdditionalVisDiscount",
                                        "(Lnet/minecraft/entity/player/EntityPlayer;Lthaumcraft/api/aspects/Aspect;I)I",
                                        false));

                        mn.instructions.insertBefore(mn.instructions.get(mn.instructions.size() - 5), updateTotal);
                    }
                }
                break;
            case GadomancyTransformer.NAME_NODE_RENDERER:
                for (MethodNode mn : node.methods) {
                    if (mn.name.equals("renderTileEntityAt")) {
                        InsnList setBefore = new InsnList();
                        setBefore.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        setBefore.add(
                                new MethodInsnNode(
                                        Opcodes.INVOKESTATIC,
                                        "makeo/gadomancy/common/events/EventHandlerRedirect",
                                        "preNodeRender",
                                        "(Lnet/minecraft/tileentity/TileEntity;)V",
                                        false));

                        mn.instructions.insertBefore(mn.instructions.get(0), setBefore);

                        AbstractInsnNode next = mn.instructions.get(0);
                        while (next != null) {
                            AbstractInsnNode insnNode = next;
                            next = insnNode.getNext();

                            if (insnNode.getOpcode() == Opcodes.RETURN) {
                                InsnList setAfter = new InsnList();
                                setAfter.add(new VarInsnNode(Opcodes.ALOAD, 1));
                                setAfter.add(
                                        new MethodInsnNode(
                                                Opcodes.INVOKESTATIC,
                                                "makeo/gadomancy/common/events/EventHandlerRedirect",
                                                "postNodeRender",
                                                "(Lnet/minecraft/tileentity/TileEntity;)V",
                                                false));
                                mn.instructions.insertBefore(insnNode, setAfter);
                            }
                        }
                    }
                }
                break;
            case GadomancyTransformer.NAME_RENDER_EVENT_HANDLER:
                for (MethodNode mn : node.methods) {
                    if (mn.name.equals("blockHighlight")) {
                        InsnList setBefore = new InsnList();
                        setBefore.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        setBefore.add(
                                new MethodInsnNode(
                                        Opcodes.INVOKESTATIC,
                                        "makeo/gadomancy/common/events/EventHandlerRedirect",
                                        "preBlockHighlight",
                                        "(Lnet/minecraftforge/client/event/DrawBlockHighlightEvent;)V",
                                        false));

                        mn.instructions.insertBefore(mn.instructions.get(0), setBefore);

                        AbstractInsnNode next = mn.instructions.get(0);
                        while (next != null) {
                            AbstractInsnNode insnNode = next;
                            next = insnNode.getNext();

                            if (insnNode.getOpcode() == Opcodes.RETURN) {
                                InsnList setAfter = new InsnList();
                                setAfter.add(new VarInsnNode(Opcodes.ALOAD, 1));
                                setAfter.add(
                                        new MethodInsnNode(
                                                Opcodes.INVOKESTATIC,
                                                "makeo/gadomancy/common/events/EventHandlerRedirect",
                                                "postBlockHighlight",
                                                "(Lnet/minecraftforge/client/event/DrawBlockHighlightEvent;)V",
                                                false));
                                mn.instructions.insertBefore(insnNode, setAfter);
                            }
                        }
                    }
                }
                break;
            case GadomancyTransformer.NAME_NEI_ITEMPANEL:
                for (MethodNode mn : node.methods) {
                    if (mn.name.equals("updateItemList")) {
                        InsnList newInstructions = new InsnList();
                        newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        newInstructions.add(
                                new MethodInsnNode(
                                        Opcodes.INVOKESTATIC,
                                        "makeo/gadomancy/common/integration/IntegrationNEI",
                                        "checkItems",
                                        "(Ljava/util/ArrayList;)V",
                                        false));
                        newInstructions.add(mn.instructions);
                        mn.instructions = newInstructions;
                    }
                }
                break;
            case GadomancyTransformer.NAME_GOLEM_ENUM:
                // Create constructor accessor
                final MethodVisitor methodVisitor = node.visitMethod(
                        Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                        "gadomancyRawCreate",
                        "(Ljava/lang/String;IIIFZIIII)Lthaumcraft/common/entities/golems/EnumGolemType;",
                        null,
                        null);
                methodVisitor.visitCode();
                methodVisitor.visitTypeInsn(Opcodes.NEW, "thaumcraft/common/entities/golems/EnumGolemType");
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitVarInsn(Opcodes.ILOAD, 1);
                methodVisitor.visitVarInsn(Opcodes.ILOAD, 2);
                methodVisitor.visitVarInsn(Opcodes.ILOAD, 3);
                methodVisitor.visitVarInsn(Opcodes.FLOAD, 4);
                methodVisitor.visitVarInsn(Opcodes.ILOAD, 5);
                methodVisitor.visitVarInsn(Opcodes.ILOAD, 6);
                methodVisitor.visitVarInsn(Opcodes.ILOAD, 7);
                methodVisitor.visitVarInsn(Opcodes.ILOAD, 8);
                methodVisitor.visitVarInsn(Opcodes.ILOAD, 9);
                methodVisitor.visitMethodInsn(
                        Opcodes.INVOKESPECIAL,
                        "thaumcraft/common/entities/golems/EnumGolemType",
                        "<init>",
                        "(Ljava/lang/String;IIIFZIIII)V",
                        false);
                methodVisitor.visitInsn(Opcodes.ARETURN);
                methodVisitor.visitMaxs(12, 10);
                methodVisitor.visitEnd();
                break;
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
}
