package nl.matsv.viabackwards.api.data;

import com.google.common.base.Preconditions;
import nl.matsv.viabackwards.api.BackwardsProtocol;
import org.jetbrains.annotations.Nullable;
import us.myles.ViaVersion.api.data.MappingData;
import us.myles.ViaVersion.api.data.Mappings;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.protocol.ProtocolRegistry;
import us.myles.viaversion.libs.fastutil.ints.Int2ObjectMap;
import us.myles.viaversion.libs.gson.JsonObject;

import java.util.Map;

public class BackwardsMappings extends MappingData {

    private final Class<? extends Protocol> vvProtocolClass;
    private Int2ObjectMap<MappedItem> backwardsItemMappings;
    private Map<String, String> backwardsSoundMappings;

    public BackwardsMappings(String oldVersion, String newVersion, @Nullable Class<? extends Protocol> vvProtocolClass) {
        this(oldVersion, newVersion, vvProtocolClass, false);
    }

    public BackwardsMappings(String oldVersion, String newVersion, @Nullable Class<? extends Protocol> vvProtocolClass, boolean hasDiffFile) {
        super(oldVersion, newVersion, hasDiffFile);
        Preconditions.checkArgument(!vvProtocolClass.isAssignableFrom(BackwardsProtocol.class));
        this.vvProtocolClass = vvProtocolClass;
        // Just re-use ViaVersion's item id map
        loadItems = false;
    }

    @Override
    protected void loadExtras(JsonObject oldMappings, JsonObject newMappings, @Nullable JsonObject diffMappings) {
        if (diffMappings != null) {
            JsonObject diffItems = diffMappings.getAsJsonObject("items");
            if (diffItems != null) {
                backwardsItemMappings = VBMappingDataLoader.loadItemMappings(oldMappings.getAsJsonObject("items"), newMappings.getAsJsonObject("items"), diffItems);
            }

            JsonObject diffSounds = diffMappings.getAsJsonObject("sounds");
            if (diffSounds != null) {
                backwardsSoundMappings = VBMappingDataLoader.objectToMap(diffSounds);
            }
        }

        // Just re-use ViaVersion's item id map
        if (vvProtocolClass != null) {
            itemMappings = ProtocolRegistry.getProtocol(vvProtocolClass).getMappingData().getItemMappings().inverse();
        }

        loadVBExtras(oldMappings, newMappings);
    }

    @Override
    @Nullable
    protected Mappings loadFromArray(JsonObject oldMappings, JsonObject newMappings, @Nullable JsonObject diffMappings, String key) {
        if (!oldMappings.has(key) || !newMappings.has(key)) return null;

        JsonObject diff = diffMappings != null ? diffMappings.getAsJsonObject(key) : null;
        return new VBMappings(oldMappings.getAsJsonArray(key), newMappings.getAsJsonArray(key), diff, shouldWarnOnMissing(key));
    }

    @Override
    @Nullable
    protected Mappings loadFromObject(JsonObject oldMappings, JsonObject newMappings, @Nullable JsonObject diffMappings, String key) {
        if (!oldMappings.has(key) || !newMappings.has(key)) return null;

        JsonObject diff = diffMappings != null ? diffMappings.getAsJsonObject(key) : null;
        return new VBMappings(oldMappings.getAsJsonObject(key), newMappings.getAsJsonObject(key), diff, shouldWarnOnMissing(key));
    }

    @Override
    protected JsonObject loadDiffFile() {
        return VBMappingDataLoader.loadFromDataDir("mapping-" + newVersion + "to" + oldVersion + ".json");
    }

    /**
     * To be overridden.
     */
    protected void loadVBExtras(JsonObject oldMappings, JsonObject newMappings) {
    }

    protected boolean shouldWarnOnMissing(String key) {
        return !key.equals("blocks") && !key.equals("statistics");
    }

    /**
     * @see #getMappedItem(int) for custom backwards mappings
     */
    @Override
    public int getNewItemId(int id) {
        // Don't warn on missing here
        return this.itemMappings.get(id);
    }

    @Override
    public int getNewBlockId(int id) {
        // Don't warn on missing here
        return this.blockMappings.getNewId(id);
    }

    @Override
    public int getOldItemId(final int id) {
        // Warn on missing
        return checkValidity(id, this.itemMappings.inverse().get(id), "item");
    }

    @Nullable
    public MappedItem getMappedItem(int id) {
        return backwardsItemMappings != null ? backwardsItemMappings.get(id) : null;
    }

    @Nullable
    public String getMappedNamedSound(String id) {
        return backwardsSoundMappings != null ? backwardsSoundMappings.get(id) : null;
    }

    @Nullable
    public Int2ObjectMap<MappedItem> getBackwardsItemMappings() {
        return backwardsItemMappings;
    }

    @Nullable
    public Map<String, String> getBackwardsSoundMappings() {
        return backwardsSoundMappings;
    }
}
