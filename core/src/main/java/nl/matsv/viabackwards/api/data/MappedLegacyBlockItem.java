package nl.matsv.viabackwards.api.data;

import net.md_5.bungee.api.ChatColor;
import nl.matsv.viabackwards.utils.Block;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;

public class MappedLegacyBlockItem {

    private final int id;
    private final short data;
    private final String name;
    private Block block;
    private BlockEntityHandler blockEntityHandler;

    public MappedLegacyBlockItem(int id, short data, String name) {
        this.id = id;
        this.data = data;
        this.name = name != null ? ChatColor.RESET + name : null;
    }

    public int getId() {
        return id;
    }

    public short getData() {
        return data;
    }

    public String getName() {
        return name;
    }

    public boolean isBlock() {
        return block != null;
    }

    // Mark this as a block item
    public void setBlock() {
        block = new Block(id, data);
    }

    public Block getBlock() {
        return block;
    }

    public boolean hasBlockEntityHandler() {
        return blockEntityHandler != null;
    }

    public BlockEntityHandler getBlockEntityHandler() {
        return blockEntityHandler;
    }

    public void setBlockEntityHandler(BlockEntityHandler blockEntityHandler) {
        this.blockEntityHandler = blockEntityHandler;
    }

    @FunctionalInterface
    public interface BlockEntityHandler {

        CompoundTag handleOrNewCompoundTag(int block, CompoundTag tag);
    }
}
