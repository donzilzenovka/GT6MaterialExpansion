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

    public static void register(OreDictMaterial soil, OreDictMaterial plant,
                                OreDictMaterial mycelium, OreDictMaterial organic,
                                OreDictMaterial fibre, OreDictMaterial wool) {
        // Terrain: replace one ninth of the dirt baseline with its surface layer.
        put("minecraft:grass", 0, soil, 8 * U, plant, U);
        put("minecraft:dirt", 2, soil, 8 * U, plant, U); // podzol
        put("minecraft:farmland", 0, soil, 9 * U);
        put("minecraft:mycelium", 0, soil, 8 * U, mycelium, U);
        put("minecraft:cactus", 0, plant, U);
        // Nether Wart is treated as a fungal growth, not generic vegetation.
        put("minecraft:nether_wart", 0, mycelium, U / 4, MT.Ma, U / 8);
        put("minecraft:sponge", 0, organic, U);
        put("minecraft:sponge", 1, organic, U);
        put("minecraft:sandstone_stairs", 0, MT.Sand, 3 * U / 4);

        // Animal-derived substances only: never substitute Organic Matter for flora.
        put("minecraft:spider_eye", 0, organic, U / 4);
        put("minecraft:ghast_tear", 0, MT.Ma, U / 4);
        put("minecraft:dye", 0, organic, U / 4); // ink sac

        // Simple objects and multi-ingredient structures.
        for (int meta = 0; meta < 16; meta++)
            put("minecraft:carpet", meta, wool, U / 3);
        put("minecraft:lead", 0, fibre, U / 2);
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
        put("minecraft:reeds", 0, plant, U / 4, MT.Sugar, U / 4);
        put("minecraft:dye", 3, MT.Cocoa, U);
        put("minecraft:dye", 4, MT.Lapis, U);
        put("minecraft:dye", 15, MT.Bone, U);
        put("minecraft:experience_bottle", 0, MT.Glass, U / 4, MT.Ma, U / 4);
        // Potion effect metadata/NBT does not change its glass and water contents.
        put("minecraft:potion", 32767, MT.Glass, U / 4, MT.H2O, U);
        put("minecraft:fermented_spider_eye", 0, organic, U / 4,
            MT.Sugar, U / 4, MT.Biomass, U / 4);
        put("minecraft:item_frame", 0, MT.Leather, U, MT.Wood, 4 * U);
        supplement("minecraft:chest_minecart", MT.Fe, 5 * U);
        supplement("minecraft:furnace_minecart", MT.Fe, 5 * U);
        // Composite structures: provisional recipe-scale quantities.
        put("minecraft:bed", 0, MT.Wood, 3 * U, wool, 3 * U);
        put("minecraft:brewing_stand", 0, MT.Stone, 3 * U, MT.Blaze, U);
        put("minecraft:tnt_minecart", 0, MT.Fe, 5 * U,
            MT.Gunpowder, 4 * U, MT.Paper, U);
        put("minecraft:hopper_minecart", 0, MT.Fe, 10 * U);
        put("minecraft:command_block_minecart", 0, MT.Fe, 5 * U, MT.Bedrock, U);
        // Skull metadata: 0 skeleton, 1 wither, 2 zombie, 3 player, 4 creeper.
        put("minecraft:skull", 2, MT.MeatRotten, U, MT.Bone, U);
        put("minecraft:skull", 3, MT.MeatRaw, U, MT.Bone, U);
        put("minecraft:skull", 4, plant, U, MT.Gunpowder, U / 9);
        put("minecraft:dragon_egg", 0, MT.Bedrock, U);
        put("minecraft:command_block", 0, MT.Bedrock, U);
    }

    /** Preserve existing components; only supplement carts lacking iron. */
    private static void supplement(String id, OreDictMaterial material, long amount) {
        Object obj = Item.itemRegistry.getObject(id);
        if (!(obj instanceof Item)) return;
        ItemStack stack = new ItemStack((Item) obj, 1, 0);
        OreDictItemData before = OreDictManager.INSTANCE.getItemData(stack);
        if (before == null || before.mMaterial == null) return;
        java.util.List<OreDictMaterialStack> parts = before.getAllMaterialStacks();
        for (OreDictMaterialStack part : parts)
            if (part.mMaterial == material) return;
        OreDictMaterialStack[] extras = new OreDictMaterialStack[parts.size()];
        for (int i = 1; i < parts.size(); i++)
            extras[i - 1] = new OreDictMaterialStack(parts.get(i).mMaterial, parts.get(i).mAmount);
        extras[parts.size() - 1] = new OreDictMaterialStack(material, amount);
        boolean ok = OreDictManager.INSTANCE.setItemData(stack,
            new OreDictItemData(parts.get(0).mMaterial, parts.get(0).mAmount, extras));
        FMLLog.info("[GT6MaterialExpansion] %s added chassis=%s", id, ok);
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
