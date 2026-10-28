package zenovka.gt6materialexpansion;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.util.OM;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Stack-local default composition. Never registers global GT6 item data.
 * Only annotate a missing composition; never replace recipe inheritance.
 */
public final class MaterialAcquisitionHandler {
    private static final String ORIGINAL = "zenovka.original.mats";
    private static final String RECYCLING = "gt.recycling.mats";

    @SubscribeEvent
    public void onPickup(EntityItemPickupEvent event) {
        if (event.entityPlayer == null || event.entityPlayer.worldObj.isRemote
                || event.item == null) return;
        annotate(event.item.getEntityItem());
    }

    /** Safe to call from future container and inventory insertion adapters. */
    public static boolean annotate(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && (tag.hasKey(ORIGINAL) || tag.hasKey(RECYCLING))) return false;
        OreDictItemData data = OM.anydata(stack);
        if (data != null && data.getAllMaterialStacks() != null
                && !data.getAllMaterialStacks().isEmpty()) return false;

        List<OreDictMaterialStack> fallback = defaultMaterials(stack);
        if (fallback == null || fallback.isEmpty()) return false;
        NBTTagCompound result = tag == null ? new NBTTagCompound() : (NBTTagCompound) tag.copy();
        OreDictMaterialStack.saveList(ORIGINAL, result, fallback);
        OreDictMaterialStack.saveList(RECYCLING, result, fallback);
        stack.setTagCompound(result);
        return true;
    }

    private static List<OreDictMaterialStack> defaultMaterials(ItemStack stack) {
        // Conservative first probe: GT6 bread is produced from two 0.5 U dough.
        // Avoid arbitrary first-recipe selection until ambiguity handling is built.
        if (stack.getItem() != Items.bread || stack.getItemDamage() != 0) return null;
        List<OreDictMaterialStack> result = new ArrayList<OreDictMaterialStack>(1);
        result.add(OM.stack(MT.Wheat, CS.U));
        return result;
    }
}
