package zenovka.gt6materialexpansion.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public final class MixerTransformer implements IClassTransformer, Opcodes {
    private static final String TARGET = "gregapi.tileentity.machines.MultiTileEntityBasicMachine";
    private static final String OWNER = "gregapi/tileentity/machines/MultiTileEntityBasicMachine";
    private static final String BRIDGE = "zenovka/gt6materialexpansion/core/MixerInheritanceBridge";
    @Override public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)) return bytes;
        try {
            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, 0);
            for (MethodNode method : node.methods) {
                if (!"checkRecipe".equals(method.name) || !"(ZZ)I".equals(method.desc)) continue;
                AbstractInsnNode outputWrite = null;
                int consumptionCalls = 0;
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof MethodInsnNode) {
                        MethodInsnNode call = (MethodInsnNode) insn;
                        if ("gregapi/recipes/Recipe".equals(call.owner) && "isRecipeInputEqual".equals(call.name)
                                && call.desc.startsWith("(ZZ")) consumptionCalls++;
                    }
                    if (insn instanceof FieldInsnNode) {
                        FieldInsnNode field = (FieldInsnNode) insn;
                        if (insn.getOpcode() == PUTFIELD && OWNER.equals(field.owner)
                                && "mOutputItems".equals(field.name)) outputWrite = insn;
                    }
                }
                // Exact 6.17.06 pattern: two consumption branches, one generated output assignment.
                if (consumptionCalls != 2 || outputWrite == null) {
                    System.err.println("[GT6MaterialExpansion] Mixer ASM signature mismatch; inheritance disabled.");
                    return bytes;
                }
                int saved = method.maxLocals++;
                // Snapshot before GT6 mutates the input stack. Do this at each successful-consumption candidate.
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (!(insn instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode) insn;
                    if (!"gregapi/recipes/Recipe".equals(call.owner) || !"isRecipeInputEqual".equals(call.name)
                            || !call.desc.startsWith("(ZZ")) continue;
                    InsnList capture = new InsnList();
                    capture.add(new VarInsnNode(ALOAD, 0));
                    capture.add(new InsnNode(ICONST_0));
                    capture.add(new MethodInsnNode(INVOKEVIRTUAL, OWNER, "slot", "(I)Lnet/minecraft/item/ItemStack;", false));
                    capture.add(new MethodInsnNode(INVOKESTATIC, BRIDGE, "snapshot", "(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;", false));
                    capture.add(new VarInsnNode(ASTORE, saved));
                    method.instructions.insertBefore(insn, capture);
                }
                InsnList apply = new InsnList();
                apply.add(new VarInsnNode(ALOAD, 0));
                apply.add(new VarInsnNode(ALOAD, saved));
                apply.add(new MethodInsnNode(INVOKESTATIC, BRIDGE, "apply", "(Lgregapi/tileentity/machines/MultiTileEntityBasicMachine;Lnet/minecraft/item/ItemStack;)V", false));
                method.instructions.insert(outputWrite, apply);
                // Initialised at method entry so all paths are verifier-safe.
                InsnList init = new InsnList();
                init.add(new InsnNode(ACONST_NULL));
                init.add(new VarInsnNode(ASTORE, saved));
                method.instructions.insert(init);
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                node.accept(writer);
                System.out.println("[GT6MaterialExpansion] Installed GT6 6.17.06 Mixer inheritance hook.");
                return writer.toByteArray();
            }
        } catch (Throwable error) {
            System.err.println("[GT6MaterialExpansion] Mixer ASM failed; leaving GT6 unchanged.");
            error.printStackTrace();
        }
        return bytes;
    }
}
