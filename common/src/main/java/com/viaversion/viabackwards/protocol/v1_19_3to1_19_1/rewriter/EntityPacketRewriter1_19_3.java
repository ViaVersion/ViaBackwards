/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2024 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_19_3to1_19_1.rewriter;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_19_3to1_19_1.Protocol1_19_3To1_19_1;
import com.viaversion.viabackwards.protocol.v1_19_3to1_19_1.storage.ChatTypeStorage1_19_3;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ProfileKey;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_19_3;
import com.viaversion.viaversion.api.minecraft.signature.storage.ChatSession1_19_3;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.BitSetType;
import com.viaversion.viaversion.api.type.types.version.Types1_19;
import com.viaversion.viaversion.api.type.types.version.Types1_19_3;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.viaversion.protocols.v1_19_1to1_19_3.packet.ClientboundPackets1_19_3;
import com.viaversion.viaversion.protocols.v1_19_1to1_19_3.packet.ServerboundPackets1_19_3;
import com.viaversion.viaversion.protocols.v1_19to1_19_1.packet.ClientboundPackets1_19_1;
import com.viaversion.viaversion.util.TagUtil;
import java.util.BitSet;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class EntityPacketRewriter1_19_3 extends EntityRewriter<ClientboundPackets1_19_3, Protocol1_19_3To1_19_1> {

    private static final BitSetType PROFILE_ACTIONS_ENUM_TYPE = new BitSetType(6);
    private static final int[] PROFILE_ACTIONS = {2, 4, 5}; // Ignore initialize chat and listed status; add player already handled before
    private static final int ADD_PLAYER = 0;
    private static final int INITIALIZE_CHAT = 1;
    private static final int UPDATE_GAMEMODE = 2;
    private static final int UPDATE_LISTED = 3;
    private static final int UPDATE_LATENCY = 4;
    private static final int UPDATE_DISPLAYNAME = 5;

    public EntityPacketRewriter1_19_3(final Protocol1_19_3To1_19_1 protocol) {
        super(protocol, Types1_19.ENTITY_DATA_TYPES.optionalComponentType, Types1_19.ENTITY_DATA_TYPES.booleanType);
    }

    @Override
    protected void registerPackets() {
        registerSetEntityData(ClientboundPackets1_19_3.SET_ENTITY_DATA, Types1_19_3.ENTITY_DATA_LIST, Types1_19.ENTITY_DATA_LIST);
        registerRemoveEntities(ClientboundPackets1_19_3.REMOVE_ENTITIES);
        registerTrackerWithData1_19(ClientboundPackets1_19_3.ADD_ENTITY, EntityTypes1_19_3.FALLING_BLOCK);

        protocol.registerClientbound(ClientboundPackets1_19_3.LOGIN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // Entity id
                map(Types.BOOLEAN); // Hardcore
                map(Types.BYTE); // Gamemode
                map(Types.BYTE); // Previous Gamemode
                map(Types.STRING_ARRAY); // World List
                map(Types.NAMED_COMPOUND_TAG); // Dimension registry
                map(Types.STRING); // Dimension key
                map(Types.STRING); // World
                handler(dimensionDataHandler());
                handler(biomeSizeTracker());
                handler(worldDataTrackerHandlerByKey());
                handler(wrapper -> {
                    final ChatTypeStorage1_19_3 chatTypeStorage = wrapper.user().get(ChatTypeStorage1_19_3.class);
                    chatTypeStorage.clear();

                    final CompoundTag registry = wrapper.get(Types.NAMED_COMPOUND_TAG, 0);
                    final ListTag<CompoundTag> chatTypes = TagUtil.getRegistryEntries(registry, "chat_type", new ListTag<>(CompoundTag.class));
                    for (final CompoundTag chatType : chatTypes) {
                        final NumberTag idTag = chatType.getNumberTag("id");
                        chatTypeStorage.addChatType(idTag.asInt(), chatType);
                    }
                });
                handler(wrapper -> {
                    final ChatSession1_19_3 chatSession = wrapper.user().get(ChatSession1_19_3.class);

                    if (chatSession != null) {
                        final PacketWrapper chatSessionUpdate = wrapper.create(ServerboundPackets1_19_3.CHAT_SESSION_UPDATE);
                        chatSessionUpdate.write(Types.UUID, chatSession.getSessionId());
                        chatSessionUpdate.write(Types.PROFILE_KEY, chatSession.getProfileKey());
                        chatSessionUpdate.sendToServer(Protocol1_19_3To1_19_1.class);
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19_3.RESPAWN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.STRING); // Dimension
                map(Types.STRING); // World
                map(Types.LONG); // Seed
                map(Types.UNSIGNED_BYTE); // Gamemode
                map(Types.BYTE); // Previous gamemode
                map(Types.BOOLEAN); // Debug
                map(Types.BOOLEAN); // Flat
                handler(worldDataTrackerHandlerByKey());
                handler(wrapper -> {
                    // Old clients will always keep entity data (packed here as 0x02), nothing we can do there
                    final byte keepDataMask = wrapper.read(Types.BYTE);
                    wrapper.write(Types.BOOLEAN, (keepDataMask & 1) != 0); // Keep attributes
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19_3.PLAYER_INFO_UPDATE, ClientboundPackets1_19_1.PLAYER_INFO, wrapper -> {
            wrapper.cancel();
            final BitSet actions = wrapper.read(PROFILE_ACTIONS_ENUM_TYPE);
            final int entries = wrapper.read(Types.VAR_INT);
            if (actions.get(ADD_PLAYER)) {
                // Special case, as we need to write everything into one action
                final PacketWrapper playerInfoPacket = wrapper.create(ClientboundPackets1_19_1.PLAYER_INFO);
                playerInfoPacket.write(Types.VAR_INT, 0);
                playerInfoPacket.write(Types.VAR_INT, entries);
                for (int i = 0; i < entries; i++) {
                    playerInfoPacket.write(Types.UUID, wrapper.read(Types.UUID));
                    playerInfoPacket.write(Types.STRING, wrapper.read(Types.STRING)); // Player Name

                    final int properties = wrapper.read(Types.VAR_INT);
                    playerInfoPacket.write(Types.VAR_INT, properties);
                    for (int j = 0; j < properties; j++) {
                        playerInfoPacket.write(Types.STRING, wrapper.read(Types.STRING)); // Name
                        playerInfoPacket.write(Types.STRING, wrapper.read(Types.STRING)); // Value
                        playerInfoPacket.write(Types.OPTIONAL_STRING, wrapper.read(Types.OPTIONAL_STRING)); // Signature
                    }

                    // Now check for the other parts individually and add dummy values if not present
                    final ProfileKey profileKey;
                    if (actions.get(INITIALIZE_CHAT) && wrapper.read(Types.BOOLEAN)) {
                        wrapper.read(Types.UUID); // Session UUID
                        profileKey = wrapper.read(Types.PROFILE_KEY);
                    } else {
                        profileKey = null;
                    }

                    final int gamemode = actions.get(UPDATE_GAMEMODE) ? wrapper.read(Types.VAR_INT) : 0;

                    if (actions.get(UPDATE_LISTED)) {
                        wrapper.read(Types.BOOLEAN); // Listed - throw away
                    }

                    final int latency = actions.get(UPDATE_LATENCY) ? wrapper.read(Types.VAR_INT) : 0;

                    final JsonElement displayName = actions.get(UPDATE_DISPLAYNAME) ? wrapper.read(Types.OPTIONAL_COMPONENT) : null;
                    playerInfoPacket.write(Types.VAR_INT, gamemode);
                    playerInfoPacket.write(Types.VAR_INT, latency);
                    playerInfoPacket.write(Types.OPTIONAL_COMPONENT, displayName);
                    playerInfoPacket.write(Types.OPTIONAL_PROFILE_KEY, profileKey);
                }
                playerInfoPacket.send(Protocol1_19_3To1_19_1.class);
                return;
            }

            final PlayerProfileUpdate[] updates = new PlayerProfileUpdate[entries];
            for (int i = 0; i < entries; i++) {
                final UUID uuid = wrapper.read(Types.UUID);
                int gamemode = 0;
                int latency = 0;
                JsonElement displayName = null;
                for (final int action : PROFILE_ACTIONS) {
                    if (!actions.get(action)) {
                        continue;
                    }
                    switch (action) {
                        case UPDATE_GAMEMODE -> gamemode = wrapper.read(Types.VAR_INT);
                        case UPDATE_LATENCY -> latency = wrapper.read(Types.VAR_INT);
                        case UPDATE_DISPLAYNAME -> displayName = wrapper.read(Types.OPTIONAL_COMPONENT);
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
            final UUID[] uuids = wrapper.read(Types.UUID_ARRAY);
            wrapper.write(Types.VAR_INT, 4); // Remove player
            wrapper.write(Types.VAR_INT, uuids.length);
            for (final UUID uuid : uuids) {
                wrapper.write(Types.UUID, uuid);
            }
        });
    }

    private void sendPlayerProfileUpdate(final UserConnection connection, final int action, final PlayerProfileUpdate[] updates) {
        final PacketWrapper playerInfoPacket = PacketWrapper.create(ClientboundPackets1_19_1.PLAYER_INFO, connection);
        playerInfoPacket.write(Types.VAR_INT, action);
        playerInfoPacket.write(Types.VAR_INT, updates.length);
        for (final PlayerProfileUpdate update : updates) {
            playerInfoPacket.write(Types.UUID, update.uuid());
            if (action == 1) {
                playerInfoPacket.write(Types.VAR_INT, update.gamemode());
            } else if (action == 2) {
                playerInfoPacket.write(Types.VAR_INT, update.latency());
            } else if (action == 3) {
                playerInfoPacket.write(Types.OPTIONAL_COMPONENT, update.displayName());
            } else {
                throw new IllegalArgumentException("Invalid action: " + action);
            }
        }
        playerInfoPacket.send(Protocol1_19_3To1_19_1.class);
    }

    @Override
    public void registerRewrites() {
        filter().handler((event, meta) -> {
            final int id = meta.dataType().typeId();
            if (id > 2) {
                meta.setDataType(Types1_19.ENTITY_DATA_TYPES.byId(id - 1)); // long added
            } else if (id != 2) {
                meta.setDataType(Types1_19.ENTITY_DATA_TYPES.byId(id));
            }
        });
        registerMetaTypeHandler(Types1_19.ENTITY_DATA_TYPES.itemType, null, Types1_19.ENTITY_DATA_TYPES.optionalBlockStateType, Types1_19.ENTITY_DATA_TYPES.particleType,
            Types1_19.ENTITY_DATA_TYPES.componentType, Types1_19.ENTITY_DATA_TYPES.optionalComponentType);

        filter().dataType(Types1_19.ENTITY_DATA_TYPES.poseType).handler((event, meta) -> {
            // Sitting pose added
            final int pose = meta.value();
            if (pose == 10) {
                meta.setValue(0); // Standing
            } else if (pose > 10) {
                meta.setValue(pose - 1);
            }
        });
        filter().type(EntityTypes1_19_3.ABSTRACT_MINECART).index(11).handler((event, meta) -> {
            final int data = (int) meta.getValue();
            meta.setValue(protocol.getMappingData().getNewBlockStateId(data));
        });

        filter().type(EntityTypes1_19_3.CAMEL).cancel(19); // Dashing
        filter().type(EntityTypes1_19_3.CAMEL).cancel(20); // Last pose change time
    }

    @Override
    public void onMappingDataLoaded() {
        mapTypes();
        mapEntityTypeWithData(EntityTypes1_19_3.CAMEL, EntityTypes1_19_3.DONKEY).jsonName();
    }

    @Override
    public EntityType typeFromId(final int typeId) {
        return EntityTypes1_19_3.getTypeFromId(typeId);
    }

    private record PlayerProfileUpdate(UUID uuid, int gamemode, int latency, @Nullable JsonElement displayName) {
    }
}
