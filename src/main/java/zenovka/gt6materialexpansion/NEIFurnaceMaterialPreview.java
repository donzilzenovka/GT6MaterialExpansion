package zenovka.gt6materialexpansion;

import codechicken.nei.PositionedStack;
import codechicken.nei.NEIServerUtils;
import codechicken.nei.recipe.FurnaceRecipeHandler;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.recipe.IUsageHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregapi.oredict.OreDictMaterialStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import java.util.List;
import java.util.Map;

/**
 * Decorates NEI's existing vanilla furnace tab, without changing smelting
 * registrations or item stacks. Resolves the input for each displayed recipe.
 */
@SideOnly(Side.CLIENT)
public final class NEIFurnaceMaterialPreview extends FurnaceRecipeHandler {
    @Override
    public String getHandlerId() {
        return FurnaceRecipeHandler.class.getName();
    }

    private static boolean isBakedPotato(ItemStack stack) {
        return stack != null && stack.getItem() == net.minecraft.init.Items.baked_potato;
    }

    public static void install() {
        int count = 0;
        for (ICraftingHandler handler : GuiCraftingRecipe.craftinghandlers)
            if (handler instanceof FurnaceRecipeHandler) count++;
        FMLLog.info("[GT6MaterialExpansion][nei-furnace] before install furnace crafting handlers=%d", count);
        replaceCrafting(GuiCraftingRecipe.craftinghandlers);
        replaceCrafting(GuiCraftingRecipe.serialCraftingHandlers);
        replaceUsage(GuiUsageRecipe.usagehandlers);
        replaceUsage(GuiUsageRecipe.serialUsageHandlers);
        int count = 0;
        for (ICraftingHandler handler : GuiCraftingRecipe.craftinghandlers)
            if (handler instanceof NEIFurnaceMaterialPreview) count++;
        FMLLog.info("[GT6MaterialExpansion][nei-furnace] after install decorated crafting handlers=%d", count);
    }

    private static void replaceCrafting(List<ICraftingHandler> handlers) {
        synchronized (handlers) {
            for (int i = 0; i < handlers.size(); i++) {
                ICraftingHandler handler = handlers.get(i);
                if (handler != null && handler.getClass() == FurnaceRecipeHandler.class) {
                    handlers.set(i, new NEIFurnaceMaterialPreview());
                    FMLLog.info("[GT6MaterialExpansion] Decorated existing NEI furnace crafting handler");
                }
            }
        }
    }

    private static void replaceUsage(List<IUsageHandler> handlers) {
        synchronized (handlers) {
            for (int i = 0; i < handlers.size(); i++) {
                IUsageHandler handler = handlers.get(i);
                if (handler != null && handler.getClass() == FurnaceRecipeHandler.class)
                    handlers.set(i, new NEIFurnaceMaterialPreview());
            }
        }
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        super.loadCraftingRecipes(outputId, results);
        decorate();
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        if (isBakedPotato(result))
            FMLLog.info("[GT6MaterialExpansion][nei-furnace] baked potato lookup invoked metadata=%d nbt=%s handler=%s",
                    result.getItemDamage(), result.getTagCompound(), getHandlerId());
        super.loadCraftingRecipes(result);
        if (isBakedPotato(result))
            FMLLog.info("[GT6MaterialExpansion][nei-furnace] baked potato matches before decoration=%d", arecipes.size());
        decorate();
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        super.loadUsageRecipes(ingredient);
        decorate();
    }

    private void decorate() {
        for (CachedRecipe cached : arecipes) {
            PositionedStack output = cached.getResult();
            if (output == null || output.item == null) continue;
            List<PositionedStack> ingredients = cached.getIngredients();
            if (ingredients == null || ingredients.size() != 1
                    || ingredients.get(0) == null || ingredients.get(0).item == null) continue;
            ItemStack shownInput = ingredients.get(0).item;
            ItemStack shownOutput = output.item;
            ItemStack matchedInput = null;
            int matches = 0;
            for (Object raw : FurnaceRecipes.smelting().getSmeltingList().entrySet()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) raw;
                if (!(entry.getKey() instanceof ItemStack) || !(entry.getValue() instanceof ItemStack)) continue;
                ItemStack input = (ItemStack) entry.getKey();
                ItemStack result = (ItemStack) entry.getValue();
                if (NEIServerUtils.areStacksSameTypeCrafting(input, shownInput)
                        && NEIServerUtils.areStacksSameTypeCrafting(result, shownOutput)) {
                    matches++;
                    matchedInput = input;
                    if (matches > 1) break;
                }
            }
            if (matches != 1) continue; // No guessing for ambiguous furnace mappings.
            List<OreDictMaterialStack> materials = MaterialDefaultResolver.resolve(matchedInput);
            ItemStack preview = RecipePreviewTags.copyWithMaterials(shownOutput, materials);
            if (preview == null) continue;
            output.items = new ItemStack[] { preview };
            output.item = preview;
        }
    }
}
