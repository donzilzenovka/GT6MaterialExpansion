package zenovka.gt6materialexpansion;

import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictManager;
import gregapi.data.CS;
import net.minecraft.item.Item;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.util.OM;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Experimental gap-filling per-stack composition for crafted items.
 * Uses the actual crafting matrix, not a guessed canonical recipe.
 * Missing ingredient data or ambiguous returned containers fail closed.
 * Does not overwrite global GT6 material registrations.
 */
public final class FoodMaterials {
    private static final String ORIGINAL_TAG = "zenovka.original.mats";
    private static final String RECYCLING_TAG = "gt.recycling.mats";

    private FoodMaterials() {}

    public static void register(OreDictMaterial apple, OreDictMaterial carrot, OreDictMaterial egg) {
        assignIfMissing("minecraft:apple", apple);
        assignIfMissing("minecraft:carrot", carrot);
        assignIfMissing("minecraft:egg", egg);
    }

    private static void assignIfMissing(String id, OreDictMaterial material) {
        Object registered = Item.itemRegistry.getObject(id);
        if (!(registered instanceof Item)) return;
        ItemStack stack = new ItemStack((Item) registered, 1, 0);
        if (OreDictManager.INSTANCE.getItemData(stack) == null)
            OreDictManager.INSTANCE.addItemData(stack, new OreDictItemData(material, CS.U));
    }

    /**
     * Called before the crafting result is placed in its preview slot.
     * Never mutate the recipe template supplied by CraftingManager.
     */
    public static ItemStack preview(ItemStack template, IInventory matrix) {
        if (template == null || matrix == null) return template;
        ItemStack output = template.copy();
        NBTTagCompound existing = output.getTagCompound();
        if (existing != null && existing.hasKey(ORIGINAL_TAG)) return output;
        OreDictItemData canonical = OreDictManager.INSTANCE.getItemData(output);
        if (canonical != null && canonical.getAllMaterialStacks() != null
                && !canonical.getAllMaterialStacks().isEmpty()) return output;
        List<OreDictMaterialStack> materials = derive(matrix, output.stackSize);
        if (materials == null || materials.isEmpty()) return output;
        NBTTagCompound tag = existing == null ? new NBTTagCompound() : existing;
        OreDictMaterialStack.saveList(ORIGINAL_TAG, tag, materials);
        OreDictMaterialStack.saveList(RECYCLING_TAG, tag, materials);
        output.setTagCompound(tag);
        return output;
    }

    private static List<OreDictMaterialStack> derive(IInventory matrix, int count) {
        if (count <= 0) return null;
        Map<OreDictMaterial, Long> totals = new LinkedHashMap<OreDictMaterial, Long>();
        boolean any = false;
        for (int i = 0; i < matrix.getSizeInventory(); i++) {
            ItemStack ingredient = matrix.getStackInSlot(i);
            if (ingredient == null || ingredient.getItem() == null) continue;
            any = true;
            ItemStack one = ingredient.copy();
            one.stackSize = 1;
            OreDictItemData data = OM.anydata(one);
            if (data == null || data.getAllMaterialStacks().isEmpty() || !add(totals, data, 1)) return null;
            if (one.getItem().hasContainerItem(one)) {
                ItemStack container = one.getItem().getContainerItem(one);
                if (container != null) {
                    OreDictItemData returned = OM.anydata(container);
                    if (returned == null || !add(totals, returned, -1)) return null;
                }
            }
        }
        if (!any) return null;
        List<OreDictMaterialStack> result = new ArrayList<OreDictMaterialStack>();
        for (Map.Entry<OreDictMaterial, Long> entry : totals.entrySet()) {
            long amount = entry.getValue();
            if (amount < 0 || amount % count != 0) return null;
            if (amount > 0) result.add(OM.stack(entry.getKey(), amount / count));
        }
        return result;
    }

    private static boolean add(Map<OreDictMaterial, Long> totals, OreDictItemData data, int sign) {
        for (OreDictMaterialStack part : data.getAllMaterialStacks()) {
            if (part == null || part.mMaterial == null || part.mAmount < 0) return false;
            Long prior = totals.get(part.mMaterial);
            long previous = prior == null ? 0 : prior;
            long delta = sign * part.mAmount;
            long next = previous + delta;
            if ((delta > 0 && next < previous) || (delta < 0 && next > previous)) return false;
            totals.put(part.mMaterial, next);
        }
        return true;
    }
}
