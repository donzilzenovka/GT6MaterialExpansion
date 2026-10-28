package zenovka.gt6materialexpansion;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregapi.NEI_RecipeMap;
import gregapi.data.RM;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.recipes.Recipe;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.List;

/**
 * Experimental additional NEI Mixer view, not a replacement for GT6's handler.
 * This keeps the original GT6 Mixer display untouched during compatibility tests.
 */
@SideOnly(Side.CLIENT)
public final class NEIMaterialPreview extends NEI_RecipeMap {
    private NEIMaterialPreview() {
        super(RM.Mixer);
    }

    public static void register() {
        new NEIMaterialPreview().init();
        FMLLog.info("[GT6MaterialExpansion] Registered experimental NEI Mixer material preview");
    }

    @Override
    public NEI_RecipeMap newInstance() {
        return new NEIMaterialPreview();
    }

    @Override
    public String getOverlayIdentifier() {
        return "gt6materialexpansion.mixer.materials";
    }

    @Override
    public String getRecipeName() {
        return "Mixer (Material Preview)";
    }

    @Override
    public void drawExtras(int recipeIndex) {
        super.drawExtras(recipeIndex);
        if (recipeIndex < 0 || recipeIndex >= arecipes.size()
                || !(arecipes.get(recipeIndex) instanceof CachedDefaultRecipe)) return;
        Recipe recipe = ((CachedDefaultRecipe) arecipes.get(recipeIndex)).mRecipe;
        List<OreDictMaterialStack> parts = RecipeMaterialPreview.forOutput(recipe, 0);
        // Alter only the cached NEI display stack, never the registered GT6 recipe output.
        if (parts != null && !parts.isEmpty() && !((CachedDefaultRecipe) arecipes.get(recipeIndex)).mOutputs.isEmpty()) {
            codechicken.nei.PositionedStack displayed = ((CachedDefaultRecipe) arecipes.get(recipeIndex)).mOutputs.get(0);
            ItemStack preview = recipe.mOutputs[0].copy();
            NBTTagCompound tag = preview.getTagCompound();
            if (tag == null) tag = new NBTTagCompound();
            OreDictMaterialStack.saveList("gt.recycling.mats", tag, parts);
            preview.setTagCompound(tag);
            displayed.items = new ItemStack[] {preview};
            displayed.item = preview;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.fontRenderer == null) return;
        String label = "Material: ";
        if (parts == null || parts.isEmpty()) {
            label += "Unresolved";
        } else {
            StringBuilder value = new StringBuilder();
            for (OreDictMaterialStack part : parts) {
                if (value.length() > 0) value.append(", ");
                value.append(part.mMaterial.mNameInternal);
                if (value.length() > 42) {
                    value.append("...");
                    break;
                }
            }
            label += value.toString();
        }
        mc.fontRenderer.drawString(label, 10, 166, 0x404040);
    }
}
