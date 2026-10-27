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
import cpw.mods.fml.common.FMLLog;
import net.minecraftforge.common.config.Configuration;

/**
 * Initial GT6 6.17.06 material-data experiment.
 * No blocks, items, recipes, or ore dictionary entries are registered here.
 */
@Mod(modid = "gt6materialexpansion", name = "GT6 Material Expansion",
     version = "0.1.0", dependencies = "required-after:gregtech")
public final class GT6MaterialExpansion {
    private static final long U = CS.U;
    public static boolean stringIsCotton = false;

    // IDs are in GT6's custom-material range; check for pack conflicts before release.
    public static OreDictMaterial Loam;
    public static OreDictMaterial Soil;
    public static OreDictMaterial PlantMatter;
    public static OreDictMaterial OrganicMatter;
    public static OreDictMaterial Mycelium;
    public static OreDictMaterial AppleFruit, Carrot, Egg;
    public static OreDictMaterial Keratin, Silk, Chitin, Cotton, Hemp, Flax, Fibre, Wool;
	
    public static OreDictMaterial ChickenRaw;
    public static OreDictMaterial ChickenCooked;
    public static OreDictMaterial BeefRaw, BeefCooked;
    public static OreDictMaterial PorkRaw, PorkCooked;
    public static OreDictMaterial MuttonRaw, MuttonCooked;
    public static OreDictMaterial CodRaw, CodCooked, SalmonRaw, SalmonCooked;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        try {
            config.load();
            stringIsCotton = config.getBoolean("stringIsCotton", "materials", false,
                "Use Cotton for generic Minecraft string instead of Silk. Does not change recipes or cobwebs.");
        } finally {
            if (config.hasChanged()) config.save();
        }
        Loam = OreDictMaterial.createMaterial(32011, "Loam", "Loam");
        Soil = OreDictMaterial.createMaterial(32013, "Soil", "Soil");
        PlantMatter = OreDictMaterial.createMaterial(32026, "PlantMatter", "Plant Matter");
        PlantMatter.setDensity(0.45D);
        PlantMatter.setRGBa(90, 145, 55, 255);
        OrganicMatter = OreDictMaterial.createMaterial(32036, "OrganicMatter", "Organic Matter");
        // Hydrated, undifferentiated animal tissue; provisional bulk density.
        OrganicMatter.setDensity(1.05D);
        OrganicMatter.setRGBa(170, 125, 105, 255);
        Mycelium = OreDictMaterial.createMaterial(32027, "Mycelium", "Mycelium");
        // Provisional bulk density; revisit against GT6 mushroom processing.
        Mycelium.setDensity(0.25D);
        AppleFruit = OreDictMaterial.createMaterial(32037, "AppleFruit", "Apple").setDensity(0.85D);
        AppleFruit.setRGBa(190, 55, 45, 255);
        Carrot = OreDictMaterial.createMaterial(32038, "Carrot", "Carrot").setDensity(0.95D);
        Carrot.setRGBa(235, 125, 35, 255);
        Egg = OreDictMaterial.createMaterial(32039, "Egg", "Egg").setDensity(1.03D);
        Egg.setRGBa(235, 220, 185, 255);
        // Composition-only biological materials; densities are provisional.
        Keratin = OreDictMaterial.createMaterial(32028, "Keratin", "Keratin").setDensity(1.30D);
        Silk = OreDictMaterial.createMaterial(32029, "Silk", "Silk").setDensity(1.30D);
        Chitin = OreDictMaterial.createMaterial(32030, "Chitin", "Chitin").setDensity(1.40D);
        Cotton = OreDictMaterial.createMaterial(32031, "Cotton", "Cotton").setDensity(1.50D);
        Hemp = OreDictMaterial.createMaterial(32032, "Hemp", "Hemp").setDensity(1.48D);
        Flax = OreDictMaterial.createMaterial(32033, "Flax", "Flax").setDensity(1.50D);
        Fibre = OreDictMaterial.createMaterial(32034, "Fibre", "Fibre").setDensity(1.40D);
        Wool = OreDictMaterial.createMaterial(32035, "Wool", "Wool").setDensity(1.30D);
        ChickenRaw = OreDictMaterial.createMaterial(32014, "ChickenRaw", "Raw Chicken");
        ChickenCooked = OreDictMaterial.createMaterial(32015, "ChickenCooked", "Cooked Chicken");
        BeefRaw = OreDictMaterial.createMaterial(32016, "BeefRaw", "Raw Beef");
        BeefCooked = OreDictMaterial.createMaterial(32017, "BeefCooked", "Cooked Beef");
        PorkRaw = OreDictMaterial.createMaterial(32018, "PorkRaw", "Raw Pork");
        PorkCooked = OreDictMaterial.createMaterial(32019, "PorkCooked", "Cooked Pork");
        MuttonRaw = OreDictMaterial.createMaterial(32020, "MuttonRaw", "Raw Mutton");
        MuttonCooked = OreDictMaterial.createMaterial(32021, "MuttonCooked", "Cooked Mutton");

        CodRaw = OreDictMaterial.createMaterial(32022, "CodRaw", "Raw Cod");
        CodCooked = OreDictMaterial.createMaterial(32023, "CodCooked", "Cooked Cod");
        SalmonRaw = OreDictMaterial.createMaterial(32024, "SalmonRaw", "Raw Salmon");
        SalmonCooked = OreDictMaterial.createMaterial(32025, "SalmonCooked", "Cooked Salmon");

        configureFish(CodRaw, CodCooked, "Cod", 192, 174, 137, 181, 138, 87);
        configureFish(SalmonRaw, SalmonCooked, "Salmon", 250, 142, 122, 205, 108, 82);

        configureMeat(ChickenRaw, ChickenCooked, "Chicken", 255, 185, 193, 190, 130, 83);
        configureMeat(BeefRaw, BeefCooked, "Beef", 170, 55, 65, 118, 65, 49);
        configureMeat(PorkRaw, PorkCooked, "Pork", 239, 155, 160, 186, 111, 81);
        configureMeat(MuttonRaw, MuttonCooked, "Mutton", 177, 77, 87, 137, 81, 57);
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
        FloraMaterials.register(PlantMatter);
        FungalMaterials.register(Mycelium);
        MiscMaterials.register(Soil, PlantMatter, Mycelium, OrganicMatter, Fibre, Wool);
        BiologicalFibreMaterials.register(Keratin, Silk, Chitin, Cotton, Hemp, Flax, Fibre, Wool, stringIsCotton);
        // Vanilla torches are invariant, fuel-agnostic objects; no per-stack NBT.
        // Respect any composition another mod may have registered.
        assignIfMissing("minecraft:torch", 0, MT.Wood, U / 2);
        FoodMaterials.register(AppleFruit, Carrot, Egg); // Per-stack composition of crafted foods; no global overrides.
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

    private static void configureMeat(OreDictMaterial raw, OreDictMaterial cooked,
            String name, int rr, int rg, int rb, int cr, int cg, int cb) {
        raw.setDensity(MT.MeatRaw.mGramPerCubicCentimeter);
        raw.setLocal("Raw " + name);
        raw.setRGBa(rr, rg, rb, 255);
        cooked.setDensity(MT.MeatRaw.mGramPerCubicCentimeter * 0.75D);
        cooked.setLocal("Cooked " + name);
        cooked.setRGBa(cr, cg, cb, 255);
        for (OreDictMaterial material : new OreDictMaterial[] {raw, cooked}) {
            material.put(TD.Properties.MEAT, TD.Properties.FOOD,
                TD.Processing.MORTAR, TD.ItemGenerator.INGOTS,
                TD.Processing.MELTING, TD.Processing.EXTRUDER,
                TD.Processing.EXTRUDER_SIMPLE);
            material.setPulver(material, U);
            OP.dust.forceItemGeneration(material);
            OP.dustSmall.forceItemGeneration(material);
            OP.dustTiny.forceItemGeneration(material);
            OP.dustDiv72.forceItemGeneration(material);
        }
        raw.setSmelting(cooked, U);
        raw.setForging(cooked, U);
    }

    private static void configureFish(OreDictMaterial raw, OreDictMaterial cooked,
            String name, int rr, int rg, int rb, int cr, int cg, int cb) {
        raw.setDensity(MT.FishRaw.mGramPerCubicCentimeter);
        raw.setLocal("Raw " + name);
        raw.setRGBa(rr, rg, rb, 255);
        cooked.setDensity(MT.FishRaw.mGramPerCubicCentimeter * 0.75D);
        cooked.setLocal("Cooked " + name);
        cooked.setRGBa(cr, cg, cb, 255);
        for (OreDictMaterial material : new OreDictMaterial[] {raw, cooked}) {
            material.put(TD.Properties.MEAT, TD.Properties.FOOD,
                TD.Processing.MORTAR, TD.ItemGenerator.INGOTS,
                TD.Processing.MELTING, TD.Processing.EXTRUDER,
                TD.Processing.EXTRUDER_SIMPLE);
            material.setPulver(material, U);
            OP.dust.forceItemGeneration(material);
            OP.dustSmall.forceItemGeneration(material);
            OP.dustTiny.forceItemGeneration(material);
            OP.dustDiv72.forceItemGeneration(material);
        }
        raw.setSmelting(cooked, U);
        raw.setForging(cooked, U);
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

    @EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        MiscMaterials.diagnoseMinecarts();
        registerMeatProcessing("minecraft:chicken", ChickenRaw, ChickenCooked);
        registerMeatProcessing("minecraft:beef", BeefRaw, BeefCooked);
        registerMeatProcessing("minecraft:porkchop", PorkRaw, PorkCooked);
        SpeciesFoodRecipes.register(MuttonRaw, MuttonCooked, MT.MeatRotten);
        overrideMeatMortarOutput("gregtech:gt.multiitem.food", 1400, MuttonRaw);
        overrideMeatMortarOutput("gregtech:gt.multiitem.food", 1210, BeefRaw);
        registerFishProcessing(0, CodRaw, CodCooked);
        registerFishProcessing(1, SalmonRaw, SalmonCooked);
    }

    private static void registerMeatProcessing(String itemName, OreDictMaterial raw, OreDictMaterial cooked) {
        overrideMeatMortarOutput(itemName, 0, raw);
        SpeciesFoodRecipes.register(raw, cooked, MT.MeatRotten);
    }

    private static void registerFishProcessing(int metadata, OreDictMaterial raw, OreDictMaterial cooked) {
        // GT6 fish mortar outputs may be one unit, rather than the meat recipe's two.
        overrideFishMortarOutput(metadata, raw);
        SpeciesFoodRecipes.register(raw, cooked, MT.FishRotten);
    }

    private static void overrideFishMortarOutput(int metadata, OreDictMaterial raw) {
        Object registered = Item.itemRegistry.getObject("minecraft:fish");
        if (!(registered instanceof Item)) return;
        ItemStack fish = new ItemStack((Item)registered, 1, metadata);
        Recipe candidate = null;
        ItemStack replacement = null;
        int matches = 0;
        for (Recipe recipe : RM.Mortar.mRecipeList) {
            if (recipe.mInputs == null || recipe.mOutputs == null
                    || recipe.mInputs.length != 1 || recipe.mOutputs.length != 1) continue;
            if (!ST.equal(recipe.mInputs[0], fish, true) || recipe.mInputs[0].stackSize != 1) continue;
            for (long amount : new long[] {U, 2 * U}) {
                ItemStack generic = OM.dust(MT.FishRaw, amount);
                ItemStack specific = OM.dust(raw, amount);
                if (generic != null && specific != null
                        && ST.equal(recipe.mOutputs[0], generic, true)
                        && recipe.mOutputs[0].stackSize == generic.stackSize) {
                    candidate = recipe;
                    replacement = specific;
                    matches++;
                    break;
                }
            }
        }
        if (matches == 1) {
            candidate.mOutputs[0] = replacement;
            FMLLog.info("[GT6MaterialExpansion] Fish metadata %d mortar output replaced", metadata);
        } else {
            FMLLog.info("[GT6MaterialExpansion] Fish metadata %d mortar matches=%d; unchanged", metadata, matches);
        }
    }

    /** Replace only a unique, unambiguous GT6 generic-meat mortar recipe. */
    private static void overrideMeatMortarOutput(String registryName, int metadata, OreDictMaterial raw) {
        Object registered = Item.itemRegistry.getObject(registryName);
        if (!(registered instanceof Item)) return;
        ItemStack item = new ItemStack((Item)registered, 1, metadata);
        ItemStack generic = OM.dust(MT.MeatRaw, 2 * U);
        ItemStack specific = OM.dust(raw, 2 * U);
        if (generic == null || specific == null) return;
        Recipe candidate = null;
        int matches = 0;
        for (Recipe recipe : RM.Mortar.mRecipeList) {
            if (recipe.mInputs == null || recipe.mOutputs == null
                    || recipe.mInputs.length != 1 || recipe.mOutputs.length != 1) continue;
            if (!ST.equal(recipe.mInputs[0], item, true) || recipe.mInputs[0].stackSize != 1) continue;
            if (!ST.equal(recipe.mOutputs[0], generic, true)
                    || recipe.mOutputs[0].stackSize != generic.stackSize) continue;
            candidate = recipe;
            matches++;
        }
        if (matches == 1) {
            candidate.mOutputs[0] = specific;
            FMLLog.info("[GT6MaterialExpansion] %s mortar now outputs species meat", registryName);
        } else {
            FMLLog.info("[GT6MaterialExpansion] %s mortar expected one generic recipe, found %d; unchanged", registryName, matches);
        }
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
