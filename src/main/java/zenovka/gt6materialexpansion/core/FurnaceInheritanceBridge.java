package zenovka.gt6materialexpansion.core;

import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictManager;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.util.OM;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;

/** Per-operation furnace output; never modifies FurnaceRecipes' shared template. */
public final class FurnaceInheritanceBridge {
    private static final String ORIGINAL = "zenovka.original.mats";
    private static final String RECYCLING = "gt.recycling.mats";

    private FurnaceInheritanceBridge() {}

    public static ItemStack result(FurnaceRecipes recipes, ItemStack input) {
        ItemStack template = recipes.getSmeltingResult(input);
        if (template == null) return null;
        ItemStack output = template.copy();
        if (input == null || input.stackSize <= 0 || output.stackSize != 1) return output;

        NBTTagCompound tag = output.getTagCompound();
        if (tag != null && tag.hasKey(ORIGINAL)) return output;
        OreDictItemData registered = OreDictManager.INSTANCE.getItemData(output);
        if (registered != null && registered.getAllMaterialStacks() != null
                && !registered.getAllMaterialStacks().isEmpty()) return output;

        OreDictItemData source = OM.anydata(input);
        if (source == null || source.getAllMaterialStacks() == null
                || source.getAllMaterialStacks().isEmpty()) return output;

        java.util.List<OreDictMaterialStack> parts = new java.util.ArrayList<OreDictMaterialStack>();
        for (OreDictMaterialStack part : source.getAllMaterialStacks()) {
            if (part == null || part.mMaterial == null || part.mAmount <= 0) return output;
            parts.add(OM.stack(part.mMaterial, part.mAmount));
        }
        if (parts.isEmpty()) return output;
        if (tag == null) tag = new NBTTagCompound();
        OreDictMaterialStack.saveList(ORIGINAL, tag, parts);
        OreDictMaterialStack.saveList(RECYCLING, tag, parts);
        output.setTagCompound(tag);
        return output;
    }
}
