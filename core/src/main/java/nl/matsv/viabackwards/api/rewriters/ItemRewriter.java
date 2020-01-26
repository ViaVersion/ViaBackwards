package nl.matsv.viabackwards.api.rewriters;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.data.MappedItem;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.rewriters.IdRewriteFunction;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.StringTag;

public abstract class ItemRewriter<T extends BackwardsProtocol> extends ItemRewriterBase<T> {

    private final MappedItemFunction mappedItemFunction;

    protected ItemRewriter(T protocol, IdRewriteFunction oldRewriter, IdRewriteFunction newRewriter, MappedItemFunction mappedItemFunction) {
        super(protocol, oldRewriter, newRewriter, true);
        this.mappedItemFunction = mappedItemFunction;
    }

    protected ItemRewriter(T protocol, MappedItemFunction mappedItemFunction) {
        super(protocol, true);
        this.mappedItemFunction = mappedItemFunction;
    }

    @Override
    public Item handleItemToClient(Item item) {
        if (item == null) return null;

        MappedItem data = mappedItemFunction.get(item.getIdentifier());
        if (data == null) {
            // Just rewrite the id
            return super.handleItemToClient(item);
        }

        if (item.getTag() == null) {
            item.setTag(new CompoundTag(""));
        }

        // Backup data for toServer
        item.getTag().put(createViaNBT(item));

        // Also includes the already mapped id
        item.setIdentifier(data.getId());

        // Set custom name
        CompoundTag tag = item.getTag().get("display");
        if (tag == null) {
            item.getTag().put(tag = new CompoundTag("display"));
        }
        // Only set name if there is no original one
        if (!tag.contains("Name")) {
            tag.put(new StringTag("Name", data.getJsonName()));
        }

        return item;
    }

    @FunctionalInterface
    public interface MappedItemFunction {

        MappedItem get(int id);
    }
}
