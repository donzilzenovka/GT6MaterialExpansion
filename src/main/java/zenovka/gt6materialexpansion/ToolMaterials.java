package zenovka.gt6materialexpansion;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.data.MT;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.util.OM;
import gregapi.util.UT;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import cpw.mods.fml.common.FMLLog;
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
    private static final String ORIGINAL_TAG = "zenovka.original.mats";

    private ToolMaterials() {}

    public static void register() {
        ToolMaterials handler = new ToolMaterials();
        FMLCommonHandler.instance().bus().register(handler);
        MinecraftForge.EVENT_BUS.register(handler);
    }

    @SubscribeEvent
    public void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack result = event.crafting;
        if (result == null || event.craftMatrix == null) return;
        boolean debugCake = "minecraft:cake".equals(Item.itemRegistry.getNameForObject(result.getItem()));
        if (debugCake) FMLLog.info("[GT6MaterialExpansion/Crafting] Cake crafted; checking inheritance");
        NBTTagCompound existing = result.getTagCompound();
        // A previously established stack-specific composition always wins.
        if (existing != null && (existing.hasKey(RECYCLING_TAG) || existing.hasKey(ORIGINAL_TAG))) {
            if (debugCake) FMLLog.info("[GT6MaterialExpansion/Crafting] Cake skipped: existing stack composition tag");
            return;
        }

        boolean tool = result.getItem() instanceof MultiItemTool;
        // For ordinary items, respect GT6's existing material association (e.g. sticks).
        // Tools retain their established recipe-derived original-material behaviour.
        if (!tool) {
            OreDictItemData outputData = OM.anydata(result);
            if (outputData != null && !outputData.getAllMaterialStacks().isEmpty()) {
                if (debugCake) FMLLog.info("[GT6MaterialExpansion/Crafting] Cake skipped: output already has GT6 material data");
                return;
            }
        }

        List<OreDictMaterialStack> components = resolveIngredients(event.craftMatrix, result.stackSize, debugCake);
        if (components == null || components.isEmpty()) {
            if (debugCake) FMLLog.info("[GT6MaterialExpansion/Crafting] Cake skipped: ingredient resolution failed");
            return;
        }
        if (debugCake) FMLLog.info("[GT6MaterialExpansion/Crafting] Cake inherited %d material components", components.size());

        NBTTagCompound tag = existing == null ? new NBTTagCompound() : existing;
        if (tool) {
            OreDictMaterialStack.saveList(ORIGINAL_TAG, tag, components);
            result.setTagCompound(tag);
            synchronize(result);
        } else {
            OreDictMaterialStack.saveList(RECYCLING_TAG, tag, components);
            result.setTagCompound(tag);
        }
    }


    /** Refresh player-held tools on the server; no global item registry or coremod. */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.worldObj.isRemote) return;
        for (int slot = 0; slot < event.player.inventory.getSizeInventory(); slot++) {
            synchronize(event.player.inventory.getStackInSlot(slot));
        }
    }

    /** Ensure the displayed composition is current when the tooltip is requested. */
    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        synchronize(event.itemStack);
    }

    private static void synchronize(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof MultiItemTool)) return;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(ORIGINAL_TAG)) return;
        List<OreDictMaterialStack> original = OreDictMaterialStack.loadList(ORIGINAL_TAG, tag);
        if (original.isEmpty()) return;
        long max = MultiItemTool.getToolMaxDamage(stack);
        if (max <= 0) return;
        long remaining = Math.max(0L, max - MultiItemTool.getToolDamage(stack));
        OreDictMaterial head = MultiItemTool.getPrimaryMaterial(stack);
        OreDictMaterial shaft = MultiItemTool.getSecondaryMaterial(stack);
        // Only change unambiguously identified head material. If both parts use
        // the same material, aggregated recipe composition cannot separate them.
        if (head == null || head == MT.NULL || head == shaft) return;
        List<OreDictMaterialStack> adjusted = new ArrayList<OreDictMaterialStack>();
        for (OreDictMaterialStack component : original) {
            if (component == null || component.mMaterial == null || component.mAmount < 0) return;
            long amount = component.mMaterial == head
                ? UT.Code.units(remaining, max, component.mAmount, false)
                : component.mAmount;
            if (amount > 0) adjusted.add(OM.stack(component.mMaterial, amount));
        }
        List<OreDictMaterialStack> current = OreDictMaterialStack.loadList(RECYCLING_TAG, tag);
        if (sameMaterials(current, adjusted)) return;
        OreDictMaterialStack.saveList(RECYCLING_TAG, tag, adjusted);
        stack.setTagCompound(tag);
    }

    private static boolean sameMaterials(List<OreDictMaterialStack> left, List<OreDictMaterialStack> right) {
        if (left.size() != right.size()) return false;
        for (int i = 0; i < left.size(); i++) {
            OreDictMaterialStack a = left.get(i), b = right.get(i);
            if (a.mMaterial != b.mMaterial || a.mAmount != b.mAmount) return false;
        }
        return true;
    }

    /**
     * Use the actual crafting inputs, not an arbitrary matching recipe from NEI.
     * Fail closed if an ingredient is unknown or its returned container cannot
     * be resolved, so we never silently claim an incomplete composition.
     */
    private static List<OreDictMaterialStack> resolveIngredients(IInventory grid, int outputCount, boolean debugCake) {
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
                if (debugCake) FMLLog.info("[GT6MaterialExpansion/Crafting] Input slot %d has no readable materials: %s", slot, single.getDisplayName());
                return null;
            }
            if (debugCake) FMLLog.info("[GT6MaterialExpansion/Crafting] Input slot %d: %s, components=%d", slot, single.getDisplayName(), data.getAllMaterialStacks().size());
            // Keep each ingredient separate: a returned container must never
            // subtract material supplied by a different recipe ingredient.
            Map<OreDictMaterial, Long> consumed = new LinkedHashMap<OreDictMaterial, Long>();
            if (!accumulate(consumed, data, 1)) return null;

            if (single.getItem().hasContainerItem(single)) {
                ItemStack returned = single.getItem().getContainerItem(single);
                if (returned != null) {
                    OreDictItemData returnedData = OM.anydata(returned);
                    if (debugCake) FMLLog.info("[GT6MaterialExpansion/Crafting] Returned container: %s, material data=%s", returned.getDisplayName(), returnedData == null ? "missing" : "present");
                    if (returnedData != null && !returnedData.getAllMaterialStacks().isEmpty()) {
                        // Some filled containers (notably GT6 Heavy Cream) describe
                        // their contents only; their glass is not in the input data.
                        // Subtract a returned container only when its entire
                        // composition is actually included in this ingredient.
                        boolean represented = true;
                        boolean overlaps = false;
                        for (OreDictMaterialStack part : returnedData.getAllMaterialStacks()) {
                            if (part == null || part.mMaterial == null || part.mAmount < 0) return null;
                            Long amount = consumed.get(part.mMaterial);
                            if (amount != null && amount > 0) overlaps = true;
                            if (amount == null || amount < part.mAmount) represented = false;
                        }
                        if (represented) {
                            if (!accumulate(consumed, returnedData, -1)) return null;
                            if (debugCake) FMLLog.info("[GT6MaterialExpansion/Crafting] Container material subtracted from ingredient");
                        } else if (!overlaps) {
                            if (debugCake) FMLLog.info("[GT6MaterialExpansion/Crafting] Container material not represented in ingredient; no subtraction");
                        } else {
                            if (debugCake) FMLLog.info("[GT6MaterialExpansion/Crafting] Container partly represented; skipping ambiguous recipe");
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
