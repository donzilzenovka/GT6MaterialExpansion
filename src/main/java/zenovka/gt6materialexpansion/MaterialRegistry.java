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

/** Defines custom materials and their initial physical properties. */
public final class MaterialRegistry {
    private static final long U = CS.U;
    private MaterialRegistry() {}
    public static void register() {
        ProduceMaterials.register();
        Loam = OreDictMaterial.createMaterial(32011, "Loam", "Loam");
        Soil = OreDictMaterial.createMaterial(32013, "Soil", "Soil");
        PlantMatter = OreDictMaterial.createMaterial(32026, "PlantMatter", "Plant Matter");
        PlantMatter.setDensity(0.45D);
        PlantMatter.setRGBa(90, 145, 55, 255);
        Matter = OreDictMaterial.createMaterial(32042, "Matter", "Matter (Unresolved)");
        Matter.setDensity(1.00D);
        Matter.setRGBa(255, 0, 200, 255);
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
        // Composition-only counterparts to GT6 food fluids; do not alter MT.Cream (-1).
        HeavyCream = OreDictMaterial.createMaterial(32040, "HeavyCream", "Heavy Cream").setDensity(0.99D);
        CoconutCream = OreDictMaterial.createMaterial(32041, "CoconutCream", "Coconut Cream").setDensity(1.00D);
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

}
