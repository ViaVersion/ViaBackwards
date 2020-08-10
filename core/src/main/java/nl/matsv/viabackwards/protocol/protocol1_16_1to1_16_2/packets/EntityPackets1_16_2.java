package nl.matsv.viabackwards.protocol.protocol1_16_1to1_16_2.packets;

import com.google.common.collect.Sets;
import nl.matsv.viabackwards.api.rewriters.EntityRewriter;
import nl.matsv.viabackwards.protocol.protocol1_16_1to1_16_2.Protocol1_16_1To1_16_2;
import us.myles.ViaVersion.api.entities.Entity1_16Types;
import us.myles.ViaVersion.api.entities.Entity1_16_2Types;
import us.myles.ViaVersion.api.entities.EntityType;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.minecraft.metadata.MetaType;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.minecraft.metadata.types.MetaType1_14;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.version.Types1_14;
import us.myles.ViaVersion.protocols.protocol1_16_2to1_16_1.ClientboundPackets1_16_2;
import us.myles.ViaVersion.protocols.protocol1_16to1_15_2.packets.EntityPackets;
import us.myles.viaversion.libs.gson.JsonElement;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.StringTag;

import java.util.Set;

public class EntityPackets1_16_2 extends EntityRewriter<Protocol1_16_1To1_16_2> {

    private final Set<String> oldDimensions = Sets.newHashSet("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");

    public EntityPackets1_16_2(Protocol1_16_1To1_16_2 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        registerSpawnTrackerWithData(ClientboundPackets1_16_2.SPAWN_ENTITY, Entity1_16_2Types.EntityType.FALLING_BLOCK, Protocol1_16_1To1_16_2::getNewBlockStateId);
        registerSpawnTracker(ClientboundPackets1_16_2.SPAWN_MOB);
        registerExtraTracker(ClientboundPackets1_16_2.SPAWN_EXPERIENCE_ORB, Entity1_16_2Types.EntityType.EXPERIENCE_ORB);
        registerExtraTracker(ClientboundPackets1_16_2.SPAWN_PAINTING, Entity1_16_2Types.EntityType.PAINTING);
        registerExtraTracker(ClientboundPackets1_16_2.SPAWN_PLAYER, Entity1_16_2Types.EntityType.PLAYER);
        registerEntityDestroy(ClientboundPackets1_16_2.DESTROY_ENTITIES);
        registerMetadataRewriter(ClientboundPackets1_16_2.ENTITY_METADATA, Types1_14.METADATA_LIST);

        protocol.registerOutgoing(ClientboundPackets1_16_2.JOIN_GAME, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // Entity ID
                handler(wrapper -> {
                    boolean hardcore = wrapper.read(Type.BOOLEAN);
                    short gamemode = wrapper.read(Type.UNSIGNED_BYTE);
                    if (hardcore) {
                        gamemode |= 0x08;
                    }
                    wrapper.write(Type.UNSIGNED_BYTE, gamemode);
                });
                map(Type.BYTE); // Previous Gamemode
                map(Type.STRING_ARRAY); // World List
                handler(wrapper -> {
                    // Just screw the registry and write the defaults for 1.16 and 1.16.1 clients
                    wrapper.read(Type.NBT);
                    wrapper.write(Type.NBT, EntityPackets.DIMENSIONS_TAG);

                    CompoundTag dimensionData = wrapper.read(Type.NBT);
                    wrapper.write(Type.STRING, getDimensionFromData(dimensionData));
                });
                map(Type.STRING); // Dimension
                map(Type.LONG); // Seed
                handler(wrapper -> {
                    int maxPlayers = wrapper.read(Type.VAR_INT);
                    wrapper.write(Type.UNSIGNED_BYTE, (short) Math.max(maxPlayers, 255));
                });
                // ...
                handler(getTrackerHandler(Entity1_16_2Types.EntityType.PLAYER, Type.INT));
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_16_2.RESPAWN, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    CompoundTag dimensionData = wrapper.read(Type.NBT);
                    wrapper.write(Type.STRING, getDimensionFromData(dimensionData));
                });
            }
        });
    }

    private String getDimensionFromData(CompoundTag dimensionData) {
        // This may technically break other custom dimension settings for 1.16/1.16.1 clients, so those cases are considered semi "unsupported" here
        StringTag effectsLocation = dimensionData.get("effects");
        return effectsLocation != null && oldDimensions.contains(effectsLocation.getValue()) ? effectsLocation.getValue() : "minecraft:overworld";
    }

    @Override
    protected void registerRewrites() {
        registerMetaHandler().handle(e -> {
            Metadata meta = e.getData();
            MetaType type = meta.getMetaType();
            if (type == MetaType1_14.Slot) {
                meta.setValue(protocol.getBlockItemPackets().handleItemToClient((Item) meta.getValue()));
            } else if (type == MetaType1_14.BlockID) {
                meta.setValue(Protocol1_16_1To1_16_2.getNewBlockStateId((int) meta.getValue()));
            } else if (type == MetaType1_14.OptChat) {
                JsonElement text = meta.getCastedValue();
                if (text != null) {
                    protocol.getTranslatableRewriter().processText(text);
                }
            }
            return meta;
        });

        mapTypes(Entity1_16_2Types.EntityType.values(), Entity1_16Types.EntityType.class);
        mapEntity(Entity1_16_2Types.EntityType.PIGLIN_BRUTE, Entity1_16_2Types.EntityType.PIGLIN).jsonName("Piglin Brute");

        registerMetaHandler().filter(Entity1_16_2Types.EntityType.ABSTRACT_PIGLIN, true).handle(meta -> {
            if (meta.getIndex() == 15) {
                meta.getData().setId(16);
            } else if (meta.getIndex() == 16) {
                meta.getData().setId(15);
            }
            return meta.getData();
        });
    }

    @Override
    protected EntityType getTypeFromId(int typeId) {
        return Entity1_16Types.getTypeFromId(typeId);
    }
}
