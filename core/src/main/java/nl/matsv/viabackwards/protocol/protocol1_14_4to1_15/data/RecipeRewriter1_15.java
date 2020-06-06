package nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.data;

import nl.matsv.viabackwards.api.rewriters.ItemRewriterBase;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.data.RecipeRewriter1_14;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.type.Type;

public class RecipeRewriter1_15 extends RecipeRewriter1_14 {

    public RecipeRewriter1_15(ItemRewriterBase rewriter) {
        super(rewriter);
    }

    @Override
    public void handle(PacketWrapper wrapper, String type) throws Exception {
        switch (type) {
            case "crafting_shapeless":
                handleCraftingShapeless(wrapper);
                break;
            case "crafting_shaped":
                handleCraftingShaped(wrapper);
                break;
            case "blasting": // new
            case "smoking": // new
            case "campfire_cooking": // new
            case "smelting":
                handleSmelting(wrapper);
                break;
            case "stonecutting": // new
                handleStonecutting(wrapper);
                break;
        }
    }

    public void handleStonecutting(PacketWrapper wrapper) throws Exception {
        wrapper.passthrough(Type.STRING);
        Item[] items = wrapper.passthrough(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients
        for (Item item : items) {
            rewriter.handleItemToClient(item);
        }

        rewriter.handleItemToClient(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM)); // Result
    }
}
