package zenovka.gt6materialexpansion;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.oredict.OreDictItemData;
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
 * Experimental recipe-derived GT6 tool composition.
 * Only newly crafted stacks are modified; no inventory scanning or global
 * item/metadata registrations. Missing input material data causes a safe skip.
 *
 * The recycling tag is active GT6 processing data, not passive documentation.
 */
public final class ToolMaterials {
    private static final String RECYCLING_TAG = "gt.recycling.mats";

    private ToolMaterials() {}

    public static void register() {
        FMLCommonHandler.instance().bus().register(new ToolMaterials());
    }

    @SubscribeEvent
    public void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack result = event.crafting;
        if (result == null || !(result.getItem() instanceof MultiItemTool)) return;
        if (event.craftMatrix == null) return;
        NBTTagCompound existing = result.getTagCompound();
        if (existing != null && existing.hasKey(RECYCLING_TAG)) return;

        List<OreDictMaterialStack> components = resolveIngredients(event.craftMatrix, result.stackSize);
        if (components == null || components.isEmpty()) return;

        NBTTagCompound tag = existing == null ? new NBTTagCompound() : existing;
        OreDictMaterialStack.saveList(RECYCLING_TAG, tag, components);
        result.setTagCompound(tag);
    }

    /**
     * Use the actual crafting inputs, not an arbitrary matching recipe from NEI.
     * Fail closed if an ingredient is unknown or its returned container cannot
     * be resolved, so we never silently claim an incomplete composition.
     */
    private static List<OreDictMaterialStack> resolveIngredients(IInventory grid, int outputCount) {
        Map<OreDictMaterial, Long> totals = new LinkedHashMap<OreDictMaterial, Long>();
        boolean found = false;
        for (int slot = 0; slot < grid.getSizeInventory(); slot++) {
            ItemStack ingredient = grid.getStackInSlot(slot);
            if (ingredient == null || ingredient.getItem() == null) continue;
            found = true;
            ItemStack single = ingredient.copy();
            single.stackSize = 1;

            OreDictItemData data = OM.anydata(single);
            if (data == null || data.getAllMaterialStacks().isEmpty()) return null;
            if (!accumulate(totals, data, 1)) return null;

            if (single.getItem().hasContainerItem(single)) {
                ItemStack returned = single.getItem().getContainerItem(single);
                if (returned != null) {
                    OreDictItemData returnedData = OM.anydata(returned);
                    if (returnedData == null || !accumulate(totals, returnedData, -1)) return null;
                }
            }
        }
        if (!found || outputCount <= 0) return null;
        List<OreDictMaterialStack> result = new ArrayList<OreDictMaterialStack>();
        for (Map.Entry<OreDictMaterial, Long> entry : totals.entrySet()) {
            long amount = entry.getValue();
            if (amount < 0 || amount % outputCount != 0) return null;
            if (amount > 0) result.add(OM.stack(entry.getKey(), amount / outputCount));
        }
        return result;
    }

    private static boolean accumulate(Map<OreDictMaterial, Long> totals, OreDictItemData data, int sign) {
        for (OreDictMaterialStack component : data.getAllMaterialStacks()) {
            if (component == null || component.mMaterial == null || component.mAmount < 0) return false;
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
