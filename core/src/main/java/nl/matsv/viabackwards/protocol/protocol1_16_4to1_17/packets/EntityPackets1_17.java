package nl.matsv.viabackwards.protocol.protocol1_16_4to1_17.packets;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.rewriters.EntityRewriter;
import nl.matsv.viabackwards.protocol.protocol1_16_4to1_17.Protocol1_16_4To1_17;
import us.myles.ViaVersion.api.entities.Entity1_16_2Types;
import us.myles.ViaVersion.api.entities.Entity1_17Types;
import us.myles.ViaVersion.api.entities.EntityType;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.minecraft.metadata.MetaType;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.minecraft.metadata.types.MetaType1_14;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.Particle;
import us.myles.ViaVersion.api.type.types.version.Types1_14;
import us.myles.ViaVersion.api.type.types.version.Types1_17;
import us.myles.ViaVersion.protocols.protocol1_17to1_16_4.ClientboundPackets1_17;
import us.myles.viaversion.libs.gson.JsonElement;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.IntTag;

public class EntityPackets1_17 extends EntityRewriter<Protocol1_16_4To1_17> {

    public EntityPackets1_17(Protocol1_16_4To1_17 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        registerSpawnTrackerWithData(ClientboundPackets1_17.SPAWN_ENTITY, Entity1_16_2Types.EntityType.FALLING_BLOCK);
        registerSpawnTracker(ClientboundPackets1_17.SPAWN_MOB);
        registerExtraTracker(ClientboundPackets1_17.SPAWN_EXPERIENCE_ORB, Entity1_16_2Types.EntityType.EXPERIENCE_ORB);
        registerExtraTracker(ClientboundPackets1_17.SPAWN_PAINTING, Entity1_16_2Types.EntityType.PAINTING);
        registerExtraTracker(ClientboundPackets1_17.SPAWN_PLAYER, Entity1_16_2Types.EntityType.PLAYER);
        registerEntityDestroy(ClientboundPackets1_17.DESTROY_ENTITIES);
        registerMetadataRewriter(ClientboundPackets1_17.ENTITY_METADATA, Types1_17.METADATA_LIST, Types1_14.METADATA_LIST);
        protocol.registerOutgoing(ClientboundPackets1_17.JOIN_GAME, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // Entity ID
                map(Type.BOOLEAN); // Hardcore
                map(Type.UNSIGNED_BYTE); // Gamemode
                map(Type.BYTE); // Previous Gamemode
                map(Type.STRING_ARRAY); // Worlds
                map(Type.NBT); // Dimension registry
                map(Type.NBT); // Current dimension data
                handler(wrapper -> {
                    byte previousGamemode = wrapper.get(Type.BYTE, 0);
                    if (previousGamemode == -1) { // "Unset" gamemode removed
                        wrapper.set(Type.BYTE, 0, (byte) 0);
                    }
                });
                handler(getTrackerHandler(Entity1_16_2Types.EntityType.PLAYER, Type.INT));
                handler(getWorldDataTracker(1));
                handler(wrapper -> warnForExtendedHeight(wrapper.get(Type.NBT, 1)));
            }
        });
        protocol.registerOutgoing(ClientboundPackets1_17.RESPAWN, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.NBT); // Dimension data
                handler(getWorldDataTracker(0));
                handler(wrapper -> warnForExtendedHeight(wrapper.get(Type.NBT, 0)));
            }
        });
    }

    @Override
    protected void registerRewrites() {
        registerMetaHandler().handle(e -> {
            Metadata meta = e.getData();
            meta.setMetaType(MetaType1_14.byId(meta.getMetaType().getTypeID()));

            MetaType type = meta.getMetaType();
            if (type == MetaType1_14.Slot) {
                meta.setValue(protocol.getBlockItemPackets().handleItemToClient((Item) meta.getValue()));
            } else if (type == MetaType1_14.BlockID) {
                meta.setValue(protocol.getMappingData().getNewBlockStateId((int) meta.getValue()));
            } else if (type == MetaType1_14.OptChat) {
                JsonElement text = meta.getCastedValue();
                if (text != null) {
                    //protocol.getTranslatableRewriter().processText(text); //TODO
                }
            } else if (type == MetaType1_14.PARTICLE) {
                Particle particle = (Particle) meta.getValue();
                if (particle.getId() == 14 || particle.getId() == 15) { // Dust / Dust Transition
                    // RGB is encoded as doubles in 1.17
                    for (int i = 0; i < 3; i++) {
                        Particle.ParticleData data = particle.getArguments().get(i);
                        data.setValue(((Number) data.getValue()).floatValue());
                        data.setType(Type.FLOAT);
                    }

                    if (particle.getId() == 15) {
                        // Remove transition target color values 4-6
                        particle.getArguments().subList(4, 7).clear();
                    }
                } else if (particle.getId() == 36) { // Vibration Signal
                    // No nice mapping possible without tracking entity positions and doing particle tasks
                    particle.setId(0);
                    particle.getArguments().clear();
                    return meta;
                }

                rewriteParticle(particle);
            }
            return meta;
        });

        mapTypes(Entity1_17Types.EntityType.values(), Entity1_16_2Types.EntityType.class);
        registerMetaHandler().filter(Entity1_17Types.EntityType.AXOLOTL, 17).removed();
        registerMetaHandler().filter(Entity1_17Types.EntityType.AXOLOTL, 18).removed();
        registerMetaHandler().filter(Entity1_17Types.EntityType.AXOLOTL, 19).removed();

        registerMetaHandler().filter(Entity1_17Types.EntityType.GLOW_SQUID, 16).removed();

        mapEntity(Entity1_17Types.EntityType.AXOLOTL, Entity1_17Types.EntityType.TROPICAL_FISH).jsonName("Axolotl");

        mapEntity(Entity1_17Types.EntityType.GLOW_SQUID, Entity1_17Types.EntityType.SQUID).jsonName("Glow Squid");
        mapEntity(Entity1_17Types.EntityType.GLOW_ITEM_FRAME, Entity1_17Types.EntityType.ITEM_FRAME);

        registerMetaHandler().filter(7).removed(); // Ticks frozen
        registerMetaHandler().handle(meta -> {
            if (meta.getIndex() > 7) {
                meta.getData().setId(meta.getIndex() - 1);
            }
            return meta.getData();
        });
    }

    @Override
    protected EntityType getTypeFromId(int typeId) {
        return Entity1_17Types.getTypeFromId(typeId);
    }

    private void warnForExtendedHeight(CompoundTag tag) {
        IntTag minY = tag.get("min_y");
        IntTag height = tag.get("height");
        if (minY.getValue() != 0 || height.getValue() != 256) {
            ViaBackwards.getPlatform().getLogger().severe("Custom worlds heights are NOT SUPPORTED for 1.16 players and older and may lead to errors!");
            ViaBackwards.getPlatform().getLogger().severe("You have min/max set to " + minY.getValue() + "/" + height.getValue());
        }
    }
}
