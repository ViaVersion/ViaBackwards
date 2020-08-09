package nl.matsv.viabackwards.api.rewriters;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import org.jetbrains.annotations.Nullable;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.rewriters.IdRewriteFunction;
import us.myles.viaversion.libs.opennbt.conversion.builtin.CompoundTagConverter;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.ListTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.StringTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.Tag;

public abstract class ItemRewriterBase<T extends BackwardsProtocol> extends Rewriter<T> {

    protected static final CompoundTagConverter CONVERTER = new CompoundTagConverter();
    protected final IdRewriteFunction toClientRewriter;
    protected final IdRewriteFunction toServerRewriter;
    protected final String nbtTagName;
    protected final boolean jsonNameFormat;

    protected ItemRewriterBase(T protocol, @Nullable IdRewriteFunction toClientRewriter, @Nullable IdRewriteFunction toServerRewriter, boolean jsonNameFormat) {
        super(protocol);
        this.toClientRewriter = toClientRewriter;
        this.toServerRewriter = toServerRewriter;
        this.jsonNameFormat = jsonNameFormat;
        nbtTagName = "VB|" + protocol.getClass().getSimpleName();
    }

    protected ItemRewriterBase(T protocol, boolean jsonNameFormat) {
        this(protocol, null, null, jsonNameFormat);
    }

    @Nullable
    public Item handleItemToClient(Item item) {
        if (item == null) return null;
        if (toClientRewriter != null) {
            item.setIdentifier(toClientRewriter.rewrite(item.getIdentifier()));
        }
        return item;
    }

    @Nullable
    public Item handleItemToServer(Item item) {
        if (item == null) return null;
        if (toServerRewriter != null) {
            item.setIdentifier(toServerRewriter.rewrite(item.getIdentifier()));
        }
        restoreDisplayTag(item);
        return item;
    }

    protected void saveNameTag(CompoundTag displayTag, StringTag original) {
        displayTag.put(new StringTag(nbtTagName + "|o" + original.getName(), original.getValue()));
    }

    protected void saveLoreTag(CompoundTag displayTag, ListTag original) {
        displayTag.put(new ListTag(nbtTagName + "|o" + original.getName(), original.getValue()));
    }

    protected void restoreDisplayTag(Item item) {
        if (item.getTag() == null) return;

        CompoundTag display = item.getTag().get("display");
        if (display != null) {
            // Remove custom name / restore original name
            if (display.remove(nbtTagName + "|customName") != null) {
                display.remove("Name");
            } else {
                restoreDisplayTag(display, "Name");
            }

            // Restore lore
            restoreDisplayTag(display, "Lore");
        }
    }

    protected void restoreDisplayTag(CompoundTag displayTag, String tagName) {
        Tag original = displayTag.remove(nbtTagName + "|o" + tagName);
        if (original != null) {
            displayTag.put(original);
        }
    }
}
