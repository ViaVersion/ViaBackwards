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

package nl.matsv.viabackwards.protocol.protocol1_9_4to1_10.packets;

import nl.matsv.viabackwards.api.exceptions.RemovedValueException;
import nl.matsv.viabackwards.api.rewriters.EntityRewriter;
import nl.matsv.viabackwards.api.storage.EntityTracker;
import nl.matsv.viabackwards.protocol.protocol1_9_4to1_10.Protocol1_9To1_10;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.version.Types1_9;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import us.myles.ViaVersion.protocols.protocol1_9to1_8.metadata.NewType;

public class EntityPackets extends EntityRewriter<Protocol1_9To1_10> {

    @Override
    protected void registerPackets(Protocol1_9To1_10 protocol) {

        // Spawn Object
        protocol.registerOutgoing(State.PLAY, 0x00, 0x00, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity id
                map(Type.UUID); // 1 - UUID
                map(Type.BYTE); // 2 - Type

                // Track Entity
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        addTrackedEntity(
                                wrapper.user(),
                                wrapper.get(Type.VAR_INT, 0),
                                true,
                                wrapper.get(Type.BYTE, 0)
                        );
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
                                true,
                                (short) 2
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
                                true,
                                wrapper.get(Type.BYTE, 0)
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
                map(Type.UNSIGNED_BYTE); // 2 - Entity Type
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
                                false,
                                wrapper.get(Type.UNSIGNED_BYTE, 0)
                        );
                    }
                });

                // Rewrite entity ids
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.set(Type.UNSIGNED_BYTE, 0,
                                getNewEntityType(
                                        wrapper.user(),
                                        wrapper.get(Type.VAR_INT, 0)
                                ));


                    }
                });

                // Rewrite metadata
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.set(
                                Types1_9.METADATA_LIST,
                                0,
                                handleMeta(
                                        wrapper.user(),
                                        wrapper.get(Type.VAR_INT, 0),
                                        wrapper.get(Types1_9.METADATA_LIST, 0)
                                )
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
                                true,
                                (short) 9
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
                                false,
                                (short) -12
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
                                false,
                                (short) -12
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
                                        wrapper.get(Types1_9.METADATA_LIST, 0)
                                )
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
                            wrapper.user().get(EntityTracker.class).removeEntity(entity);
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
                                        wrapper.get(Types1_9.METADATA_LIST, 0)
                                )
                        );
                    }
                });
            }
        });
    }

    @Override
    protected void registerRewrites() {
        rewriteEntityId(102, 91); // Replace polar bear with sheep

        // Handle Polar bear
        registerMetaRewriter((isObject, entityType, data) -> { // Change the sheep color when the polar bear is stending up
            if (!isObject && entityType != 102)
                return data;

            if (data.getId() == 13) { // is boolean
                boolean b = (boolean) data.getValue();

                data.setId(13);
                data.setType(Type.BYTE);
                data.setTypeID(NewType.Byte.getTypeID());
                data.setValue(b ? (byte) (14 & 0x0F) : 0);
            }
            return data;
        });

        // Handle Husk
        registerMetaRewriter((isObject, entityType, data) -> { // Change husk to normal zombie
            if (isObject || entityType != 54)
                return data;

            if (data.getId() == 13 && data.getTypeID() == 1 && (int) data.getValue() == 6)
                data.setValue(0);
            return data;
        });

        // Handle stray
        registerMetaRewriter((isObject, entityType, data) -> { // Change stray- to normal skeleton
            if (isObject || entityType != 51)
                return data;

            if (data.getId() == 12 && data.getTypeID() == 1 && (int) data.getValue() == 2)
                data.setValue(0);
            return data;
        });

        // Handle the missing NoGravity tag
        registerMetaRewriter((isObject, entityType, m) -> {
            if (m.getId() == 5)
                throw new RemovedValueException();
            else if (m.getId() >= 5)
                m.setId(m.getId() - 1);
            return m;
        });
    }
}
