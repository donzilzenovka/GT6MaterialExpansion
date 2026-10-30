package zenovka.gt6materialexpansion;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
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
import net.minecraft.init.Items;
import cpw.mods.fml.common.FMLLog;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.MinecraftForge;

/**
 * GT6 material associations and recipe-derived composition integration.
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
    public static OreDictMaterial Matter;
    public static OreDictMaterial Mycelium;
    public static OreDictMaterial AppleFruit, Carrot, Egg;
    public static OreDictMaterial HeavyCream, CoconutCream;
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
        MaterialRegistry.register();
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        MaterialDensityRegistry.apply();
        ItemMaterialRegistry.register();
    }

    @EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        if (event.getSide().isClient() && Loader.isModLoaded("NotEnoughItems")) {
            try {
                Class.forName("zenovka.gt6materialexpansion.NEIMaterialPreview")
                    .getMethod("install").invoke(null);
                Class.forName("zenovka.gt6materialexpansion.NEICraftingMaterialPreview")
                    .getMethod("install").invoke(null);
                Class.forName("zenovka.gt6materialexpansion.NEIFurnaceMaterialPreview")
                    .getMethod("install").invoke(null);
                if (Loader.isModLoaded("EnderIO"))
                    Class.forName("zenovka.gt6materialexpansion.NEIIngredientDecorator")
                        .getMethod("install").invoke(null);
            } catch (Throwable error) {
                FMLLog.warning("[GT6MaterialExpansion] NEI crafting material integration failed: %s", error);
            }
        }
    }

    @EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
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

}
