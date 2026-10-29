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

/** Existing GT6 density adjustments; preserve initialization order. */
public final class MaterialDensityRegistry {
    private MaterialDensityRegistry() {}
    public static void apply() {
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


    }
}
