package zenovka.gt6materialexpansion;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictManager;
import gregapi.oredict.configurations.OreDictConfigurationComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Initial GT6 6.17.06 material-data experiment.
 * No blocks, items, recipes, or ore dictionary entries are registered here.
 */
@Mod(modid = "gt6materialexpansion", name = "GT6 Material Expansion",
     version = "0.1.0", dependencies = "required-after:gregtech")
public final class GT6MaterialExpansion {
    private static final long U = CS.U;

    // IDs are in GT6's custom-material range; check for pack conflicts before release.
    public static OreDictMaterial SILT;
    public static OreDictMaterial LOAM;
    public static OreDictMaterial HUMUS;
    public static OreDictMaterial SOIL;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        SILT = OreDictMaterial.createMaterial(32010, "ZenovkaSilt", "Silt");
        LOAM = OreDictMaterial.createMaterial(32011, "ZenovkaLoam", "Loam");
        HUMUS = OreDictMaterial.createMaterial(32012, "ZenovkaHumus", "Humus");
        SOIL = OreDictMaterial.createMaterial(32013, "ZenovkaSoil", "Soil");

        // The composition values are provisional material-model ratios.
        // GT6's siltstone is the mineral proxy for loose silt.
        SILT.setMoleculeConfiguration(new OreDictConfigurationComponent(1,
            new OreDictMaterialStack(MT.STONES.Siltstone, U)));
        LOAM.setMoleculeConfiguration(new OreDictConfigurationComponent(1,
            new OreDictMaterialStack(MT.Sand, 4 * U),
            new OreDictMaterialStack(SILT, 4 * U),
            new OreDictMaterialStack(MT.Clay, 2 * U)));
        // Humus chemistry is intentionally not invented in this first pass.
        // SOIL remains an unconfigured material until its composition is agreed.
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        // Deliberately conservative: only unannotated item stacks receive data.
        // Do not replace GT6's existing item material assignments.
        assignIfMissing("minecraft:dirt", 0, SOIL, 9 * U);
        assignIfMissing("minecraft:dirt", 1, SOIL, 9 * U);
        assignIfMissing("BiomesOPlenty:newBopDirt", 4, SILT, 9 * U);
        assignIfMissing("BiomesOPlenty:newBopGrass", 0, LOAM, 9 * U);
        assignIfMissing("BiomesOPlenty:mud", 0, SOIL, 9 * U);
        // Additional item associations will follow after pack-specific verification.
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
