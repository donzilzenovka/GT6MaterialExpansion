package zenovka.gt6materialexpansion.core;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import zenovka.gt6materialexpansion.AlloySmelterMaterials;
import zenovka.gt6materialexpansion.RecipePreviewTags;
import gregapi.oredict.OreDictMaterialStack;

/**
 * Optional Ender IO integration. Reflection keeps Ender IO out of the
 * compile classpath. Only alloy recipes in TileAlloySmelter are enriched.
 */
public final class AlloySmelterBridge {
    private AlloySmelterBridge() {}

    private static Field field(Class<?> type, String name) throws Exception {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(name);
    }

    private static Method method(Class<?> type, String name, int count) throws Exception {
        for (Class<?> c = type; c != null; c = c.getSuperclass())
            for (Method m : c.getDeclaredMethods())
                if (m.getName().equals(name) && m.getParameterTypes().length == count) {
                    m.setAccessible(true);
                    return m;
                }
        throw new NoSuchMethodException(name);
    }

    public static void merge(Object machine, Object results) {
        try {
            if (machine.getClass().getName().equals("crazypants.enderio.machine.alloy.TileAlloySmelter")) {
                enrich(machine, results);
            }
        } catch (Throwable error) {
            System.err.println("[GT6MaterialExpansion/Alloy] Material enrichment skipped: " + error);
        }
        // Always invoke Ender IO's original merger, even if enrichment failed.
        try {
            method(machine.getClass(), "mergeResults", 1).invoke(machine, results);
        } catch (Throwable error) {
            throw new RuntimeException("Ender IO output merge failed", error);
        }
    }

    private static void enrich(Object machine, Object results) throws Exception {
        if (results == null || Array.getLength(results) != 1) return;
        Object result = Array.get(results, 0);
        ItemStack output = (ItemStack) field(result.getClass(), "item").get(result);
        if (output == null || output.stackSize <= 0) return;

        // Do not attach provenance where Ender IO might merge into an
        // already occupied slot without checking material NBT.
        ItemStack[] inventory = (ItemStack[]) field(machine.getClass(), "inventory").get(machine);
        Object slots = field(machine.getClass(), "slotDefinition").get(machine);
        int outputSlot = field(slots.getClass(), "minOutputSlot").getInt(slots);
        if (inventory[outputSlot] != null) {
            System.out.println("[GT6MaterialExpansion/Alloy] Existing output stack: material tagging deferred.");
            return;
        }

        Object task = method(machine.getClass(), "getCurrentTask", 0).invoke(machine);
        if (task == null) return;
        Object recipe = method(task.getClass(), "getRecipe", 0).invoke(task);
        if (recipe == null) return;
        String recipeName = recipe.getClass().getName();
        boolean furnaceRecipe = recipeName.equals("crazypants.enderio.machine.alloy.VanillaSmeltingRecipe");
        String mode = String.valueOf(method(machine.getClass(), "getMode", 0).invoke(machine));
        if (!furnaceRecipe && !("ALLOY".equals(mode) || "ALL".equals(mode))) return;
        Object inputs = method(task.getClass(), "getInputs", 0).invoke(task);
        Object consumed = method(recipe.getClass(), "getQuantitiesConsumed", 1).invoke(recipe, inputs);
        if (!(consumed instanceof List)) return;
        List<?> consumedList = (List<?>) consumed;
        List<ItemStack> items = new ArrayList<ItemStack>();
        for (Object entry : consumedList) {
            if (entry == null) continue;
            ItemStack stack = (ItemStack) field(entry.getClass(), "item").get(entry);
            if (stack != null && stack.stackSize > 0) items.add(stack.copy());
        }
        List<OreDictMaterialStack> composition = AlloySmelterMaterials.calculate(
                items.toArray(new ItemStack[items.size()]), output);
        if (composition == null) {
            System.out.println("[GT6MaterialExpansion/Alloy] Could not resolve consumed material composition.");
            return;
        }
        ItemStack tagged = RecipePreviewTags.copyWithMaterials(output, composition);
        output.setTagCompound(tagged.getTagCompound());
        System.out.println("[GT6MaterialExpansion/Alloy] " + (furnaceRecipe ? "Furnace" : "Alloy")
                + " recipe: tagged " + tagged.stackSize
                + " output items from " + items.size() + " consumed ingredient stacks.");
    }
}
