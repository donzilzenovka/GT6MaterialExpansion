package zenovka.gt6materialexpansion;

import gregapi.data.CS;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            String key = tag.hasKey("gt.recycling.mats") ? "gt.recycling.mats"
                    : tag.hasKey("zenovka.original.mats") ? "zenovka.original.mats" : null;
            if (key != null) {
                List<OreDictMaterialStack> inherited = OreDictMaterialStack.loadList(key, tag);
                if (inherited != null && !inherited.isEmpty()) return inherited;
            }
        }
        OreDictItemData direct = OM.anydata(stack);
        if (direct != null && direct.getAllMaterialStacks() != null
                && !direct.getAllMaterialStacks().isEmpty())
            return direct.getAllMaterialStacks();

        String key = String.valueOf(net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem()))
                + ":" + stack.getItemDamage();
        if (!visiting.add(key)) return matter();
        try {
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
            return matter();
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
