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
    private final Set<PositionedStack> decorated =
            Collections.newSetFromMap(new IdentityHashMap<PositionedStack, Boolean>());

    private NEIIngredientDecorator(IRecipeHandler delegate) { this.delegate = delegate; }

    private static boolean eligible(IRecipeHandler handler) {
        return handler != null && !(handler instanceof NEIIngredientDecorator)
                && handler.getClass().getName().toLowerCase(java.util.Locale.ROOT).contains("enderio");
    }

    private static boolean registered;
    private static boolean foundHandlers;

    /** Ender IO registers its NEI handlers after FML load-complete. */
    public static void install() {
        if (registered) return;
        registered = true;
        FMLCommonHandler.instance().bus().register(new NEIIngredientDecorator(null));
        FMLLog.info("[GT6MaterialExpansion][nei-ingredients] Waiting for Ender IO NEI handlers");
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (foundHandlers || event.phase != TickEvent.Phase.END) return;
        int count = 0;
        count += wrapCrafting(GuiCraftingRecipe.craftinghandlers);
        count += wrapCrafting(GuiCraftingRecipe.serialCraftingHandlers);
        count += wrapUsage(GuiUsageRecipe.usagehandlers);
        count += wrapUsage(GuiUsageRecipe.serialUsageHandlers);
        if (count > 0) {
            foundHandlers = true;
            FMLLog.info("[GT6MaterialExpansion][nei-ingredients] Wrapped %d late Ender IO handler registrations", count);
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
                copies[i] = item == null || item.getItem() == null
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
        if (!(delegate instanceof IUsageHandler)) return this;
        IUsageHandler selected = ((IUsageHandler) delegate).getUsageHandler(id, normalized(args));
        return selected == null ? null : new NEIIngredientDecorator(selected);
    }

    @Override public IUsageHandler getUsageAndCatalystHandler(String id, Object... args) {
        if (!(delegate instanceof IUsageHandler)) return this;
        IUsageHandler selected = ((IUsageHandler) delegate).getUsageAndCatalystHandler(id, normalized(args));
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
        decorate(stacks);
        return stacks;
    }
    @Override public List<PositionedStack> getOtherStacks(int recipe) {
        List<PositionedStack> stacks = delegate.getOtherStacks(recipe);
        decorate(stacks);
        return stacks;
    }
    @Override public PositionedStack getResultStack(int recipe) { return delegate.getResultStack(recipe); }
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
