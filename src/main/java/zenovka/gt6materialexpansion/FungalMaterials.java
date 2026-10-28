package zenovka.gt6materialexpansion;

import cpw.mods.fml.common.FMLLog;
import gregapi.data.CS;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictManager;
import gregapi.oredict.OreDictMaterial;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.HashSet;
import java.util.Set;

/** First-pass fungal material associations; existing GT6 data takes precedence. */
public final class FungalMaterials {
    private static final long U = CS.U;
    private FungalMaterials() {}

    public static void register(OreDictMaterial Mycelium) {
        Set<String> seen = new HashSet<String>();
        assignBlock("minecraft:brown_mushroom", 0, Mycelium, U / 4, seen);
        assignBlock("minecraft:red_mushroom", 0, Mycelium, U / 4, seen);
        // Mushroom blocks use metadata for cap, pores and stem textures.
        for (int meta = 0; meta < 16; meta++) {
            assignBlock("minecraft:brown_mushroom_block", meta, Mycelium, U, seen);
            assignBlock("minecraft:red_mushroom_block", meta, Mycelium, U, seen);
        }
        for (String oreName : OreDictionary.getOreNames()) {
            String name = oreName.toLowerCase(java.util.Locale.ROOT);
            boolean mushroom = name.startsWith("mushroom") || name.startsWith("cropmushroom")
                || name.startsWith("blockmushroom");
            if (!mushroom) continue;
            long amount = name.startsWith("block") ? U : U / 4;
            for (ItemStack stack : OreDictionary.getOres(oreName))
                assign(stack, Mycelium, amount, seen);
        }
        FMLLog.info("[GT6MaterialExpansion] Fungal scan assigned %d stacks", seen.size());
    }

    private static void assignBlock(String name, int meta, OreDictMaterial material,
                                    long amount, Set<String> seen) {
        Block block = Block.getBlockFromName(name);
        if (block != null) assign(new ItemStack(block, 1, meta), material, amount, seen);
    }

    private static void assign(ItemStack stack, OreDictMaterial material,
                               long amount, Set<String> seen) {
        if (stack == null || stack.getItem() == null
                || stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) return;
        String key = net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem())
            + ":" + stack.getItemDamage();
        if (seen.contains(key)) return;
        ItemStack single = stack.copy();
        single.stackSize = 1;
        if (OreDictManager.INSTANCE.getItemData(single) != null) return;
        OreDictManager.INSTANCE.addItemData(single, new OreDictItemData(material, amount));
        if (OreDictManager.INSTANCE.getItemData(single) != null) seen.add(key);
    }
}
