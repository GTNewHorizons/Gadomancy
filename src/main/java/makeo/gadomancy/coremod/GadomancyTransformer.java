package makeo.gadomancy.coremod;

import java.io.IOException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.asm.transformers.AccessTransformer;

/**
 * This class is part of the Gadomancy Mod Gadomancy is Open Source and distributed under the GNU LESSER GENERAL PUBLIC
 * LICENSE for more read the LICENSE file
 * <p/>
 * Created by makeo @ 07.12.2015 21:48
 */
public class GadomancyTransformer extends AccessTransformer {

    public static final String NAME_GOLEM_ENUM = "thaumcraft.common.entities.golems.EnumGolemType";

    public GadomancyTransformer() throws IOException {}

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        boolean needsTransform = transformedName.equalsIgnoreCase(GadomancyTransformer.NAME_GOLEM_ENUM);
        if (!needsTransform) return super.transform(name, transformedName, bytes);

        FMLLog.info("[GadomancyTransformer] Transforming " + name + ": " + transformedName);

        ClassNode node = new ClassNode();
        ClassReader reader = new ClassReader(bytes);
        reader.accept(node, 0);

        if (transformedName.equalsIgnoreCase(GadomancyTransformer.NAME_GOLEM_ENUM)) {
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
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        bytes = writer.toByteArray();
        return super.transform(name, transformedName, bytes);
    }
}
