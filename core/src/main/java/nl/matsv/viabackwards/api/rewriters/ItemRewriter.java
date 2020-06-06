package nl.matsv.viabackwards.api.rewriters;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.data.MappedItem;
import org.jetbrains.annotations.Nullable;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.rewriters.IdRewriteFunction;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.ListTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.StringTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.Tag;

public abstract class ItemRewriter<T extends BackwardsProtocol> extends ItemRewriterBase<T> {

    private final MappedItemFunction mappedItemFunction;
    private final TranslatableRewriter translatableRewriter;

    protected ItemRewriter(T protocol, @Nullable TranslatableRewriter translatableRewriter,
                           @Nullable IdRewriteFunction oldRewriter, @Nullable IdRewriteFunction newRewriter, MappedItemFunction mappedItemFunction) {
        super(protocol, oldRewriter, newRewriter, true);
        this.translatableRewriter = translatableRewriter;
        this.mappedItemFunction = mappedItemFunction;
    }

    protected ItemRewriter(T protocol, @Nullable TranslatableRewriter translatableRewriter, MappedItemFunction mappedItemFunction) {
        super(protocol, true);
        this.translatableRewriter = translatableRewriter;
        this.mappedItemFunction = mappedItemFunction;
    }

    @Override
    @Nullable
    public Item handleItemToClient(Item item) {
        if (item == null) return null;

        CompoundTag tag = null;
        boolean textChanged = false;
        if (translatableRewriter != null
                && item.getTag() != null && (tag = item.getTag().get("display")) != null) {
            // Handle name and lore components
            StringTag name = tag.get("Name");
            if (name != null) {
                String newValue = translatableRewriter.processText(name.getValue());
                if (name.getValue().equals(newValue)) {
                    textChanged = true;
                }
                name.setValue(newValue);
            }

            ListTag lore = tag.get("Lore");
            if (lore != null) {
                for (Tag loreEntry : lore) {
                    if (!(loreEntry instanceof StringTag)) continue;

                    StringTag stringTag = (StringTag) loreEntry;
                    String newValue = translatableRewriter.processText(stringTag.getValue());
                    if (stringTag.getValue().equals(newValue)) {
                        textChanged = true;
                    }
                    stringTag.setValue(newValue);
                }
            }
        }

        if (textChanged) {
            // Backup data for toServer
            item.getTag().put(createViaNBT(item));
        }

        MappedItem data = mappedItemFunction.get(item.getIdentifier());
        if (data == null) {
            // Just rewrite the id
            return super.handleItemToClient(item);
        }

        // Backup data for toServer if not already done above
        if (!textChanged) {
            if (item.getTag() == null) {
                item.setTag(new CompoundTag(""));
            }
            item.getTag().put(createViaNBT(item));
        }

        // Set remapped id
        item.setIdentifier(data.getId());

        // Set custom name - only done if there is no original one
        if (tag == null) {
            item.getTag().put(tag = new CompoundTag("display"));
        }
        if (!tag.contains("Name")) {
            tag.put(new StringTag("Name", data.getJsonName()));
        }
        return item;
    }

    @FunctionalInterface
    public interface MappedItemFunction {

        @Nullable
        MappedItem get(int id);
    }
}
