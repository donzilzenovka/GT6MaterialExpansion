package zenovka.gt6materialexpansion;

import gregapi.oredict.OreDictMaterial;
import java.util.LinkedHashMap;
import java.util.Map;

/** Fruit and vegetable substance identities shared by solid foods and fluid products.
 * These are composition identities, not new Forge fluids or recipes.
 */
public final class ProduceMaterials {
    private static final Map<String, OreDictMaterial> MATERIALS = new LinkedHashMap<String, OreDictMaterial>();
    private ProduceMaterials() {}

    public static void register() {
        if (!MATERIALS.isEmpty()) return;
        add(32100, "Raspberry", "Raspberry", 150, 45, 65);
        add(32101, "Blackberry", "Blackberry", 65, 35, 85);
        add(32102, "Currant", "Currant", 145, 35, 65);
        add(32103, "Blueberry", "Blueberry", 60, 75, 145);
        add(32104, "Gooseberry", "Gooseberry", 125, 165, 65);
        add(32105, "Elderberry", "Elderberry", 75, 40, 90);
        add(32106, "Cranberry", "Cranberry", 180, 45, 55);
        add(32107, "Strawberry", "Strawberry", 215, 55, 65);
        add(32108, "Plum", "Plum", 125, 65, 130);
        add(32109, "Peach", "Peach", 235, 150, 100);
        add(32110, "Grapefruit", "Grapefruit", 230, 135, 100);
        add(32111, "Lime", "Lime", 150, 195, 55);
        add(32112, "Lemon", "Lemon", 230, 215, 65);
        add(32113, "OrangeFruit", "Orange", 235, 150, 40);
        add(32114, "Kiwi", "Kiwi", 130, 165, 65);
        add(32115, "Persimmon", "Persimmon", 220, 115, 45);
        add(32116, "Melon", "Melon", 150, 185, 100);
        add(32117, "Apricot", "Apricot", 225, 150, 70);
        add(32118, "Pear", "Pear", 165, 185, 85);
        add(32119, "Grape", "Grape", 135, 85, 150);
        add(32120, "Pineapple", "Pineapple", 205, 180, 65);
        add(32121, "Banana", "Banana", 220, 205, 100);
        add(32122, "Cherry", "Cherry", 165, 45, 65);
        add(32123, "Mango", "Mango", 235, 160, 60);
        add(32124, "Pomegranate", "Pomegranate", 170, 55, 65);
        add(32125, "Starfruit", "Starfruit", 210, 205, 75);
        add(32126, "Papaya", "Papaya", 220, 145, 75);
        add(32127, "Fig", "Fig", 125, 70, 120);
        add(32128, "CoconutFruit", "Coconut", 170, 135, 95);
        add(32129, "Date", "Date", 125, 75, 50);
        add(32130, "Tomato", "Tomato", 205, 65, 55);
        add(32131, "Beet", "Beet", 130, 45, 80);
        add(32132, "Pumpkin", "Pumpkin", 215, 130, 45);
        add(32133, "Cucumber", "Cucumber", 85, 150, 65);
        add(32134, "Onion", "Onion", 190, 165, 110);
        add(32135, "Cactus", "Cactus", 65, 145, 70);
        add(32136, "Reed", "Reed", 125, 165, 85);
    }

    private static void add(int id, String key, String label, int red, int green, int blue) {
        OreDictMaterial material = OreDictMaterial.createMaterial(id, key, label);
        material.setDensity(1.00D); // Provisional; identity registration only.
        material.setRGBa(red, green, blue, 255);
        MATERIALS.put(key, material);
    }

    public static OreDictMaterial get(String key) {
        return MATERIALS.get(key);
    }
}
