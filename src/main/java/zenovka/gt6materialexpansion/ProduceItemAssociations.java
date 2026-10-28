package zenovka.gt6materialexpansion;

import cpw.mods.fml.common.FMLLog;
import gregapi.data.CS;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictManager;
import gregapi.oredict.OreDictMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Attach produce identities to unannotated crops using GT6/Forge ore names.
 * No recipes or fluid registrations are changed.
 */
public final class ProduceItemAssociations {
    private static final long U = CS.U;
    // 111 mB-equivalent per berry (user's 0.111 U baseline).
    private static final long BERRY = U * 111 / 1000;
    private static final Map<String, String> NAMES = new HashMap<String, String>();
    private static final Set<String> BERRIES = new HashSet<String>();
    private ProduceItemAssociations() {}

    static {
        for (String berry : new String[] {"Raspberry", "Blackberry", "Currant", "Blueberry",
                "Gooseberry", "Elderberry", "Cranberry", "Strawberry"}) {
            NAMES.put(berry.toLowerCase(Locale.ROOT), berry);
            BERRIES.add(berry);
        }
        for (String fruit : new String[] {"Plum", "Peach", "Grapefruit", "Lime", "Lemon",
                "Kiwi", "Persimmon", "Melon", "Apricot", "Pear", "Grape", "Pineapple",
                "Banana", "Cherry", "Mango", "Pomegranate", "Starfruit", "Papaya",
                "Fig", "Date", "Tomato", "Beet", "Pumpkin", "Cucumber", "Onion"}) {
            NAMES.put(fruit.toLowerCase(Locale.ROOT), fruit);
        }
        NAMES.put("orange", "OrangeFruit");
        NAMES.put("coconut", "CoconutFruit");
        NAMES.put("ananas", "Pineapple");
        NAMES.put("beetroot", "Beet");
        NAMES.put("currants", "Currant");
        NAMES.put("currantsred", "Currant");
        NAMES.put("currantsblack", "Currant");
        NAMES.put("currantswhite", "Currant");
        NAMES.put("grapered", "Grape");
        NAMES.put("grapewhite", "Grape");
        NAMES.put("grapegreen", "Grape");
        NAMES.put("grapepurple", "Grape");
        NAMES.put("grapes", "Grape");
        NAMES.put("cactusfruit", "Cactus");
        NAMES.put("cactus", "Cactus");
        NAMES.put("reed", "Reed");
        NAMES.put("reeds", "Reed");
        NAMES.put("sugarcane", "Reed");
    }

    public static void register() {
        Set<String> seen = new HashSet<String>();
        int assigned = 0;
        for (String ore : OreDictionary.getOreNames()) {
            String name = cropName(ore);
            if (name == null) continue;
            OreDictMaterial material = ProduceMaterials.get(name);
            if (material == null) continue;
            long amount = BERRIES.contains(name) ? BERRY : U;
            for (ItemStack entry : OreDictionary.getOres(ore)) {
                if (entry == null || entry.getItem() == null
                        || entry.getItemDamage() == OreDictionary.WILDCARD_VALUE) continue;
                ItemStack single = entry.copy();
                single.stackSize = 1;
                String key = Item.itemRegistry.getNameForObject(single.getItem())
                        + ":" + single.getItemDamage();
                if (!seen.add(key)) continue;
                if (OreDictManager.INSTANCE.getItemData(single) != null) continue;
                if (OreDictManager.INSTANCE.addItemData(single, new OreDictItemData(material, amount))) {
                    assigned++;
                    FMLLog.info("[GT6MaterialExpansion] Produce %s via %s: %s %d/%d U",
                        key, ore, name, amount, U);
                }
            }
        }
        FMLLog.info("[GT6MaterialExpansion] Produce ore scan assigned %d item variants", assigned);
    }

    private static String cropName(String ore) {
        String suffix;
        if (ore.startsWith("crop")) suffix = ore.substring(4);
        else if (ore.startsWith("fruit")) suffix = ore.substring(5);
        else if (ore.startsWith("berry")) suffix = ore.substring(5);
        else if (ore.startsWith("vegetable")) suffix = ore.substring(9);
        else return null;
        String normalized = suffix.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
        return NAMES.get(normalized);
    }
}
