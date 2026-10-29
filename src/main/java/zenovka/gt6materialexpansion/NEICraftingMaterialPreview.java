package zenovka.gt6materialexpansion;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.recipe.IUsageHandler;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.recipe.ShapelessRecipeHandler;
import codechicken.nei.NEIServerUtils;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Experimental in-place replacement for NEI's shaped handler. Keeps its overlay
 * identifier and original GUI; only decorates copied output stacks.
 */
@SideOnly(Side.CLIENT)
public final class NEICraftingMaterialPreview extends ShapelessRecipeHandler {
    public static void install() {
        replace(GuiCraftingRecipe.craftinghandlers);
        replace(GuiCraftingRecipe.serialCraftingHandlers);
        replaceUsage(GuiUsageRecipe.usagehandlers);
        replaceUsage(GuiUsageRecipe.serialUsageHandlers);
    }

    private static void replace(List<ICraftingHandler> handlers) {
        synchronized (handlers) {
            for (int i = 0; i < handlers.size(); i++) {
                ICraftingHandler handler = handlers.get(i);
                if (handler != null && handler.getClass() == codechicken.nei.recipe.ShapedRecipeHandler.class) {
                    handlers.set(i, new NEICraftingMaterialPreview());
                    FMLLog.info("[GT6MaterialExpansion] Decorated existing NEI shaped crafting handler");
                }
            }
            for (int i = handlers.size() - 1; i >= 0; i--) {
                ICraftingHandler handler = handlers.get(i);
                if (handler != null && handler.getClass() == ShapelessRecipeHandler.class) {
                    handlers.remove(i);
                    FMLLog.info("[GT6MaterialExpansion] Merged NEI shapeless crafting into Crafting");
                }
            }
        }
    }

    private static void replaceUsage(List<IUsageHandler> handlers) {
        synchronized (handlers) {
            for (int i = 0; i < handlers.size(); i++) {
                IUsageHandler handler = handlers.get(i);
                if (handler != null && handler.getClass() == codechicken.nei.recipe.ShapedRecipeHandler.class)
                    handlers.set(i, new NEICraftingMaterialPreview());
            }
            for (int i = handlers.size() - 1; i >= 0; i--) {
                IUsageHandler handler = handlers.get(i);
                if (handler != null && handler.getClass() == ShapelessRecipeHandler.class)
                    handlers.remove(i);
            }
        }
    }

    @Override
    public String getHandlerId() {
        return codechicken.nei.recipe.ShapedRecipeHandler.class.getName();
    }

    @Override
    public String getRecipeTabName() {
        return getRecipeName();
    }

    @Override
    public String getRecipeName() {
        return NEIClientUtils.translate("recipe.shaped");
    }

    private void append(IRecipe recipe) {
        if (recipe instanceof ShapelessRecipes || recipe instanceof ShapelessOreRecipe) {
            appendShapeless(recipe);
            return;
        }
        CachedShapedRecipe cached;
        if (recipe instanceof ShapedRecipes) {
            cached = new CachedShapedRecipe((ShapedRecipes) recipe);
        } else if (recipe instanceof ShapedOreRecipe) {
            cached = forgeShapedRecipe((ShapedOreRecipe) recipe);
        } else return;
        if (cached == null) return;
        ItemStack original = recipe.getRecipeOutput();
        if (original != null) {
            ItemStack preview = RecipePreviewTags.copyWithMaterials(
                    original, RecipeMaterialPreview.forCrafting(recipe));
            cached.result = new PositionedStack(preview, 119, 24);
        }
        cached.computeVisuals();
        decorateInputs(cached);
        arecipes.add(cached);
    }

    private void appendShapeless(IRecipe recipe) {
        try {
            CachedShapelessRecipe cached;
            if (recipe instanceof ShapelessRecipes) {
                ShapelessRecipes vanilla = (ShapelessRecipes) recipe;
                if (vanilla.recipeItems == null) return;
                cached = new CachedShapelessRecipe(vanilla.recipeItems, recipe.getRecipeOutput());
            } else if (recipe instanceof ShapelessOreRecipe) {
                List<?> ingredients = ((ShapelessOreRecipe) recipe).getInput();
                for (Object ingredient : ingredients)
                    if (ingredient instanceof List && ((List<?>) ingredient).isEmpty()) return;
                cached = new CachedShapelessRecipe(ingredients, recipe.getRecipeOutput());
            } else return;
            if (recipe.getRecipeOutput() == null) return;
            cached.setResult(RecipePreviewTags.copyWithMaterials(
                    recipe.getRecipeOutput(), RecipeMaterialPreview.forCrafting(recipe)));
            for (PositionedStack displayed : cached.ingredients) {
                if (displayed == null || displayed.items == null) continue;
                ItemStack[] copies = new ItemStack[displayed.items.length];
                for (int i = 0; i < copies.length; i++) {
                    ItemStack input = displayed.items[i];
                    copies[i] = input == null || input.getItem() == null || input.stackSize <= 0
                            ? input : RecipePreviewTags.copyWithMaterials(
                                    input, MaterialDefaultResolver.resolve(input));
                }
                displayed.items = copies;
                if (copies.length > 0) displayed.item = copies[0];
            }
            arecipes.add(cached);
        } catch (RuntimeException e) {
            FMLLog.warning("[GT6MaterialExpansion] Could not preview shapeless recipe: %s", e);
        }
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if ("crafting".equals(outputId) || "crafting2x2".equals(outputId)) {
            for (Object candidate : CraftingManager.getInstance().getRecipeList()) {
                if (!(candidate instanceof IRecipe)) continue;
                IRecipe recipe = (IRecipe) candidate;
                if ("crafting2x2".equals(outputId)) {
                    if (recipe instanceof ShapedRecipes) {
                        ShapedRecipes shaped = (ShapedRecipes) recipe;
                        if (shaped.recipeWidth > 2 || shaped.recipeHeight > 2) continue;
                    } else if (recipe instanceof net.minecraftforge.oredict.ShapedOreRecipe
                            || recipe instanceof ShapelessRecipes || recipe instanceof ShapelessOreRecipe) {
                        if (recipe.getRecipeSize() > 4) continue;
                    } else continue;
                }
                append(recipe);
            }
        } else super.loadCraftingRecipes(outputId, results);
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        for (Object candidate : CraftingManager.getInstance().getRecipeList()) {
            if (!(candidate instanceof IRecipe)) continue;
            IRecipe recipe = (IRecipe) candidate;
            if (NEIServerUtils.areStacksSameTypeCrafting(recipe.getRecipeOutput(), result))
                append(recipe);
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        for (Object candidate : CraftingManager.getInstance().getRecipeList()) {
            if (!(candidate instanceof IRecipe)) continue;
            IRecipe recipe = (IRecipe) candidate;
            int before = arecipes.size();
            append(recipe);
            if (arecipes.size() == before) continue;
            CachedRecipe cached = arecipes.get(arecipes.size() - 1);
            if (!cached.contains(cached.getIngredients(), ingredient)) {
                arecipes.remove(arecipes.size() - 1);
            } else {
                cached.setIngredientPermutation(cached.getIngredients(), ingredient);
            }
        }
    }

    private void decorateMatching() {
        // The base NEI handler has already selected the exact recipes. Match by
        // output identity; refuse ambiguous matches rather than invent composition.
        for (Object entry : arecipes) {
            if (!(entry instanceof CachedShapedRecipe)) continue;
            CachedShapedRecipe cached = (CachedShapedRecipe) entry;
            if (cached.result == null || cached.result.item == null) continue;
            ItemStack output = cached.result.item;
            IRecipe selected = null;
            int matches = 0;
            for (Object candidate : CraftingManager.getInstance().getRecipeList()) {
                if (!(candidate instanceof ShapedRecipes) && !(candidate instanceof ShapedOreRecipe)) continue;
                IRecipe recipe = (IRecipe) candidate;
                ItemStack expected = recipe.getRecipeOutput();
                if (expected == null || expected.getItem() != output.getItem()
                        || expected.getItemDamage() != output.getItemDamage()) continue;
                selected = recipe;
                matches++;
                if (matches > 1) break;
            }
            decorateInputs(cached);
            if (matches != 1) continue;
            ItemStack preview = RecipePreviewTags.copyWithMaterials(
                    output, RecipeMaterialPreview.forCrafting(selected));
            cached.result = new PositionedStack(preview, 119, 24);
        }
    }
    private void decorateInputs(CachedShapedRecipe cached) {
        // NEI's ingredient stacks are display copies. Decorate each permutation
        // without changing registered recipe inputs or reusable tools.
        for (PositionedStack displayed : cached.ingredients) {
            if (displayed == null || displayed.items == null) continue;
            ItemStack[] copies = new ItemStack[displayed.items.length];
            for (int i = 0; i < copies.length; i++) {
                ItemStack input = displayed.items[i];
                if (input == null || input.stackSize <= 0) {
                    copies[i] = input;
                } else {
                    copies[i] = RecipePreviewTags.copyWithMaterials(
                            input, MaterialDefaultResolver.resolve(input));
                }
            }
            displayed.items = copies;
            if (copies.length > 0) displayed.item = copies[0];
        }
    }

}
