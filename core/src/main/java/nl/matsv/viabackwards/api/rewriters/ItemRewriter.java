package nl.matsv.viabackwards.api.rewriters;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.data.MappedItem;
import org.jetbrains.annotations.Nullable;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.viaversion.libs.opennbt.tag.builtin.ByteTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.IntTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.ListTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.StringTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.Tag;

public abstract class ItemRewriter<T extends BackwardsProtocol> extends ItemRewriterBase<T> {

    private final TranslatableRewriter translatableRewriter;

    protected ItemRewriter(T protocol, @Nullable TranslatableRewriter translatableRewriter) {
        super(protocol, true);
        this.translatableRewriter = translatableRewriter;
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

        MappedItem data = protocol.getMappingData().getMappedItem(item.getIdentifier());
        if (data == null) {
            // Just rewrite the id
            return super.handleItemToClient(item);
        }

        if (item.getTag() == null) {
            item.setTag(new CompoundTag(""));
        }

        // Save original id, set remapped id
        item.getTag().put(new IntTag(nbtTagName + "|id", item.getIdentifier()));
        item.setIdentifier(data.getId());

        // Set custom name - only done if there is no original one
        if (display == null) {
            item.getTag().put(display = new CompoundTag("display"));
        }
        if (!display.contains("Name")) {
            display.put(new StringTag("Name", data.getJsonName()));
            display.put(new ByteTag(nbtTagName + "|customName"));
        }
        return item;
    }

    @Override
    @Nullable
    public Item handleItemToServer(Item item) {
        if (item == null) return null;

        super.handleItemToServer(item);
        if (item.getTag() != null) {
            IntTag originalId = item.getTag().remove(nbtTagName + "|id");
            if (originalId != null) {
                item.setIdentifier(originalId.getValue());
            }
        }
        return item;
    }
}
