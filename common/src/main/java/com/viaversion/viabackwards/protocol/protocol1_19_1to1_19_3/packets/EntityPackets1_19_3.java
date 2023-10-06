/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2023 ViaVersion and contributors
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
import com.viaversion.viabackwards.protocol.protocol1_19_1to1_19_3.storage.ChatTypeStorage1_19_3;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ProfileKey;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_19_3Types;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.BitSetType;
import com.viaversion.viaversion.api.type.types.version.Types1_19;
import com.viaversion.viaversion.api.type.types.version.Types1_19_3;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_19_1to1_19.ClientboundPackets1_19_1;
import com.viaversion.viaversion.protocols.protocol1_19_3to1_19_1.ClientboundPackets1_19_3;
import java.util.BitSet;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class EntityPackets1_19_3 extends EntityRewriter<ClientboundPackets1_19_3, Protocol1_19_1To1_19_3> {

    private static final BitSetType PROFILE_ACTIONS_ENUM_TYPE = new BitSetType(6);
    private static final int[] PROFILE_ACTIONS = {2, 4, 5}; // Ignore initialize chat and listed status; add player already handled before
    private static final int ADD_PLAYER = 0;
    private static final int INITIALIZE_CHAT = 1;
    private static final int UPDATE_GAMEMODE = 2;
    private static final int UPDATE_LISTED = 3;
    private static final int UPDATE_LATENCY = 4;
    private static final int UPDATE_DISPLAYNAME = 5;

    public EntityPackets1_19_3(final Protocol1_19_1To1_19_3 protocol) {
        super(protocol, Types1_19.META_TYPES.optionalComponentType, Types1_19.META_TYPES.booleanType);
    }

    @Override
    protected void registerPackets() {
        registerMetadataRewriter(ClientboundPackets1_19_3.ENTITY_METADATA, Types1_19_3.METADATA_LIST, Types1_19.METADATA_LIST);
        registerRemoveEntities(ClientboundPackets1_19_3.REMOVE_ENTITIES);
        registerTrackerWithData1_19(ClientboundPackets1_19_3.SPAWN_ENTITY, Entity1_19_3Types.FALLING_BLOCK);

        protocol.registerClientbound(ClientboundPackets1_19_3.JOIN_GAME, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // Entity id
                map(Type.BOOLEAN); // Hardcore
                map(Type.UNSIGNED_BYTE); // Gamemode
                map(Type.BYTE); // Previous Gamemode
                map(Type.STRING_ARRAY); // World List
                map(Type.NAMED_COMPOUND_TAG); // Dimension registry
                map(Type.STRING); // Dimension key
                map(Type.STRING); // World
                handler(dimensionDataHandler());
                handler(biomeSizeTracker());
                handler(worldDataTrackerHandlerByKey());
                handler(wrapper -> {
                    final ChatTypeStorage1_19_3 chatTypeStorage = wrapper.user().get(ChatTypeStorage1_19_3.class);
                    chatTypeStorage.clear();
                    final CompoundTag registry = wrapper.get(Type.NAMED_COMPOUND_TAG, 0);
                    final ListTag chatTypes = ((CompoundTag) registry.get("minecraft:chat_type")).get("value");
                    for (final Tag chatType : chatTypes) {
                        final CompoundTag chatTypeCompound = (CompoundTag) chatType;
                        final NumberTag idTag = chatTypeCompound.get("id");
                        chatTypeStorage.addChatType(idTag.asInt(), chatTypeCompound);
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19_3.RESPAWN, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING); // Dimension
                map(Type.STRING); // World
                map(Type.LONG); // Seed
                map(Type.UNSIGNED_BYTE); // Gamemode
                map(Type.BYTE); // Previous gamemode
                map(Type.BOOLEAN); // Debug
                map(Type.BOOLEAN); // Flat
                handler(worldDataTrackerHandlerByKey());
                handler(wrapper -> {
                    // Old clients will always keep entity data (packed here as 0x02), nothing we can do there
                    final byte keepDataMask = wrapper.read(Type.BYTE);
                    wrapper.write(Type.BOOLEAN, (keepDataMask & 1) != 0); // Keep attributes
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19_3.PLAYER_INFO_UPDATE, ClientboundPackets1_19_1.PLAYER_INFO, wrapper -> {
            wrapper.cancel();
            final BitSet actions = wrapper.read(PROFILE_ACTIONS_ENUM_TYPE);
            final int entries = wrapper.read(Type.VAR_INT);
            if (actions.get(ADD_PLAYER)) {
                // Special case, as we need to write everything into one action
                final PacketWrapper playerInfoPacket = wrapper.create(ClientboundPackets1_19_1.PLAYER_INFO);
                playerInfoPacket.write(Type.VAR_INT, 0);
                playerInfoPacket.write(Type.VAR_INT, entries);
                for (int i = 0; i < entries; i++) {
                    playerInfoPacket.write(Type.UUID, wrapper.read(Type.UUID));
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
                    if (actions.get(INITIALIZE_CHAT) && wrapper.read(Type.BOOLEAN)) {
                        wrapper.read(Type.UUID); // Session UUID
                        profileKey = wrapper.read(Type.PROFILE_KEY);
                    } else {
                        profileKey = null;
                    }

                    final int gamemode = actions.get(UPDATE_GAMEMODE) ? wrapper.read(Type.VAR_INT) : 0;

                    if (actions.get(UPDATE_LISTED)) {
                        wrapper.read(Type.BOOLEAN); // Listed - throw away
                    }

                    final int latency = actions.get(UPDATE_LATENCY) ? wrapper.read(Type.VAR_INT) : 0;

                    final JsonElement displayName = actions.get(UPDATE_DISPLAYNAME) ? wrapper.read(Type.OPTIONAL_COMPONENT) : null;
                    playerInfoPacket.write(Type.VAR_INT, gamemode);
                    playerInfoPacket.write(Type.VAR_INT, latency);
                    playerInfoPacket.write(Type.OPTIONAL_COMPONENT, displayName);
                    playerInfoPacket.write(Type.OPTIONAL_PROFILE_KEY, profileKey);
                }
                playerInfoPacket.send(Protocol1_19_1To1_19_3.class);
                return;
            }

            final PlayerProfileUpdate[] updates = new PlayerProfileUpdate[entries];
            for (int i = 0; i < entries; i++) {
                final UUID uuid = wrapper.read(Type.UUID);
                int gamemode = 0;
                int latency = 0;
                JsonElement displayName = null;
                for (final int action : PROFILE_ACTIONS) {
                    if (!actions.get(action)) {
                        continue;
                    }
                    switch (action) {
                        case UPDATE_GAMEMODE:
                            gamemode = wrapper.read(Type.VAR_INT);
                            break;
                        case UPDATE_LATENCY:
                            latency = wrapper.read(Type.VAR_INT);
                            break;
                        case UPDATE_DISPLAYNAME:
                            displayName = wrapper.read(Type.OPTIONAL_COMPONENT);
                            break;
                    }
                }

                updates[i] = new PlayerProfileUpdate(uuid, gamemode, latency, displayName);
            }

            if (actions.get(UPDATE_GAMEMODE)) {
                sendPlayerProfileUpdate(wrapper.user(), 1, updates);
            } else if (actions.get(UPDATE_LATENCY)) {
                sendPlayerProfileUpdate(wrapper.user(), 2, updates);
            } else if (actions.get(UPDATE_DISPLAYNAME)) {
                sendPlayerProfileUpdate(wrapper.user(), 3, updates);
            }
        });
        protocol.registerClientbound(ClientboundPackets1_19_3.PLAYER_INFO_REMOVE, ClientboundPackets1_19_1.PLAYER_INFO, wrapper -> {
            final UUID[] uuids = wrapper.read(Type.UUID_ARRAY);
            wrapper.write(Type.VAR_INT, 4); // Remove player
            wrapper.write(Type.VAR_INT, uuids.length);
            for (final UUID uuid : uuids) {
                wrapper.write(Type.UUID, uuid);
            }
        });
    }

    private void sendPlayerProfileUpdate(final UserConnection connection, final int action, final PlayerProfileUpdate[] updates) throws Exception {
        final PacketWrapper playerInfoPacket = PacketWrapper.create(ClientboundPackets1_19_1.PLAYER_INFO, connection);
        playerInfoPacket.write(Type.VAR_INT, action);
        playerInfoPacket.write(Type.VAR_INT, updates.length);
        for (final PlayerProfileUpdate update : updates) {
            playerInfoPacket.write(Type.UUID, update.uuid());
            if (action == 1) {
                playerInfoPacket.write(Type.VAR_INT, update.gamemode());
            } else if (action == 2) {
                playerInfoPacket.write(Type.VAR_INT, update.latency());
            } else if (action == 3) {
                playerInfoPacket.write(Type.OPTIONAL_COMPONENT, update.displayName());
            } else {
                throw new IllegalArgumentException("Invalid action: " + action);
            }
        }
        playerInfoPacket.send(Protocol1_19_1To1_19_3.class);
    }

    @Override
    public void registerRewrites() {
        filter().handler((event, meta) -> {
            final int id = meta.metaType().typeId();
            if (id > 2) {
                meta.setMetaType(Types1_19.META_TYPES.byId(id - 1)); // long added
            } else if (id != 2) {
                meta.setMetaType(Types1_19.META_TYPES.byId(id));
            }
        });
        registerMetaTypeHandler(Types1_19.META_TYPES.itemType, Types1_19.META_TYPES.blockStateType, null, Types1_19.META_TYPES.particleType,
                Types1_19.META_TYPES.componentType, Types1_19.META_TYPES.optionalComponentType);

        filter().index(6).handler((event, meta) -> {
            // Sitting pose added
            final int pose = meta.value();
            if (pose == 10) {
                meta.setValue(0); // Standing
            } else if (pose > 10) {
                meta.setValue(pose - 1);
            }
        });
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

    private static final class PlayerProfileUpdate {
        private final UUID uuid;
        private final int gamemode;
        private final int latency;
        private final JsonElement displayName;

        private PlayerProfileUpdate(final UUID uuid, final int gamemode, final int latency, @Nullable final JsonElement displayName) {
            this.uuid = uuid;
            this.gamemode = gamemode;
            this.latency = latency;
            this.displayName = displayName;
        }

        public UUID uuid() {
            return uuid;
        }

        public int gamemode() {
            return gamemode;
        }

        public int latency() {
            return latency;
        }

        public @Nullable JsonElement displayName() {
            return displayName;
        }
    }
}