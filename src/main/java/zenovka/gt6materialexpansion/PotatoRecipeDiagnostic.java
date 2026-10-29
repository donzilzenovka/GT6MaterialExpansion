package zenovka.gt6materialexpansion;

import cpw.mods.fml.common.FMLLog;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.recipes.Recipe;
import gregapi.util.OM;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import java.util.List;
import java.util.Map;

/** Read-only startup probe: distinguish canonical recipe resolution from item data. */
public final class PotatoRecipeDiagnostic {
    private PotatoRecipeDiagnostic() {}

    public static void dump() {
        ItemStack raw = new ItemStack(Items.potato);
        ItemStack baked = new ItemStack(Items.baked_potato);
        FMLLog.info("[GT6MaterialExpansion][potato] BEGIN");
        describe("raw", raw);
        describe("baked", baked);
        int crafting = 0, furnace = 0, machines = 0;
        for (Object candidate : CraftingManager.getInstance().getRecipeList()) {
            if (!(candidate instanceof IRecipe)) continue;
            IRecipe recipe = (IRecipe) candidate;
            if (!same(recipe.getRecipeOutput(), baked)) continue;
            crafting++;
            FMLLog.info("[GT6MaterialExpansion][potato] crafting #%d type=%s output=%s",
                    crafting, recipe.getClass().getName(), item(recipe.getRecipeOutput()));
        }
        for (Object rawEntry : FurnaceRecipes.smelting().getSmeltingList().entrySet()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) rawEntry;
            if (!(entry.getKey() instanceof ItemStack) || !(entry.getValue() instanceof ItemStack)
                    || !same((ItemStack) entry.getValue(), baked)) continue;
            furnace++;
            ItemStack input = (ItemStack) entry.getKey();
            FMLLog.info("[GT6MaterialExpansion][potato] furnace #%d input=%s output=%s",
                    furnace, item(input), item((ItemStack) entry.getValue()));
            describe("furnace input #" + furnace, input);
        }
        for (Recipe.RecipeMap map : Recipe.RecipeMap.RECIPE_MAP_LIST) {
            for (Recipe recipe : map.mRecipeList) {
                if (recipe.mOutputs == null) continue;
                for (ItemStack output : recipe.mOutputs) {
                    if (!same(output, baked)) continue;
                    machines++;
                    FMLLog.info("[GT6MaterialExpansion][potato] machine #%d map=%s output=%s inputs=%s",
                            machines, map.mNameInternal, item(output), items(recipe.mInputs));
                }
            }
        }
        FMLLog.info("[GT6MaterialExpansion][potato] counts crafting=%d furnace=%d machines=%d",
                crafting, furnace, machines);
        FMLLog.info("[GT6MaterialExpansion][potato] END");
    }

    private static boolean same(ItemStack a, ItemStack b) {
        return a != null && b != null && a.getItem() == b.getItem()
                && a.getItemDamage() == b.getItemDamage();
    }

    private static void describe(String label, ItemStack stack) {
        OreDictItemData data = OM.anydata(stack);
        List<OreDictMaterialStack> direct = data == null ? null : data.getAllMaterialStacks();
        FMLLog.info("[GT6MaterialExpansion][potato] %s %s direct=%s canonical=%s nbt=%s",
                label, item(stack), parts(direct), parts(MaterialDefaultResolver.resolve(stack)),
                stack.getTagCompound());
    }

    private static String parts(List<OreDictMaterialStack> list) {
        if (list == null) return "null";
        StringBuilder s = new StringBuilder("[");
        for (OreDictMaterialStack part : list)
            s.append(part == null ? "null" : part.mMaterial + ":" + part.mAmount).append("; ");
        return s.append("]").toString();
    }

    private static String item(ItemStack stack) {
        if (stack == null) return "null";
        return Item.itemRegistry.getNameForObject(stack.getItem()) + ":" + stack.getItemDamage()
                + " x" + stack.stackSize;
    }

    private static String items(ItemStack[] stacks) {
        if (stacks == null) return "null";
        StringBuilder s = new StringBuilder("[");
        for (ItemStack stack : stacks) s.append(item(stack)).append("; ");
        return s.append("]").toString();
    }
}
