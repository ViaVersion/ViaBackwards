/*
 *
 *     Copyright (C) 2016 Matsv
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.matsv.viabackwards.api.rewriters;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.utils.Block;
import nl.matsv.viabackwards.utils.ItemUtil;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.viaversion.libs.opennbt.conversion.builtin.CompoundTagConverter;
import us.myles.viaversion.libs.opennbt.tag.builtin.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BlockItemRewriter<T extends BackwardsProtocol> extends Rewriter<T> {
    private static final CompoundTagConverter converter = new CompoundTagConverter();
    private final Map<Short, Item> itemRewriter = new ConcurrentHashMap<>();
    private final Map<Integer, Block> blockRewriter = new ConcurrentHashMap<>();

    protected void rewriteItem(int oldItem, Item newItem) {
        itemRewriter.put((short) oldItem, newItem);
    }

    protected void rewriteBlockItem(int oldId, Item newItem, Block newBlock) {
        itemRewriter.put((short) oldId, newItem);
        blockRewriter.put(oldId, newBlock);
    }

    protected Item handleItemToClient(Item item) {
        if (item == null)
            return null;
        if (!itemRewriter.containsKey(item.getId()))
            return item;
        Item i = ItemUtil.copyItem(itemRewriter.get(item.getId()));

        if (i.getTag() == null)
            i.setTag(new CompoundTag(""));
        i.getTag().put(createViaNBT(item));

        if (item.getTag() != null)
            for (Tag ai : item.getTag())
                i.getTag().put(ai);

        i.setAmount(item.getAmount());
        return i;
    }

    protected Item handleItemToServer(Item item) {
        if (item == null || item.getTag() == null)
            return null;
        CompoundTag tag = item.getTag();
        if (tag.contains("ViaBackwards")) {
            CompoundTag via = tag.get("ViaBackwards");

            short id = (short) via.get("id").getValue();
            short data = (short) via.get("data").getValue();
            byte amount = (byte) via.get("amount").getValue();
            CompoundTag extras = via.get("extras");

            item.setId(id);
            item.setData(data);
            item.setAmount(amount);
            item.setTag(converter.convert("", converter.convert(extras)));
            // Remove data tag
            tag.remove("ViaBackwards");
        }
        return item;
    }

    protected Block handleBlock(int block) {
        if (!containsBlock(block))
            return null;

        return blockRewriter.get(block);
    }

    protected boolean containsBlock(int block) {
        return blockRewriter.containsKey(block);
    }

    private CompoundTag createViaNBT(Item i) {
        CompoundTag tag = new CompoundTag("ViaBackwards");
        tag.put(new ShortTag("id", i.getId()));
        tag.put(new ShortTag("data", i.getData()));
        tag.put(new ByteTag("amount", i.getAmount()));
        if (i.getTag() != null) {
            tag.put(converter.convert("extras", converter.convert(i.getTag())));
        } else
            tag.put(new CompoundTag("extras"));
        return tag;
    }

    protected CompoundTag getNamedTag(String text) {
        CompoundTag tag = new CompoundTag("");
        tag.put(new CompoundTag("display"));
        ((CompoundTag) tag.get("display")).put(new StringTag("Name", text));
        return tag;
    }
}
