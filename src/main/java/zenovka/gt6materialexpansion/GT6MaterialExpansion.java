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
    public static OreDictMaterial BeefRaw, BeefCooked;
    public static OreDictMaterial PorkRaw, PorkCooked;
    public static OreDictMaterial MuttonRaw, MuttonCooked;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Loam = OreDictMaterial.createMaterial(32011, "Loam", "Loam");
        Soil = OreDictMaterial.createMaterial(32013, "Soil", "Soil");
        ChickenRaw = OreDictMaterial.createMaterial(32014, "ChickenRaw", "Raw Chicken");
        ChickenCooked = OreDictMaterial.createMaterial(32015, "ChickenCooked", "Cooked Chicken");
        BeefRaw = OreDictMaterial.createMaterial(32016, "BeefRaw", "Raw Beef");
        BeefCooked = OreDictMaterial.createMaterial(32017, "BeefCooked", "Cooked Beef");
        PorkRaw = OreDictMaterial.createMaterial(32018, "PorkRaw", "Raw Pork");
        PorkCooked = OreDictMaterial.createMaterial(32019, "PorkCooked", "Cooked Pork");
        MuttonRaw = OreDictMaterial.createMaterial(32020, "MuttonRaw", "Raw Mutton");
        MuttonCooked = OreDictMaterial.createMaterial(32021, "MuttonCooked", "Cooked Mutton");

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
        // Additional item associations will follow after pack-specific verification.
        overrideMeatItem("minecraft:chicken", ChickenRaw, true);
        overrideMeatItem("minecraft:cooked_chicken", ChickenCooked, false);
        overrideMeatItem("minecraft:beef", BeefRaw, true);
        overrideMeatItem("minecraft:cooked_beef", BeefCooked, false);
        overrideMeatItem("minecraft:porkchop", PorkRaw, true);
        overrideMeatItem("minecraft:cooked_porkchop", PorkCooked, false);
        // Mutton item association awaits the actual modded registry names.
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

    private static void overrideMeatItem(String registryName, OreDictMaterial material, boolean raw) {
        Object registered = Item.itemRegistry.getObject(registryName);
        if (!(registered instanceof Item)) return;
        ItemStack stack = new ItemStack((Item)registered, 1, 0);
        OreDictItemData before = OreDictManager.INSTANCE.getItemData(stack);
        long amount = raw && before != null && before.mMaterial != null
            ? before.mMaterial.mAmount : 2 * U;
        OreDictItemData replacement = raw
            ? new OreDictItemData(material, amount, new OreDictMaterialStack(MT.Bone, U / 9))
            : new OreDictItemData(material, 2 * U);
        boolean accepted = OreDictManager.INSTANCE.setItemData(stack, replacement);
        FMLLog.info("[GT6MaterialExpansion] %s material association accepted=%s", registryName, accepted);
    }

    @EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        registerMeatProcessing("minecraft:chicken", ChickenRaw, ChickenCooked);
        registerMeatProcessing("minecraft:beef", BeefRaw, BeefCooked);
        registerMeatProcessing("minecraft:porkchop", PorkRaw, PorkCooked);
        ChickenFoodRecipes.register(MuttonRaw, MuttonCooked);
    }

    private static void registerMeatProcessing(String itemName, OreDictMaterial raw, OreDictMaterial cooked) {
        overrideMeatMortarOutput(itemName, raw);
        ChickenFoodRecipes.register(raw, cooked);
    }

    /** Replace only a unique, unambiguous GT6 generic-meat mortar recipe. */
    private static void overrideMeatMortarOutput(String registryName, OreDictMaterial raw) {
        Object registered = Item.itemRegistry.getObject(registryName);
        if (!(registered instanceof Item)) return;
        ItemStack item = new ItemStack((Item)registered, 1, 0);
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
