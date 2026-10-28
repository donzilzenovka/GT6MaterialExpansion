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

/**
 * Fixed, conservative vanilla material associations.
 * Existing GT6/mod registrations always win. No NBT, recipes or ore names changed.
 * Amounts not established by GT6 are provisional composition estimates.
 */
public final class MiscMaterials {
    private static final long U = CS.U;
    private MiscMaterials() {}

    public static void register(OreDictMaterial Soil, OreDictMaterial PlantMatter,
                                OreDictMaterial Mycelium, OreDictMaterial OrganicMatter,
                                OreDictMaterial Fibre, OreDictMaterial Wool,
                                OreDictMaterial Cactus, OreDictMaterial Reed) {
        // Terrain: replace one ninth of the dirt baseline with its surface layer.
        put("minecraft:grass", 0, Soil, 8 * U, PlantMatter, U);
        put("minecraft:dirt", 2, Soil, 8 * U, PlantMatter, U); // podzol
        put("minecraft:farmland", 0, Soil, 9 * U);
        put("minecraft:Mycelium", 0, Soil, 8 * U, Mycelium, U);
        put("minecraft:cactus", 0, Cactus, U);
        // Nether Wart is treated as a fungal growth, not generic vegetation.
        put("minecraft:nether_wart", 0, Mycelium, U / 4, MT.Ma, U / 8);
        put("minecraft:sponge", 0, OrganicMatter, U);
        put("minecraft:sponge", 1, OrganicMatter, U);
        put("minecraft:sandstone_stairs", 0, MT.Sand, 3 * U / 4);

        // Animal-derived substances only: never substitute OrganicMatter Matter for flora.
        put("minecraft:spider_eye", 0, OrganicMatter, U / 4);
        put("minecraft:ghast_tear", 0, MT.Ma, U / 4);
        put("minecraft:dye", 0, OrganicMatter, U); // ink sac

        // Simple objects and multi-ingredient structures.
        for (int meta = 0; meta < 16; meta++)
            put("minecraft:carpet", meta, Wool, U / 3);
        put("minecraft:lead", 0, Fibre, U / 2);
        put("minecraft:name_tag", 0, MT.Paper, U / 4);
        put("minecraft:tnt", 0, MT.Gunpowder, 4 * U, MT.Paper, U);
        put("minecraft:daylight_detector", 0, MT.Wood, 3 * U,
            MT.Glass, 3 * U, MT.NetherQuartz, 3 * U);
        put("minecraft:activator_rail", 0, MT.Fe, U / 4,
            MT.WoodTreated, U / 4, MT.Redstone, U / 4);
        put("minecraft:unpowered_repeater", 0, MT.Stone, U,
            MT.Redstone, U / 2, MT.Wood, U / 2);
        put("minecraft:powered_repeater", 0, MT.Stone, U,
            MT.Redstone, U / 2, MT.Wood, U / 2);
        put("minecraft:unpowered_comparator", 0, MT.Stone, U,
            MT.Redstone, U / 2, MT.NetherQuartz, U / 4);
        put("minecraft:powered_comparator", 0, MT.Stone, U,
            MT.Redstone, U / 2, MT.NetherQuartz, U / 4);

        // Identical iron container regardless of fluid contents.
        put("minecraft:water_bucket", 0, MT.Fe, 3 * U, MT.H2O, U);
        put("minecraft:milk_bucket", 0, MT.Fe, 3 * U, MT.Milk, U);
        put("minecraft:lava_bucket", 0, MT.Fe, 3 * U, MT.Lava, U);

        // Preserve specific GT6 materials rather than inventing dye or slime elements.
        put("minecraft:slime_ball", 0, MT.Glue, U / 9);
        put("minecraft:magma_cream", 0, MT.Glue, U / 9, MT.Blaze, U / 9);
        put("minecraft:reeds", 0, Reed, U / 4, MT.Sugar, U / 4);
        put("minecraft:dye", 3, MT.Cocoa, U);
        put("minecraft:dye", 4, MT.Lapis, U);
        put("minecraft:dye", 15, MT.Bone, U);
        put("minecraft:experience_bottle", 0, MT.Glass, U / 4, MT.Ma, U / 4);
        // Potion effect metadata/NBT does not change its glass and water contents.
        put("minecraft:potion", 32767, MT.Glass, U / 4, MT.H2O, U);
        put("minecraft:fermented_spider_eye", 0, OrganicMatter, U / 4,
            MT.Sugar, U / 4, MT.Biomass, U / 4);
        put("minecraft:item_frame", 0, MT.Leather, U, MT.Wood, 4 * U);
        putOverride("minecraft:chest_minecart", 0, MT.Fe, 5 * U, MT.Wood, 8 * U);
        putOverride("minecraft:furnace_minecart", 0, MT.Fe, 5 * U, MT.Stone, 8 * U);
        // Composite structures: provisional recipe-scale quantities.
        put("minecraft:bed", 0, MT.Wood, 3 * U, Wool, 3 * U);
        put("minecraft:brewing_stand", 0, MT.Stone, 3 * U, MT.Blaze, U);
        put("minecraft:tnt_minecart", 0, MT.Fe, 5 * U,
            MT.Gunpowder, 4 * U, MT.Paper, U);
        put("minecraft:hopper_minecart", 0, MT.Fe, 10 * U, MT.Wood, 8 * U);
        put("minecraft:command_block_minecart", 0, MT.Fe, 5 * U, MT.Bedrock, U);
        // Skull metadata: 0 skeleton, 1 wither, 2 zombie, 3 player, 4 creeper.
        put("minecraft:skull", 2, MT.MeatRotten, U, MT.Bone, U);
        put("minecraft:skull", 3, MT.MeatRaw, U, MT.Bone, U);
        put("minecraft:skull", 4, PlantMatter, U, MT.Gunpowder, U / 9);
        put("minecraft:dragon_egg", 0, MT.Bedrock, U);
        put("minecraft:command_block", 0, MT.Bedrock, U);
    }

    /** Explicit replacement for composite carts with incomplete existing GT6 data. */
    private static void putOverride(String id, int meta, OreDictMaterial primary, long amount,
                                    Object... extras) {
        Item item = (Item) Item.itemRegistry.getObject(id);
        if (item == null) return;
        OreDictMaterialStack[] additional = new OreDictMaterialStack[extras.length / 2];
        for (int i = 0; i < additional.length; i++)
            additional[i] = new OreDictMaterialStack(
                (OreDictMaterial) extras[2 * i], (Long) extras[2 * i + 1]);
        ItemStack stack = new ItemStack(item, 1, meta);
        logComposition(id, "before", OreDictManager.INSTANCE.getItemData(stack));
        boolean updated = OreDictManager.INSTANCE.setItemData(
            stack, new OreDictItemData(primary, amount, additional));
        FMLLog.info("[GT6MaterialExpansion] Misc override %s:%d assigned=%s", id, meta, updated);
        logComposition(id, "after", OreDictManager.INSTANCE.getItemData(stack));
    }

    /** Log the actual material lookup, not just the registration result. */
    public static void diagnoseMinecarts() {
        for (String id : new String[] {"minecraft:chest_minecart", "minecraft:furnace_minecart"}) {
            Object obj = Item.itemRegistry.getObject(id);
            if (!(obj instanceof Item)) {
                FMLLog.info("[GT6MaterialExpansion] %s not found", id);
                continue;
            }
            logComposition(id, "serverStarted",
                OreDictManager.INSTANCE.getItemData(new ItemStack((Item) obj, 1, 0)));
        }
    }

    private static void logComposition(String id, String phase, OreDictItemData data) {
        if (data == null) {
            FMLLog.info("[GT6MaterialExpansion] %s %s composition=<none>", id, phase);
            return;
        }
        for (OreDictMaterialStack part : data.getAllMaterialStacks())
            FMLLog.info("[GT6MaterialExpansion] %s %s material=%s amount=%d",
                id, phase, String.valueOf(part.mMaterial), part.mAmount);
    }

    private static void put(String id, int meta, OreDictMaterial primary, long amount,
                            Object... extras) {
        Item item = (Item) Item.itemRegistry.getObject(id);
        if (item == null) {
            Block block = Block.getBlockFromName(id);
            if (block != null) item = Item.getItemFromBlock(block);
        }
        if (item == null || primary == null) return;
        ItemStack stack = new ItemStack(item, 1, meta);
        if (OreDictManager.INSTANCE.getItemData(stack) != null) return;
        OreDictMaterialStack[] additional = new OreDictMaterialStack[extras.length / 2];
        for (int i = 0; i < additional.length; i++)
            additional[i] = new OreDictMaterialStack(
                (OreDictMaterial) extras[2 * i], (Long) extras[2 * i + 1]);
        boolean added = OreDictManager.INSTANCE.addItemData(
            stack, new OreDictItemData(primary, amount, additional));
        FMLLog.info("[GT6MaterialExpansion] Misc %s:%d assigned=%s", id, meta, added);
    }
}
