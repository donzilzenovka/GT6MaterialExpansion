package zenovka.gt6materialexpansion;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Remove only our material-provenance annotations from a temporary NEI
 * recipe-search stack. Never mutate the displayed or registered item.
 * Other NBT remains significant for modded recipe matching.
 */
public final class NEIRecipeLookup {
    private NEIRecipeLookup() {}

    public static ItemStack identity(ItemStack original) {
        if (original == null || !original.hasTagCompound()) return original;
        NBTTagCompound tag = original.getTagCompound();
        if (!tag.hasKey("gt.recycling.mats") && !tag.hasKey("zenovka.original.mats"))
            return original;
        ItemStack copy = original.copy();
        NBTTagCompound cleaned = copy.getTagCompound();
        cleaned.removeTag("gt.recycling.mats");
        cleaned.removeTag("zenovka.original.mats");
        if (cleaned.hasNoTags()) copy.setTagCompound(null);
        return copy;
    }
}
