package zenovka.gt6materialexpansion;

import codechicken.nei.PositionedStack;
import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.api.IRecipeOverlayRenderer;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.IUsageHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Delegating NEI view decorator. The underlying mod still selects and renders
 * its recipes; only ingredient display copies receive missing material data.
 * Initial rollout is restricted to Ender IO to verify compatibility.
 */
@SideOnly(Side.CLIENT)
public final class NEIIngredientDecorator implements ICraftingHandler, IUsageHandler {
    private final IRecipeHandler delegate;
    private final Set<Integer> diagnosedRecipes = new java.util.HashSet<Integer>();
    private final Set<PositionedStack> decorated =
            Collections.newSetFromMap(new IdentityHashMap<PositionedStack, Boolean>());

    private NEIIngredientDecorator(IRecipeHandler delegate) { this.delegate = delegate; }

    private static boolean eligible(IRecipeHandler handler) {
        return handler != null && !(handler instanceof NEIIngredientDecorator)
                && !(handler instanceof NEIMaterialPreview)
                && !(handler instanceof NEICraftingMaterialPreview)
                && !(handler instanceof NEIFurnaceMaterialPreview);
    }

    private static boolean registered;

    /** Ender IO registers its NEI handlers after FML load-complete. */
    public static void install() {
        if (registered) return;
        registered = true;
        FMLCommonHandler.instance().bus().register(new NEIIngredientDecorator(null));
        FMLLog.info("[GT6MaterialExpansion][nei-ingredients] Waiting for NEI usage handlers");
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        int count = 0;
        count += wrapCrafting(GuiCraftingRecipe.craftinghandlers);
        count += wrapCrafting(GuiCraftingRecipe.serialCraftingHandlers);
        count += wrapUsage(GuiUsageRecipe.usagehandlers);
        count += wrapUsage(GuiUsageRecipe.serialUsageHandlers);
        if (count > 0) {
            FMLLog.info("[GT6MaterialExpansion][nei-ingredients] Wrapped %d late NEI crafting/usage handler registrations", count);
        }
    }

    private static int wrapCrafting(List<ICraftingHandler> handlers) {
        int count = 0;
        synchronized (handlers) {
            for (int i = 0; i < handlers.size(); i++) {
                ICraftingHandler handler = handlers.get(i);
                if (eligible(handler)) {
                    handlers.set(i, new NEIIngredientDecorator(handler));
                    count++;
                }
            }
        }
        return count;
    }

    private static int wrapUsage(List<IUsageHandler> handlers) {
        int count = 0;
        synchronized (handlers) {
            for (int i = 0; i < handlers.size(); i++) {
                IUsageHandler handler = handlers.get(i);
                if (eligible(handler)) {
                    handlers.set(i, new NEIIngredientDecorator(handler));
                    count++;
                }
            }
        }
        return count;
    }

    private void decorate(List<PositionedStack> stacks) {
        if (stacks == null) return;
        for (PositionedStack stack : stacks) {
            if (stack == null || stack.items == null || !decorated.add(stack)) continue;
            ItemStack[] copies = new ItemStack[stack.items.length];
            for (int i = 0; i < copies.length; i++) {
                ItemStack item = stack.items[i];
                // Isolation test: the four GT6 multitileentity variants below
                // enqueue client sounds when the generic material resolver is
                // invoked by NEI ingredient cycling. Keep the original preview
                // stack untouched; do not alter recipes or machine outputs.
                Object registryName = item == null || item.getItem() == null ? null
                        : net.minecraft.item.Item.itemRegistry.getNameForObject(item.getItem());
                boolean isolate = "gregtech:gt.multitileentity".equals(String.valueOf(registryName))
                        && item.getItemDamage() >= 1001 && item.getItemDamage() <= 1004;
                copies[i] = item == null || item.getItem() == null || isolate
                        ? item : RecipePreviewTags.copyWithMaterials(
                                item, MaterialDefaultResolver.resolve(item));
            }
            stack.items = copies;
            if (copies.length > 0) stack.item = copies[0];
        }
    }

    @Override public ICraftingHandler getRecipeHandler(String id, Object... args) {
        if (!(delegate instanceof ICraftingHandler)) return this;
        Object[] lookup = normalized(args);
        ICraftingHandler selected = ((ICraftingHandler) delegate).getRecipeHandler(id, lookup);
        return selected == null ? null : new NEIIngredientDecorator(selected);
    }

    @Override public IUsageHandler getUsageHandler(String id, Object... args) {
        return selectUsage(id, args, false);
    }

    @Override public IUsageHandler getUsageAndCatalystHandler(String id, Object... args) {
        return selectUsage(id, args, true);
    }

    private IUsageHandler selectUsage(String id, Object[] args, boolean catalyst) {
        if (!(delegate instanceof IUsageHandler)) return null;
        IUsageHandler handler = (IUsageHandler) delegate;
        Object[] original = normalized(args);
        Object[] enriched = original.clone();
        boolean changed = false;
        if ("item".equals(id)) {
            for (int i = 0; i < enriched.length; i++) {
                if (!(enriched[i] instanceof ItemStack)) continue;
                ItemStack item = (ItemStack) enriched[i];
                if (item.getItem() == null) continue;
                gregapi.oredict.OreDictItemData existing = gregapi.util.OM.anydata(item);
                if (existing != null && existing.getAllMaterialStacks() != null
                        && !existing.getAllMaterialStacks().isEmpty()
                        && existing.getAllMaterialStacks().get(0).mMaterial != GT6MaterialExpansion.Matter)
                    continue;
                ItemStack resolved = RecipePreviewTags.copyWithMaterials(
                        item, MaterialDefaultResolver.resolve(item));
                if (resolved != null && !net.minecraft.item.ItemStack.areItemStackTagsEqual(item, resolved)) {
                    enriched[i] = resolved;
                    changed = true;
                }
            }
        }
        IUsageHandler selected = catalyst
                ? handler.getUsageAndCatalystHandler(id, enriched)
                : handler.getUsageHandler(id, enriched);
        if (changed && (selected == null || selected.numRecipes() == 0)) {
            selected = catalyst ? handler.getUsageAndCatalystHandler(id, original)
                    : handler.getUsageHandler(id, original);
        }
        return selected == null ? null : new NEIIngredientDecorator(selected);
    }

    private static Object[] normalized(Object[] args) {
        Object[] result = args.clone();
        for (int i = 0; i < result.length; i++)
            if (result[i] instanceof ItemStack)
                result[i] = NEIRecipeLookup.identity((ItemStack) result[i]);
        return result;
    }

    @Override public String getHandlerId() { return delegate.getHandlerId(); }
    @Override public String getRecipeName() { return delegate.getRecipeName(); }
    @Override public String getRecipeTabName() { return delegate.getRecipeTabName(); }
    @Override public int getRecipeHeight(int recipe) { return delegate.getRecipeHeight(recipe); }
    @Override public int numRecipes() { return delegate.numRecipes(); }
    @Override public void drawBackground(int recipe) { delegate.drawBackground(recipe); }
    @Override public void drawForeground(int recipe) { delegate.drawForeground(recipe); }
    @Override public List<PositionedStack> getIngredientStacks(int recipe) {
        List<PositionedStack> stacks = delegate.getIngredientStacks(recipe);
        diagnoseSlice(recipe, stacks);
        decorate(stacks);
        return stacks;
    }
    /**
     * One-shot, read-only Slice'N'Splice diagnostics. Position and permutation
     * counts help distinguish tool slots from consumed ingredient slots.
     */
    private void diagnoseSlice(int recipe, List<PositionedStack> inputs) {
        String name = delegate.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        String id = delegate.getHandlerId();
        String title = delegate.getRecipeName();
        String descriptor = (id == null ? "" : id) + " " + (title == null ? "" : title);
        if (!name.contains("slice") && !descriptor.toLowerCase(java.util.Locale.ROOT).contains("slice"))
            return;
        synchronized (diagnosedRecipes) {
            if (!diagnosedRecipes.add(recipe)) return;
        }
        FMLLog.info("[GT6MaterialExpansion][slice-diagnostic] handler=%s id=%s recipe=%d/%d inputs=%d",
                delegate.getClass().getName(), id, recipe, delegate.numRecipes(),
                inputs == null ? 0 : inputs.size());
        if (inputs != null) {
            for (int i = 0; i < inputs.size(); i++) {
                PositionedStack slot = inputs.get(i);
                if (slot == null) continue;
                ItemStack[] variants = slot.items;
                FMLLog.info("[GT6MaterialExpansion][slice-diagnostic] input[%d] x=%d y=%d variants=%d",
                        i, slot.relx, slot.rely, variants == null ? 0 : variants.length);
                if (variants == null) continue;
                for (int j = 0; j < variants.length && j < 8; j++)
                    diagnoseSliceItem("input[" + i + "].variant[" + j + "]", variants[j]);
            }
        }
        PositionedStack output = delegate.getResultStack(recipe);
        if (output != null) diagnoseSliceItem("output", output.item);
        List<PositionedStack> other = delegate.getOtherStacks(recipe);
        if (other != null) for (int i = 0; i < other.size(); i++) {
            PositionedStack slot = other.get(i);
            if (slot != null) diagnoseSliceItem("other[" + i + "]", slot.item);
        }
    }

    private static void diagnoseSliceItem(String label, ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            FMLLog.info("[GT6MaterialExpansion][slice-diagnostic] %s null", label);
            return;
        }
        Object key = net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem());
        gregapi.oredict.OreDictItemData existing = gregapi.util.OM.anydata(stack);
        java.util.List<gregapi.oredict.OreDictMaterialStack> inferred = MaterialDefaultResolver.resolve(stack);
        FMLLog.info("[GT6MaterialExpansion][slice-diagnostic] %s item=%s meta=%d count=%d nbt=%s existing=%s inferred=%s",
                label, key, stack.getItemDamage(), stack.stackSize,
                stack.hasTagCompound() ? stack.getTagCompound().toString() : "none",
                String.valueOf(existing), String.valueOf(inferred));
    }

    @Override public List<PositionedStack> getOtherStacks(int recipe) {
        List<PositionedStack> stacks = delegate.getOtherStacks(recipe);
        decorate(stacks);
        return stacks;
    }
    @Override public PositionedStack getResultStack(int recipe) {
        PositionedStack output = delegate.getResultStack(recipe);
        if (output == null || output.item == null) return output;
        String type = delegate.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        String id = delegate.getHandlerId();
        String descriptor = type + " " + (id == null ? "" : id.toLowerCase(java.util.Locale.ROOT));
        if (descriptor.contains("alloy")) {
            // NEI's displayed ingredient counts represent a recipe operation.
            // Only accept deterministic inputs: no cycling ore-dictionary variants.
            List<PositionedStack> ingredients = delegate.getIngredientStacks(recipe);
            if (ingredients == null || ingredients.isEmpty()) return output;
            java.util.ArrayList<ItemStack> consumed = new java.util.ArrayList<ItemStack>();
            for (PositionedStack ingredient : ingredients) {
                if (ingredient == null || ingredient.items == null || ingredient.items.length != 1
                        || ingredient.items[0] == null || ingredient.items[0].stackSize <= 0)
                    return output;
                consumed.add(ingredient.items[0].copy());
            }
            List<gregapi.oredict.OreDictMaterialStack> materials = AlloySmelterMaterials.calculate(
                    consumed.toArray(new ItemStack[consumed.size()]), output.item);
            if (materials == null) return output;
            ItemStack preview = RecipePreviewTags.copyWithMaterials(output.item, materials);
            if (preview != null && !ItemStack.areItemStackTagsEqual(output.item, preview))
                return new PositionedStack(preview, output.relx, output.rely);
            return output;
        }
        if (!descriptor.contains("slice")) return output;
        ItemStack resolved = RecipePreviewTags.copyWithMaterials(
                output.item, MaterialDefaultResolver.resolve(output.item));
        // Never modify the registered Ender IO recipe's output stack.
        if (resolved != null && !ItemStack.areItemStackTagsEqual(output.item, resolved))
            return new PositionedStack(resolved, output.relx, output.rely);
        return output;
    }
    @Override public void onUpdate() { delegate.onUpdate(); }
    @Override public boolean hasOverlay(GuiContainer gui, Container container, int recipe) {
        return delegate.hasOverlay(gui, container, recipe);
    }
    @Override public IRecipeOverlayRenderer getOverlayRenderer(GuiContainer gui, int recipe) {
        return delegate.getOverlayRenderer(gui, recipe);
    }
    @Override public IOverlayHandler getOverlayHandler(GuiContainer gui, int recipe) {
        return delegate.getOverlayHandler(gui, recipe);
    }
    @Override public String getOverlayIdentifier() { return delegate.getOverlayIdentifier(); }
    @Override public List<String> handleTooltip(GuiRecipe<?> gui, List<String> tip, int recipe) {
        return delegate.handleTooltip(gui, tip, recipe);
    }
    @Override public List<String> handleItemTooltip(GuiRecipe<?> gui, ItemStack stack, List<String> tip, int recipe) {
        return delegate.handleItemTooltip(gui, stack, tip, recipe);
    }
    @Override public boolean keyTyped(GuiRecipe<?> gui, char c, int code, int recipe) {
        return delegate.keyTyped(gui, c, code, recipe);
    }
    @Override public boolean mouseClicked(GuiRecipe<?> gui, int button, int recipe) {
        return delegate.mouseClicked(gui, button, recipe);
    }
    @Override public boolean mouseScrolled(GuiRecipe<?> gui, int scroll, int recipe) {
        return delegate.mouseScrolled(gui, scroll, recipe);
    }
    @Override public int recipiesPerPage() { return delegate.recipiesPerPage(); }
}
