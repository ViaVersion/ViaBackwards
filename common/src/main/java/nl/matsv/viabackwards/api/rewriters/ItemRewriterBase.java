package nl.matsv.viabackwards.api.rewriters;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import org.jetbrains.annotations.Nullable;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.ListTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.StringTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.Tag;

public abstract class ItemRewriterBase<T extends BackwardsProtocol> extends Rewriter<T> {

    protected final String nbtTagName;
    protected final boolean jsonNameFormat;

    protected ItemRewriterBase(T protocol, boolean jsonNameFormat) {
        super(protocol);
        this.jsonNameFormat = jsonNameFormat;
        nbtTagName = "VB|" + protocol.getClass().getSimpleName();
    }

    @Nullable
    public Item handleItemToClient(Item item) {
        if (item == null) return null;
        if (protocol.getMappingData() != null && protocol.getMappingData().getItemMappings() != null) {
            item.setIdentifier(protocol.getMappingData().getNewItemId(item.getIdentifier()));
        }
        return item;
    }

    @Nullable
    public Item handleItemToServer(Item item) {
        if (item == null) return null;
        if (protocol.getMappingData() != null && protocol.getMappingData().getItemMappings() != null) {
            item.setIdentifier(protocol.getMappingData().getOldItemId(item.getIdentifier()));
        }
        restoreDisplayTag(item);
        return item;
    }

    protected boolean hasBackupTag(CompoundTag displayTag, String tagName) {
        return displayTag.contains(nbtTagName + "|o" + tagName);
    }

    protected void saveStringTag(CompoundTag displayTag, StringTag original) {
        // Multiple places might try to backup data
        String name = nbtTagName + "|o" + original.getName();
        if (!displayTag.contains(name)) {
            displayTag.put(new StringTag(name, original.getValue()));
        }
    }

    protected void saveListTag(CompoundTag displayTag, ListTag original) {
        // Multiple places might try to backup data
        String name = nbtTagName + "|o" + original.getName();
        if (!displayTag.contains(name)) {
            // Clone all tag entries
            ListTag listTag = new ListTag(name);
            for (Tag tag : original.getValue()) {
                listTag.add(tag.clone());
            }

            displayTag.put(listTag);
        }
    }

    protected void restoreDisplayTag(Item item) {
        if (item.getTag() == null) return;

        CompoundTag display = item.getTag().get("display");
        if (display != null) {
            // Remove custom name / restore original name
            if (display.remove(nbtTagName + "|customName") != null) {
                display.remove("Name");
            } else {
                restoreStringTag(display, "Name");
            }

            // Restore lore
            restoreListTag(display, "Lore");
        }
    }

    protected void restoreStringTag(CompoundTag tag, String tagName) {
        StringTag original = tag.remove(nbtTagName + "|o" + tagName);
        if (original != null) {
            tag.put(new StringTag(tagName, original.getValue()));
        }
    }

    protected void restoreListTag(CompoundTag tag, String tagName) {
        ListTag original = tag.remove(nbtTagName + "|o" + tagName);
        if (original != null) {
            tag.put(new ListTag(tagName, original.getValue()));
        }
    }

    public String getNbtTagName() {
        return nbtTagName;
    }
}
