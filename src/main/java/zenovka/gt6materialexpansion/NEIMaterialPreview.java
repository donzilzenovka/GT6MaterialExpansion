package zenovka.gt6materialexpansion;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.recipe.IUsageHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregapi.NEI_RecipeMap;
import gregapi.data.RM;
import gregapi.recipes.Recipe;
import gregapi.oredict.OreDictMaterialStack;
import net.minecraft.item.ItemStack;
import java.util.List;

/** Replaces GT6's NEI Mixer handler in-place; does not modify machine recipes. */
@SideOnly(Side.CLIENT)
public final class NEIMaterialPreview extends NEI_RecipeMap {
    public NEIMaterialPreview() { super(RM.Mixer); }

    @Override
    public NEI_RecipeMap newInstance() { return new NEIMaterialPreview(); }

    private static boolean installed;
    private static boolean registered;

    public static void install() {
        if (registered) return;
        registered = true;
        FMLCommonHandler.instance().bus().register(new NEIMaterialPreview());
        FMLLog.info("[GT6MaterialExpansion] Mixer NEI integration queued for client ticks");
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (installed || event.phase != TickEvent.Phase.END) return;
        if (replaceWhenAvailable()) installed = true;
    }

    private static boolean replaceWhenAvailable() {
        NEIMaterialPreview replacement = new NEIMaterialPreview();
        int replaced = 0;
        replaced += replaceCrafting(GuiCraftingRecipe.craftinghandlers, replacement);
        replaced += replaceCrafting(GuiCraftingRecipe.serialCraftingHandlers, replacement);
        replaced += replaceUsage(GuiUsageRecipe.usagehandlers, replacement);
        replaced += replaceUsage(GuiUsageRecipe.serialUsageHandlers, replacement);
        if (replaced == 0) return false;
        {
            for (ICraftingHandler handler : GuiCraftingRecipe.craftinghandlers) {
                if (handler != null) FMLLog.info("[GT6MaterialExpansion] NEI crafting handler: id=%s class=%s",
                        handler.getHandlerId(), handler.getClass().getName());
            }
            for (IUsageHandler handler : GuiUsageRecipe.usagehandlers) {
                if (handler != null) FMLLog.info("[GT6MaterialExpansion] NEI usage handler: id=%s class=%s",
                        handler.getHandlerId(), handler.getClass().getName());
            }
        }
        FMLLog.info("[GT6MaterialExpansion] Mixer NEI handler replacements: " + replaced
                + " (crafting=" + GuiCraftingRecipe.craftinghandlers.size()
                + ", serialCrafting=" + GuiCraftingRecipe.serialCraftingHandlers.size()
                + ", usage=" + GuiUsageRecipe.usagehandlers.size()
                + ", serialUsage=" + GuiUsageRecipe.serialUsageHandlers.size() + ")");
        return true;
    }

    private static int replaceCrafting(List<ICraftingHandler> handlers, NEIMaterialPreview replacement) {
        int count = 0;
        synchronized (handlers) {
            for (int i = 0; i < handlers.size(); i++) {
                ICraftingHandler handler = handlers.get(i);
                if (handler != null && RM.Mixer.mNameNEI.equals(handler.getHandlerId())) {
                    handlers.set(i, replacement);
                    count++;
                }
            }
        }
        return count;
    }

    private static int replaceUsage(List<IUsageHandler> handlers, NEIMaterialPreview replacement) {
        int count = 0;
        synchronized (handlers) {
            for (int i = 0; i < handlers.size(); i++) {
                IUsageHandler handler = handlers.get(i);
                if (handler != null && handler instanceof NEI_RecipeMap
                        && RM.Mixer.mNameNEI.equals(handler.getHandlerId())) {
                    handlers.set(i, replacement);
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public void loadCraftingRecipes(String id, Object... args) {
        super.loadCraftingRecipes(id, args);
        decorate();
    }

    @Override
    public void loadCraftingRecipes(ItemStack output) {
        super.loadCraftingRecipes(output);
        decorate();
    }

    @Override
    public void loadUsageRecipes(ItemStack input) {
        super.loadUsageRecipes(input);
        decorate();
    }

    private void decorate() {
        for (CachedRecipe cached : arecipes) {
            if (!(cached instanceof CachedDefaultRecipe)) continue;
            CachedDefaultRecipe displayedRecipe = (CachedDefaultRecipe) cached;
            Recipe recipe = displayedRecipe.mRecipe;
            if (recipe == null || recipe.mOutputs == null || recipe.mOutputs.length != 1
                    || recipe.mOutputs[0] == null || displayedRecipe.mOutputs.isEmpty()) continue;
            List<OreDictMaterialStack> parts = RecipeMaterialPreview.forOutput(recipe, 0);
            if (parts == null || parts.isEmpty()) continue;
            ItemStack preview = RecipePreviewTags.copyWithMaterials(recipe.mOutputs[0], parts);
            if (preview == null) continue;
            PositionedStack displayed = displayedRecipe.mOutputs.get(0);
            displayed.items = new ItemStack[] {preview};
            displayed.item = preview;
        }
    }
}
