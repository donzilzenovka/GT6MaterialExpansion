package zenovka.gt6materialexpansion;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.recipe.ShapedRecipeHandler;
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
public final class NEICraftingMaterialPreview extends ShapedRecipeHandler {
    public static void install() {
        replace(GuiCraftingRecipe.craftinghandlers);
        replace(GuiCraftingRecipe.serialCraftingHandlers);
    }

    private static void replace(List<ICraftingHandler> handlers) {
        synchronized (handlers) {
            for (int i = 0; i < handlers.size(); i++) {
                ICraftingHandler handler = handlers.get(i);
                if (handler != null && handler.getClass() == ShapedRecipeHandler.class) {
                    handlers.set(i, new NEICraftingMaterialPreview());
                    FMLLog.info("[GT6MaterialExpansion] Decorated existing NEI shaped crafting handler");
                }
            }
        }
    }

    private void append(IRecipe recipe) {
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
        arecipes.add(cached);
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if ("crafting".equals(outputId) || "crafting2x2".equals(outputId)) {
            for (Object candidate : CraftingManager.getInstance().getRecipeList()) {
                if (!(candidate instanceof IRecipe)) continue;
                IRecipe recipe = (IRecipe) candidate;
                if ("crafting2x2".equals(outputId)) {
                    if (!(recipe instanceof ShapedRecipes)) continue;
                    ShapedRecipes shaped = (ShapedRecipes) recipe;
                    if (shaped.recipeWidth > 2 || shaped.recipeHeight > 2) continue;
                }
                append(recipe);
            }
        } else super.loadCraftingRecipes(outputId, results);
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        super.loadCraftingRecipes(result);
        decorateMatching();
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        super.loadUsageRecipes(ingredient);
        decorateMatching();
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
            if (matches != 1) continue;
            ItemStack preview = RecipePreviewTags.copyWithMaterials(
                    output, RecipeMaterialPreview.forCrafting(selected));
            cached.result = new PositionedStack(preview, 119, 24);
        }
    }
}
