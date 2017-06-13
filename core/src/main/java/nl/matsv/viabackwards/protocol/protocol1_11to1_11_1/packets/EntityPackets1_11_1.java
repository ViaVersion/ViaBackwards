/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_11to1_11_1.packets;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.entities.storage.EntityData;
import nl.matsv.viabackwards.api.entities.storage.MetaStorage;
import nl.matsv.viabackwards.api.entities.types.AbstractEntityType;
import nl.matsv.viabackwards.api.rewriters.EntityRewriter;
import nl.matsv.viabackwards.protocol.protocol1_11to1_11_1.Protocol1_11To1_11_1;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.version.Types1_9;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

import java.util.Optional;

import static nl.matsv.viabackwards.api.entities.types.EntityType1_11.*;

public class EntityPackets1_11_1 extends EntityRewriter<Protocol1_11To1_11_1> {

    @Override
    protected void registerPackets(Protocol1_11To1_11_1 protocol) {
        // Spawn Object
        protocol.registerOutgoing(State.PLAY, 0x00, 0x00, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity id
                map(Type.UUID); // 1 - UUID
                map(Type.BYTE); // 2 - Type
                map(Type.DOUBLE); // 3 - x
                map(Type.DOUBLE); // 4 - y
                map(Type.DOUBLE); // 5 - z
                map(Type.BYTE); // 6 - Pitch
                map(Type.BYTE); // 7 - Yaw
                map(Type.INT); // 8 - data

                // Track Entity
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        addTrackedEntity(
                                wrapper.user(),
                                wrapper.get(Type.VAR_INT, 0),
                                getTypeFromId(wrapper.get(Type.BYTE, 0), true)
                        );
                    }
                });

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        Optional<ObjectType> type = ObjectType.findById(wrapper.get(Type.BYTE, 0));

                        if (type.isPresent()) {
                            Optional<EntityData> optEntDat = getObjectData(type.get());
                            if (optEntDat.isPresent()) {
                                EntityData data = optEntDat.get();
                                wrapper.set(Type.BYTE, 0, ((Integer) data.getReplacementId()).byteValue());
                                if (data.getObjectData() != -1)
                                    wrapper.set(Type.INT, 0, data.getObjectData());
                            }
                        } else {
                            if (Via.getManager().isDebug()) {
                                ViaBackwards.getPlatform().getLogger().warning("Could not find Entity Type" + wrapper.get(Type.BYTE, 0));
                            }
                        }
                    }
                });
            }
        });

        // Spawn Experience Orb
        protocol.registerOutgoing(State.PLAY, 0x01, 0x01, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity id

                // Track entity
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        addTrackedEntity(
                                wrapper.user(),
                                wrapper.get(Type.VAR_INT, 0),
                                ObjectType.THROWN_EXP_BOTTLE.getType()
                        );
                    }
                });
            }
        });

        // Spawn Global Entity
        protocol.registerOutgoing(State.PLAY, 0x02, 0x02, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.BYTE); // 1 - Type

                // Track entity
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        addTrackedEntity(
                                wrapper.user(),
                                wrapper.get(Type.VAR_INT, 0),
                                EntityType.WEATHER // Always thunder according to wiki.vg
                        );
                    }
                });
            }
        });

        // Spawn Mob
        protocol.registerOutgoing(State.PLAY, 0x03, 0x03, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity id
                map(Type.UUID); // 1 - UUID
                map(Type.VAR_INT); // 2 - Entity Type
                map(Type.DOUBLE); // 3 - X
                map(Type.DOUBLE); // 4 - Y
                map(Type.DOUBLE); // 5 - Z
                map(Type.BYTE); // 6 - Yaw
                map(Type.BYTE); // 7 - Pitch
                map(Type.BYTE); // 8 - Head Pitch
                map(Type.SHORT); // 9 - Velocity X
                map(Type.SHORT); // 10 - Velocity Y
                map(Type.SHORT); // 11 - Velocity Z
                map(Types1_9.METADATA_LIST); // 12 - Metadata

                // Track entity
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        addTrackedEntity(
                                wrapper.user(),
                                wrapper.get(Type.VAR_INT, 0),
                                getTypeFromId(wrapper.get(Type.VAR_INT, 1), false)
                        );
                    }
                });


                // Rewrite entity type / metadata
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int entityId = wrapper.get(Type.VAR_INT, 0);
                        AbstractEntityType type = getEntityType(wrapper.user(), entityId);

                        MetaStorage storage = new MetaStorage(wrapper.get(Types1_9.METADATA_LIST, 0));
                        handleMeta(
                                wrapper.user(),
                                wrapper.get(Type.VAR_INT, 0),
                                storage
                        );

                        Optional<EntityData> optEntDat = getEntityData(type);
                        if (optEntDat.isPresent()) {
                            EntityData data = optEntDat.get();
                            wrapper.set(Type.VAR_INT, 1, data.getReplacementId());
                            if (data.hasBaseMeta())
                                data.getDefaultMeta().handle(storage);
                        }

                        // Rewrite Metadata
                        wrapper.set(
                                Types1_9.METADATA_LIST,
                                0,
                                storage.getMetaDataList()
                        );
                    }
                });
            }
        });

        // Spawn Painting
        protocol.registerOutgoing(State.PLAY, 0x04, 0x04, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID

                // Track entity
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        addTrackedEntity(
                                wrapper.user(),
                                wrapper.get(Type.VAR_INT, 0),
                                EntityType.PAINTING
                        );
                    }
                });
            }
        });

        // Join game
        protocol.registerOutgoing(State.PLAY, 0x23, 0x23, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Entity ID
                map(Type.UNSIGNED_BYTE); // 1 - Gamemode
                map(Type.INT); // 2 - Dimension

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        addTrackedEntity(
                                wrapper.user(),
                                wrapper.get(Type.INT, 0),
                                EntityType.PLAYER
                        );
                    }
                });

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                        int dimensionId = wrapper.get(Type.INT, 1);
                        clientWorld.setEnvironment(dimensionId);
                    }
                });
            }
        });

        // Respawn Packet (save dimension id)
        protocol.registerOutgoing(State.PLAY, 0x33, 0x33, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Dimension ID
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                        int dimensionId = wrapper.get(Type.INT, 0);
                        clientWorld.setEnvironment(dimensionId);
                    }
                });
            }
        });

        // Spawn Player
        protocol.registerOutgoing(State.PLAY, 0x05, 0x05, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.UUID); // 1 - Player UUID
                map(Type.DOUBLE); // 2 - X
                map(Type.DOUBLE); // 3 - Y
                map(Type.DOUBLE); // 4 - Z
                map(Type.BYTE); // 5 - Yaw
                map(Type.BYTE); // 6 - Pitch
                map(Types1_9.METADATA_LIST); // 7 - Metadata list

                // Track Entity
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        addTrackedEntity(
                                wrapper.user(),
                                wrapper.get(Type.VAR_INT, 0),
                                EntityType.PLAYER
                        );
                    }
                });

                // Rewrite Metadata
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.set(
                                Types1_9.METADATA_LIST,
                                0,
                                handleMeta(
                                        wrapper.user(),
                                        wrapper.get(Type.VAR_INT, 0),
                                        new MetaStorage(wrapper.get(Types1_9.METADATA_LIST, 0))
                                ).getMetaDataList()
                        );
                    }
                });
            }
        });

        // Destroy entities
        protocol.registerOutgoing(State.PLAY, 0x30, 0x30, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT_ARRAY); // 0 - Entity IDS

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        for (int entity : wrapper.get(Type.VAR_INT_ARRAY, 0))
                            getEntityTracker(wrapper.user()).removeEntity(entity);
                    }
                });
            }
        });

        // Metadata packet
        protocol.registerOutgoing(State.PLAY, 0x39, 0x39, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Types1_9.METADATA_LIST); // 1 - Metadata list

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.set(
                                Types1_9.METADATA_LIST,
                                0,
                                handleMeta(
                                        wrapper.user(),
                                        wrapper.get(Type.VAR_INT, 0),
                                        new MetaStorage(wrapper.get(Types1_9.METADATA_LIST, 0))
                                ).getMetaDataList()
                        );
                    }
                });
            }
        });
    }

    @Override
    protected void registerRewrites() {
        // Handle non-existing firework metadata (index 7 entity id for boosting)
        registerMetaHandler().filter(EntityType.FIREWORK, 7).removed();

        // Handle non-existing pig metadata (index 14 - boost time)
        registerMetaHandler().filter(EntityType.PIG, 14).removed();
    }
}
