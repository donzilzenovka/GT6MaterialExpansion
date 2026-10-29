package zenovka.gt6materialexpansion;

import gregapi.data.CS;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.recipes.Recipe;
import gregapi.data.MT;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.util.OM;
import cpw.mods.fml.common.FMLLog;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Conservative canonical resolver. Handles unambiguous crafting and furnace
 * recipes; ambiguous NEI ordering and GT6 machine/fluid recipes remain unresolved.
 * Unknown or cyclic ingredients become Matter, never a fabricated material.
 */
public final class MaterialDefaultResolver {
    private MaterialDefaultResolver() {}
    private static volatile Map<String, List<Recipe>> MACHINE_INDEX;
    private static final Map<String, List<OreDictMaterialStack>> CANONICAL_CACHE =
            new ConcurrentHashMap<String, List<OreDictMaterialStack>>();

    private static String itemKey(ItemStack stack) {
        return String.valueOf(net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem()))
                + ":" + stack.getItemDamage();
    }

    private static synchronized Map<String, List<Recipe>> machineIndex() {
        if (MACHINE_INDEX != null) return MACHINE_INDEX;
        Map<String, List<Recipe>> index = new LinkedHashMap<String, List<Recipe>>();
        for (Recipe.RecipeMap map : Recipe.RecipeMap.RECIPE_MAP_LIST) {
            for (Recipe recipe : map.mRecipeList) {
                if (recipe.mOutputs == null) continue;
                for (ItemStack output : recipe.mOutputs) {
                    if (output == null || output.getItem() == null) continue;
                    String key = itemKey(output);
                    List<Recipe> recipes = index.get(key);
                    if (recipes == null) {
                        recipes = new ArrayList<Recipe>();
                        index.put(key, recipes);
                    }
                    recipes.add(recipe);
                }
            }
        }
        MACHINE_INDEX = index;
        FMLLog.info("[GT6MaterialExpansion] Indexed %s GT6 machine output identities", index.size());
        return index;
    }

    private static List<OreDictMaterialStack> resolveMachine(Recipe recipe, ItemStack target,
            Set<String> visiting, int depth) {
        if (recipe.mInputs == null || recipe.mOutputs == null) return matter();
        ItemStack output = null;
        int count = 0;
        for (ItemStack candidate : recipe.mOutputs) {
            if (candidate != null && candidate.stackSize > 0) {
                count++;
                if (candidate.getItem() == target.getItem()
                        && candidate.getItemDamage() == target.getItemDamage()) output = candidate;
            }
        }
        if (count != 1 || output == null || output.stackSize <= 0) return matter();
        Map<OreDictMaterial, Long> totals = new LinkedHashMap<OreDictMaterial, Long>();
        for (ItemStack input : recipe.mInputs) {
            if (input == null || input.stackSize <= 0) continue;
            ItemStack single = input.copy();
            single.stackSize = 1;
            if (!add(totals, resolve(single, visiting, depth + 1), input.stackSize)) return matter();
        }
        List<OreDictMaterialStack> result = new ArrayList<OreDictMaterialStack>();
        for (Map.Entry<OreDictMaterial, Long> part : totals.entrySet()) {
            if (part.getValue() < 0 || part.getValue() % output.stackSize != 0) return matter();
            if (part.getValue() > 0) result.add(OM.stack(part.getKey(), part.getValue() / output.stackSize));
        }
        return result.isEmpty() ? matter() : result;
    }

    /**
     * Ender IO Slice'N'Splice: only the six manufacturing slots contribute
     * material. The axe and shears are tooling, not recipe ingredients.
     * Reflection keeps Ender IO optional at class-loading time.
     */
    private static List<OreDictMaterialStack> resolveSlice(ItemStack target,
            Set<String> visiting, int depth) {
        if (!cpw.mods.fml.common.Loader.isModLoaded("EnderIO")) return null;
        try {
            Class<?> managerClass = Class.forName(
                    "crazypants.enderio.machine.slicensplice.SliceAndSpliceRecipeManager");
            Object manager = managerClass.getMethod("getInstance").invoke(null);
            Object raw = managerClass.getMethod("getRecipes").invoke(manager);
            if (!(raw instanceof Iterable)) return null;
            List<Object> matches = new ArrayList<Object>();
            for (Object recipe : (Iterable<?>) raw) {
                Object[] outputs = (Object[]) recipe.getClass().getMethod("getOutputs").invoke(recipe);
                if (outputs == null || outputs.length != 1 || outputs[0] == null) continue;
                ItemStack output = (ItemStack) outputs[0].getClass().getMethod("getOutput").invoke(outputs[0]);
                if (output != null && output.getItem() == target.getItem()
                        && output.getItemDamage() == target.getItemDamage()) matches.add(recipe);
            }
            if (matches.isEmpty()) return null;
            if (matches.size() != 1) {
                warnOnce(itemKey(target), "multiple Slice'N'Splice recipes; composition ambiguous");
                return matter();
            }
            Object recipe = matches.get(0);
            Object[] outputs = (Object[]) recipe.getClass().getMethod("getOutputs").invoke(recipe);
            ItemStack output = (ItemStack) outputs[0].getClass().getMethod("getOutput").invoke(outputs[0]);
            if (output == null || output.stackSize <= 0) return matter();
            Object[] inputs = (Object[]) recipe.getClass().getMethod("getInputs").invoke(recipe);
            if (inputs == null) return matter();
            Map<OreDictMaterial, Long> totals = new LinkedHashMap<OreDictMaterial, Long>();
            for (Object input : inputs) {
                if (input == null) continue;
                Class<?> type = input.getClass();
                int slot = ((Number) type.getMethod("getSlotNumber").invoke(input)).intValue();
                if (slot < 0 || slot > 5) return matter();
                if (Boolean.TRUE.equals(type.getMethod("isFluid").invoke(input))) return matter();
                ItemStack primary = (ItemStack) type.getMethod("getInput").invoke(input);
                if (primary == null || primary.getItem() == null || primary.stackSize <= 0) return matter();
                List<OreDictMaterialStack> parts = resolve(primary, visiting, depth + 1);
                if (isMatter(parts)) {
                    warnOnce(itemKey(target), "Slice'N'Splice unresolved ingredient slot " + slot
                            + ": " + itemKey(primary));
                    return matter();
                }
                Object alternatives = type.getMethod("getEquivelentInputs").invoke(input);
                if (alternatives instanceof ItemStack[]) {
                    for (ItemStack alternative : (ItemStack[]) alternatives) {
                        if (alternative == null || alternative.getItem() == null) return matter();
                        List<OreDictMaterialStack> other = resolve(alternative, visiting, depth + 1);
                        if (!sameMaterials(parts, other)) {
                            warnOnce(itemKey(target), "Slice'N'Splice alternative differs at slot " + slot
                                    + ": " + itemKey(primary) + " versus " + itemKey(alternative));
                            return matter();
                        }
                    }
                }
                if (!add(totals, parts, primary.stackSize)) {
                    warnOnce(itemKey(target), "Slice'N'Splice invalid material amount at slot " + slot);
                    return matter();
                }
            }
            List<OreDictMaterialStack> result = new ArrayList<OreDictMaterialStack>();
            for (Map.Entry<OreDictMaterial, Long> entry : totals.entrySet()) {
                if (entry.getValue() < 0 || entry.getValue() % output.stackSize != 0) {
                    warnOnce(itemKey(target), "Slice'N'Splice material quantities not divisible by output count");
                    return matter();
                }
                if (entry.getValue() > 0)
                    result.add(OM.stack(entry.getKey(), entry.getValue() / output.stackSize));
            }
            return result.isEmpty() ? matter() : result;
        } catch (ReflectiveOperationException | ClassCastException ex) {
            warnOnce(itemKey(target), "Slice'N'Splice registry lookup failed: " + ex.getClass().getSimpleName());
            return null;
        }
    }

    private static boolean isMatter(List<OreDictMaterialStack> parts) {
        return parts == null || parts.isEmpty()
                || (parts.size() == 1 && parts.get(0).mMaterial == GT6MaterialExpansion.Matter);
    }

    private static boolean sameMaterials(List<OreDictMaterialStack> a, List<OreDictMaterialStack> b) {
        if (isMatter(a) || isMatter(b)) return false;
        Map<OreDictMaterial, Long> left = new LinkedHashMap<OreDictMaterial, Long>();
        Map<OreDictMaterial, Long> right = new LinkedHashMap<OreDictMaterial, Long>();
        return add(left, a, 1) && add(right, b, 1) && left.equals(right);
    }

    private static final Set<String> WARNED = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private static void warnOnce(String key, String reason) {
        if (WARNED.add(key + "|" + reason))
            FMLLog.warning("[GT6MaterialExpansion][canonical-order] %s: %s; assigning Matter (Unresolved)", key, reason);
    }

    public static List<OreDictMaterialStack> resolve(ItemStack stack) {
        List<OreDictMaterialStack> result = resolve(stack, new HashSet<String>(), 0);
        if (result != null && !result.isEmpty()) return result;
        return matter();
    }

    private static List<OreDictMaterialStack> resolve(ItemStack stack, Set<String> visiting, int depth) {
        if (stack == null || stack.getItem() == null || depth > 24) return matter();
        // A GT6 tool is not a canonical craft ingredient: do not recursively
        // invent a material composition for its manufacturing recipe.
        if (stack.getItem() instanceof MultiItemTool) return Collections.emptyList();
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            String key = tag.hasKey("gt.recycling.mats") ? "gt.recycling.mats"
                    : tag.hasKey("zenovka.original.mats") ? "zenovka.original.mats" : null;
            if (key != null) {
                List<OreDictMaterialStack> inherited = OreDictMaterialStack.loadList(key, tag);
                if (inherited != null && !inherited.isEmpty()) return inherited;
            }
        }
        // Forge's 32767 is a recipe wildcard, not a concrete material identity.
        // Resolve metadata 0 only for items that do not have subtypes; for
        // subtype-bearing items the wildcard may represent different materials.
        if (stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
            if (stack.getItem().getHasSubtypes()) return matter();
            ItemStack concrete = stack.copy();
            concrete.setItemDamage(0);
            return resolve(concrete, visiting, depth + 1);
        }
        OreDictItemData direct = OM.anydata(stack);
        if (direct != null && direct.getAllMaterialStacks() != null
                && !direct.getAllMaterialStacks().isEmpty())
            return direct.getAllMaterialStacks();

        String key = String.valueOf(net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem()))
                + ":" + stack.getItemDamage();
        if (!visiting.add(key)) return matter();
        List<OreDictMaterialStack> cached = CANONICAL_CACHE.get(key);
        if (cached != null) {
            visiting.remove(key);
            return cached;
        }
        try {
            // Prefer the machine's own recipe over unrelated crafting/GT6 paths.
            List<OreDictMaterialStack> sliced = resolveSlice(stack, visiting, depth);
            if (sliced != null) {
                if (!isMatter(sliced)) CANONICAL_CACHE.put(key, sliced);
                return sliced;
            }
            IRecipe selected = null;
            int craftingMatches = 0;
            for (Object entry : CraftingManager.getInstance().getRecipeList()) {
                if (!(entry instanceof IRecipe)) continue;
                IRecipe recipe = (IRecipe) entry;
                ItemStack output = recipe.getRecipeOutput();
                if (output == null || output.getItem() != stack.getItem()
                        || output.getItemDamage() != stack.getItemDamage()) continue;
                craftingMatches++;
                selected = recipe;
            }
            if (craftingMatches > 1) {
                warnOnce(key, "multiple crafting recipes; NEI handler ordering not verified");
                return matter();
            }
            if (craftingMatches == 0) {
                ItemStack furnaceInput = null;
                int furnaceMatches = 0;
                for (Object raw : FurnaceRecipes.smelting().getSmeltingList().entrySet()) {
                    Map.Entry<?, ?> entry = (Map.Entry<?, ?>) raw;
                    if (!(entry.getValue() instanceof ItemStack) || !(entry.getKey() instanceof ItemStack)) continue;
                    ItemStack output = (ItemStack) entry.getValue();
                    if (output.getItem() == stack.getItem() && output.getItemDamage() == stack.getItemDamage()) {
                        furnaceMatches++;
                        furnaceInput = (ItemStack) entry.getKey();
                    }
                }
                if (furnaceMatches > 1) {
                    warnOnce(key, "multiple furnace recipes; NEI handler ordering not verified");
                    return matter();
                }
                if (furnaceMatches == 1) return resolve(furnaceInput, visiting, depth + 1);
                List<Recipe> machines = machineIndex().get(key);
                if (machines != null && !machines.isEmpty()) {
                    if (machines.size() > 1)
                        warnOnce(key, "multiple machine recipes; using indexed registration order, not verified NEI order");
                    List<OreDictMaterialStack> resolved = resolveMachine(machines.get(0), stack, visiting, depth);
                    if (resolved != null && !resolved.isEmpty()
                            && !(resolved.size() == 1 && resolved.get(0).mMaterial == GT6MaterialExpansion.Matter))
                        CANONICAL_CACHE.put(key, resolved);
                    return resolved;
                }
                return matter();
            }
            IRecipe recipe = selected;
            ItemStack output = recipe.getRecipeOutput();
            Object[] inputs = inputs(recipe);
            if (inputs == null || output.stackSize <= 0) {
                warnOnce(key, "unsupported crafting recipe type or invalid output count");
                return matter();
            }
            {
                Map<OreDictMaterial, Long> totals = new LinkedHashMap<OreDictMaterial, Long>();
                for (Object ingredient : inputs) {
                    ItemStack chosen = choose(ingredient);
                    if (chosen == null) {
                        if (ingredient == null) continue;
                        return matter();
                    }
                    List<OreDictMaterialStack> parts = resolve(chosen, visiting, depth + 1);
                    if (!add(totals, parts, 1)) return matter();
                    // Container returns must be accounted for per ingredient.
                    if (chosen.getItem().hasContainerItem(chosen)) {
                        ItemStack container = chosen.getItem().getContainerItem(chosen);
                        if (container != null) {
                            List<OreDictMaterialStack> returned = resolve(container, visiting, depth + 1);
                            if (!add(totals, returned, -1)) return matter();
                        }
                    }
                }
                List<OreDictMaterialStack> result = new ArrayList<OreDictMaterialStack>();
                for (Map.Entry<OreDictMaterial, Long> part : totals.entrySet()) {
                    if (part.getValue() < 0 || part.getValue() % output.stackSize != 0)
                        return matter();
                    if (part.getValue() > 0)
                        result.add(OM.stack(part.getKey(), part.getValue() / output.stackSize));
                }
                return result.isEmpty() ? matter() : result;
            }
        } finally {
            visiting.remove(key);
        }
    }

    private static Object[] inputs(IRecipe recipe) {
        if (recipe instanceof ShapedRecipes) return ((ShapedRecipes) recipe).recipeItems;
        if (recipe instanceof ShapelessRecipes)
            return ((ShapelessRecipes) recipe).recipeItems.toArray();
        if (recipe instanceof ShapedOreRecipe) return ((ShapedOreRecipe) recipe).getInput();
        if (recipe instanceof ShapelessOreRecipe)
            return ((ShapelessOreRecipe) recipe).getInput().toArray();
        return null;
    }

    private static ItemStack choose(Object ingredient) {
        if (ingredient instanceof ItemStack) {
            ItemStack copy = ((ItemStack) ingredient).copy();
            copy.stackSize = 1;
            return copy;
        }
        if (ingredient instanceof List) {
            for (Object candidate : (List<?>) ingredient)
                if (candidate instanceof ItemStack) return choose(candidate);
        }
        return null;
    }

    private static boolean add(Map<OreDictMaterial, Long> totals,
                               List<OreDictMaterialStack> parts, int sign) {
        if (parts == null) return false;
        for (OreDictMaterialStack part : parts) {
            if (part == null || part.mMaterial == null) return false;
            if (part.mMaterial == MT.Empty && part.mAmount == -1) continue;
            if (part.mAmount < 0) return false;
            long before = totals.containsKey(part.mMaterial) ? totals.get(part.mMaterial) : 0L;
            long delta = part.mAmount * sign;
            long next = before + delta;
            if ((delta > 0 && next < before) || (delta < 0 && next > before)) return false;
            totals.put(part.mMaterial, next);
        }
        return true;
    }

    private static List<OreDictMaterialStack> matter() {
        List<OreDictMaterialStack> result = new ArrayList<OreDictMaterialStack>(1);
        result.add(OM.stack(GT6MaterialExpansion.Matter, CS.U));
        return result;
    }
}
