package zenovka.gt6materialexpansion;

import gregapi.data.MT;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.util.OM;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared, conservative material accounting for crafting previews and completed items. */
public final class CraftingMaterialResolver {
    private CraftingMaterialResolver() {}

    /**
     * Use the actual crafting inputs, not an arbitrary matching recipe from NEI.
     * Fail closed if an ingredient is unknown or its returned container cannot
     * be resolved, so we never silently claim an incomplete composition.
     */
    public static List<OreDictMaterialStack> resolveIngredients(IInventory grid, int outputCount) {
        Map<OreDictMaterial, Long> totals = new LinkedHashMap<OreDictMaterial, Long>();
        boolean found = false;
        for (int slot = 0; slot < grid.getSizeInventory(); slot++) {
            ItemStack ingredient = grid.getStackInSlot(slot);
            if (ingredient == null || ingredient.getItem() == null) continue;
            found = true;
            ItemStack single = ingredient.copy();
            single.stackSize = 1;

            OreDictItemData data = OM.anydata(single);
            if (data == null || data.getAllMaterialStacks().isEmpty()) {
                
                return null;
            }
            // Keep each ingredient separate: a returned container must never
            // subtract material supplied by a different recipe ingredient.
            Map<OreDictMaterial, Long> consumed = new LinkedHashMap<OreDictMaterial, Long>();
            if (!accumulate(consumed, data, 1)) {
                return null;
            }

            if (single.getItem().hasContainerItem(single)) {
                ItemStack returned = single.getItem().getContainerItem(single);
                if (returned != null) {
                    OreDictItemData returnedData = OM.anydata(returned);
                    if (returnedData != null && !returnedData.getAllMaterialStacks().isEmpty()) {
                        // Some filled containers (notably GT6 Heavy Cream) describe
                        // their contents only; their glass is not in the input data.
                        // Subtract a returned container only when its entire
                        // composition is actually included in this ingredient.
                        boolean represented = true;
                        boolean overlaps = false;
                        for (OreDictMaterialStack part : returnedData.getAllMaterialStacks()) {
                            if (part == null || part.mMaterial == null) return null;
                            if (part.mMaterial == MT.Empty && part.mAmount == -1) continue;
                            if (part.mAmount < 0) return null;
                            Long amount = consumed.get(part.mMaterial);
                            if (amount != null && amount > 0) overlaps = true;
                            if (amount == null || amount < part.mAmount) represented = false;
                        }
                        if (represented) {
                            if (!accumulate(consumed, returnedData, -1)) return null;
                        } else if (!overlaps) {
                        } else {
                            return null;
                        }
                    }
                }
            }
            for (Map.Entry<OreDictMaterial, Long> part : consumed.entrySet()) {
                Long previous = totals.get(part.getKey());
                long before = previous == null ? 0 : previous;
                long amount = part.getValue();
                if (amount > Long.MAX_VALUE - before) return null;
                totals.put(part.getKey(), before + amount);
            }
        }
        if (!found || outputCount <= 0) return null;
        List<OreDictMaterialStack> result = new ArrayList<OreDictMaterialStack>();
        for (Map.Entry<OreDictMaterial, Long> entry : totals.entrySet()) {
            long amount = entry.getValue();
            if (amount < 0 || amount % outputCount != 0) {
                return null;
            }
            if (amount > 0) result.add(OM.stack(entry.getKey(), amount / outputCount));
        }
        return result;
    }

    private static boolean accumulate(Map<OreDictMaterial, Long> totals, OreDictItemData data, int sign) {
        for (OreDictMaterialStack component : data.getAllMaterialStacks()) {
            if (component == null || component.mMaterial == null) return false;
            // GT6 includes Empty:-1 as a nonphysical placeholder in some
            // material lists. It must not contribute to conserved mass.
            if (component.mMaterial == MT.Empty && component.mAmount == -1) continue;
            if (component.mAmount < 0) return false;
            Long old = totals.get(component.mMaterial);
            long previous = old == null ? 0 : old;
            long delta = sign * component.mAmount;
            long next = previous + delta;
            if ((delta > 0 && next < previous) || (delta < 0 && next > previous)) return false;
            totals.put(component.mMaterial, next);
        }
        return true;
    }
}
