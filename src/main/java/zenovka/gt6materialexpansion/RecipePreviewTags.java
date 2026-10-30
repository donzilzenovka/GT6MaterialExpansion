package zenovka.gt6materialexpansion;

import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.util.OM;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.List;

/** Shared preview-only material tagging. Never mutates registered recipe outputs. */
public final class RecipePreviewTags {
    private RecipePreviewTags() {}

    public static ItemStack copyWithMaterials(ItemStack output, List<OreDictMaterialStack> materials) {
        if (output == null) return null;
        ItemStack copy = output.copy();
        if (materials == null || materials.isEmpty()) return copy;
        OreDictItemData existing = OM.anydata(output);
        if (existing != null && existing.getAllMaterialStacks() != null
                && !existing.getAllMaterialStacks().isEmpty()) return copy;
        NBTTagCompound tag = copy.getTagCompound();
        if (tag == null) tag = new NBTTagCompound();
        if (tag.hasKey("gt.recycling.mats") || tag.hasKey("zenovka.original.mats")) return copy;
        OreDictMaterialStack.saveList("gt.recycling.mats", tag, materials);
        copy.setTagCompound(tag);
        return copy;
    }
}
