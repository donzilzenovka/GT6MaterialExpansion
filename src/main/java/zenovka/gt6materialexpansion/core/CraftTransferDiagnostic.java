package zenovka.gt6materialexpansion.core;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Temporary, read-only output-slot transfer trace. */
public final class CraftTransferDiagnostic {
    private static int lines;
    private CraftTransferDiagnostic() {}

    public static synchronized void trace(Object owner, String phase, Object playerArg) {
        if (lines >= 160) return;
        lines++;
        EntityPlayer player = playerArg instanceof EntityPlayer ? (EntityPlayer) playerArg : null;
        ItemStack cursor = player == null ? null : player.inventory.getItemStack();
        ItemStack output = null;
        if (owner instanceof Slot) output = ((Slot) owner).getStack();
        if (owner instanceof Container) {
            Container container = (Container) owner;
            if (!container.inventorySlots.isEmpty()) {
                Object slot = container.inventorySlots.get(0);
                if (slot instanceof Slot) output = ((Slot) slot).getStack();
            }
        }
        System.out.println("[GT6MaterialExpansion/TransferDiagnostic] "
                + (player != null && player.worldObj.isRemote ? "CLIENT " : player != null ? "SERVER " : "UNKNOWN ")
                + phase + " output={" + describe(output) + "} cursor={" + describe(cursor) + "}"
                + " compatible=" + compatible(output, cursor));
    }

    private static String compatible(ItemStack a, ItemStack b) {
        if (a == null || b == null) return "n/a";
        return "item=" + (a.getItem() == b.getItem())
                + ",meta=" + (a.getItemDamage() == b.getItemDamage())
                + ",nbt=" + ItemStack.areItemStackTagsEqual(a, b);
    }

    private static String describe(ItemStack s) {
        if (s == null) return "null";
        return Item.itemRegistry.getNameForObject(s.getItem()) + ":" + s.getItemDamage()
                + " x" + s.stackSize + " tag=" + (s.getTagCompound() == null ? "null" : s.getTagCompound().toString());
    }
}
