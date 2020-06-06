package nl.matsv.viabackwards.api.data;

import net.md_5.bungee.api.ChatColor;
import nl.matsv.viabackwards.utils.Block;
import org.jetbrains.annotations.Nullable;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;

public class MappedLegacyBlockItem {

    private final int id;
    private final short data;
    private final String name;
    private final Block block;
    private BlockEntityHandler blockEntityHandler;

    public MappedLegacyBlockItem(int id, short data, @Nullable String name, boolean block) {
        this.id = id;
        this.data = data;
        this.name = name != null ? ChatColor.RESET + name : null;
        this.block = block ? new Block(id, data) : null;
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

    public Block getBlock() {
        return block;
    }

    public boolean hasBlockEntityHandler() {
        return blockEntityHandler != null;
    }

    @Nullable
    public BlockEntityHandler getBlockEntityHandler() {
        return blockEntityHandler;
    }

    public void setBlockEntityHandler(@Nullable BlockEntityHandler blockEntityHandler) {
        this.blockEntityHandler = blockEntityHandler;
    }

    @FunctionalInterface
    public interface BlockEntityHandler {

        CompoundTag handleOrNewCompoundTag(int block, CompoundTag tag);
    }
}
