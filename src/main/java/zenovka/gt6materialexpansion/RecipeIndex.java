package zenovka.gt6materialexpansion;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import codechicken.nei.NEIServerUtils;
import java.util.*;

/**
 * Session-only reverse lookup for NEI. Holds recipe references, never material
 * preview results or copies of the registered recipes.
 */
public final class RecipeIndex {
    private static List<?> source;
    private static IRecipe[] snapshot = new IRecipe[0];
    private static final Map<Item, List<IRecipe>> inputs = new IdentityHashMap<Item, List<IRecipe>>();
    private static final Map<Item, List<IRecipe>> outputs = new IdentityHashMap<Item, List<IRecipe>>();
    private RecipeIndex() {}

    private static synchronized void ensureCurrent() {
        List<?> current = CraftingManager.getInstance().getRecipeList();
        // Detect additions, removals, reorderings and recipe-object replacement.
        boolean changed = source != current || current.size() != snapshot.length;
        if (!changed) {
            for (int i = 0; i < snapshot.length; i++) {
                if (current.get(i) != snapshot[i]) { changed = true; break; }
            }
        }
        if (!changed) return;
        inputs.clear();
        outputs.clear();
        snapshot = new IRecipe[current.size()];
        source = current;
        for (int i = 0; i < current.size(); i++) {
            Object candidate = current.get(i);
            if (!(candidate instanceof IRecipe)) continue;
            IRecipe recipe = (IRecipe) candidate;
            snapshot[i] = recipe;
            ItemStack output = recipe.getRecipeOutput();
            if (output != null && output.getItem() != null)
                add(outputs, output.getItem(), recipe);
            Object[] ingredients = ingredients(recipe);
            if (ingredients == null) continue;
            Set<Item> seen = Collections.newSetFromMap(new IdentityHashMap<Item, Boolean>());
            for (Object ingredient : ingredients)
                collect(ingredient, seen);
            for (Item item : seen) add(inputs, item, recipe);
        }
    }

    private static void add(Map<Item, List<IRecipe>> map, Item item, IRecipe recipe) {
        List<IRecipe> recipes = map.get(item);
        if (recipes == null) { recipes = new ArrayList<IRecipe>(); map.put(item, recipes); }
        recipes.add(recipe);
    }

    private static void collect(Object ingredient, Set<Item> found) {
        if (ingredient instanceof ItemStack) {
            Item item = ((ItemStack) ingredient).getItem();
            if (item != null) found.add(item);
        } else if (ingredient instanceof List<?>) {
            for (Object alternative : (List<?>) ingredient) collect(alternative, found);
        }
    }

    private static Object[] ingredients(IRecipe recipe) {
        if (recipe instanceof ShapedRecipes) return ((ShapedRecipes) recipe).recipeItems;
        if (recipe instanceof ShapedOreRecipe) return ((ShapedOreRecipe) recipe).getInput();
        if (recipe instanceof ShapelessRecipes) {
            List<?> list = ((ShapelessRecipes) recipe).recipeItems;
            return list == null ? null : list.toArray();
        }
        if (recipe instanceof ShapelessOreRecipe) {
            List<?> list = ((ShapelessOreRecipe) recipe).getInput();
            return list == null ? null : list.toArray();
        }
        return null;
    }

    public static synchronized List<IRecipe> all() {
        ensureCurrent();
        return new ArrayList<IRecipe>(Arrays.asList(snapshot));
    }

    public static synchronized List<IRecipe> producing(ItemStack wanted) {
        ensureCurrent();
        if (wanted == null || wanted.getItem() == null) return Collections.emptyList();
        List<IRecipe> candidates = outputs.get(wanted.getItem());
        if (candidates == null) return Collections.emptyList();
        List<IRecipe> matches = new ArrayList<IRecipe>();
        for (IRecipe recipe : candidates)
            if (NEIServerUtils.areStacksSameTypeCrafting(recipe.getRecipeOutput(), wanted))
                matches.add(recipe);
        return matches;
    }

    public static synchronized List<IRecipe> using(ItemStack wanted) {
        ensureCurrent();
        if (wanted == null || wanted.getItem() == null) return Collections.emptyList();
        List<IRecipe> candidates = inputs.get(wanted.getItem());
        if (candidates == null) return Collections.emptyList();
        List<IRecipe> matches = new ArrayList<IRecipe>();
        for (IRecipe recipe : candidates) {
            Object[] ingredients = ingredients(recipe);
            if (ingredients == null) continue;
            for (Object ingredient : ingredients) {
                if (matches(ingredient, wanted)) { matches.add(recipe); break; }
            }
        }
        return matches;
    }

    private static boolean matches(Object input, ItemStack wanted) {
        if (input instanceof ItemStack)
            return NEIServerUtils.areStacksSameTypeCrafting((ItemStack) input, wanted);
        if (input instanceof List<?>) {
            for (Object alternative : (List<?>) input)
                if (matches(alternative, wanted)) return true;
        }
        return false;
    }

    /** Call when a mod mutates ingredient arrays without replacing recipe objects. */
    public static synchronized void invalidate() {
        source = null;
    }
}
