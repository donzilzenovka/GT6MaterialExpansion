package zenovka.gt6materialexpansion.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public final class MixerTransformer implements IClassTransformer, Opcodes {
    private static final String TARGET = "gregapi.tileentity.machines.MultiTileEntityBasicMachine";
    private static final String BOWL = "gregtech.tileentity.tools.MultiTileEntityMixingBowl";
    private static final String OWNER = "gregapi/tileentity/machines/MultiTileEntityBasicMachine";
    private static final String BRIDGE = "zenovka/gt6materialexpansion/core/MixerInheritanceBridge";

    private byte[] transformBowl(byte[] bytes) {
        try {
            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, 0);
            int patched = 0;
            for (MethodNode method : node.methods) {
                int inputLocal = -1;
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (!(insn instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode) insn;
                    if (!"gregapi/recipes/Recipe".equals(call.owner) || !"isRecipeInputEqual".equals(call.name)) continue;
                    // The actual ItemStack[] is the final argument on the operand stack.
                    // DUP preserves it for GT6 while passing a copy to our snapshot helper.
                    InsnList capture = new InsnList();
                    capture.add(new InsnNode(DUP));
                    capture.add(new MethodInsnNode(INVOKESTATIC, BRIDGE, "snapshotInputs", "([Lnet/minecraft/item/ItemStack;)V", false));
                    method.instructions.insertBefore(insn, capture);
                    patched++;
                }
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (!(insn instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode) insn;
                    if ("gregapi/recipes/Recipe".equals(call.owner) && "getOutputs".equals(call.name)
                            && "()[Lnet/minecraft/item/ItemStack;".equals(call.desc)) {
                        call.setOpcode(INVOKESTATIC);
                        call.owner = BRIDGE;
                        call.name = "bowlOutputs";
                        call.desc = "(Lgregapi/recipes/Recipe;)[Lnet/minecraft/item/ItemStack;";
                        call.itf = false;
                        patched++;
                    }
                }
            }
            if (patched != 4) {
                System.err.println("[GT6MaterialExpansion] Mixing bowl signature mismatch (" + patched + "); no patch applied.");
                return bytes;
            }
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            System.out.println("[GT6MaterialExpansion] Installed manual mixing bowl inheritance hook.");
            return writer.toByteArray();
        } catch (Throwable error) {
            System.err.println("[GT6MaterialExpansion] Manual bowl ASM failed; leaving GT6 unchanged.");
            error.printStackTrace();
            return bytes;
        }
    }
    @Override public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return bytes;
        if (BOWL.equals(transformedName)) return transformBowl(bytes);
        if (!TARGET.equals(transformedName)) return bytes;
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
                    capture.add(new MethodInsnNode(INVOKESTATIC, BRIDGE, "snapshot", "(Lnet/minecraft/item/ItemStack;)V", false));
                    method.instructions.insertBefore(insn, capture);
                }
                InsnList apply = new InsnList();
                apply.add(new VarInsnNode(ALOAD, 0));
                apply.add(new MethodInsnNode(INVOKESTATIC, BRIDGE, "apply", "(Lgregapi/tileentity/machines/MultiTileEntityBasicMachine;)V", false));
                method.instructions.insert(outputWrite, apply);
                // Clear stale snapshots from prior recipe checks on this thread.
                InsnList init = new InsnList();
                init.add(new MethodInsnNode(INVOKESTATIC, BRIDGE, "clear", "()V", false));
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
