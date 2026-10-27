package zenovka.gt6materialexpansion.core;

import gregapi.data.RM;
import gregapi.recipes.Recipe;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.tileentity.machines.MultiTileEntityBasicMachine;
import gregapi.util.OM;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import java.util.ArrayList;
import java.util.List;

/** Invoked at recipe execution only. Never polls machines or inventories. */
public final class MixerInheritanceBridge {
    private static final String ORIGINAL = "zenovka.original.mats";
    private static final String RECYCLING = "gt.recycling.mats";
    private static final ThreadLocal<ItemStack> INPUT = new ThreadLocal<ItemStack>();
    private static int diagnostics = 0;
    private MixerInheritanceBridge() {}
    private static void diagnostic(String message) {
        if (diagnostics++ < 20) System.out.println("[GT6MaterialExpansion/Mixer] " + message);
    }

    public static void clear() { INPUT.remove(); CONSUMED.remove(); }

    public static void snapshotInputs(ItemStack[] inputs) {
        ItemStack primary = null;
        if (inputs != null) for (ItemStack item : inputs) {
            if (item != null && item.stackSize > 0) { primary = item; break; }
        }
        snapshot(primary);
    }

    public static ItemStack[] bowlOutputs(Recipe recipe) {
        ItemStack[] outputs = recipe.getOutputs();
        ItemStack primary = INPUT.get();
        INPUT.remove();
        if (primary != null && outputs != null && outputs.length > 0)
            inherit(recipe, primary, outputs[0]);
        return outputs;
    }

    public static void snapshot(ItemStack input) {
        INPUT.set(input == null ? null : input.copy());
    }

    // Committed item consumption, recorded using GT6's own matching and deduction.
    private static final ThreadLocal<java.util.Map<gregapi.oredict.OreDictMaterial, Long>> CONSUMED =
            new ThreadLocal<java.util.Map<gregapi.oredict.OreDictMaterial, Long>>();

    public static boolean consume(Recipe recipe, boolean decrease, boolean dontCheck,
                                  net.minecraftforge.fluids.IFluidTank[] tanks, ItemStack[] inputs) {
        ItemStack[] before = copies(inputs);
        boolean success = recipe.isRecipeInputEqual(decrease, dontCheck, tanks, inputs);
        if (success && decrease) record(before, inputs);
        return success;
    }

    public static int consumeParallel(Recipe recipe, int max,
                                      net.minecraftforge.fluids.IFluidTank[] tanks, ItemStack[] inputs) {
        ItemStack[] before = copies(inputs);
        int completed = recipe.isRecipeInputEqual(max, tanks, inputs);
        if (completed > 0) record(before, inputs);
        return completed;
    }

    private static ItemStack[] copies(ItemStack[] inputs) {
        if (inputs == null) return null;
        ItemStack[] result = new ItemStack[inputs.length];
        for (int i = 0; i < inputs.length; i++)
            if (inputs[i] != null) result[i] = inputs[i].copy();
        return result;
    }

    private static void record(ItemStack[] before, ItemStack[] after) {
        if (before == null || after == null) return;
        java.util.Map<gregapi.oredict.OreDictMaterial, Long> totals = CONSUMED.get();
        if (totals == null) {
            totals = new java.util.HashMap<gregapi.oredict.OreDictMaterial, Long>();
            CONSUMED.set(totals);
        }
        for (int i = 0; i < before.length && i < after.length; i++) {
            ItemStack old = before[i];
            if (old == null) continue;
            int remaining = after[i] == null ? 0 : after[i].stackSize;
            long used = (long) old.stackSize - remaining;
            if (used <= 0) continue;
            OreDictItemData data = OM.anydata(old);
            if (data == null || data.getAllMaterialStacks() == null) {
                CONSUMED.remove();
                return;
            }
            for (OreDictMaterialStack part : data.getAllMaterialStacks()) {
                if (part == null || part.mMaterial == null || part.mAmount < 0
                        || (used != 0 && part.mAmount > Long.MAX_VALUE / used)) {
                    CONSUMED.remove();
                    return;
                }
                long delta = used * part.mAmount;
                Long previous = totals.get(part.mMaterial);
                long base = previous == null ? 0 : previous;
                if (delta > Long.MAX_VALUE - base) { CONSUMED.remove(); return; }
                totals.put(part.mMaterial, base + delta);
            }
        }
    }

    private static void inheritConsumed(ItemStack output) {
        java.util.Map<gregapi.oredict.OreDictMaterial, Long> totals = CONSUMED.get();
        if (output == null || output.stackSize <= 0 || totals == null || totals.isEmpty()) return;
        NBTTagCompound existing = output.getTagCompound();
        if (existing != null && existing.hasKey(ORIGINAL)) return;
        java.util.List<gregapi.oredict.OreDictMaterial> keys =
                new java.util.ArrayList<gregapi.oredict.OreDictMaterial>(totals.keySet());
        java.util.Collections.sort(keys, new java.util.Comparator<gregapi.oredict.OreDictMaterial>() {
            @Override public int compare(gregapi.oredict.OreDictMaterial a, gregapi.oredict.OreDictMaterial b) {
                return Integer.compare(a.mID, b.mID);
            }
        });
        List<OreDictMaterialStack> parts = new ArrayList<OreDictMaterialStack>();
        for (gregapi.oredict.OreDictMaterial material : keys) {
            long amount = totals.get(material);
            if (amount % output.stackSize != 0) return;
            if (amount > 0) parts.add(OM.stack(material, amount / output.stackSize));
        }
        if (parts.isEmpty()) return;
        NBTTagCompound tag = existing == null ? new NBTTagCompound() : existing;
        OreDictMaterialStack.saveList(ORIGINAL, tag, parts);
        OreDictMaterialStack.saveList(RECYCLING, tag, parts);
        output.setTagCompound(tag);
    }

    public static void apply(MultiTileEntityBasicMachine machine) {
        ItemStack primary = INPUT.get();
        INPUT.remove();
        if (machine.mRecipes != RM.Mixer) {
            if (primary != null) diagnostic("Recipe map differs from RM.Mixer: " + machine.mRecipes);
            return;
        }
        if ((primary == null && (CONSUMED.get() == null || CONSUMED.get().isEmpty())) || machine.mCurrentRecipe == null
                || machine.mOutputItems == null || machine.mOutputItems.length == 0) {
            diagnostic("No primary snapshot, recipe, or output; primary=" + primary);
            return;
        }
        ItemStack output = machine.mOutputItems[0];
        if (CONSUMED.get() != null && !CONSUMED.get().isEmpty()) inheritConsumed(output);
        else inherit(machine.mCurrentRecipe, primary, output);
        CONSUMED.remove();
    }

    private static void inherit(Recipe recipe, ItemStack primary, ItemStack output) {
        if (output == null || output.stackSize <= 0) { diagnostic("Empty primary output"); return; }
        NBTTagCompound existing = output.getTagCompound();
        if (existing != null && existing.hasKey(ORIGINAL)) return;
        if (recipe.mInputs == null || recipe.mInputs.length == 0
                || recipe.mInputs[0] == null) { diagnostic("No recipe primary item input"); return; }
        OreDictItemData data = OM.anydata(primary);
        if (data == null || data.getAllMaterialStacks().isEmpty()) { diagnostic("No material data on primary input " + primary); return; }

        // Primary ingredient count per operation; infer operations from primary output count.
        ItemStack recipeOutput = recipe.mOutputs == null
                || recipe.mOutputs.length == 0 ? null : recipe.mOutputs[0];
        if (recipeOutput == null || recipeOutput.stackSize <= 0) { diagnostic("Missing recipe primary output"); return; }
        if (output.stackSize % recipeOutput.stackSize != 0) { diagnostic("Output count mismatch"); return; }
        long operations = output.stackSize / recipeOutput.stackSize;
        long consumed = operations * recipe.mInputs[0].stackSize;
        if (consumed <= 0 || consumed > primary.stackSize) { diagnostic("Input count mismatch: consumed=" + consumed + " available=" + primary.stackSize); return; }

        List<OreDictMaterialStack> parts = new ArrayList<OreDictMaterialStack>();
        for (OreDictMaterialStack part : data.getAllMaterialStacks()) {
            if (part == null || part.mMaterial == null || part.mAmount < 0) return;
            long amount = part.mAmount * consumed;
            if (consumed != 0 && amount / consumed != part.mAmount) return;
            if (amount % output.stackSize != 0) { diagnostic("Non-integral material split"); return; }
            if (amount > 0) parts.add(OM.stack(part.mMaterial, amount / output.stackSize));
        }
        if (parts.isEmpty()) { diagnostic("No inherited parts"); return; }
        NBTTagCompound tag = existing == null ? new NBTTagCompound() : existing;
        OreDictMaterialStack.saveList(ORIGINAL, tag, parts);
        OreDictMaterialStack.saveList(RECYCLING, tag, parts);
        output.setTagCompound(tag);
        diagnostic("Applied " + parts.size() + " material(s) to " + output.getDisplayName());
    }
}
