package zenovka.gt6materialexpansion;

import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.util.OM;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Material accounting for an Ender IO Alloy Smelter operation.
 *
 * This class is deliberately independent of Ender IO's classes. A future
 * task-completion adapter must supply the ACTUAL quantities consumed by the
 * recipe, not the full input-slot contents.
 *
 * No recipe registrations or output stacks are mutated here.
 */
public final class AlloySmelterMaterials {
    private AlloySmelterMaterials() {}

    /**
     * Returns per-item composition for a single deterministic output.
     * Null means the operation cannot be accounted for safely.
     *
     * The caller must handle returned containers, secondary outputs and
     * existing output-slot material compatibility before using the result.
     */
    public static List<OreDictMaterialStack> calculate(
            ItemStack[] consumed, ItemStack output) {
        if (consumed == null || output == null || output.getItem() == null
                || output.stackSize <= 0) return null;
        Map<OreDictMaterial, Long> totals = new LinkedHashMap<OreDictMaterial, Long>();
        boolean any = false;
        for (ItemStack input : consumed) {
            if (input == null || input.stackSize <= 0) continue;
            any = true;
            ItemStack one = input.copy();
            one.stackSize = 1;
            List<OreDictMaterialStack> parts = MaterialDefaultResolver.resolve(one);
            if (parts == null || parts.isEmpty()) return null;
            for (OreDictMaterialStack part : parts) {
                if (part == null || part.mMaterial == null
                        || part.mMaterial == GT6MaterialExpansion.Matter
                        || part.mAmount < 0) return null;
                if (part.mMaterial == MT.Empty && part.mAmount == -1) continue;
                long quantity = part.mAmount;
                if (quantity != 0 && input.stackSize > Long.MAX_VALUE / quantity) return null;
                long delta = quantity * input.stackSize;
                Long previous = totals.get(part.mMaterial);
                long before = previous == null ? 0 : previous.longValue();
                if (Long.MAX_VALUE - before < delta) return null;
                totals.put(part.mMaterial, before + delta);
            }
        }
        if (!any || totals.isEmpty()) return null;
        List<OreDictMaterialStack> result = new ArrayList<OreDictMaterialStack>();
        for (Map.Entry<OreDictMaterial, Long> entry : totals.entrySet()) {
            if (entry.getValue() % output.stackSize != 0) return null;
            long amount = entry.getValue() / output.stackSize;
            if (amount > 0) result.add(OM.stack(entry.getKey(), amount));
        }
        return result.isEmpty() ? null : result;
    }
}
