package zenovka.gt6materialexpansion;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.RM;
import gregapi.recipes.Recipe;
import gregapi.util.ST;
import gregapi.util.OM;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictManager;
import gregapi.oredict.configurations.OreDictConfigurationComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import java.util.Map;
import cpw.mods.fml.common.FMLLog;

/**
 * Initial GT6 6.17.06 material-data experiment.
 * No blocks, items, recipes, or ore dictionary entries are registered here.
 */
@Mod(modid = "gt6materialexpansion", name = "GT6 Material Expansion",
     version = "0.1.0", dependencies = "required-after:gregtech")
public final class GT6MaterialExpansion {
    private static final long U = CS.U;

    // IDs are in GT6's custom-material range; check for pack conflicts before release.
    public static OreDictMaterial Loam;
    public static OreDictMaterial Soil;
	
    public static OreDictMaterial ChickenRaw;
    public static OreDictMaterial ChickenCooked;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Loam = OreDictMaterial.createMaterial(32011, "Loam", "Loam");
        Soil = OreDictMaterial.createMaterial(32013, "Soil", "Soil");
        ChickenRaw = OreDictMaterial.createMaterial(32014, "ChickenRaw", "Raw Chicken");
        ChickenCooked = OreDictMaterial.createMaterial(32015, "ChickenCooked", "Cooked Chicken");
        // Test material: inherit raw meat density; no recipes or processing registered.
        ChickenRaw.setDensity(MT.MeatRaw.mGramPerCubicCentimeter);
        ChickenRaw.setLocal("Raw Chicken");
        ChickenRaw.setRGBa(255, 185, 193, 255);
        // GT6 food mortar recipes inspect the MEAT material tag, not merely ore names.
        ChickenRaw.put(TD.Properties.MEAT);
        // Shredder recycling follows the pulverisation target of each constituent.
        ChickenRaw.setPulver(ChickenRaw, U);
        // GT6 PrefixItem.run() registers only materials passing isGeneratingItem().
        // Force all four dust fractions before GT6's Init-phase registration pass.
        OP.dust.forceItemGeneration(ChickenRaw);
        OP.dustSmall.forceItemGeneration(ChickenRaw);
        OP.dustTiny.forceItemGeneration(ChickenRaw);
        OP.dustDiv72.forceItemGeneration(ChickenRaw);
        // Register equivalent GT6 material forms under the generic raw-meat name.
        ChickenRaw.addReRegistrations(MT.MeatRaw);

        // Preserve species identity while inheriting GT6's generic cooked-meat recipes.
        ChickenCooked.setDensity(MT.MeatRaw.mGramPerCubicCentimeter * 0.75D);
        ChickenCooked.setLocal("Cooked Chicken");
        ChickenCooked.setRGBa(190, 130, 83, 255);
        ChickenCooked.put(TD.Properties.MEAT);
        ChickenCooked.setPulver(ChickenCooked, U);
        OP.dust.forceItemGeneration(ChickenCooked);
        OP.dustSmall.forceItemGeneration(ChickenCooked);
        OP.dustTiny.forceItemGeneration(ChickenCooked);
        OP.dustDiv72.forceItemGeneration(ChickenCooked);
        ChickenCooked.addReRegistrations(MT.MeatCooked);

        // Soil uses GT6 constituent masses, never a hardcoded kilogram value.
        // Nine-part ratio: 6 Stone, 2 Biomass, 1 Water.
        Soil.setMoleculeConfiguration(new OreDictConfigurationComponent(9,
            new OreDictMaterialStack(MT.Stone, 6 * U),
            new OreDictMaterialStack(MT.Biomass, 2 * U),
            new OreDictMaterialStack(MT.H2O, U)));
        // GT6 already defines Biomass; do not register a duplicate.
        Loam.setMoleculeConfiguration(new OreDictConfigurationComponent(10,
            new OreDictMaterialStack(MT.STONES.Siltstone, 4 * U),
            new OreDictMaterialStack(MT.Clay, 2 * U),
            new OreDictMaterialStack(Soil, 4 * U)));
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        // GT6 item weight is based on density, not the molecular mMass statistic.
        // Cooking retains 75% of raw meat weight without changing components or recipes.
        MT.MeatCooked.setDensity(MT.MeatRaw.mGramPerCubicCentimeter * 0.75D);

        // Species-specific wood densities (g/cm^3). Keep generic Wood and Bark unchanged.
        // Real woods use approximate air-dry densities; magical woods use agreed estimates.
        MT.WOODS.Oak.setDensity(0.70D);
        MT.WOODS.Birch.setDensity(0.65D);
        MT.WOODS.Spruce.setDensity(0.45D);
        MT.WOODS.Mahogany.setDensity(0.60D);
        MT.WOODS.Maple.setDensity(0.70D);
        MT.WOODS.Balsa.setDensity(0.16D);
        MT.WOODS.Ebony.setDensity(1.05D);
        MT.WOODS.Greenheart.setDensity(1.00D);
        MT.WOODS.Ipe.setDensity(1.05D);
        // Approximate air-dry densities for other identifiable real-world species.
        MT.WOODS.Jungle.setDensity(0.65D);
        MT.WOODS.Acacia.setDensity(0.70D);
        MT.WOODS.DarkOak.setDensity(0.72D);
        MT.WOODS.Willow.setDensity(0.45D);
        MT.WOODS.BlueMahoe.setDensity(0.60D);
        MT.WOODS.Hazel.setDensity(0.65D);
        MT.WOODS.Cinnamon.setDensity(0.55D);
        MT.WOODS.Coconut.setDensity(0.65D);
        MT.WOODS.BlueSpruce.setDensity(0.43D);
        MT.WOODS.Aspen.setDensity(0.42D);
        MT.WOODS.DouglasFir.setDensity(0.53D);
        MT.WOODS.Sycamore.setDensity(0.59D);
        MT.WOODS.WhiteCedar.setDensity(0.35D);
        MT.WOODS.WhiteElm.setDensity(0.56D);
        MT.WOODS.Alder.setDensity(0.45D);
        MT.WOODS.Hawthorn.setDensity(0.75D);
        MT.WOODS.Rowan.setDensity(0.70D);
        MT.WOODS.Palm.setDensity(0.60D);
        MT.WOODS.Cypress.setDensity(0.51D);
        MT.WOODS.Fir.setDensity(0.43D);
        MT.WOODS.JapaneseMaple.setDensity(0.62D);
        MT.WOODS.RainbowEucalyptus.setDensity(0.70D);
        MT.WOODS.Redwood.setDensity(0.42D);
        MT.WOODS.Sakura.setDensity(0.58D);
        MT.WOODS.Baobab.setDensity(0.25D);
        MT.WOODS.Cherry.setDensity(0.58D);
        MT.WOODS.Chestnut.setDensity(0.56D);
        MT.WOODS.Citrus.setDensity(0.70D);
        MT.WOODS.Cocobolo.setDensity(1.05D);
        MT.WOODS.Kapok.setDensity(0.30D);
        MT.WOODS.Larch.setDensity(0.59D);
        MT.WOODS.Lime.setDensity(0.48D);
        MT.WOODS.Mahoe.setDensity(0.60D);
        MT.WOODS.Padauk.setDensity(0.75D);
        MT.WOODS.Papaya.setDensity(0.45D);
        MT.WOODS.Plum.setDensity(0.75D);
        MT.WOODS.Poplar.setDensity(0.42D);
        MT.WOODS.Sequoia.setDensity(0.40D);
        MT.WOODS.Teak.setDensity(0.65D);
        MT.WOODS.Walnut.setDensity(0.64D);
        MT.WOODS.Wenge.setDensity(0.87D);
        MT.WOODS.Zebrawood.setDensity(0.78D);
        MT.WOODS.Pine.setDensity(0.50D);
        MT.WOODS.Jacaranda.setDensity(0.55D);
        MT.WOODS.Mangrove.setDensity(0.75D);
        MT.WOODS.Apple.setDensity(0.70D);
        MT.WOODS.Ash.setDensity(0.67D);
        MT.WOODS.Beech.setDensity(0.72D);
        MT.WOODS.Box.setDensity(0.95D);
        MT.WOODS.Brazilwood.setDensity(0.85D);
        MT.WOODS.Butternut.setDensity(0.45D);
        MT.WOODS.Cedar.setDensity(0.38D);
        MT.WOODS.Elder.setDensity(0.50D);
        MT.WOODS.Elm.setDensity(0.57D);
        MT.WOODS.Eucalyptus.setDensity(0.75D);
        MT.WOODS.Fig.setDensity(0.55D);
        MT.WOODS.Gingko.setDensity(0.45D);
        MT.WOODS.Hemlock.setDensity(0.45D);
        MT.WOODS.Hickory.setDensity(0.78D);
        MT.WOODS.Holly.setDensity(0.72D);
        MT.WOODS.Hornbeam.setDensity(0.78D);
        MT.WOODS.Iroko.setDensity(0.66D);
        MT.WOODS.Locust.setDensity(0.77D);
        MT.WOODS.Logwood.setDensity(0.90D);
        MT.WOODS.Maclura.setDensity(0.85D);
        MT.WOODS.Olive.setDensity(0.90D);
        MT.WOODS.Pear.setDensity(0.70D);
        MT.WOODS.PinkIvory.setDensity(0.95D);
        MT.WOODS.Purpleheart.setDensity(0.86D);
        MT.WOODS.Rosewood.setDensity(0.85D);
        MT.WOODS.Sweetgum.setDensity(0.55D);
        MT.WOODS.Syzgium.setDensity(0.70D);
        MT.WOODS.Whitebeam.setDensity(0.72D);
        MT.WOODS.Yew.setDensity(0.67D);
        MT.Skyroot.setDensity(0.55D);
        MT.Greatwood.setDensity(0.80D);
        MT.Silverwood.setDensity(0.50D);
        MT.Livingwood.setDensity(0.90D);
        MT.Dreamwood.setDensity(0.40D);

        // Deliberately conservative: only unannotated item stacks receive data.
        // Do not replace GT6's existing item material assignments.
        assignIfMissing("minecraft:dirt", 0, Soil, 9 * U);
        assignIfMissing("minecraft:dirt", 1, Soil, 9 * U);
        assignIfMissing("BiomesOPlenty:newBopDirt", 4, MT.STONES.Siltstone, 9 * U);
        assignIfMissing("BiomesOPlenty:newBopGrass", 0, Loam, 9 * U);
        assignIfMissing("BiomesOPlenty:mud", 0, Soil, 9 * U);
        // Additional item associations will follow after pack-specific verification.
        testChickenOverride();
        diagnoseChickenProcessing();
        // Mortar recipes may be populated by GT6 after postInit.
        // Keep the early diagnostics, but defer any recipe mutation.
    }

    /** Diagnostic only: use GT6's public API; do not bypass protected associations. */
    private static void testChickenOverride() {
        Object registered = Item.itemRegistry.getObject("minecraft:chicken");
        if (!(registered instanceof Item)) {
            FMLLog.info("[GT6MaterialExpansion] Chicken override test: vanilla item missing");
            return;
        }
        ItemStack chicken = new ItemStack((Item) registered, 1, 0);
        OreDictItemData before = OreDictManager.INSTANCE.getItemData(chicken);
        FMLLog.info("[GT6MaterialExpansion] Chicken override: before=%s",
            describeMaterial(before));
        // Preserve the original GT6 amount, if present, to isolate association behaviour.
        long amount = before != null && before.mMaterial != null
            ? before.mMaterial.mAmount : U;
        boolean accepted = OreDictManager.INSTANCE.setItemData(
            chicken, new OreDictItemData(ChickenRaw, amount,
                new OreDictMaterialStack(MT.Bone, U / 9)));
        OreDictItemData after = OreDictManager.INSTANCE.getItemData(chicken);
        FMLLog.info("[GT6MaterialExpansion] Chicken override: accepted=%s, after=%s",
            accepted, describeMaterial(after));
        if (!accepted) {
            FMLLog.info("[GT6MaterialExpansion] Chicken override refused by GT6; no forced mutation attempted");
        }
    }

    @EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        FMLLog.info("[GT6MaterialExpansion] Chicken mortar: checking after server startup");
        overrideChickenMortarOutput();
        overrideChickenSmeltingOutput();
    }

    /** Change only the already-registered vanilla chicken mortar output. */
    private static void overrideChickenMortarOutput() {
        Object registered = Item.itemRegistry.getObject("minecraft:chicken");
        if (!(registered instanceof Item)) return;
        ItemStack chicken = new ItemStack((Item)registered, 1, 0);
        ItemStack generic = OM.dust(MT.MeatRaw, 2 * U);
        ItemStack specific = OM.dust(ChickenRaw, 2 * U);
        if (generic == null || specific == null) {
            FMLLog.info("[GT6MaterialExpansion] Chicken mortar: output unavailable; unchanged");
            return;
        }
        FMLLog.info("[GT6MaterialExpansion] Chicken mortar diagnostic: total recipes=%d", RM.Mortar.mRecipeList.size());
        int inputMatches = 0;
        int outputMatches = 0;
        for (Recipe recipe : RM.Mortar.mRecipeList) {
            if (recipe.mInputs == null) continue;
            for (ItemStack input : recipe.mInputs) {
                if (input == null || input.getItem() != chicken.getItem()
                        || input.getItemDamage() != chicken.getItemDamage()) continue;
                inputMatches++;
                boolean expectedOutput = recipe.mOutputs != null
                    && recipe.mOutputs.length == 1 && recipe.mOutputs[0] != null
                    && ST.equal(recipe.mOutputs[0], generic, true)
                    && recipe.mOutputs[0].stackSize == generic.stackSize;
                if (expectedOutput) outputMatches++;
                StringBuilder outputs = new StringBuilder();
                if (recipe.mOutputs != null) for (ItemStack output : recipe.mOutputs) {
                    if (outputs.length() > 0) outputs.append(", ");
                    outputs.append(describeStack(output));
                }
                FMLLog.info("[GT6MaterialExpansion] Chicken mortar diagnostic: input=%s inputs=%d outputs=%d outputStacks=[%s] expectedOutput=%s enabled=%s",
                    describeStack(input), recipe.mInputs.length,
                    recipe.mOutputs == null ? -1 : recipe.mOutputs.length,
                    outputs.toString(), expectedOutput, recipe.mEnabled);
            }
        }
        FMLLog.info("[GT6MaterialExpansion] Chicken mortar diagnostic: chickenInputs=%d expectedOutputs=%d",
            inputMatches, outputMatches);
        Recipe candidate = null;
        int matches = 0;
        for (Recipe recipe : RM.Mortar.mRecipeList) {
            if (recipe.mInputs.length != 1 || recipe.mOutputs.length != 1) continue;
            if (!ST.equal(recipe.mInputs[0], chicken, true)) continue;
            if (recipe.mInputs[0].stackSize != 1) continue;
            if (!ST.equal(recipe.mOutputs[0], generic, true)) continue;
            if (recipe.mOutputs[0].stackSize != generic.stackSize) continue;
            candidate = recipe;
            matches++;
        }
        if (matches != 1) {
            FMLLog.info("[GT6MaterialExpansion] Chicken mortar: expected one vanilla recipe, found %d; unchanged", matches);
            return;
        }
        // GT6 Recipe explicitly permits output-array changes without reindexing inputs.
        candidate.mOutputs[0] = specific;
        FMLLog.info("[GT6MaterialExpansion] Chicken mortar: replaced generic output with %s", describeStack(specific));
    }

    /**
     * Keep GT6's ore-dictionary generated furnace recipe and replace only its
     * result. Avoid registering a competing furnace recipe or changing input data.
     */
    private static void overrideChickenSmeltingOutput() {
        ItemStack raw = OM.dust(ChickenRaw, U);
        ItemStack generic = OM.dust(MT.MeatCooked, U);
        ItemStack cooked = OM.dust(ChickenCooked, U);
        if (raw == null || generic == null || cooked == null) {
            FMLLog.info("[GT6MaterialExpansion] Chicken smelting: dust form unavailable; unchanged");
            return;
        }
        int matches = 0;
        int alreadySpecific = 0;
        ItemStack candidateInput = null;
        for (Object rawEntry : FurnaceRecipes.smelting().getSmeltingList().entrySet()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) rawEntry;
            if (!(entry.getKey() instanceof ItemStack)
                    || !(entry.getValue() instanceof ItemStack)) continue;
            ItemStack input = (ItemStack) entry.getKey();
            ItemStack output = (ItemStack) entry.getValue();
            if (input == null || input.getItem() != raw.getItem()
                    || input.getItemDamage() != raw.getItemDamage()) continue;
            if (output != null && ST.equal(output, cooked, true)
                    && output.stackSize == cooked.stackSize) {
                alreadySpecific++;
            } else if (output != null && ST.equal(output, generic, true)
                    && output.stackSize == generic.stackSize) {
                candidateInput = input;
                matches++;
            } else {
                FMLLog.info("[GT6MaterialExpansion] Chicken smelting: unexpected existing output=%s",
                    describeStack(output));
            }
        }
        if (matches == 0 && alreadySpecific == 1) {
            FMLLog.info("[GT6MaterialExpansion] Chicken smelting: already species-specific");
            return;
        }
        if (matches != 1 || alreadySpecific != 0) {
            FMLLog.info("[GT6MaterialExpansion] Chicken smelting: expected one generic recipe, found %d; already-specific=%d; unchanged",
                matches, alreadySpecific);
            return;
        }
        FurnaceRecipes.smelting().getSmeltingList().put(candidateInput, cooked);
        FMLLog.info("[GT6MaterialExpansion] Chicken smelting: replaced generic output with %s",
            describeStack(cooked));
    }

    /** Read-only diagnostics for generated dust and shredder pulverisation targets. */
    private static void diagnoseChickenProcessing() {
        ItemStack dust = OM.dust(ChickenRaw, 2 * U);
        FMLLog.info("[GT6MaterialExpansion] Chicken processing: dust(ChickenRaw, 2U)=%s",
            describeStack(dust));
        FMLLog.info("[GT6MaterialExpansion] Chicken processing: pulver target=%s, amount=%d",
            String.valueOf(ChickenRaw.mTargetPulver.mMaterial),
            ChickenRaw.mTargetPulver.mAmount);
        FMLLog.info("[GT6MaterialExpansion] Chicken processing: bone pulver target=%s, amount=%d",
            String.valueOf(MT.Bone.mTargetPulver.mMaterial),
            MT.Bone.mTargetPulver.mAmount);
        Object item = Item.itemRegistry.getObject("minecraft:chicken");
        if (item instanceof Item) {
            OreDictItemData data = OreDictManager.INSTANCE.getItemData(
                new ItemStack((Item)item, 1, 0));
            if (data != null) {
                for (OreDictMaterialStack component : data.getAllMaterialStacks()) {
                    FMLLog.info("[GT6MaterialExpansion] Chicken processing: component=%s amount=%d pulver=%s ratio=%d",
                        String.valueOf(component.mMaterial), component.mAmount,
                        String.valueOf(component.mMaterial.mTargetPulver.mMaterial),
                        component.mMaterial.mTargetPulver.mAmount);
                }
            }
        }
    }

    private static String describeStack(ItemStack stack) {
        if (stack == null) return "<NULL>";
        return String.valueOf(stack.getItem()) + " x" + stack.stackSize
            + " meta=" + stack.getItemDamage();
    }

    private static String describeMaterial(OreDictItemData data) {
        if (data == null || data.mMaterial == null) return "<none>";
        return String.valueOf(data.mMaterial.mMaterial) + " (" + data.mMaterial.mAmount + " units)";
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
}
