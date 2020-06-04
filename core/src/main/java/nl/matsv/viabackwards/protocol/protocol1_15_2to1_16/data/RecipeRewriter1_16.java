package nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.data;

import nl.matsv.viabackwards.api.rewriters.ItemRewriterBase;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.data.RecipeRewriter1_15;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;

public class RecipeRewriter1_16 extends RecipeRewriter1_15 {

    public RecipeRewriter1_16(ItemRewriterBase rewriter) {
        super(rewriter);
    }

    public void register(int oldId, int newId) {
        // Remove new smithing type, only in this handler
        rewriter.getProtocol().registerOutgoing(State.PLAY, oldId, newId, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    int size = wrapper.passthrough(Type.VAR_INT);
                    int newSize = size;
                    for (int i = 0; i < size; i++) {
                        String originalType = wrapper.read(Type.STRING);
                        String type = originalType.replace("minecraft:", "");
                        if (type.equals("smithing")) {
                            newSize--;

                            wrapper.read(Type.STRING);
                            wrapper.read(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT);
                            wrapper.read(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT);
                            wrapper.read(Type.FLAT_VAR_INT_ITEM);
                            continue;
                        }

                        wrapper.write(Type.STRING, originalType);
                        String id = wrapper.passthrough(Type.STRING); // Recipe Identifier
                        handle(wrapper, type);
                    }

                    wrapper.set(Type.VAR_INT, 0, newSize);
                });
            }
        });
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
            case "blasting":
            case "smoking":
            case "campfire_cooking":
            case "smelting":
                handleSmelting(wrapper);
                break;
            case "stonecutting":
                handleStonecutting(wrapper);
                break;
            case "smithing": // new
                handleSmithing(wrapper);
                break;
        }
    }

    public void handleSmithing(PacketWrapper wrapper) throws Exception {
        Item[] baseIngredients = wrapper.passthrough(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT);
        for (Item item : baseIngredients) {
            rewriter.handleItemToClient(item);
        }
        Item[] ingredients = wrapper.passthrough(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT);
        for (Item item : ingredients) {
            rewriter.handleItemToClient(item);
        }
        rewriter.handleItemToClient(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM)); // Result
    }
}
