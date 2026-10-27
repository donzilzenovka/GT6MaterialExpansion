package zenovka.gt6materialexpansion.core;

import gregapi.data.RM;
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

    public static void clear() { INPUT.remove(); }

    public static void snapshot(ItemStack input) {
        INPUT.set(input == null ? null : input.copy());
    }

    public static void apply(MultiTileEntityBasicMachine machine) {
        ItemStack primary = INPUT.get();
        INPUT.remove();
        if (machine.mRecipes != RM.Mixer) {
            if (primary != null) diagnostic("Recipe map differs from RM.Mixer: " + machine.mRecipes);
            return;
        }
        if (primary == null || machine.mCurrentRecipe == null
                || machine.mOutputItems == null || machine.mOutputItems.length == 0) {
            diagnostic("No primary snapshot, recipe, or output; primary=" + primary);
            return;
        }
        ItemStack output = machine.mOutputItems[0];
        if (output == null || output.stackSize <= 0) { diagnostic("Empty primary output"); return; }
        NBTTagCompound existing = output.getTagCompound();
        if (existing != null && existing.hasKey(ORIGINAL)) return;
        if (machine.mCurrentRecipe.mInputs == null || machine.mCurrentRecipe.mInputs.length == 0
                || machine.mCurrentRecipe.mInputs[0] == null) { diagnostic("No recipe primary item input"); return; }
        OreDictItemData data = OM.anydata(primary);
        if (data == null || data.getAllMaterialStacks().isEmpty()) { diagnostic("No material data on primary input " + primary); return; }

        // Primary ingredient count per operation; infer operations from primary output count.
        ItemStack recipeOutput = machine.mCurrentRecipe.mOutputs == null
                || machine.mCurrentRecipe.mOutputs.length == 0 ? null : machine.mCurrentRecipe.mOutputs[0];
        if (recipeOutput == null || recipeOutput.stackSize <= 0) { diagnostic("Missing recipe primary output"); return; }
        if (output.stackSize % recipeOutput.stackSize != 0) { diagnostic("Output count mismatch"); return; }
        long operations = output.stackSize / recipeOutput.stackSize;
        long consumed = operations * machine.mCurrentRecipe.mInputs[0].stackSize;
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
