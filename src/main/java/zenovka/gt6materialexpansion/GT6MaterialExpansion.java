package zenovka.gt6materialexpansion;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import gregapi.data.CS;
import gregapi.data.MT;
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
    public static OreDictMaterial LOAM;
    public static OreDictMaterial SOIL;
    public static OreDictMaterial RAW_CHICKEN;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOAM = OreDictMaterial.createMaterial(32011, "Loam", "Loam");
        SOIL = OreDictMaterial.createMaterial(32013, "Soil", "Soil");
        RAW_CHICKEN = OreDictMaterial.createMaterial(32014, "ChickenRaw", "Raw Chicken");
        // Test material: inherit raw meat density; no recipes or processing registered.
        RAW_CHICKEN.setDensity(MT.MeatRaw.mGramPerCubicCentimeter);
        // GT6 food mortar recipes inspect the MEAT material tag, not merely ore names.
        RAW_CHICKEN.put(TD.Properties.MEAT);
        // Register equivalent GT6 material forms under the generic raw-meat name.
        RAW_CHICKEN.addReRegistrations(MT.MeatRaw);

        // Soil uses GT6 constituent masses, never a hardcoded kilogram value.
        // Nine-part ratio: 6 Stone, 2 Biomass, 1 Water.
        SOIL.setMoleculeConfiguration(new OreDictConfigurationComponent(9,
            new OreDictMaterialStack(MT.Stone, 6 * U),
            new OreDictMaterialStack(MT.Biomass, 2 * U),
            new OreDictMaterialStack(MT.H2O, U)));
        // GT6 already defines Biomass; do not register a duplicate.
        LOAM.setMoleculeConfiguration(new OreDictConfigurationComponent(10,
            new OreDictMaterialStack(MT.STONES.Siltstone, 4 * U),
            new OreDictMaterialStack(MT.Clay, 2 * U),
            new OreDictMaterialStack(SOIL, 4 * U)));
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
        assignIfMissing("minecraft:dirt", 0, SOIL, 9 * U);
        assignIfMissing("minecraft:dirt", 1, SOIL, 9 * U);
        assignIfMissing("BiomesOPlenty:newBopDirt", 4, MT.STONES.Siltstone, 9 * U);
        assignIfMissing("BiomesOPlenty:newBopGrass", 0, LOAM, 9 * U);
        assignIfMissing("BiomesOPlenty:mud", 0, SOIL, 9 * U);
        // Additional item associations will follow after pack-specific verification.
        testChickenOverride();
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
            chicken, new OreDictItemData(RAW_CHICKEN, amount,
                new OreDictMaterialStack(MT.Bone, U / 9)));
        OreDictItemData after = OreDictManager.INSTANCE.getItemData(chicken);
        FMLLog.info("[GT6MaterialExpansion] Chicken override: accepted=%s, after=%s",
            accepted, describeMaterial(after));
        if (!accepted) {
            FMLLog.info("[GT6MaterialExpansion] Chicken override refused by GT6; no forced mutation attempted");
        }
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
