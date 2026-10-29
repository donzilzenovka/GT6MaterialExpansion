package zenovka.gt6materialexpansion.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public final class MixerTransformer implements IClassTransformer, Opcodes {
    private static final String CRAFTING = "net.minecraft.item.crafting.CraftingManager";
    private static final String FURNACE = "net.minecraft.tileentity.TileEntityFurnace";
    private static final String FURNACE_BRIDGE = "zenovka/gt6materialexpansion/core/FurnaceInheritanceBridge";
    private static final String TARGET = "gregapi.tileentity.machines.MultiTileEntityBasicMachine";
    private static final String BOWL = "gregtech.tileentity.tools.MultiTileEntityMixingBowl";
    private static final String OWNER = "gregapi/tileentity/machines/MultiTileEntityBasicMachine";
    private static final String BRIDGE = "zenovka/gt6materialexpansion/core/MixerInheritanceBridge";

    private byte[] transformTransfer(byte[] bytes, boolean slotClass) {
        try {
            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, 0);
            int patched = 0;
            for (MethodNode method : node.methods) {
                if ((method.access & (ACC_STATIC | ACC_ABSTRACT | ACC_NATIVE)) != 0) continue;
                Type[] args = Type.getArgumentTypes(method.desc);
                Type result = Type.getReturnType(method.desc);
                boolean playerFirst = args.length > 0 && args[0].getSort() == Type.OBJECT
                        && (args[0].getInternalName().equals("yz")
                        || args[0].getInternalName().equals("net/minecraft/entity/player/EntityPlayer"));
                boolean transfer = !slotClass && playerFirst && args.length == 2
                        && args[1].getSort() == Type.INT && result.getSort() == Type.OBJECT;
                boolean pickup = slotClass && playerFirst && args.length == 2
                        && args[1].getSort() == Type.OBJECT && result.getSort() == Type.VOID;
                boolean remove = slotClass && args.length == 1 && args[0].getSort() == Type.INT
                        && result.getSort() == Type.OBJECT;
                if (!transfer && !pickup && !remove) continue;
                String label = (slotClass ? "SlotCrafting." : "ContainerWorkbench.") + method.name + method.desc;
                InsnList start = new InsnList();
                start.add(new VarInsnNode(ALOAD, 0));
                start.add(new LdcInsnNode("ENTER " + label));
                if (playerFirst) start.add(new VarInsnNode(ALOAD, 1));
                else start.add(new InsnNode(ACONST_NULL));
                start.add(new MethodInsnNode(INVOKESTATIC,
                        "zenovka/gt6materialexpansion/core/CraftTransferDiagnostic", "trace",
                        "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V", false));
                method.instructions.insert(start);
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn.getOpcode() != ARETURN && insn.getOpcode() != RETURN) continue;
                    InsnList end = new InsnList();
                    end.add(new VarInsnNode(ALOAD, 0));
                    end.add(new LdcInsnNode("EXIT " + label));
                    if (playerFirst) end.add(new VarInsnNode(ALOAD, 1));
                    else end.add(new InsnNode(ACONST_NULL));
                    end.add(new MethodInsnNode(INVOKESTATIC,
                            "zenovka/gt6materialexpansion/core/CraftTransferDiagnostic", "trace",
                            "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V", false));
                    method.instructions.insertBefore(insn, end);
                }
                patched++;
            }
            System.out.println("[GT6MaterialExpansion/TransferDiagnostic] "
                    + (slotClass ? "SlotCrafting" : "ContainerWorkbench") + " traced methods=" + patched);
            if (patched == 0) {
                for (MethodNode method : node.methods)
                    System.out.println("[GT6MaterialExpansion/TransferDiagnostic] CANDIDATE " + method.name + method.desc);
                return bytes;
            }
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            return writer.toByteArray();
        } catch (Throwable error) {
            System.err.println("[GT6MaterialExpansion/TransferDiagnostic] Transfer instrumentation failed.");
            error.printStackTrace();
            return bytes;
        }
    }

    private byte[] transformCrafting(byte[] bytes) {
        try {
            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, 0);
            int matches = 0;
            for (MethodNode method : node.methods) {
                boolean readable = ("findMatchingRecipe".equals(method.name)
                        || "func_82787_a".equals(method.name))
                        && "(Lnet/minecraft/inventory/InventoryCrafting;Lnet/minecraft/world/World;)Lnet/minecraft/item/ItemStack;".equals(method.desc);
                boolean notch = "a".equals(method.name) && "(Laae;Lahb;)Ladd;".equals(method.desc);
                if (!readable && !notch) continue;
                // Result is on stack at each ARETURN; matrix is local 1.
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn.getOpcode() != ARETURN) continue;
                    InsnList hook = new InsnList();
                    hook.add(new VarInsnNode(ALOAD, 1));
                    hook.add(new MethodInsnNode(INVOKESTATIC,
                            "zenovka/gt6materialexpansion/FoodMaterials", "preview",
                            notch ? "(Ladd;Laae;)Ladd;"
                                    : "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/inventory/IInventory;)Lnet/minecraft/item/ItemStack;", false));
                    method.instructions.insertBefore(insn, hook);
                    matches++;
                }
            }
            if (matches == 0) {
                System.err.println("[GT6MaterialExpansion/Crafting] No readable recipe lookup found; incoming method signatures:");
                for (MethodNode method : node.methods)
                    System.err.println("[GT6MaterialExpansion/Crafting] " + method.name + method.desc);
                return bytes;
            }
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            System.out.println("[GT6MaterialExpansion] Installed crafting preview inheritance hook (" + matches + " returns).");
            return writer.toByteArray();
        } catch (Throwable error) {
            System.err.println("[GT6MaterialExpansion] Crafting preview ASM failed.");
            error.printStackTrace();
            return bytes;
        }
    }

    private byte[] transformFurnace(byte[] bytes) {
        try {
            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, 0);
            int patched = 0;
            for (MethodNode method : node.methods) {
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (!(insn instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode) insn;
                    // LaunchWrapper may deliver either MCP/SRG or notch-obfuscated bytecode.
                    // Match the observed 1.7.10 notch call as well as the readable form.
                    boolean notch = "afa".equals(call.owner) && "a".equals(call.name)
                            && "(Ladd;)Ladd;".equals(call.desc);
                    boolean readable = "net/minecraft/item/crafting/FurnaceRecipes".equals(call.owner)
                            && "(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;".equals(call.desc);
                    if (insn.getOpcode() != INVOKEVIRTUAL || (!notch && !readable)) continue;
                    call.setOpcode(INVOKESTATIC);
                    call.owner = FURNACE_BRIDGE;
                    call.name = "result";
                    call.desc = notch ? "(Lafa;Ladd;)Ladd;"
                            : "(Lnet/minecraft/item/crafting/FurnaceRecipes;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;";
                    call.itf = false;
                    patched++;
                }
            }
            // canSmelt() is k()Z in the observed notch runtime.
            // Fail closed before vanilla's NBT-blind output capacity check.
            int guards = 0;
            for (MethodNode method : node.methods) {
                boolean notchCanSmelt = "k".equals(method.name) && "()Z".equals(method.desc);
                boolean readableCanSmelt = ("canSmelt".equals(method.name) || "func_145948_k".equals(method.name))
                        && "()Z".equals(method.desc);
                if (!notchCanSmelt && !readableCanSmelt) continue;
                LabelNode allowed = new LabelNode();
                InsnList guard = new InsnList();
                guard.add(new VarInsnNode(ALOAD, 0));
                guard.add(new MethodInsnNode(INVOKESTATIC, FURNACE_BRIDGE, "canAccept",
                        "(Ljava/lang/Object;)Z", false));
                guard.add(new JumpInsnNode(IFNE, allowed));
                guard.add(new InsnNode(ICONST_0));
                guard.add(new InsnNode(IRETURN));
                guard.add(allowed);
                method.instructions.insert(guard);
                guards++;
            }
            System.out.println("[GT6MaterialExpansion] Furnace provenance output guards: " + guards);
            if (patched < 1) {
                // The incoming class is still in notch-obfuscated form.
                // Print every invocation in its two smelting routines, including
                // obfuscated owners/descriptors that contain no readable names.
                for (MethodNode method : node.methods) {
                    if (!("k".equals(method.name) && "()Z".equals(method.desc))
                            && !("j".equals(method.name) && "()V".equals(method.desc))) continue;
                    System.err.println("[GT6MaterialExpansion/Furnace] METHOD " + method.name + method.desc);
                    for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                        if (!(insn instanceof MethodInsnNode)) continue;
                        MethodInsnNode call = (MethodInsnNode) insn;
                        System.err.println("[GT6MaterialExpansion/Furnace] CALL " + call.owner + "." + call.name
                                + call.desc + " opcode=" + call.getOpcode());
                    }
                }
            }
            if (patched < 1) {
                System.err.println("[GT6MaterialExpansion] Vanilla furnace recipe lookup signature not found.");
                return bytes;
            }
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            System.out.println("[GT6MaterialExpansion] Installed vanilla furnace inheritance hook (" + patched + " lookups).");
            return writer.toByteArray();
        } catch (Throwable error) {
            System.err.println("[GT6MaterialExpansion] Vanilla furnace ASM failed; leaving furnace unchanged.");
            error.printStackTrace();
            return bytes;
        }
    }

    private byte[] transformBowl(byte[] bytes) {
        try {
            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, 0);
            int patched = 0;
            for (MethodNode method : node.methods) {
                // The manual bowl consumes exactly one recipe per successful interaction.
                // Replace the committed recipe call with the same tracker used by powered mixers.
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (!(insn instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode) insn;
                    if (!"gregapi/recipes/Recipe".equals(call.owner) || !"isRecipeInputEqual".equals(call.name)) continue;
                    if (!"(ZZ[Lnet/minecraftforge/fluids/IFluidTank;[Lnet/minecraft/item/ItemStack;)Z".equals(call.desc)) continue;
                    call.setOpcode(INVOKESTATIC);
                    call.owner = BRIDGE;
                    call.name = "consume";
                    call.desc = "(Lgregapi/recipes/Recipe;ZZ[Lnet/minecraftforge/fluids/IFluidTank;[Lnet/minecraft/item/ItemStack;)Z";
                    call.itf = false;
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
    private byte[] transformEnderIOAlloy(byte[] bytes) {
        try {
            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, 0);
            int patched = 0;
            for (MethodNode method : node.methods) {
                if (!"taskComplete".equals(method.name) || !"()V".equals(method.desc)) continue;
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (!(insn instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode) insn;
                    if (!"mergeResults".equals(call.name)
                            || !call.desc.startsWith("([Lcrazypants/enderio/machine/IMachineRecipe$ResultStack;)")) continue;
                    call.setOpcode(INVOKESTATIC);
                    call.owner = "zenovka/gt6materialexpansion/core/AlloySmelterBridge";
                    call.name = "merge";
                    call.desc = "(Ljava/lang/Object;Ljava/lang/Object;)V";
                    call.itf = false;
                    patched++;
                }
            }
            if (patched != 1) {
                System.err.println("[GT6MaterialExpansion/Alloy] Completion signature mismatch: " + patched);
                return bytes;
            }
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            System.out.println("[GT6MaterialExpansion/Alloy] Installed task completion hook.");
            return writer.toByteArray();
        } catch (Throwable error) {
            System.err.println("[GT6MaterialExpansion/Alloy] Transformer failed.");
            error.printStackTrace();
            return bytes;
        }
    }

    @Override public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return bytes;
        if ("crazypants.enderio.machine.AbstractPoweredTaskEntity".equals(transformedName)) return transformEnderIOAlloy(bytes);
        if ("net.minecraft.inventory.SlotCrafting".equals(transformedName)) return transformTransfer(bytes, true);
        if ("net.minecraft.inventory.ContainerWorkbench".equals(transformedName)) return transformTransfer(bytes, false);
        if (CRAFTING.equals(transformedName)) return transformCrafting(bytes);
        if (FURNACE.equals(transformedName)) return transformFurnace(bytes);
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
                // Wrap GT6's own committed consumption calls; compare item counts
                // before/after instead of guessing which input slots belong to the recipe.
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (!(insn instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode) insn;
                    if (!"gregapi/recipes/Recipe".equals(call.owner)
                            || !"isRecipeInputEqual".equals(call.name)) continue;
                    if ("(ZZ[Lnet/minecraftforge/fluids/IFluidTank;[Lnet/minecraft/item/ItemStack;)Z".equals(call.desc)) {
                        call.setOpcode(INVOKESTATIC);
                        call.owner = BRIDGE;
                        call.name = "consume";
                        call.desc = "(Lgregapi/recipes/Recipe;ZZ[Lnet/minecraftforge/fluids/IFluidTank;[Lnet/minecraft/item/ItemStack;)Z";
                        call.itf = false;
                    } else if ("(I[Lnet/minecraftforge/fluids/IFluidTank;[Lnet/minecraft/item/ItemStack;)I".equals(call.desc)) {
                        call.setOpcode(INVOKESTATIC);
                        call.owner = BRIDGE;
                        call.name = "consumeParallel";
                        call.desc = "(Lgregapi/recipes/Recipe;I[Lnet/minecraftforge/fluids/IFluidTank;[Lnet/minecraft/item/ItemStack;)I";
                        call.itf = false;
                    }
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
