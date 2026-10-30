package zenovka.gt6materialexpansion;

import gregapi.data.MT;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.recipes.Recipe;
import gregapi.util.OM;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

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


    /**
     * Calculate the output of a specific crafting recipe, without changing its registered result.
     * Ore-dictionary alternatives use the first available stack for a canonical preview.
     */
    public static List<OreDictMaterialStack> forCrafting(IRecipe recipe) {
        if (recipe == null) return null;
        int beforeOutput = SoundQueueDiagnostic.pending();
        ItemStack output = recipe.getRecipeOutput();
        SoundQueueDiagnostic.report("crafting.getRecipeOutput", output, beforeOutput);
        if (output == null || output.stackSize <= 0) return null;

        int beforeInputs = SoundQueueDiagnostic.pending();
        Object[] ingredients;
        if (recipe instanceof ShapedRecipes) ingredients = ((ShapedRecipes) recipe).recipeItems;
        else if (recipe instanceof ShapelessRecipes) ingredients = ((ShapelessRecipes) recipe).recipeItems.toArray();
        else if (recipe instanceof ShapedOreRecipe) ingredients = ((ShapedOreRecipe) recipe).getInput();
        else if (recipe instanceof ShapelessOreRecipe) ingredients = ((ShapelessOreRecipe) recipe).getInput().toArray();
        else return null;
        SoundQueueDiagnostic.report("crafting.getIngredients", output, beforeInputs);

        Map<OreDictMaterial, Long> totals = new LinkedHashMap<OreDictMaterial, Long>();
        for (int index = 0; index < ingredients.length; index++) {
            Object ingredient = ingredients[index];
            if (ingredient == null) continue;
            int beforeChoose = SoundQueueDiagnostic.pending();
            ItemStack chosen = choose(ingredient);
            SoundQueueDiagnostic.report("crafting.choose[" + index + "]", chosen, beforeChoose);
            if (chosen == null) return null;
            if (chosen.stackSize == 0 || chosen.getItem() instanceof MultiItemTool) continue;

            int beforeResolve = SoundQueueDiagnostic.pending();
            List<OreDictMaterialStack> materials = MaterialDefaultResolver.resolve(chosen);
            SoundQueueDiagnostic.report("crafting.resolve[" + index + "]", chosen, beforeResolve);
            if (!accumulate(totals, materials, 1)) return null;

            int beforeContainer = SoundQueueDiagnostic.pending();
            boolean hasContainer = chosen.getItem().hasContainerItem(chosen);
            SoundQueueDiagnostic.report("crafting.hasContainer[" + index + "]", chosen, beforeContainer);
            if (hasContainer) {
                int beforeGetContainer = SoundQueueDiagnostic.pending();
                ItemStack container = chosen.getItem().getContainerItem(chosen);
                SoundQueueDiagnostic.report("crafting.getContainer[" + index + "]", chosen, beforeGetContainer);
                if (container != null && !accumulate(totals, MaterialDefaultResolver.resolve(container), -1)) return null;
            }
        }
        return divided(totals, output.stackSize);
    }

    private static ItemStack choose(Object ingredient) {
        if (ingredient instanceof ItemStack) {
            ItemStack stack = ((ItemStack) ingredient).copy();
            // Preserve zero-size GT6 tool markers; they are not consumed.
            if (stack.stackSize > 0) stack.stackSize = 1;
            return stack;
        }
        if (ingredient instanceof List) {
            for (Object alternative : (List<?>) ingredient) {
                ItemStack chosen = choose(alternative);
                if (chosen != null) return chosen;
            }
        }
        return null;
    }

    private static boolean accumulate(Map<OreDictMaterial, Long> totals,
            List<OreDictMaterialStack> composition, int multiplier) {
        if (composition == null) return false;
        for (OreDictMaterialStack part : composition) {
            if (part == null || part.mMaterial == null) return false;
            if (part.mMaterial == MT.Empty && part.mAmount == -1) continue;
            if (part.mAmount < 0) return false;
            if (multiplier > 0 && part.mAmount > Long.MAX_VALUE / multiplier) return false;
            long delta = part.mAmount * multiplier;
            long previous = totals.containsKey(part.mMaterial) ? totals.get(part.mMaterial) : 0;
            long next = previous + delta;
            if ((delta > 0 && next < previous) || (delta < 0 && next > previous)) return false;
            totals.put(part.mMaterial, next);
        }
        return true;
    }

    private static List<OreDictMaterialStack> divided(Map<OreDictMaterial, Long> totals, int count) {
        if (count <= 0 || totals.isEmpty()) return null;
        List<OreDictMaterialStack> result = new ArrayList<OreDictMaterialStack>();
        for (Map.Entry<OreDictMaterial, Long> part : totals.entrySet()) {
            if (part.getValue() < 0 || part.getValue() % count != 0) return null;
            if (part.getValue() > 0) result.add(OM.stack(part.getKey(), part.getValue() / count));
        }
        return result.isEmpty() ? null : result;
    }

    public static List<OreDictMaterialStack> forOutput(Recipe recipe, int outputIndex) {
        if (recipe == null || recipe.mOutputs == null || outputIndex < 0
                || outputIndex >= recipe.mOutputs.length) return null;
        ItemStack output = recipe.mOutputs[outputIndex];
        if (output == null || output.stackSize <= 0) return null;

        Map<OreDictMaterial, Long> totals = new LinkedHashMap<OreDictMaterial, Long>();
        if (recipe.mInputs == null || recipe.mInputs.length == 0) return null;
        for (ItemStack input : recipe.mInputs) {
            if (input == null || input.stackSize <= 0 || input.getItem() instanceof MultiItemTool) continue; // reusable tools
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
