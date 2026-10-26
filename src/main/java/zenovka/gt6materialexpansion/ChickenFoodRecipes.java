package zenovka.gt6materialexpansion;

import gregapi.data.ANY;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.RM;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregapi.util.OM;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.FMLLog;

import static gregapi.data.CS.*;

/**
 * Extends GT6's explicitly authored food processing recipes to species materials.
 * No machine behaviour or generic meat ore-dictionary registrations are changed.
 */
public final class ChickenFoodRecipes {
    private ChickenFoodRecipes() {}

    private static boolean registered;

    public static void register(OreDictMaterial raw, OreDictMaterial cooked) {
        if (registered) return;
        registered = true;

        // Match Loader_Recipes_Food's compressor conversions, including IC2.
        OreDictPrefix[] input = {OP.dustTiny, OP.dustSmall, OP.dust};
        OreDictPrefix[] output = {OP.nugget, OP.chunkGt, OP.ingot};
        for (OreDictMaterial material : new OreDictMaterial[] {raw, cooked}) {
            for (int i = 0; i < input.length; i++) {
                ItemStack from = input[i].mat(material, 1);
                ItemStack to = output[i].mat(material, 1);
                if (from == null || to == null) continue;
                RM.Compressor.addRecipe1(T, 16, 16, from, to);
                RM.ic2_compressor(from, to);
            }
        }

        // GT6 deliberately converges raw and cooked meat into generic rotten meat.
        OreDictPrefix[] fermentForms = {
            OP.dustTiny, OP.dustSmall, OP.dust,
            OP.nugget, OP.chunkGt, OP.billet, OP.ingot
        };
        int[] durations = {16, 36, 144, 16, 36, 96, 144};
        for (OreDictMaterial material : new OreDictMaterial[] {raw, cooked}) {
            for (int i = 0; i < fermentForms.length; i++) {
                ItemStack from = fermentForms[i].mat(material, 1);
                ItemStack to = fermentForms[i].mat(MT.MeatRotten, 1);
                if (from != null && to != null)
                    RM.Fermenter.addRecipe1(T, 16, durations[i], from, to);
            }
        }

        // GT6's flour-based meat mixing is a manually authored food recipe.
        for (OreDictMaterial flour : ANY.FlourGrains.mToThis) {
            ItemStack flourDust = OM.dust(flour);
            ItemStack rawDust = OM.dust(raw);
            ItemStack rawIngot = OM.ingot(raw);
            if (flourDust != null && rawDust != null && rawIngot != null)
                RM.Mixer.addRecipe2(T, 16, 16, flourDust, rawDust, rawIngot);
        }

        // Explicit GT6 food-loader furnace recipes do not consult setSmelting().
        // Register only species-specific forms that actually exist.
        OreDictPrefix[] cookForms = {
            OP.dustTiny, OP.dustSmall, OP.dust,
            OP.nugget, OP.chunkGt, OP.billet, OP.ingot
        };
        for (OreDictPrefix prefix : cookForms) {
            ItemStack from = prefix.mat(raw, 1);
            ItemStack to = prefix.mat(cooked, 1);
            if (from != null && to != null)
                RM.add_smelting(from, to, F, T, F);
        }

        FMLLog.info("[GT6MaterialExpansion] Registered chicken food processing extensions");
    }
}
