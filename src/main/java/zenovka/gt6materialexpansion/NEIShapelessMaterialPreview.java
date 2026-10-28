package zenovka.gt6materialexpansion;

import codechicken.nei.PositionedStack;
import codechicken.nei.NEIServerUtils;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.recipe.ShapelessRecipeHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import java.util.List;

/** Decorates NEI's existing shapeless recipe display, never recipe registry outputs. */
@SideOnly(Side.CLIENT)
public final class NEIShapelessMaterialPreview extends ShapelessRecipeHandler {
    public static void install() {
        replace(GuiCraftingRecipe.craftinghandlers);
        replace(GuiCraftingRecipe.serialCraftingHandlers);
    }

    private static void replace(List<ICraftingHandler> handlers) {
        synchronized (handlers) {
            for (int i = 0; i < handlers.size(); i++) {
                ICraftingHandler handler = handlers.get(i);
                if (handler != null && handler.getClass() == ShapelessRecipeHandler.class) {
                    handlers.set(i, new NEIShapelessMaterialPreview());
                    FMLLog.info("[GT6MaterialExpansion] Decorated existing NEI shapeless crafting handler");
                }
            }
        }
    }

    private CachedShapelessRecipe make(IRecipe recipe) {
        try {
            CachedShapelessRecipe cached;
            if (recipe instanceof ShapelessRecipes) {
                ShapelessRecipes vanilla = (ShapelessRecipes) recipe;
                if (vanilla.recipeItems == null) return null;
                cached = new CachedShapelessRecipe(vanilla.recipeItems, recipe.getRecipeOutput());
            } else if (recipe instanceof ShapelessOreRecipe) {
                List<?> ingredients = ((ShapelessOreRecipe) recipe).getInput();
                for (Object ingredient : ingredients)
                    if (ingredient instanceof List && ((List<?>) ingredient).isEmpty()) return null;
                cached = new CachedShapelessRecipe(ingredients, recipe.getRecipeOutput());
            } else return null;

            ItemStack original = recipe.getRecipeOutput();
            if (original != null) cached.setResult(RecipePreviewTags.copyWithMaterials(
                    original, RecipeMaterialPreview.forCrafting(recipe)));
            for (PositionedStack displayed : cached.ingredients) {
                if (displayed == null || displayed.items == null) continue;
                ItemStack[] copies = new ItemStack[displayed.items.length];
                for (int i = 0; i < copies.length; i++) {
                    ItemStack input = displayed.items[i];
                    copies[i] = input == null || input.stackSize <= 0 ? input
                            : RecipePreviewTags.copyWithMaterials(input, MaterialDefaultResolver.resolve(input));
                }
                displayed.items = copies;
                if (copies.length > 0) displayed.item = copies[0];
            }
            return cached;
        } catch (RuntimeException e) {
            FMLLog.warning("[GT6MaterialExpansion] Could not preview shapeless recipe: %s", e);
            return null;
        }
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if (!"crafting".equals(outputId) && !"crafting2x2".equals(outputId)) {
            super.loadCraftingRecipes(outputId, results);
            return;
        }
        for (Object entry : CraftingManager.getInstance().getRecipeList()) {
            if (!(entry instanceof IRecipe)) continue;
            IRecipe recipe = (IRecipe) entry;
            if ("crafting2x2".equals(outputId) && recipe.getRecipeSize() > 4) continue;
            CachedShapelessRecipe cached = make(recipe);
            if (cached != null) arecipes.add(cached);
        }
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        for (Object entry : CraftingManager.getInstance().getRecipeList()) {
            if (!(entry instanceof IRecipe)) continue;
            IRecipe recipe = (IRecipe) entry;
            if (!NEIServerUtils.areStacksSameTypeCrafting(recipe.getRecipeOutput(), result)) continue;
            CachedShapelessRecipe cached = make(recipe);
            if (cached != null) arecipes.add(cached);
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        for (Object entry : CraftingManager.getInstance().getRecipeList()) {
            if (!(entry instanceof IRecipe)) continue;
            CachedShapelessRecipe cached = make((IRecipe) entry);
            if (cached == null || !cached.contains(cached.ingredients, ingredient)) continue;
            cached.setIngredientPermutation(cached.ingredients, ingredient);
            arecipes.add(cached);
        }
    }
}
