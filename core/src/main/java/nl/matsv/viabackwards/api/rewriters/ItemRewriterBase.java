package nl.matsv.viabackwards.api.rewriters;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.rewriters.IdRewriteFunction;
import us.myles.viaversion.libs.opennbt.conversion.builtin.CompoundTagConverter;
import us.myles.viaversion.libs.opennbt.tag.builtin.ByteTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.ShortTag;

public abstract class ItemRewriterBase<T extends BackwardsProtocol> extends Rewriter<T> {

    protected static final CompoundTagConverter CONVERTER = new CompoundTagConverter();
    protected final IdRewriteFunction toClientRewriter;
    protected final IdRewriteFunction toServerRewriter;
    protected final String nbtTagName;
    protected final boolean jsonNameFormat;

    protected ItemRewriterBase(T protocol, IdRewriteFunction toClientRewriter, IdRewriteFunction toServerRewriter, boolean jsonNameFormat) {
        super(protocol);
        this.toClientRewriter = toClientRewriter;
        this.toServerRewriter = toServerRewriter;
        this.jsonNameFormat = jsonNameFormat;
        nbtTagName = "ViaBackwards|" + protocol.getClass().getSimpleName();
    }

    protected ItemRewriterBase(T protocol, boolean jsonNameFormat) {
        this(protocol, null, null, jsonNameFormat);
    }

    public Item handleItemToClient(Item item) {
        if (item == null) return null;
        if (toClientRewriter != null) {
            item.setIdentifier(toClientRewriter.rewrite(item.getIdentifier()));
        }
        return item;
    }

    public Item handleItemToServer(Item item) {
        if (item == null) return null;

        CompoundTag tag = item.getTag();
        if (tag == null) {
            if (toServerRewriter != null) {
                item.setIdentifier(toServerRewriter.rewrite(item.getIdentifier()));
            }
            return item;
        }

        CompoundTag viaTag = tag.get(nbtTagName);
        if (viaTag != null) {
            short id = (short) viaTag.get("id").getValue();
            short data = (short) viaTag.get("data").getValue();
            byte amount = (byte) viaTag.get("amount").getValue();
            CompoundTag extras = viaTag.get("extras");

            item.setIdentifier(id);
            item.setData(data);
            item.setAmount(amount);
            if (extras != null) {
                item.setTag(CONVERTER.convert("", CONVERTER.convert(extras)));
            }
            // Remove data tag
            tag.remove(nbtTagName);
        } else {
            // Rewrite id normally
            if (toServerRewriter != null) {
                item.setIdentifier(toServerRewriter.rewrite(item.getIdentifier()));
            }
        }
        return item;
    }

    protected CompoundTag createViaNBT(Item item) {
        CompoundTag tag = new CompoundTag(nbtTagName);
        tag.put(new ShortTag("id", (short) item.getIdentifier()));
        tag.put(new ShortTag("data", item.getData()));
        tag.put(new ByteTag("amount", item.getAmount()));
        if (item.getTag() != null) {
            tag.put(CONVERTER.convert("extras", CONVERTER.convert(item.getTag())));
        }
        return tag;
    }
}
