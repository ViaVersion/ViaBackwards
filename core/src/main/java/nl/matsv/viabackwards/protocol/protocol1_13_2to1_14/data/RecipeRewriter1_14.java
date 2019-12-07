package nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.data;

import nl.matsv.viabackwards.api.rewriters.BlockItemRewriter;
import nl.matsv.viabackwards.api.rewriters.RecipeRewriter;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.type.Type;

public class RecipeRewriter1_14 extends RecipeRewriter {

    public RecipeRewriter1_14(final BlockItemRewriter rewriter) {
        super(rewriter);
    }

    public void handle(PacketWrapper wrapper, String type) throws Exception {
        switch (type) {
            case "crafting_shapeless":
                handleCraftingShapeless(wrapper);
                break;
            case "crafting_shaped":
                handleCraftingShaped(wrapper);
                break;
            case "smelting":
                handleSmelting(wrapper);
                break;
        }
    }

    public void handleSmelting(PacketWrapper wrapper) throws Exception {
        wrapper.passthrough(Type.STRING); // Group
        Item[] items = wrapper.passthrough(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients
        for (Item item : items) {
            rewriter.handleItemToClient(item);
        }

        rewriter.handleItemToClient(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM)); // Result

        wrapper.passthrough(Type.FLOAT); // EXP
        wrapper.passthrough(Type.VAR_INT); // Cooking time
    }

    public void handleCraftingShaped(PacketWrapper wrapper) throws Exception {
        int ingredientsNo = wrapper.passthrough(Type.VAR_INT) * wrapper.passthrough(Type.VAR_INT);
        wrapper.passthrough(Type.STRING); // Group
        for (int j = 0; j < ingredientsNo; j++) {
            Item[] items = wrapper.passthrough(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients
            for (Item item : items) {
                rewriter.handleItemToClient(item);
            }
        }
        rewriter.handleItemToClient(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM)); // Result
    }

    public void handleCraftingShapeless(PacketWrapper wrapper) throws Exception {
        wrapper.passthrough(Type.STRING); // Group
        int ingredientsNo = wrapper.passthrough(Type.VAR_INT);
        for (int j = 0; j < ingredientsNo; j++) {
            Item[] items = wrapper.passthrough(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients
            for (Item item : items) {
                rewriter.handleItemToClient(item);
            }
        }
        rewriter.handleItemToClient(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM)); // Result
    }
}
