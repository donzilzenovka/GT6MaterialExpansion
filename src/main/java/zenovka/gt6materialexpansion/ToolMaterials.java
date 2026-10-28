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
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe-derived GT6 material composition for crafting outputs and tools.
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
        NBTTagCompound existing = result.getTagCompound();
        // A previously established stack-specific composition always wins.
        if (existing != null && (existing.hasKey(RECYCLING_TAG) || existing.hasKey(ORIGINAL_TAG))) {
            return;
        }

        boolean tool = result.getItem() instanceof MultiItemTool;
        // For ordinary items, respect GT6's existing material association (e.g. sticks).
        // Tools retain their established recipe-derived original-material behaviour.
        if (!tool) {
            OreDictItemData outputData = OM.anydata(result);
            if (outputData != null && !outputData.getAllMaterialStacks().isEmpty()) {
                return;
            }
        }

        List<OreDictMaterialStack> components = CraftingMaterialResolver.resolveIngredients(event.craftMatrix, result.stackSize);
        if (components == null || components.isEmpty()) {
            return;
        }

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


    /**
     * Populate the live crafting result, without changing the recipe or GT6's
     * existing material associations. The same resolver handles pickup.
     * Both logical sides update their local preview; the server is authoritative.
     */
    @SubscribeEvent
    public void onCraftPreview(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Container container = event.player.openContainer;
        if (container == null || container.inventorySlots.isEmpty()) return;
        Object first = container.inventorySlots.get(0);
        if (!(first instanceof SlotCrafting)) return;
        IInventory grid;
        if (container instanceof ContainerWorkbench) {
            grid = ((ContainerWorkbench) container).craftMatrix;
        } else if (container instanceof ContainerPlayer) {
            grid = ((ContainerPlayer) container).craftMatrix;
        } else {
            return; // Do not guess at third-party container internals.
        }
        Slot outputSlot = (Slot) first;
        ItemStack result = outputSlot.getStack();
        if (result == null || result.getItem() == null) return;
        if (result.getItem() instanceof MultiItemTool) return; // Existing tool path unchanged.
        NBTTagCompound existing = result.getTagCompound();
        if (existing != null && (existing.hasKey(RECYCLING_TAG) || existing.hasKey(ORIGINAL_TAG))) return;
        OreDictItemData outputData = OM.anydata(result);
        if (outputData != null && !outputData.getAllMaterialStacks().isEmpty()) return;

        List<OreDictMaterialStack> components = CraftingMaterialResolver.resolveIngredients(grid, result.stackSize);
        if (components == null || components.isEmpty()) return;
        NBTTagCompound tag = existing == null ? new NBTTagCompound() : (NBTTagCompound) existing.copy();
        OreDictMaterialStack.saveList(RECYCLING_TAG, tag, components);
        result.setTagCompound(tag);
        if (!event.player.worldObj.isRemote) container.detectAndSendChanges();
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

}
