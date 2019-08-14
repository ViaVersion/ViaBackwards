package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data.BackwardsMappings;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.providers.BackwardsBlockEntityProvider;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.blockconnections.ConnectionData;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data.MappingData;
import us.myles.viaversion.libs.gson.JsonElement;
import us.myles.viaversion.libs.gson.JsonObject;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.IntTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.StringTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.Tag;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class PistonHandler implements BackwardsBlockEntityProvider.BackwardsBlockEntityHandler {

    private final Map<String, Integer> pistonIds = new HashMap<>();

    public PistonHandler() {
        if (Via.getConfig().isServersideBlockConnections()) {
            Map<String, Integer> keyToId;
            try {
                Field field = ConnectionData.class.getDeclaredField("keyToId");
                field.setAccessible(true);
                keyToId = (Map<String, Integer>) field.get(null);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
                return;
            }

            for (Map.Entry<String, Integer> entry : keyToId.entrySet()) {
                if (!entry.getKey().contains("piston")) continue;

                addEntries(entry.getKey(), entry.getValue());
            }
        } else {
            JsonObject mappings = MappingData.loadData("mapping-1.13.json").getAsJsonObject("blocks");
            for (Map.Entry<String, JsonElement> blockState : mappings.entrySet()) {
                String key = blockState.getValue().getAsString();
                if (!key.contains("piston")) continue;

                addEntries(key, Integer.parseInt(blockState.getKey()));
            }
        }
    }

    // There doesn't seem to be a nicer way around it :(
    private void addEntries(String data, int id) {
        id = BackwardsMappings.blockMappings.getNewBlock(id);
        pistonIds.put(data, id);

        String substring = data.substring(10);
        if (!substring.startsWith("piston") && !substring.startsWith("sticky_piston")) return;

        // Swap properties and add them to the map
        String[] split = data.substring(0, data.length() - 1).split("\\[");
        String[] properties = split[1].split(",");
        data = split[0] + "[" + properties[1] + "," + properties[0] + "]";
        pistonIds.put(data, id);
    }

    @Override
    public CompoundTag transform(UserConnection user, int blockId, CompoundTag tag) {
        CompoundTag blockState = tag.get("blockState");
        String dataFromTag = getDataFromTag(blockState);
        if (dataFromTag == null) return tag;

        if (!pistonIds.containsKey(dataFromTag)) {
            ViaBackwards.getPlatform().getLogger().warning("Unmapped piston id: " + dataFromTag);
            return tag;
        }

        int id = pistonIds.get(dataFromTag);
        tag.put(new IntTag("blockId", id >> 4));
        tag.put(new IntTag("blockData", id & 15));
        return tag;
    }

    // The type hasn't actually been updated in the blockstorage, so we need to construct it
    private String getDataFromTag(CompoundTag tag) {
        StringTag name = tag.get("Name");
        if (name == null) return null;

        CompoundTag properties = tag.get("Properties");
        if (properties == null) return name.getValue();

        StringJoiner joiner = new StringJoiner(",", name.getValue() + "[", "]");
        for (Tag property : properties) {
            if (!(property instanceof StringTag)) continue;
            joiner.add(property.getName() + "=" + ((StringTag) property).getValue());
        }
        return joiner.toString();
    }
}
