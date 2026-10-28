package zenovka.gt6materialexpansion;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.recipes.Recipe;
import gregapi.util.OM;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only, per-recipe material preview for GT6 machine recipes.
 * The caller supplies the exact recipe (for example the one selected in NEI).
 * Fluids, energy and zero-count reusable tools are secondary resources.
 * Does not modify recipe outputs or attach NBT to actual machine products.
 */
public final class RecipeMaterialPreview {
    private RecipeMaterialPreview() {}

    public static List<OreDictMaterialStack> forOutput(Recipe recipe, int outputIndex) {
        if (recipe == null || recipe.mOutputs == null || outputIndex < 0
                || outputIndex >= recipe.mOutputs.length) return null;
        ItemStack output = recipe.mOutputs[outputIndex];
        if (output == null || output.stackSize <= 0) return null;

        Map<OreDictMaterial, Long> totals = new LinkedHashMap<OreDictMaterial, Long>();
        if (recipe.mInputs == null || recipe.mInputs.length == 0) return null;
        for (ItemStack input : recipe.mInputs) {
            if (input == null || input.stackSize <= 0) continue; // molds and reusable tools
            ItemStack single = input.copy();
            single.stackSize = 1;
            List<OreDictMaterialStack> composition = MaterialDefaultResolver.resolve(single);
            if (composition == null || composition.isEmpty()) return null;
            for (OreDictMaterialStack part : composition) {
                if (part == null || part.mMaterial == null || part.mAmount < 0) return null;
                if (part.mMaterial == MT.Empty) continue;
                if (part.mAmount > Long.MAX_VALUE / input.stackSize) return null;
                long amount = part.mAmount * input.stackSize;
                Long previous = totals.get(part.mMaterial);
                long old = previous == null ? 0L : previous.longValue();
                if (amount > Long.MAX_VALUE - old) return null;
                totals.put(part.mMaterial, old + amount);
            }
        }
        if (totals.isEmpty()) return null;

        // A multi-output machine recipe cannot safely attribute all inputs to one output.
        int nonemptyOutputs = 0;
        for (ItemStack candidate : recipe.mOutputs)
            if (candidate != null && candidate.stackSize > 0) nonemptyOutputs++;
        if (nonemptyOutputs != 1) return null;

        List<OreDictMaterialStack> result = new ArrayList<OreDictMaterialStack>();
        for (Map.Entry<OreDictMaterial, Long> part : totals.entrySet()) {
            long amount = part.getValue();
            if (amount % output.stackSize != 0) return null;
            if (amount > 0) result.add(OM.stack(part.getKey(), amount / output.stackSize));
        }
        return result.isEmpty() ? null : result;
    }
}
