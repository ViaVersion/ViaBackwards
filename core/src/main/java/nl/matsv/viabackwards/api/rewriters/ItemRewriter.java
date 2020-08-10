package nl.matsv.viabackwards.api.rewriters;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.data.MappedItem;
import org.jetbrains.annotations.Nullable;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.rewriters.IdRewriteFunction;
import us.myles.viaversion.libs.opennbt.tag.builtin.ByteTag;
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

        CompoundTag display = null;
        if (translatableRewriter != null
                && item.getTag() != null && (display = item.getTag().get("display")) != null) {
            // Handle name and lore components
            StringTag name = display.get("Name");
            if (name != null) {
                String newValue = translatableRewriter.processText(name.getValue()).toString();
                if (!newValue.equals(name.getValue())) {
                    saveNameTag(display, name);
                }

                name.setValue(newValue);
            }

            ListTag lore = display.get("Lore");
            if (lore != null) {
                ListTag original = null;
                boolean changed = false;
                for (Tag loreEntryTag : lore) {
                    if (!(loreEntryTag instanceof StringTag)) continue;

                    StringTag loreEntry = (StringTag) loreEntryTag;
                    String newValue = translatableRewriter.processText(loreEntry.getValue()).toString();
                    if (!changed && !newValue.equals(loreEntry.getValue())) {
                        changed = true;
                        original = lore.clone();
                    }

                    loreEntry.setValue(newValue);
                }

                if (changed) {
                    saveLoreTag(display, original);
                }
            }
        }

        MappedItem data = mappedItemFunction.get(item.getIdentifier());
        if (data == null) {
            // Just rewrite the id
            return super.handleItemToClient(item);
        }

        // Set remapped id
        item.setIdentifier(data.getId());

        // Set custom name - only done if there is no original one
        if (item.getTag() == null) {
            item.setTag(new CompoundTag(""));
        }
        if (display == null) {
            item.getTag().put(display = new CompoundTag("display"));
        }
        if (!display.contains("Name")) {
            display.put(new StringTag("Name", data.getJsonName()));
            display.put(new ByteTag(nbtTagName + "|customName"));
        }
        return item;
    }

    @FunctionalInterface
    public interface MappedItemFunction {

        @Nullable
        MappedItem get(int id);
    }
}
