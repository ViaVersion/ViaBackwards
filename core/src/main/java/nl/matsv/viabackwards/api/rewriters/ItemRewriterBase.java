package nl.matsv.viabackwards.api.rewriters;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import org.jetbrains.annotations.Nullable;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.rewriters.IdRewriteFunction;
import us.myles.viaversion.libs.opennbt.conversion.builtin.CompoundTagConverter;
import us.myles.viaversion.libs.opennbt.tag.builtin.ByteTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.ShortTag;
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

        CompoundTag tag = item.getTag();
        if (tag == null) {
            if (toServerRewriter != null) {
                item.setIdentifier(toServerRewriter.rewrite(item.getIdentifier()));
            }
            return item;
        }

        CompoundTag viaTag = tag.remove(nbtTagName);
        if (viaTag != null) {
            short id = (short) viaTag.get("id").getValue();
            item.setIdentifier(id);

            Tag dataTag = viaTag.get("data");
            short data = dataTag != null ? (short) dataTag.getValue() : 0;
            item.setData(data);

            Tag amountTag = viaTag.get("amount");
            byte amount = amountTag != null ? (byte) amountTag.getValue() : 1;
            item.setAmount(amount);

            CompoundTag extras = viaTag.get("extras");
            if (extras != null) {
                item.setTag(CONVERTER.convert("", CONVERTER.convert(extras)));
            }
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
        if (item.getAmount() != 1) {
            tag.put(new ByteTag("amount", item.getAmount()));
        }
        if (item.getData() != 0) {
            tag.put(new ShortTag("data", item.getData()));
        }
        if (item.getTag() != null) {
            tag.put(CONVERTER.convert("extras", CONVERTER.convert(item.getTag())));
        }
        return tag;
    }
}
