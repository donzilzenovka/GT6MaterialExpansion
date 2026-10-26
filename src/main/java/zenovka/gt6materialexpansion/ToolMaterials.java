package zenovka.gt6materialexpansion;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.util.OM;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Arrays;

/**
 * Experimental GT6 shovel material data. Enabled only for GT6 shovels and
 * never replaces an existing recycling-material tag.
 *
 * Note: gt.recycling.mats is active GT6 recycling data, not a passive tooltip.
 */
public final class ToolMaterials {
    private static final String RECYCLING_TAG = "gt.recycling.mats";

    private ToolMaterials() {}

    public static void register() {
        FMLCommonHandler.instance().bus().register(new ToolMaterials());
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.worldObj.isRemote
                || event.player.ticksExisted % 20 != 0) return;
        EntityPlayer player = event.player;
        for (ItemStack stack : player.inventory.mainInventory) annotate(stack);
    }

    private static void annotate(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof MultiItemTool)) return;
        MultiItemTool tool = (MultiItemTool) stack.getItem();
        // Limit the experiment to the ordinary GT6 shovel, not other tools.
        if (!(tool.getToolStatsInternal(stack) instanceof gregtech.items.tools.early.GT_Tool_Shovel)) return;

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || tag.hasKey(RECYCLING_TAG)) return;
        OreDictMaterial head = MultiItemTool.getPrimaryMaterial(stack);
        OreDictMaterial handle = MultiItemTool.getSecondaryMaterial(stack);
        if (head == null || handle == null || head == MT.NULL || handle == MT.NULL) return;

        // Head prefix amount plus two half-unit sticks (one full unit of handle material).
        // Verify GT6 shovel assembly against actual crafting recipes in-game.
        OreDictMaterialStack headPart = OM.stack(head, OP.toolHeadShovel.mAmount);
        OreDictMaterialStack handlePart = OM.stack(handle, CS.U);
        if (headPart == null || handlePart == null) return;

        OreDictMaterialStack.saveList(RECYCLING_TAG, tag, Arrays.asList(headPart, handlePart));
        stack.setTagCompound(tag);
    }
}
