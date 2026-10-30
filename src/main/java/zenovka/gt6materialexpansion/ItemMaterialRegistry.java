package zenovka.gt6materialexpansion;

import static zenovka.gt6materialexpansion.GT6MaterialExpansion.*;
import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.util.OM;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictManager;
import gregapi.oredict.configurations.OreDictConfigurationComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import cpw.mods.fml.common.FMLLog;
import net.minecraftforge.common.MinecraftForge;

/** Pack-aware item material associations and deliberate overrides. */
public final class ItemMaterialRegistry {
    private static final long U = CS.U;
    private ItemMaterialRegistry() {}
    public static void register() {
        // Deliberately conservative: only unannotated item stacks receive data.
        // Do not replace GT6's existing item material assignments.
        assignIfMissing("minecraft:dirt", 0, Soil, 9 * U);
        assignIfMissing("minecraft:dirt", 1, Soil, 9 * U);
        assignIfMissing("BiomesOPlenty:newBopDirt", 4, MT.STONES.Siltstone, 9 * U);
        assignIfMissing("BiomesOPlenty:newBopGrass", 0, Loam, 9 * U);
        assignIfMissing("BiomesOPlenty:mud", 0, Soil, 9 * U);
        // Ender IO Silicon (numeric item ID may be 4321 in this pack).
        // Registry identity is EnderIO:itemMaterial, metadata 0.
        // The GT6 silicon plate alternatives each contain 1 U Silicon.
        assignIfMissing("EnderIO:itemMaterial", 0, MT.Si, U);
        // Provisional Enderman head: seven units bone and two Ender Pearls (eyes).
        // Restrict this test to the base head metadata; do not affect other skull variants.
        assignEndermanHeadIfMissing();
        ProduceItemAssociations.register();
        FloraMaterials.register(PlantMatter);
        FungalMaterials.register(Mycelium);
        MiscMaterials.register(Soil, PlantMatter, Mycelium, OrganicMatter, Fibre, Wool,
            ProduceMaterials.get("Cactus"), ProduceMaterials.get("Reed"));
        registerSpecificDyes();
        BiologicalFibreMaterials.register(Keratin, Silk, Chitin, Cotton, Hemp, Flax, Fibre, Wool, stringIsCotton);
        // Vanilla torches are invariant, fuel-agnostic objects; no per-stack NBT.
        // Respect any composition another mod may have registered.
        assignIfMissing("minecraft:torch", 0, MT.Wood, U / 2);
        // GT6 Fruit Remains (MultiItemFood metadata 12101): 0.036 U per item.
        // Explicit override: the generic itemPlantRemains ore association may already
        // have supplied 0.250 U, so an assign-if-missing check is insufficient.
        Object fruitRemainsItem = Item.itemRegistry.getObject("gregtech:gt.multiitem.food");
        if (fruitRemainsItem instanceof Item) {
            ItemStack fruitRemains = new ItemStack((Item) fruitRemainsItem, 1, 12101);
            boolean changed = OreDictManager.INSTANCE.setItemData(
                fruitRemains, new OreDictItemData(PlantMatter, 36 * U / 1000));
            FMLLog.info("[GT6MaterialExpansion] Fruit Remains 0.036 U override accepted=%s", changed);
        }
        FoodMaterials.register(AppleFruit, Carrot, Egg); // Per-stack composition of crafted foods; no global overrides.
        MinecraftForge.EVENT_BUS.register(new MaterialAcquisitionHandler());
        // Bottled food fluids and ice cream lack item-material data in GT6.
        // Existing assignments take precedence; no recipe ingredient (including salt) is inferred.
        assignBottledFluidIfMissing("gregtech:gt.multiitem.bottles", 1101, HeavyCream, U);
        assignBottledFluidIfMissing("gregtech:gt.multiitem.bottles", 5604, CoconutCream, U);
        assignIfMissing("gregtech:gt.multiitem.food", 13000, HeavyCream, U);
        ToolMaterials.register(); // Experimental GT6 shovel-only composition probe.\n        // Additional item associations will follow after pack-specific verification.
        overrideMeatItem("minecraft:chicken", 0, ChickenRaw);
        overrideMeatItem("minecraft:cooked_chicken", 0, ChickenCooked);
        overrideMeatItem("minecraft:beef", 0, BeefRaw);
        overrideMeatItem("minecraft:cooked_beef", 0, BeefCooked);
        overrideMeatItem("minecraft:porkchop", 0, PorkRaw);
        overrideMeatItem("minecraft:cooked_porkchop", 0, PorkCooked);
        overrideMeatItem("gregtech:gt.multiitem.food", 1400, MuttonRaw);
        overrideMeatItem("gregtech:gt.multiitem.food", 1401, MuttonCooked);
        overrideMeatItem("gregtech:gt.multiitem.food", 1210, BeefRaw);
        overrideMeatItem("gregtech:gt.multiitem.food", 1211, BeefCooked);
        overrideMeatItem("minecraft:fish", 0, CodRaw);
        overrideMeatItem("minecraft:cooked_fished", 0, CodCooked);
        overrideMeatItem("minecraft:fish", 1, SalmonRaw);
        overrideMeatItem("minecraft:cooked_fished", 1, SalmonCooked);
    }
    /** Assign 1 U of colour only to actual dye items, not dye substitutes. */
    private static void registerSpecificDyes() {
        // Vanilla 1.7.10 dye metadata: 0 ink, 3 cocoa, 4 lapis, 15 bone meal.
        // Those four retain their existing physical material associations.
        int[] vanillaMetadata = {1, 2, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
        OreDictMaterial[] vanillaMaterials = {
            MT.Red, MT.Green, MT.Purple, MT.Cyan, MT.LightGray, MT.Gray,
            MT.Pink, MT.Lime, MT.Yellow, MT.LightBlue, MT.Magenta, MT.Orange
        };
        for (int i = 0; i < vanillaMetadata.length; i++)
            diagnoseDye("minecraft:dye", vanillaMetadata[i], vanillaMaterials[i]);

        // Et Futurum Requiem's separate dyes (white, blue, brown, black).
        diagnoseDye("etfuturum:dye", 0, MT.White);
        diagnoseDye("etfuturum:dye", 1, MT.Blue);
        diagnoseDye("etfuturum:dye", 2, MT.Brown);
        diagnoseDye("etfuturum:dye", 3, MT.Black);
    }

    /** Temporary beta diagnostic: compare GT6's direct and general material lookups. */
    private static void diagnoseDye(String id, int meta, OreDictMaterial material) {
        Object obj = Item.itemRegistry.getObject(id);
        if (!(obj instanceof Item)) {
            FMLLog.warning("[GT6MaterialExpansion][dye-diagnostic] missing item %s:%d", id, meta);
            return;
        }
        ItemStack stack = new ItemStack((Item) obj, 1, meta);
        OreDictItemData before = OreDictManager.INSTANCE.getItemData(stack);
        OreDictItemData beforeAny = OM.anydata(stack);
        FMLLog.info("[GT6MaterialExpansion][dye-diagnostic] %s:%d expected=%s before=%s anyBefore=%s",
            id, meta, String.valueOf(material), dyeData(before), dyeData(beforeAny));
        boolean accepted = false;
        if (before == null)
            accepted = OreDictManager.INSTANCE.addItemData(stack, new OreDictItemData(material, U));
        OreDictItemData after = OreDictManager.INSTANCE.getItemData(stack);
        OreDictItemData afterAny = OM.anydata(stack);
        FMLLog.info("[GT6MaterialExpansion][dye-diagnostic] %s:%d attempted=%s accepted=%s after=%s anyAfter=%s",
            id, meta, before == null, accepted, dyeData(after), dyeData(afterAny));
    }

    private static String dyeData(OreDictItemData data) {
        if (data == null) return "<null>";
        java.util.List<OreDictMaterialStack> parts = data.getAllMaterialStacks();
        if (parts == null || parts.isEmpty()) return "<empty>";
        StringBuilder result = new StringBuilder();
        for (OreDictMaterialStack part : parts) {
            if (result.length() > 0) result.append(", ");
            result.append(part == null ? "<null-part>" :
                String.valueOf(part.mMaterial) + "=" + part.mAmount);
        }
        return result.toString();
    }

    private static void assignEndermanHeadIfMissing() {
        Object registered = Item.itemRegistry.getObject("EnderIO:blockEndermanSkull");
        if (!(registered instanceof Item)) return;
        ItemStack stack = new ItemStack((Item) registered, 1, 0);
        if (OreDictManager.INSTANCE.getItemData(stack) != null) return;
        OreDictManager.INSTANCE.addItemData(stack, new OreDictItemData(
            OM.stack(MT.Bone, 7 * U), OM.stack(MT.EnderPearl, 2 * U)));
        FMLLog.info("[GT6MaterialExpansion] Registered provisional Enderman head: 7 U Bone + 2 U Ender Pearl");
    }
    private static void overrideMeatItem(String registryName, int metadata, OreDictMaterial material) {
        Object registered = Item.itemRegistry.getObject(registryName);
        if (!(registered instanceof Item)) {
            FMLLog.info("[GT6MaterialExpansion] Missing meat item %s:%d", registryName, metadata);
            return;
        }
        ItemStack stack = new ItemStack((Item)registered, 1, metadata);
        OreDictItemData before = OreDictManager.INSTANCE.getItemData(stack);
        if (before == null || before.mMaterial == null) {
            FMLLog.info("[GT6MaterialExpansion] No original meat composition for %s:%d; unchanged",
                registryName, metadata);
            return;
        }
        // Replace the primary meat only; retain GT6's original bone and other byproducts.
        OreDictItemData replacement = new OreDictItemData(material,
            before.mMaterial.mAmount, before.mByProducts);
        boolean accepted = OreDictManager.INSTANCE.setItemData(stack, replacement);
        FMLLog.info("[GT6MaterialExpansion] %s:%d meat association accepted=%s, byproducts=%d",
            registryName, metadata, accepted, before.mByProducts.length);
    }

    private static void assignIfMissing(String registryName, int metadata,
                                        OreDictMaterial material, long amount) {
        Object registered = Item.itemRegistry.getObject(registryName);
        if (!(registered instanceof Item) || material == null) return;
        ItemStack stack = new ItemStack((Item) registered, 1, metadata);
        if (OreDictManager.INSTANCE.getItemData(stack) == null) {
            OreDictManager.INSTANCE.addItemData(stack, new OreDictItemData(material, amount));
        }
    }
    /**
     * A filled fluid bottle represents its contents AND its returned glass.
     * Copy GT6's actual empty-bottle material quantity rather than guessing
     * an amount of Glass. Do not override existing GT6 item assignments.
     */
    private static void assignBottledFluidIfMissing(String registryName, int metadata,
                                                    OreDictMaterial fluidMaterial, long fluidAmount) {
        Object registered = Item.itemRegistry.getObject(registryName);
        if (!(registered instanceof Item) || fluidMaterial == null) return;
        ItemStack filled = new ItemStack((Item) registered, 1, metadata);
        if (OreDictManager.INSTANCE.getItemData(filled) != null) return;

        OreDictItemData emptyData = OM.anydata(new ItemStack(Items.glass_bottle, 1, 0));
        if (emptyData == null || emptyData.getAllMaterialStacks().isEmpty()) {
            FMLLog.warning("[GT6MaterialExpansion] No material data for empty glass bottle; %s:%d unchanged",
                registryName, metadata);
            return;
        }
        java.util.List<OreDictMaterialStack> bottle = emptyData.getAllMaterialStacks();
        OreDictItemData combined = new OreDictItemData(OM.stack(fluidMaterial, fluidAmount),
            bottle.toArray(new OreDictMaterialStack[bottle.size()]));
        OreDictManager.INSTANCE.addItemData(filled, combined);
        FMLLog.info("[GT6MaterialExpansion] Bottled fluid %s:%d includes %d empty-bottle material components",
            registryName, metadata, bottle.size());
    }

}
