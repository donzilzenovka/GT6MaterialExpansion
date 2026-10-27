package zenovka.gt6materialexpansion.core;

import gregapi.data.RM;
import gregapi.recipes.Recipe;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.tileentity.machines.MultiTileEntityBasicMachine;
import gregapi.util.OM;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Invoked at recipe execution only. Never polls machines or inventories. */
public final class MixerInheritanceBridge {
    private static final String ORIGINAL = "zenovka.original.mats";
    private static final String RECYCLING = "gt.recycling.mats";
    private static final ThreadLocal<ItemStack[]> INPUT = new ThreadLocal<ItemStack[]>();
    private static int diagnostics = 0;
    private MixerInheritanceBridge() {}
    private static void diagnostic(String message) {
        if (diagnostics++ < 30) System.out.println("[GT6MaterialExpansion/Mixer] " + message);
    }

    public static void clear() { INPUT.remove(); }

    public static void snapshotInputs(ItemStack[] inputs) {
        if (inputs == null) { INPUT.remove(); return; }
        ItemStack[] copy = new ItemStack[inputs.length];
        for (int i = 0; i < inputs.length; i++)
            copy[i] = inputs[i] == null ? null : inputs[i].copy();
        INPUT.set(copy);
    }

    public static ItemStack[] bowlOutputs(Recipe recipe) {
        ItemStack[] outputs = recipe.getOutputs();
        ItemStack[] inputs = INPUT.get();
        INPUT.remove();
        if (inputs != null && outputs != null && outputs.length > 0)
            inherit(recipe, inputs, outputs[0]);
        return outputs;
    }

    public static void snapshot(ItemStack input) {
        snapshotInputs(input == null ? null : new ItemStack[] {input});
    }

    public static void apply(MultiTileEntityBasicMachine machine) {
        ItemStack[] inputs = INPUT.get();
        INPUT.remove();
        if (machine.mRecipes != RM.Mixer) return;
        if (inputs == null || machine.mCurrentRecipe == null
                || machine.mOutputItems == null || machine.mOutputItems.length == 0) {
            diagnostic("No input snapshot, recipe, or output");
            return;
        }
        inherit(machine.mCurrentRecipe, inputs, machine.mOutputItems[0]);
    }

    private static void inherit(Recipe recipe, ItemStack[] inputs, ItemStack output) {
        if (output == null || output.stackSize <= 0) return;
        NBTTagCompound existing = output.getTagCompound();
        if (existing != null && existing.hasKey(ORIGINAL)) return;
        if (recipe.mInputs == null || recipe.mInputs.length == 0) return;
        ItemStack recipeOutput = recipe.mOutputs == null || recipe.mOutputs.length == 0
                ? null : recipe.mOutputs[0];
        if (recipeOutput == null || recipeOutput.stackSize <= 0
                || output.stackSize % recipeOutput.stackSize != 0) return;
        long operations = output.stackSize / recipeOutput.stackSize;

        // Match recipe requirements to actual slots, not positional indices:
        // the player may place the ingredients in any order.
        boolean[] used = new boolean[inputs.length];
        Map<OreDictMaterial, Long> totals = new LinkedHashMap<OreDictMaterial, Long>();
        for (ItemStack required : recipe.mInputs) {
            if (required == null || required.stackSize <= 0) return;
            int match = -1;
            for (int i = 0; i < inputs.length; i++) {
                ItemStack candidate = inputs[i];
                if (used[i] || candidate == null || candidate.getItem() == null) continue;
                if (!OreDictionary.itemMatches(required, candidate, false)) continue;
                if ((long) required.stackSize * operations > candidate.stackSize) continue;
                match = i;
                break;
            }
            if (match < 0) {
                diagnostic("Could not uniquely match recipe ingredient " + required);
                return;
            }
            used[match] = true;
            ItemStack ingredient = inputs[match];
            OreDictItemData data = OM.anydata(ingredient);
            if (data == null || data.getAllMaterialStacks() == null
                    || data.getAllMaterialStacks().isEmpty()) return;
            long consumed = (long) required.stackSize * operations;
            for (OreDictMaterialStack part : data.getAllMaterialStacks()) {
                if (part == null || part.mMaterial == null || part.mAmount < 0) return;
                if (consumed != 0 && part.mAmount > Long.MAX_VALUE / consumed) return;
                long delta = part.mAmount * consumed;
                Long previous = totals.get(part.mMaterial);
                long old = previous == null ? 0 : previous;
                if (Long.MAX_VALUE - old < delta) return;
                totals.put(part.mMaterial, old + delta);
            }
        }
        // Do not silently omit any occupied input slot.
        for (int i = 0; i < inputs.length; i++)
            if (!used[i] && inputs[i] != null && inputs[i].stackSize > 0) {
                diagnostic("Unmatched occupied mixer slot " + i);
                return;
            }

        OreDictMaterial[] keys = totals.keySet().toArray(new OreDictMaterial[totals.size()]);
        Arrays.sort(keys, new Comparator<OreDictMaterial>() {
            @Override public int compare(OreDictMaterial a, OreDictMaterial b) {
                return Integer.compare(a.mID, b.mID);
            }
        });
        List<OreDictMaterialStack> parts = new ArrayList<OreDictMaterialStack>();
        for (OreDictMaterial material : keys) {
            long amount = totals.get(material);
            if (amount % output.stackSize != 0) return;
            if (amount > 0) parts.add(OM.stack(material, amount / output.stackSize));
        }
        if (parts.isEmpty()) return;
        NBTTagCompound tag = existing == null ? new NBTTagCompound() : existing;
        OreDictMaterialStack.saveList(ORIGINAL, tag, parts);
        OreDictMaterialStack.saveList(RECYCLING, tag, parts);
        output.setTagCompound(tag);
        diagnostic("Applied " + parts.size() + " constituent material(s) to " + output.getDisplayName());
    }
}
