package zenovka.gt6materialexpansion;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictManager;
import gregapi.data.CS;
import net.minecraft.item.Item;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.util.OM;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Experimental per-stack composition for crafted foods.
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
        FMLCommonHandler.instance().bus().register(new FoodMaterials());
    }

    private static void assignIfMissing(String id, OreDictMaterial material) {
        Object registered = Item.itemRegistry.getObject(id);
        if (!(registered instanceof Item)) return;
        ItemStack stack = new ItemStack((Item) registered, 1, 0);
        if (OreDictManager.INSTANCE.getItemData(stack) == null)
            OreDictManager.INSTANCE.addItemData(stack, new OreDictItemData(material, CS.U));
    }

    @SubscribeEvent
    public void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack output = event.crafting;
        if (output == null || !(output.getItem() instanceof ItemFood) || event.craftMatrix == null) return;
        NBTTagCompound existing = output.getTagCompound();
        if (existing != null && existing.hasKey(ORIGINAL_TAG)) return;

        List<OreDictMaterialStack> materials = derive(event.craftMatrix, output.stackSize);
        if (materials == null || materials.isEmpty()) return;

        NBTTagCompound tag = existing == null ? new NBTTagCompound() : existing;
        OreDictMaterialStack.saveList(ORIGINAL_TAG, tag, materials);
        OreDictMaterialStack.saveList(RECYCLING_TAG, tag, materials);
        output.setTagCompound(tag);
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
