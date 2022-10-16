/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2022 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.viaversion.viabackwards.protocol.protocol1_19_1to1_19_3.packets;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_19_1to1_19_3.Protocol1_19_1To1_19_3;
import com.viaversion.viaversion.api.minecraft.ProfileKey;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_19_3Types;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.BitSetType;
import com.viaversion.viaversion.api.type.types.version.Types1_19;
import com.viaversion.viaversion.api.type.types.version.Types1_19_3;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.protocols.protocol1_19_1to1_19.ClientboundPackets1_19_1;
import com.viaversion.viaversion.protocols.protocol1_19_3to1_19_1.ClientboundPackets1_19_3;

import java.util.BitSet;
import java.util.UUID;

public final class EntityPackets1_19_3 extends EntityRewriter<Protocol1_19_1To1_19_3> {

    private static final BitSetType PROFILE_ACTIONS_ENUM_TYPE = new BitSetType(6);
    private static final int[] PROFILE_ACTIONS = {0, 2, 4, 5}; // Ignore initialize chat and listed status

    public EntityPackets1_19_3(final Protocol1_19_1To1_19_3 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        registerMetadataRewriter(ClientboundPackets1_19_3.ENTITY_METADATA, Types1_19_3.METADATA_LIST, Types1_19.METADATA_LIST);
        registerRemoveEntities(ClientboundPackets1_19_3.REMOVE_ENTITIES);
        registerTrackerWithData1_19(ClientboundPackets1_19_3.SPAWN_ENTITY, Entity1_19_3Types.FALLING_BLOCK);

        protocol.registerClientbound(ClientboundPackets1_19_3.JOIN_GAME, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // Entity id
                map(Type.BOOLEAN); // Hardcore
                map(Type.UNSIGNED_BYTE); // Gamemode
                map(Type.BYTE); // Previous Gamemode
                map(Type.STRING_ARRAY); // World List
                map(Type.NBT); // Dimension registry
                map(Type.STRING); // Dimension key
                map(Type.STRING); // World
                handler(dimensionDataHandler());
                handler(biomeSizeTracker());
                handler(worldDataTrackerHandlerByKey());
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19_3.RESPAWN, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Dimension
                map(Type.STRING); // World
                handler(worldDataTrackerHandlerByKey());
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19_3.PLAYER_INFO_UPDATE, ClientboundPackets1_19_1.PLAYER_INFO, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    wrapper.cancel();
                    final BitSet actions = wrapper.read(PROFILE_ACTIONS_ENUM_TYPE);
                    final int entries = wrapper.read(Type.VAR_INT);
                    for (final int action : PROFILE_ACTIONS) {
                        if (!actions.get(action)) {
                            continue;
                        }

                        final PacketWrapper playerInfoPacket = wrapper.create(ClientboundPackets1_19_1.PLAYER_INFO);
                        if (action == 0) {
                            playerInfoPacket.write(Type.VAR_INT, 0);
                        } else if (action == 2) {
                            playerInfoPacket.write(Type.VAR_INT, 1);
                        } else {
                            playerInfoPacket.write(Type.VAR_INT, action - 2);
                        }

                        playerInfoPacket.write(Type.VAR_INT, entries);
                        playerInfoPacket.write(Type.UUID, wrapper.read(Type.UUID));
                        switch (action) {
                            case 0: // Add player
                                playerInfoPacket.write(Type.STRING, wrapper.read(Type.STRING)); // Player Name

                                final int properties = wrapper.read(Type.VAR_INT);
                                playerInfoPacket.write(Type.VAR_INT, properties);
                                for (int j = 0; j < properties; j++) {
                                    playerInfoPacket.write(Type.STRING, wrapper.read(Type.STRING)); // Name
                                    playerInfoPacket.write(Type.STRING, wrapper.read(Type.STRING)); // Value
                                    playerInfoPacket.write(Type.OPTIONAL_STRING, wrapper.read(Type.OPTIONAL_STRING)); // Signature
                                }

                                // Now check for the other parts individually and add dummy values if not present
                                final ProfileKey profileKey;
                                if (actions.get(1)) {
                                    wrapper.read(Type.UUID); // Session UUID
                                    profileKey = wrapper.read(Type.OPTIONAL_PROFILE_KEY);
                                } else {
                                    profileKey = null;
                                }

                                final int gamemode = actions.get(2) ? wrapper.read(Type.VAR_INT) : 0;
                                if (actions.get(3)) {
                                    wrapper.read(Type.BOOLEAN); // Listed - throw away
                                }

                                final int latency = actions.get(4) ? wrapper.read(Type.VAR_INT) : 0;
                                final JsonElement displayName = actions.get(5) ? wrapper.read(Type.OPTIONAL_COMPONENT) : null;
                                playerInfoPacket.write(Type.VAR_INT, gamemode);
                                playerInfoPacket.write(Type.VAR_INT, latency);
                                playerInfoPacket.write(Type.OPTIONAL_COMPONENT, displayName);
                                playerInfoPacket.write(Type.OPTIONAL_PROFILE_KEY, profileKey);
                                playerInfoPacket.send(Protocol1_19_1To1_19_3.class);
                                return; // We're done
                            case 2: // Update gamemode
                            case 4: // Update latency
                                playerInfoPacket.write(Type.VAR_INT, wrapper.read(Type.VAR_INT));
                                break;
                            case 5: // Update display name
                                playerInfoPacket.write(Type.OPTIONAL_COMPONENT, wrapper.read(Type.OPTIONAL_COMPONENT));
                                break;
                        }
                        playerInfoPacket.send(Protocol1_19_1To1_19_3.class);
                    }
                });
            }
        });
        protocol.registerClientbound(ClientboundPackets1_19_3.PLAYER_INFO_REMOVE, ClientboundPackets1_19_1.PLAYER_INFO, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    final UUID[] uuids = wrapper.read(Type.UUID_ARRAY);
                    wrapper.write(Type.VAR_INT, 4); // Remove player
                    wrapper.write(Type.VAR_INT, uuids.length);
                    for (final UUID uuid : uuids) {
                        wrapper.write(Type.UUID, uuid);
                    }
                });
            }
        });
    }

    @Override
    public void registerRewrites() {
        //TODO metadata oopsies
        filter().handler((event, meta) -> {
            final int id = meta.metaType().typeId();
            meta.setMetaType(Types1_19.META_TYPES.byId(id > 2 ? id - 1 : id)); // long added
        });
        registerMetaTypeHandler(Types1_19.META_TYPES.itemType, Types1_19.META_TYPES.blockStateType, Types1_19.META_TYPES.particleType, Types1_19.META_TYPES.optionalComponentType);

        filter().filterFamily(Entity1_19_3Types.MINECART_ABSTRACT).index(11).handler((event, meta) -> {
            final int data = (int) meta.getValue();
            meta.setValue(protocol.getMappingData().getNewBlockStateId(data));
        });

        filter().type(Entity1_19_3Types.CAMEL).cancel(19); // Dashing
        filter().type(Entity1_19_3Types.CAMEL).cancel(20); // Last pose change time
    }

    @Override
    public void onMappingDataLoaded() {
        mapTypes();
        mapEntityTypeWithData(Entity1_19_3Types.CAMEL, Entity1_19_3Types.DONKEY).jsonName();
    }

    @Override
    public EntityType typeFromId(final int typeId) {
        return Entity1_19_3Types.getTypeFromId(typeId);
    }
}