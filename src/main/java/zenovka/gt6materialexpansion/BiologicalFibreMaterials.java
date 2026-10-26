package zenovka.gt6materialexpansion;

import cpw.mods.fml.common.FMLLog;
import gregapi.data.CS;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictManager;
import gregapi.oredict.OreDictMaterial;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Composition-only annotations; do not replace GT6 data or change recipes. */
public final class BiologicalFibreMaterials {
    private static final long U = CS.U;
    private BiologicalFibreMaterials() {}

    public static void register(OreDictMaterial keratin, OreDictMaterial silk,
            OreDictMaterial chitin, OreDictMaterial cotton, OreDictMaterial hemp,
            OreDictMaterial flax, OreDictMaterial fibre, OreDictMaterial wool,
            boolean stringIsCotton) {
        Set<String> assigned = new HashSet<String>();
        // Explicit vanilla associations, including all wool colours.
        for (int meta = 0; meta < 16; meta++)
            assignBlock("minecraft:wool", meta, wool, 4 * U, assigned);
        assignItem("minecraft:feather", 0, keratin, U / 16, assigned);
        assignItem("minecraft:string", 0, stringIsCotton ? cotton : silk, U / 16, assigned);
        assignBlock("minecraft:web", 0, silk, U / 2, assigned);

        for (String ore : OreDictionary.getOreNames()) {
            String name = ore.toLowerCase(Locale.ROOT);
            OreDictMaterial material = null;
            if (name.equals("itemfeather") || name.equals("feather")) material = keratin;
            else if (name.equals("itemwool") || name.equals("blockwool")) material = wool;
            else if (name.equals("itemstring") || name.equals("string"))
                material = stringIsCotton ? cotton : silk;
            else if (name.equals("blockweb") || name.equals("itemspidersilk")
                    || name.equals("itemspiderweb") || name.equals("itemsilk")) material = silk;
            else if (name.equals("itemchitin") || name.equals("chitin")) material = chitin;
            else if (name.equals("itemcotton") || name.equals("cropcotton")
                    || name.equals("fibrecotton") || name.equals("fibercotton")) material = cotton;
            else if (name.equals("itemhempfiber") || name.equals("itemhempfibre")
                    || name.equals("fiberhemp") || name.equals("fibrehemp")) material = hemp;
            else if (name.equals("itemflaxfiber") || name.equals("itemflaxfibre")
                    || name.equals("fiberflax") || name.equals("fibreflax")) material = flax;
            else if (name.equals("itemfiber") || name.equals("itemfibre")) material = fibre;
            if (material == null) continue;
            long amount = name.equals("blockwool") ? 4 * U
                : name.equals("blockweb") ? U / 2 : U / 16;
            for (ItemStack stack : OreDictionary.getOres(ore))
                assign(stack, material, amount, assigned);
        }
        FMLLog.info("[GT6MaterialExpansion] Biological fibre scan assigned %d stacks", assigned.size());
    }

    private static void assignItem(String name, int meta, OreDictMaterial material,
                                   long amount, Set<String> assigned) {
        Object item = Item.itemRegistry.getObject(name);
        if (item instanceof Item) assign(new ItemStack((Item)item, 1, meta), material, amount, assigned);
    }

    private static void assignBlock(String name, int meta, OreDictMaterial material,
                                    long amount, Set<String> assigned) {
        Block block = Block.getBlockFromName(name);
        if (block != null) assign(new ItemStack(block, 1, meta), material, amount, assigned);
    }

    private static void assign(ItemStack stack, OreDictMaterial material,
                               long amount, Set<String> assigned) {
        if (stack == null || stack.getItem() == null || material == null
                || stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) return;
        ItemStack single = stack.copy();
        single.stackSize = 1;
        String key = Item.itemRegistry.getNameForObject(single.getItem()) + ":" + single.getItemDamage();
        if (assigned.contains(key) || OreDictManager.INSTANCE.getItemData(single) != null) return;
        OreDictManager.INSTANCE.addItemData(single, new OreDictItemData(material, amount));
        if (OreDictManager.INSTANCE.getItemData(single) != null) assigned.add(key);
    }
}
