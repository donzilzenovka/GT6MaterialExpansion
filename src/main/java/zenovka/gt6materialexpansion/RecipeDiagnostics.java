package zenovka.gt6materialexpansion;

import cpw.mods.fml.common.FMLLog;
import gregapi.recipes.Recipe;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.fluids.FluidStack;

import java.util.Collection;
import java.util.Map;

/** Temporary server-startup diagnostic for bread/dough recipe chains. */
public final class RecipeDiagnostics {
    private static final int LIMIT = 80;
    private RecipeDiagnostics() {}

    public static void dump() {
        FMLLog.info("[GT6MaterialExpansion][recipes] BEGIN bread/dough diagnostic");
        int hits = 0;
        int index = 0;
        for (Object entry : CraftingManager.getInstance().getRecipeList()) {
            if (entry instanceof IRecipe) {
                IRecipe recipe = (IRecipe) entry;
                if (target(recipe.getRecipeOutput()) && hits++ < LIMIT) {
                    FMLLog.info("[GT6MaterialExpansion][recipes] crafting[%d] %s => %s; inputs=%s",
                            index, entry.getClass().getName(), item(recipe.getRecipeOutput()), craftingInputs(recipe));
                }
            }
            index++;
        }
        hits = 0;
        index = 0;
        for (Object raw : FurnaceRecipes.smelting().getSmeltingList().entrySet()) {
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) raw;
            if (e.getValue() instanceof ItemStack && target((ItemStack) e.getValue()) && hits++ < LIMIT)
                FMLLog.info("[GT6MaterialExpansion][recipes] furnace[%d] %s => %s",
                        index, e.getKey(), item((ItemStack) e.getValue()));
            index++;
        }
        for (Recipe.RecipeMap map : Recipe.RecipeMap.RECIPE_MAP_LIST) {
            hits = 0;
            index = 0;
            for (Recipe recipe : map.mRecipeList) {
                if (hasTarget(recipe.mOutputs) && hits++ < LIMIT) {
                    FMLLog.info("[GT6MaterialExpansion][recipes] gt6 map=%s index=%d inputs=%s fluidsIn=%s outputs=%s fluidsOut=%s",
                            map.mNameInternal, index, items(recipe.mInputs), fluids(recipe.mFluidInputs),
                            items(recipe.mOutputs), fluids(recipe.mFluidOutputs));
                }
                index++;
            }
        }
        FMLLog.info("[GT6MaterialExpansion][recipes] END bread/dough diagnostic");
    }

    private static boolean target(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        String registry = String.valueOf(Item.itemRegistry.getNameForObject(stack.getItem())).toLowerCase(java.util.Locale.ROOT);
        String name;
        try { name = stack.getDisplayName().toLowerCase(java.util.Locale.ROOT); }
        catch (Exception e) { name = ""; }
        return registry.contains("bread") || registry.contains("dough")
                || name.contains("bread") || name.contains("dough");
    }

    private static boolean hasTarget(ItemStack[] stacks) {
        if (stacks == null) return false;
        for (ItemStack stack : stacks) if (target(stack)) return true;
        return false;
    }

    private static String item(ItemStack stack) {
        if (stack == null) return "null";
        String name;
        try { name = stack.getDisplayName(); } catch (Exception e) { name = "?"; }
        return String.valueOf(Item.itemRegistry.getNameForObject(stack.getItem()))
                + ":" + stack.getItemDamage() + " x" + stack.stackSize + " (" + name + ")";
    }

    private static String items(ItemStack[] stacks) {
        if (stacks == null) return "[]";
        StringBuilder result = new StringBuilder("[");
        for (ItemStack stack : stacks) result.append(item(stack)).append("; ");
        return result.append("]").toString();
    }

    private static String fluids(FluidStack[] stacks) {
        if (stacks == null) return "[]";
        StringBuilder result = new StringBuilder("[");
        for (FluidStack stack : stacks)
            result.append(stack == null ? "null" : stack.getFluid().getName() + " " + stack.amount + "mB").append("; ");
        return result.append("]").toString();
    }

    private static String craftingInputs(IRecipe recipe) {
        if (recipe instanceof ShapedRecipes) return items(((ShapedRecipes) recipe).recipeItems);
        if (recipe instanceof ShapelessRecipes) {
            StringBuilder result = new StringBuilder("[");
            for (Object ingredient : ((ShapelessRecipes) recipe).recipeItems) {
                result.append(ingredient instanceof ItemStack ? item((ItemStack) ingredient) : String.valueOf(ingredient)).append("; ");
            }
            return result.append("]").toString();
        }
        Object[] input = null;
        if (recipe instanceof ShapedOreRecipe) input = ((ShapedOreRecipe) recipe).getInput();
        if (recipe instanceof ShapelessOreRecipe) input = ((ShapelessOreRecipe) recipe).getInput().toArray();
        if (input == null) return "<unhandled recipe type>";
        StringBuilder result = new StringBuilder("[");
        for (Object ingredient : input) {
            if (ingredient instanceof ItemStack) result.append(item((ItemStack) ingredient));
            else if (ingredient instanceof Collection) {
                result.append("{");
                int n = 0;
                for (Object candidate : (Collection<?>) ingredient) {
                    if (n++ >= 4) { result.append("..."); break; }
                    if (candidate instanceof ItemStack) result.append(item((ItemStack) candidate)).append(", ");
                }
                result.append("}");
            } else result.append(String.valueOf(ingredient));
            result.append("; ");
        }
        return result.append("]").toString();
    }
}
