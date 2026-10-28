package zenovka.gt6materialexpansion;

import cpw.mods.fml.common.FMLLog;
import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictManager;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.HashSet;
import java.util.Set;

/**
 * Conservative first pass: annotate unassigned flora only.
 * Does not register ore names or alter GT6 biomass recipes.
 */
public final class FloraMaterials {
    private FloraMaterials() {}
    private static final long U = CS.U;
    private static final long FRESH_GRASS = U / 4;
    // GT6 dryer: 20 mB water removed per grass; PlantMatter is 50 kg/U.
    private static final long DRY_GRASS = Math.round(U * 0.2496D);
    private static final Set<String> seen = new HashSet<String>();

    public static void register(OreDictMaterial plantMatter) {
        seen.clear();
        // Vanilla metadata is explicit: ore registrations vary between packs.
        for (int meta = 0; meta < 6; meta++) {
            OreDictMaterial wood;
            switch (meta) {
                case 0: wood = MT.WOODS.Oak; break;
                case 1: wood = MT.WOODS.Spruce; break;
                case 2: wood = MT.WOODS.Birch; break;
                case 3: wood = MT.WOODS.Jungle; break;
                case 4: wood = MT.WOODS.Acacia; break;
                default: wood = MT.WOODS.DarkOak; break;
            }
            assign(new ItemStack(Block.getBlockFromName("minecraft:sapling"), 1, meta),
                plantMatter, wood, "vanilla sapling");
        }
        for (int meta = 0; meta < 2; meta++)
            assignBlock("minecraft:yellow_flower", meta, plantMatter, null);
        for (int meta = 0; meta < 9; meta++)
            assignBlock("minecraft:red_flower", meta, plantMatter, null);
        for (int meta = 0; meta < 3; meta++)
            assignBlock("minecraft:tallgrass", meta, plantMatter, null);
        // Sunflower is double_plant metadata 0; annotate before generic flora.
        // Retain its quarter-unit plant body and add 0.1 U sunflower oil.
        Block sunflower = Block.getBlockFromName("minecraft:double_plant");
        if (sunflower != null) {
            ItemStack stack = new ItemStack(sunflower, 1, 0);
            if (OreDictManager.INSTANCE.getItemData(stack) == null) {
                OreDictManager.INSTANCE.addItemData(stack,
                    new OreDictItemData(plantMatter, U / 4,
                        new OreDictMaterialStack(MT.SunflowerOil, U / 10)));
                seen.add("minecraft:double_plant:0");
            }
        }
        for (int meta = 1; meta < 6; meta++)
            assignBlock("minecraft:double_plant", meta, plantMatter, null);
        assignBlock("minecraft:deadbush", 0, plantMatter, null);
        for (int meta = 0; meta < 4; meta++)
            assignBlock("minecraft:leaves", meta, plantMatter, null, U / 2);
        for (int meta = 0; meta < 2; meta++)
            assignBlock("minecraft:leaves2", meta, plantMatter, null, U / 2);
        assignBlock("minecraft:vine", 0, plantMatter, null);
        assignBlock("minecraft:waterlily", 0, plantMatter, null);

        for (String oreName : OreDictionary.getOreNames()) {
            boolean sapling = oreName.equals("treeSapling") || oreName.equals("sapling")
                || oreName.startsWith("treeSapling") || oreName.startsWith("sapling");
            boolean soft = oreName.equals("flower") || oreName.startsWith("flower")
                || oreName.equals("itemGrass") || oreName.equals("itemGrassTall")
                || oreName.equals("itemPlantRemains");
            boolean leaves = oreName.equals("treeLeaves") || oreName.startsWith("treeLeaves")
                || oreName.equals("leaf") || oreName.equals("leaves");
            boolean bale = oreName.equals("baleGrass") || oreName.equals("baleGrassDry")
                || oreName.equals("baleGrassMoldy") || oreName.equals("baleGrassRotten");
            boolean grass = oreName.equals("itemGrassDry") || oreName.equals("itemGrassMoldy")
                || oreName.equals("itemGrassRotten");
            boolean vine = oreName.equals("vine") || oreName.equals("itemVine")
                || oreName.equals("lilyPad") || oreName.equals("waterlily");
            if (!sapling && !soft && !leaves && !bale && !grass && !vine) continue;
            for (ItemStack stack : OreDictionary.getOres(oreName)) {
                if (stack == null || stack.getItem() == null) continue;
                // Wildcard registrations can represent mixed metadata. Never write wildcard data.
                if (stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) continue;
                OreDictMaterial wood = sapling ? woodFor(oreName, stack) : null;
                long amount = leaves ? U / 2 : FRESH_GRASS;
                if (oreName.equals("itemGrassDry")) amount = DRY_GRASS;
                if (bale) amount = 9 * (oreName.equals("baleGrassDry") ? DRY_GRASS : FRESH_GRASS);
                assign(stack, plantMatter, wood, oreName, amount);
            }
        }
        FMLLog.info("[GT6MaterialExpansion] Flora composition scan complete; assigned %d stacks", seen.size());
    }

    private static void assignBlock(String name, int meta, OreDictMaterial plant, OreDictMaterial wood) {
        Block block = Block.getBlockFromName(name);
        if (block != null) assign(new ItemStack(block, 1, meta), plant, wood, name);
    }

    private static void assignBlock(String name, int meta, OreDictMaterial plant, OreDictMaterial wood, long amount) {
        Block block = Block.getBlockFromName(name);
        if (block != null) assign(new ItemStack(block, 1, meta), plant, wood, name, amount);
    }

    private static OreDictMaterial woodFor(String oreName, ItemStack stack) {
        // Conservative species resolution; unresolved modded saplings use generic Wood.
        String token = (oreName + " " + stack.getUnlocalizedName()).toLowerCase(java.util.Locale.ROOT);
        if (token.contains("darkoak") || token.contains("dark_oak")) return MT.WOODS.DarkOak;
        if (token.contains("spruce")) return MT.WOODS.Spruce;
        if (token.contains("birch")) return MT.WOODS.Birch;
        if (token.contains("acacia")) return MT.WOODS.Acacia;
        if (token.contains("jungle")) return MT.WOODS.Jungle;
        if (token.contains("mahogany")) return MT.WOODS.Mahogany;
        if (token.contains("willow")) return MT.WOODS.Willow;
        if (token.contains("redwood")) return MT.WOODS.Redwood;
        if (token.contains("cherry")) return MT.WOODS.Cherry;
        if (token.contains("maple")) return MT.WOODS.Maple;
        if (token.contains("oak")) return MT.WOODS.Oak;
        return MT.Wood;
    }

    private static void assign(ItemStack stack, OreDictMaterial plant, OreDictMaterial wood, String source) {
        assign(stack, plant, wood, source, U / 4);
    }

    private static void assign(ItemStack stack, OreDictMaterial plant, OreDictMaterial wood, String source, long amount) {
        if (stack == null || stack.getItem() == null) return;
        if (stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) return;
        Item item = stack.getItem();
        String key = Item.itemRegistry.getNameForObject(item) + ":" + stack.getItemDamage();
        if (seen.contains(key)) return;
        ItemStack single = stack.copy();
        single.stackSize = 1;
        if (OreDictManager.INSTANCE.getItemData(single) != null) return;
        // A stick is U/2 in GT6. Saplings contain that amount of wood.
        OreDictItemData data = wood == null
            ? new OreDictItemData(plant, amount)
            : new OreDictItemData(plant, amount, new OreDictMaterialStack(wood, U / 2));
        OreDictManager.INSTANCE.addItemData(single, data);
        if (OreDictManager.INSTANCE.getItemData(single) != null) {
            seen.add(key);
            FMLLog.info("[GT6MaterialExpansion] Flora %s (%s) wood=%s", key, source,
                wood == null ? "none" : wood.mNameInternal);
        }
    }
}
